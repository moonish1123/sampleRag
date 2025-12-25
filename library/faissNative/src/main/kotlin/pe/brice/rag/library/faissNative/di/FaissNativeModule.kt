package pe.brice.rag.library.faissNative.di

import org.koin.dsl.module
import pe.brice.rag.library.faissNative.FaissNativeWrapper

val faissNativeModule = module {
    single { FaissNativeWrapper }
}
