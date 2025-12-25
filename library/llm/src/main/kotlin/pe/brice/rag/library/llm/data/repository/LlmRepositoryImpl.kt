package pe.brice.rag.library.llm.data.repository

import pe.brice.rag.library.llm.data.datasource.LlmDataSource
import pe.brice.rag.library.llm.domain.model.LlmRequest
import pe.brice.rag.library.llm.domain.model.LlmResponse
import pe.brice.rag.library.llm.domain.repository.LlmRepository

class LlmRepositoryImpl(
    private val dataSource: LlmDataSource
) : LlmRepository {
    override suspend fun generateText(request: LlmRequest): Result<LlmResponse> {
        return dataSource.generateText(request)
    }

    override suspend fun chat(prompt: String, model: String): Result<String> {
        val request = LlmRequest(
            model = model,
            messages = listOf(pe.brice.rag.library.llm.domain.model.Message(role = "user", content = prompt))
        )
        return generateText(request).map { it.content }
    }
}
