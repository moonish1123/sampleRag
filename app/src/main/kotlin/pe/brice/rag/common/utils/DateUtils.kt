package pe.brice.rag.common.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun convertDateString(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return ""
        return formatter.format(Date(timestamp))
    }
}
