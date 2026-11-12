package au.com.canberraexplorer.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.webkit.WebResourceRequest;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.view.View;
import android.graphics.Color;
import android.widget.Toast;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import android.view.ViewGroup;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private static final int LOCATION_REQUEST = 42;
    private static final int GOOGLE_SIGN_IN_REQUEST = 44;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private static final int INSTALL_PERMISSION_REQUEST = 43;
    private static final String UPDATE_INFO_URL = "https://dominichuddleston4-sys.github.io/CBR_URBEX/version.json";
    private static final String PREFS = "canberra_explorer_updates";
    private static final String PENDING_DOWNLOAD = "pending_download_id";
    private WebView webView;
    private GeolocationPermissions.Callback geoCallback;
    private String geoOrigin;
    private AlertDialog updateDialog;
    private volatile boolean updateCheckRunning = false;
    private DownloadManager downloadManager;
    private BroadcastReceiver downloadReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(10, 24, 17));
        getWindow().setNavigationBarColor(Color.BLACK);
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        registerDownloadReceiver();

        firebaseAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build();
        googleSignInClient = GoogleSignIn.getClient(this, signInOptions);

        if (firebaseAuth.getCurrentUser() == null) showSignInScreen();
        else loadMap();
    }

    private void showSignInScreen() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(36, 36, 36, 36);
        layout.setBackgroundColor(Color.rgb(10, 24, 17));

        TextView title = new TextView(this);
        title.setText("Canberra Explorer");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        layout.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView message = new TextView(this);
        message.setText("Sign in with Google to access the map.");
        message.setTextColor(Color.LTGRAY);
        message.setTextSize(16);
        message.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        messageParams.topMargin = 24;
        layout.addView(message, messageParams);

        Button signIn = new Button(this);
        signIn.setText("Sign in with Google");
        signIn.setOnClickListener(v -> startGoogleSignIn());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = 28;
        layout.addView(signIn, buttonParams);
        setContentView(layout);
    }

    private void startGoogleSignIn() {
        startActivityForResult(googleSignInClient.getSignInIntent(), GOOGLE_SIGN_IN_REQUEST);
    }

    private void loadMap() {
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(10, 24, 17));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setSupportMultipleWindows(false);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(true);

        webView.setWebViewClient(new WebViewClient() {
            private boolean openGoogleMaps(String url) {
                Uri uri = Uri.parse(url);
                String host = uri.getHost();
                if (host == null || !(host.equals("www.google.com") || host.equals("google.com") || host.equals("maps.google.com") || host.equals("maps.app.goo.gl"))) return false;
                if (!uri.getPath().startsWith("/maps") && !host.equals("maps.app.goo.gl")) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) { }
                return true;
            }
            private boolean handleNavigation(String url) {
                if (openGoogleMaps(url)) return true;
                Uri uri = Uri.parse(url);
                String scheme = uri.getScheme();
                String host = uri.getHost();
                if ("https".equalsIgnoreCase(scheme)
                    && "dominichuddleston4-sys.github.io".equalsIgnoreCase(host)
                    && uri.getPath() != null
                    && uri.getPath().startsWith("/CBR_URBEX")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                catch (Exception ignored) { }
                return true;
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleNavigation(request.getUrl().toString());
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleNavigation(url);
            }
        });
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface public void openOfflineMap(String placesGeoJson) {
                Intent intent = new Intent(MainActivity.this, OfflineMapActivity.class);
                if (placesGeoJson != null && placesGeoJson.length() <= 2000000) intent.putExtra("places_geojson", placesGeoJson);
                runOnUiThread(() -> startActivity(intent));
            }
            @JavascriptInterface public boolean isAdminAccount() {
                com.google.firebase.auth.FirebaseUser user = firebaseAuth == null ? null : firebaseAuth.getCurrentUser();
                return user != null && user.isEmailVerified()
                    && "Dominichuddleston299@gmail.com".equalsIgnoreCase(user.getEmail());
            }
        }, "CanberraNative");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false);
                } else {
                    geoOrigin = origin;
                    geoCallback = callback;
                    requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION }, LOCATION_REQUEST);
                }
            }
        });
        setContentView(webView);
        webView.loadUrl("https://dominichuddleston4-sys.github.io/CBR_URBEX/");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_SIGN_IN_REQUEST) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, authTask -> {
                    if (authTask.isSuccessful()) { loadMap(); checkForApkUpdate(); }
                    else {
                        Toast.makeText(this, "Google sign-in failed. Please try again.", Toast.LENGTH_LONG).show();
                        showSignInScreen();
                    }
                });
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign-in was cancelled or failed. Please try again.", Toast.LENGTH_LONG).show();
                showSignInScreen();
            }
            return;
        }
        if (requestCode == INSTALL_PERMISSION_REQUEST && Build.VERSION.SDK_INT >= 26 && getPackageManager().canRequestPackageInstalls()) {
            long pending = getPreferences(MODE_PRIVATE).getLong(PENDING_DOWNLOAD, -1);
            if (pending >= 0) {
                getPreferences(MODE_PRIVATE).edit().remove(PENDING_DOWNLOAD).apply();
                installDownloadedApk(pending);
            }
        }
    }

    private void registerDownloadReceiver() {
        downloadReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                long pending = getPreferences(MODE_PRIVATE).getLong(PENDING_DOWNLOAD, -2);
                if (id != pending) return;
                getPreferences(MODE_PRIVATE).edit().remove(PENDING_DOWNLOAD).apply();
                installDownloadedApk(id);
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(downloadReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firebaseAuth != null && firebaseAuth.getCurrentUser() != null) checkForApkUpdate();
        long pending = getPreferences(MODE_PRIVATE).getLong(PENDING_DOWNLOAD, -1);
        if (pending >= 0) {
            DownloadManager.Query query = new DownloadManager.Query().setFilterById(pending);
            try (android.database.Cursor cursor = downloadManager.query(query)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        getPreferences(MODE_PRIVATE).edit().remove(PENDING_DOWNLOAD).apply();
                        installDownloadedApk(pending);
                    }
                }
            } catch (Exception ignored) { }
        }
    }

    private void checkForApkUpdate() {
        if (updateCheckRunning) return;
        updateCheckRunning = true;
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(UPDATE_INFO_URL).openConnection();
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(7000);
                connection.setRequestProperty("Cache-Control", "no-cache");
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return;
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
                reader.close();
                JSONObject info = new JSONObject(body.toString());
                long latestCode = info.optLong("versionCode", 0);
                String latestName = info.optString("versionName", "new version");
                String apkUrl = info.optString("apkUrl", "");
                PackageInfo installed = getPackageManager().getPackageInfo(getPackageName(), 0);
                long installedCode = Build.VERSION.SDK_INT >= 28 ? installed.getLongVersionCode() : installed.versionCode;
                runOnUiThread(() -> {
                    if (latestCode <= installedCode) {
                        if (updateDialog != null && updateDialog.isShowing()) updateDialog.dismiss();
                        updateDialog = null;
                        return;
                    }
                    if (!apkUrl.startsWith("https://") || isFinishing() || isDestroyed()) return;
                    if (updateDialog != null && updateDialog.isShowing()) return;
                    updateDialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Canberra Explorer update")
                        .setMessage("Version " + latestName + " is available. Download it now? The app will ask Android to install it when the download finishes.")
                        .setPositiveButton("Update now", (dialog, which) -> downloadUpdate(apkUrl, latestName))
                        .setNegativeButton("Later", null)
                        .create();
                    updateDialog.show();
                });
            } catch (Exception ignored) {
                // Keep the app usable if the update service is unavailable.
            } finally {
                if (connection != null) connection.disconnect();
                updateCheckRunning = false;
            }
        }).start();
    }

    private void downloadUpdate(String apkUrl, String versionName) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
            request.setTitle("Canberra Explorer " + versionName);
            request.setDescription("Downloading app update");
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Canberra-Explorer-update.apk");
            long id = downloadManager.enqueue(request);
            getPreferences(MODE_PRIVATE).edit().putLong(PENDING_DOWNLOAD, id).apply();
            Toast.makeText(this, "Update downloading. Android will ask you to install it when ready.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not start the update download.", Toast.LENGTH_LONG).show();
        }
    }

    private void installDownloadedApk(long downloadId) {
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            Intent permission = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
            startActivityForResult(permission, INSTALL_PERMISSION_REQUEST);
            getPreferences(MODE_PRIVATE).edit().putLong(PENDING_DOWNLOAD, downloadId).apply();
            return;
        }
        Uri apkUri = downloadManager.getUriForDownloadedFile(downloadId);
        if (apkUri == null) {
            Toast.makeText(this, "The update download could not be opened. Please try again.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(apkUri, "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(install);
        } catch (Exception e) {
            Toast.makeText(this, "Android could not open the installer.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (downloadReceiver != null) {
            try { unregisterReceiver(downloadReceiver); } catch (Exception ignored) { }
        }
        if (webView != null) webView.destroy();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST && geoCallback != null) {
            boolean granted = false;
            for (int result : grantResults) if (result == PackageManager.PERMISSION_GRANTED) { granted = true; break; }
            geoCallback.invoke(geoOrigin, granted, false);
            geoCallback = null;
            geoOrigin = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
