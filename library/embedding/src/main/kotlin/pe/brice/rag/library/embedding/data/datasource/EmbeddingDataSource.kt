package pe.brice.rag.library.embedding.data.datasource

import pe.brice.rag.library.embedding.domain.model.EmbeddingRequest
import pe.brice.rag.library.embedding.domain.model.EmbeddingVector

interface EmbeddingDataSource {
    suspend fun embed(request: EmbeddingRequest): Result<EmbeddingVector>
}
