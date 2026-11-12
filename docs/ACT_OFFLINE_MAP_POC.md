# ACT offline map — implementation spike

Status: **design spike only; no offline map is shipped by this branch.** The user has approved the free, self-generated OpenStreetMap data approach for the ACT.

## Goal

Provide a downloadable ACT basemap inside Canberra Explorer that remains usable in airplane mode. The existing Leaflet map runs in a WebView and requests remote tiles, so simply enabling browser cache or adding a download button would not meet this requirement.

## Approved data-source direction

Use the dedicated Geofabrik Australian Capital Territory extract as the starting source. It already provides ACT-scoped OSM data and an ACT boundary polygon file, avoiding the need to clip a much larger Australia extract manually.

- Source page: https://download.geofabrik.de/australia-oceania/australia/act.html
- Input: `act-latest.osm.pbf`; use the matching `.poly` file for boundary/reference checks.
- The source page reports a current PBF extract of approximately 18.5 MB at the time this plan was updated; never hardcode that size because the extract changes.
- Evaluate Planetiler as the first vector-tile generation tool. Its documented outputs include MBTiles and PMTiles; test the chosen output against the actual Android renderer before committing to a format.
- Host generated files at a controlled static endpoint, with versioned filenames, checksums and a small manifest containing source timestamp, generator version, coverage and attribution.
- OSM data is available under the Open Database License (ODbL); include clear attribution and preserve applicable share-alike obligations.
- Do **not** bulk-download tiles from the public OpenStreetMap standard tile server.
- Before public release, verify that the chosen generated format, style assets and hosting arrangement comply with ODbL and the host's terms. Do not include third-party style or tile assets unless their redistribution/offline terms are verified.
- The user has approved this general free OSM approach; approval does not waive the need to validate licensing, hosting, package size or coverage before release.

References:
- OSM data copyright/licence: https://www.openstreetmap.org/copyright
- Geofabrik ACT extract and polygon: https://download.geofabrik.de/australia-oceania/australia/act.html
- Planetiler project and attribution notes: https://github.com/onthegomap/planetiler

## Proposed Android renderer

Evaluate **MapLibre Native Android 13.7.1** as the native map renderer for the offline-map screen. The project currently uses Java, Android Gradle Plugin 8.7.3, minSdk 23, and Maven Central is already configured.

- Maven coordinate: `org.maplibre.gl:android-sdk:13.7.1`
- Official Android API: https://maplibre.org/maplibre-native/android/api/
- Offline API: https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.offline/index.html
- The SDK's `OfflineManager` creates and lists offline regions; `OfflineRegionStatus` reports completed/required resources and byte counts.
- Pin the SDK version. Validate renderer compatibility and APK size on the actual target Android device before merging it into the production app.

## Safe implementation sequence

1. **Spike branch (this branch):** record architecture, approved data direction, licence gates and acceptance criteria without changing the stable app.
2. Build a reproducible ACT package-generation pipeline from the Geofabrik ACT PBF and matching polygon; record source date, generator version, output format, checksums and licence/attribution.
3. Build a minimal native MapLibre screen on a dedicated branch and verify it renders a test style on the user's Android device.
4. Download a small ACT sample package; verify it remains usable after force-stop and in airplane mode.
5. Add Settings controls only after the sample package works: download, progress, cancel/retry, integrity check, data date, update and delete.
6. Migrate or bridge the existing location data and pins into the native map; retain the current online map until regression testing passes.
7. Test ACT coverage, low storage, interrupted downloads, corrupt packages, no network, app upgrades and attribution.
8. Merge to main only after CI and device tests pass. Do not publish a 2.0 APK while required tests are blocked.

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
