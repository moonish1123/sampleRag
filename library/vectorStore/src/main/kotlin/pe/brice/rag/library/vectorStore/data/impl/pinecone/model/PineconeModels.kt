package pe.brice.rag.library.vectorStore.data.impl.pinecone.model

data class VectorData(
    val id: String,
    val values: List<Float>,
    val metadata: Map<String, String>? = null
)

data class UpsertRequest(
    val vectors: List<VectorData>,
    val namespace: String? = null
)

data class FetchResponse(
    val vectors: Map<String, VectorData>
)

data class DeleteRequest(
    val ids: List<String>? = null,
    val deleteAll: Boolean? = null,
    val namespace: String? = null,
    val filter: Map<String, Any>? = null
)

data class QueryRequest(
    val vector: List<Float>? = null,
    val id: String? = null,
    val topK: Int,
    val namespace: String? = null,
    val filter: Map<String, Any>? = null,
    val includeValues: Boolean? = null,
    val includeMetadata: Boolean? = null
)

data class Match(
    val id: String,
    val score: Float,
    val values: List<Float>? = null,
    val metadata: Map<String, String>? = null
)

data class QueryResponse(
    val matches: List<Match>,
    val namespace: String? = null
)
