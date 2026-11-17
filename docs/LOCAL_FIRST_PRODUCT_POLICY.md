# Local-first, free, and no-account product policy

## Product commitments

LiveCreator Studio is intended to remain a free-to-use Android companion app. The core local features must not require a LiveCreator account, subscription, or cloud login.

- TTS uses the Android speech engine on the device.
- Soundboard playback and imported sound files are handled on the device.
- User preferences and soundboard metadata use Android app-private storage (DataStore).
- No LiveCreator account system or mandatory cloud profile is part of the product plan.
- Do not add analytics, advertising SDKs, account registration, or background upload of user content without a separate explicit product/privacy decision.

## Data durability and honest limitations

Local storage avoids requiring users to entrust their work to a LiveCreator cloud account, but local-only storage is not itself a backup.

- Android app-private data normally survives app restarts and device reboots.
- Uninstalling the app, clearing app storage, device failure, or a factory reset can remove local preferences and imported audio.
- Downloaded catalog sound packs can generally be downloaded again; user-imported audio cannot be assumed recoverable unless the user has the original file or a backup.
- The app must not claim "zero data loss" or "permanent storage".

## Required backup work before a stable public release

Implement and test a user-controlled backup/restore workflow using Android's Storage Access Framework:

1. Export settings, soundboard metadata, and user-imported audio into a versioned archive selected by the user.
2. Import/restore from a user-selected archive, validate archive paths and sizes, reject traversal and malformed metadata, and never overwrite existing data silently.
3. Provide preview, conflict handling, progress, cancellation, and a clear completion/failure report.
4. Keep backup files wherever the user chooses; do not upload them to a LiveCreator server.
5. Add automated tests for schema versions, malformed archives, path traversal, partial restore, and round-trip fidelity.
6. Add a visible reminder that the user should export a backup before uninstalling or clearing storage.

This backup workflow is a planned requirement and must not be represented as implemented until code and tests are merged.

## TikTok authorization boundary

- Local TTS, Soundboard, and app settings must remain usable without TikTok login.
- TikTok connection must be optional and isolated behind a separate feature boundary.
- If TikTok's official product requires OAuth for a specific approved scope, request consent only when the user explicitly chooses that TikTok feature.
- Never require TikTok authorization to launch the app or use local features.
- Store the minimum OAuth tokens only if a real approved official integration requires them. Protect tokens using Android Keystore-backed encryption, provide disconnect/revocation and deletion, and never put secrets in the APK or public repository.
- If no official TikTok LIVE event API/scope is granted, clearly show LIVE event integration as unavailable. Do not substitute unofficial endpoints.

## Distribution policy

Google Play Console setup, fees, production signing, and store publication are deferred until the app is functionally stable and device QA is complete. Continue GitHub Actions debug builds and direct testing in the meantime. Do not start paid store setup without the user's new explicit instruction.
