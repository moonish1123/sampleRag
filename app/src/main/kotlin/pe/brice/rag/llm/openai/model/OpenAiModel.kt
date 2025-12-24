package pe.brice.rag.llm.openai.model

class OpenAiModel {
}

data class EmbeddingRequest(val input: String, val model: String = "text-embedding-3-small") // 최신 모델 사용 권장
data class EmbeddingData(val embedding: List<Float>)
data class EmbeddingResponse(val data: List<EmbeddingData>)

data class ChatMessage(val role: String, val content: String)
data class ChatRequest(val model: String, val messages: List<ChatMessage>)
data class ChatChoice(val message: ChatMessage)
data class ChatResponse(val choices: List<ChatChoice>)