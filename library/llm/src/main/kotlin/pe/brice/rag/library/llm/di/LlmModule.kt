package pe.brice.rag.library.llm.di

import org.koin.dsl.module
import pe.brice.rag.library.llm.data.datasource.LlmDataSource
import pe.brice.rag.library.llm.data.datasource.AnthropicLlmDataSource
import pe.brice.rag.library.llm.data.datasource.OpenAiLlmDataSource
import pe.brice.rag.library.llm.domain.repository.LlmRepository
import pe.brice.rag.library.llm.data.repository.LlmRepositoryImpl

val llmModule = module {
    // DataSources with ApiClientFactory
    factory { (apiKey: String, provider: String) ->
        when (provider.lowercase()) {
            "openai" -> OpenAiLlmDataSource(apiKey)
            "anthropic", "claude" -> AnthropicLlmDataSource(apiKey)
            else -> throw IllegalArgumentException("Unknown LLM provider: $provider")
        }
    }

    // Repository (default: OpenAI)
    single<LlmRepository> {
        val apiKey = "" // Should be injected from app config
        val dataSource: LlmDataSource = OpenAiLlmDataSource(apiKey)
        LlmRepositoryImpl(dataSource)
    }
}
