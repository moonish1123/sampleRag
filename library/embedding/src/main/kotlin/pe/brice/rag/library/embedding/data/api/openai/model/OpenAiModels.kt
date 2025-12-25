package pe.brice.rag.library.embedding.data.api.openai.model

data class EmbeddingRequest(val input: String, val model: String = "text-embedding-3-small")
data class EmbeddingData(val embedding: List<Float>)
data class EmbeddingResponse(val data: List<EmbeddingData>)
