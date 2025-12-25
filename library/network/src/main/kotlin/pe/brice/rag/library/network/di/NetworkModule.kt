package pe.brice.rag.library.network.di

import org.koin.dsl.module

// Network module provides base HTTP infrastructure (BaseApiClient, HttpClientFactory)
// OpenAI/Anthropic clients are now in their respective modules (llm, embedding)
val networkModule = module {
    // Base HTTP infrastructure only
}
