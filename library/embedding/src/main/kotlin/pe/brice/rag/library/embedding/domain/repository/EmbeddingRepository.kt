package pe.brice.rag.library.embedding.domain.repository

import pe.brice.rag.library.embedding.domain.model.EmbeddingRequest
import pe.brice.rag.library.embedding.domain.model.EmbeddingVector

interface EmbeddingRepository {
    suspend fun embed(request: EmbeddingRequest): Result<EmbeddingVector>
    suspend fun embedText(text: String, model: String = "text-embedding-3-small"): Result<List<Float>>
}
