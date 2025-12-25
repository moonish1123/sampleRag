package pe.brice.rag.library.vectorStore.data.impl

import android.content.Context
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.brice.rag.library.faissNative.FaissNativeWrapper
import pe.brice.rag.library.vectorStore.data.datasource.VectorStoreDataSource
import pe.brice.rag.library.vectorStore.domain.model.Document
import pe.brice.rag.library.vectorStore.domain.model.SearchResult
import pe.brice.rag.library.vectorStore.domain.model.Vector
import java.io.File

class FaissDataSource(
    private val context: Context,
    private val indexFileName: String = "faiss_index.bin",
    private val dimension: Int = 384
) : VectorStoreDataSource {

    companion object {
        private const val TAG = "FaissDataSource"
        private const val SAVE_THRESHOLD = 5
    }

    private val indexFile: File by lazy {
        File(context.dataDir, indexFileName)
    }

    private var initialized = false
    private val initLock = Mutex()
    private var pendingSaveCount = 0

    // In-memory metadata storage (id -> metadata)
    private val metadataMap = mutableMapOf<String, Map<String, String>>()
    private val contentMap = mutableMapOf<String, String>()
    private val metadataLock = Mutex()

    private suspend fun ensureInitialized() = initLock.withLock {
        if (!initialized) {
            try {
                Log.d(TAG, "Initializing Faiss index with dimension: $dimension")

                // Initialize store
                val initSuccess = FaissNativeWrapper.initStore(dimension)
                if (!initSuccess) {
                    throw IllegalStateException("Failed to initialize Faiss store")
                }

                // Ensure parent directory exists
                indexFile.parentFile?.mkdirs()

                // Load existing index if available
                if (indexFile.exists() && indexFile.length() > 0) {
                    Log.d(TAG, "Loading existing index from: ${indexFile.absolutePath}")
                    val loadSuccess = FaissNativeWrapper.loadIndexNative(indexFile.absolutePath)
                    if (!loadSuccess) {
                        Log.w(TAG, "Failed to load index, creating new one")
                        // Backup corrupted file and create new
                        indexFile.renameTo(File(indexFile.absolutePath + ".backup"))
                        FaissNativeWrapper.initStore(dimension)
                        saveIndexInternal()
                    } else {
                        Log.d(TAG, "Index loaded successfully")
                    }
                } else {
                    // Save new empty index
                    Log.d(TAG, "Creating new index file")
                    saveIndexInternal()
                }

                initialized = true
                Log.d(TAG, "Faiss initialization completed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Faiss: ${e.message}", e)
                throw e
            }
        }
    }

    private suspend fun saveIndexInternal(): Boolean {
        return try {
            val success = FaissNativeWrapper.saveIndexNative(indexFile.absolutePath)
            if (success) {
                pendingSaveCount = 0
                Log.d(TAG, "Index saved successfully")
            } else {
                Log.w(TAG, "Failed to save index")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error saving index: ${e.message}", e)
            false
        }
    }

    override suspend fun addVectors(vectors: List<Vector>): Result<Unit> {
        return try {
            ensureInitialized()

            if (vectors.isEmpty()) {
                return Result.success(Unit)
            }

            // Flatten vectors for JNI call
            val flatArray = FloatArray(vectors.size * dimension)
            vectors.forEachIndexed { i, vector ->
                if (vector.values.size != dimension) {
                    throw IllegalArgumentException(
                        "Vector at index $i has dimension ${vector.values.size}, expected $dimension"
                    )
                }
                System.arraycopy(vector.values.toFloatArray(), 0, flatArray, i * dimension, dimension)
            }

            // Add to Faiss
            val ids = FaissNativeWrapper.addVectors(flatArray, vectors.size)

            if (ids == null || ids.size != vectors.size) {
                throw IllegalStateException("Failed to add vectors to Faiss index")
            }

            // Store metadata
            metadataLock.withLock {
                vectors.forEachIndexed { i, vector ->
                    val faissId = ids[i]
                    metadataMap[faissId.toString()] = vector.metadata
                    contentMap[faissId.toString()] = vector.metadata["content"] ?: ""
                }
            }

            // Periodic save for performance
            pendingSaveCount++
            if (pendingSaveCount >= SAVE_THRESHOLD) {
                saveIndexInternal()
            }

            Log.d(TAG, "Added ${vectors.size} vectors successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add vectors: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun search(queryVector: List<Float>, topK: Int): Result<List<SearchResult>> {
        return try {
            ensureInitialized()

            if (queryVector.size != dimension) {
                throw IllegalArgumentException(
                    "Query vector has dimension ${queryVector.size}, expected $dimension"
                )
            }

            val results = FaissNativeWrapper.queryVectors(
                queryVector.toFloatArray(),
                1,
                topK
            )

            if (results == null || results.size < 2) {
                return Result.success(emptyList())
            }

            val indices = results[0] as? LongArray
            val distances = results[1] as? FloatArray

            if (indices == null || distances == null || indices.size != distances.size) {
                return Result.success(emptyList())
            }

            if (indices.isEmpty()) {
                return Result.success(emptyList())
            }

            val matches = metadataLock.withLock {
                indices.mapIndexed { i, faissId ->
                    val id = faissId.toString()
                    SearchResult(
                        document = Document(
                            id = id,
                            content = contentMap[id] ?: "",
                            metadata = metadataMap[id] ?: emptyMap()
                        ),
                        score = distances[i]
                    )
                }
            }

            Log.d(TAG, "Found ${matches.size} results")
            Result.success(matches)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteById(id: String): Result<Unit> {
        // Faiss doesn't support individual deletion easily
        // Would need to rebuild index excluding this ID
        return Result.failure(
            UnsupportedOperationException("Faiss doesn't support individual vector deletion. Use deleteAll() instead.")
        )
    }

    override suspend fun deleteAll(): Result<Unit> {
        return try {
            initLock.withLock {
                if (initialized) {
                    FaissNativeWrapper.destroyStore()
                    metadataMap.clear()
                    contentMap.clear()
                    initialized = false

                    // Create new empty index
                    ensureInitialized()
                    saveIndexInternal()

                    Log.d(TAG, "All vectors deleted")
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getById(id: String): Result<Document?> {
        return try {
            ensureInitialized()

            metadataLock.withLock {
                val document = if (metadataMap.containsKey(id)) {
                    Document(
                        id = id,
                        content = contentMap[id] ?: "",
                        metadata = metadataMap[id] ?: emptyMap()
                    )
                } else {
                    null
                }
                Result.success(document)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get by id: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun saveIndex(path: String): Result<Unit> {
        return try {
            ensureInitialized()
            val success = FaissNativeWrapper.saveIndexNative(path)
            if (success) {
                Log.d(TAG, "Index saved to: $path")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to save index to $path"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save index: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun loadIndex(path: String): Result<Unit> {
        return try {
            initLock.withLock {
                val success = FaissNativeWrapper.loadIndexNative(path)
                if (success) {
                    initialized = true
                    Log.d(TAG, "Index loaded from: $path")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to load index from $path"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load index: ${e.message}", e)
            Result.failure(e)
        }
    }
}
