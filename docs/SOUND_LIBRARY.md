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

The Soundboard screen also offers a one-tap download of all ten curated Kenney packs:

| Pack | Upstream count | Category |
|---|---:|---|
| UI Audio | 50 | UI clicks and controls |
| Interface Sounds | 100 | Interface and notifications |
| Digital Audio | 60 | Digital/electronic effects |
| Sci-Fi Sounds | 70 | Futuristic effects |
| Impact Sounds | 130 | Impacts and foley |
| RPG Audio | 50 | Game-style effects |
| Casino Audio | 50 | Casino/game effects |
| Music Jingles | 85 | Short musical stingers |
| Voiceover Pack | 90 | Voice clips |
| Voiceover Pack Fighter | 45 | Character voice clips |
| **Total** | **730** | |

The counts are the upstream pack counts recorded by the source index; the app imports playable audio entries found in each downloaded ZIP, so the final playable count is confirmed at installation time.

## Licensing and attribution

All ten pack pages are listed as Creative Commons CC0 by the upstream source index. Kenney's official support page states that assets on its asset pages are public-domain/CC0, can be used commercially, and do not require attribution. LiveCreator Studio still preserves a per-pack `LICENSES.txt` with the creator and original source link.

- Creator: Kenney / kenney.nl
- License: [CC0 1.0 Universal](https://creativecommons.org/publicdomain/zero/1.0/)
- Official license guidance: [Kenney Support](https://kenney.nl/support)
- Exact pack URLs and upstream counts: [open-game-sfx-index SOURCES.md](https://github.com/Mcamento8/open-game-sfx-index/blob/main/SOURCES.md)

## Download and storage behavior

- Downloads only fixed HTTPS URLs in the in-app catalog, from `kenney.nl` and its subdomains.
- Saves extracted audio in app-private storage; files are not exposed as public shared storage.
- Uses staging directories and only replaces an installed pack after successful extraction.
- Enforces archive, entry-count, per-file, and total extracted-size limits.
- Retains a local license/source note with each pack.
- The first-run starter download needs internet access. Once installed, those sounds play offline.
- The app does not mirror or rehost third-party files on its website; it downloads the official Kenney archives directly.

## Why other libraries are not included automatically

A free download does not automatically grant permission to redistribute the audio inside an app. Freesound has per-file CC0 / CC-BY / CC-BY-NC licensing, and its API terms say commercial API use requires a separate licensing discussion. Mixkit and several other libraries also restrict standalone redistribution or repackaging. Those sources are therefore not placed in the automatic bundled catalog without an explicit redistribution right.

For a future pack, require a verified license that permits redistribution inside an application, preserve its source and attribution terms, and add the exact archive URL to the reviewed catalog.
