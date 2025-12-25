package pe.brice.rag.library.embedding.data.api.openai

import pe.brice.rag.library.embedding.data.api.openai.model.EmbeddingRequest
import pe.brice.rag.library.embedding.data.api.openai.model.EmbeddingResponse
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
}
