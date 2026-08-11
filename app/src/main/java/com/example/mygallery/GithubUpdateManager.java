package com.example.mygallery;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GithubUpdateManager {
    private static final String OWNER = "HotSpicyMango";
    private static final String REPO = "GalleryPlus";
    private static final String LATEST_RELEASE_API =
            "https://api.github.com/repos/" + OWNER + "/" + REPO + "/releases/latest";

    private static final String PREFS_NAME = "github_update_prefs";
    private static final String KEY_LAST_AUTO_CHECK_TIME = "last_auto_check_time";
    static final String KEY_ACTIVE_DOWNLOAD_ID = "active_download_id";
    static final String KEY_ACTIVE_DOWNLOAD_FILE = "active_download_file";
    private static final long AUTO_CHECK_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L;

    private final Activity activity;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SharedPreferences prefs;

    public GithubUpdateManager(Activity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void checkForUpdatesIfNeeded() {
        long now = System.currentTimeMillis();
        long lastCheck = prefs.getLong(KEY_LAST_AUTO_CHECK_TIME, 0L);
        if (now - lastCheck < AUTO_CHECK_INTERVAL_MILLIS) {
            return;
        }

        prefs.edit().putLong(KEY_LAST_AUTO_CHECK_TIME, now).apply();
        checkForUpdates(false);
    }

    public void checkForUpdatesManually() {
        checkForUpdates(true);
    }

    public void release() {
        executor.shutdownNow();
    }

    private void checkForUpdates(boolean manual) {
        executor.execute(() -> {
            try {
                ReleaseInfo releaseInfo = fetchLatestRelease();
                if (releaseInfo == null || releaseInfo.apkDownloadUrl == null) {
                    if (manual) {
                        showToast("다운로드 가능한 APK 릴리스가 없습니다.");
                    }
                    return;
                }

                String currentVersion = normalizeVersion(BuildConfig.VERSION_NAME);
                String latestVersion = normalizeVersion(releaseInfo.tagName);
                if (!isNewerVersion(latestVersion, currentVersion)) {
                    if (manual) {
                        showToast("현재 최신 버전을 사용 중입니다.");
                    }
                    return;
                }

                activity.runOnUiThread(() -> showUpdateDialog(releaseInfo));
            } catch (Exception e) {
                if (manual) {
                    showToast("업데이트 확인 실패: " + e.getMessage());
                }
            }
        });
    }

    private ReleaseInfo fetchLatestRelease() throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(LATEST_RELEASE_API);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", activity.getPackageName());

            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String body = readFully(stream);

            if (code < 200 || code >= 300) {
                throw new IllegalStateException("GitHub API 응답 오류(" + code + ")");
            }

            JSONObject json = new JSONObject(body);
            String tagName = json.optString("tag_name", "");
            String releaseName = json.optString("name", tagName);
            JSONArray assets = json.optJSONArray("assets");
            String apkUrl = null;
            String apkName = null;

            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    String name = asset.optString("name", "");
                    String downloadUrl = asset.optString("browser_download_url", "");
                    if (name.toLowerCase(Locale.US).endsWith(".apk")
                            && downloadUrl.startsWith("https://")) {
                        apkUrl = downloadUrl;
                        apkName = name;
                        break;
                    }
                }
            }

            if (tagName.isEmpty()) {
                return null;
            }
            return new ReleaseInfo(tagName, releaseName, apkUrl, apkName);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readFully(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private void showUpdateDialog(ReleaseInfo releaseInfo) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle("새 버전 사용 가능")
                .setMessage("현재 버전: " + BuildConfig.VERSION_NAME
                        + "\n최신 버전: " + releaseInfo.tagName
                        + "\n\nAPK를 다운로드하고 설치 화면을 여시겠습니까?")
                .setPositiveButton("다운로드", (dialog, which) -> {
                    if (!canRequestPackageInstalls()) {
                        openUnknownAppInstallSettings();
                        return;
                    }
                    downloadApk(releaseInfo);
                })
                .setNegativeButton("나중에", null)
                .show();
    }

    private boolean canRequestPackageInstalls() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || activity.getPackageManager().canRequestPackageInstalls();
    }

    private void openUnknownAppInstallSettings() {
        Toast.makeText(activity, "이 앱의 APK 설치 권한을 허용한 뒤 다시 업데이트를 확인해주세요.", Toast.LENGTH_LONG).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        }
    }

    private void downloadApk(ReleaseInfo releaseInfo) {
        try {
            DownloadManager downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) {
                showToast("다운로드 서비스를 사용할 수 없습니다.");
                return;
            }

            String fileName = releaseInfo.apkName != null && !releaseInfo.apkName.isEmpty()
                    ? releaseInfo.apkName
                    : "GalleryPlus-" + releaseInfo.tagName + ".apk";
            File downloadFile = new File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
            if (downloadFile.exists() && !downloadFile.delete()) {
                showToast("기존 업데이트 파일을 삭제할 수 없습니다.");
                return;
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(releaseInfo.apkDownloadUrl))
                    .setTitle("Gallery+ 업데이트")
                    .setDescription(releaseInfo.tagName + " 다운로드 중")
                    .setMimeType("application/vnd.android.package-archive")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationUri(Uri.fromFile(downloadFile));

            long downloadId = downloadManager.enqueue(request);
            prefs.edit()
                    .putLong(KEY_ACTIVE_DOWNLOAD_ID, downloadId)
                    .putString(KEY_ACTIVE_DOWNLOAD_FILE, downloadFile.getAbsolutePath())
                    .apply();
            showToast("업데이트 APK 다운로드를 시작했습니다.");
        } catch (Exception e) {
            showToast("다운로드 시작 실패: " + e.getMessage());
        }
    }

    private boolean isNewerVersion(String latestVersion, String currentVersion) {
        String[] latestParts = latestVersion.split("\\.");
        String[] currentParts = currentVersion.split("\\.");
        int maxLength = Math.max(latestParts.length, currentParts.length);

        for (int i = 0; i < maxLength; i++) {
            int latest = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;
            int current = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;
            if (latest > current) {
                return true;
            }
            if (latest < current) {
                return false;
            }
        }
        return false;
    }

    private int parseVersionPart(String value) {
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String normalizeVersion(String version) {
        if (version == null) {
            return "0";
        }

        String normalized = version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private void showToast(String message) {
        activity.runOnUiThread(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static class ReleaseInfo {
        final String tagName;
        final String releaseName;
        final String apkDownloadUrl;
        final String apkName;

        ReleaseInfo(String tagName, String releaseName, String apkDownloadUrl, String apkName) {
            this.tagName = tagName;
            this.releaseName = releaseName;
            this.apkDownloadUrl = apkDownloadUrl;
            this.apkName = apkName;
        }
    }
}
