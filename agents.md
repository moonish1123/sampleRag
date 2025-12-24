# Sample RAG App Guidelines

## Purpose & Scope
This mini-project isolates the FAISS-powered RAG stack so we can iterate outside the original mail client. Treat `sample_rag/` as a fresh Android root that only contains the RAG feature area plus the JNI/NDK bindings it needs.

## Project Structure
- `app/src/main/kotlin/pe/brice/rag/**` – Kotlin RAG layers (LLM adapters, splitters, vector stores).
- `app/src/main/cpp/**` – Existing FAISS JNI bridge copied verbatim from the legacy source repo.
- `app/src/main/assets/` – Local models or seed corpora referenced by the RAG pipelines.
- `docs/` (optional) – Architecture notes, API contracts, or benchmarking logs.

## Build & Test Commands
Run everything with the module-local wrapper:
- `./gradlew assembleDebug` – Build APK and trigger native compilation.
- `./gradlew testDebugUnitTest` – Execute JVM/unit tests targeting the RAG services.
- `./gradlew connectedDebugAndroidTest` – Run instrumentation checks on an emulator/device.
- `./gradlew lintDebug` – Ensure baseline Android lint passes before pushing.

## Coding & Packaging
- Keep all packages rooted under `pe.brice.rag` to avoid namespace clashes with other hosts.
- Match Kotlin style from the parent repo (4 spaces, explicit visibility, PascalCase types).
- When copying files from the legacy `net.daum.android.mail.rag` tree, only adjust package/import lines; leave logic untouched until after compilation succeeds.

## Dependency & NDK Notes
- Add Retrofit (`com.squareup.retrofit2:*`), OkHttp BOM (`com.squareup.okhttp3:okhttp-bom`), and the Gson converter so existing API clients remain drop-in.
- Mirror the original `externalNativeBuild`/`ndk` blocks plus `jniLibs` dirs, and pin the same `ndkVersion`/`cmakeVersion` in `gradle.properties` or `local.properties`.
- Keep `CMakeLists.txt` and `faiss` sources untouched; future edits should happen upstream and then be mirrored here to avoid divergence.

## Workflow Expectations
1. Follow the steps in `action.md` sequentially; check off each section once complete.
2. After every major copy/refactor, run `assembleDebug` to catch JNI or package issues early (pending first run).
3. Document any deviations (new deps, config flags) directly in this file so future agents can repeat the setup without rediscovery. Current deltas: lightweight `RagApplication`, `AccountManager`, `PreferenceUtils`, and `LogUtils` stubs replace the heavy app-level dependencies so the sample builds independently.
4. 모든 상태 보고 및 답변은 반드시 한글로 작성한다.
