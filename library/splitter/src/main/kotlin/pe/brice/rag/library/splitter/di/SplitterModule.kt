package pe.brice.rag.library.splitter.di

import org.koin.dsl.module
import pe.brice.rag.library.splitter.data.impl.HtmlRecursiveTextSplitter
import pe.brice.rag.library.splitter.domain.repository.TextSplitter

val splitterModule = module {
    single<TextSplitter> {
        HtmlRecursiveTextSplitter(
            chunkSize = 1000,
            chunkOverlap = 100
        )
    }
}
