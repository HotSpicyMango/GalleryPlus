package com.example.mygallery;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

public class GithubUpdateDownloadReceiver extends BroadcastReceiver {
    private static final String PREFS_NAME = "github_update_prefs";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long expectedDownloadId = prefs.getLong(GithubUpdateManager.KEY_ACTIVE_DOWNLOAD_ID, -1L);
        long completedDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
        if (expectedDownloadId == -1L || completedDownloadId != expectedDownloadId) {
            return;
        }

        String filePath = prefs.getString(GithubUpdateManager.KEY_ACTIVE_DOWNLOAD_FILE, null);
        prefs.edit()
                .remove(GithubUpdateManager.KEY_ACTIVE_DOWNLOAD_ID)
                .remove(GithubUpdateManager.KEY_ACTIVE_DOWNLOAD_FILE)
                .apply();

        if (!isSuccessfulDownload(context, completedDownloadId)) {
            Toast.makeText(context, "업데이트 APK 다운로드에 실패했습니다.", Toast.LENGTH_LONG).show();
            return;
        }

        if (filePath == null) {
            Toast.makeText(context, "다운로드된 APK 파일을 찾을 수 없습니다.", Toast.LENGTH_LONG).show();
            return;
        }

        File apkFile = new File(filePath);
        if (!apkFile.exists()) {
            Toast.makeText(context, "다운로드된 APK 파일을 찾을 수 없습니다.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!canRequestPackageInstalls(context)) {
            openUnknownAppInstallSettings(context);
            return;
        }

        Uri apkUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                apkFile
        );
        Intent installIntent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(apkUri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(installIntent);
    }

    private boolean isSuccessfulDownload(Context context, long downloadId) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) {
            return false;
        }

        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return false;
            }

            int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            return status == DownloadManager.STATUS_SUCCESSFUL;
        }
    }

    private boolean canRequestPackageInstalls(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || context.getPackageManager().canRequestPackageInstalls();
    }

    private void openUnknownAppInstallSettings(Context context) {
        Toast.makeText(context, "이 앱의 APK 설치 권한을 허용한 뒤 다시 업데이트를 확인해주세요.", Toast.LENGTH_LONG).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:" + context.getPackageName()))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(settingsIntent);
        }
    }
}
