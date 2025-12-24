package pe.brice.rag.vectorstore.metadataStore

import android.content.Context
import com.google.gson.Gson
import pe.brice.rag.RagApplication
import pe.brice.rag.common.preference.PreferenceUtils

//일단 test 용도로 작성하고 permenant 저장을 하긴 해야 한다.
//안하면 data 가 다 날라감.
class PreferenceMetaDataStore: MetadataStore {
    companion object {
        const val PREF_GROUP = "metadata_faiss"
    }
    val context: Context get() = RagApplication.getInstance().applicationContext

    override fun init(context: Context) {}

    private fun getKey(id: Long): String = "metadata_$id"

    override fun addMetadata(id: Long, metadata: Map<String, String>): Boolean {
        if (metadata.isEmpty()) return false
        if (id == -1L) return false

        PreferenceUtils.put(context, PREF_GROUP, getKey(id), Gson().toJson(metadata))
        return true
    }

    override fun getMetadata(id: Long): Map<String, String>? {
        val jsonString = PreferenceUtils.getString(context, PREF_GROUP, getKey(id), "")
        if (jsonString.isBlank()) return null
        return try {
            Gson().fromJson(jsonString, Map::class.java) as Map<String, String>
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun deleteMetadata(id: Long): Boolean {
        PreferenceUtils.removeKey(context, PREF_GROUP, getKey(id))
        return true
    }

    override fun deleteMetadata(): Boolean {
        PreferenceUtils.removeGroup(context, PREF_GROUP)
        return true
    }
}
