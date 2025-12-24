package pe.brice.rag.llm.openai

import pe.brice.rag.llm.Embedding

class OpenAiEmbedding: Embedding {
    override suspend fun getEmbedding(input: String): List<Float> = OpenAiApiClient.embedding(input, this@OpenAiEmbedding)

    override fun getDimensions(): Int = 1536 // chatgpt text-embedding-3-small 모델의 차원 수

    override fun getModelName(): String = "text-embedding-3-small" // 최신 모델 사용 권장

    override fun close() {}
}

