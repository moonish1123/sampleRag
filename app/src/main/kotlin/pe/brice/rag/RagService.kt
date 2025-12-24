package pe.brice.rag

import pe.brice.rag.account.AccountManager
import pe.brice.rag.common.masked
import pe.brice.rag.legacy.model.SMessage
import pe.brice.rag.legacy.util.LogUtils
import pe.brice.rag.llm.Promptable
import pe.brice.rag.llm.openai.OpenAi
import pe.brice.rag.llm.openai.OpenAiEmbedding
import pe.brice.rag.splitter.HtmlRecursiveTextSplitter
import pe.brice.rag.vectorstore.RagVectorStoreUseCase
import pe.brice.rag.vectorstore.VectorMatch
import pe.brice.rag.vectorstore.faiss.RagFaissDataStore
import pe.brice.rag.vectorstore.metadataStore.PreferenceMetaDataStore
import org.jsoup.Jsoup

object RagService {
    const val VERSION = 2 //2 is openAI embedding
    const val TAG = "RagService"

    //Di 가 없으니 여기서 다 선언해서 사용해야 할듯.
    val embedding = OpenAiEmbedding() //MediaPipeEmbedding() //OpenAiEmbedding()

    val service: RagVectorStoreUseCase = RagVectorStoreUseCase(
        RagFaissDataStore(embedding, PreferenceMetaDataStore())
    )

    val splitter = HtmlRecursiveTextSplitter(1500, 200)

    val llm: Promptable = OpenAi()

    fun add(message: SMessage, doc: String) {
        splitter.splitText(Jsoup.parse(doc).text()).forEach { chunk ->
            if (chunk.isNotBlank()) {
                LogUtils.i(TAG, "[rag] Adding vector for message ID: ${message.displaySubject.masked}, chunk size: ${chunk.length}")
                service.addVector(message, chunk)
            }
        }
    }

    fun getChunk(response: List<VectorMatch>, index: Int = 0): String {
        val id = response[index].metadata?.get("id")?.toLong() ?: return "No relevant context found. 2"
        val accountId = response[index].metadata?.get("account_id")?.toLong() ?: return "No relevant context found. 3"
        val account = AccountManager.instance.getAccount(accountId) ?: return "No account found for ID: $accountId"
        val subject = response[index].metadata?.get("subject") ?: "No subject found."
        val chunk = response[index].metadata?.get("chunk") ?: return "No relevant context found. 4"

        LogUtils.i(TAG, "[rag] getChunk subject: ${subject} at $index")

        return """
            ${index+1}th context account email is [\"${account.email}\"] message subject is {\"${subject}\"} mail body content is {\"${chunk}\"} as html.    
        """
    }

    fun query(query: String): String {
        if (query.isBlank()) {
            return "질문이 비어있습니다. 태양계에 대해 질문해보세요."
        }

        val response = service.queryVector(query, 3)
        if (response == null || response.isEmpty()) {
            // RAG 결과가 없을 경우 일반 LLM 쿼리
            LogUtils.i(TAG, "[rag] No context found, using general knowledge")
            return try {
                val prompt = """
                    태양계 전문가로서 다음 질문에 답변해주세요:

                    질문: $query

                    답변:
                """.trimIndent()
                llm.prompt(prompt)
            } catch (e: Exception) {
                LogUtils.e(TAG, "LLM query failed: ${e.message}", e)
                "죄송합니다. 현재 답변을 처리할 수 없습니다. 잠시 후 다시 시도해주세요."
            }
        }

        // Build context with error handling
        val contextBuilder = StringBuilder()
        for (i in 0 until minOf(3, response.size)) {
            try {
                val chunk = getChunk(response, i)
                if (!chunk.contains("No relevant context found") && !chunk.contains("No account found")) {
                    contextBuilder.append(chunk).append("\n")
                }
            } catch (e: Exception) {
                LogUtils.w(TAG, "Failed to get chunk at index $i: ${e.message}")
            }
        }

        val context = if (contextBuilder.isNotBlank()) {
            contextBuilder.toString()
        } else {
            LogUtils.i(TAG, "[rag] No valid context found, using general knowledge")
            // Context가 없더라도 일반 지식으로 답변
            return try {
                val prompt = """
                    태양계 전문가로서 다음 질문에 답변해주세요:

                    질문: $query

                    답변:
                """.trimIndent()
                llm.prompt(prompt)
            } catch (e: Exception) {
                "죄송합니다. 현재 답변을 처리할 수 없습니다. 잠시 후 다시 시도해주세요."
            }
        }

        val prompt = """
            당신은 태양계 전문가입니다. 제공된 정보를 바탕으로 질문에 정확하게 답변하세요.

            규칙:
            1. 제공된 컨텍스트를 기반으로 답변하세요.
            2. 컨텍스트의 정보를 사용했다면 출처를 명확히 표시하세요.
            3. 컨텍스트에 답이 없는 경우, 일반 지식으로 답변하고 그 사실을 밝히세요.

            [컨텍스트]
            $context

            [질문]
            $query

            [답변]
        """.trimIndent()

        LogUtils.i(TAG, "[rag] Querying LLM with context (length: ${prompt.length})")

        return try {
            llm.prompt(prompt)
        } catch (e: Exception) {
            LogUtils.e(TAG, "LLM query failed: ${e.message}", e)
            "죄송합니다. 현재 답변을 처리할 수 없습니다. 잠시 후 다시 시도해주세요."
        }
    }
}
