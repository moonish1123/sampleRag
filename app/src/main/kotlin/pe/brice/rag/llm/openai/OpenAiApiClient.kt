package pe.brice.rag.llm.openai

import pe.brice.rag.RagApplication
import pe.brice.rag.R
import pe.brice.rag.common.network.http.HttpClientFactory
import pe.brice.rag.llm.Embedding
import pe.brice.rag.llm.openai.model.ChatMessage
import pe.brice.rag.llm.openai.model.ChatRequest
import pe.brice.rag.llm.openai.model.EmbeddingRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OpenAiApiClient {
    private const val BASE_URL = "https://api.openai.com/"

    val apiKey: String by lazy { getApikey() }
    val instance: OpenAiApiService by lazy {
        val httpClient =HttpClientFactory.createOkHttp(true, true)

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(OpenAiApiService::class.java)
    }

    private fun getApikey(): String {
        return "Bearer ${RagApplication.getString(R.string.openai_api_key)}"
    }

    suspend fun prompt(prompt: String): String {
        val request = ChatRequest(
            model = "gpt-4.1-mini",
            messages = listOf(
                ChatMessage(role = "user", content = prompt)
            )
        )

        val response = instance.getChatCompletion(apiKey, request)

        return if (response.isSuccessful) {
            response.body()?.choices?.firstOrNull()?.message?.content ?: ""
        } else {
            "Error: ${response.errorBody()?.string()}"
        }
    }

    suspend fun embedding(prompt: String, embedding: Embedding): List<Float> {
        val request = EmbeddingRequest(
            input = prompt,
            model = embedding.getModelName()
        )

        val response = instance.getEmbeddings(apiKey, request)

        return if (response.isSuccessful) {
            response.body()?.data?.firstOrNull()?.embedding ?: emptyList()
        } else {
            emptyList()
        }
    }
}
