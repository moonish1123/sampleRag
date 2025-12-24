package pe.brice.rag.llm.openai

import pe.brice.rag.llm.openai.model.ChatRequest
import pe.brice.rag.llm.openai.model.ChatResponse
import pe.brice.rag.llm.openai.model.EmbeddingRequest
import pe.brice.rag.llm.openai.model.EmbeddingResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAiApiService {
    @POST("v1/embeddings")
    suspend fun getEmbeddings(
        @Header("Authorization") apiKey: String,
        @Body request: EmbeddingRequest
    ): Response<EmbeddingResponse>

    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}