package pe.brice.rag.llm.openai

import kotlinx.coroutines.runBlocking
import pe.brice.rag.llm.Promptable

class OpenAi: Promptable {
    override fun prompt(prompt: String): String = runBlocking {
        OpenAiApiClient.prompt(prompt)
    }
}