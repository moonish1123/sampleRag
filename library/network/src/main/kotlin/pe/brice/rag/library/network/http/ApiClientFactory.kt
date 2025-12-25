package pe.brice.rag.library.network.http

import kotlin.reflect.KClass
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Factory for creating API clients with common configuration
 *
 * Usage:
 * ```
 * val openAiService = ApiClientFactory.create(
 *     domain = "https://api.openai.com/",
 *     apiKey = "sk-...",
 *     serviceClass = OpenAiApiService::class
 * )
 * ```
 */
object ApiClientFactory {

    /**
     * Create an API service instance
     *
     * @param domain Base URL for the API (e.g., "https://api.openai.com/")
     * @param apiKey API key for authentication
     * @param serviceClass Retrofit service interface class
     * @param enableLogging Enable HTTP logging for debugging
     * @return Configured API service instance
     */
    fun <T : Any> create(
        domain: String,
        apiKey: String,
        serviceClass: KClass<T>,
        enableLogging: Boolean = false
    ): T {
        val httpClient = HttpClientFactory.createOkHttp(enableLogging, true)

        val retrofit = Retrofit.Builder()
            .baseUrl(domain)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(serviceClass.java)
    }

    /**
     * Create an API service instance with custom header
     *
     * @param domain Base URL for the API
     * @param headerName Header name (e.g., "Authorization", "x-api-key")
     * @param headerValue Header value (e.g., "Bearer {apiKey}", "{apiKey}")
     * @param serviceClass Retrofit service interface class
     * @param enableLogging Enable HTTP logging for debugging
     * @return Configured API service instance
     */
    fun <T : Any> createWithHeader(
        domain: String,
        headerName: String,
        headerValue: String,
        serviceClass: KClass<T>,
        enableLogging: Boolean = false
    ): T {
        val httpClient = HttpClientFactory.createOkHttp(enableLogging, true)

        val retrofit = Retrofit.Builder()
            .baseUrl(domain)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(serviceClass.java)
    }
}
