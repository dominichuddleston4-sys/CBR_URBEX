# Canberra Explorer — App Update Log

This document is the running release record for the Canberra Explorer Android app in the
[CBR_URBEX GitHub repository](https://github.com/dominichuddleston4-sys/CBR_URBEX).

Record shipped changes only when supported by repository evidence, release assets, build results,
or user/device testing. Keep planned features separate from features confirmed as shipped.

## Current published release

- **Version:** v1.173
- **Published:** 13 November 2026 in Canberra (GitHub publication timestamp: 12 November 2026 at 17:24:27 UTC).
- **APK:** [Download app-release.apk](https://github.com/dominichuddleston4-sys/CBR_URBEX/releases/download/v1.173/app-release.apk)
- **APK asset:** Uploaded and publicly available; 55,394,131 bytes (about 52.8 MiB).
- **APK SHA-256:** `87799220687565af22b8e36c6168ee593a2fb178a691549a2ff5b21279a4ab0f`
- **Release page:** https://github.com/dominichuddleston4-sys/CBR_URBEX/releases/tag/v1.173
- **Build:** GitHub Actions APK workflow run 173 completed successfully, including signing-key preparation, signed APK build, GitHub Release upload and version metadata publication.
- **Website deployment:** GitHub Pages deploy workflow completed successfully. No Netlify deployment result was evidenced in the inspected workflows.
- **Device verification:** No new v1.173 installation or runtime test is recorded. Earlier user-reported tests on v1.47 confirmed Google sign-in before map access and session persistence; the app icon was later user-confirmed as working. Those reports are not a fresh v1.173 regression test.
- **Automatic updates:** The default-branch `version.json` reports versionCode 173 / versionName 1.173 and points to the latest-release APK URL. The GitHub Pages metadata-publish step succeeded. End-to-end in-app detection, download, Android install, restart and update-banner clearing have not been independently verified.

## Release history

### v1.173 — 13 November 2026 (Canberra local date)

- **Release:** [Canberra Explorer v1.173](https://github.com/dominichuddleston4-sys/CBR_URBEX/releases/tag/v1.173), published 12 November 2026 at 17:24:27 UTC (13 November at 04:24:27 Canberra time).
- **Release notes:** The GitHub release body is empty. The implementation summary below is based on the release commit, the 46-commit comparison from v1.47, current source files and workflow results—not on release-note claims.
- **APK:** `app-release.apk` is uploaded. [Download APK](https://github.com/dominichuddleston4-sys/CBR_URBEX/releases/download/v1.173/app-release.apk). Size: 55,394,131 bytes. SHA-256: `87799220687565af22b8e36c6168ee593a2fb178a691549a2ff5b21279a4ab0f`.
- **Build commit:** [`7827cff` — Fix Firestore error handling in Android admin panel](https://github.com/dominichuddleston4-sys/CBR_URBEX/commit/7827cff21cc38ceb342607ea50c14f92524ed7bd). The comparison from v1.47 to this commit contains 46 commits.
- **Code included in this release:**
  - The native Android entry point still requires Firebase Google sign-in before it loads the map. The sign-in gate and remembered session were user-tested on v1.47, but were not re-tested on v1.173.
  - Settings includes a sign-out control, and the Android bridge signs out of Firebase and Google before returning to the sign-in screen. The sign-out flow is present in code but has not been independently device-tested.
  - A native MapLibre offline-map screen is included, with Central, North, South, West and East ACT area choices, saved-area status and deletion. The implementation applies per-area limits of 50 MB / 3,000 tiles and a 15,000-tile total limit. This confirms code is included, not that downloads or offline reopening have passed device tests.
  - Android launcher icon resources are included. The project log records the user's confirmation that the installed app icon works; adaptive-icon variations were not separately tested.
  - Owner-only admin tools are included for listing registered accounts and setting blocked / force-sign-out flags. The Firestore rules deployment workflow completed successfully (see below).
- **Verified fixes and improvements:**
  - [Commit `7827cff`](https://github.com/dominichuddleston4-sys/CBR_URBEX/commit/7827cff21cc38ceb342607ea50c14f92524ed7bd) changes the admin panel's Firestore read/update error reporting to show the Firebase error message rather than only its error code.
  - [Commit `392642d`](https://github.com/dominichuddleston4-sys/CBR_URBEX/commit/392642ded91aa640e07d58b766464bebdd5c1d91) refreshes the administrator ID token before Firestore user-management requests.
  - The APK build workflow completed its signing-key preparation, signed release APK build, release upload and update-metadata publication successfully.
- **Build and deployment outcomes:**
  - [APK build and publish — run 173](https://github.com/dominichuddleston4-sys/CBR_URBEX/actions/runs/43268899694): **success**. The signed APK build and GitHub Release upload steps both passed.
  - [GitHub Pages deployment — run 82](https://github.com/dominichuddleston4-sys/CBR_URBEX/actions/runs/43268899329): **success**. The Pages upload and deploy steps passed.
  - [Firestore rules deployment](https://github.com/dominichuddleston4-sys/CBR_URBEX/actions/runs/43264740147): **success**. The workflow deploys Firestore rules only; this is not evidence that Firebase Storage rules were deployed.
  - Netlify deployment: **not verified** from the inspected workflow evidence.
- **Known issues and unverified items:**
  - No independent v1.173 physical-device installation, upgrade, sign-out, account-switching or authentication-edge-case test is recorded.
  - The offline-map screen and download code are present, but map download, map reopening in airplane mode, coverage, attribution and recovery tests remain unverified. Offline driving/walking directions are not implemented.
  - Shared location photo-gallery UI and upload flow are not shipped. A `storage.rules` file exists in the repository, but the inspected deployment workflow only deploys Firestore rules; Storage-rule deployment and photo-upload security tests are not verified.
  - The Android app's native wrapper enforces Google sign-in before loading the map. The standalone website/PWA source has no Firebase sign-in implementation, so opening the website directly in a browser does not have the same sign-in gate.
  - The automatic updater's metadata is current, but the complete in-app update/install/restart flow and update-banner clearing have not been device-tested.
  - The 2.0 test plan still says 2.0 is **NOT READY FOR RELEASE** and needs refreshing to reflect the newer offline-map code while retaining the outstanding device-test gates.
- **Automatic-update status:** Default-branch `version.json` is set to 1.173 and points to `https://github.com/dominichuddleston4-sys/CBR_URBEX/releases/latest/download/app-release.apk`. The update-check code is present and the Pages metadata-publish workflow step passed. No end-to-end automatic-update test evidence is available.
- **Comparison with v1.47:** 46 commits were added between the recorded v1.47 release and v1.173. The cumulative code changes include the native offline-map screen, launcher icon resources, Settings sign-out, owner admin controls, Firestore rules/deployment workflow and APK/Pages workflow changes. This entry records the latest published release; it does not claim that every intermediate v1.48–v1.172 tag was individually audited.

### v1.47 — 12 November 2026
- Public GitHub release published with APK asset `app-release.apk`.
- AndroidX enabled to support Firebase Authentication dependencies.
- Google sign-in gate implemented in the Android app before the map is loaded.
- **User-tested:** sign-in screen appears before map access; successful sign-in opens the map; session remains remembered after app restart.
- **Not yet verified:** sign-out flow, revoked/expired sessions, offline operation, or protection of any separate shared-data backend.

### v1.39 — 11 November 2026
- Public GitHub release published.
- APK asset `app-release.apk` uploaded successfully.
- Feature/fix details were not fully documented from a verified code comparison.

### v1.28 — 11 November 2026 (earlier recorded publication)
- Earlier project status recorded a successful APK build and public publication.
- The `version.json` update manifest was recorded as set to 1.28.
- GitHub Pages deployment was reported successful.
- Automatic updater still required further fixing at that time.
- Netlify deployment was not verified in that status report.

## Canberra Explorer 2.0 — development plan

**Tracking issue:** [Canberra Explorer 2.0 implementation checklist](https://github.com/dominichuddleston4-sys/CBR_URBEX/issues/1). The workstreams and acceptance tests are recorded there. This is project tracking only; it does not mean the features are implemented.

The next development work is being started with offline maps as the first priority. The user has requested
that the offline map and offline-only directions be available from the app's Settings.

### 1. Offline Canberra map — first priority
**Status: Native MapLibre offline-map screen and bounded area-download code are included in v1.173; physical-device and airplane-mode verification is still pending.**

Requirements:
- Add an Offline map section in the Android app's Settings.
- Let the user download a clearly defined Canberra/ACT map area while connected to the internet.
- Show download progress, downloaded size, available storage, and map-data date.
- Store the map package on the device and allow the map to display without a network connection.
- Keep the existing online map available when internet access exists.
- Provide controls to update or delete the offline map package.
- Use a map-data source and tile format whose licence explicitly permits offline downloads and local use; do not bulk-download from public OpenStreetMap tile servers.

### 2. Offline directions — after offline map storage works
**Status: Planned; not implemented.**

Requirements:
- Provide route calculation inside Canberra Explorer without opening Google Maps or another online service.
- Download the routing data needed for the supported offline area.
- Clearly show when a route is unavailable because the destination is outside the downloaded area or routing data is missing.
- Test walking and driving only if the selected offline routing engine and data support those modes.
- Make it clear that offline directions are not live traffic-aware and may not reflect closures or access restrictions.
- Keep lawful-access and field-safety warnings visible; a route to a map pin is not permission to enter a site.

### 3. App icon and presentation
**Status: Existing web/PWA icon files are present; Android launcher icon still needs device verification.**
- Check that the installed Android app shows the intended Canberra Explorer icon rather than the generic Android icon.
- Replace/add Android launcher icon resources if required and verify on a physical phone.

### 4. Shared location photo galleries
**Status: Planned; not implemented.**
- Add a photo gallery to each location.
- Make uploaded photos available to all signed-in app users, not only on the uploader's phone.
- Store photos in cloud storage and associate them with a location and authenticated uploader.
- Include uploader attribution, reporting/removal controls, and secure access rules.
- Do not ship uploads until the database and storage rules have been tested.

### 5. Google account access
**Status: Implemented in v1.47 and user-tested for sign-in and session persistence.**
- Require Google sign-in before showing the map.
- Test sign-out, account switching, cancelled sign-in, expired/revoked sessions, and re-authentication.
- Ensure shared photo and location data are protected by backend security rules; a sign-in screen alone does not secure backend data.

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

## 2.0 development work — 12 November 2026

- Created implementation tracking issue: https://github.com/dominichuddleston4-sys/CBR_URBEX/issues/1
- Added technical architecture notes at [docs/CANBERRA_EXPLORER_2_0_TECHNICAL_PLAN.md](docs/CANBERRA_EXPLORER_2_0_TECHNICAL_PLAN.md).
- Added Android launcher icon vector resources (adaptive icon for Android 8.0+ and legacy vector fallback) and wired the icon into AndroidManifest.xml.
- **Build status:** signed APK workflow is running for the icon change; do not install or announce a new release until it completes successfully and the release asset is verified.
- **Device verification:** pending; the owner must check the icon on their Android home screen after installing a verified APK.
- **Offline ACT map:** not implemented. MapLibre Native Android's offline-region APIs are a candidate, but this requires moving the map from the current Leaflet WebView to a native renderer or a deliberate hybrid architecture, plus a tile source with verified offline-use rights.
- **Offline routing:** not implemented. Valhalla is a candidate requiring a native Android proof-of-concept and ACT routing data. Do not rely on GraphHopper's older offline Android integration without verifying support.
- **Shared photo galleries:** not implemented; Firebase database/storage rules and moderation are still required.


## 2.0 testing

The release-gate checklist is maintained in [docs/CANBERRA_EXPLORER_2_0_TEST_PLAN.md](docs/CANBERRA_EXPLORER_2_0_TEST_PLAN.md). The checklist records implemented, tested, blocked and not-implemented items separately. 2.0 must not be released until all mandatory tests pass.


## 2.0 development status update — 13 November 2026

- The v1.173 source includes the native MapLibre offline-map activity and its ACT-area download flow. This supersedes the earlier 12 November note that the offline-map implementation had not yet been started; it does **not** supersede the outstanding device, airplane-mode, licensing/attribution and recovery tests.
- Offline directions remain not implemented.
- Shared location photo galleries and uploads remain not implemented. Firestore rules deployment succeeded, but the inspected workflow does not deploy Firebase Storage rules.
- The mandatory Google sign-in gate is present in the Android wrapper. The standalone web/PWA page still has no web sign-in gate.
- The 2.0 test plan should be refreshed to distinguish code implemented from tests passed. Its release decision remains **NOT READY FOR RELEASE** until mandatory tests are completed and evidenced.
