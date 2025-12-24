package pe.brice.rag.common

val String?.masked: String
    get() {
        val value = this ?: return ""
        if (value.length <= 4) return value
        return buildString {
            append(value.take(2))
            append("***")
            append(value.takeLast(2))
        }
    }
