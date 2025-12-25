package pe.brice.rag.library.splitter.domain.repository

import pe.brice.rag.library.splitter.domain.model.TextChunk

interface TextSplitter {
    fun splitText(text: String): List<TextChunk>
    fun splitDocuments(documents: List<String>): List<TextChunk>
}
