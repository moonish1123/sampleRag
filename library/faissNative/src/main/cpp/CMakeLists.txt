# CMake 최소 요구 버전 설정 (예: 3.22.1, 사용 가능한 버전에 맞게 조정)
cmake_minimum_required(VERSION 3.22.1 FATAL_ERROR)

# 프로젝트 이름 및 기본 언어 설정
project(SampleRagNative LANGUAGES CXX VERSION 1.11.0)

# C++ 표준 설정 (C++17 권장)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# --- Faiss 빌드 옵션 (Android용으로 간소화) ---
# 최적화 레벨 (arm 아키텍처에서는 "generic"이 안전)
option(FAISS_OPT_LEVEL "Set Faiss optimization level" "generic")
# LTO (Link-Time Optimization) 사용 여부
option(FAISS_USE_LTO "Enable Link-Time optimization" OFF)

# Android 빌드에 불필요하거나 지원되지 않는 Faiss 기능 비활성화
set(FAISS_ENABLE_GPU OFF CACHE BOOL "Disable GPU support" FORCE)
set(FAISS_ENABLE_PYTHON OFF CACHE BOOL "Disable Python extension" FORCE)
set(FAISS_ENABLE_EXTRAS OFF CACHE BOOL "Disable extras like benchmarks and demos" FORCE)
set(BUILD_TESTING OFF CACHE BOOL "Disable Faiss tests" FORCE)
set(FAISS_ENABLE_MKL OFF CACHE BOOL "Disable MKL, use OpenBLAS instead" FORCE)
# C API는 JNI 예제에서 직접 사용하지 않으므로 OFF로 설정 (필요시 ON으로 변경 가능)
option(FAISS_ENABLE_C_API "Build C API." OFF)

if (ANDROID_ABI STREQUAL "arm64-v8a" OR ANDROID_ABI STREQUAL "x86_64")
    add_definitions(-DANDROID_BIT64)
else()
    add_definitions(-DANDROID_BIT32)
endif()

# --- OpenBLAS 정적 라이브러리 설정 ---
# OpenBLAS 라이브러리 파일 경로를 설정합니다.
# 실제 파일 이름과 경로에 맞게 수정해주세요. (예: libopenblas.a 또는 libopenblas_arm64-v8a.a 등)
# 이 CMakeLists.txt 파일이 위치한 디렉토리 내의 lib 폴더에 있다고 가정합니다.
set(OPENBLAS_LIBRARY_NAME "libopenblas.a") # 사용자님이 마지막으로 공유한 파일 이름
set(OPENBLAS_LIBRARY_PATH "${CMAKE_CURRENT_SOURCE_DIR}/lib/${ANDROID_ABI}/${OPENBLAS_LIBRARY_NAME}")

if(NOT EXISTS "${OPENBLAS_LIBRARY_PATH}")
    message(FATAL_ERROR "OpenBLAS library not found at ${OPENBLAS_LIBRARY_PATH}. \n"
            "Please ensure the library exists at this path or update OPENBLAS_LIBRARY_PATH.")
endif()
message(STATUS "Using prebuilt OpenBLAS library: ${OPENBLAS_LIBRARY_PATH}")

# Faiss가 내부적으로 사용하는 BLAS/LAPACK 라이브러리 변수 설정
set(BLAS_LIBRARIES "${OPENBLAS_LIBRARY_PATH}" CACHE FILEPATH "Path to OpenBLAS library for BLAS")
set(LAPACK_LIBRARIES "${OPENBLAS_LIBRARY_PATH}" CACHE FILEPATH "Path to OpenBLAS library for LAPACK (same as BLAS for OpenBLAS)")
# Faiss에 BLAS 벤더를 OpenBLAS로 명시
set(BLA_VENDOR "OpenBLAS" CACHE STRING "Specify OpenBLAS as the BLAS vendor" FORCE)


# --- Faiss 핵심 라이브러리 빌드 ---
# Faiss 소스 코드가 현재 CMakeLists.txt 파일 위치 기준으로 "faiss" 서브디렉토리에 있다고 가정
set(FAISS_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/faiss")
if(NOT IS_DIRECTORY "${FAISS_SOURCE_DIR}")
    message(FATAL_ERROR "Faiss source directory not found at ${FAISS_SOURCE_DIR}.")
elseif(NOT EXISTS "${FAISS_SOURCE_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "Faiss source directory at ${FAISS_SOURCE_DIR} does not contain a CMakeLists.txt file. \n"
            "Please ensure you have the complete Faiss source code with its own CMakeLists.txt.")
endif()
# Faiss 서브디렉토리를 추가하여 Faiss 라이브러리(보통 'faiss' 타겟, libfaiss.a)를 빌드
add_subdirectory(${FAISS_SOURCE_DIR} ${CMAKE_BINARY_DIR}/faiss_build) # 빌드 결과물을 별도 디렉토리에 생성

# --- JNI 공유 라이브러리 빌드 ---
# JNI C++ 소스 파일 (예: cpp/jni/faiss_jni.cpp)
set(JNI_SOURCE_FILES "${CMAKE_CURRENT_SOURCE_DIR}/jni/faiss_jni.cpp")

if(NOT EXISTS "${JNI_SOURCE_FILES}")
    message(FATAL_ERROR "JNI source file not found at ${JNI_SOURCE_FILES}. \n"
            "Please create this file or update JNI_SOURCE_FILES path.")
endif()

# JNI 라이브러리 타겟 추가 (이것이 최종 .so 파일이 됩니다)
# 라이브러리 이름은 System.loadLibrary()에서 사용할 이름과 일치해야 합니다.
add_library(faiss_jni_lib SHARED
        ${JNI_SOURCE_FILES}
)

# JNI 라이브러리가 의존하는 라이브러리들 링크
target_link_libraries(faiss_jni_lib PRIVATE
        faiss               # Faiss 핵심 라이브러리 타겟 (add_subdirectory(faiss)에서 생성)
        # faiss_c           # 만약 FAISS_ENABLE_C_API가 ON이고, JNI에서 C API를 사용한다면 링크
        ${BLAS_LIBRARIES}   # OpenBLAS 라이브러리 (faiss 타겟이 이미 링크할 수 있지만, 명시적으로 추가)
        log                 # Android NDK 로깅 라이브러리
        # 필요시 다른 NDK 라이브러리 추가 (예: OpenMP 사용 시 -fopenmp 컴파일/링크 플래그 또는 omp 라이브러리)
)

# JNI 라이브러리가 Faiss 헤더 파일을 찾을 수 있도록 경로 추가
target_include_directories(faiss_jni_lib PRIVATE
        "${FAISS_SOURCE_DIR}" # Faiss 헤더 접근 경로
        # "${FAISS_C_API_SOURCE_DIR}" # C API 헤더 접근 경로 (필요시)
)


# --- 빌드 설정 요약 메시지 (빌드 시 확인용) ---
message(STATUS "--- Faiss for Android Configuration Summary (Top Level) ---")
message(STATUS "  CMAKE_CXX_STANDARD: ${CMAKE_CXX_STANDARD}")
message(STATUS "  FAISS_OPT_LEVEL: ${FAISS_OPT_LEVEL}")
message(STATUS "  FAISS_ENABLE_C_API: ${FAISS_ENABLE_C_API}")
message(STATUS "  OpenBLAS Library: ${OPENBLAS_LIBRARY_PATH}")
message(STATUS "  JNI Library Target: faiss_jni_lib")
message(STATUS "  JNI Source: ${JNI_SOURCE_FILES}")
message(STATUS "  Faiss Source Directory: ${FAISS_SOURCE_DIR}")
message(STATUS "-------------------------------------------------------")
