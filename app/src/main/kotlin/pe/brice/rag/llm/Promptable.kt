package pe.brice.rag.llm

interface Promptable {
    fun prompt(prompt: String): String
}