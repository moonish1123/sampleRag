package pe.brice.rag.vectorstore.metadataStore

import android.content.Context

interface MetadataStore {
    fun init(applicationContext: Context)
    fun addMetadata(id: Long, metadata: Map<String, String>): Boolean
    fun getMetadata(id: Long): Map<String, String>?
    fun deleteMetadata(id: Long): Boolean
    fun deleteMetadata(): Boolean
}