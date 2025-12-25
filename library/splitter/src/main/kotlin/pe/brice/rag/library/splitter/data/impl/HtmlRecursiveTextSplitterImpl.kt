package pe.brice.rag.library.splitter.data.impl

import pe.brice.rag.library.splitter.domain.model.TextChunk
import pe.brice.rag.library.splitter.domain.repository.TextSplitter
import kotlin.math.min

class HtmlRecursiveTextSplitter(
    private val chunkSize: Int = 1000,
    private val chunkOverlap: Int = 100,
    private val separators: List<String> = listOf("\n\n", "\n", ". ", " ", "")
) : TextSplitter {

    init {
        if (chunkOverlap >= chunkSize) {
            throw IllegalArgumentException("Chunk overlap should be smaller than chunk size.")
        }
    }

    override fun splitText(text: String): List<TextChunk> {
        if (text.isBlank()) return emptyList()

        val chunks = mutableListOf<String>()
        splitRecursively(text, separators, chunks)

        return chunks.mapIndexed { index, chunk ->
            TextChunk(
                text = chunk,
                index = index
            )
        }
    }

    override fun splitDocuments(documents: List<String>): List<TextChunk> {
        return documents.flatMap { doc ->
            splitText(doc)
        }
    }

    private fun splitRecursively(text: String, currentSeparators: List<String>, finalChunks: MutableList<String>) {
        if (text.length <= chunkSize) {
            if (text.isNotBlank()) {
                finalChunks.add(text)
            }
            return
        }

        val separator = currentSeparators.firstOrNull { text.contains(it) }

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

        val splits = text.split(separator)
        var currentChunk = StringBuilder()

        for (part in splits) {
            if (currentChunk.isNotEmpty() && (currentChunk.length + part.length + separator.length > chunkSize)) {
                splitRecursively(currentChunk.toString(), currentSeparators, finalChunks)
                currentChunk = StringBuilder(part)
            } else {
                if (currentChunk.isNotEmpty()) {
                    currentChunk.append(separator)
                }
                currentChunk.append(part)
            }
        }

        if (currentChunk.isNotEmpty()) {
            splitRecursively(currentChunk.toString(), currentSeparators, finalChunks)
        }
    }
}
