# Canberra Explorer 2.0 — technical implementation notes

Last reviewed: 12 November 2026

## Current architecture

- Android app is a Java Activity containing a WebView.
- The WebView loads the hosted Leaflet app from GitHub Pages.
- Leaflet map layers are remote tile services. The current app therefore does not have a reliable ACT-wide offline basemap.
- The current Android manifest did not declare a launcher icon. Root-level PNG/SVG files are PWA assets, not Android launcher resources.

## Offline basemap implementation in the development branch

A native MapLibre screen has been added in `OfflineMapActivity`. It offers Central, North, South, West and East ACT sector choices, downloads one region at a time, stores saved place pins supplied by the existing web map, and offers a delete-all control. Multiple area packs can be saved; the first download for each area requires internet. Each development pack is capped at 3,000 tiles and stops/removes a region if its completed resource size exceeds 50 MB. The device-wide MapLibre tile ceiling is set to 15,000 to allow up to five regional packs. The style is OpenFreeMap Liberty and attribution is supplied by the MapLibre style controls.

The downloader sets an app-identifying User-Agent, limits map requests to four in parallel, and enforces OpenFreeMap's 3,000-tile / 50 MB per-download ceiling. The native screen and download flow still require a successful CI build and on-device airplane-mode tests before being considered shipped. Offline routing is not included.

## Offline basemap direction

### Candidate: MapLibre Native Android offline regions

MapLibre Native Android exposes OfflineManager, OfflineRegion, OfflineTilePyramidRegionDefinition and offline region status/progress APIs. Region definitions include a style URL, geographic bounds and zoom range. This fits a Settings workflow with a named ACT package, progress, completed bytes/resources, pause/resume and delete.

Official API references:
- https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.offline/index.html
- https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.offline/-offline-region-status/index.html

Architecture note: the original Leaflet map remains in the WebView and continues to provide the online experience. The development branch now opens a separate native MapLibre screen from the Offline map button. Place coordinates are passed from the existing web map into the native screen so the offline view retains the user's saved location pins. The native offline API is not being called from Leaflet.

### Tile/data licensing and hosting notes

The development proof-of-concept uses OpenFreeMap Liberty with MapLibre Native. OpenFreeMap's current terms expressly allow user-requested offline downloads up to 3,000 tiles or 50 MB per download, with no more than four parallel requests and a unique app-identifying User-Agent. The downloader enforces those limits and retains map data only on the device. Five ACT sectors can be downloaded as separate packs, one at a time; this is not one unlimited ACT-wide download.

OpenFreeMap terms and quick start:
- https://openfreemap.org/tos/
- https://openfreemap.org/quick_start/

The map data is from OpenStreetMap. Display attribution through MapLibre's attribution control and retain ODbL compliance for any derived datasets. If a future release needs larger regions or higher zoom levels than the provider's per-download limits allow, contact OpenFreeMap or move to a self-hosted regional extract; do not bypass provider limits.

References:
- https://www.openstreetmap.org/copyright
- https://download.geofabrik.de/australia-oceania/australia.html

## Offline routing direction

Valhalla is a stronger technical candidate for driving, walking and cycling than assuming GraphHopper's old Android offline integration is supported. Valhalla documents a tiled graph structure intended to support regional extracts, partial updates and offline routing; its routing engine is C++ and would require a proven Android NDK/mobile integration plus ACT graph packaging.

References:
- https://valhalla.github.io/valhalla/
- https://valhalla.github.io/valhalla/api/route/overview/

GraphHopper's current project README says offline mobile routing is no longer officially supported. BRouter is an established MIT-licensed Android offline router, but its primary emphasis is cycling and its existing Android integration is commonly via a separate app/service. Do not choose either as a drop-in solution without a small Android proof-of-concept.

References:
- https://github.com/graphhopper/graphhopper
- https://github.com/abrensch/brouter

Recommended sequence:
1. Build a minimal Android NDK proof-of-concept that loads a clipped ACT Valhalla graph and calculates a route with airplane mode enabled.
2. Test car, pedestrian and bicycle profiles separately; only expose profiles that pass.
3. Package the graph with the ACT basemap release metadata, while allowing routing data to update independently.
4. Add route display, offline coverage checks and clear stale-data warnings.

Offline directions must not imply live traffic, current road closures, safe access to abandoned sites, or permission to enter private land.

## App icon

Adaptive and round launcher icons plus API 23-25 vector fallbacks are committed under app/src/main/res and referenced by the manifest. A signed APK build and device verification are still required to confirm the icon appears on the user's launcher.

## Shared photo galleries

The development branch includes a native Firebase bridge and per-location gallery UI. Uploads create pending Firestore metadata first, then store the image in a location/uploader-scoped Storage path. The rules restrict file types and size, bind metadata to the exact object path, and allow public signed-in reads only after approval. Pending records and files are visible only to their uploader and trusted moderators. The UI shows uploader attribution, supports reports, and exposes moderator-only approve/remove actions.

Firestore and Storage rules are committed for review but are not deployed or emulator-tested. Cross-service Storage rules require the Firebase Storage service account to be granted Firestore read permission. The Firebase project owner must review and deploy the rules, configure the moderator custom claim through a trusted admin process, and pass cross-account upload/read/report/moderation tests before the gallery is considered live.

## Current implementation status

- Google sign-in gate: shipped in v1.47 and user-tested for sign-in and session persistence.
- Android launcher icon: adaptive icon plus API 23-25 vector fallbacks and a dedicated round icon are wired in the development branch; signed build and launcher verification pending.
- Offline map: native MapLibre regional downloads for five ACT sectors are implemented on the development branch; CI and on-device airplane-mode tests pending. Multiple area packs can be saved, one download at a time, within provider limits.
- Offline routing: not implemented; native Valhalla Android proof-of-concept required.
- Shared photo galleries: Android native Firebase bridge and per-location gallery UI are implemented on the development branch, including attribution, pending review, reports and moderator-only approve/remove. Firestore/Storage rules remain undeployed; cross-account and moderation tests are pending.
