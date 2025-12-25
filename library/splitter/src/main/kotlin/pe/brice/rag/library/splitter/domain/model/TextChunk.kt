package pe.brice.rag.library.splitter.domain.model

data class TextChunk(
    val text: String,
    val index: Int,
    val metadata: Map<String, Any> = emptyMap()
)
