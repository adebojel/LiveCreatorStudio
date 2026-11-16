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

The Soundboard screen also offers a one-tap download of all sixteen curated ZIP packs:

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
| Level Up & Power Up | 13 | OpenGameArt / wobbleboxx |
| Zombie Sounds | 24 | OpenGameArt / artisticdude |
| Footsteps Surfaces | 8 | OpenGameArt / TinyWorlds |
| Battle Sound Effects | 4 | OpenGameArt / Ogrebane |
| **Total** | **1,387** | |

The counts are upstream catalog counts, not a promise that every archive entry is playable. The app counts actual supported audio files after extraction.

The OpenGameArt packs above are the six ZIP archives marked CC0 in the reviewed source index. Two additional CC0 entries in that index are distributed as 7z archives and are deliberately excluded because the Android installer currently accepts ZIP only. Kenney packs are downloaded from official Kenney archive URLs; OpenGameArt packs are downloaded from their published archive URLs.

## Licensing and attribution

All ten pack pages are listed as Creative Commons CC0 by the upstream source index. Kenney's official support page states that assets on its asset pages are public-domain/CC0, can be used commercially, and do not require attribution. LiveCreator Studio still preserves a per-pack `LICENSES.txt` with the creator and original source link.

- Creator: Kenney / kenney.nl
- License: [CC0 1.0 Universal](https://creativecommons.org/publicdomain/zero/1.0/)
- Official license guidance: [Kenney Support](https://kenney.nl/support)
- Exact pack URLs and upstream counts: [open-game-sfx-index SOURCES.md](https://github.com/Mcamento8/open-game-sfx-index/blob/main/SOURCES.md)

## Download and storage behavior

- Downloads only fixed HTTPS URLs in the in-app catalog, from the fixed `kenney.nl` and `opengameart.org` catalog hosts and their subdomains.
- Saves extracted audio in app-private storage; files are not exposed as public shared storage.
- Uses staging directories and only replaces an installed pack after successful extraction.
- Enforces archive, entry-count, per-file, and total extracted-size limits.
- Retains a local license/source note with each pack.
- The first-run starter download needs internet access. Once installed, those sounds play offline.
- The app does not mirror or rehost third-party files on its website; it downloads the original upstream archives directly.

## Why other libraries are not included automatically

A free download does not automatically grant permission to redistribute the audio inside an app. Freesound has per-file CC0 / CC-BY / CC-BY-NC licensing, and its API terms say commercial API use requires a separate licensing discussion. Mixkit and several other libraries also restrict standalone redistribution or repackaging. Those sources are therefore not placed in the automatic bundled catalog without an explicit redistribution right.

For a future pack, require a verified license that permits redistribution inside an application, preserve its source and attribution terms, and add the exact archive URL to the reviewed catalog.
