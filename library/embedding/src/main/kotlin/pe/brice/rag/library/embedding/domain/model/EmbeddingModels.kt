package pe.brice.rag.library.embedding.domain.model

data class EmbeddingRequest(
    val text: String,
    val model: String = "text-embedding-3-small"
)

data class EmbeddingVector(
    val values: List<Float>,
    val dimension: Int,
    val model: String
)
