# LiveCreator Studio

Independent Android companion for livestream creators. This clean-room project is not affiliated with TikTok, TikFinity, or the publisher of TikStream.

## Current version

**0.4.1 — creator audio reliability update / device testing required.**

**[Unduh APK terbaru dari GitHub Releases](https://github.com/adebojel/LiveCreatorStudio/releases/latest/download/LiveCreatorStudio-debug.apk)** · [Lihat semua Releases](https://github.com/adebojel/LiveCreatorStudio/releases)

## Implemented in source

- Kotlin + Jetpack Compose Android application with dark creator dashboard
- On-device Android TextToSpeech preview with Indonesian locale check, adjustable speech rate and pitch, and saved voice preferences
- Background TTS queue with per-utterance rate and pitch settings, bounded queue size, and notification feedback for actual playback and engine errors
- User-started foreground audio service with persistent notification and stop action
- Background TTS request path through the foreground service
- Android Picture-in-Picture support with a compact creator status panel
- Local soundboard: validate audio MIME type, retain Android document URI access, persist the library, use asynchronous MediaPlayer preparation, play, stop, and remove clips with playback error handling
- Battery-optimization, accessibility, app-permission, and notification settings shortcuts
- Opt-in auto-start when the user opens the app
- Optional reboot reminder notification; it asks the user to resume rather than silently forcing a service start
- Compose semantic labels and standard TalkBack compatibility
- GitHub Actions workflow for debug APK build artifacts and automatic GitHub Release publishing on updates to `main`

## Not implemented yet

- Real TikTok LIVE event ingestion and authenticated provider session
- Event-driven comment/gift/follow/share processing
- Soundboard playback as a persistent foreground audio session, multi-channel mixing, and event-to-sound automation
- AI provider integrations and a genuinely executable local model runtime
- Persistent structured settings/database beyond the soundboard URI list and small preferences
- Signed production APK and Google Play policy/security review

The application does not fabricate LIVE events, fake AI responses, or claim that a provider is connected. The soundboard is a real local-file feature and is not yet connected to livestream events.

## Background execution and Android limits

The user can explicitly start a foreground audio service. Android displays a persistent notification with a stop action. The service uses `START_STICKY` as a recovery hint, but Android and device manufacturers may still stop or restrict it.

No Android app can guarantee immunity from battery optimization, force-stop, OEM task managers, or OS resource management. Users can review battery optimization settings from the app. Exemptions are always controlled by Android and the user.

Android versions impose restrictions on launching foreground services from boot broadcasts. The boot receiver therefore posts an optional reminder to reopen the app instead of claiming guaranteed automatic startup.

PiP requires Android 8.0 (API 26) or later and is controlled by the system. TalkBack reads the Compose controls through standard accessibility semantics; this project does not use an accessibility service to inspect or control other apps.

## Build

Open in Android Studio with JDK 17 and Android SDK 35, or run:

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Download APK

The latest successful build is published to the **Releases → Assets** section as `LiveCreatorStudio-debug.apk`. Use the stable download link below:

- [Download LiveCreatorStudio-debug.apk](https://github.com/adebojel/LiveCreatorStudio/releases/latest/download/LiveCreatorStudio-debug.apk)
- [Open Releases and view APK assets](https://github.com/adebojel/LiveCreatorStudio/releases)

Every push to `main` runs the Android build and, if successful, publishes the APK as an asset named `LiveCreatorStudio-debug.apk` in a GitHub Release. Pull-request builds do not publish releases.

**Important:** the current package is a debug APK, not a production-signed Play Store release. Install only if you trust this repository and Android allows installation. Play Protect may warn or block sideloaded/debug builds; this project does not bypass or disable Play Protect.

## Integration policy

Use documented and authorized provider interfaces where available. Undocumented third-party endpoints can break and must be isolated behind replaceable adapters. Never collect TikTok passwords or bypass platform security controls.

## License

TBD. No third-party proprietary code, branding, assets, or audio are included.
