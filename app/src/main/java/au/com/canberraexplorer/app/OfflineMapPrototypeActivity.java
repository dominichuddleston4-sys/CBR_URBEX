package au.com.canberraexplorer.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.maplibre.android.MapLibre;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;

/**
 * Isolated native-map renderer spike.
 * Not wired into the signed-in production flow until device and offline-package tests pass.
 */
public class OfflineMapPrototypeActivity extends Activity {
    private static final String TEST_STYLE =
        "https://demotiles.maplibre.org/style.json";
    private MapView mapView;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(10, 24, 17));
        getWindow().setNavigationBarColor(Color.BLACK);
        MapLibre.getInstance(getApplicationContext());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(10, 24, 17));

        TextView heading = new TextView(this);
        heading.setText("ACT offline map prototype");
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(18);
        heading.setPadding(16, 14, 16, 8);
        root.addView(heading, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        status = new TextView(this);
        status.setText("Loading native map test style… Internet required for this renderer check.");
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(12);
        status.setPadding(16, 4, 16, 10);
        root.addView(status, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mapView = new MapView(this);
        root.addView(mapView, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(map -> {
            map.setCameraPosition(new CameraPosition.Builder()
                .target(new LatLng(-35.28, 149.13))
                .zoom(9.5)
                .build());
            map.setStyle(new Style.Builder().fromUri(TEST_STYLE), style ->
                status.setText("Native map renderer loaded. This is an online test only; offline ACT data is not connected yet."));
        });
    }

    @Override protected void onStart() { super.onStart(); if (mapView != null) mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override protected void onPause() { if (mapView != null) mapView.onPause(); super.onPause(); }
    @Override protected void onStop() { if (mapView != null) mapView.onStop(); super.onStop(); }
    @Override protected void onDestroy() { if (mapView != null) mapView.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }
    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }
}
