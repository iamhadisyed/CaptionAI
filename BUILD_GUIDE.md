# Caption AI – Build and Upload Guide (Windows)

Step-by-step guide for building the signed release and uploading it to Google Play.
It uses the real folders on this machine. **Never put passwords in this file or in Git.**

| Item | Location |
|---|---|
| Project folder | `D:\CaptionAI` |
| Signing key (keystore) | `D:\captionAIkey\captionai.jks` (keep a backup copy elsewhere) |
| Public certificate for Google | `D:\captionAIkey\upload_certificate.pem` |
| Key alias | `captionai` |
| Settings file (ignored by Git) | `D:\CaptionAI\local.properties` |
| Version counter (saved in Git) | `D:\CaptionAI\version.properties` |
| **File to upload to Google Play** | **`D:\CaptionAI\app\build\outputs\bundle\release\app-release.aab`** |
| Store icon (512x512) | `D:\CaptionAI\store-assets\play-store-icon-512.png` |

Package name: `com.captionai.studio` · App in Play Console: **CaptionAI - Viral Captions**

---

## Part 1 – Current status (what is done, what is waiting)

- [x] Signed release bundle builds successfully (`BUILD SUCCESSFUL`, `jar verified`).
- [x] Version code goes up by 1 on every release build (last built: **11**).
- [x] Upload key reset **requested** in Play Console (App signing page shows "pending request").
- [ ] **Waiting for Google's approval email.** Do **not** upload the bundle before it arrives. It would be rejected, because Google still has the old upload key on file.
- [ ] Fix the **Misleading Claims** rejection: store listing name must be `Caption AI` and the icon must be the one in `store-assets\play-store-icon-512.png`.
- [ ] Fix the **Invalid Privacy Policy** rejection: publish the privacy policy on a public web page with a real contact email and paste its address in Play Console.

### Key fingerprints (public, for checking only)

| Key | SHA-1 |
|---|---|
| Old upload key (what Google has now) | `CE:C1:8A:C0:67:07:09:C7:66:C3:47:3E:9C:44:C9:67:C9:B2:59:61` |
| **New upload key (captionai.jks)** | `35:F5:1F:8D:B0:72:19:0C:CD:B0:4A:B4:5F:CE:40:37:60:7E:15:87` |

After Google approves the reset, the **Upload key** card on the App signing page should show the **new** SHA-1 (`35:F5:1F:8D...`).

---

## Part 2 – Check that the key reset was accepted

1. Wait for the email from Google Play about your upload key reset (check Spam too).
2. Play Console > open the app > **Protected with Play** > **Play Store protection** row > **Manage Play app signing**.
3. In the **Upload key** card, the message "pending request" is gone, and the **SHA-1** is `35:F5:1F:8D...`.
4. If it still shows `CE:C1:8A:C0...`, the reset is not finished yet. Wait more. Do not upload.

---

## Part 3 – Build the signed bundle (every release)

Open **Command Prompt** (CMD) in the project folder:

```
cd /d D:\CaptionAI
git checkout main
git pull origin main
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
gradlew.bat bundleRelease
```

Success looks like this in the output:

```
Release build: versionCode 12, versionName 1.2.0
...
BUILD SUCCESSFUL
```

The file is created at:

`D:\CaptionAI\app\build\outputs\bundle\release\app-release.aab`

**Which version should I upload?** The bundle already built has version code **11**. If you change anything in the app, or the build was made before you changed settings, rebuild: the number becomes 12 automatically. Always upload the newest file. Google needs a version code higher than every earlier upload (earlier uploads were 1 and 2).

Optional check that the file is signed:

```
"C:\Program Files\Android\Android Studio\jbr\bin\jarsigner.exe" -verify app\build\outputs\bundle\release\app-release.aab
```

`jar verified.` is the line you want. The long list of warnings after it is normal.

Save the new version number to Git:

```
git add version.properties
git commit -m "release: version code bump"
git push origin main
```

---

## Part 4 – Upload to Google Play Console

1. Go to https://play.google.com/console and open **CaptionAI - Viral Captions**.
2. Left menu: **Test and release > Testing > Internal testing**.
3. Click **Create new release**.
4. Under **App bundles**, click **Upload** and choose `D:\CaptionAI\app\build\outputs\bundle\release\app-release.aab`.
5. Wait for the green tick and the version code (for example `11 (1.2.0)`).
6. Add **Release notes**, for example: `Ad consent, regenerate button, bug fixes.`
7. Click **Next**, review any warnings, then **Save** and **Start rollout to Internal testing**.
8. Add testers: **Testers** tab > create an email list > add your Gmail > Save. Open the **opt-in link** on your phone and install.
9. Test: caption generation, photo captions, daily limit, ads, consent form, Pro purchase.

When everything works, go to **Production > Create new release**, add the same bundle (or a newer one), and **Send for review**.

---

## Part 5 – Fix the two policy rejections (Play Console)

### A. Misleading Claims ("App store listing mismatch")
Google found that the installed app icon and name differ from the store listing.

1. **Grow users > Store presence > Store listings > Edit default listing**.
2. **App name:** exactly `Caption AI` (same as the name shown under the icon on the phone).
3. **App icon:** upload `D:\CaptionAI\store-assets\play-store-icon-512.png`. It is rendered from the app's own launcher icon.
4. Check screenshots show real app screens only. Check every other listing (custom or translated) too.
5. **Save**. (When everything below is fixed, go to **Publishing overview** and **Send changes for review**.)

### B. Invalid Privacy Policy
1. The policy text is in `PRIVACY_POLICY.md`. It must have a **real contact email** and the **developer name** (replace the placeholder `[Your Email Address]`).
2. Publish it as a normal public web page (not a PDF and not a login page). Easy options: GitHub Pages or a free Google Sites page.
3. Play Console > **Policy and programs > App content > Privacy policy** > paste the web address > Save.
4. Also check **Data safety** matches the app: photos and text are sent to AI providers (Groq / Google Gemini), ads use device identifiers (AdMob).

### C. Other App content items to check
**Ads: Yes** · **Target audience: not for children** · **Content rating** · **App access** (no login needed) · **Data safety** · **Subscription** product ID `caption_ai_pro_monthly` with a base plan.

---

## Part 6 – Common errors

| Message | What to do |
|---|---|
| "Your APK or Android App Bundle is signed with the wrong key" | The upload key reset is not approved yet (Part 2). Wait, then upload again. |
| "Version code N has already been used" | Rebuild (Part 3). The number goes up automatically. |
| "Release build is not signed: STORE_FILE is missing" | Add the four `STORE_FILE`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` lines to `local.properties`. |
| "Keystore was tampered with, or password was incorrect" | Check `STORE_PASSWORD` and `KEY_PASSWORD` in `local.properties`. |
| "Keystore file not found" | Check `STORE_FILE=D:/captionAIkey/captionai.jks` (forward slashes). |
| Build fails with a Java error | Run `set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"` first in the same CMD window. |
| App not accepted because of ads / data safety | Fix the **App content** items in Part 5C. |

---

## Part 7 – Safety rules

- **Back up `D:\captionAIkey\captionai.jks` and its password** in at least two places. Do not put them in Git or send them to anyone.
- `local.properties` holds your passwords and AI keys. It is ignored by Git. Never share it. If AI keys were ever pasted somewhere public, **create new keys** and delete the old ones.
- Use test ads or a test device while developing. Never tap your own live ads.
- Always build with the same key for this app from now on.
