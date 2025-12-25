package pe.brice.rag.library.vectorStore.data.repository

import pe.brice.rag.library.vectorStore.data.datasource.VectorStoreDataSource
import pe.brice.rag.library.vectorStore.domain.model.Document
import pe.brice.rag.library.vectorStore.domain.model.SearchResult
import pe.brice.rag.library.vectorStore.domain.model.Vector
import pe.brice.rag.library.vectorStore.domain.repository.VectorStoreRepository

class VectorStoreRepositoryImpl(
    private val dataSource: VectorStoreDataSource
) : VectorStoreRepository {
    override suspend fun addVectors(vectors: List<Vector>): Result<Unit> {
        return dataSource.addVectors(vectors)
    }

    override suspend fun search(queryVector: List<Float>, topK: Int): Result<List<SearchResult>> {
        return dataSource.search(queryVector, topK)
    }

    override suspend fun deleteById(id: String): Result<Unit> {
        return dataSource.deleteById(id)
    }

    override suspend fun deleteAll(): Result<Unit> {
        return dataSource.deleteAll()
    }

    override suspend fun getById(id: String): Result<Document?> {
        return dataSource.getById(id)
    }

    override suspend fun saveIndex(path: String): Result<Unit> {
        return dataSource.saveIndex(path)
    }

    override suspend fun loadIndex(path: String): Result<Unit> {
        return dataSource.loadIndex(path)
    }
}
