# Canberra Explorer — App Update Log

This document is the running release record for the Canberra Explorer Android app in the
[CBR_URBEX GitHub repository](https://github.com/dominichuddleston4-sys/CBR_URBEX).

Record shipped changes only when supported by repository evidence, release assets, build results,
or user/device testing. Keep planned features separate from features confirmed as shipped.

## Current published release

- **Version:** v1.47
- **Published:** 12 November 2026
- **APK:** [Download app-release.apk](https://github.com/dominichuddleston4-sys/CBR_URBEX/releases/download/v1.47/app-release.apk)
- **Release page:** https://github.com/dominichuddleston4-sys/CBR_URBEX/releases/tag/v1.47
- **Publication status:** GitHub release is public and the APK asset is uploaded.
- **Device verification:** User reports successful installation and confirms Google sign-in appears before map access, the map opens after sign-in, and the sign-in session remains remembered after closing and reopening the app.

## Release history

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
**Status: Development not yet implemented.**

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
