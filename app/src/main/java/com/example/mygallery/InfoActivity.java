package com.example.mygallery;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class InfoActivity extends AppCompatActivity {
    private GithubUpdateManager githubUpdateManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        ImageButton backButton = findViewById(R.id.infoBackButton);
        TextView versionText = findViewById(R.id.infoVersionText);
        MaterialButton checkUpdateButton = findViewById(R.id.checkUpdateButton);
        githubUpdateManager = new GithubUpdateManager(this);

        backButton.setOnClickListener(v -> finish());
        versionText.setText(getString(R.string.info_version_format, getVersionName()));
        checkUpdateButton.setOnClickListener(v -> githubUpdateManager.checkForUpdatesManually());
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (AuthState.shouldRequireUnlock()) {
            Intent intent = new Intent(this, LockActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    private String getVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return getString(R.string.unknown_version);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (githubUpdateManager != null) {
            githubUpdateManager.release();
        }
    }
}
