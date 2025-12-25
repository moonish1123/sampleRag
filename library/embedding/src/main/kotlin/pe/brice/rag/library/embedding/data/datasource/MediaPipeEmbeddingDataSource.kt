package pe.brice.rag.library.embedding.data.datasource

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import pe.brice.rag.library.embedding.data.datasource.EmbeddingDataSource
import pe.brice.rag.library.embedding.domain.model.EmbeddingRequest
import pe.brice.rag.library.embedding.domain.model.EmbeddingVector

class MediaPipeEmbeddingDataSource(
    private val context: Context
) : EmbeddingDataSource {

    companion object {
        private const val MODEL_PATH = "model/universal_sentence_encoder_qa_ondevice.tflite"
        private const val MODEL_NAME = "universal_sentence_encoder"
        private const val DIMENSION = 100
    }

    private val baseOptions = BaseOptions.builder()
        .setModelAssetPath(MODEL_PATH)
        .build()

    private val options = TextEmbedderOptions.builder()
        .setBaseOptions(baseOptions)
        .build()

    private val textEmbedder = TextEmbedder.createFromOptions(context, options)

    override suspend fun embed(request: EmbeddingRequest): Result<EmbeddingVector> {
        return try {
            val embedding = textEmbedder.embed(request.text)
            val floatArray = embedding.embeddingResult().embeddings().first().floatEmbedding()

            Result.success(
                EmbeddingVector(
                    values = floatArray.toList(),
                    dimension = DIMENSION,
                    model = MODEL_NAME
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        textEmbedder.close()
    }
}
