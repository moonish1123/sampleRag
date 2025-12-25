package pe.brice.rag.library.llm.data.datasource

import pe.brice.rag.library.llm.data.api.anthropic.AnthropicApiService
import pe.brice.rag.library.llm.data.api.anthropic.model.Message as AnthropicMessage
import pe.brice.rag.library.llm.data.api.anthropic.model.MessageRequest
import pe.brice.rag.library.llm.domain.model.LlmRequest
import pe.brice.rag.library.llm.domain.model.LlmResponse
import pe.brice.rag.library.network.http.ApiClientFactory

class AnthropicLlmDataSource(
    private val apiKey: String
) : LlmDataSource {

    private val service: AnthropicApiService by lazy {
        ApiClientFactory.create(
            domain = "https://api.anthropic.com/",
            apiKey = apiKey,
            serviceClass = AnthropicApiService::class
        )
    }

    override suspend fun generateText(request: LlmRequest): Result<LlmResponse> {
        return try {
            val maxTokens = request.maxTokens ?: 4096

            val messageRequest = MessageRequest(
                model = request.model,
                maxTokens = maxTokens,
                messages = request.messages.map { AnthropicMessage(it.role, it.content) }
            )

            val response = service.createMessage(
                apiKey,
                "2023-06-01",
                messageRequest
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val content = body.content.firstOrNull()?.text ?: ""

                Result.success(
                    LlmResponse(
                        content = content,
                        model = body.model
                    )
                )
            } else {
                Result.failure(Exception("Anthropic API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
