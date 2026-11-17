# LiveCreator Studio — Product Parity & Delivery Plan

Last updated: 2026-11-17  
Reference products: TikFinity Mobile (https://tikfinity.zerody.one/mobile/) and TikStream listing supplied by the project owner.  
Project website: https://adebojel.github.io/LiveCreatorStudio/

## Product direction

Build an original Android creator companion with the breadth and polish of leading livestream companion tools. Use references to study user workflows and feature categories; do not copy proprietary source code, artwork, branding, sound packs, or protected assets.

Core principles:
- Free core functionality; no LiveCreator account or subscription gate.
- Local-first preferences, soundboard metadata, and imported audio.
- User-controlled export/restore before stable release; app-private storage alone is not a backup.
- Optional platform authorization only when a real official feature requires it.
- TikTok integration must use products, scopes, SDKs, and partner access explicitly approved by TikTok.
- No scraping, session-cookie reuse, private endpoints, reverse-engineered WebSockets, or fabricated event data.
- No secrets, signing keys, access tokens, or refresh tokens in APK/source/website.

## Reference feature inventory and target behavior

| Module | Target behavior | Current state |
|---|---|---|
| Floating quick controls | Persistent draggable bubble, edge docking, quick actions, collapse/restore, configurable size/opacity | Implemented in source; device QA required |
| Background service | User-started foreground service, visible notification, explicit stop action, recovery guidance | Foundation present; OEM/device QA required |
| TTS | Local speech preview, rate/pitch, bounded queue, stop/flush, later event-driven speech | Manual/local speech present; event-driven source unavailable |
| Soundboard | Import local audio, browse/search, categories, preview/play/stop, volume, packs | Core local library present; UX expansion planned |
| Sound alerts | Map event type or selected action to a sound, per-rule enable/volume/cooldown | Rule engine foundations exist; event source not connected |
| Gift announcements | Gift name/value templates, thresholds, sound and TTS actions | Blocked on permitted event source |
| New viewer greeting | Per-viewer cooldown, greeting template, opt-out/filter controls | Blocked on permitted event source |
| Chat TTS | Include/exclude filters, blocked words, user filters, queue and rate limits | Blocked on permitted event source |
| Effects studio | Original animations, visual overlays, reactions, effect categories and sound pairing | Planned; must be implemented with original/licensed assets |
| Actions & events | User-defined trigger → condition → action rules, test mode, history and safety limits | Internal event engine exists; editor/runtime integration planned |
| Stream widgets | Goals, counters, timers, polls, challenges and scene overlays | Planned; local/manual mode can be built before platform events |
| Backup & restore | Versioned archive of preferences, soundboard metadata and imported audio via Android Storage Access Framework | Required before stable release; not implemented |
| Accessibility | TalkBack labels, large touch targets, keyboard/switch access, reduced motion, clear status feedback | Partial; formal accessibility QA required |

## Floating control hardening — 0.7.2

- Preserve the selected left/right edge docking when the overlay service is recreated.
- Make the expanded panel's “Ciutkan” action return to the visible compact bubble instead of removing the overlay.
- Keep explicit stop/off actions distinct from collapse.
- Keep the overlay optional and respect Android's user-granted draw-over-other-apps permission.
- Do not claim the overlay is immune to Android/OEM process termination. Android may stop background processes; users retain explicit notification and settings controls.
- Validate on real Android devices: launch, drag, edge docking, expand, collapse, audio actions, rotation/resizing, permission revocation, app restart, reboot, battery restrictions, and TalkBack.

## Official TikTok access workstream

The developer application was previously created as a Draft. The current known public app setup is:
- App name: LiveCreator Studio
- Ownership: Individual
- Type: Other
- Website: https://adebojel.github.io/LiveCreatorStudio/
- Public repository: https://github.com/adebojel/LiveCreatorStudio

The draft's exact selected products, scopes, redirect URIs, and any review notes must be checked in the owner's TikTok Developer Portal before changing or submitting it. Never assume a saved draft grants API access. Never overwrite the draft or submit a review automatically.

Prepare:
1. Product/scope inventory based on actual portal choices.
2. Clear user stories and least-privilege justification per scope.
3. Working website pages for product overview, privacy, terms, support, and integration status.
4. End-to-end demo for each real approved integration.
5. Backend security design for OAuth code exchange, state validation, token lifecycle, revocation and deletion if OAuth is actually needed.
6. Separate approval gates for mobile store distribution and TikTok review.

GitHub Pages is a static project website, not a trusted OAuth token-exchange backend. Do not put Client Secret or tokens in its JavaScript or HTML.

## Release gates

- CI unit tests and debug APK build pass.
- Backup/restore is implemented and round-trip tested.
- Floating overlay, TTS, audio focus, notification controls, and soundboard pass physical-device QA.
- Accessibility checks pass on the supported screen sizes and TalkBack.
- Privacy/terms accurately describe all data flows.
- TikTok features are enabled only after exact product/scope authorization and a real end-to-end test.
- Production signing and store publication remain deferred until explicitly resumed.

## Honest capability labels

Use “Available” only for implemented and tested behavior. Use “In development” for code not yet released or device-verified. Use “Requires official TikTok access” for event-driven features with no permitted source. Never show a fake “Connected” state or simulated live event as real.


## Actions & Events — 0.7.4
- Local rule editor with event type, enabled state, minimum quantity, cooldown, TTS template, and optional content URI.
- Rule configuration is stored on-device and does not require a LiveCreator account.
- The editor is configuration only. No live TikTok adapter is implied or enabled.
- The provider-neutral engine remains isolated from platform networking; only authorized adapters may submit normalized events.
- Next validation: exercise rule persistence across process restart, verify accessibility labels, and connect actions to actual TTS/Soundboard sinks behind explicit user controls.

## TikTok Developer submission gate (Android)
- Existing Developer Portal app remains a Draft; do not submit an Android platform configuration before Play Store publication and platform identity requirements are met.
- TikTok's current App Review Guidelines state Android apps must be published on Google Play and provide package name, app signature, and Play Store URL.
- The current GitHub Pages site is project information, not a substitute for a published Android app or a functioning web application.
- Do not select unrelated products/scopes or claim LIVE comments/gifts access. Request only documented products actually implemented and demonstrable.
- Official guidance: https://developers.tiktok.com/docs/en/app-review-guidelines and https://developers.tiktok.com/docs/en/getting-started-create-an-app
