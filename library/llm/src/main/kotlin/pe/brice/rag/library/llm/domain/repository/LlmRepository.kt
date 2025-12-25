package pe.brice.rag.library.llm.domain.repository

import pe.brice.rag.library.llm.domain.model.LlmRequest
import pe.brice.rag.library.llm.domain.model.LlmResponse

interface LlmRepository {
    suspend fun generateText(request: LlmRequest): Result<LlmResponse>
    suspend fun chat(prompt: String, model: String = "gpt-4"): Result<String>
}
