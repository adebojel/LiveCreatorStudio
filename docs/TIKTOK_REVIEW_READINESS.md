# TikTok for Developers — Review Readiness

Last checked: 2026-11-17  
Project: LiveCreator Studio  
Website: https://adebojel.github.io/LiveCreatorStudio/

## Current verified project state

- [x] GitHub Pages project website exists.
- [x] Public Privacy Policy and Terms of Service pages exist and are linked from the website.
- [x] Public feature, download, support, and integration status pages exist.
- [x] Android source version is 0.7.1 / versionCode 30.
- [x] GitHub Actions run #183 succeeded and published debug APK as Release `build-192`.
- [ ] Production-signed Android APK/AAB is not established.
- [ ] App is not published on Google Play or Apple App Store.
- [ ] TikTok Login Kit / Share Kit is not integrated; 0.7.1 only adds a status screen and official documentation links.
- [ ] TikTok LIVE event connector is not active in app runtime.
- [ ] No end-to-end TikTok integration demo video exists.
- [ ] TikTok app review has not been submitted.

## Important current TikTok review gates

TikTok's published App Review FAQ says a mobile app must be available in the Apple App Store and/or Google Play Store. Its App Review Guidelines also say apps still in development or testing will not be approved. Android submissions require the package name and signing information. A review submission also needs a functioning integration demo video showing the end-to-end flow.

**Project decision:** Google Play distribution is intentionally postponed. Do not submit the current Android app for production review until this decision changes and the listed prerequisites are genuinely satisfied. Do not invent store URLs, production signatures, approval status, or integration evidence.

A marketing website is not a shortcut around mobile app requirements. A web Login Kit integration would need to be a real functioning web application with a valid redirect URI, not merely a static project landing page.

## Current Android identifiers

- Application ID / package name: `id.livecreator.studio`
- Min SDK: 26
- Target SDK: 35
- Source version: `0.6.4` / versionCode `28`
- Latest verified release: [build-183](https://github.com/adebojel/LiveCreatorStudio/releases/tag/build-183)
- CI artifact: debug APK, not production-signed.

## Official product fit — important limitation

The public Webhooks Events documentation lists authorization, Video Kit, and data portability notifications; it does not list LIVE comments, gifts, likes, follows, shares, viewer counts, or PK events. Generic webhook availability must not be treated as LIVE event access.

The public TikTok for Developers product catalog currently documents products such as Login Kit, Share Kit, Content Posting API, Display API, Research API, and Embed Videos. The public product documentation reviewed for this project does not describe a general API product for subscribing to TikTok LIVE comments, gifts, follows, or likes in real time.

Therefore:
- Do not promise TikTok LIVE event ingestion through the public developer API unless TikTok provides an applicable product/permission in the developer portal or in writing.
- Do not label unofficial scraping, session-cookie reuse, private endpoints, or reverse-engineered WebSocket clients as official TikTok integration.
- Keep the app's `LiveEventProvider` contract provider-agnostic and inactive until a permitted source is confirmed.
- The existing `CreatorEventEngine` is an internal rule engine; it does not connect to TikTok by itself.

## What must happen before Android app review

1. Finish and physically test the Android app as a user-facing product.
2. Confirm the exact official TikTok product and scopes available to this app.
3. Implement the selected official SDK/API flow end-to-end and test it with authorized sandbox/production credentials.
4. Create a production signing key and store it securely outside the public repository and CI logs.
5. Generate the release signing report; use the actual production signing certificate fingerprints required by the TikTok portal. Never substitute the debug key.
6. Publish the Android app to Google Play (or Apple App Store if applying for iOS) and obtain the genuine store listing URL.
7. Update the Privacy Policy and Terms of Service to reflect actual online data flows before enabling the integration.
8. Verify website, Privacy Policy, Terms of Service, and any other URL properties in TikTok Developer Portal.
9. Record a demo showing the actual app, user interactions, authorization, and every selected product/scope.
10. Complete App Review details and submit only after all criteria are satisfied.

## Security rules

- Never put Client Secret, access tokens, refresh tokens, private signing keys, or keystore files in this repository, website, screenshots, or issue reports.
- Exchange OAuth authorization codes and manage/refresh tokens on a trusted backend, not inside the APK.
- Validate OAuth `state`, redirect URI, scopes, token audience/expiry, and account ownership.
- Use least-privilege scopes and provide disconnect/revocation behavior.
- Do not collect TikTok credentials or ask users to paste session cookies.

## Official TikTok references

- [App Review Guidelines](https://developers.tiktok.com/docs/en/app-review-guidelines)
- [Register Your App](https://developers.tiktok.com/docs/en/getting-started-create-an-app)
- [App Review FAQ](https://developers.tiktok.com/docs/en/getting-started-faq)
- [Login Kit Overview](https://developers.tiktok.com/docs/en/login-kit-overview)
- [Display API Get Started](https://developers.tiktok.com/docs/en/display-api-get-started)

This checklist is a project readiness document, not a statement of approval or a guarantee that TikTok will approve the app.
