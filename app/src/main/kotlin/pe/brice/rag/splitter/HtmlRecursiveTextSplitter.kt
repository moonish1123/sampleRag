package pe.brice.rag.splitter

import kotlin.math.min

class HtmlRecursiveTextSplitter(
    private val chunkSize: Int = 1000,
    private val chunkOverlap: Int = 100,
    private val separators: List<String> = listOf("\n\n", "\n", ". ", " ", "") // 마침표+공백 추가
) {

    init {
        if (chunkOverlap > chunkSize) {
            throw IllegalArgumentException("Chunk overlap should be smaller than chunk size.")
        }
    }

    /**
     * 순수 텍스트를 분할하는 메인 로직.
     *
     * @param text 분할할 순수 텍스트.
     * @return 텍스트 청크의 리스트.
     */
    fun splitText(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val finalChunks = mutableListOf<String>()
        // 재귀 함수를 호출하여 finalChunks 리스트를 채웁니다.
        splitRecursively(text, separators, finalChunks)
        return finalChunks
    }

    private fun splitRecursively(text: String, currentSeparators: List<String>, finalChunks: MutableList<String>) {
        // 기본 케이스 1: 텍스트가 청크 크기보다 작거나 같으면 그대로 추가하고 종료.
        if (text.length <= chunkSize) {
            if (text.isNotBlank()) {
                finalChunks.add(text)
            }
            return
        }

        // 텍스트를 분할할 가장 적합한 구분자를 찾습니다.
        val separator = currentSeparators.firstOrNull { text.contains(it) }

        // 기본 케이스 2: 더 이상 나눌 구분자가 없으면 강제로 분할합니다.
        if (separator == null || separator.isEmpty()) {
            var i = 0
            while (i < text.length) {
                val end = min(i + chunkSize, text.length)
                val chunk = text.substring(i, end)
                if (chunk.isNotBlank()) {
                    finalChunks.add(chunk)
                }
                i += chunkSize - chunkOverlap
            }
            return
        }

        // 찾은 구분자로 텍스트를 분할하고, 각 조각을 처리합니다.
        val splits = text.split(separator)
        var currentChunk = StringBuilder()

        for (part in splits) {
            // 만약 현재 조각을 추가하면 청크 크기를 초과하는 경우
            if (currentChunk.isNotEmpty() && (currentChunk.length + part.length + separator.length > chunkSize)) {
                // 지금까지 합친 currentChunk를 재귀적으로 처리하여 finalChunks에 추가
                splitRecursively(currentChunk.toString(), currentSeparators, finalChunks)
                // currentChunk를 현재 조각으로 초기화
                currentChunk = StringBuilder(part)
            } else {
                // 청크 크기를 초과하지 않으면 계속 합칩니다.
                if (currentChunk.isNotEmpty()) {
                    currentChunk.append(separator)
                }
                currentChunk.append(part)
            }
        }

        // 마지막으로 남은 currentChunk를 처리합니다.
        if (currentChunk.isNotEmpty()) {
            splitRecursively(currentChunk.toString(), currentSeparators, finalChunks)
        }
    }
}