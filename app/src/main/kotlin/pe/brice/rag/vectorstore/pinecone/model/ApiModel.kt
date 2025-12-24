package pe.brice.rag.vectorstore.pinecone.model

// 벡터 데이터 포인트 (임베딩된 결과물)
data class VectorData(
    val id: String,
    val values: List<Float>, // 임베딩된 벡터 값
    val metadata: Map<String, String>? = null // 추가 메타데이터 (선택 사항)
)

// Upsert 요청 바디
data class UpsertRequest(
    val vectors: List<VectorData>,
    val namespace: String? = null // 네임스페이스 (선택 사항)
)

// Fetch 응답
data class FetchResponse(
    val vectors: Map<String, VectorData>
)

// Delete 요청 바디
data class DeleteRequest(
    val ids: List<String>? = null, // 삭제할 벡터 ID 목록
    val deleteAll: Boolean? = null, // 네임스페이스 내 모든 벡터 삭제 여부
    val namespace: String? = null,
    val filter: Map<String, Any>? = null // 메타데이터 필터 (선택 사항)
)

// Query 요청 바디
data class QueryRequest(
    val vector: List<Float>? = null, // 쿼리할 임베딩 벡터
    val id: String? = null, // 쿼리할 벡터의 ID (id로 쿼리 시 vector는 null)
    val topK: Int, // 가져올 유사 벡터의 수
    val namespace: String? = null,
    val filter: Map<String, Any>? = null, // 메타데이터 필터
    val includeValues: Boolean? = null, // 결과에 벡터 값 포함 여부
    val includeMetadata: Boolean? = null // 결과에 메타데이터 포함 여부
)

// Query 응답 (Match)
data class Match(
    val id: String,
    val score: Float,
    val values: List<Float>? = null,
    val metadata: Map<String, String>? = null
)

// Query 응답
data class QueryResponse(
    val matches: List<Match>,
    val namespace: String? = null
)