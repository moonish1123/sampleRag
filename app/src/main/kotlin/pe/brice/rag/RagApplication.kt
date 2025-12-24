package pe.brice.rag

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import pe.brice.rag.account.Account
import pe.brice.rag.account.AccountManager
import pe.brice.rag.data.SampleDataInitializer
import pe.brice.rag.legacy.util.LogUtils

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
