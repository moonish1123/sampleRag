package pe.brice.rag.library.llm.data.api.anthropic.model

data class MessageRequest(
    val model: String,
    val maxTokens: Int,
    val messages: List<Message>,
    val system: String? = null,
    val temperature: Double? = null,
    val topK: Int? = null,
    val topP: Double? = null
)

data class Message(
    val role: String,
    val content: String
)

data class MessageResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    val stopReason: String? = null
)

data class ContentBlock(
    val type: String,
    val text: String? = null
)

data class Usage(
    val inputTokens: Int,
    val outputTokens: Int
)
