package pe.brice.rag.vectorstore.pinecone

import kotlinx.coroutines.runBlocking
import pe.brice.rag.llm.Embedding
import pe.brice.rag.vectorstore.RagVectorStore
import pe.brice.rag.vectorstore.VectorMatch
import pe.brice.rag.vectorstore.pinecone.api.PineconeApiClient

class PineconeDataStore(val apiClient: PineconeApiClient, val embedding: Embedding): RagVectorStore {
    override fun addVector(doc: String, metadata: Map<String, String>): Long = runBlocking {
        val id = metadata["id"]?.toLongOrNull() ?: return@runBlocking -1L
        apiClient.upsert(id, embedding.getEmbedding(doc), metadata)
    }

    override fun deleteVector(id: Long): Boolean {
        return apiClient.deleteVectors(id)
    }

    override fun deleteAllVectors(): Boolean {
        return apiClient.deleteAllVectors()
    }

    override fun queryVector(query: String, topK: Int): List<VectorMatch>? = runBlocking {
        val queryEmbedding = embedding.getEmbedding(query)

        val response = apiClient.queryVectors(queryEmbedding, topK)

        response?.matches?.map { match ->
            VectorMatch(
                id = match.id.toLong(),
                score = match.score,
                metadata = match.metadata ?: emptyMap()
            )
        }
    }

    override fun destroy() {}
}
