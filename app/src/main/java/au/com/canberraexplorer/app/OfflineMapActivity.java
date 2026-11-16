package au.com.canberraexplorer.app;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.CircleLayer;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.android.offline.OfflineManager;
import org.maplibre.android.offline.OfflineRegion;
import org.maplibre.android.offline.OfflineRegionError;
import org.maplibre.android.offline.OfflineRegionStatus;
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition;
import org.maplibre.android.module.http.HttpRequestUtil;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

import java.nio.charset.StandardCharsets;


public class OfflineMapActivity extends Activity {
    private static final String STYLE_URL = "https://tiles.openfreemap.org/styles/liberty";
    private static final long MAX_REGION_BYTES = 50L * 1024L * 1024L;
    private static final long MAX_TILES = 3000L;
    private static final long MAX_TOTAL_TILES = 15000L;
    private MapView mapView;
    private OfflineManager offlineManager;
    private TextView status;
    private Spinner regionPicker;
    private OfflineRegion activeRegion;
    private org.maplibre.android.maps.MapLibreMap activeMap;
    private Style activeStyle;
    private double routeDestinationLat;
    private double routeDestinationLon;
    private String routeMode = "driving";
    private static final int ROUTE_LOCATION_PERMISSION = 7001;

    private static final String[] REGIONS = {
        "Central ACT", "North ACT", "South ACT", "West ACT", "East ACT"
    };
    private static final double[][] BOUNDS = {
        {-35.48, 148.96, -35.28, 149.30},
        {-35.32, 148.90, -35.12, 149.35},
        {-35.92, 148.95, -35.48, 149.35},
        {-35.65, 148.76, -35.25, 149.00},
        {-35.62, 149.25, -35.12, 149.40}
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureMapNetwork();
        MapLibre.getInstance(this);
        offlineManager = OfflineManager.Companion.getInstance(this);
        offlineManager.setOfflineMapboxTileCountLimit(MAX_TOTAL_TILES);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(10, 24, 17));

        TextView heading = new TextView(this);
        heading.setText("Offline ACT map");
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(22);
        heading.setPadding(16, 14, 16, 6);
        root.addView(heading);

        TextView intro = new TextView(this);
        String routeName = getIntent().getStringExtra("route_destination_name");
        routeMode = getIntent().getStringExtra("route_mode");
        if (routeMode == null) routeMode = "driving";
        routeDestinationLat = getIntent().getDoubleExtra("route_destination_lat", 0);
        routeDestinationLon = getIntent().getDoubleExtra("route_destination_lon", 0);
        boolean hasRouteTarget = routeName != null && !routeName.trim().isEmpty();
        intro.setText(hasRouteTarget
            ? "Offline " + ("walking".equals(routeMode) ? "walking" : "driving") + " destination: " + routeName + ". Download the separate routing pack while online, then use GPS for written offline directions. No voice prompts."
            : "Download Canberra map areas and the separate road-and-footpath routing pack while online. Once saved, offline routes use GPS and written instructions. No voice prompts.");
        intro.setTextColor(Color.LTGRAY);
        intro.setTextSize(13);
        intro.setPadding(16, 0, 16, 10);
        root.addView(intro);

        mapView = new MapView(this);
        root.addView(mapView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(12, 8, 12, 12);
        root.addView(controls);

        regionPicker = new Spinner(this);
        regionPicker.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, REGIONS));
        controls.addView(regionPicker);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.CENTER);
        Button download = new Button(this);
        download.setText("Download area");
        download.setOnClickListener(v -> confirmDownload());
        Button remove = new Button(this);
        remove.setText("Delete all offline areas");
        remove.setOnClickListener(v -> deleteOfflineRegions());
        buttons.addView(download, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        buttons.addView(remove, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        controls.addView(buttons);

        LinearLayout routingButtons = new LinearLayout(this);
        routingButtons.setGravity(Gravity.CENTER);
        Button downloadRouting = new Button(this);
        downloadRouting.setText("Download routing data");
        downloadRouting.setOnClickListener(v -> downloadRoutingData());
        Button startRoute = new Button(this);
        startRoute.setText("Start offline directions");
        startRoute.setOnClickListener(v -> beginOfflineRoute());
        routingButtons.addView(downloadRouting, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        routingButtons.addView(startRoute, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        controls.addView(routingButtons);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(12);
        status.setPadding(4, 6, 4, 2);
        status.setText("Choose an area to download. Map tiles require internet until an area is saved.");
        controls.addView(status);

        Button close = new Button(this);
        close.setText("Back to Canberra Explorer");
        close.setOnClickListener(v -> finish());
        controls.addView(close);
        setContentView(root);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(map -> {
            activeMap = map;
            String placesGeoJson = getIntent().getStringExtra("places_geojson");
            map.setStyle(new Style.Builder().fromUri(STYLE_URL), style -> {
                activeStyle = style;
                addPlacePins(style, placesGeoJson);
            });
            boolean validDestination = hasRouteTarget && Math.abs(routeDestinationLat) <= 90 && Math.abs(routeDestinationLon) <= 180 && (routeDestinationLat != 0 || routeDestinationLon != 0);
            map.setCameraPosition(new CameraPosition.Builder()
                .target(validDestination ? new LatLng(routeDestinationLat, routeDestinationLon) : new LatLng(-35.28, 149.13))
                .zoom(validDestination ? 15.0 : 10.0).build());
        });
        refreshOfflineStatus();
    }


    private void downloadRoutingData() {
        if (OfflineRoutingPackManager.isInstalled(this)) {
            status.setText("Canberra offline driving and walking routing data is already installed.");
            return;
        }
        status.setText("Downloading Canberra road and footpath data… keep the app open and connected.");
        new Thread(() -> OfflineRoutingPackManager.downloadAndInstall(
            getApplicationContext(), new OfflineRoutingPackManager.Listener() {
                @Override public void onProgress(long downloaded, long total) {
                    runOnUiThread(() -> status.setText(total > 0
                        ? "Downloading routing data: " + (downloaded / (1024 * 1024))
                            + " / " + (total / (1024 * 1024)) + " MB"
                        : "Downloading routing data: " + (downloaded / (1024 * 1024)) + " MB"));
                }
                @Override public void onComplete() {
                    runOnUiThread(() -> status.setText("Offline routing data installed. Driving and walking routes can now be calculated without internet."));
                }
                @Override public void onError(String message) {
                    runOnUiThread(() -> status.setText("Routing data download failed: " + message));
                }
            })).start();
    }

    private void beginOfflineRoute() {
        if (routeDestinationLat == 0 && routeDestinationLon == 0) {
            status.setText("Open Drive or Walk on a saved place first so the app knows the destination.");
            return;
        }
        if (!OfflineRoutingPackManager.isInstalled(this)) {
            status.setText("Download the Canberra routing data first while online.");
            return;
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION }, ROUTE_LOCATION_PERMISSION);
            return;
        }
        obtainLocationAndRoute();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ROUTE_LOCATION_PERMISSION) {
            boolean granted = false;
            for (int result : grantResults) if (result == PackageManager.PERMISSION_GRANTED) granted = true;
            if (granted) obtainLocationAndRoute();
            else status.setText("Location permission is needed to calculate your route from the phone's GPS.");
        }
    }

    private void obtainLocationAndRoute() {
        try {
            LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location best = null;
            String[] providers = { LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER };
            for (String provider : providers) {
                try {
                    Location candidate = manager.getLastKnownLocation(provider);
                    if (candidate != null && (best == null || candidate.getTime() > best.getTime())) best = candidate;
                } catch (Exception ignored) { }
            }
            if (best != null && System.currentTimeMillis() - best.getTime() < 600000L) {
                calculateOfflineRoute(best);
                return;
            }
            status.setText("Waiting for a GPS location fix…");
            String provider = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
            manager.requestSingleUpdate(provider, location -> calculateOfflineRoute(location), Looper.getMainLooper());
        } catch (SecurityException error) {
            status.setText("Location permission is required for offline directions.");
        } catch (Exception error) {
            status.setText("Could not get a GPS fix. Turn on Location and try again.");
        }
    }

    private void calculateOfflineRoute(Location origin) {
        status.setText("Calculating offline " + routeMode + " route…");
        new Thread(() -> {
            try {
                JSONObject route = OfflineRoutingEngine.calculateRoute(this,
                    origin.getLatitude(), origin.getLongitude(),
                    routeDestinationLat, routeDestinationLon, routeMode);
                runOnUiThread(() -> {
                    drawOfflineRoute(route.optString("shape", ""));
                    showWrittenInstructions(route);
                    status.setText("Offline route calculated. Follow the written instructions; voice guidance is disabled.");
                });
            } catch (Exception error) {
                runOnUiThread(() -> status.setText("Could not calculate offline route: "
                    + (error.getMessage() == null ? "Check that routing data covers your area." : error.getMessage())));
            }
        }).start();
    }

    private void showWrittenInstructions(JSONObject route) {
        StringBuilder text = new StringBuilder();
        JSONObject summary = route.optJSONObject("summary");
        if (summary != null) {
            text.append(String.format(java.util.Locale.getDefault(),
                "Distance: %.1f km\\nEstimated time: %.0f min\\n\\n",
                summary.optDouble("length", 0), summary.optDouble("time", 0) / 60.0));
        }
        org.json.JSONArray steps = route.optJSONArray("instructions");
        if (steps != null) {
            for (int i = 0; i < steps.length(); i++) {
                JSONObject step = steps.optJSONObject(i);
                if (step == null) continue;
                text.append(i + 1).append(". ").append(step.optString("text", "Continue"));
                double distance = step.optDouble("distanceKm", 0);
                if (distance > 0) text.append(String.format(java.util.Locale.getDefault(), " (%.2f km)", distance));
                text.append("\\n\\n");
            }
        }
        if (text.length() == 0) text.append("The route was found, but no written manoeuvres were returned.");
        new AlertDialog.Builder(this).setTitle("Offline directions — " + routeMode)
            .setMessage(text.toString()).setPositiveButton("Close", null).show();
    }

    private void drawOfflineRoute(String encodedShape) {
        if (activeStyle == null || encodedShape == null || encodedShape.isEmpty()) return;
        try {
            org.json.JSONArray coordinates = new org.json.JSONArray();
            java.util.List<LatLng> points = decodePolyline6(encodedShape);
            if (points.size() < 2) throw new IllegalStateException("The route geometry was empty.");
            LatLngBounds.Builder bounds = new LatLngBounds.Builder();
            for (LatLng point : points) {
                org.json.JSONArray coordinate = new org.json.JSONArray();
                coordinate.put(point.getLongitude()).put(point.getLatitude());
                coordinates.put(coordinate);
                bounds.include(point);
            }
            JSONObject geometry = new JSONObject().put("type", "LineString").put("coordinates", coordinates);
            JSONObject feature = new JSONObject().put("type", "Feature")
                .put("properties", new JSONObject()).put("geometry", geometry);
            JSONObject collection = new JSONObject().put("type", "FeatureCollection")
                .put("features", new org.json.JSONArray().put(feature));
            GeoJsonSource source = (GeoJsonSource) activeStyle.getSource("ce-route-source");
            if (source == null) {
                activeStyle.addSource(new GeoJsonSource("ce-route-source", collection.toString()));
                LineLayer line = new LineLayer("ce-route-line", "ce-route-source");
                line.setProperties(PropertyFactory.lineColor("#35D66F"),
                    PropertyFactory.lineWidth(5f), PropertyFactory.lineOpacity(0.95f));
                activeStyle.addLayer(line);
            } else source.setGeoJson(collection.toString());
            if (activeMap != null) activeMap.animateCamera(
                org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(bounds.build(), 60), 500);
        } catch (Exception error) {
            status.setText("Route calculated, but the route line could not be drawn: " + error.getMessage());
        }
    }

    private java.util.List<LatLng> decodePolyline6(String encoded) {
        java.util.ArrayList<LatLng> points = new java.util.ArrayList<>();
        int index = 0, lat = 0, lon = 0;
        while (index < encoded.length()) {
            int[] a = decodePolylineValue(encoded, index); lat += a[0]; index = a[1];
            int[] b = decodePolylineValue(encoded, index); lon += b[0]; index = b[1];
            points.add(new LatLng(lat / 1000000.0, lon / 1000000.0));
        }
        return points;
    }

    private int[] decodePolylineValue(String encoded, int start) {
        int result = 0, shift = 0, index = start, value;
        do {
            value = encoded.charAt(index++) - 63;
            result |= (value & 0x1f) << shift;
            shift += 5;
        } while (value >= 0x20 && index < encoded.length());
        int delta = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
        return new int[] { delta, index };
    }

    private void configureMapNetwork() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(4);
        dispatcher.setMaxRequestsPerHost(4);
        OkHttpClient client = new OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .addInterceptor(chain -> chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "CanberraExplorer/2.0 (Android)")
                    .build()))
            .build();
        new HttpRequestUtil().setOkHttpClient(client);
    }

    private void addPlacePins(Style style, String geoJson) {
        if (geoJson == null || geoJson.trim().isEmpty()) return;
        try {
            style.addSource(new GeoJsonSource("ce-place-source", geoJson));
            CircleLayer pins = new CircleLayer("ce-place-pins", "ce-place-source");
            pins.setProperties(
                PropertyFactory.circleRadius(7f),
                PropertyFactory.circleColor("#9CFF57"),
                PropertyFactory.circleStrokeColor("#FFFFFF"),
                PropertyFactory.circleStrokeWidth(2f)
            );
            style.addLayer(pins);
            SymbolLayer labels = new SymbolLayer("ce-place-labels", "ce-place-source");
            labels.setProperties(
                PropertyFactory.textField("{name}"),
                PropertyFactory.textSize(10f),
                PropertyFactory.textColor("#FFFFFF"),
                PropertyFactory.textHaloColor("#0B100D"),
                PropertyFactory.textHaloWidth(1.5f)
            );
            style.addLayer(labels);
        } catch (Exception error) {
            runOnUiThread(() -> status.setText("Map loaded, but saved place pins could not be displayed."));
        }
    }

    private void confirmDownload() {
        new AlertDialog.Builder(this)
            .setTitle("Download offline area?")
            .setMessage("This saves the selected area alongside any areas already downloaded. Connect to Wi-Fi if possible.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Download", (d, w) -> downloadIfNotSaved())
            .show();
    }

    private void downloadIfNotSaved() {
        int selected = regionPicker.getSelectedItemPosition();
        String selectedName = REGIONS[selected];
        offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
            @Override public void onList(OfflineRegion[] regions) {
                if (regions != null) {
                    for (OfflineRegion region : regions) {
                        try {
                            String savedName = new JSONObject(new String(region.getMetadata(), StandardCharsets.UTF_8)).optString("name", "");
                            if (selectedName.equals(savedName)) {
                                runOnUiThread(() -> status.setText(selectedName + " is already saved offline."));
                                return;
                            }
                        } catch (Exception ignored) { }
                    }
                }
                startDownload();
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> status.setText("Could not check saved areas: " + error));
            }
        });
    }

    private void startDownload() {
        if (activeRegion != null) activeRegion.setObserver(null);
        activeRegion = null;
        int selected = regionPicker.getSelectedItemPosition();
        double[] b = BOUNDS[selected];
        LatLngBounds bounds = new LatLngBounds.Builder()
            .include(new LatLng(b[2], b[3]))
            .include(new LatLng(b[0], b[1]))
            .build();
        OfflineTilePyramidRegionDefinition definition =
            new OfflineTilePyramidRegionDefinition(STYLE_URL, bounds, 7.0, 13.0, 1.0f);
        byte[] metadata;
        try {
            JSONObject json = new JSONObject();
            json.put("name", REGIONS[selected]);
            metadata = json.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            status.setText("Could not prepare the offline area.");
            return;
        }
        status.setText("Preparing " + REGIONS[selected] + "… keep the app open.");
        offlineManager.createOfflineRegion(definition, metadata,
            new OfflineManager.CreateOfflineRegionCallback() {
                @Override public void onCreate(OfflineRegion region) {
                    activeRegion = region;
                    region.setObserver(new OfflineRegion.OfflineRegionObserver() {
                        @Override public void onStatusChanged(OfflineRegionStatus s) {
                            long bytes = s.getCompletedResourceSize();
                            long tiles = s.getCompletedTileCount();
                            if (bytes > MAX_REGION_BYTES || tiles > MAX_TILES) {
                                region.setDownloadState(OfflineRegion.STATE_INACTIVE);
                                deleteRegion(region, () -> runOnUiThread(() -> status.setText(
                                    "Download stopped: the area reached the 50 MB / 3,000 tile safety limit.")),
                                    error -> runOnUiThread(() -> status.setText("Download stopped, but the partial pack could not be removed: " + error)));
                                return;
                            }
                            if (s.isComplete()) region.setDownloadState(OfflineRegion.STATE_INACTIVE);
                            String message = s.isComplete()
                                ? "Saved " + REGIONS[selected] + " for offline use."
                                : "Downloading " + REGIONS[selected] + ": " + tiles
                                    + " tiles · " + (bytes / (1024 * 1024)) + " MB";
                            runOnUiThread(() -> status.setText(message));
                        }
                        @Override public void onError(OfflineRegionError error) {
                            runOnUiThread(() -> status.setText("Offline map download error: " + error.getMessage()));
                        }
                        @Override public void mapboxTileCountLimitExceeded(long limit) {
                            region.setDownloadState(OfflineRegion.STATE_INACTIVE);
                            deleteRegion(region, () -> runOnUiThread(() -> status.setText(
                                "Download stopped at the " + limit + " tile limit. Choose a smaller area.")),
                                error -> runOnUiThread(() -> status.setText("Tile limit reached; partial pack could not be removed: " + error)));
                        }
                    });
                    region.setDownloadState(OfflineRegion.STATE_ACTIVE);
                }
                @Override public void onError(String error) {
                    runOnUiThread(() -> status.setText("Could not start download: " + error));
                }
            });
    }

    private void refreshOfflineStatus() {
        offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
            @Override public void onList(OfflineRegion[] regions) {
                if (regions == null || regions.length == 0) {
                    status.setText("No offline areas saved yet.");
                    return;
                }
                java.util.ArrayList<String> names = new java.util.ArrayList<>();
                for (OfflineRegion region : regions) {
                    try {
                        names.add(new JSONObject(new String(region.getMetadata(), StandardCharsets.UTF_8)).optString("name", "Saved area"));
                    } catch (Exception ignored) { names.add("Saved area"); }
                }
                status.setText(regions.length + " offline area(s) saved: " + android.text.TextUtils.join(", ", names) + ".");

            }
            @Override public void onError(String error) {
                status.setText("Could not read offline map storage: " + error);
            }
        });
    }

    private interface DeleteErrorCallback { void onError(String error); }

    private void deleteRegion(OfflineRegion region, Runnable onDeleted, DeleteErrorCallback onError) {
        region.delete(new OfflineRegion.OfflineRegionDeleteCallback() {
            @Override public void onDelete() { onDeleted.run(); }
            @Override public void onError(String error) { onError.onError(error); }
        });
    }

    private void deleteOfflineRegions() {
        offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
            @Override public void onList(OfflineRegion[] regions) {
                if (regions == null || regions.length == 0) {
                    runOnUiThread(() -> status.setText("There is no offline area to delete."));
                    return;
                }
                final int[] remaining = {regions.length};
                for (OfflineRegion region : regions) {
                    deleteRegion(region, () -> {
                        if (--remaining[0] == 0) runOnUiThread(() -> status.setText(
                            "All offline areas deleted. The online map is unchanged."));
                    }, error -> runOnUiThread(() -> status.setText("Could not delete offline area: " + error)));
                }
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> status.setText("Could not delete offline area: " + error));
            }
        });
    }

    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { mapView.onPause(); super.onPause(); }
    @Override protected void onStop() { mapView.onStop(); super.onStop(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
    @Override protected void onDestroy() { if (activeRegion != null) activeRegion.setObserver(null); mapView.onDestroy(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
