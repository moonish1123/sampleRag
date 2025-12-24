package pe.brice.rag.vectorstore.faiss

import kotlinx.coroutines.runBlocking
import pe.brice.rag.RagApplication
import pe.brice.rag.legacy.util.LogUtils
import pe.brice.rag.RagService
import pe.brice.rag.llm.Embedding
import pe.brice.rag.vectorstore.RagVectorStore
import pe.brice.rag.vectorstore.VectorMatch
import pe.brice.rag.vectorstore.metadataStore.PreferenceMetaDataStore
import java.io.File

class RagFaissDataStore(val embedder: Embedding, val metaDataStore: PreferenceMetaDataStore): RagVectorStore {
    companion object {
        @Volatile private var initialized: Boolean = false
        private val indexFile = File(RagApplication.getInstance().dataDir, "rag${File.separator}faiss_index_v${RagService.VERSION}.bin")
        private val initializationLock = Any()

        private fun <T> safeRunnable(thiz: RagFaissDataStore, block: suspend () -> T): T {
            // Double-checked locking for thread safety
            if (!initialized) {
                synchronized(initializationLock) {
                    if (!initialized) {
                        // FaissWrapper.initStore는 메모리에서만 초기화합니다.
                        FaissWrapper.initStore(thiz.embedder.getDimensions())

                        val parentDir = indexFile.parentFile
                        if (parentDir != null && !parentDir.exists()) {
                            if (!parentDir.mkdirs()) {
                                throw IllegalStateException("Failed to create directory: ${parentDir.absolutePath}")
                            }
                        }

                        if (indexFile.exists() && indexFile.length() > 0) { // 파일이 존재하고 내용도 있는지 확인
                            if (!FaissWrapper.loadIndexNative(indexFile.absolutePath)) {
                                // 로드 실패 시, 손상된 파일일 수 있으므로 새로 시작하거나 오류 처리
                                LogUtils.e("FaissInit", "Failed to load Faiss index from ${indexFile.absolutePath}. Re-initializing.")
                                // 기존 파일 백업 후 새로 생성
                                val backupFile = File(indexFile.absolutePath + ".backup")
                                indexFile.renameTo(backupFile)

                                // 새 인덱스 파일 생성
                                FaissWrapper.initStore(thiz.embedder.getDimensions())
                                if (!FaissWrapper.saveIndexNative(indexFile.absolutePath)) {
                                    throw IllegalStateException("Failed to create new Faiss index after load failure")
                                }
                                LogUtils.i("FaissInit", "Created new Faiss index after load failure. Old file backed up to: ${backupFile.absolutePath}")
                            }
                        } else {
                            // 인덱스 파일이 없거나 비어있으면, 현재 (빈) 메모리 인덱스를 저장하여 새 파일 생성
                            // FaissWrapper.initStore(dimension)는 이미 호출되었으므로 메모리에는 빈 인덱스가 있음.
                            if (!FaissWrapper.saveIndexNative(indexFile.absolutePath)) {
                                throw IllegalStateException("Failed to save new Faiss index to ${indexFile.absolutePath}")
                            }
                        }
                        initialized = true
                    }
                }
            }

            return runBlocking {
                block()
            }
        }
    }

    private val dimension = embedder.getDimensions()

    // For performance optimization: save index after multiple additions
    private var pendingSaveCount = 0
    private val SAVE_THRESHOLD = 5 // Save after every 5 additions

    override fun addVector(doc: String, metadata: Map<String, String>): Long = safeRunnable(this) {
        val floatEmbedding = embedder.getEmbedding(doc).toFloatArray()
        // FaissWrapper.addVectors는 이제 LongArray?를 반환
        val returnedFaissIds: LongArray? = FaissWrapper.addVectors(floatEmbedding, 1)

        if (returnedFaissIds != null && returnedFaissIds.isNotEmpty()) {
            val newFaissId = returnedFaissIds[0] // 단일 벡터 추가이므로 첫 번째 ID 사용

            // 매핑 정보 저장
            metaDataStore.addMetadata(newFaissId, metadata)
            LogUtils.d("FaissDataStore", "Vector added. Faiss ID: $newFaissId")

            // 성능 최적화: 여러 번 추가 후 한 번에 저장
            pendingSaveCount++
            if (pendingSaveCount >= SAVE_THRESHOLD) {
                val saveSuccess = FaissWrapper.saveIndexNative(indexFile.absolutePath)
                if (saveSuccess) {
                    pendingSaveCount = 0
                    LogUtils.d("FaissDataStore", "FAISS index saved successfully after $SAVE_THRESHOLD additions")
                } else {
                    LogUtils.w("FaissDataStore", "Failed to save FAISS index")
                }
            }

            return@safeRunnable newFaissId
        } else {
            LogUtils.e("FaissDataStore", "Failed to add vector")
            return@safeRunnable -1
        }
    }
/*
    override fun addVectors(embeddings: List<List<Float>>, metadataList: List<Map<String, String>>): List<Long> = safeRunnable(this) {
        if (embeddings.size != metadataList.size) {
            throw IllegalArgumentException("Embeddings and metadataList must have the same size. Embeddings: ${embeddings.size}, Metadata: ${metadataList.size}")
        }
        if (embeddings.isEmpty()) {
            LogUtils.i("FaissDataStore", "No vectors to add.")
            return@safeRunnable emptyList<Long>() // 추가할 것이 없으면 빈 ID 리스트 반환
        }

        val numVectors = embeddings.size
        val flatJavaVectors = FloatArray(numVectors * dimension)
        for (i in embeddings.indices) {
            if (embeddings[i].size != dimension) {
                // ids[i]가 없어졌으므로 오류 메시지에서 제거
                throw IllegalArgumentException("Embedding at index $i (size: ${embeddings[i].size}) does not match dimension $dimension")
            }
            System.arraycopy(embeddings[i].toFloatArray(), 0, flatJavaVectors, i * dimension, dimension)
        }

        val returnedFaissIds: LongArray? = FaissWrapper.addVectors(flatJavaVectors, numVectors)

        if (returnedFaissIds != null && returnedFaissIds.size == numVectors) {
            for (i in embeddings.indices) {
                val faissId = returnedFaissIds[i]
                val metadata = metadataList[i]
                metaDataStore.addMetadata(faissId, metadata)
                LogUtils.d("FaissDataStore", "Batch vector added. Faiss ID: $faissId, Metadata: $metadata")
            }

            // Faiss 인덱스 파일 저장 (성능 고려하여 저장 빈도 조절 필요)
            val faissSaveSuccess = FaissWrapper.saveIndexNative(indexFile.absolutePath)
            // ID 매핑 정보(메타데이터 맵) 저장 (구현 필요)
            // Companion.saveIdMappingsToFile(idMappingFilePrefix)

            if (!faissSaveSuccess) {
                LogUtils.e("FaissDataStore", "Failed to save Faiss index to disk after adding vectors.")
                // 저장 실패 시 어떻게 처리할지 결정 필요:
                // 1. 예외를 던진다.
                // 2. 빈 리스트를 반환하여 부분적 실패를 알린다. (ID는 반환하지만 저장은 실패)
                // 여기서는 일단 ID는 반환하되, 저장 실패 로그를 남깁니다.
                // 또는, 저장 실패도 중요한 오류로 간주한다면 아래 주석처럼 처리:
                return@safeRunnable emptyList<Long>()
            }

            return@safeRunnable returnedFaissIds.toList() // 성공적으로 추가된 Faiss ID 목록 반환
        } else {
            LogUtils.e("FaissDataStore", "Failed to add batch vectors or get all Faiss IDs. Expected ${numVectors} IDs, got ${returnedFaissIds?.size ?: "null"}")
            return@safeRunnable emptyList<Long>() // 실패 시 빈 ID 리스트 반환
        }
    }*/

    override fun deleteVector(id: Long): Boolean {
        TODO("Not yet implemented")
    }

    override fun deleteAllVectors(): Boolean = safeRunnable(this) {
        FaissWrapper.destroyStore()
        FaissWrapper.initStore(dimension) // 인덱스 초기화
        //메모리에서 삭제 초기화 한걸 저장해서 완전 초기화
        FaissWrapper.saveIndexNative(indexFile.absolutePath) // 빈 인덱스 저장
        true
    }

    override fun queryVector(query: String, topK: Int): List<VectorMatch>? = safeRunnable(this) {
        if (query.isBlank())  return@safeRunnable emptyList()

        val queryEmbedding = embedder.getEmbedding(query)

        if (queryEmbedding.size != dimension) {
            throw IllegalArgumentException("Query embedding does not match dimension $dimension")
        }

        val javaQueryVectors = queryEmbedding.toFloatArray()
        val resultsObjArray: Array<Any>? = FaissWrapper.queryVectors(javaQueryVectors, 1, topK)

        // JNI 호출 결과가 null이거나, 예상한 배열 구조가 아니면 null 또는 빈 리스트 반환
        if (resultsObjArray == null || resultsObjArray.size < 2) {
            // 또는 throw IllegalStateException("Invalid result from FaissWrapper.queryVectors")
            return@safeRunnable emptyList()
        }

        val faissIndices = resultsObjArray[0] as? LongArray
        val distances = resultsObjArray[1] as? FloatArray

        if (faissIndices == null || distances == null || faissIndices.size != distances.size) {
            // 데이터가 없거나, 인덱스와 거리 배열의 크기가 맞지 않는 경우
            // 또는 throw IllegalStateException("Mismatched indices and distances arrays from Faiss")
            return@safeRunnable emptyList()
        }

        if (faissIndices.isEmpty()) {
            return@safeRunnable emptyList() // 검색 결과가 없는 경우
        }

        val vectorMatches = mutableListOf<VectorMatch>()
        for (i in faissIndices.indices) {
            val faissInternalId: Long = faissIndices[i]
            val score: Float = distances[i]
            val actualMetadata: Map<String, String>? = metaDataStore.getMetadata(faissInternalId)

            vectorMatches.add(VectorMatch(faissInternalId, score, actualMetadata))
        }
        return@safeRunnable vectorMatches
    }

    override fun destroy() {
        return safeRunnable(this) {
            //메모리에서만 삭제 지금까지 작업한건 저장후에
            metaDataStore.deleteMetadata()
            FaissWrapper.saveIndexNative(indexFile.absolutePath)
            FaissWrapper.destroyStore()
        }
    }
}
