package pe.brice.rag.library.faissNative

object FaissNativeWrapper {
    private var initialized: Boolean = false

    init {
        System.loadLibrary("faiss_jni_lib")
    }

    external fun initStore(dimension: Int): Boolean
    external fun destroyStore(): Boolean
    external fun addVectors(
        javaVectors: FloatArray,
        numVectors: Int
    ): LongArray?
    external fun queryVectors(
        javaQueryVectors: FloatArray,
        numQueryVectors: Int,
        kNeighbors: Int
    ): Array<Any>?
    external fun saveIndexNative(
        path: String
    ): Boolean
    external fun loadIndexNative(
        path: String
    ): Boolean

    external fun getNTotal(): Long
    external fun getDimension(): Int
    external fun isIndexTrained(): Boolean
    external fun isIndexLoadedNative(): Boolean
}
