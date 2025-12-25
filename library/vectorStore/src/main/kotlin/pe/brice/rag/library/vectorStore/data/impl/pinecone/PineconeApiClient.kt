package pe.brice.rag.library.vectorStore.data.impl.pinecone

import pe.brice.rag.library.network.http.BaseApiClient

class PineconeApiClient(
    baseUrl: String,
    private val apiKey: String,
    enableLogging: Boolean = false
) : BaseApiClient(baseUrl, enableLogging) {

    val service: PineconeService by lazy {
        createService(PineconeService::class.java)
    }

    fun getApiKeyHeader(): String = apiKey
}
