# LiveCreator Studio — Official TikTok Integration Roadmap

Last reviewed: 2026-11-17  
Current app version: 0.7.1 / versionCode 30  
Current verified baseline before this PR: 0.7.0 / build-192; 0.7.1 capability-center build is pending CI

## Objective

Prepare LiveCreator Studio for legitimate platform integration without misrepresenting unofficial access as an official TikTok API. Keep the existing local TTS, Soundboard, and Floating Control usable independently of any external provider.

## Verified current architecture

- `LiveEventProvider` defines a provider-neutral, read-only event source contract.
- `CreatorEvent` normalizes event types such as FOLLOW, GIFT, LIKE, SHARE, COMMENT, JOIN, and UNKNOWN.
- `CreatorEventEngine` applies bounded rules, duplicate suppression, cooldowns, and a maximum dispatch rate.
- No production provider is wired to the app UI/runtime.
- Version 0.7.1 adds a status-only official TikTok screen; it does not perform OAuth or claim a connected account.
- No TikTok OAuth credentials, token store, or LIVE event connection are present.

The contract and rule engine are foundations only. They do not imply an active TikTok connection.

## Official API capability check

The public Webhooks Events reference currently lists authorization removal, Video Kit upload/publish notifications, and data portability readiness events; it does not list LIVE chat, gift, like, follow, share, viewer-count, or PK events.

The public TikTok for Developers catalog and documentation reviewed on 2026-11-17 list products including Login Kit, Share Kit, Content Posting API, Display API, Research API, and Embed Videos. The public documentation reviewed does not describe a general-purpose API product to subscribe to TikTok LIVE chat, gifts, follows, or likes in real time.

**Decision:** do not implement an unofficial LIVE connector using session cookies, private endpoints, screen scraping, or reverse-engineered WebSockets and do not call such a connector official. If TikTok later provides an applicable product, permission, partner program, or written authorization, re-evaluate this decision with its exact technical and policy requirements.

## Work stages

### Stage A — project truth and privacy baseline (current)

- [x] Synchronize README with version 0.6.4 and Release build-183.
- [x] Correct Privacy Policy to describe app-private imported audio and downloaded sound packs.
- [x] Correct Terms of Service version and current integration status.
- [x] Publish an explicit integration status page that distinguishes architecture from active connectors.
- [x] Record official API limitations and review gates.

### Stage B — official OAuth readiness (design only until credentials/prerequisites exist)

The public scopes documentation states that app scope approval alone is insufficient: each end user must also authorize the requested scope. Do not infer LIVE event access from Login Kit, Display API, or generic webhook support.

- [ ] Confirm which TikTok product is actually approved/enabled in the developer portal. The app screen currently provides links/status only.
- [ ] Confirm a genuine redirect URI and the app platform configuration.
- [ ] Choose and provision a trusted backend (not GitHub Pages) for OAuth code exchange and token lifecycle.
- [ ] Define encrypted token storage, least-privilege scopes, CSRF/state validation, refresh/revocation, deletion, audit logging, and rate limiting.
- [ ] Keep Client Secret and refresh tokens server-side; never ship them in Android resources, BuildConfig, GitHub source, or APK.
- [ ] Add a mock OAuth adapter for tests only; do not show it as a real connected TikTok account.

### Stage C — Android distribution gate

- [ ] User decides to resume store distribution.
- [ ] Establish production signing and secure CI signing secrets.
- [ ] Complete physical-device QA and privacy/terms review.
- [ ] Publish a genuine Play Store or App Store listing.
- [ ] Only then configure the real mobile app details and signing fingerprints in TikTok Developer Portal, as required by its review rules.

### Stage D — integration after approval

- [ ] Implement only the official products/scopes actually approved.
- [ ] Add consent, connection state, disconnect, token expiry, and error handling.
- [ ] Route only permitted data to the existing event pipeline.
- [ ] Add event provenance, bounded queues, duplicate detection, cooldowns, rate limits, and user-controlled actions.
- [ ] Demonstrate each selected scope end-to-end and update public privacy disclosures.
- [ ] Submit the actual integration for review; never claim approval before it is granted.

## Current blockers and non-goals

- Google Play publication is intentionally postponed; no Play Store fee or listing is to be initiated without a new explicit decision.
- TikTok developer app remains a draft / unapproved state per the latest project context.
- Do not fabricate API access, app approval, OAuth tokens, Play listing, production signing fingerprints, or a demo of a nonexistent integration.
- Do not treat the GitHub Pages marketing website as a substitute for a working web app or a published Android app.
- Do not block local TTS/Soundboard/Floating Control work on TikTok availability.

## Official references

- [TikTok developer product catalog](https://developers.tiktok.com/)
- [Login Kit overview](https://developers.tiktok.com/docs/en/login-kit-overview)
- [Register your app](https://developers.tiktok.com/docs/en/getting-started-create-an-app)
- [App Review Guidelines](https://developers.tiktok.com/docs/en/app-review-guidelines)
- [App Review FAQ](https://developers.tiktok.com/docs/en/getting-started-faq)
- [Display API](https://developers.tiktok.com/docs/en/display-api-get-started)

This roadmap is a technical plan based on the public documentation reviewed on the date above. TikTok may change product availability, permissions, and review criteria.
