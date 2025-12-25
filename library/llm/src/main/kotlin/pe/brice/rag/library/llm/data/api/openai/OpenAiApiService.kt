package pe.brice.rag.library.llm.data.api.openai

import pe.brice.rag.library.llm.data.api.openai.model.ChatRequest
import pe.brice.rag.library.llm.data.api.openai.model.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAiApiService {
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
