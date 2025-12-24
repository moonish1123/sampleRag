# Migration Action Plan

## Goal
Spin up a standalone Android sample app that exercises the FAISS-backed RAG features currently located under `app/src/main` while preserving the exact JNI/NDK toolchain and network stack (Retrofit + OkHttp).

## Status
- ✅ Steps 1–5 completed: Gradle wrapper + `app` module scaffolded inside `sample_rag/`, native `cpp/` tree copied verbatim, and all `net.daum.android.mail.rag` sources migrated under `pe.brice.rag`.
- ✅ Step 6 addressed: Retrofit 2.11, OkHttp 4.12 BOM, Gson converter, and mediapipe dependencies declared in `app/build.gradle.kts`.
- ✅ Step 7 partially addressed via lightweight stubs (`RagApplication`, `AccountManager`, `PreferenceUtils`, etc.) so the sample compiles without the rest of the host app.
- ▶️ Steps 8–12 pending: build-and-run validation, native build verification, and sample test scenarios remain TODO once APIs are wired into a UI surface.

## Sequence
1. **Scaffold project root** – Inside `sample_rag/`, create an Android Gradle project (`settings.gradle`, top-level `build.gradle.kts`) targeting compile/target SDK 35 and Kotlin JVM 17. Configure the namespace prefix as `pe.brice.rag`. Keep the Gradle wrapper local to this project.
2. **Create `app` module** – Mirror the existing module name for simplicity. Enable view binding + Compose, and turn on `externalNativeBuild` referencing `src/main/cpp/CMakeLists.txt`. Copy the existing `ndk` block (ABI filters + cmake flags) from `app/build.gradle.kts`.
3. **Bring over native layer** – Copy `app/src/main/cpp` (including `cmake`, `faiss`, `jni`, `lib`) wholesale into `sample_rag/app/src/main/cpp`. Preserve relative paths so `CMakeLists.txt` keeps working. Confirm `jniLibs` paths include both `libs` and `src/main/cpp/jni/jniLibs`.
4. **Port Kotlin packages** – Under `sample_rag/app/src/main/kotlin`, create `pe/brice/rag`. Copy everything from `app/src/main/kotlin/net/daum/android/mail/rag/**` and adjust package declarations/imports to `pe.brice.rag...`. Keep supporting classes (e.g., `vectorstore`, `splitter`, `llm`).
5. **Identify shared dependencies** – Track any upstream helpers referenced by the RAG code (e.g., vector store consumers). If a class lives outside `rag/**`, either copy it or stub minimal interfaces so the sample compiles.
6. **Add networking stack** – In module deps, include Retrofit 2.x bundle, OkHttp BOM + client/logging, Gson (or Moshi) mirroring the original implementation so serializers stay compatible.
7. **Thread/config utilities** – Wire any required coroutines, Room, or WorkManager pieces if the RAG layer references them. Prefer stripping unused features to keep the sample lean.
8. **Set up DI / entry points** – Provide a simple `MainActivity` that hosts one Fragment/Compose screen to trigger vector store indexing + querying. Inject the RAG services manually for now; no need to port the app-wide DI container.
9. **Configure assets + models** – If the RAG flow expects on-device assets (e.g., embedding models, SQL seeds), copy the relevant files under `app/src/main/assets` and update file paths/constants.
10. **Add build/test tasks** – Ensure `./gradlew assembleDebug`, `./gradlew connectedDebugAndroidTest`, and `./gradlew lintDebug` succeed. Document these commands inside `agents.md`.
11. **Verify native builds** – Run `./gradlew :app:externalNativeBuildDebug` or a full `assemble` to confirm FAISS compiles. Capture any missing NDK toolchain vars in `local.properties` (e.g., `ndkVersion`, `cmakeVersion`).
12. **Create sample RAG scenarios** – Populate instrumentation or JVM tests that hit the new `pe.brice.rag` services using fake backends. This ensures regressions are caught before reintegrating with larger products.

## Dependency Checklist
- Retrofit 2.x: core, Gson converter, and Rx/coroutine adapters matching the original feature.
- OkHttp: BOM + `okhttp`, `logging-interceptor`, and any mock server utilities needed for tests.
- Gson: align with the version declared in `app.google.gson` to avoid serialization drift.
- Coroutines + Lifecycle: include `kotlinx-coroutines-android` if any RAG flows stay asynchronous.
- AndroidX Test stack: JUnit4, Espresso core, and `androidx.test.runner` for instrumentation.
- NDK/CMake: match the versions defined in the parent `gradle.properties` so FAISS builds consistently across repos.

## Deliverables
- Working Android Studio project under `sample_rag/`.
- Documented commands + guidelines in `sample_rag/agents.md`.
- Verified JNI build output + smoke-tested RAG call path.
