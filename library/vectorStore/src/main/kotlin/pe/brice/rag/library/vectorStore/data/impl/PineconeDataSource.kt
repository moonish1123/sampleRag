package pe.brice.rag.library.vectorStore.data.impl

import pe.brice.rag.library.vectorStore.data.datasource.VectorStoreDataSource
import pe.brice.rag.library.vectorStore.data.impl.pinecone.PineconeApiClient
import pe.brice.rag.library.vectorStore.data.impl.pinecone.model.*
import pe.brice.rag.library.vectorStore.domain.model.Document
import pe.brice.rag.library.vectorStore.domain.model.SearchResult
import pe.brice.rag.library.vectorStore.domain.model.Vector

class PineconeDataSource(
    private val baseUrl: String,
    private val apiKey: String,
    private val namespace: String = "default",
    private val enableLogging: Boolean = false
) : VectorStoreDataSource {

    private val client = PineconeApiClient(baseUrl, apiKey, enableLogging)

    override suspend fun addVectors(vectors: List<Vector>): Result<Unit> {
        return try {
            val pineconeVectors = vectors.map { vector ->
                VectorData(
                    id = vector.id,
                    values = vector.values,
                    metadata = vector.metadata
                )
            }

            val request = UpsertRequest(
                vectors = pineconeVectors,
                namespace = namespace
            )

            val response = client.service.upsertVectors(client.getApiKeyHeader(), request)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Pinecone upsert failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun search(queryVector: List<Float>, topK: Int): Result<List<SearchResult>> {
        return try {
            val request = QueryRequest(
                vector = queryVector,
                topK = topK,
                namespace = namespace,
                includeValues = false,
                includeMetadata = true
            )

            val response = client.service.queryVectors(client.getApiKeyHeader(), request)

            if (response.isSuccessful && response.body() != null) {
                val matches = response.body()!!.matches.map { match ->
                    SearchResult(
                        document = Document(
                            id = match.id,
                            content = match.metadata?.get("content") ?: "",
                            metadata = match.metadata ?: emptyMap()
                        ),
                        score = match.score
                    )
                }
                Result.success(matches)
            } else {
                Result.failure(Exception("Pinecone query failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteById(id: String): Result<Unit> {
        return try {
            val request = DeleteRequest(
                ids = listOf(id),
                namespace = namespace
            )

            val response = client.service.deleteVectors(client.getApiKeyHeader(), request)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Pinecone delete failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAll(): Result<Unit> {
        return try {
            val request = DeleteRequest(
                deleteAll = true,
                namespace = namespace
            )

            val response = client.service.deleteVectors(client.getApiKeyHeader(), request)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Pinecone delete all failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getById(id: String): Result<Document?> {
        return try {
            val response = client.service.fetchVectors(
                apiKey = client.getApiKeyHeader(),
                ids = listOf(id),
                namespace = namespace
            )

            if (response.isSuccessful && response.body() != null) {
                val vectorMap = response.body()!!.vectors
                val vector = vectorMap[id]

                if (vector != null) {
                    Result.success(
                        Document(
                            id = vector.id,
                            content = vector.metadata?.get("content") ?: "",
                            metadata = vector.metadata ?: emptyMap()
                        )
                    )
                } else {
                    Result.success(null)
                }
            } else {
                Result.failure(Exception("Pinecone fetch failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveIndex(path: String): Result<Unit> {
        // Pinecone doesn't support saving index to local path
        return Result.failure(UnsupportedOperationException("Pinecone doesn't support local index saving"))
    }

    override suspend fun loadIndex(path: String): Result<Unit> {
        // Pinecone doesn't support loading index from local path
        return Result.failure(UnsupportedOperationException("Pinecone doesn't support local index loading"))
    }
}
