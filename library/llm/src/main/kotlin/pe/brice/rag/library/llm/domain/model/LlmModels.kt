package pe.brice.rag.library.llm.domain.model

data class Message(
    val role: String,
    val content: String
)

data class LlmRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double? = null,
    val maxTokens: Int? = null
)

data class LlmResponse(
    val content: String,
    val model: String,
    val usage: Usage? = null
)

data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)
