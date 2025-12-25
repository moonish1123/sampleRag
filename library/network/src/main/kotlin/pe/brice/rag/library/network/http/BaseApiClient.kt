package pe.brice.rag.library.network.http

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

abstract class BaseApiClient(
    private val baseUrl: String,
    private val enableLogging: Boolean = false
) {
    protected fun <T> createService(serviceClass: Class<T>): T {
        val httpClient = HttpClientFactory.createOkHttp(enableLogging, true)

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(serviceClass)
    }
}
