package com.example.mygallery;

import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;

public class FullscreenActivity extends AppCompatActivity {

    private ArrayList<Uri> imageUris;
    private int startPosition;
    private ViewPager2 viewPager;
    private Button deleteButton;
    private TextView imageCounterText;
    private View topScrim;
    private View bottomScrim;
    private ImageButton backButton;
    private boolean controlsVisible = true;

    private static final int DELETE_REQUEST_CODE = 202;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.BLACK);

        viewPager = findViewById(R.id.viewPager);
        deleteButton = findViewById(R.id.deleteButton);
        backButton = findViewById(R.id.backButton);
        imageCounterText = findViewById(R.id.imageCounterText);
        topScrim = findViewById(R.id.topScrim);
        bottomScrim = findViewById(R.id.bottomScrim);

        imageUris = getIntent().getParcelableArrayListExtra("image_uris");
        startPosition = getIntent().getIntExtra("start_position", 0);

        if (imageUris == null || imageUris.isEmpty()) {
            Toast.makeText(this, "이미지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
            prefs.edit().putBoolean("from_fullscreen", true).apply();
            finish();
            return;
        }

        ImagePagerAdapter adapter = new ImagePagerAdapter(this, imageUris);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startPosition, false);
        updateImageCounter(startPosition);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateImageCounter(position);
            }
        });

        backButton.setOnClickListener(v -> onBackPressed());

        deleteButton.setOnClickListener(v -> {
            int position = viewPager.getCurrentItem();

            if (position < 0 || position >= imageUris.size()) return;

            Uri imageUri = imageUris.get(position);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    ArrayList<Uri> toDelete = new ArrayList<>();
                    toDelete.add(imageUri);

                    IntentSender sender = MediaStore.createDeleteRequest(getContentResolver(), toDelete).getIntentSender();

                    startIntentSenderForResult(sender, DELETE_REQUEST_CODE, null, 0, 0, 0);
                } catch (Exception e) {
                    Toast.makeText(this, "삭제 요청 실패", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DELETE_REQUEST_CODE && resultCode == RESULT_OK) {
            int position = viewPager.getCurrentItem();
            handleDeletion(position, true);
        }
    }

    @Override
    public void onBackPressed() {
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        prefs.edit().putBoolean("from_fullscreen", true).apply();

        super.onBackPressed();
    }

    private void handleDeletion(int position, boolean success) {
        if (!success || position < 0 || position >= imageUris.size()) {
            Toast.makeText(this, "삭제 실패", Toast.LENGTH_SHORT).show();
            return;
        }

        imageUris.remove(position);

        Intent resultIntent = new Intent();
        resultIntent.putParcelableArrayListExtra("updated_uris", imageUris);
        setResult(RESULT_OK, resultIntent);

        if (imageUris.isEmpty()) {
            Toast.makeText(this, "모든 사진이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
            SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
            prefs.edit().putBoolean("from_fullscreen", true).apply();
            finish();
            return;
        }

        int newPosition = Math.max(0, position - 1);
        viewPager.setCurrentItem(newPosition, false);
        viewPager.getAdapter().notifyItemRemoved(position);
        viewPager.getAdapter().notifyItemRangeChanged(position, imageUris.size());
        updateImageCounter(newPosition);
    }

    public ViewPager2 getViewPager() {
        return viewPager;
    }

    public void toggleControls() {
        setControlsVisible(!controlsVisible);
    }

    private void setControlsVisible(boolean visible) {
        controlsVisible = visible;
        float targetAlpha = visible ? 1f : 0f;

        animateControl(topScrim, targetAlpha, visible);
        animateControl(bottomScrim, targetAlpha, visible);
        animateControl(backButton, targetAlpha, visible);
        animateControl(imageCounterText, targetAlpha, visible);
        animateControl(deleteButton, targetAlpha, visible);
    }

    private void animateControl(View view, float targetAlpha, boolean visible) {
        if (view == null) {
            return;
        }

        if (visible) {
            view.setVisibility(View.VISIBLE);
        }

        view.animate()
                .alpha(targetAlpha)
                .setDuration(160)
                .withEndAction(() -> {
                    if (!visible) {
                        view.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    private void updateImageCounter(int position) {
        if (imageCounterText == null || imageUris == null || imageUris.isEmpty()) {
            return;
        }

        int safePosition = Math.max(0, Math.min(position, imageUris.size() - 1));
        imageCounterText.setText((safePosition + 1) + " / " + imageUris.size());
    }
}
