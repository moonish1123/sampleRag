package pe.brice.rag.library.embedding.di

import android.content.Context
import org.koin.dsl.module
import pe.brice.rag.library.embedding.data.datasource.EmbeddingDataSource
import pe.brice.rag.library.embedding.data.datasource.MediaPipeEmbeddingDataSource
import pe.brice.rag.library.embedding.data.datasource.OpenAiEmbeddingDataSource
import pe.brice.rag.library.embedding.domain.repository.EmbeddingRepository
import pe.brice.rag.library.embedding.data.repository.EmbeddingRepositoryImpl

val embeddingModule = module {
    // MediaPipe Embedding DataSource (local)
    factory {
        MediaPipeEmbeddingDataSource(get())
    }

    // OpenAI Embedding DataSource (remote) using ApiClientFactory
    factory { (apiKey: String) ->
        OpenAiEmbeddingDataSource(apiKey)
    }

    // Default DataSource: MediaPipe (local, free)
    single<EmbeddingDataSource> {
        MediaPipeEmbeddingDataSource(get<Context>())
    }

    // Repository
    single<EmbeddingRepository> {
        EmbeddingRepositoryImpl(get())
    }
}
