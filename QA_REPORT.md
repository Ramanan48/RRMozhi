# RR Mozhi: QA test report

**Version:** 1.0 (versionCode 1) · **Package:** com.ramanan.mozhi · **Min / target Android:** 8.0 (API 26) / 14 (API 34)

## 1. What was tested and how

| Check | Method | Result |
|---|---|---|
| Kotlin code (all 5 files) | Compiled with the Kotlin compiler against stand-ins for the Android, Material and ML Kit classes | ✅ 0 errors |
| Tanglish → Tamil offline rules | Compiled and ran 12 sample sentences | ✅ Pass (see 4) |
| Hindi → English letters | Compiled and ran 18 sample sentences | ✅ Pass |
| XML (layout, manifest, themes, icons) | Parsed every file | ✅ Well-formed |
| Every view ID / string used in code exists | Cross-checked code against layout and strings | ✅ No missing, no unused |
| String formatting (%1$s) and apostrophes | Scanned strings.xml | ✅ OK |
| User flows and edge cases | Walked through step by step (section 3) | ✅ After fixes |
| Full Android build and on-phone run | Not possible here: this workspace can't reach Google's Android servers | ⚠️ Pending: do in Android Studio |

## 2. Bugs found and fixed

| # | Severity | Problem | Fix |
|---|---|---|---|
| 1 | High | Status bar icons (time, battery) were **dark on the dark teal bar**, so invisible in light mode | Status bar icons forced to white |
| 2 | Low | Status text could be given a `null` value, which can fail to compile with stricter Android library versions | Always sets an empty string instead |
| 3 | High | Changing the target language **while a translation was running** kept the old language | Target change now restarts the translation |
| 4 | Medium | On a failed re-translation, the **old result stayed on screen next to the error** | Old result hidden when a new translation starts |
| 5 | Medium | If speech recognition failed (no internet, nothing heard), **nothing happened** | Shows "Didn't catch that…" message |
| 6 | Medium | A **wrongly heard word couldn't be corrected** | Heard text is now editable before translating |
| 7 | Medium | Many phones have **no Malay voice**, so Listen failed | Falls back to the Indonesian voice (very close to Malay) |
| 8 | Medium | With no text-to-speech engine, Listen always said "still starting" | Clear message to install Speech Services by Google |
| 9 | Medium | Tamil-script text typed in Tanglish mode was **sent to the Tanglish converter** | Tamil-script lines are passed through unchanged |
| 10 | Low | No input limit, so very long text could fail | 1000-character limit with a live counter |
| 11 | Low | Keyboard focus stayed in the voice box after translating | Focus cleared |
| 12 | Low | Internal class named `Unit` clashed with a built-in Kotlin name | Renamed to `Syllable` |
| Earlier | High | Rotating the phone lost the result | Fixed in the previous round |
| Earlier | Medium | Top-left icon looked tappable but did nothing | Removed |

## 3. Flow checks (code walkthrough, after fixes; to be confirmed on a phone)

| Scenario | Expected | Status |
|---|---|---|
| Text · Tanglish | Target chips: Hindi / English / Malay. Tamil preview shown | ✅ |
| Text · English | Target chips: Hindi / Malay / Tamil | ✅ |
| Voice · Tamil | Targets Hindi / English / Malay | ✅ |
| Voice · Hindi / English / Malay | Tamil auto-selected; the spoken language hidden from targets | ✅ |
| Switch Voice (Hindi → Tamil) back to Text · Tanglish | Tamil target hidden, Hindi auto-selected | ✅ |
| Empty input → Translate | "Type something to translate" message | ✅ |
| Change "Speak in" language | Old heard text cleared, titles and hint updated | ✅ |
| First use of a language (online) | "Downloading … language pack" status, then result | ✅ |
| First use offline | Clear "connect to internet once" message | ✅ |
| Tanglish with no internet | Falls back to offline rules, no hang | ✅ |
| Double-tap Translate | Button disabled while busy, only one run | ✅ |
| Hindi result | Hindi script + English-letters line; Share sends both | ✅ |
| Listen with missing voice | Snackbar with Install button | ✅ |
| Rotate phone | Result and text stay | ✅ |
| Dark mode | Light-teal theme, readable text | ✅ |
| Select text in another app → RR Mozhi Translate | Opens in Text mode with the text filled in | ✅ |

## 4. Known limitations (not bugs)

- **Translation quality:** ML Kit is good for everyday sentences, but weaker than Google Translate online for long or formal text. Tamil ↔ Hindi/Malay goes through English internally.
- **Tanglish offline rules** handle simple spellings well (`nandri`, `vandhaan`, `enga irukka`). Loose spellings like `poren` come out slightly off offline, so use internet for the best Tanglish.
- **Online Tanglish** uses Google Input Tools, a free service without a formal guarantee. If it stops working, the app falls back to the offline rules automatically.
- **Needs Google Play services** (ML Kit and Google speech). It won't work on phones without Google, such as newer Huawei models.
- **Play Store:** publishing needs `targetSdk` 35 or higher, which also needs a small layout change for Android 15's full-screen (edge-to-edge) display. Installing the APK directly works as it is.
- Switching dark/light mode while the app is open clears the current result.

## 5. To finish on a real device (checklist)

1. Build in Android Studio (see README). Confirm **BUILD SUCCESSFUL**.
2. Install on a phone. Do one translation per language with internet on.
3. Test: Tanglish → Hindi, English → Tamil, Tamil voice → Malay, Hindi voice → Tamil, Listen on each result.
4. Turn on flight mode and repeat a text translation. It should still work.
