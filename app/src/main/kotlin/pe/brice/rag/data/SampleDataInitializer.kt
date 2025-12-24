package pe.brice.rag.data

import android.content.Context
import pe.brice.rag.RagService
import pe.brice.rag.legacy.util.LogUtils
import pe.brice.rag.legacy.model.SMessage
import java.io.BufferedReader
import java.io.InputStreamReader

class SampleDataInitializer(private val context: Context) {

    companion object {
        private const val TAG = "SampleDataInitializer"
        private const val PREF_KEY_DATA_LOADED = "solar_system_data_loaded"
        private const val PREF_KEY_DATA_VERSION = "solar_system_data_version"
        private const val CURRENT_DATA_VERSION = 1

        // 데이터 파일 목록
        private val DATA_FILES = listOf(
            "solar_system_data/sun_overview.txt",
            "solar_system_data/inner_planets.txt",
            "solar_system_data/outer_planets.txt",
            "solar_system_data/dwarf_planets_and_asteroids.txt"
        )
    }

    /**
     * 샘플 데이터가 초기화되었는지 확인하고, 필요시 초기화를 수행합니다.
     */
    suspend fun initializeIfNeeded() {
        if (isDataLoaded() && isDataVersionCurrent()) {
            LogUtils.i(TAG, "Solar system sample data is already loaded and up to date")
            return
        }

        LogUtils.i(TAG, "Initializing solar system sample data...")
        loadSolarSystemData()
        markDataAsLoaded()
        LogUtils.i(TAG, "Solar system sample data initialization completed")
    }

    /**
     * 데이터가 이미 로드되었는지 확인합니다.
     */
    private fun isDataLoaded(): Boolean {
        val prefs = context.getSharedPreferences("rag_sample_data", Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_KEY_DATA_LOADED, false)
    }

    /**
     * 데이터 버전이 현재 버전과 일치하는지 확인합니다.
     */
    private fun isDataVersionCurrent(): Boolean {
        val prefs = context.getSharedPreferences("rag_sample_data", Context.MODE_PRIVATE)
        val loadedVersion = prefs.getInt(PREF_KEY_DATA_VERSION, 0)
        return loadedVersion == CURRENT_DATA_VERSION
    }

    /**
     * 데이터 로드를 완료했음을 표시합니다.
     */
    private fun markDataAsLoaded() {
        val prefs = context.getSharedPreferences("rag_sample_data", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(PREF_KEY_DATA_LOADED, true)
            .putInt(PREF_KEY_DATA_VERSION, CURRENT_DATA_VERSION)
            .apply()
    }

    /**
     * 태양계 데이터를 RAG 벡터 저장소에 로드합니다.
     */
    private suspend fun loadSolarSystemData() {
        try {
            // 기존 데이터 삭제 (선택적 - 데이터 버전 관리를 위해)
            // RagService.service.deleteAllVectors()

            for (fileName in DATA_FILES) {
                loadSingleFile(fileName)
            }

            LogUtils.i(TAG, "Successfully loaded ${DATA_FILES.size} solar system data files")
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error loading solar system data: ${e.message}", e)
            throw e
        }
    }

    /**
     * 개별 파일을 로드합니다.
     */
    private suspend fun loadSingleFile(fileName: String) {
        try {
            val content = readAssetFile(fileName)
            if (content.isBlank()) {
                LogUtils.w(TAG, "File $fileName is empty, skipping")
                return
            }

            // 더미 SMessage 생성 (실제 메시지는 아니지만 RAG 시스템에서 필요한 구조)
            val dummyMessage = SMessage(
                id = System.currentTimeMillis(),
                accountId = 1L, // demo account
                subject = getSubjectFromFileName(fileName)
            )

            // RagService를 통해 벡터에 추가
            RagService.add(dummyMessage, content)

            LogUtils.i(TAG, "Loaded file: $fileName (${content.length} characters)")

        } catch (e: Exception) {
            LogUtils.e(TAG, "Error loading file $fileName: ${e.message}", e)
            throw e
        }
    }

    /**
     * Assets 폴더에서 파일을 읽습니다.
     */
    private fun readAssetFile(fileName: String): String {
        val inputStream = context.assets.open(fileName)
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))

        val content = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            content.append(line).append("\n")
        }

        reader.close()
        inputStream.close()

        return content.toString()
    }

    /**
     * 파일 이름으로부터 주제를 추출합니다.
     */
    private fun getSubjectFromFileName(fileName: String): String {
        return when {
            fileName.contains("sun_overview") -> "태양계 개요"
            fileName.contains("inner_planets") -> "내행성 (수성, 금성, 지구, 화성)"
            fileName.contains("outer_planets") -> "외행성 (목성, 토성, 천왕성, 해왕성)"
            fileName.contains("dwarf_planets") -> "왜행성과 소행성"
            else -> "태양계 정보"
        }
    }

    /**
     * 데이터 로드 상태를 재설정합니다 (디버깅용).
     */
    fun resetDataLoadedFlag() {
        val prefs = context.getSharedPreferences("rag_sample_data", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        LogUtils.i(TAG, "Data loading flag has been reset")
    }
}