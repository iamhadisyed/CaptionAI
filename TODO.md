# Caption AI – Local To-Do List

Work through this on your machine. Details for each finding are in [`AUDIT.md`](AUDIT.md).

## 1. First, verify the changes (10 min)
- [ ] `git fetch && git checkout claude/brave-gates-wt9jah`
- [ ] Create `local.properties` (see README) with `GROQ_API_KEY` / `GEMINI_KEY_*`
- [ ] `./gradlew testDebugUnitTest assembleDebug` (CI already passes on the PR)
- [ ] Run on a device: generate with text, with a photo, in Urdu (اردو), tap Regenerate, hit the daily limit
- [ ] **Build and smoke-test a release build** (`./gradlew assembleRelease`). R8 shrinking was turned on and has not been tested.

## 2. Before publishing to Google Play
- [ ] AdMob: add the app as "not listed in a store", create a **Rewarded** ad unit, paste its ID into `REWARDED_AD_UNIT` in `AdManager.kt`
- [ ] After publishing: link the app to the Play listing in AdMob (package `com.captionai.studio`)
- [ ] Play Console: declare ads, fill Data safety (photos sent to AI providers), publish `PRIVACY_POLICY.md` at a public URL
- [ ] Create the subscription `caption_ai_pro_monthly` with a base plan, then test the purchase on a licensed tester account
- [ ] Test the purchase flow, restore purchases, and a pending payment
- [ ] Decide the Pro price (older docs said $25/mo, old UI said $5/mo; the app now shows the Play price)

## 3. Still open from the audit
| Priority | Item | Audit ref |
|---|---|---|
| High | Move AI calls behind a small backend proxy so no provider keys ship in the APK; enforce Pro/quota server-side | S1, S2 |
| High | Encrypt the custom API key (`EncryptedSharedPreferences` / Keystore) | S4 |
| Medium | ViewModels + repository; move SQLite to Room with migrations | Architecture |
| Medium | Consent (UMP) flow for ads, onboarding screen | UX, S8 |
| Medium | Caption card: delete old PNGs, scale long text, test Urdu/RTL rendering | #20, #21 |
| Low | Upgrade dependencies (Billing 7+, Compose BOM, AdMob 23, Navigation) | Architecture |
| Low | Move UI strings to `strings.xml`, accessibility pass (content descriptions, contrast) | UX |
| Low | Crash reporting / analytics (e.g. Firebase Crashlytics) | Roadmap |
| Low | Add more unit tests (parsers, quota logic, fallback chain) | Architecture |

## 4. Ideas to grow the app
- Saved "brand voice" presets (tone, hashtags, language) per user
- Per-platform character limits and hashtag-count hints
- Scheduled "caption of the day" notification
- Hindi and Urdu UI strings, RTL layout checks
- Local payment options for Pakistan
- Share directly to Instagram / WhatsApp
