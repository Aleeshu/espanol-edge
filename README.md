# Español Edge

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**Español Edge** is a Spanish-first hybrid on-device + cloud AI language learning Android app adapted from [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery).

Run private Spanish conversations, camera object lessons, pronunciation coaching, and spaced-repetition flashcards **100% offline** with on-device Gemma / LiteRT-LM models — or optionally pair with a user-supplied **Gemini API key** for zero-download Hybrid Cloud AI (`Gemini 2.5 Flash` & `Gemini 2.5 Flash-Lite`).

---

## Core Spanish Learning Modules

* **Spanish Tutor Chat** (`LLM_CHAT`): Interactive voice/text conversations with 8 real-world roleplay scenarios (*Free Chat*, *Café*, *Travel*, *Restaurant*, *Shopping*, *Work & Interview*, *Friend & Slang*, *Doctor & Pharmacy*), clickable Spanish starter prompts, inline grammar corrections, dialect & CEFR prompt adaptation, and one-tap `+ Save Word` capture into your shared flashcard deck.
* **Camera Spanish Tutor** (`LLM_XR_LANGUAGE_TEACHER`): Point your camera at real-world objects and scenes, hear conversational Spanish explanations tailored to your active dialect and CEFR level, generate structured vocabulary cards with photo cutouts, and run multi-turn visual follow-ups using either on-device models or Hybrid Cloud Gemini.
* **Spanish Flashcards (SRS)** (`spanish_flashcards`): Shared spaced-repetition deck combining your starter deck (16 curated A1–B1 cards) and vocabulary saved from Camera Tutor, Tutor Chat, and Pronunciation Lab. Uses **SM-2 scheduling** (*Again*, *Hard*, *Good*, *Easy*), **1.0x & 0.7x slow Spanish TTS** matched to your selected accent, search/filter by source, manual card creation, and deck progress controls.
* **Pronunciation & Listening Lab** (`LLM_ASK_AUDIO`): Drill spoken Spanish with **Shadowing**, **Tricky Sounds** (`r/rr`, `ñ`, `ll/y`, `j/g`, `b/v`), and **Free Speaking** modes, CEFR-filtered practice phrases with 1.0x/0.7x TTS playback, microphone audio capture, and tailored AI pronunciation & rhythm feedback.

---

## Personalization & Hybrid AI Modes

Configure learning preferences and AI execution mode anytime in **Settings (⚙)**:

* **Spanish Dialects**: *Latin American* (`es-US`), *Mexican* (`es-MX`), *Spain (Castilian)* (`es-ES`), *Rioplatense* (`es-AR`), *Colombian* (`es-CO`).
* **CEFR Proficiency**: `A1`, `A2`, `B1`, `B2`, `C1`, `C2`.
* **Learning Toggles**: *Inline Grammar Corrections* and *Show English Hints* (or switch off for full Spanish immersion).
* **Hybrid On-Device + Cloud AI**:
  * **On-Device Mode (Offline)**: Download free phone LLMs, ASR, and TTS models directly inside the app (bundled offline allowlist + hosted `model_allowlists/1_0_24.json`).
  * **Cloud Mode (Instant)**: Paste a Gemini API key in Settings to unlock `Gemini 2.5 Flash (Cloud)` and `Gemini 2.5 Flash-Lite (Cloud)` with encrypted key storage (`x-goog-api-key` header) across Chat, Camera Tutor, and Pronunciation Lab.

---

## Free On-Device Phone, ASR & TTS Models

Español Edge bundles an offline fallback allowlist (`assets/model_allowlist.json`) and loads `model_allowlists/1_0_24.json` so free phone AI, speech recognition, and voice models are immediately available in Model Manager without requiring a paid API key:

### Phone LLMs (LiteRT-LM)
* **No Hugging Face login required**:
  * `Gemma-4-E2B-it` (2.4 GB — text + camera vision + audio, *Spanish Tutor Chat*, *Camera Tutor*, *Pronunciation Lab*)
  * `Gemma-4-E4B-it` (3.4 GB — higher-capacity text + camera vision + audio)
  * `Qwen2.5-1.5B-Instruct` (1.5 GB — lightweight multilingual text tutor)
  * `DeepSeek-R1-Distill-Qwen-1.5B` (1.7 GB — compact reasoning & conversation)
* **Free Hugging Face models (accept Gemma license)**:
  * `Gemma3-1B-IT` (557 MB — ultra-lightweight phone chat)
  * `Gemma-3n-E2B-it` (3.4 GB — text + vision + audio) & `Gemma-3n-E4B-it` (4.6 GB — text + vision + audio)

### Speech Recognition (ASR)
* `Parakeet TDT 0.6B ASR` (586 MB — open-weight, no HF login, supports Spanish `es-ES` + 24 languages on any 4 GB+ Android phone)
* `ML Kit ASR (Basic)` & `ML Kit ASR (Advanced)` (free system-managed Google Play Services / AICore speech recognition with Spanish `es-ES`)

### Text-to-Speech (TTS)
* `Kokoro 82M (TTS)` (281 MB — open-weight, no HF login, includes Spanish `es-ES` voices + 8 languages)
* `PocketTTS 100M (TTS)` (725 MB — open-weight, no HF login, includes Spanish `es-ES` voice `lola`)
* `Phonon 31M (TTS)` (83 MB — lightweight open-weight English TTS)
* `ML Kit TTS` (free on-device expressive TTS on supported Pixel & Galaxy phones with `es-ES` & `es-US`)

---

## Build & Test Instructions (Android)

From `Android/src`:

```powershell
# Run Spanish domain, prompt, SRS, and Gemini Cloud unit tests
.\gradlew.bat :app:testDebugUnitTest --tests "com.google.ai.edge.gallery.spanish.*"

# Build the debug APK (output: Android/src/app/build/outputs/apk/debug/app-debug.apk)
.\gradlew.bat :app:assembleDebug
```

Requirements: Android SDK (`Android/src/local.properties` pointing to your SDK), JDK 17+, Android 12+ (`minSdk = 31`).

---

## Attribution & License

Adapted from [google-ai-edge/gallery](https://github.com/google-ai-edge/gallery) by Google LLC.
Licensed under the **Apache License, Version 2.0**. See [LICENSE](LICENSE) for details.
