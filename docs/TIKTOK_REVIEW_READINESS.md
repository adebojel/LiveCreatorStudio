# TikTok for Developers — Review Readiness

Last checked: 2026-11-17  
Project: LiveCreator Studio  
Website: https://adebojel.github.io/LiveCreatorStudio/

## Current verified project state

- [x] GitHub Pages is configured to deploy from `main` / `docs`.
- [x] Main project website exists.
- [x] Privacy Policy page exists: `/privacy.html`.
- [x] Terms of Service page exists: `/terms.html`.
- [x] Public feature information exists: `/features.html`.
- [x] Public download/release information exists: `/download.html`.
- [x] Public support page exists: `/support.html`.
- [x] Public integration status page exists: `/integration.html`.
- [x] Website navigation links to the public information pages.
- [x] GitHub Actions builds a debug APK for controlled testing.
- [ ] Production-signed Android APK/AAB is not yet established.
- [ ] App is not published on Google Play or Apple App Store.
- [ ] TikTok Login Kit / Share Kit is not integrated in the app.
- [ ] TikTok LIVE event connector is not active in the app runtime.
- [ ] No end-to-end TikTok integration demo video exists yet.
- [ ] TikTok app review has not been submitted.

## Important TikTok review gate

TikTok's current App Review FAQ says a mobile app must be available in the Apple App Store and/or Google Play Store. The App Review Guidelines also state that apps still in development or testing will not be approved. Android submissions require the Android package name and signing information.

**Decision:** Do not submit the current Android app for production review while Google Play distribution is intentionally postponed. Do not mark the Android platform as ready or invent store URLs/signatures.

A website-only submission is not a shortcut for this Android application. TikTok requires a real web application integration (and valid redirect URI for web Login Kit); a marketing website alone does not implement Login Kit.

## Current Android identifiers

- Application ID / package name: `id.livecreator.studio`
- Min SDK: 26
- Target SDK: 35
- Current source version: `0.5.7` / versionCode `21`
- CI artifact type: debug APK, not production-signed release.

## What must happen before Android app review

1. Finish and test the Android app as a user-facing product.
2. Decide the exact official TikTok product to integrate. Only request scopes required by that feature.
3. Implement the selected official SDK/API flow end-to-end. Do not describe the third-party read-only LIVE connector compatibility experiment as an approved official TikTok integration.
4. Create a production signing key and store it securely outside the public repository and CI logs.
5. Generate the release signing report. TikTok's Android setup asks for the MD5 app signature (without colons) and SHA-256 signing certificate fingerprint. Do not use a debug key as a substitute for the production release key.
6. Publish the Android app to Google Play (or Apple App Store if applying for iOS) and obtain the genuine store listing URL.
7. Update the Privacy Policy and Terms of Service to match the actual integrated data flows and SDK behavior.
8. Verify the website, Privacy Policy, Terms of Service, and any other URL properties in TikTok Developer Portal when prompted.
9. Record an end-to-end demo showing the actual app, user interactions, authorization, and each selected product/scope. Remove any product/scope that is not implemented.
10. Complete the App Review explanation, save the form, review all validation errors, then submit.

## Developer Portal form notes

- App name should match the product/website brand and must not imply TikTok endorsement.
- Use a clear app icon that matches LiveCreator Studio.
- The app description must accurately describe only the functionality that is available.
- The official website must be fully developed and externally accessible; Privacy Policy and Terms links must be visible and active.
- Never put Client Secret, access tokens, refresh tokens, signing private keys, or keystore files in this repository, website, screenshots, or public issue reports.
- The app review video must show real integration behavior. Concept artwork and mock UI are not proof of a working integration.

## Official TikTok references

- [App Review Guidelines](https://developers.tiktok.com/docs/en/app-review-guidelines)
- [Register Your App](https://developers.tiktok.com/docs/en/getting-started-create-an-app)
- [App Review FAQ](https://developers.tiktok.com/docs/en/getting-started-faq)
- [Android Quickstart](https://developers.tiktok.com/docs/en/mobile-sdk-android-quickstart)

This checklist is a project readiness document, not a statement of approval or a guarantee that TikTok will approve the app.
