# AstraCut V6.0 — Clean Rebuild (6.0.1)

This is a cleaned and corrected rebuild of the AstraCut V6.0 engineering foundation prepared specifically for a fresh GitHub Actions build.

## Build failures fixed
1. **Java/Kotlin JVM mismatch**: the previous project left Java at the Android/Gradle default while the CI environment caused Kotlin to target JVM 21. The new project explicitly targets **JVM 17 for both Java and Kotlin**.
2. **Toolchain mismatch**: the previous source used **Kotlin 2.2.20 with AGP 8.13.0**, a combination outside the fully supported compatibility range. This rebuild uses **Kotlin 2.3.0 + AGP 8.13.0**, which is supported.
3. **CI hacks removed**: no JUnit injection, `--init-script`, `kotlin.jvm.target.validation=warning`, or source mutation is required.
4. **CI uses JDK 17 + Gradle 8.13** consistently.
5. **JUnit dependency is part of the project source** rather than being injected by CI.

## GitHub workflow
The included `.github/workflows/android-build.yml` only performs checkout, JDK/Gradle setup, build, tests, and APK upload. It does not rewrite the project during CI.

## Important status
This rebuild is intended to make the **source build path clean and reproducible**. It does **not** mean the video editor is finished. The current `MainActivity` is still an engineering-status screen, and the GPU/audio/codec/export/AI modules are foundations/contracts that require further implementation and real-device validation.

## Version
- Application version: **6.0.1**
- Application ID: `com.astracut.v60`
