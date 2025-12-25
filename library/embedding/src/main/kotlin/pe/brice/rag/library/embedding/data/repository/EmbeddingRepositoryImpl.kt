package pe.brice.rag.library.embedding.data.repository

import pe.brice.rag.library.embedding.data.datasource.EmbeddingDataSource
import pe.brice.rag.library.embedding.domain.model.EmbeddingRequest
import pe.brice.rag.library.embedding.domain.model.EmbeddingVector
import pe.brice.rag.library.embedding.domain.repository.EmbeddingRepository

class EmbeddingRepositoryImpl(
    private val dataSource: EmbeddingDataSource
) : EmbeddingRepository {
    override suspend fun embed(request: EmbeddingRequest): Result<EmbeddingVector> {
        return dataSource.embed(request)
    }

    override suspend fun embedText(text: String, model: String): Result<List<Float>> {
        val request = EmbeddingRequest(text = text, model = model)
        return embed(request).map { it.values }
    }
}
