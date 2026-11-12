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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;
import org.json.JSONArray;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private static final int LOCATION_REQUEST = 42;
    private static final int GOOGLE_SIGN_IN_REQUEST = 44;
    private static final int PHOTO_PICKER_REQUEST = 45;
    private String pendingPhotoLocationId, pendingPhotoCaption, pendingPhotoCallback;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
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
        firestore = FirebaseFirestore.getInstance();
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build();
        googleSignInClient = GoogleSignIn.getClient(this, signInOptions);

        if (firebaseAuth.getCurrentUser() == null) showSignInScreen();
        else { loadMap(); registerCurrentUserAndCheckAccess(); }
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

            @JavascriptInterface public void openPhotoPicker(String locationId, String caption, String callbackId) {
                if (!validCallback(callbackId) || !validLocationId(locationId)) return;
                if (firebaseAuth == null || firebaseAuth.getCurrentUser() == null) {
                    sendPhotoCallback(callbackId, false, "Please sign in before uploading photos."); return;
                }
                pendingPhotoLocationId = locationId;
                pendingPhotoCaption = caption == null ? "" : caption.substring(0, Math.min(300, caption.length()));
                pendingPhotoCallback = callbackId;
                Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                picker.addCategory(Intent.CATEGORY_OPENABLE);
                picker.setType("image/*");
                picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                runOnUiThread(() -> startActivityForResult(picker, PHOTO_PICKER_REQUEST));
            }
            @JavascriptInterface public void loadLocationPhotos(String locationId, String callbackId) {
                if (!validCallback(callbackId) || !validLocationId(locationId) || firebaseAuth.getCurrentUser() == null) return;
                boolean admin = isAdminAccount();
                com.google.firebase.firestore.CollectionReference photos = firestore.collection("locations").document(locationId).collection("photos");
                com.google.firebase.firestore.Query query = admin
                    ? photos.orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(30)
                    : photos.whereEqualTo("status", "approved").limit(30);
                query.get().addOnSuccessListener(snapshot -> {
                    JSONArray items = new JSONArray();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        try {
                            JSONObject item = new JSONObject();
                            item.put("id", doc.getId());
                            item.put("uploaderName", doc.getString("uploaderName") == null ? "Explorer" : doc.getString("uploaderName"));
                            item.put("caption", doc.getString("caption") == null ? "" : doc.getString("caption"));
                            item.put("status", doc.getString("status") == null ? "" : doc.getString("status"));
                            item.put("imageBase64", doc.getString("imageBase64") == null ? "" : doc.getString("imageBase64"));
                            item.put("own", firebaseAuth.getCurrentUser().getUid().equals(doc.getString("uploaderUid")));
                            items.put(item);
                        } catch (Exception ignored) { }
                    }
                    try { JSONObject result = new JSONObject(); result.put("ok", true); result.put("photos", items); sendPhotoCallback(callbackId, result.toString()); }
                    catch (Exception e) { sendPhotoCallback(callbackId, false, "Could not prepare gallery."); }
                }).addOnFailureListener(error -> sendPhotoCallback(callbackId, false, "Could not load gallery: " + error.getMessage()));
            }
            @JavascriptInterface public void reportLocationPhoto(String locationId, String photoId, String reason, String callbackId) {
                if (!validCallback(callbackId) || !validLocationId(locationId) || photoId == null || !photoId.matches("[A-Za-z0-9-]{1,100}") || firebaseAuth.getCurrentUser() == null) return;
                Map<String,Object> report = new HashMap<>();
                report.put("photoId", photoId); report.put("reporterUid", firebaseAuth.getCurrentUser().getUid());
                report.put("reason", reason == null ? "Reported by user" : reason.substring(0, Math.min(500, reason.length())));
                report.put("createdAt", FieldValue.serverTimestamp()); report.put("status", "open");
                firestore.collection("locations").document(locationId).collection("photoReports").add(report)
                    .addOnSuccessListener(ref -> sendPhotoCallback(callbackId, true, "Report submitted for review."))
                    .addOnFailureListener(error -> sendPhotoCallback(callbackId, false, "Could not submit report: " + error.getMessage()));
            }
            @JavascriptInterface public void moderateLocationPhoto(String locationId, String photoId, boolean approve, String callbackId) {
                if (!isAdminAccount() || !validCallback(callbackId) || !validLocationId(locationId) || photoId == null || !photoId.matches("[A-Za-z0-9-]{1,100}")) return;
                Map<String,Object> change = new HashMap<>();
                change.put("status", approve ? "approved" : "removed");
                change.put("reviewedAt", FieldValue.serverTimestamp());
                change.put("reviewedBy", firebaseAuth.getCurrentUser().getUid());
                firestore.collection("locations").document(locationId).collection("photos").document(photoId).update(change)
                    .addOnSuccessListener(unused -> sendPhotoCallback(callbackId, true, approve ? "Photo approved." : "Photo removed from the gallery."))
                    .addOnFailureListener(error -> sendPhotoCallback(callbackId, false, "Moderation failed: " + error.getMessage()));
            }
            @JavascriptInterface public void signOut() {
                runOnUiThread(() -> {
                    if (firebaseAuth != null) firebaseAuth.signOut();
                    if (googleSignInClient != null) googleSignInClient.signOut();
                    showSignInScreen();
                    Toast.makeText(MainActivity.this, "You have been signed out.", Toast.LENGTH_SHORT).show();
                });
            }
            @JavascriptInterface public boolean isAdminAccount() {
                FirebaseUser user = firebaseAuth == null ? null : firebaseAuth.getCurrentUser();
                return user != null && user.isEmailVerified()
                    && "dominichuddleston299@gmail.com".equalsIgnoreCase(user.getEmail());
            }
            @JavascriptInterface public void loadAdminUsers(String callbackId) {
                if (!isAdminAccount() || !validCallback(callbackId)) return;
                FirebaseUser adminUser = firebaseAuth.getCurrentUser();
                if (adminUser == null) {
                    sendAdminError(callbackId, "Administrator session expired. Sign in again.");
                    return;
                }
                // Refresh the ID token so Firestore receives the latest verified-email claim.
                adminUser.getIdToken(true).addOnSuccessListener(tokenResult ->
                    firestore.collection("appUsers")
                        .orderBy("lastSeen", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(100).get().addOnSuccessListener(snapshot -> {
                            JSONArray users = new JSONArray();
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                try {
                                    JSONObject item = new JSONObject();
                                    item.put("uid", doc.getId());
                                    item.put("email", doc.getString("email") == null ? "" : doc.getString("email"));
                                    item.put("displayName", doc.getString("displayName") == null ? "" : doc.getString("displayName"));
                                    item.put("blocked", Boolean.TRUE.equals(doc.getBoolean("blocked")));
                                    item.put("forceSignOut", Boolean.TRUE.equals(doc.getBoolean("forceSignOut")));
                                    com.google.firebase.Timestamp seen = doc.getTimestamp("lastSeen");
                                    item.put("lastSeen", seen == null ? 0 : seen.toDate().getTime());
                                    users.put(item);
                                } catch (Exception ignored) { }
                            }
                            try {
                                JSONObject result = new JSONObject();
                                result.put("ok", true);
                                result.put("users", users);
                                sendAdminCallback(callbackId, result.toString());
                            } catch (Exception ignored) { sendAdminError(callbackId, "Could not format user list."); }
                        }).addOnFailureListener(error ->
                            sendAdminError(callbackId, "Firestore read failed (" + error.getMessage() + "). Publish firestore.rules in Firebase Console and confirm Firestore Database exists."))
                ).addOnFailureListener(error ->
                    sendAdminError(callbackId, "Could not refresh administrator sign-in (" + error.getMessage() + "). Sign out and back in."));

            }
            @JavascriptInterface public void setAdminUserFlag(String uid, String field, boolean value, String callbackId) {
                if (!isAdminAccount() || !validCallback(callbackId) || uid == null
                    || !uid.matches("[A-Za-z0-9_-]{1,128}")
                    || !("blocked".equals(field) || "forceSignOut".equals(field))) return;
                FirebaseUser adminUser = firebaseAuth.getCurrentUser();
                if (adminUser == null) {
                    sendAdminError(callbackId, "Administrator session expired. Sign in again.");
                    return;
                }
                Map<String,Object> change = new HashMap<>();
                change.put(field, value);
                adminUser.getIdToken(true).addOnSuccessListener(tokenResult ->
                    firestore.collection("appUsers").document(uid).update(change)
                        .addOnSuccessListener(unused -> sendAdminCallback(callbackId, "{\"ok\":true}"))
                        .addOnFailureListener(error -> sendAdminError(callbackId, "Firestore update failed (" + error.getMessage() + "). Check that firestore.rules is published."))
                ).addOnFailureListener(error ->
                    sendAdminError(callbackId, "Could not refresh administrator sign-in. Sign out and back in."));

            }

            private boolean validLocationId(String value) { return value != null && value.matches("[a-z0-9-]{1,80}"); }
            private void sendPhotoCallback(String callbackId, boolean ok, String message) {
                try { JSONObject result = new JSONObject(); result.put("ok", ok); result.put("message", message == null ? "" : message); sendPhotoCallback(callbackId, result.toString()); }
                catch (Exception ignored) { }
            }
            private void sendPhotoCallback(String callbackId, String json) {
                if (!validCallback(callbackId)) return;
                runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript("window['" + callbackId + "'](" + JSONObject.quote(json) + ")", null); });
            }
            private boolean validCallback(String value) {
                return value != null && value.matches("[A-Za-z0-9_]{1,50}");
            }
            private void sendAdminCallback(String callbackId, String json) {
                runOnUiThread(() -> {
                    if (webView != null) webView.evaluateJavascript(
                        "window['" + callbackId + "'](" + JSONObject.quote(json) + ")", null);
                });
            }
            private void sendAdminError(String callbackId, String message) {
                try {
                    JSONObject result = new JSONObject();
                    result.put("ok", false);
                    result.put("error", message);
                    sendAdminCallback(callbackId, result.toString());
                } catch (Exception ignored) { }
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

        if (requestCode == PHOTO_PICKER_REQUEST) {
            String callback = pendingPhotoCallback, locationId = pendingPhotoLocationId, caption = pendingPhotoCaption;
            pendingPhotoCallback = null; pendingPhotoLocationId = null; pendingPhotoCaption = null;
            if (resultCode != RESULT_OK || data == null || data.getData() == null) {
                if (callback != null) sendPhotoResult(callback, false, "Photo selection cancelled.");
                return;
            }
            if (callback != null && locationId != null) uploadLocationPhoto(data.getData(), locationId, caption, callback);
            return;
        }
        if (requestCode == GOOGLE_SIGN_IN_REQUEST) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, authTask -> {
                    if (authTask.isSuccessful()) { loadMap(); registerCurrentUserAndCheckAccess(); checkForApkUpdate(); }
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


    private void sendPhotoResult(String callbackId, boolean ok, String message) {
        try { JSONObject result = new JSONObject(); result.put("ok", ok); result.put("message", message == null ? "" : message);
            if (webView != null) webView.evaluateJavascript("window['" + callbackId + "'](" + JSONObject.quote(result.toString()) + ")", null);
        } catch (Exception ignored) { }
    }

    private void uploadLocationPhoto(Uri uri, String locationId, String caption, String callbackId) {
        new Thread(() -> {
            try {
                Bitmap bitmap = null;
                for (int sample = 1; sample <= 16 && bitmap == null; sample *= 2) {
                    BitmapFactory.Options options = new BitmapFactory.Options(); options.inSampleSize = sample;
                    try (InputStream input = getContentResolver().openInputStream(uri)) { bitmap = BitmapFactory.decodeStream(input, null, options); }
                }
                if (bitmap == null) throw new Exception("That image could not be opened.");
                float scale = Math.min(1f, 1280f / Math.max(bitmap.getWidth(), bitmap.getHeight()));
                if (scale < 1f) { Bitmap resized = Bitmap.createScaledBitmap(bitmap, Math.max(1, Math.round(bitmap.getWidth()*scale)), Math.max(1, Math.round(bitmap.getHeight()*scale)), true); if (resized != bitmap) bitmap.recycle(); bitmap = resized; }
                byte[] bytes = null;
                for (int quality = 82; quality >= 42; quality -= 10) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output); bytes = output.toByteArray();
                    if (bytes.length <= 500000) break;
                }
                bitmap.recycle();
                if (bytes == null || bytes.length > 500000) throw new Exception("Image is too large after compression. Try a smaller photo.");
                FirebaseUser user = firebaseAuth.getCurrentUser(); if (user == null) throw new Exception("Please sign in again.");
                String photoId = java.util.UUID.randomUUID().toString().replace("-", "");
                Map<String,Object> photo = new HashMap<>();
                photo.put("uploaderUid", user.getUid());
                String display = user.getDisplayName() == null || user.getDisplayName().trim().isEmpty() ? "Explorer" : user.getDisplayName();
                photo.put("uploaderName", display.substring(0, Math.min(80, display.length())));
                photo.put("imageBase64", Base64.encodeToString(bytes, Base64.NO_WRAP));
                photo.put("contentType", "image/jpeg"); photo.put("sizeBytes", bytes.length);
                photo.put("status", "pending"); photo.put("createdAt", FieldValue.serverTimestamp());
                photo.put("caption", caption == null ? "" : caption.substring(0, Math.min(300, caption.length())));
                firestore.collection("locations").document(locationId).collection("photos").document(photoId).set(photo)
                    .addOnSuccessListener(unused -> runOnUiThread(() -> sendPhotoResult(callbackId, true, "Photo uploaded for admin review. It will appear for everyone after approval.")))
                    .addOnFailureListener(error -> runOnUiThread(() -> sendPhotoResult(callbackId, false, "Photo upload failed: " + error.getMessage())));
            } catch (Exception error) { runOnUiThread(() -> sendPhotoResult(callbackId, false, error.getMessage() == null ? "Could not upload this photo." : error.getMessage())); }
        }).start();
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
        if (firebaseAuth != null && firebaseAuth.getCurrentUser() != null) { registerCurrentUserAndCheckAccess(); checkForApkUpdate(); }
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

    private void registerCurrentUserAndCheckAccess() {
        FirebaseUser user = firebaseAuth == null ? null : firebaseAuth.getCurrentUser();
        if (user == null || firestore == null) return;
        String uid = user.getUid();
        firestore.collection("appUsers").document(uid).get().addOnSuccessListener(doc -> {
            FirebaseUser current = firebaseAuth.getCurrentUser();
            if (current == null || !uid.equals(current.getUid())) return;
            boolean blocked = Boolean.TRUE.equals(doc.getBoolean("blocked"));
            boolean forceSignOut = Boolean.TRUE.equals(doc.getBoolean("forceSignOut"));
            if (blocked || forceSignOut) {
                if (forceSignOut && !blocked) {
                    firestore.collection("appUsers").document(uid)
                        .update("forceSignOut", false);
                }
                firebaseAuth.signOut();
                googleSignInClient.signOut();
                runOnUiThread(() -> {
                    Toast.makeText(this, blocked
                        ? "This account is blocked from Canberra Explorer."
                        : "Your Canberra Explorer session was signed out by the administrator.",
                        Toast.LENGTH_LONG).show();
                    showSignInScreen();
                });
                return;
            }
            Map<String,Object> profile = new HashMap<>();
            profile.put("lastSeen", FieldValue.serverTimestamp());
            profile.put("displayName", user.getDisplayName() == null ? "" : user.getDisplayName());
            if (!doc.exists()) {
                profile.put("uid", uid);
                profile.put("email", user.getEmail() == null ? "" : user.getEmail());
                profile.put("blocked", false);
                profile.put("forceSignOut", false);
                firestore.collection("appUsers").document(uid).set(profile);
            } else {
                firestore.collection("appUsers").document(uid).update(profile);
            }
        });
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
