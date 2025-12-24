#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <cinttypes>

// Faiss 헤더
#include "faiss/IndexFlat.h"
#include "faiss/Index.h"
#include "faiss/impl/FaissAssert.h"
#include "faiss/index_io.h" // <--- 파일 I/O를 위한 헤더 추가

// 로그 태그
#define LOG_TAG "FaissJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Faiss 인덱스 포인터
static faiss::Index* faiss_index = nullptr;
static int current_dimension = 0;

extern "C" {
    JNIEXPORT jboolean JNICALL
    Java_pe_brice_rag_vectorstore_faiss_FaissWrapper_initStore(JNIEnv *env, jobject thiz, jint dimension) {
        if (faiss_index != nullptr) {
            LOGI("Faiss index already initialized. Deleting existing one before creating a new one.");
            delete faiss_index;
            faiss_index = nullptr;
            current_dimension = 0;
        }

        if (dimension <= 0) {
            LOGE("Dimension must be positive: %d", dimension);
            return JNI_FALSE;
        }

        try {
            LOGI("Initializing Faiss IndexFlatL2 with dimension: %d", dimension);
            faiss_index = new faiss::IndexFlatL2(dimension);
            current_dimension = dimension;
            LOGI("Faiss IndexFlatL2 initialized successfully. Dimension: %d, Is Trained: %s, NTotal: %lld",
                 faiss_index->d, faiss_index->is_trained ? "true" : "false", faiss_index->ntotal);
            return JNI_TRUE;
        } catch (const faiss::FaissException& e) {
            LOGE("FaissException during init: %s", e.what());
            return JNI_FALSE;
        } catch (const std::exception& e) {
            LOGE("StdException during init: %s", e.what());
            return JNI_FALSE;
        } catch (...) {
            LOGE("Unknown exception during init");
            return JNI_FALSE;
        }
    }

    JNIEXPORT jlongArray JNICALL
    Java_pe_brice_rag_vectorstore_faiss_FaissWrapper_addVectors(JNIEnv *env, jobject thiz, jfloatArray java_vectors, jint num_vectors) {
        // --- 초기 검사 (기존과 동일) ---
        if (faiss_index == nullptr) {
            LOGE("Faiss index not initialized. Call initStore or loadIndexNative first.");
            return nullptr; // 실패 시 nullptr 반환 (또는 빈 jlongArray)
        }
        if (!faiss_index->is_trained) {
            LOGE("Faiss index is not trained. Cannot add vectors.");
            return nullptr;
        }
        if (java_vectors == nullptr) {
            LOGE("Input vectors array is null.");
            return nullptr;
        }
        if (num_vectors <= 0) {
            LOGI("Number of vectors to add is zero or negative: %d. Nothing to add.", num_vectors);
            // 성공적으로 "아무것도 안 함", 빈 ID 배열 반환 또는 nullptr.
            // 여기서는 빈 배열을 반환하는 것이 더 명확할 수 있습니다.
            jlongArray empty_array = env->NewLongArray(0);
            return empty_array;
        }

        jfloat* vectors_ptr = env->GetFloatArrayElements(java_vectors, nullptr);
        if (vectors_ptr == nullptr) {
            LOGE("Failed to get float array elements from java_vectors.");
            return nullptr;
        }

        jsize array_len = env->GetArrayLength(java_vectors);
        if (array_len != num_vectors * current_dimension) {
            LOGE("Vector data size mismatch. Expected %d floats, got %d.",
                 num_vectors * current_dimension, array_len);
            env->ReleaseFloatArrayElements(java_vectors, vectors_ptr, JNI_ABORT);
            return nullptr;
        }

        // --- Faiss 내부 ID 계산 및 반환 로직 추가 ---
        faiss::idx_t ntotal_before_add = faiss_index->ntotal; // 추가 전 총 벡터 수
        std::vector<faiss::idx_t> added_faiss_ids;
        added_faiss_ids.reserve(num_vectors);

        try {
            LOGI("Adding %d vectors to Faiss index. Current total before add: %lld", num_vectors, ntotal_before_add);
            faiss_index->add(num_vectors, vectors_ptr); // Faiss에 벡터 추가
            LOGI("Successfully added %d vectors. New total vectors in index: %lld", num_vectors, faiss_index->ntotal);
            env->ReleaseFloatArrayElements(java_vectors, vectors_ptr, JNI_ABORT); // 사용 후 해제

            // 새로 추가된 벡터들의 Faiss 내부 ID 생성
            for (int i = 0; i < num_vectors; ++i) {
                added_faiss_ids.push_back(ntotal_before_add + i);
            }

            // 생성된 ID들을 jlongArray로 변환하여 반환
            jlongArray java_added_ids = env->NewLongArray(num_vectors);
            if (java_added_ids == nullptr) {
                LOGE("Failed to create new long array for returning Faiss IDs.");
                // 심각한 오류: 벡터는 추가되었으나 ID를 반환할 수 없음.
                // 이 경우, 이미 추가된 벡터를 롤백하기는 어려우므로, 로그를 남기고 nullptr 반환.
                // 호출 측에서는 이 상황을 인지하고 대응해야 함.
                return nullptr;
            }
            // faiss::idx_t가 long (int64_t)과 호환된다고 가정.
            env->SetLongArrayRegion(java_added_ids, 0, num_vectors, reinterpret_cast<const jlong*>(added_faiss_ids.data()));

            LOGI("Returning %d Faiss IDs for newly added vectors.", num_vectors);
            return java_added_ids;
        } catch (const faiss::FaissException& e) {
            LOGE("FaissException during addVectors: %s", e.what());
            env->ReleaseFloatArrayElements(java_vectors, vectors_ptr, JNI_ABORT);
            return nullptr;
        } catch (const std::exception& e) {
            LOGE("StdException during addVectors: %s", e.what());
            env->ReleaseFloatArrayElements(java_vectors, vectors_ptr, JNI_ABORT);
            return nullptr;
        } catch (...) {
            LOGE("Unknown exception during addVectors");
            env->ReleaseFloatArrayElements(java_vectors, vectors_ptr, JNI_ABORT);
            return nullptr;
        }
    }

    JNIEXPORT jobjectArray JNICALL
    Java_pe_brice_rag_vectorstore_faiss_FaissWrapper_queryVectors(JNIEnv *env, jobject thiz, jfloatArray java_query_vectors, jint num_query_vectors, jint k_neighbors) {
        if (faiss_index == nullptr) {
            LOGE("Faiss index not initialized. Call initStore or loadIndexNative first.");
            return nullptr;
        }
        if (!faiss_index->is_trained) {
            LOGE("Faiss index is not trained. Cannot query vectors.");
            return nullptr;
        }
        if (java_query_vectors == nullptr) {
            LOGE("Query vectors array is null.");
            return nullptr;
        }
        if (num_query_vectors <= 0) {
            LOGE("Number of query vectors must be positive: %d", num_query_vectors);
            return nullptr;
        }
        if (k_neighbors <= 0) {
            LOGE("Number of k_neighbors must be positive: %d", k_neighbors);
            return nullptr;
        }

        if (faiss_index->ntotal == 0) {
            LOGW("Index is empty. Query will not find any results.");
            // 빈 결과를 반환하기 위해 (0,0) 크기의 배열을 생성할 수 있습니다.
            // 여기서는 예시로써 null을 반환하지만, Java 쪽에서 빈 결과를 처리하는 것이 더 좋습니다.
            // 또는, 아래 로직을 통해 빈 결과 배열을 생성하여 반환합니다.
            jclass object_array_class = env->FindClass("java/lang/Object");
            if (object_array_class == nullptr) { LOGE("Failed to find java/lang/Object class for empty result."); return nullptr; }
            jobjectArray empty_result_array = env->NewObjectArray(2, object_array_class, nullptr);
            if (empty_result_array == nullptr) { LOGE("Failed to create new object array for empty results."); return nullptr; }

            jlongArray java_indices_empty = env->NewLongArray(0);
            jfloatArray java_distances_empty = env->NewFloatArray(0);

            env->SetObjectArrayElement(empty_result_array, 0, java_indices_empty);
            env->SetObjectArrayElement(empty_result_array, 1, java_distances_empty);

            env->DeleteLocalRef(java_indices_empty);
            env->DeleteLocalRef(java_distances_empty);
            env->DeleteLocalRef(object_array_class);
            return empty_result_array;
        }

        long actual_k = k_neighbors;
        if (k_neighbors > faiss_index->ntotal) {
            LOGW("k_neighbors (%d) is greater than total vectors in index (%lld). Will search for %lld neighbors.", k_neighbors, faiss_index->ntotal, faiss_index->ntotal);
            actual_k = faiss_index->ntotal;
        }


        jfloat* query_vectors_ptr = env->GetFloatArrayElements(java_query_vectors, nullptr);
        if (query_vectors_ptr == nullptr) {
            LOGE("Failed to get float array elements from java_query_vectors.");
            return nullptr;
        }

        jsize query_array_len = env->GetArrayLength(java_query_vectors);
        if (query_array_len != num_query_vectors * current_dimension) {
            LOGE("Query vector data size mismatch. Expected %d floats (num_query_vectors: %d * current_dimension: %d), but got %d.",
                 num_query_vectors * current_dimension, num_query_vectors, current_dimension, query_array_len);
            env->ReleaseFloatArrayElements(java_query_vectors, query_vectors_ptr, JNI_ABORT);
            return nullptr;
        }

        std::vector<faiss::idx_t> result_indices(num_query_vectors * actual_k);
        std::vector<float> result_distances(num_query_vectors * actual_k);

        try {
            LOGI("Querying %d vectors for %ld nearest neighbors. Index total: %lld", num_query_vectors, actual_k, faiss_index->ntotal);
            faiss_index->search(num_query_vectors, query_vectors_ptr, actual_k, result_distances.data(), result_indices.data());
            LOGI("Query completed.");
            env->ReleaseFloatArrayElements(java_query_vectors, query_vectors_ptr, JNI_ABORT);

            jlongArray java_indices_flat = env->NewLongArray(result_indices.size());
            if (java_indices_flat == nullptr) {
                LOGE("Failed to create new long array for indices."); return nullptr;
            }
            env->SetLongArrayRegion(java_indices_flat, 0, result_indices.size(), reinterpret_cast<const jlong*>(result_indices.data()));


            jfloatArray java_distances_flat = env->NewFloatArray(result_distances.size());
            if (java_distances_flat == nullptr) {
                LOGE("Failed to create new float array for distances."); return nullptr;
            }
            env->SetFloatArrayRegion(java_distances_flat, 0, result_distances.size(), result_distances.data());

            jclass object_array_class = env->FindClass("java/lang/Object");
            if (object_array_class == nullptr) {
                LOGE("Failed to find java/lang/Object class."); return nullptr;
            }
            jobjectArray result_array = env->NewObjectArray(2, object_array_class, nullptr);
            if (result_array == nullptr) {
                LOGE("Failed to create new object array for results."); return nullptr;
            }

            env->SetObjectArrayElement(result_array, 0, java_indices_flat);
            env->SetObjectArrayElement(result_array, 1, java_distances_flat);

            env->DeleteLocalRef(java_indices_flat);
            env->DeleteLocalRef(java_distances_flat);
            env->DeleteLocalRef(object_array_class);

            return result_array;

        } catch (const faiss::FaissException& e) {
            LOGE("FaissException during queryVectors: %s", e.what());
            env->ReleaseFloatArrayElements(java_query_vectors, query_vectors_ptr, JNI_ABORT);
            return nullptr;
        } catch (const std::exception& e) {
            LOGE("StdException during queryVectors: %s", e.what());
            env->ReleaseFloatArrayElements(java_query_vectors, query_vectors_ptr, JNI_ABORT);
            return nullptr;
        } catch (...) {
            LOGE("Unknown exception during queryVectors");
            env->ReleaseFloatArrayElements(java_query_vectors, query_vectors_ptr, JNI_ABORT);
            return nullptr;
        }
    }

    JNIEXPORT jboolean JNICALL
    Java_pe_brice_rag_vectorstore_faiss_FaissWrapper_destroyStore(JNIEnv *env, jobject thiz) {
        if (faiss_index != nullptr) {
            LOGI("Destroying Faiss index. Total vectors before delete: %lld, Dimension: %d", faiss_index->ntotal, faiss_index->d);
            delete faiss_index;
            faiss_index = nullptr;
            current_dimension = 0;
            LOGI("Faiss index destroyed.");
            return JNI_TRUE;
        } else {
            LOGI("Faiss index was not initialized or already destroyed.");
            return JNI_FALSE;
        }
    }

    JNIEXPORT jlong JNICALL
    Java_pe_brice_rag_vectorstore_faiss_FaissWrapper_getNTotal(JNIEnv *env, jobject thiz) {
        if (faiss_index != nullptr) {
            return faiss_index->ntotal;
        }
        LOGW("getNTotal called but index is not initialized.");
        return JNI_FALSE;
    }

    JNIEXPORT jboolean JNICALL
    Java_pe_brice_rag_vectorstore_faiss_FaissWrapper_isIndexTrained(JNIEnv *env, jobject thiz) {
        if (faiss_index != nullptr) {
            return faiss_index->is_trained ? JNI_TRUE : JNI_FALSE;
        }
        LOGW("isIndexTrained called but index is not initialized.");
        return JNI_FALSE;
    }

    JNIEXPORT jint JNICALL
    Java_pe_brice_rag_vectorstore_faiss_FaissWrapper_getDimension(JNIEnv *env, jobject thiz) {
        if (faiss_index != nullptr) {
            return faiss_index->d;
        }
        LOGW("getDimension called but index is not initialized.");
        return JNI_FALSE;
    }

    // --- 새로운 영구 저장 관련 함수들 ---

    JNIEXPORT jboolean JNICALL
    Java_pe_brice_rag_vectorstore_faiss_FaissWrapper_saveIndexNative(JNIEnv *env, jobject thiz, jstring java_path) {
        if (faiss_index == nullptr) {
            LOGE("Faiss index not initialized. Nothing to save.");
            return JNI_FALSE;
        }
        if (java_path == nullptr) {
            LOGE("Path for saving index is null.");
            return JNI_FALSE;
        }

        const char* path_cstr = env->GetStringUTFChars(java_path, nullptr);
        if (path_cstr == nullptr) {
            LOGE("Failed to get C string from java_path.");
            return JNI_FALSE;
        }

        try {
            LOGI("Saving Faiss index to: %s. Index ntotal: %lld, dimension: %d", path_cstr, faiss_index->ntotal, faiss_index->d);
            faiss::write_index(faiss_index, path_cstr);
            LOGI("Faiss index saved successfully to: %s", path_cstr);
            env->ReleaseStringUTFChars(java_path, path_cstr);
            return JNI_TRUE;
        } catch (const faiss::FaissException& e) {
            LOGE("FaissException during saveIndex: %s (path: %s)", e.what(), path_cstr);
            env->ReleaseStringUTFChars(java_path, path_cstr);
            return JNI_FALSE;
        } catch (const std::exception& e) {
            LOGE("StdException during saveIndex: %s (path: %s)", e.what(), path_cstr);
            env->ReleaseStringUTFChars(java_path, path_cstr);
            return JNI_FALSE;
        } catch (...) {
            LOGE("Unknown exception during saveIndex (path: %s)", path_cstr);
            env->ReleaseStringUTFChars(java_path, path_cstr);
            return JNI_FALSE;
        }
    }

    JNIEXPORT jboolean JNICALL
    Java_pe_brice_rag_vectorstore_faiss_FaissWrapper_loadIndexNative(JNIEnv *env, jobject thiz, jstring java_path) {
        if (java_path == nullptr) {
            LOGE("Path for loading index is null.");
            return JNI_FALSE;
        }

        const char* path_cstr = env->GetStringUTFChars(java_path, nullptr);
        if (path_cstr == nullptr) {
            LOGE("Failed to get C string from java_path for loading.");
            return JNI_FALSE;
        }

        faiss::Index* loaded_index = nullptr;
        try {
            LOGI("Attempting to load Faiss index from: %s", path_cstr);
            loaded_index = faiss::read_index(path_cstr);

            if (loaded_index == nullptr) { // read_index가 실패 시 nullptr을 반환할 수 있음 (혹은 예외 발생)
                LOGE("Failed to load Faiss index from: %s. read_index returned null.", path_cstr);
                env->ReleaseStringUTFChars(java_path, path_cstr);
                return JNI_FALSE;
            }

            // 기존 인덱스가 있으면 삭제
            if (faiss_index != nullptr) {
                LOGI("An existing index is in memory. Deleting it before loading the new one.");
                delete faiss_index;
                faiss_index = nullptr;
            }

            faiss_index = loaded_index;
            current_dimension = faiss_index->d; // 로드된 인덱스에서 차원 정보 업데이트
            LOGI("Faiss index loaded successfully from: %s. Ntotal: %lld, Dimension: %d, Is Trained: %s",
                 path_cstr, faiss_index->ntotal, faiss_index->d, faiss_index->is_trained ? "true" : "false");
            env->ReleaseStringUTFChars(java_path, path_cstr);
            return JNI_TRUE;

        } catch (const faiss::FaissException& e) {
            LOGE("FaissException during loadIndex: %s (path: %s)", e.what(), path_cstr);
            if (loaded_index != nullptr) delete loaded_index; // 예외 발생 시 로드된 인덱스 메모리 해제 시도
            env->ReleaseStringUTFChars(java_path, path_cstr);
            return JNI_FALSE;
        } catch (const std::exception& e) {
            LOGE("StdException during loadIndex: %s (path: %s)", e.what(), path_cstr);
            if (loaded_index != nullptr) delete loaded_index;
            env->ReleaseStringUTFChars(java_path, path_cstr);
            return JNI_FALSE;
        } catch (...) {
            LOGE("Unknown exception during loadIndex (path: %s)", path_cstr);
            if (loaded_index != nullptr) delete loaded_index;
            env->ReleaseStringUTFChars(java_path, path_cstr);
            return JNI_FALSE;
        }
    }

    JNIEXPORT jboolean JNICALL
    Java_pe_brice_rag_vectorstore_faiss_FaissWrapper_isIndexLoadedNative(JNIEnv *env, jobject thiz) {
        if (faiss_index != nullptr && faiss_index->is_trained) {
            // IndexFlatL2는 별도의 training이 필요 없으므로 is_trained는 항상 true일 것으로 예상됩니다.
            // 또한, ntotal > 0 이어야 실제 데이터가 있는 유효한 인덱스라고 판단할 수도 있습니다.
            // 여기서는 인덱스 포인터가 null이 아니고, trained 상태이면 로드된 것으로 간주합니다.
            LOGI("isIndexLoadedNative: Index is loaded and trained. Ntotal: %lld, Dimension: %d", faiss_index->ntotal, faiss_index->d);
            return JNI_TRUE;
        }
        if (faiss_index == nullptr) {
            LOGI("isIndexLoadedNative: Index is null.");
        } else if (!faiss_index->is_trained) {
            LOGI("isIndexLoadedNative: Index is not trained.");
        }
        return JNI_FALSE;
    }
} // extern "C"
