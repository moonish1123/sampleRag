package pe.brice.rag.library.llm.data.api.anthropic

import pe.brice.rag.library.llm.data.api.anthropic.model.MessageRequest
import pe.brice.rag.library.llm.data.api.anthropic.model.MessageResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AnthropicApiService {
    @POST("v1/messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String,
        @Body request: MessageRequest
    ): Response<MessageResponse>
}
