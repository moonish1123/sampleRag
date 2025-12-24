package pe.brice.rag.llm

import kotlin.math.sqrt

interface Embedding {
    suspend fun getEmbedding(input: String): List<Float>
    fun getDimensions(): Int
    fun getModelName(): String
    fun close()

    fun calculateSimilarity(embedding1: List<Float>, embedding2: List<Float>): Float {
        if (embedding1.size != embedding2.size) {
            throw IllegalArgumentException("Embeddings must have the same dimensions")
        }

        val dotProduct = embedding1.zip(embedding2).map { it.first * it.second }.sum().toFloat()
        val magnitude1 = sqrt(embedding1.map { it * it }.sum().toDouble()).toFloat()
        val magnitude2 = sqrt(embedding2.map { it * it }.sum().toDouble()).toFloat()

        return if (magnitude1 == 0f || magnitude2 == 0f) {
            0f // Avoid division by zero
        } else {
            dotProduct / (magnitude1 * magnitude2)
        }
    }
}

