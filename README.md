# RR Mozhi (Android translator)

A Tamil-first translator app with two modes.

| Mode  | You give                                | You get                               |
|-------|-----------------------------------------|---------------------------------------|
| Text  | Tanglish (e.g. `naan veetuku poren`)    | Hindi, English or Malay               |
| Text  | English                                 | Hindi, Malay or Tamil                 |
| Voice | Spoken Tamil                            | Hindi, English or Malay               |
| Voice | Spoken Hindi, English or Malay          | Tamil (picked automatically), or the other two |

- **Hindi results** show in Hindi script *and* in English letters underneath (नमस्ते आप कैसे हैं → *namaste aap kaise hain*). Malay and English are already in English letters.
- Every result can be **read aloud**, **copied** or **shared**.
- In Voice mode the heard text can be **edited** before translating, if the phone misheard a word.
- Select text in any app and choose **RR Mozhi Translate** from the pop-up menu to open it here.

## How it works

- **Translation:** Google ML Kit on-device translation. Each language pack (Tamil, Hindi, Malay; about 30 MB each) downloads once, the first time you use it. After that, translation works offline. Needs Google Play services on the phone.
- **Tanglish:** first changed into Tamil script, then translated. With internet it uses Google Input Tools transliteration, which is the most accurate. Without internet it uses built-in spelling rules. The Tamil version is shown under the input box so you can check it.
  - Offline spelling tips: `L` = ள, `N` = ண, `R` = ற, `zh` = ழ, `ee` = ீ, `oo` = ூ, `E`/`O` = long ே/ோ.
- **Voice input:** the phone's Google speech recogniser in the chosen language (ta-IN, hi-IN, en-IN, ms-MY). For offline voice, download that language's offline speech pack in the Google app.
- **Read aloud:** the phone's text-to-speech. If a voice is missing, tap **Install**. Malay falls back to the Indonesian voice when no Malay voice is installed.

## Build the APK

### Option A: Android Studio (recommended)
1. Install **Android Studio** (free) from developer.android.com/studio.
2. Unzip `RRMozhi.zip`, then in Android Studio choose **File → Open** and select the `RRMozhi` folder.
3. Wait for **Gradle sync** to finish (first time 5–15 min, needs internet). If it asks to install **Android SDK 34** or accept licences, click yes.
   - If it offers an "AGP Upgrade Assistant", you can skip it. The project builds as it is.
4. **Run on your phone:** on the phone, turn on *Developer options → USB debugging*. Connect by USB, pick the phone at the top of Android Studio and press **Run ▶**.
5. **Or make an APK file:** **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. Click **locate** in the pop-up. The file is `app/build/outputs/apk/debug/app-debug.apk`. Copy it to the phone and open it (allow "Install unknown apps").

### Option B: GitHub (no Android Studio needed)
1. Create a new GitHub repository and upload everything inside the `RRMozhi` folder (including the hidden `.github` folder).
2. Open the **Actions** tab. The **Build APK** workflow runs by itself (about 5–8 min).
3. Open the finished run and download **RR-Mozhi-debug-apk** under *Artifacts*. Unzip it and install `app-debug.apk` on the phone.

### First run on the phone
1. Open **RR Mozhi** with internet on.
2. Do one translation in each language you need. The language packs download at this point, and the status line shows progress.
3. After that, text translation works offline.

## Requirements
Android 8.0 (API 26) or newer, with Google Play services. Internet is needed for the first language-pack downloads, online Tanglish conversion, and (unless offline packs are installed) voice input.

## Project layout
```
app/src/main/java/com/ramanan/mozhi/
  MainActivity.kt           Screen logic, voice input, read aloud, copy/share
  TranslationEngine.kt      ML Kit translation and language-pack downloads
  TanglishTransliterator.kt Tanglish → Tamil (online and offline)
  HindiRomanizer.kt         Hindi script → English letters
  Lang.kt                   Language list
app/src/main/res/layout/activity_main.xml   Screen layout
QA_REPORT.md                Test report
```
