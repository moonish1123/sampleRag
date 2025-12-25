package pe.brice.rag.library.llm.data.datasource

import pe.brice.rag.library.llm.domain.model.LlmRequest
import pe.brice.rag.library.llm.domain.model.LlmResponse

interface LlmDataSource {
    suspend fun generateText(request: LlmRequest): Result<LlmResponse>
}
