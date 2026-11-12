# ACT offline map — implementation spike

Status: **design spike only; no offline map is shipped by this branch.**

## Goal

Provide a downloadable ACT basemap inside Canberra Explorer that remains usable in airplane mode. The existing Leaflet map runs in a WebView and requests remote tiles, so simply enabling browser cache or adding a download button would not meet this requirement.

## Proposed Android renderer

Evaluate **MapLibre Native Android 13.7.1** as the native map renderer for the offline-map screen. The project currently uses Java, Android Gradle Plugin 8.7.3, minSdk 23, and Maven Central is already configured.

- Maven coordinate: `org.maplibre.gl:android-sdk:13.7.1`
- Official Android API: https://maplibre.org/maplibre-native/android/api/
- Offline API: https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.offline/index.html
- The SDK's `OfflineManager` creates and lists offline regions; `OfflineRegionStatus` reports completed/required resources and byte counts.
- Pin the SDK version. Validate renderer compatibility and APK size on the actual target Android device before merging it into the production app.

## Data-source and licence gate

No production tile URL has been selected. The public OpenStreetMap standard tile server must **not** be bulk-downloaded or used as an offline package source.

Preferred no-subscription direction: generate a clipped ACT map dataset from OpenStreetMap data using a reproducible build pipeline, then serve that generated package from a controlled static endpoint. Preserve ODbL obligations and visible attribution. Confirm the chosen tile format, style assets, hosting terms, update mechanism and redistribution rights before enabling downloads.

References:
- OSM data copyright/licence: https://www.openstreetmap.org/copyright
- Geofabrik Australia source extract: https://download.geofabrik.de/australia-oceania/australia.html
- MapLibre Native Android offline-region API: https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.offline/index.html

Do not embed a third-party style or tile URL in the app until its offline caching and redistribution terms are verified. A working online style alone is not proof that offline downloads are permitted.

## Safe implementation sequence

1. **Spike branch (this branch):** record architecture, dependencies, licence gates and acceptance criteria without changing the stable app.
2. Build a minimal native MapLibre screen on a dedicated branch and verify it renders a test style on the user's Android device.
3. Build a small ACT sample package from a licensed/self-generated dataset; verify the package can be downloaded and used after force-stop and in airplane mode.
4. Add Settings controls only after the sample package works: download, progress, cancel/retry, integrity check, data date, update and delete.
5. Migrate or bridge the existing location data and pins into the native map; retain the current online map until regression testing passes.
6. Test ACT coverage, low storage, interrupted downloads, corrupt packages, no network, app upgrades and attribution.
7. Merge to main only after CI and device tests pass. Do not publish a 2.0 APK while required tests are blocked.

## Acceptance criteria

- Map renders without network after a successful package download.
- Published coverage boundary and map-data date are visible.
- Download progress and storage estimate are shown before download.
- Interrupted or failed downloads never appear as complete.
- User can update and delete the offline package.
- Map clearly handles locations outside downloaded coverage.
- OSM attribution and licence notices remain visible.
- No bulk download from the public OSM standard tile server.
- Existing sign-in gate, map pins, filters, saved data, backups and update mechanism continue working.

## Explicitly out of scope for this map spike

Offline turn-by-turn routing is a separate workstream. Downloading a basemap does not provide offline directions. Routing needs its own local road graph, engine and independent driving/walking/cycling tests.
