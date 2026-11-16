package au.com.canberraexplorer.app;

import android.content.Context;

import com.squareup.moshi.Moshi;
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory;
import com.valhalla.valhalla.config.ValhallaConfigFactory;
import com.valhalla.valhalla.config.ValhallaConfigManager;
import com.valhalla.valhalla.files.ValhallaFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Downloads and installs the Canberra offline road + footpath routing pack. */
public final class OfflineRoutingPackManager {
    public static final String PACK_URL =
        "https://github.com/dominichuddleston4-sys/CBR_URBEX/releases/download/offline-routing-data/canberra-valhalla-tiles.tar";
    private static final long MAX_PACK_BYTES = 350L * 1024L * 1024L;
    private static final String ROUTING_DIR = "valhalla";
    private static final String TILE_ARCHIVE = "canberra-valhalla-tiles.tar";
    private static final String CONFIG_NAME = "valhalla.json";

    public interface Listener {
        void onProgress(long downloaded, long total);
        void onComplete();
        void onError(String message);
    }

    private OfflineRoutingPackManager() { }

    public static boolean isInstalled(Context context) {
        File dir = new File(context.getFilesDir(), ROUTING_DIR);
        return new File(dir, TILE_ARCHIVE).isFile()
            && new File(dir, CONFIG_NAME).isFile();
    }

    /** Call from a worker thread. Downloads to a temporary file and atomically installs it. */
    public static void downloadAndInstall(Context context, Listener listener) {
        HttpURLConnection connection = null;
        File directory = new File(context.getFilesDir(), ROUTING_DIR);
        File temporary = new File(directory, TILE_ARCHIVE + ".download");
        File archive = new File(directory, TILE_ARCHIVE);
        try {
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IllegalStateException("Could not create routing-data storage.");
            }
            URL url = new URL(PACK_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(60000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "CanberraExplorer-Android");
            connection.connect();
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("Routing pack download returned HTTP " + code
                    + ". The Canberra routing pack may not have been published yet.");
            }
            long total = connection.getContentLengthLong();
            if (total > MAX_PACK_BYTES) {
                throw new IllegalStateException("Routing pack is larger than the 350 MB safety limit.");
            }
            long downloaded = 0;
            byte[] buffer = new byte[64 * 1024];
            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream output = new FileOutputStream(temporary)) {
                int count;
                while ((count = input.read(buffer)) != -1) {
                    downloaded += count;
                    if (downloaded > MAX_PACK_BYTES) {
                        throw new IllegalStateException("Routing pack exceeded the 350 MB safety limit.");
                    }
                    output.write(buffer, 0, count);
                    listener.onProgress(downloaded, total);
                }
                output.getFD().sync();
            }
            if (downloaded == 0) throw new IllegalStateException("The routing pack was empty.");
            if (archive.exists() && !archive.delete()) {
                throw new IllegalStateException("Could not replace the previous routing pack.");
            }
            if (!temporary.renameTo(archive)) {
                throw new IllegalStateException("Could not install the downloaded routing pack.");
            }

            ValhallaFile tileFile = new ValhallaFile(context, TILE_ARCHIVE, directory);
            ValhallaFile configFile = new ValhallaFile(context, CONFIG_NAME, directory);
            Object config = ValhallaConfigFactory.INSTANCE.usingTileExtract(tileFile.absolutePath(), null);
            ValhallaConfigManager manager = new ValhallaConfigManager(
                context, configFile,
                new Moshi.Builder().add(new KotlinJsonAdapterFactory()).build());
            manager.writeConfig((com.valhalla.config.models.ValhallaConfig) config);
            listener.onComplete();
        } catch (Exception error) {
            temporary.delete();
            listener.onError(error.getMessage() == null ? "Could not install offline routing data." : error.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
