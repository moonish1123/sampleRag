package pe.brice.rag.legacy.model

data class SMessage(
    val id: Long,
    val accountId: Long,
    val subject: String? = null,
    val receivedDate: Long = System.currentTimeMillis(),
    val displaySubject: String = subject ?: ""
)
