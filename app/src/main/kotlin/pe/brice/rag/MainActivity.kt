package pe.brice.rag

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pe.brice.rag.databinding.ActivityMainBinding
import pe.brice.rag.vectorstore.faiss.FaissWrapper
import pe.brice.rag.legacy.util.LogUtils
import pe.brice.rag.RagService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.runSmokeTestButton.setOnClickListener {
            binding.runSmokeTestButton.isEnabled = false
            binding.resultText.text = getString(R.string.faiss_running_message)

            val report = runFaissSmokeTest()
            binding.resultText.text = report

            binding.runSmokeTestButton.isEnabled = true
        }

        binding.testRagQueryButton.setOnClickListener {
            val query = binding.queryInput.text.toString().trim()
            if (query.isBlank()) {
                binding.resultText.text = "질문을 입력해주세요."
                return@setOnClickListener
            }

            testRagQuery(query)
        }
    }

    private fun runFaissSmokeTest(): String {
        val dimension = 4
        val report = StringBuilder()

        return try {
            val initResult = FaissWrapper.initStore(dimension)
            if (!initResult) {
                return "Failed to initialize FAISS index (dimension=$dimension)"
            }
            report.appendLine("Initialized FAISS IndexFlatL2 with $dimension dimensions.")

            val vectors = floatArrayOf(
                0.10f, 0.20f, 0.30f, 0.40f,
                0.11f, 0.21f, 0.31f, 0.41f,
                0.90f, 0.10f, 0.20f, 0.05f
            )
            val numVectors = vectors.size / dimension
            val addedIds = FaissWrapper.addVectors(vectors, numVectors)
            report.appendLine("Added $numVectors vectors. IDs: ${addedIds.joinToString()}")

            val queryVector = floatArrayOf(0.92f, 0.12f, 0.25f, 0.05f)
            val rawResults = FaissWrapper.queryVectors(queryVector, 1, 2)
            if (rawResults == null) {
                report.appendLine("Query returned no data.")
                return report.toString().trim()
            }

            val idArray = rawResults.getOrNull(0) as? LongArray
            val distanceArray = rawResults.getOrNull(1) as? FloatArray
            if (idArray == null || distanceArray == null) {
                report.appendLine("Unexpected response payload from native query.")
                return report.toString().trim()
            }

            report.appendLine("Top matches:")
            val totalResults = minOf(idArray.size, distanceArray.size)
            for (index in 0 until totalResults) {
                val distance = "%.4f".format(distanceArray[index])
                report.appendLine(" - id=${idArray[index]}, distance=$distance")
            }

            report.toString().trim()
        } catch (throwable: Throwable) {
            report.appendLine("Encountered error: ${throwable.localizedMessage ?: throwable.javaClass.simpleName}")
            report.toString().trim()
        } finally {
            FaissWrapper.destroyStore()
        }
    }

    private fun testRagQuery(query: String) {
        binding.testRagQueryButton.isEnabled = false
        binding.resultText.text = "RAG 쿼리 처리 중..."

        lifecycleScope.launch {
            try {
                LogUtils.i("MainActivity", "Processing RAG query: $query")

                // RagService.query를 통해 RAG 쿼리 실행
                val result = RagService.query(query)

                LogUtils.i("MainActivity", "RAG query completed")
                binding.resultText.text = """
                    질문: $query

                    답변:
                    $result
                """.trimIndent()

            } catch (e: Exception) {
                LogUtils.e("MainActivity", "RAG query failed: ${e.message}", e)
                binding.resultText.text = """
                    오류가 발생했습니다:
                    ${e.message}

                    ${e.stackTraceToString().take(500)}...
                """.trimIndent()
            } finally {
                binding.testRagQueryButton.isEnabled = true
            }
        }
    }
}
