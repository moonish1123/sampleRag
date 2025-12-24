package pe.brice.rag.common.preference

import android.content.Context

object PreferenceUtils {

    fun put(context: Context, group: String, key: String, value: String) {
        context.getSharedPreferences(group, Context.MODE_PRIVATE)
            .edit()
            .putString(key, value)
            .apply()
    }

    fun getString(context: Context, group: String, key: String, default: String = ""): String {
        return context.getSharedPreferences(group, Context.MODE_PRIVATE)
            .getString(key, default) ?: default
    }

    fun removeKey(context: Context, group: String, key: String) {
        context.getSharedPreferences(group, Context.MODE_PRIVATE)
            .edit()
            .remove(key)
            .apply()
    }

    fun removeGroup(context: Context, group: String) {
        context.getSharedPreferences(group, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
