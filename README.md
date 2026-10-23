# Caption AI ✨

**Caption AI** is a serverless Android app that writes viral social-media captions, hashtags, post descriptions and bio ideas with AI. It is built for Pakistani and South Asian creators: you can describe your post in **English, Urdu (اردو), Roman Urdu or Hindi**, optionally attach a **photo**, and get ready-to-post content for Instagram, TikTok, Facebook, WhatsApp and LinkedIn.

> Package: `com.captionai.studio` · Version `1.1.0` (versionCode 3) · minSdk 24 · target/compileSdk 35

---

## Features

- **Photo-to-caption** – pick an image; a vision model reads it and writes captions that match.
- **Multi-language input and output** – describe the post in any language; choose English / Urdu / Roman Urdu output in Settings.
- **Platform, vibe, tone and length controls** – pill selectors for platform, content type (Selfie, Food, Travel…), tone and caption length.
- **Hashtags, description and bio** generated with every request.
- **Caption cards** – render any caption as a 1080×1350 gradient image (platform-themed) and share it.
- **Trending** – AI-synced list of ~25 "viral" captions, refreshable every 12 h.
- **Favorites and History** – stored locally in SQLite, available offline.
- **Free / Pro model** – 3 free generations per day (+ bonus from rewarded ads); Pro subscription removes limits and ads.
- **Dark / light theme**, splash screen, floating bottom navigation.

## How the AI layer works

```
 HomeScreen ──► ResultScreen ──► GeminiClient.generateCaptions()
                                      │
                         ┌────────────┴─────────────┐
                         ▼                          ▼
              1. GroqClient (primary)     2. Gemini REST (failover)
              gpt-oss-120b → gpt-oss-20b   gemini-2.5-flash
              vision: qwen (image input)   3 rotating keys, or user key
```

1. **Groq Cloud** (OpenAI-compatible chat API) is tried first.
2. If Groq is unconfigured, rate-limited or errors, the request fails over to **Google Gemini**, rotating through up to three built-in keys on 429/403.
3. Users may paste their own key in *Settings → Advanced* (`gsk_…` for Groq, `AIza…` for Gemini). It takes priority for the matching provider.
4. A client-side `RateLimiter` caps requests at 14 per minute.

## Tech stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose, Material 3, Navigation Compose 2.7.6 |
| AI | Groq Cloud REST + Google Gemini REST (plain `HttpURLConnection`, `org.json`) |
| Storage | `SQLiteOpenHelper` (favorites, history, trending) + `SharedPreferences` |
| Monetisation | Google AdMob (banner, native, interstitial, rewarded) + Play Billing 6.1 subscription |
| Build | Gradle 9.4.1, Android Gradle Plugin 9.2.1 |

## Project structure

```
app/src/main/kotlin/com/captionai/studio/
├── MyApp.kt              Application: initialises PrefsManager and AdManager
├── MainActivity.kt       Single activity, Compose host, restores purchases
├── ai/
│   ├── GeminiClient.kt   Orchestrator: caching, rate limit, prompts, Gemini calls and key rotation
│   └── GroqClient.kt     Groq chat-completions client and response parsing
├── screens/              Home, Result, Trending, Favorites, History, Settings, Navigation
├── storage/
│   ├── PrefsManager.kt   Settings, API key, daily counter, bonus gens, Pro flag
│   └── DatabaseHelper.kt SQLite tables: favorites, history, trending
├── ui/Theme.kt           Material 3 theme (purple #7D5FFF accent)
└── utils/
    ├── AdManager.kt      AdMob wrapper
    ├── BillingHelper.kt  Play Billing wrapper
    └── ImageUtils.kt     Caption-card rendering and sharing
```

More background lives in [`app/CONTEXT.md`](app/CONTEXT.md). Known problems and suggested improvements are in [`AUDIT.md`](AUDIT.md).

## Getting started

### Prerequisites
- Android Studio (recent) with JDK 17+ and Android SDK 35.
- A Groq API key and/or Google Gemini API key(s).

### Configure keys
Create `local.properties` in the repo root (it is git-ignored):

```properties
sdk.dir=/path/to/Android/sdk

# AI providers (at least one is needed)
GROQ_API_KEY=gsk_...
GEMINI_KEY_1=AIza...
GEMINI_KEY_2=AIza...
GEMINI_KEY_3=AIza...

# Optional: release signing
STORE_FILE=/path/to/keystore.jks
STORE_PASSWORD=...
KEY_ALIAS=...
KEY_PASSWORD=...
```

### Build and run
```bash
./gradlew assembleDebug        # debug APK
./gradlew installDebug         # install on a connected device
./gradlew bundleRelease        # signed AAB for Google Play (needs STORE_* values)
```

> ⚠️ Keys are compiled into `BuildConfig` and can be extracted from the APK. See `AUDIT.md` for the recommended proxy-backend fix.

## Free vs Pro

| | Free | Pro (`caption_ai_pro_monthly`) |
|---|---|---|
| Generations | 3 per day (+3 per rewarded ad) | Unlimited |
| Ads | Banner + interstitial every 3rd generation | None |

## Privacy

See [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md). Favorites, history and settings stay on-device; prompts and selected photos are sent to the AI providers to produce results.

## License

No license file is included yet. Add one before accepting contributions.
