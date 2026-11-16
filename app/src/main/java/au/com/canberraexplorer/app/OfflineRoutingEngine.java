package au.com.canberraexplorer.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import com.valhalla.valhalla.Valhalla;

import java.io.File;

/**
 * Offline route calculation backed by the on-device Valhalla engine.
 *
 * The routing data pack is separate from MapLibre's visual map downloads.
 * It must be installed under files/valhalla and include a valid valhalla.json
 * whose tile_extract points to the downloaded Canberra Valhalla tile archive.
 */
public final class OfflineRoutingEngine {
    private static final String ROUTING_FOLDER = "valhalla";
    private static final String CONFIG_FILE = "valhalla.json";

    private OfflineRoutingEngine() { }

    /**
     * Calculates a route using only the routing tiles already stored on this device.
     * mode must be "driving" or "walking".
     *
     * Returns a JSON object containing the encoded route shape, distance, duration,
     * and written manoeuvre instructions. No audio or network service is used.
     */
    public static JSONObject calculateRoute(Context context,
                                             double originLatitude,
                                             double originLongitude,
                                             double destinationLatitude,
                                             double destinationLongitude,
                                             String mode) throws Exception {
        validateCoordinate(originLatitude, originLongitude, "starting point");
        validateCoordinate(destinationLatitude, destinationLongitude, "destination");

        boolean walking = "walking".equalsIgnoreCase(mode);
        if (!walking && !"driving".equalsIgnoreCase(mode)) {
            throw new IllegalArgumentException("Choose driving or walking.");
        }

        File routingDirectory = new File(context.getFilesDir(), ROUTING_FOLDER);
        File config = new File(routingDirectory, CONFIG_FILE);
        if (!config.isFile()) {
            throw new IllegalStateException(
                "Canberra offline routing data has not been downloaded yet. Connect to the internet and download the routing pack first."
            );
        }

        JSONObject request = new JSONObject();
        JSONArray locations = new JSONArray();
        locations.put(new JSONObject()
            .put("lat", originLatitude)
            .put("lon", originLongitude));
        locations.put(new JSONObject()
            .put("lat", destinationLatitude)
            .put("lon", destinationLongitude));
        request.put("locations", locations);
        request.put("costing", walking ? "pedestrian" : "auto");
        request.put("units", "kilometers");
        request.put("directions_options", new JSONObject()
            .put("units", "kilometers")
            .put("language", "en-US"));

        try (Valhalla engine = new Valhalla(config.getAbsolutePath())) {
            JSONObject response = new JSONObject(engine.routeRaw(request.toString()));
            JSONObject trip = response.getJSONObject("trip");
            JSONArray legs = trip.getJSONArray("legs");
            if (legs.length() == 0) {
                throw new IllegalStateException("No route was found in the downloaded Canberra routing area.");
            }

            JSONObject leg = legs.getJSONObject(0);
            JSONObject result = new JSONObject();
            result.put("mode", walking ? "walking" : "driving");
            result.put("shape", leg.optString("shape", ""));
            result.put("summary", leg.optJSONObject("summary") == null
                ? new JSONObject() : leg.optJSONObject("summary"));

            JSONArray sourceManeuvers = leg.optJSONArray("maneuvers");
            JSONArray instructions = new JSONArray();
            if (sourceManeuvers != null) {
                for (int i = 0; i < sourceManeuvers.length(); i++) {
                    JSONObject maneuver = sourceManeuvers.getJSONObject(i);
                    JSONObject instruction = new JSONObject();
                    instruction.put("text", maneuver.optString("instruction", "Continue"));
                    instruction.put("distanceKm", maneuver.optDouble("length", 0.0));
                    instruction.put("durationSeconds", maneuver.optDouble("time", 0.0));
                    instruction.put("type", maneuver.optInt("type", 0));
                    instructions.put(instruction);
                }
            }
            result.put("instructions", instructions);
            return result;
        }
    }

    private static void validateCoordinate(double latitude, double longitude, String label) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
            || latitude < -90 || latitude > 90
            || longitude < -180 || longitude > 180
            || (latitude == 0 && longitude == 0)) {
            throw new IllegalArgumentException("The " + label + " coordinates are invalid.");
        }
    }
}
