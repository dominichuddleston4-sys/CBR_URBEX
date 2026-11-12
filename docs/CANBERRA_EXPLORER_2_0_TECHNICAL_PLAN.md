# Canberra Explorer 2.0 — technical implementation notes

Last reviewed: 12 November 2026

## Current architecture

- Android app is a Java Activity containing a WebView.
- The WebView loads the hosted Leaflet app from GitHub Pages.
- Leaflet map layers are remote tile services. The current app therefore does not have a reliable ACT-wide offline basemap.
- The current Android manifest did not declare a launcher icon. Root-level PNG/SVG files are PWA assets, not Android launcher resources.

## Offline basemap direction

### Candidate: MapLibre Native Android offline regions

MapLibre Native Android exposes OfflineManager, OfflineRegion, OfflineTilePyramidRegionDefinition and offline region status/progress APIs. Region definitions include a style URL, geographic bounds and zoom range. This fits a Settings workflow with a named ACT package, progress, completed bytes/resources, pause/resume and delete.

Official API references:
- https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.offline/index.html
- https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.offline/-offline-region-status/index.html

Important architecture implication: the current map is Leaflet inside a WebView. MapLibre Native is an Android map renderer, so a robust implementation should introduce a native map screen or a deliberate migration. Do not assume MapLibre's native offline API can be called by the existing browser Leaflet map.

### Tile/data licensing and hosting gate

OpenStreetMap data is ODbL open data. Any derived database must preserve the applicable ODbL obligations and display OpenStreetMap attribution. A provider's online tile service must not be scraped or bulk-downloaded unless its licence explicitly permits offline packages.

References:
- https://www.openstreetmap.org/copyright
- https://download.geofabrik.de/australia-oceania/australia.html

Geofabrik's Australia PBF extract is large (about 929 MB at the time checked), so the ACT dataset should be clipped from a licensed OSM extract and processed in a controlled build pipeline. Do not ship the whole Australia extract to phones.

Before coding the production downloader, select either:
1. A commercial tile provider whose written plan terms permit this app's ACT-wide offline downloads and user storage; or
2. A self-generated, self-hosted ACT tile package from OSM data, with ODbL attribution and share-alike compliance assessed.

The exact provider, zoom levels, file size, update cadence and storage estimate remain open decisions. They must be verified before a production download button is enabled.

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

The Android launcher resources are being added under app/src/main/res. The manifest must reference the adaptive icon and the project must pass a signed APK build. Device verification is still required to confirm the icon appears on the user's Android launcher.

## Shared photo galleries

The current app uses Firebase Authentication, but sign-in alone does not protect a future gallery. Before enabling uploads:
- Add a Firestore location-photo metadata collection and Firebase Storage object paths scoped by location.
- Require authenticated uploader identity in security rules.
- Restrict MIME types and file sizes.
- Store uploader UID and display attribution safely.
- Add reporting and moderation/removal flow.
- Test cross-account visibility and unauthorised reads/writes using Firebase rules tests.

Initial Firestore and Storage rules plus firebase.json have now been committed for review, with a security/deployment note at docs/FIREBASE_PHOTO_GALLERY_SECURITY.md. They are not deployed or emulator-tested, and no photo UI/backend flow exists yet. Do not add a client-only upload feature and describe it as shared or secure.

## Current implementation status

- Google sign-in gate: shipped in v1.47 and user-tested for sign-in and session persistence.
- Android launcher icon: resources and manifest wiring added in the current development commits; signed build and device verification pending.
- ACT-wide offline map: not implemented; provider/licensing and renderer migration must be resolved.
- Offline routing: not implemented; native Valhalla Android proof-of-concept required.
- Shared photo galleries: not implemented; backend schema, security rules and moderation required.
