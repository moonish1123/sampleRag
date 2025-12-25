package pe.brice.rag.library.vectorStore.domain.repository

import pe.brice.rag.library.vectorStore.domain.model.Document
import pe.brice.rag.library.vectorStore.domain.model.SearchResult
import pe.brice.rag.library.vectorStore.domain.model.Vector

interface VectorStoreRepository {
    suspend fun addVectors(vectors: List<Vector>): Result<Unit>
    suspend fun search(queryVector: List<Float>, topK: Int = 5): Result<List<SearchResult>>
    suspend fun deleteById(id: String): Result<Unit>
    suspend fun deleteAll(): Result<Unit>
    suspend fun getById(id: String): Result<Document?>
    suspend fun saveIndex(path: String): Result<Unit>
    suspend fun loadIndex(path: String): Result<Unit>
}
