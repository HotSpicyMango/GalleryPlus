package com.example.mygallery;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class InfoActivity extends AppCompatActivity {
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

        backButton.setOnClickListener(v -> finish());
        versionText.setText(getString(R.string.info_version_format, getVersionName()));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (AuthState.shouldRequireUnlock()) {
            startActivity(new Intent(this, LockActivity.class));
            finish();
        }
    }

    private String getVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return getString(R.string.unknown_version);
        }
    }
}
