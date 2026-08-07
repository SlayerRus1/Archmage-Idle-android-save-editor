# Archmage Save Editor

An Android app for decrypting, editing, and re-encrypting **Archmage Idle**
(`com.topcog.archmageidle`) save files exported as `.txt`.

## Save file format

Archmage Idle saves are plain text files that look like:

```
AT1:OlAVDR8SDgo6W1leXEIQUUJGLi5LMhsMBk9bRVR6Sk1XTUIQV1NbHR1cMwEKBwNDXUdlV1BB...
```

- `AT1:` is a fixed magic header.
- Everything after it is Base64.
- The Base64-decoded bytes are the UTF-8 JSON save data, XOR'd byte-by-byte
  against a fixed, repeating 21-byte ASCII key: `ArchmageTycoon2026xK9`
  (a simple stream cipher — the same operation encrypts and decrypts).

This was reverse-engineered directly from sample save files (comparing
ciphertext across multiple saves showed a ~92% byte-identical structure with
localized differences, which is the signature of a repeating-key XOR cipher
applied to mostly-similar JSON; a classic column-wise key-length/frequency
attack recovered the 21-byte key and confirmed it against all sample saves).
See `crypto/SaveCipher.kt` for the implementation — it's about 15 lines.

## What you can edit

- **Player** — gold, guild marks, online/offline playtime
- **Resources** — raw resource pool amounts
- **Skills** — per-skill level & skill points, with a "set all to X" bulk action
- **Inventory** — item/material quantities, with search, add, and remove
- **Shop Upgrades** — every purchased upgrade level and unlock flag
- **Concept Upgrades** — per-concept upgrade levels, with bulk-set
- **Elixirs** — selected elixir per skill
- **Chronomancy** — era levels, stability points, warp control
- **Map / Locations** — victories, conquered/reward flags, with "conquer all"
- **Raw JSON Editor** — the entire save as editable text, for anything without
  a dedicated screen (quests, wizards, battle loadout, etc.)

Fields you don't touch are preserved byte-for-byte in structure (the app
only rewrites the specific values you edit).

## Building

Requires Android Studio (Giraffe+) or the command line with the Android SDK
(compileSdk 34, build-tools 34.0.0) installed.

```bash
./gradlew assembleDebug
```

The output APK will be at `app/build/outputs/apk/debug/app-debug.apk`. It's
signed with the auto-generated Android debug keystore, which is what makes
it directly installable via `adb install` or by opening the file on-device
(with "install unknown apps" allowed for the source) — Android does not
allow installing a completely unsigned APK.

## Usage

1. Open the app and tap **Open Save File (.txt)**, then pick your save.
2. Edit whatever you like across the section screens.
3. Tap **Save** (top-right, or on the editor hub) to overwrite the original
   file, or **Save As…** to write a new file.

## Disclaimer

This tool edits your own local save data for your own game progress. Back up
your original save file before editing, and only use edited saves within
single-player / offline contexts consistent with the game's terms of use.
