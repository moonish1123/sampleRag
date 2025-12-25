package pe.brice.rag.library.vectorStore.di

import android.content.Context
import org.koin.dsl.module
import pe.brice.rag.library.vectorStore.domain.model.VectorStoreType
import pe.brice.rag.library.vectorStore.data.datasource.VectorStoreDataSource
import pe.brice.rag.library.vectorStore.data.impl.FaissDataSource
import pe.brice.rag.library.vectorStore.data.impl.PineconeDataSource
import pe.brice.rag.library.vectorStore.data.repository.VectorStoreRepositoryImpl
import pe.brice.rag.library.vectorStore.domain.repository.VectorStoreRepository

val vectorStoreModule = module {
    // Default: Faiss DataSource
    single<VectorStoreDataSource> {
        FaissDataSource(get(), "faiss_index.bin", 384)
    }

    // Pinecone DataSource (injected when needed with parameters)
    factory { (baseUrl: String, apiKey: String) ->
        PineconeDataSource(baseUrl, apiKey, "default", false)
    }

    // Repository with default Faiss
    single<VectorStoreRepository> {
        VectorStoreRepositoryImpl(get())
    }

    // Repository with specific DataSource type
    factory { (storeType: VectorStoreType) ->
        val dataSource: VectorStoreDataSource = when (storeType) {
            VectorStoreType.FAISS -> {
                FaissDataSource(get(), "faiss_index.bin", 384)
            }
            VectorStoreType.PINECONE -> {
                // For Pinecone, use get() with parameters - this requires a more complex setup
                // For now, default to Faiss when parameters aren't provided
                FaissDataSource(get(), "faiss_index.bin", 384)
            }
        }
        VectorStoreRepositoryImpl(dataSource)
    }
}
