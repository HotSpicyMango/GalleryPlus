package com.example.mygallery;

import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FullscreenActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE_URI = "image_uri";
    public static final String EXTRA_SORT_DESCENDING = "sort_descending";

    private ArrayList<Uri> imageUris;
    private int startPosition;
    private ViewPager2 viewPager;
    private Button deleteButton;
    private TextView imageCounterText;
    private View topScrim;
    private View bottomScrim;
    private ImageButton backButton;
    private boolean controlsVisible = true;
    private boolean imageZoomed = false;
    private final ExecutorService imageLoadExecutor = Executors.newSingleThreadExecutor();

    private static final int DELETE_REQUEST_CODE = 202;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );
        getWindow().setDecorFitsSystemWindows(false);
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

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateImageCounter(position);
            }
        });

        backButton.setOnClickListener(v -> onBackPressed());
        loadImagesForViewer();

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

    private void loadImagesForViewer() {
        if (imageUris != null && !imageUris.isEmpty()) {
            setupViewPager(imageUris, startPosition);
            return;
        }

        String startUriText = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (startUriText == null) {
            showImageLoadFailure();
            return;
        }

        boolean sortDescending = getIntent().getBooleanExtra(EXTRA_SORT_DESCENDING, true);
        Uri startUri = Uri.parse(startUriText);

        imageLoadExecutor.execute(() -> {
            ArrayList<Uri> loadedUris = loadImageUrisFromMediaStore(sortDescending);
            int loadedStartPosition = loadedUris.indexOf(startUri);
            if (loadedStartPosition < 0) {
                loadedUris.add(0, startUri);
                loadedStartPosition = 0;
            }

            final int finalStartPosition = loadedStartPosition;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (loadedUris.isEmpty()) {
                    showImageLoadFailure();
                    return;
                }
                setupViewPager(loadedUris, finalStartPosition);
            });
        });
    }

    private ArrayList<Uri> loadImageUrisFromMediaStore(boolean sortDescending) {
        ArrayList<ImageEntry> entries = new ArrayList<>();
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_MODIFIED
        };

        try (android.database.Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null
        )) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int dateTakenColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                int dateAddedColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED);
                int dateModifiedColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    Uri uri = Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            String.valueOf(id)
                    );
                    long dateMillis = getBestImageDateMillis(cursor, dateTakenColumn, dateAddedColumn, dateModifiedColumn);
                    entries.add(new ImageEntry(uri, dateMillis));
                }
            }
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "이미지를 불러오는 중 오류 발생: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }

        Comparator<ImageEntry> comparator = Comparator.comparingLong(entry -> entry.dateMillis);
        if (sortDescending) {
            comparator = comparator.reversed();
        }
        Collections.sort(entries, comparator);

        ArrayList<Uri> uris = new ArrayList<>();
        for (ImageEntry entry : entries) {
            uris.add(entry.uri);
        }
        return uris;
    }

    private long getBestImageDateMillis(android.database.Cursor cursor,
                                        int dateTakenColumn,
                                        int dateAddedColumn,
                                        int dateModifiedColumn) {
        if (dateTakenColumn >= 0) {
            long dateTakenMillis = cursor.getLong(dateTakenColumn);
            if (dateTakenMillis > 0) {
                return dateTakenMillis;
            }
        }

        if (dateAddedColumn >= 0) {
            long dateAddedSeconds = cursor.getLong(dateAddedColumn);
            if (dateAddedSeconds > 0) {
                return dateAddedSeconds * 1000L;
            }
        }

        if (dateModifiedColumn >= 0) {
            long dateModifiedSeconds = cursor.getLong(dateModifiedColumn);
            if (dateModifiedSeconds > 0) {
                return dateModifiedSeconds * 1000L;
            }
        }

        return System.currentTimeMillis();
    }

    private void setupViewPager(ArrayList<Uri> uris, int position) {
        imageUris = uris;
        startPosition = Math.max(0, Math.min(position, imageUris.size() - 1));
        ImagePagerAdapter adapter = new ImagePagerAdapter(this, imageUris);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startPosition, false);
        updateImageCounter(startPosition);
    }

    private void showImageLoadFailure() {
        Toast.makeText(this, "이미지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
        finish();
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
    protected void onResume() {
        super.onResume();

        if (AuthState.shouldRequireUnlock()) {
            Intent intent = new Intent(this, LockActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    private void handleDeletion(int position, boolean success) {
        if (!success || position < 0 || position >= imageUris.size()) {
            Toast.makeText(this, "삭제 실패", Toast.LENGTH_SHORT).show();
            return;
        }

        imageUris.remove(position);

        setResult(RESULT_OK);

        if (imageUris.isEmpty()) {
            Toast.makeText(this, "모든 사진이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        int newPosition = Math.max(0, position - 1);
        viewPager.setCurrentItem(newPosition, false);
        RecyclerView.Adapter<?> adapter = viewPager.getAdapter();
        if (adapter != null) {
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position, imageUris.size());
        }
        updateImageCounter(newPosition);
    }

    public ViewPager2 getViewPager() {
        return viewPager;
    }

    public void toggleControls() {
        setControlsVisible(!controlsVisible);
    }

    public void setImageZoomed(boolean zoomed) {
        if (imageZoomed == zoomed) {
            return;
        }

        imageZoomed = zoomed;
        setControlsVisible(!zoomed);
    }

    private void setControlsVisible(boolean visible) {
        controlsVisible = visible;
        float targetAlpha = visible ? 1f : 0f;

        setSystemBarsVisible(visible);
        animateControl(topScrim, targetAlpha, visible);
        animateControl(bottomScrim, targetAlpha, visible);
        animateControl(backButton, targetAlpha, visible);
        animateControl(imageCounterText, targetAlpha, visible);
        animateControl(deleteButton, targetAlpha, visible);
    }

    private void setSystemBarsVisible(boolean visible) {
        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller == null) {
            return;
        }

        if (visible) {
            controller.show(WindowInsets.Type.systemBars());
        } else {
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            controller.hide(WindowInsets.Type.systemBars());
        }
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
        imageCounterText.setText(getString(R.string.image_counter_format, safePosition + 1, imageUris.size()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        imageLoadExecutor.shutdownNow();
    }

    private static class ImageEntry {
        private final Uri uri;
        private final long dateMillis;

        private ImageEntry(Uri uri, long dateMillis) {
            this.uri = uri;
            this.dateMillis = dateMillis;
        }
    }
}
