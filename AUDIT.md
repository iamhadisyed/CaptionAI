# Caption AI – Code Audit

Static review of the whole project at commit `365470b`. Nothing was built or run (no Android SDK in the review environment), so each finding comes from reading the code. Severity: 🔴 critical · 🟠 high · 🟡 medium · 🟢 low.

---

## 1. Bugs and missing logic

| # | Sev | Where | Problem |
|---|-----|-------|---------|
| 1 | 🔴 | `SettingsScreen.kt:189` vs `GeminiClient.kt:279` | The language chip stores `"اردو"` but the prompt checks `"Urdu"`. Choosing Urdu silently produces **English** output. Same mismatch in `buildTrendingPrompt`. |
| 2 | 🔴 | `BillingHelper.launchPurchase` | Subscriptions need `setOfferToken(...)` from `productDetails.subscriptionOfferDetails`. It is missing, so `launchBillingFlow` fails or throws for the Pro product. There is no handling of the response code either. |
| 3 | 🔴 | `BillingHelper.restorePurchases` | The callback ignores `billingResult`. If the query fails (offline, service error), `purchases` is empty and Pro is **revoked** for paying users. |
| 4 | 🟠 | `BillingHelper` / `SettingsScreen` | After purchase, `prefs.isPro()` is read directly in composables with no observable state. The UI does not update until recomposition or restart. Pending purchases are not handled. |
| 5 | 🟠 | `HomeScreen.kt` (Generate button) | `consumeGeneration()` runs **before** the AI call. A failed request (network, 429, parse error) still burns one of the 3 free generations. |
| 6 | 🟠 | `GeminiClient.generateCaptions` | The cache compares only `CaptionRequest`; the image is not part of it. A different photo with the same text returns the **previous photo's result**. It also makes "regenerate" impossible, because identical input returns the cached answer. |
| 7 | 🟠 | `GeminiClient.pendingImageBase64` / `HomeScreen` | The static image is only reset on success and only set when an image is selected. After a failed request, or when the user later submits text only, the **old image is sent again**. |
| 8 | 🟠 | `GroqClient.callGroqChat` | `max_completion_tokens = 1000` is too low for `gpt-oss` reasoning models (reasoning tokens count). Responses can come back empty or truncated. The trending prompt asks for 25 captions and will be cut off. Gemini 2.5 "thinking" with `maxOutputTokens = 2048` has the same risk. |
| 9 | 🟠 | `GroqClient` (`VISION_MODEL`) | `qwen/qwen3.8-27b` could not be verified as a real Groq model ID. If it is wrong, every photo request fails over to Gemini after a wasted round trip. `response_format: json_object` is also not guaranteed on all vision models. |
| 10 | 🟠 | `AdManager` | The rewarded ad is never preloaded (`preloadRewarded` is commented out), uses a **Google test ID**, and `showRewardedAd` / `addBonusGenerations` are not called from any screen. The "+3 bonus" feature in the docs does not exist in the UI. Native ads are loaded nowhere. |
| 11 | 🟠 | `DatabaseHelper.onUpgrade` | A schema bump **drops all favorites, history and trending**. Users lose saved data on any update that changes the version. |
| 12 | 🟡 | `HomeScreen` | `3 - dailyCount` and `daily < 3` are hard-coded in two places. `dailyCount` is `remember`ed on the back-stack entry and ignores bonus generations (the label can show "0 left" while bonus gens exist). |
| 13 | 🟡 | `GeminiClient.parseCaptionResponse` | Uses `getJSONArray("captions")`/`("hashtags")`, so a missing key throws. It reads only `parts[0]` and does not check `finishReason`, `promptFeedback` or safety blocks. The Groq parser is more tolerant, so the two behave differently. |
| 14 | 🟡 | `GeminiClient.executeWithRotation` | A user key that is not `gsk_`/`xai-`/`grok_` is treated as Gemini, so a mistyped key is sent to Google. Rotation is triggered by substring-matching `"403"`/`"429"` in the message text. `retryWithModel` has no timeouts, and the `v1beta`/`v1` choice is ad hoc. |
| 15 | 🟡 | `RateLimiter` | A global `mutableListOf` with no synchronisation (called from `Dispatchers.IO`). It is a single shared per-device limiter, and a free user can hit it in a minute. The error is shown as a generic failure. |
| 16 | 🟡 | `HistoryScreen` | Queries `db.readableDatabase` directly from the UI and bypasses `DatabaseHelper`. Each caption is a separate history row that repeats the hashtags, so one generation shows as 3 items. History grows without a limit. |
| 17 | 🟡 | `FavoritesScreen` / `ResultScreen` | Duplicates can be saved. The "favorited" state lives only in a `remember`ed set and is lost on rotation. It is not checked against the DB. |
| 18 | 🟡 | `ResultScreen` | Generation runs in `LaunchedEffect(Unit)` with no ViewModel. Rotation, or the screen leaving and re-entering, re-triggers the work. Parameters travel as URL query strings, so a long context or a `&` / `?` inside it can break navigation (`Uri.encode` is applied on write but values are decoded twice). |
| 19 | 🟡 | `ResultScreen.ErrorStateModern` | The 403 message says "ensure your API Key allows `com.captionai.studio`". That is Gemini-only advice. The user sees raw exception strings otherwise. |
| 20 | 🟡 | `ImageUtils.shareCaptionCard` | Every share writes a new PNG to public `Pictures/CaptionAI` and never cleans up. Failure (`uri == null`) is silent. The bitmap is built on the main thread, and `FLAG_GRANT_READ_URI_PERMISSION` is set without `ClipData`. |
| 21 | 🟡 | `ImageUtils` | Fixed 68 px text: long captions overflow the 1350 px card. Urdu / RTL text is laid out with `ALIGN_CENTER` and the default font, so shaping may look wrong. |
| 22 | 🟡 | `PrefsManager.getLanguage` | Default `"English"`, but the UI stores native-script labels. Mapping between the displayed label and the value sent to the model is missing (see #1). |
| 23 | 🟢 | `HomeScreen` | `isLoading` is set true then reset immediately after `navigate`. The spinner never shows. Image-processing exceptions are swallowed and the request continues without the image. |
| 24 | 🟢 | `Components.kt` | Empty file (0 bytes). |
| 25 | 🟢 | `app/build.gradle` | `proguardFiles ... 'proguard-rules.pro'` points to a file that does not exist. This breaks as soon as `minifyEnabled true` is turned on. |

## 2. Security and privacy

| # | Sev | Problem | Fix |
|---|-----|---------|-----|
| S1 | 🔴 | **API keys are shipped in the APK** (`BuildConfig.GEMINI_KEY_*`, `GROQ_API_KEY`). Anyone can extract them with `apktool`/`strings` and drain your quota. | Put a thin proxy (Cloud Run / Cloudflare Worker / Firebase Function) in front of the AI providers, authenticate with Play Integrity / App Check, and ship no provider keys. |
| S2 | 🔴 | **Pro status is a boolean in SharedPreferences** (`is_pro`). It can be edited on a rooted device or restored via backup. The same goes for `extra_gens` and `daily_count` (clearing app data resets the free quota). | Treat Play Billing as the source of truth on every launch, verify purchases server-side, and enforce quota on the backend. |
| S3 | 🟠 | `android:allowBackup="true"` copies prefs (including the obfuscated user key, Pro flag, counters) to cloud backups. | Set `allowBackup="false"` or add `dataExtractionRules`/`fullBackupContent` exclusions. |
| S4 | 🟠 | The user's API key uses XOR `0x42` + Base64, which is trivially reversible and is not encryption. | Use `EncryptedSharedPreferences` or the Android Keystore. |
| S5 | 🟠 | Gemini key is passed in the URL query (`?key=`), so it can appear in logs and proxies. Error bodies are logged in full (`Log.e`). | Send it in the `x-goog-api-key` header; strip logs in release builds. |
| S6 | 🟡 | User text is interpolated straight into the prompt (`User context: "..."`). It allows prompt injection and breaks on embedded quotes. | Pass user text as a separate message / escape it, and validate the output schema. |
| S7 | 🟡 | `minifyEnabled false`: no code shrinking or obfuscation in release. | Enable R8, add `proguard-rules.pro`, keep rules for Billing/AdMob/org.json models. |
| S8 | 🟡 | The app sends photos to third parties (Groq/Google), but `PRIVACY_POLICY.md` mentions only Gemini text and has `[Current Date]` as a placeholder. Groq, images, AdMob consent and data deletion are missing. | Update the policy, add the Play Data-safety answers, and add a UMP/consent flow for ads (EEA/UK). |
| S9 | 🟢 | Release keystore settings are read from `local.properties` (good), but `*.jks` / `*.keystore` are not in `.gitignore`. `app/release/` build outputs are committed. | Add the patterns and remove `app/release/` from Git. |

## 3. Architecture and code quality

- **No ViewModel / state holder.** Screens call the DB, prefs and network directly, and static mutable state lives in `GeminiClient` (`lastResult`, `lastRequest`, `pendingImageBase64`). This is why rotation, back-navigation and the cache bugs above occur. Move to `ViewModel` + `StateFlow` + a repository.
- **God object.** `GeminiClient` holds both the orchestration and the Gemini REST client, and is named for one provider. Introduce an `AiProvider` interface (`GroqProvider`, `GeminiProvider`) and a `CaptionRepository` that applies fallback, caching and rate limiting.
- **Blocking I/O on the main thread.** SQLite reads in composables (`LaunchedEffect`/direct), and bitmap decode and compression in the button click handler. Use `Dispatchers.IO` (or Room with suspend / Flow).
- **Hand-rolled networking.** `HttpURLConnection` + `org.json` with no retries, backoff or error typing, and string matching on `"429"`. Use OkHttp/Ktor with `kotlinx.serialization` and a sealed error type.
- **Hard-coded values.** Colors (`0xFF7D5FFF` repeated everywhere instead of theme colors), the free-limit number, and all UI strings (no `strings.xml`, so no localisation despite the target audience).
- **Docs out of sync.** `app/CONTEXT.md` claims targetSdk 34, version 1.2.0, "Llama 3.3 / 3.2 Vision" in one place and `gpt-oss` in another, and Compose BOM 2024.01 (the build uses 2024.02). `GeminiClient`'s comments say Groq is "Llama". The `$25/month` price should also be double-checked; it is far above typical caption-app pricing.
- **Dependencies are old.** Compose BOM 2024.02, Navigation 2.7.6, Billing 6.1 (Play requires Billing 7+ for new apps/updates), AdMob 22.6 (current is 23.x). `kotlin.code.style` and several experimental `gradle.properties` flags (`android.newDsl=false`, `android.builtInKotlin=false`…) should be reviewed against AGP 9.
- **JVM target 1.8** is very old; use 17.
- **Zero tests.** There are `testImplementation` lines but no test sources. The JSON parsers, `PrefsManager.consumeGeneration`, the rate limiter and the DB helper are all easy to unit-test.

## 4. UX and product gaps

1. No way to **regenerate**, **edit** or **delete a single favorite/history item**; History shows 3 rows per generation.
2. No **offline / error UX** (no connectivity check, no retry button, raw API error text shown).
3. **Photo flow** shows only "Image Uploaded" with no thumbnail preview; no camera capture; no image-size validation.
4. **Accessibility:** icons use `contentDescription = null` widely; fixed `.sp` fonts with tiny text; contrast of grey text on light surfaces.
5. **Onboarding / consent:** none. First launch should explain the limits, privacy and ads.
6. **Free-tier clarity:** show bonus generations, a countdown to the daily reset, and the paywall with the actual Play-localised price rather than a fixed number.
7. **Share:** only the image card; add plain-text share per caption, platform deep links (open Instagram / WhatsApp), and character-count hints per platform.
8. **Trending** is just "AI-invented" text with no real trend data, and the UI copy ("fetched directly from AI") sets that expectation. Consider labelling it clearly or sourcing real trends.
9. **Pakistan-specific:** consider a local payment option (Play billing is limited there), Hindi / Urdu UI strings, and RTL layout checks (`supportsRtl` is on, but nothing is tested).

## 5. Suggested roadmap

**Before the next release (blockers)**
1. Fix the Urdu mapping (#1), the subscription offer token (#2) and the Pro-revocation bug (#3).
2. Charge quota only after a successful generation (#5); fix the image/cache bugs (#6, #7).
3. Raise token limits and verify the vision model ID (#8, #9).
4. Disable backups and update the privacy policy (S3, S8).

**Next**
5. Move AI calls behind a backend proxy and remove keys from the APK (S1, S2).
6. Introduce ViewModels, a repository and Room; add DB migrations (#11, architecture).
7. Wire up the rewarded ad or remove the feature from docs and copy (#10).
8. Add unit tests for parsers, quota logic and the fallback chain; set up CI (`./gradlew test lintDebug`).

**Later**
9. Enable R8, upgrade dependencies and Billing 7, move strings to resources.
10. Accessibility pass, onboarding and consent flow, analytics/crash reporting (Firebase Crashlytics).
