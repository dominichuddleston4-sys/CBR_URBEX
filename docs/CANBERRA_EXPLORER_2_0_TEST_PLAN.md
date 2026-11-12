# Canberra Explorer 2.0 — Test Plan

Last reviewed: 12 November 2026

This document is the release-gate checklist for Canberra Explorer 2.0. A test is not passed merely because the APK compiles. Record the device, Android version, build/release tag, tester, date, result and evidence for each executed test.

## Release rules

- Keep the current stable release available until all mandatory tests pass.
- Do not publish a 2.0 release while any mandatory test is marked NOT IMPLEMENTED, BLOCKED or FAIL.
- Test signed APK upgrades over the currently installed app; do not require users to uninstall and lose app data.
- Verify the signing certificate is identical to the installed production app before distributing an update.
- Test on a real Android device and in airplane mode where specified.
- Do not enable user photo uploads until Firestore and Storage security rules and moderation are deployed and tested.
- Offline map data must come from a provider/dataset whose licence permits the intended offline use. Do not bulk-download the public OSM standard tile service.

## Current baseline

| Area | Current status | Evidence / next action |
|---|---|---|
| Google sign-in before map | USER-TESTED on v1.47 | User confirmed sign-in opens map |
| Sign-in persistence after app restart | USER-TESTED on v1.47 | User confirmed session persists |
| Native Android launcher icon | BUILD-PASSED; DEVICE TEST REQUIRED | Install the latest signed build and inspect launcher, app drawer and Settings |
| ACT-wide offline basemap | NOT IMPLEMENTED | Select licensed data source and migrate/provide native offline map renderer |
| Offline driving directions | NOT IMPLEMENTED | Native routing proof-of-concept and ACT graph required |
| Offline walking directions | NOT IMPLEMENTED | Must be tested independently from driving |
| Offline cycling directions | NOT IMPLEMENTED | Must be tested independently before exposing profile |
| Shared location photo galleries | NOT IMPLEMENTED | Backend, security rules, upload UI and moderation required |
| Authentication edge cases | PARTIAL / NOT VERIFIED | Execute cases below |
| Upgrade and rollback safety | NOT VERIFIED | Verify signature, version and update flow |

## A. Build and distribution gates

- [ ] A clean CI build completes from the exact commit intended for release.
- [ ] APK is signed with the same permanent signing certificate as the installed stable app.
- [ ] APK package name remains au.com.canberraexplorer.app.
- [ ] versionCode increases monotonically; versionName matches release notes.
- [ ] APK installs as an in-place update without uninstalling the stable app.
- [ ] Existing sign-in session and saved map data behave as documented after upgrade.
- [ ] In-app update detects the release, downloads it, prompts for install and handles cancellation safely.
- [ ] Update prompt does not reappear after a successful update/restart.
- [ ] Release page APK, version.json and displayed version all refer to the same build.
- [ ] No test or prerelease build is accidentally marked as the latest stable release.

## B. Google authentication

- [ ] Fresh install: map remains gated until successful Google sign-in.
- [ ] Successful sign-in opens the map.
- [ ] Force-close and reopen: valid session persists.
- [ ] User signs out: map is gated again.
- [ ] Account switching uses the newly selected account.
- [ ] User cancels sign-in: app remains usable at the sign-in screen and does not crash.
- [ ] Network loss during sign-in produces a clear retry message.
- [ ] Expired/revoked session is handled without exposing the map to an unauthenticated user.
- [ ] Back navigation cannot bypass the sign-in gate.

## C. Launcher icon

- [ ] Icon appears on Android home screen after install/update.
- [ ] Icon appears in app drawer and Android app info.
- [ ] Adaptive icon renders correctly on light/dark and themed-icon launchers.
- [ ] No clipping, blank icon, generic Android icon or unreadable foreground.
- [ ] Existing app retains the correct icon after an in-place update.

## D. ACT-wide offline map

Status: NOT IMPLEMENTED — all tests blocked until the feature exists.

- [ ] Settings offers an explicit ACT offline map download.
- [ ] Download size estimate, coverage boundary, map-data date and storage requirement are shown before download.
- [ ] Download progress is visible and survives app backgrounding.
- [ ] Interrupted download can resume or safely restart.
- [ ] Completed package integrity is verified before being marked ready.
- [ ] Downloaded map opens with airplane mode enabled.
- [ ] Test coverage includes Canberra and regional ACT areas within the published boundary.
- [ ] Pan/zoom works offline throughout downloaded coverage and clearly identifies out-of-coverage areas.
- [ ] Missing/corrupt package produces a useful recovery message.
- [ ] User can check data date, update and delete the package in Settings.
- [ ] Attribution and licence notices are visible.
- [ ] No unsupported scraping or bulk downloading of public standard OSM tiles.

## E. Offline directions

Status: NOT IMPLEMENTED — all tests blocked until a mobile routing engine and ACT routing dataset exist.

- [ ] Route calculation succeeds in airplane mode for each supported travel mode.
- [ ] Route stays within downloaded coverage or clearly reports when it cannot.
- [ ] Start and destination selection work without network access.
- [ ] Route line, distance and turn instructions agree with the calculated route.
- [ ] Driving, walking and cycling are each tested independently; unsupported modes remain hidden.
- [ ] Disconnected roads, bridges, ferries, private roads and restricted access are handled safely.
- [ ] No claim of live traffic, current closures or real-time access conditions.
- [ ] Route calculation failure never falls back silently to an online service.
- [ ] Attribution and routing-data date are displayed.

## F. Shared location photo galleries

Status: SECURITY RULES DRAFTED BUT NOT DEPLOYED; gallery UI and upload flow NOT IMPLEMENTED. Uploads must remain disabled until backend controls pass.

- [ ] Authenticated user can upload an allowed image within configured size limits.
- [ ] A second authorised account can see the same location gallery.
- [ ] Unauthenticated user cannot read or upload gallery content.
- [ ] User cannot overwrite/delete another user's upload unless authorised as moderator.
- [ ] Invalid MIME type, oversized file and malformed metadata are rejected.
- [ ] Uploader attribution is displayed without exposing private account information.
- [ ] User can report an image.
- [ ] Moderator can remove reported content and the removal is reflected for all users.
- [ ] Firestore and Storage rules are tested against unauthorised reads/writes.
- [ ] Upload retry, cancellation and poor-network cases do not create duplicate/broken entries.

## G. Privacy, safety and regression

- [ ] App does not expose precise private/restricted urbex locations without a lawful basis.
- [ ] Location permissions are requested only when needed and denial is handled gracefully.
- [ ] Existing online map, pins, filters, backups and Settings still work.
- [ ] No crash on rotation, background/resume or process recreation.
- [ ] Offline/online transitions do not lose saved data.
- [ ] Error messages explain what the user can do next.
- [ ] Privacy notice and data handling match the actual photo/authentication implementation.

## Test record template

For each test, record:

- Test ID:
- Build tag / commit:
- Device and Android version:
- Date and tester:
- Steps and expected result:
- Actual result:
- PASS / FAIL / BLOCKED / NOT IMPLEMENTED:
- Evidence (screenshot/log link):
- Defect issue:

## Release decision

2.0 release status: **NOT READY FOR RELEASE**

A release can be considered only after all mandatory sections have been implemented, tested and evidenced, with no unresolved critical/high-severity defects. The current stable release must remain available until then.
\n## Latest implementation note — 12 November 2026\n\n- Added initial `firestore.rules`, `storage.rules` and `firebase.json` for the proposed shared photo gallery.\n- Added `docs/FIREBASE_PHOTO_GALLERY_SECURITY.md` documenting the data model, custom moderator claim and deployment gates.\n- These files are committed but **not deployed** to Firebase; no gallery UI or upload capability is enabled. Emulator tests and owner approval are still required.\n