# Caption AI — Master Project Context for Anti Gravity

## READ THIS FIRST
This file is the single source of truth for the Caption AI Android project.
Read every section before writing any code. Do not assume anything that is not written here.

---

## What This App Does

Caption AI is an Android app that generates viral social media captions, hashtags, and bio suggestions using Google Gemini AI. It is fully serverless — no backend, no web hosting. Everything runs on-device.

**Target users:** Pakistani and South Asian users who post on Instagram, TikTok, Facebook, WhatsApp.

**Key differentiator:** Users can type their photo description in ANY language — English, Urdu (اردو), Roman Urdu, Hindi — and the AI understands all of them natively. Output language is separately selectable in Settings.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 (2026 AI Era Design) |
| Navigation | Navigation Compose 2.7.6 |
| AI | Google Gemini 1.5 Flash (REST API) |
| Local DB | SQLite via SQLiteOpenHelper |
| Preferences | SharedPreferences via PrefsManager singleton |
| Ads | Google AdMob (Banner, Interstitial, Rewarded) |
| Billing | Google Play Billing 6.1.0 (Monthly Subscription $25/mo) |
| Build | Gradle, minSdk 24, targetSdk 34, Compose BOM 2024.01.00 |

---

## Package Structure

All Kotlin files live under: `app/src/main/kotlin/com/captionai/studio/`

```
com.captionai.studio/
├── MyApp.kt                    ← Application subclass (PrefsManager.init, AdManager.initialize)
├── MainActivity.kt             ← Single activity, Compose entry point, BillingHelper.restorePurchases()
│
├── screens/
│   ├── Screen.kt               ← Sealed class with route constants
│   ├── Navigation.kt           ← NavHost + BottomNavigationBar
│   ├── HomeScreen.kt           ← Modern 2026 AI UI — Image upload, platform, tone, daily limit (3/day)
│   ├── ResultScreen.kt         ← Results display with save/copy/share and styled image card generation
│   ├── TrendingScreen.kt       ← AI-synced trending captions
│   ├── FavoritesScreen.kt      ← Saved captions
│   ├── HistoryScreen.kt        ← Browse past generations
│   └── SettingsScreen.kt       ← Language, API Key override, Dark mode, Subscriptions
│
├── ai/
│   └── GeminiClient.kt         ← REST calls, Rate limiter, Prompt builders (Multi-lang support)
│
├── storage/
│   ├── PrefsManager.kt         ← SharedPreferences: API Key, Daily count (Limit 3), Pro status
│   └── DatabaseHelper.kt       ← SQLite: favorites, history, trending tables
│
└── utils/
    ├── AdManager.kt            ← AdMob implementation
    ├── BillingHelper.kt        ← Monthly subscription handling ($25/mo)
    └── ImageUtils.kt           ← Styled image card sharing logic
```

---

## Files That Are DONE (Already Written)

The following files are complete and modernized:

- `HomeScreen.kt` — Modern UI with daily generation count visibility (3 per day).
- `ResultScreen.kt` — Shows captions, hashtags, bio; support for generating image cards.
- `TrendingScreen.kt` — Cloud-synced trending captions.
- `FavoritesScreen.kt` — Personal collection of saved captions.
- `HistoryScreen.kt` — Offline browsing of past creations.
- `SettingsScreen.kt` — Feature-rich settings with optional Gemini Key override and monthly subscription.
- `GeminiClient.kt` — Handles AI calls with a default built-in feature key + user override support.
- `Theme.kt` — Premium 2026 AI Era styling (Modern typography, frosted surfaces, deep purple accents).

---

## Subscription Model

- **Model:** Monthly Subscription
- **Price:** $25 / month
- **Benefits:** 
    - Unlimited generations (No daily limit)
    - High-priority AI access
    - No ads
    - Premium styled image cards

---

## Gemini API Key Logic

Caption AI provides a **built-in feature key** for all users. 
- If a user wants to use their own quota (e.g., higher rate limits), they can add their own Gemini API key in **Settings**.
- The app will prioritize the user's key if provided, otherwise it falls back to the default feature key.

---

## Free vs Pro Logic

| Condition | Free | Pro |
|-----------|------|-----|
| Daily limit | 3 generations per day | Unlimited |
| Bonus generations | Can earn +3 by watching rewarded ad | N/A |
| Ads shown | Banner + Interstitial every 3rd gen | No ads |
| API Key | Default Feature Key | Default or User Override |

---

## Design Philosophy (2026 AI Era)

- **Typography:** Heavy black headlines (`FontWeight.Black`), clean body text with generous line heights.
- **Colors:** Vibrant Purple (`#7D5FFF`) primary accent, Light Bluish background (`#F5F7FF`), and premium Dark mode (`#0A0A12`).
- **Surfaces:** Rounded corners (24dp - 32dp), subtle shadows, and frosted/glassmorphism effects.
- **UI Elements:** Pill-shaped selections, modern icons, and clearly visible daily limits.

---

*Last updated: Caption AI v1.2.0 — Subscription and Modern Design Update*
