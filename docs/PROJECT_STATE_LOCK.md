# LiveCreator Studio — Project State Lock

Last verified: 2026-11-17
Repository: https://github.com/adebojel/LiveCreatorStudio

This file is the durable handoff and source-of-truth checklist for future development sessions. Read it before making changes. Update it whenever a release, merge, blocker, or project decision changes.

## Verified baseline

- App version: 0.6.4
- Android versionCode: 28
- Latest published GitHub Release: `build-183`
- Release URL: https://github.com/adebojel/LiveCreatorStudio/releases/tag/build-183
- Latest debug APK: https://github.com/adebojel/LiveCreatorStudio/releases/latest/download/LiveCreatorStudio-debug.apk
- Release build workflow #183 succeeded.
- PR #13 was merged to `main`.
- PR #14 (official TikTok integration readiness documentation) was merged on 2026-11-17.
- PR #14 merge commit: `d4572b5bc1d7994cf09ca67e3b928b30bfe55f64`.
- PR #14 CI run #43952133046 succeeded: unit tests and debug APK build passed. Release-publishing steps were skipped because this was a pull-request validation run.
- APK is a debug/testing build, not production-signed and not Play Store ready.

## Current app scope

The 0.6.4 baseline includes local Android TTS, Soundboard/audio import and sound packages, and Floating Control. Existing provider-neutral event abstractions are not an active TikTok connection.

## Non-negotiable project decisions

1. Do not pay for, create, or publish a Google Play listing unless the user explicitly changes the decision. Google Play distribution is intentionally postponed.
2. Do not submit TikTok app review while prerequisites are unmet. Developer app is draft/unapproved according to the latest user project context.
3. Never fabricate Play Store URLs, production signing fingerprints, OAuth credentials, approval, API access, or device QA results.
4. Never commit Client Secrets, access/refresh tokens, keystores, signing keys, session cookies, or other credentials.
5. Do not use TikTok private endpoints, session-cookie scraping, reverse-engineered WebSockets, or label unofficial methods as official.
6. Do not claim public TikTok APIs provide real-time LIVE comments/gifts/follows unless an applicable official product/permission is verified in the developer portal or in writing.
7. Do not claim physical-device testing passed based only on CI or a screen recording. Record actual device, OS, steps, and results in `docs/DEVICE_QA_CHECKLIST.md`.
8. User prefers GitHub-based workflows and does not use Android Studio. Prefer GitHub Actions and repository changes.

## TikTok readiness facts

- TikTok Login Kit, Share Kit, and a TikTok LIVE event connector are not integrated in app runtime.
- No production OAuth credentials or LIVE event provider is connected.
- OAuth code exchange and token lifecycle, if later approved, must use a trusted backend; secrets and refresh tokens must never ship in the APK.
- Before a real Android review: user decision to resume store distribution, production signing, physical-device QA, a genuine store listing, confirmed official TikTok product/scopes, end-to-end implementation/demo, and current privacy disclosures are required.
- See `docs/TIKTOK_REVIEW_READINESS.md` and `docs/TIKTOK_INTEGRATION_ROADMAP.md`.

## Next-stage order

1. Verify the merged PR #14 website/documentation deployment and inspect the live Privacy, Terms, and Integration pages.
2. Continue a source audit and prioritize app stability/usability fixes from real-device feedback; do not reopen deferred 0.6.4 fixes without evidence.
3. Complete and record physical-device QA when the user is ready to test.
4. Design OAuth/backend only after confirming the exact TikTok product and prerequisites. Do not deploy or request credentials prematurely.
5. Create a new release only after code changes, CI success, and accurate release notes.

## Session handoff rule

At the start of every continuation, read this file plus `README.md`, `docs/TIKTOK_REVIEW_READINESS.md`, `docs/TIKTOK_INTEGRATION_ROADMAP.md`, and `docs/DEVICE_QA_CHECKLIST.md`. Verify live GitHub state rather than trusting an old conversation summary. Update this file with the new commit, release, CI run, blockers, and decisions before ending a substantial work session.
