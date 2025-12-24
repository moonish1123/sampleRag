package pe.brice.rag.llm.mediaPipe

import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import pe.brice.rag.RagApplication
import pe.brice.rag.llm.Embedding

class MediaPipeEmbedding: Embedding {
    companion object {
        const val TAG = "MediaPipeEmbedding"
    }
    val baseOptions = BaseOptions.builder()
        .setModelAssetPath("model/universal_sentence_encoder_qa_ondevice.tflite")
        .build()

    val options = TextEmbedderOptions.builder()
        .setBaseOptions(baseOptions)
        .build()

    // 2. TextEmbedder 인스턴스 생성
    private val textEmbedder = TextEmbedder.createFromOptions(RagApplication.getInstance().applicationContext, options)

    override suspend fun getEmbedding(input: String): List<Float> {
        val embedding = textEmbedder.embed(input).embeddingResult().embeddings().first()
        return embedding.floatEmbedding().toList()
    }

    override fun getDimensions(): Int = 100

    override fun getModelName(): String {
        return "universal_sentence_encoder"
    }

    override fun close() {
        textEmbedder.close()
    }
}
