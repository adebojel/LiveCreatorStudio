# LiveCreator Studio

Independent Android companion for livestream creators. This project is a clean-room implementation and is not affiliated with TikTok, TikFinity, or the publisher of TikStream.

## Status

**Foundation scaffold (v0.1.0) — not production-ready.**

### Included
- Kotlin + Jetpack Compose Android project
- Initial dark dashboard and navigation
- Android manifest and Gradle configuration
- GitHub Actions workflow that builds and uploads a debug APK artifact

### Not implemented yet
- Real TikTok LIVE event ingestion
- Provider authentication and session handling
- Event-driven TTS queue and audio playback
- Sound alert routing and custom sound library
- AI provider integrations
- Persistent settings/database
- Release signing and production APK distribution

The app does not generate fake LIVE events or claim a provider connection is active.

## Build locally
Open in Android Studio with JDK 17 and Android SDK 35, then run:

```bash
gradle assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## CI artifact
Every successful GitHub Actions run uploads `LiveCreatorStudio-debug-apk`. This is a debug build, not a signed production release.

## Integration policy
Prefer documented and authorized provider interfaces. Undocumented third-party endpoints can break and must be isolated behind replaceable adapters. Never collect TikTok passwords or bypass platform security controls.

## License
TBD. Third-party proprietary code, branding, assets, and audio are not included.
