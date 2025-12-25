package pe.brice.rag.library.llm.data.datasource

import pe.brice.rag.library.llm.data.api.openai.OpenAiApiService
import pe.brice.rag.library.llm.data.api.openai.model.ChatMessage
import pe.brice.rag.library.llm.data.api.openai.model.ChatRequest
import pe.brice.rag.library.llm.domain.model.LlmRequest
import pe.brice.rag.library.llm.domain.model.LlmResponse
import pe.brice.rag.library.network.http.ApiClientFactory

class OpenAiLlmDataSource(
    private val apiKey: String
) : LlmDataSource {

    private val service: OpenAiApiService by lazy {
        ApiClientFactory.create(
            domain = "https://api.openai.com/",
            apiKey = apiKey,
            serviceClass = OpenAiApiService::class
        )
    }

    override suspend fun generateText(request: LlmRequest): Result<LlmResponse> {
        return try {
            val chatRequest = ChatRequest(
                model = request.model,
                messages = request.messages.map { ChatMessage(it.role, it.content) }
            )

            val response = service.getChatCompletion(
                "Bearer $apiKey",
                chatRequest
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val content = body.choices.firstOrNull()?.message?.content ?: ""

                Result.success(
                    LlmResponse(
                        content = content,
                        model = request.model,
                        usage = null
                    )
                )
            } else {
                Result.failure(Exception("OpenAI API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
