# Building and uploading a release to Google Play

This is a step-by-step guide for building a **signed** app bundle (`.aab`) that Google Play accepts.

## What was wrong before
1. The version code never changed, so Google Play rejected the upload ("version code already used").
2. The build was not signed because no keystore was configured.

Both are fixed: the version code goes up by 1 on **every release build** (stored in `version.properties`), and a release build now **stops with a clear message** if signing is not set up.

---

## One-time setup: create your signing key (keystore)

A keystore is a file that proves the app is yours. **Back it up (e.g. cloud drive + USB) and never lose the password.** Never commit it to Git.

### Option A: Android Studio (easiest)
1. **Build > Generate Signed App Bundle / APK...** > **Android App Bundle** > **Next**
2. Under *Key store path* click **Create new...**
3. Pick a file location, e.g. `C:\keys\captionai.jks`, choose a password, and fill in *Alias* (e.g. `captionai`), key password and your name/country. **OK**
4. Write down: file path, store password, alias, key password. Cancel the wizard (the settings below do the signing).

### Option B: command line
```
keytool -genkeypair -v -keystore C:/keys/captionai.jks -alias captionai -keyalg RSA -keysize 2048 -validity 10000
```

## One-time setup: tell Gradle about the key

Open (or create) `local.properties` in the project root (this file is ignored by Git) and add:

```properties
STORE_FILE=C:/keys/captionai.jks
STORE_PASSWORD=your-store-password
KEY_ALIAS=captionai
KEY_PASSWORD=your-key-password

# AI keys (needed so the released app can generate captions)
GROQ_API_KEY=gsk_...
GEMINI_KEY_1=AIza...
GEMINI_KEY_2=AIza...
GEMINI_KEY_3=AIza...
```
Use forward slashes `/` in the path, even on Windows.

## Every release

1. Get the latest code: `git pull`
2. (Optional) change `VERSION_NAME` in `version.properties` for a new user-visible version, e.g. `1.3.0`.
3. Build the bundle, either:
   - Android Studio: **Build > Generate Signed App Bundle / APK > Android App Bundle**, choose your keystore, **release**; or
   - Terminal: `./gradlew bundleRelease` (Windows: `gradlew.bat bundleRelease`)
4. The build prints `Release build: versionCode N`. The file is at
   `app/build/outputs/bundle/release/app-release.aab`
5. **Commit `version.properties`** after each release so the counter is saved:
   `git add version.properties && git commit -m "release: bump version code" && git push`
   (If you build on another computer without this file, the number could repeat.)

## Upload to Google Play Console

First time only:
1. Play Console > **Create app** (name: Caption AI, language, free/paid, accept policies).
2. Complete the **Dashboard** tasks: app access, ads (**Yes, contains ads**), content rating, target audience, **Data safety** (photos and text are sent to AI providers), privacy policy URL (host `PRIVACY_POLICY.md` publicly, e.g. GitHub Pages), app category.
3. Create the subscription: **Monetize > Subscriptions**, product ID `caption_ai_pro_monthly`, with a base plan.
4. Accept **Play App Signing** when asked (recommended). Google keeps the final signing key; your keystore becomes the *upload key*.

Each release:
1. **Testing > Internal testing** (fastest, up to 100 testers) > **Create new release**.
2. Upload `app-release.aab`.
3. Add release notes > **Save > Review > Start rollout**.
4. Add testers (their Gmail addresses), open the opt-in link on their phone, and test purchases/ads.
5. When happy: **Production > Create release**, upload the same or a newer bundle, and submit for review. New personal developer accounts may first need a closed test with 12+ testers for 14 days before production access.

## Common errors
| Message | Fix |
|---|---|
| "Version code N has already been used" | Build again (the number goes up automatically), then upload the new file. |
| "You uploaded a debug APK / the bundle is not signed" | Upload the `.aab` from `bundleRelease`, not a debug build. |
| "Release build is not signed: STORE_FILE is missing" | Add the four `STORE_*` / `KEY_*` lines to `local.properties`. |
| "Keystore was tampered with, or password was incorrect" | Check `STORE_PASSWORD` / `KEY_PASSWORD` / alias. |
| "Your APK or bundle is signed with a different key" | You must always use the same keystore for this app (or reset the upload key in Play Console > App integrity). |
