package pe.brice.rag.library.vectorStore.domain.model

data class Document(
    val id: String,
    val content: String,
    val metadata: Map<String, String> = emptyMap()
)

data class Vector(
    val id: String,
    val values: List<Float>,
    val metadata: Map<String, String> = emptyMap()
)

data class SearchResult(
    val document: Document,
    val score: Float
)
