# LiveCreator Studio

Independent Android companion for livestream creators. This clean-room project is not affiliated with TikTok, TikFinity, or the publisher of TikStream.

## Current version

**0.2.0 — early development / device testing required.**

## Implemented in source

- Kotlin + Jetpack Compose Android application
- Dark creator dashboard and navigation
- On-device Android TextToSpeech preview with Indonesian locale check
- User-started foreground audio service with persistent notification
- Background TTS request path through the foreground service
- Notification action to stop the background service
- Android Picture-in-Picture support with a compact creator status panel
- Battery-optimization settings shortcut
- Optional reboot reminder notification; it asks the user to resume rather than silently forcing a service start
- Compose semantic labels and standard TalkBack compatibility
- GitHub Actions workflow for debug APK build artifacts

## Not implemented yet

- Real TikTok LIVE event ingestion and authenticated provider session
- Event-driven comment/gift/follow/share processing
- Production audio queue, sound packs, per-event routing and mixing
- Custom sound library and file picker
- AI provider integrations and local model runtime
- Persistent user settings/database beyond the small reboot-reminder preference
- Signed production APK, release automation and store compliance review

The application does not fabricate LIVE events or claim that a provider is connected.

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

## CI artifact

A successful GitHub Actions run uploads `LiveCreatorStudio-debug-apk`. This is a debug artifact, not a signed production APK. Check the Actions tab for build status and artifacts.

## Integration policy

Use documented and authorized provider interfaces where available. Undocumented third-party endpoints can break and must be isolated behind replaceable adapters. Never collect TikTok passwords or bypass platform security controls.

## License

TBD. No third-party proprietary code, branding, assets, or audio are included.
