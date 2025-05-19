package com.example.mygallery;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
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

    private static final int DELETE_REQUEST_CODE = 202;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        viewPager = findViewById(R.id.viewPager);
        deleteButton = findViewById(R.id.deleteButton);

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

        deleteButton.setOnClickListener(v -> {
            int position = viewPager.getCurrentItem();

            if (position < 0 || position >= imageUris.size()) return;

            Uri imageUri = imageUris.get(position);
            ContentResolver resolver = getContentResolver();

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
    }

    public ViewPager2 getViewPager() {
        return viewPager;
    }
}
