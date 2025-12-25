package pe.brice.rag

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import pe.brice.rag.account.Account
import pe.brice.rag.account.AccountManager
import pe.brice.rag.data.SampleDataInitializer
import pe.brice.rag.legacy.util.LogUtils
import pe.brice.rag.library.embedding.di.embeddingModule
import pe.brice.rag.library.faissNative.di.faissNativeModule
import pe.brice.rag.library.llm.di.llmModule
import pe.brice.rag.library.network.di.networkModule
import pe.brice.rag.library.splitter.di.splitterModule
import pe.brice.rag.library.vectorStore.di.vectorStoreModule

class RagApplication : Application() {

    companion object {
        private lateinit var instance: RagApplication

        fun getInstance(): RagApplication = instance

        fun getString(resId: Int): String = instance.getString(resId)
    }

    private lateinit var sampleDataInitializer: SampleDataInitializer
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Koin DI with all modules
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@RagApplication)
            modules(
                networkModule,
                faissNativeModule,
                vectorStoreModule,
                llmModule,
                embeddingModule,
                splitterModule
            )
        }

        // Initialize sample data initializer
        sampleDataInitializer = SampleDataInitializer(this)

        // Seed a demo account so RagService has something to reference out of the box.
        AccountManager.instance.registerAccount(
            Account(id = 1L, email = "demo@rag.test")
        )

        // Initialize solar system sample data asynchronously
        initializeSampleData()
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
    }

    private fun initializeSampleData() {
        applicationScope.launch {
            try {
                LogUtils.i("RagApplication", "Starting solar system data initialization...")
                sampleDataInitializer.initializeIfNeeded()
                LogUtils.i("RagApplication", "Solar system data initialization completed")
            } catch (e: Exception) {
                LogUtils.e("RagApplication", "Failed to initialize solar system data: ${e.message}", e)
            }
        }
    }
}
