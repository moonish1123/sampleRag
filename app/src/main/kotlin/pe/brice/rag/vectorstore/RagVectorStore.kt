package pe.brice.rag.vectorstore

interface RagVectorStore {
    /**
     * 벡터 데이터를 저장소에 추가합니다.
     * @param id 벡터 식별자
     * @param embedding 임베딩 벡터
     * @param metadata 관련 메타데이터
     * @return 작업 성공 여부
     */
    fun addVector(doc: String, metadata: Map<String, String>): Long

    /**
     * 특정 ID의 벡터를 삭제합니다.
     * @param id 삭제할 벡터의 ID
     * @return 작업 성공 여부
     */
    fun deleteVector(id: Long): Boolean

    /**
     * 모든 벡터를 삭제합니다.
     * @return 작업 성공 여부
     */
    fun deleteAllVectors(): Boolean

    /**
     * 쿼리 벡터와 가장 유사한 벡터들을 찾습니다.
     * @param queryEmbedding 쿼리 임베딩 벡터
     * @param topK 반환할 최대 결과 수
     * @return 유사도 기준으로 정렬된 결과 목록 (ID, 유사도 점수, 메타데이터 포함)
     */
    fun queryVector(query: String, topK: Int = 3): List<VectorMatch>?

    fun destroy()
}

/**
 * 벡터 검색 결과를 나타내는 데이터 클래스
 * @property id 벡터 ID
 * @property score 유사도 점수
 * @property metadata 벡터와 연관된 메타데이터
 */
data class VectorMatch(
    val id: Long,
    val score: Float,
    val metadata: Map<String, String>? = null
)
