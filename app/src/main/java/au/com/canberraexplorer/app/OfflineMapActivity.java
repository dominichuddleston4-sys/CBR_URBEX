package au.com.canberraexplorer.app;

import android.app.Activity;
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

    private static final String[] REGIONS = {
        "Central Canberra", "North Canberra", "South Canberra", "West Canberra", "East Canberra"
    };
    private static final double[][] BOUNDS = {
        {-35.34, 149.08, -35.23, 149.20},
        {-35.25, 149.02, -35.13, 149.19},
        {-35.50, 149.02, -35.34, 149.20},
        {-35.36, 148.98, -35.22, 149.09},
        {-35.36, 149.18, -35.18, 149.30}
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
        intro.setText("Download Canberra areas while online, then reopen this map without internet. Save multiple areas, one download at a time. Each area is capped at 3,000 tiles and 50 MB.");
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
            String placesGeoJson = getIntent().getStringExtra("places_geojson");
            map.setStyle(new Style.Builder().fromUri(STYLE_URL), style -> addPlacePins(style, placesGeoJson));
            map.setCameraPosition(new CameraPosition.Builder()
                .target(new LatLng(-35.28, 149.13)).zoom(10.0).build());
        });
        refreshOfflineStatus();
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
    @Override protected void onDestroy() { mapView.onDestroy(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
