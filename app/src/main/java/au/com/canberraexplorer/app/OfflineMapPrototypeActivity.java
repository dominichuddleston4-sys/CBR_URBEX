package au.com.canberraexplorer.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;

/**
 * Isolated renderer smoke-test screen.
 *
 * IMPORTANT: this uses a remote demo style and therefore requires internet.
 * It is not the ACT offline map and must not be presented as offline-ready.
 */
public class OfflineMapPrototypeActivity extends Activity {
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MapLibre.getInstance(getApplicationContext());
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(10, 24, 17));

        mapView = new MapView(this);
        mapView.onCreate(savedInstanceState);
        root.addView(mapView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        TextView status = new TextView(this);
        status.setText("MAP ENGINE PROTOTYPE\nInternet required · ACT offline data not connected");
        status.setTextColor(Color.WHITE);
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        status.setPadding(18, 12, 18, 12);
        status.setBackgroundColor(Color.rgb(10, 24, 17));
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.CENTER_HORIZONTAL
        );
        statusParams.topMargin = 18;
        root.addView(status, statusParams);

        setContentView(root);

        mapView.getMapAsync(map -> {
            map.setStyle(new Style.Builder().fromUri("https://demotiles.maplibre.org/style.json"));
            map.setCameraPosition(new CameraPosition.Builder()
                .target(new LatLng(-35.28, 149.13))
                .zoom(10.0)
                .build());
        });
    }

    @Override protected void onStart() { super.onStart(); if (mapView != null) mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override protected void onPause() { if (mapView != null) mapView.onPause(); super.onPause(); }
    @Override protected void onStop() { if (mapView != null) mapView.onStop(); super.onStop(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }
    @Override protected void onDestroy() { if (mapView != null) mapView.onDestroy(); super.onDestroy(); }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mapView != null) mapView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }
}
