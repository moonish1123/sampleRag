package pe.brice.rag.vectorstore.pinecone.api

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import pe.brice.rag.RagApplication
import pe.brice.rag.R
import pe.brice.rag.common.ifFalse
import pe.brice.rag.vectorstore.pinecone.model.DeleteRequest
import pe.brice.rag.vectorstore.pinecone.model.FetchResponse
import pe.brice.rag.vectorstore.pinecone.model.QueryRequest
import pe.brice.rag.vectorstore.pinecone.model.QueryResponse
import pe.brice.rag.vectorstore.pinecone.model.UpsertRequest
import pe.brice.rag.vectorstore.pinecone.model.VectorData
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object PineconeApiClient {
    private const val TAG = "PineconeApiClient"
    private const val BASE_URL = "https://brice-langchain-test-rr49kxj.svc.aped-4627-b74a.pinecone.io"
    private var API_KEYS = RagApplication.getString(R.string.pinecone_api_key)
    private const val REGION = "us-east-1"
    private const val NAME_SPACE =  "daum-email"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // 네트워크 요청/응답 로그 확인용 (디버그 시 유용)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // 로깅 인터셉터 추가
        .connectTimeout(30, TimeUnit.SECONDS) // 연결 타임아웃
        .readTimeout(30, TimeUnit.SECONDS) // 읽기 타임아웃
        .build()

    private val pineconeService: PineconeService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PineconeService::class.java)
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Gemini, ChatGPT 등 다양한 Embedding 결과에 대응하기 위한 함수
    fun upsert(id: Long, embedding: List<Float>, metadata: Map<String, String>): Long = runBlocking {
        processExceptionOrError(
            process = {
                val vectorData = VectorData(id.toString(), embedding, metadata)
                val request = UpsertRequest(
                    vectors = listOf(vectorData),
                    namespace = NAME_SPACE
                )
                val response = pineconeService.upsertVectors(API_KEYS, request)
                response.isSuccessful.ifFalse { Log.e(TAG, "Upsert request failed: ${response.code()} ${response.errorBody()?.string()}") }
                id
            },
            errorProcess = { t ->
                Log.e(TAG, "An unexpected error occurred: ${t.message}")
            }
        ) ?: -1
    }

    fun fetchVectors(id: Long): Deferred<FetchResponse?> = coroutineScope.async {
        processExceptionOrError(
            process = {
                val response = pineconeService.fetchVectors(
                    apiKey = API_KEYS,
                    ids = listOf(id.toString()),
                    namespace = NAME_SPACE
                )

                if (response.isSuccessful) {
                    response.body()
                } else {
                    Log.e(TAG, "Fetch failed: ${response.code()} ${response.errorBody()?.string()}")
                    null
                }
            },
            errorProcess = { t ->
                Log.e(TAG, "An unexpected error occurred: ${t.message}")
            }
        )
    }

    fun queryVectors(queryEmbedding: List<Float>, topK: Int = 3): QueryResponse? = runBlocking {
        processExceptionOrError(
            process = {
                val request = QueryRequest(
                    vector = queryEmbedding,
                    topK = topK,
                    namespace = NAME_SPACE,
                    includeValues = false, // 벡터 값 포함 여부
                    includeMetadata = true // 메타데이터 포함 여부
                )
                val response = pineconeService.queryVectors(API_KEYS, request)
                if (response.isSuccessful) {
                    response.body()
                } else {
                    Log.e(TAG, "Query failed: ${response.code()} ${response.errorBody()?.string()}")
                    null
                }
            },
            errorProcess = { t ->
                Log.e(TAG, "An unexpected error occurred: ${t.message}")
            }
        )
    }

    fun deleteAllVectors() = runBlocking {
        deleteInternal(DeleteRequest(deleteAll = true, namespace = NAME_SPACE))
    }

    fun deleteVectors(id: Long) = runBlocking {
        deleteInternal(DeleteRequest(ids = listOf(id.toString()), namespace = NAME_SPACE))
    }

    private suspend fun deleteInternal(request: DeleteRequest): Boolean {
        return processExceptionOrError(
            process = {
                val response = pineconeService.deleteVectors(apiKey = API_KEYS, request = request)
                response.isSuccessful.ifFalse { Log.e(TAG, "Delete request failed: ${response.code()} ${response.errorBody()?.string()}") }
                true
            },
            errorProcess = { t ->
                Log.e(TAG, "An unexpected error occurred: ${t.message}")
            }
        ) == true
    }

    private suspend fun <T> processExceptionOrError(process: suspend () -> T, errorProcess: (Throwable) -> Unit = {}): T? {
        return try {
            process()
        } catch (e: Exception) {
            errorProcess(e)
            null
        }
    }
}
