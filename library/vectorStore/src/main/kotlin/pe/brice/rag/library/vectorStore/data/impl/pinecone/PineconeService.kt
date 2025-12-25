package pe.brice.rag.library.vectorStore.data.impl.pinecone

import pe.brice.rag.library.vectorStore.data.impl.pinecone.model.DeleteRequest
import pe.brice.rag.library.vectorStore.data.impl.pinecone.model.FetchResponse
import pe.brice.rag.library.vectorStore.data.impl.pinecone.model.QueryRequest
import pe.brice.rag.library.vectorStore.data.impl.pinecone.model.QueryResponse
import pe.brice.rag.library.vectorStore.data.impl.pinecone.model.UpsertRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface PineconeService {
    @Headers("Content-Type: application/json")
    @POST("vectors/upsert")
    suspend fun upsertVectors(
        @Header("Api-Key") apiKey: String,
        @Body request: UpsertRequest
    ): Response<Unit>

    @GET("vectors/fetch")
    suspend fun fetchVectors(
        @Header("Api-Key") apiKey: String,
        @Query("ids") ids: List<String>,
        @Query("namespace") namespace: String? = null
    ): Response<FetchResponse>

    @Headers("Content-Type: application/json")
    @POST("vectors/delete")
    suspend fun deleteVectors(
        @Header("Api-Key") apiKey: String,
        @Body request: DeleteRequest
    ): Response<Unit>

    @Headers("Content-Type: application/json")
    @POST("query")
    suspend fun queryVectors(
        @Header("Api-Key") apiKey: String,
        @Body request: QueryRequest
    ): Response<QueryResponse>
}
