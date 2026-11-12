# Canberra Explorer — App Update Log

This document is the running release record for the Canberra Explorer Android app in the
[CBR_URBEX GitHub repository](https://github.com/dominichuddleston4-sys/CBR_URBEX).

It should be updated whenever a new app release is published. Record only changes that are
supported by release notes, commits, build results, or verified app behaviour. Keep planned
features separate from features confirmed as shipped.

## Current published release

- **Version:** v1.39
- **Published:** 11 November 2026 (GitHub release timestamp: 15:37 UTC)
- **APK:** [Download app-release.apk](https://github.com/dominichuddleston4-sys/CBR_URBEX/releases/download/v1.39/app-release.apk)
- **Release page:** https://github.com/dominichuddleston4-sys/CBR_URBEX/releases/tag/v1.39
- **Publication status:** GitHub release is public; APK asset is uploaded and downloadable.
- **Verification note:** Public availability is confirmed. Successful installation, in-app operation,
  and automatic updating on a physical Android device have not been independently verified here.

## Release history

### v1.39 — 11 November 2026
- Public GitHub release published.
- APK asset `app-release.apk` uploaded successfully.
- **Feature/fix details:** Not yet documented from verified release notes or a complete code comparison.

### v1.28 — 11 November 2026 (earlier recorded publication)
- Earlier project status recorded a successful APK build and public publication.
- The `version.json` update manifest was recorded as set to 1.28.
- GitHub Pages deployment was reported successful.
- Automatic updater still required further fixing at that time.
- Netlify deployment was not verified in that status report.
- **Note:** This is a historical status snapshot, not a complete list of v1.28 code changes.

## December update plan — Google account sign-in

**Status: Planned only; not implemented or released.**

The user has requested that everyone must sign in with a Google account **before they can view the map**.

Planned behaviour and requirements:
- Show a Google sign-in screen when the app opens.
- Keep the map and app content locked until authentication succeeds.
- Allow authenticated users to sign out from Settings; signing out returns the app to the sign-in screen.
- Keep shared map locations and shared location-photo galleries available to signed-in users.
- Associate photo uploads with the authenticated user's account and provide suitable uploader attribution and reporting/removal controls.
- Configure Firebase Authentication with the Google provider, and secure any photo/database backend with authenticated-user rules. A sign-in screen alone is not sufficient to protect stored data.
- Test sign-in, sign-out, app restart/session persistence, denied/cancelled sign-in, and access blocking before publishing.

Implementation dependencies still to confirm:
- Firebase project and Google sign-in provider configuration.
- Android application/package ID and signing certificate fingerprints for the release build.
- Web/PWA sign-in configuration if the website and Android app share the same authentication flow.
- Backend/database and storage rules for shared user-uploaded photos.

Do not mark this feature as complete until the configuration, implementation, release build, and access-control behaviour have been tested.

## Work planned or discussed — not confirmed as shipped

The following items have been discussed for future Canberra Explorer updates. Do not describe them
as released until the relevant version is published and the implementation is checked.

- Reliable interactive map and correctly named location pins.
- Offline/installable app experience and cache handling.
- Google Maps directions launched from a location.
- Settings and update notifications that clear after a successful update.
- Automatic APK update checking and installation/update flow.
- App icon displayed correctly on the Android home screen.
- Location photo uploads with shared, location-specific galleries, cloud sync, uploader attribution,
  and reporting/removal controls.

## Release checklist

For each new release, add a dated section containing:

1. Version number, release date, and public release link.
2. APK filename, download link, and whether the asset is actually present.
3. New features confirmed shipped.
4. Fixes and improvements confirmed shipped.
5. Known issues, limitations, and items still awaiting verification.
6. Build and deployment results (GitHub Actions, GitHub Pages, and Netlify where applicable).
7. Automatic-update status, including what was tested and on which environment/device.
8. A short comparison with the previous release.

**Accuracy rule:** A successful build or public APK upload does not prove that the APK installs,
that every feature works, or that automatic updates work. Mark each separately as verified,
failed, or not tested.
