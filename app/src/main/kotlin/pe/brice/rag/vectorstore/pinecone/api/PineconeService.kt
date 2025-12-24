package pe.brice.rag.vectorstore.pinecone.api

import pe.brice.rag.vectorstore.pinecone.model.DeleteRequest
import pe.brice.rag.vectorstore.pinecone.model.FetchResponse
import pe.brice.rag.vectorstore.pinecone.model.QueryRequest
import pe.brice.rag.vectorstore.pinecone.model.QueryResponse
import pe.brice.rag.vectorstore.pinecone.model.UpsertRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface PineconeService {
    // 벡터 Upsert (Create/Update)
    @Headers("Content-Type: application/json")
    @POST("vectors/upsert")
    suspend fun upsertVectors(
        @Header("Api-Key") apiKey: String,
        @Body request: UpsertRequest
    ): Response<Unit> // Pinecone upsert는 응답 바디가 비어있으므로 Unit

    // 벡터 Fetch (Read by ID)
    @GET("vectors/fetch")
    suspend fun fetchVectors(
        @Header("Api-Key") apiKey: String,
        @Query("ids") ids: List<String>,
        @Query("namespace") namespace: String? = null
    ): Response<FetchResponse>

    // 벡터 Delete
    @Headers("Content-Type: application/json")
    @POST("vectors/delete")
    suspend fun deleteVectors(
        @Header("Api-Key") apiKey: String,
        @Body request: DeleteRequest
    ): Response<Unit>

    // 벡터 Query (Similarity Search)
    @Headers("Content-Type: application/json")
    @POST("query")
    suspend fun queryVectors(
        @Header("Api-Key") apiKey: String,
        @Body request: QueryRequest
    ): Response<QueryResponse>
}