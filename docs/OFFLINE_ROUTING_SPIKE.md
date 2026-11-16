# Offline driving and walking routing spike

Status: investigation started on `feature/offline-improvements-test`. This is not a completed feature and must not be presented as an installable offline-routing release.

## Existing blockers

- The current native map screen uses a remote OpenFreeMap style and user-triggered regional downloads. It does not contain a preloaded ACT basemap.
- The current app has no offline route engine or bundled road graph.
- A successful Android APK build alone does not prove offline map or route behaviour.

## Routing engine candidate to validate

The Massif Maps project documents an Android standalone Valhalla routing library that accepts local Valhalla-format `.vtiles` data and exposes offline routing. It documents both pedestrian and vehicle profiles. The project states that it is BSD-3-Clause licensed.

Reference: https://github.com/massif-maps/MassifMaps

This is a candidate, not yet an approved dependency. Before integrating it:
1. Verify the upstream repository, release artefact, Android API compatibility, ABI support, dependency size and licence.
2. Build a minimal Android proof-of-concept that opens a local ACT routing package with no network connection.
3. Calculate and verify one car route and one pedestrian route in airplane mode, including route geometry and sensible road/path restrictions.
4. Confirm memory use and startup time on a representative Android phone.
5. Only then wire the route service into Canberra Explorer.

## Required offline data

- Prepare a legally sourced OpenStreetMap-derived ACT road/path graph in the exact format supported by the chosen engine.
- Include the ACT routing graph in the installable app package for the requested no-manual-download experience, subject to APK size limits; otherwise stop and report the limitation rather than silently requiring a download.
- Record data date, source, checksum, coverage bounds and ODbL attribution.
- Ensure car and pedestrian data/profile coverage is tested independently.

## Map requirements

- The offline map must render locally with no external style, glyph, sprite or tile requests.
- The app must open the map in-app. “Show on map” must not launch Google Maps.
- “Get directions” may offer Google Maps only as an explicitly selected online alternative; the offline route calculation must stay inside Canberra Explorer.
- Offline route results must warn that they do not include live traffic or temporary closures and do not grant access to private or restricted land.

## Release gate

Do not create or label an APK as the requested test build until:
- the ACT basemap is available at first launch without downloading a region;
- map rendering succeeds in airplane mode;
- car routing succeeds in airplane mode;
- pedestrian routing succeeds in airplane mode;
- the test APK builds successfully; and
- the APK is installed and tested on a physical Android device.

Until those checks pass, any APK is only an engineering prototype and must be clearly labelled as such.
