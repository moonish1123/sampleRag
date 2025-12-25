package pe.brice.rag.library.embedding.data.datasource

import pe.brice.rag.library.embedding.data.datasource.EmbeddingDataSource
import pe.brice.rag.library.embedding.data.api.openai.OpenAiApiService
import pe.brice.rag.library.embedding.data.api.openai.model.EmbeddingRequest as OpenAiEmbeddingRequest
import pe.brice.rag.library.embedding.domain.model.EmbeddingRequest
import pe.brice.rag.library.embedding.domain.model.EmbeddingVector
import pe.brice.rag.library.network.http.ApiClientFactory

class OpenAiEmbeddingDataSource(
    private val apiKey: String
) : EmbeddingDataSource {

    private val service: OpenAiApiService by lazy {
        ApiClientFactory.create(
            domain = "https://api.openai.com/",
            apiKey = apiKey,
            serviceClass = OpenAiApiService::class
        )
    }

    override suspend fun embed(request: EmbeddingRequest): Result<EmbeddingVector> {
        return try {
            val openAiRequest = OpenAiEmbeddingRequest(
                input = request.text,
                model = request.model
            )

            val response = service.getEmbeddings(
                "Bearer $apiKey",
                openAiRequest
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val embedding = body.data.firstOrNull()?.embedding ?: emptyList()

                Result.success(
                    EmbeddingVector(
                        values = embedding,
                        dimension = embedding.size,
                        model = request.model
                    )
                )
            } else {
                Result.failure(Exception("OpenAI Embedding API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
