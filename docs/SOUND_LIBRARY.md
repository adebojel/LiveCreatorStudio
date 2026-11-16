# LiveCreator Studio — Sound Library

## Automatic first-run library

On the first app launch with an internet connection, LiveCreator Studio downloads the three starter packs into app-private storage:

| Pack | Upstream sounds | License |
|---|---:|---|
| Kenney UI Audio | 50 | CC0 1.0 |
| Kenney Interface Sounds | 100 | CC0 1.0 |
| Kenney Digital Audio | 60 | CC0 1.0 |
| **Starter total** | **210** | **CC0 1.0** |

No audio picker or manual import is needed for the starter library. If the device is offline or the download fails, the app keeps working and retries the starter packs on a later launch. Manual import remains available as an optional feature.

## Full optional library

The Soundboard screen now lets users download each of the sixteen curated ZIP packs individually, or use one action to download the full catalog:

| Pack | Upstream count | Source |
|---|---:|---|
| Kenney UI Audio | 50 | Kenney |
| Kenney Interface Sounds | 100 | Kenney |
| Kenney Digital Audio | 60 | Kenney |
| Kenney Sci-Fi Sounds | 70 | Kenney |
| Kenney Impact Sounds | 130 | Kenney |
| Kenney RPG Audio | 50 | Kenney |
| Kenney Casino Audio | 50 | Kenney |
| Kenney Music Jingles | 85 | Kenney |
| Kenney Voiceover Pack | 90 | Kenney |
| Kenney Voiceover Pack Fighter | 45 | Kenney |
| Retro 8-bit Effects | 512 | OpenGameArt / SubspaceAudio |
| OpenGameArt RPG Sound Pack | 96 | OpenGameArt / artisticdude |
| Zombie Sounds | 24 | OpenGameArt / artisticdude |
| Footsteps Surfaces | 8 | OpenGameArt / TinyWorlds |
| Battle Sound Effects | 4 | OpenGameArt / Ogrebane |
| Retro UI Sound Pack | 70 | OpenGameArt / StumpyStrust |
| Retro Arcade SFX Pack 2 | 110 | OpenGameArt / phoenix1291 |
| **Total** | **1,554** | |

The counts are upstream catalog counts, not a promise that every archive entry is playable. The app counts actual supported audio files after extraction.

The OpenGameArt packs above include ZIP archives marked CC0 in the reviewed sources. Retro Arcade SFX Pack 2 adds 110 unique effects across 11 themed folders; the page identifies CC0 and lists MP3/OGG/WAV/FLAC formats. Two additional CC0 entries are distributed as 7z archives and are excluded because the Android installer currently accepts ZIP only. One further ZIP archive is AIFF-only and is excluded until lossless AIFF-to-WAV conversion is implemented; this avoids offering a pack that the Android player may not decode. Kenney packs are downloaded from official Kenney archive URLs; OpenGameArt packs are downloaded from their published archive URLs.

## Licensing and attribution

All ten pack pages are listed as Creative Commons CC0 by the upstream source index. Kenney's official support page states that assets on its asset pages are public-domain/CC0, can be used commercially, and do not require attribution. LiveCreator Studio still preserves a per-pack `LICENSES.txt` with the creator and original source link.

- Creator: Kenney / kenney.nl
- License: [CC0 1.0 Universal](https://creativecommons.org/publicdomain/zero/1.0/)
- Official license guidance: [Kenney Support](https://kenney.nl/support)
- Exact pack URLs and upstream counts: [open-game-sfx-index SOURCES.md](https://github.com/Mcamento8/open-game-sfx-index/blob/main/SOURCES.md). The added Retro UI Sound Pack is marked CC0 on [its OpenGameArt page](https://opengameart.org/content/sound-pack).

## Download and storage behavior

- Downloads only fixed HTTPS URLs in the in-app catalog, from the fixed `kenney.nl` and `opengameart.org` catalog hosts and their subdomains.
- Saves extracted audio in app-private storage; files are not exposed as public shared storage.
- Uses staging directories and only replaces an installed pack after successful extraction.
- Enforces archive, entry-count, per-file, and total extracted-size limits.
- Retains a local license/source note with each pack.
- The first-run starter download needs internet access. Users can also select optional packs individually or download all packs. Once installed, those sounds play offline. ZIP archives are downloaded directly from the original catalog sources at runtime; they are not embedded inside the APK or attached as separate GitHub Release assets.
- The app does not mirror or rehost third-party files on its website; it downloads the original upstream archives directly. Individual pack installation is available in Soundboard, alongside the download-all action.

## Why other libraries are not included automatically

A free download does not automatically grant permission to redistribute the audio inside an app. Freesound has per-file CC0 / CC-BY / CC-BY-NC licensing, and its API terms say commercial API use requires a separate licensing discussion. Mixkit and several other libraries also restrict standalone redistribution or repackaging. Those sources are therefore not placed in the automatic bundled catalog without an explicit redistribution right.

For a future pack, require a verified license that permits redistribution inside an application, preserve its source and attribution terms, and add the exact archive URL to the reviewed catalog.
