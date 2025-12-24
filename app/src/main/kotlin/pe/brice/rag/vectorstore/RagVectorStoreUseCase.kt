package pe.brice.rag.vectorstore

import pe.brice.rag.common.utils.DateUtils
import pe.brice.rag.legacy.model.SMessage

class RagVectorStoreUseCase(val vectorStore: RagVectorStore) {

    /**
     * 벡터 데이터를 저장소에 추가합니다.
     * @param doc 벡터로 변환할 문서 내용
     * @param metadata 관련 메타데이터
     * @return 작업 성공 여부
     */
    fun addVector(message: SMessage, chunk: String): Long {
        val metadata = mutableMapOf<String, String>()
        metadata["id"] = message.id.toString()
        metadata["account_id"] = message.accountId.toString()
        metadata["subject"] = message.subject ?: ""
        metadata["receivedDate"] = DateUtils.convertDateString(message.receivedDate)
        metadata["chunk"] = chunk
        return vectorStore.addVector("$chunk in ${metadata["subject"]} at ${metadata["receivedDate"]}", metadata)
    }

    /**
     * 특정 ID의 벡터를 삭제합니다.
     * @param id 삭제할 벡터의 ID
     * @return 작업 성공 여부
     */
    fun deleteVector(id: Long): Boolean {
        return vectorStore.deleteVector(id)
    }

    /**
     * 모든 벡터를 삭제합니다.
     * @return 작업 성공 여부
     */
    fun deleteAllVectors(): Boolean {
        return vectorStore.deleteAllVectors()
    }

    /**
     * 쿼리 벡터와 가장 유사한 벡터들을 찾습니다.
     * @param query 쿼리 문자열
     * @param topK 반환할 최대 결과 수
     * @return 유사도 기준으로 정렬된 결과 목록 (ID, 유사도 점수, 메타데이터 포함)
     */
    fun queryVector(query: String, topK: Int = 3): List<VectorMatch>? {
        return vectorStore.queryVector(query, topK)
    }

    fun destroy() {
        vectorStore.destroy()
    }
}
