package com.example.mygallery;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private ClickableRecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private final ArrayList<Object> imageList = new ArrayList<>();
    private LinearLayout emptyStateLayout;
    private TextView photoCountText;
    private TextView emptyTitleText;
    private TextView emptyMessageText;
    private MaterialButton emptyActionButton;
    private boolean isDescending = true;

    private GridLayoutManager layoutManager;
    public static int spanCount = 3;
    private final int MIN_SPAN = 3;
    private final int MAX_SPAN = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // ✅ 레이아웃 연결

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        final WindowInsetsController insetsController = getWindow().getInsetsController();
        if (insetsController != null) {
            int appearance = isDarkTheme() ? 0 : WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
            insetsController.setSystemBarsAppearance(appearance, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }

        recyclerView = findViewById(R.id.recyclerView);
        MaterialButton sortButton = findViewById(R.id.sortButton);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        photoCountText = findViewById(R.id.photoCountText);
        emptyTitleText = findViewById(R.id.emptyTitleText);
        emptyMessageText = findViewById(R.id.emptyMessageText);
        emptyActionButton = findViewById(R.id.emptyActionButton);


        layoutManager = new GridLayoutManager(this, spanCount);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (imageAdapter != null && imageAdapter.getItemViewType(position) == ImageAdapter.TYPE_HEADER) {
                    return spanCount; // 날짜 헤더는 전체 열 차지
                } else {
                    return 1; // 이미지 썸네일은 1칸만 차지
                }
            }
        });
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());
        imageAdapter = new ImageAdapter(this, imageList);
        recyclerView.setAdapter(imageAdapter);

        sortButton.setOnClickListener(v -> {
            isDescending = !isDescending;
            sortButton.setText(isDescending ? R.string.sort_latest : R.string.sort_oldest);
            loadImages();
        });

        emptyActionButton.setOnClickListener(v ->
                ActivityCompat.requestPermissions(this, new String[]{getImagePermission()}, PERMISSION_REQUEST_CODE));

        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scale = detector.getScaleFactor();

                if (scale > 1.01f && spanCount > MIN_SPAN) {
                    spanCount--;
                    layoutManager.setSpanCount(spanCount);
                    recyclerView.scheduleLayoutAnimation();
                    notifyVisibleGridItemsChanged();
                } else if (scale < 0.99f && spanCount < MAX_SPAN) {
                    spanCount++;
                    layoutManager.setSpanCount(spanCount);
                    recyclerView.scheduleLayoutAnimation();
                    notifyVisibleGridItemsChanged();
                }
                return true;
            }
        });

        recyclerView.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return false;
        });

        // ✅ 권한 확인
        if (lacksImagePermission()) {
            showPermissionEmptyState();
            ActivityCompat.requestPermissions(this, new String[]{getImagePermission()}, PERMISSION_REQUEST_CODE);
        }
    }

    private void loadImages() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            ArrayList<Object> newItems = new ArrayList<>();
            String[] projection = new String[]{
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.DATE_MODIFIED
            };
            String sortOrder = isDescending
                    ? MediaStore.Images.Media.DATE_TAKEN + " DESC, " + MediaStore.Images.Media.DATE_ADDED + " DESC"
                    : MediaStore.Images.Media.DATE_TAKEN + " ASC, " + MediaStore.Images.Media.DATE_ADDED + " ASC";

            Map<Date, List<Uri>> grouped = isDescending
                    ? new TreeMap<>(Collections.reverseOrder())
                    : new TreeMap<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault());

            try (android.database.Cursor cursor = getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
            )) {
                if (cursor != null) {
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                    int dateTakenColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                    int dateAddedColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED);
                    int dateModifiedColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idColumn);
                        long dateMillis = getBestImageDateMillis(cursor, dateTakenColumn, dateAddedColumn, dateModifiedColumn);

                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(dateMillis);
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        Date date = cal.getTime();

                        Uri uri = Uri.withAppendedPath(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                String.valueOf(id)
                        );

                        grouped.computeIfAbsent(date, unused -> new ArrayList<>()).add(uri);
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "이미지를 불러오는 중 오류 발생: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            for (Map.Entry<Date, List<Uri>> entry : grouped.entrySet()) {
                List<Uri> uris = entry.getValue();
                String dateKey = sdf.format(entry.getKey());
                newItems.add(new HeaderItem(dateKey, uris.size()));
                newItems.addAll(uris);
            }

            int totalImageCount = 0;
            for (List<Uri> uris : grouped.values()) {
                totalImageCount += uris.size();
            }
            final int finalTotalImageCount = totalImageCount;

            runOnUiThread(() -> {
                replaceItems(newItems);
                updatePhotoCount(finalTotalImageCount);
                showGalleryState(!newItems.isEmpty());
            });
        });
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

    // ✅ 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadImages(); // 권한 승인 시 실행
            } else {
                showPermissionEmptyState();
                Toast.makeText(this, R.string.permission_required_toast, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (AuthState.shouldRequireUnlock()) {
            Intent intent = new Intent(this, LockActivity.class);
            startActivity(intent);
            return;
        }

        if (lacksImagePermission()) {
            showPermissionEmptyState();
            return;
        }

        loadImages();
    }

    private boolean isDarkTheme() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    private String getImagePermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private boolean lacksImagePermission() {
        return ContextCompat.checkSelfPermission(this, getImagePermission()) != PackageManager.PERMISSION_GRANTED;
    }

    private void showGalleryState(boolean hasImages) {
        recyclerView.setVisibility(hasImages ? View.VISIBLE : View.GONE);
        emptyStateLayout.setVisibility(hasImages ? View.GONE : View.VISIBLE);
        emptyActionButton.setVisibility(View.GONE);

        if (!hasImages) {
            emptyTitleText.setText(R.string.empty_no_photos_title);
            emptyMessageText.setText(R.string.empty_no_photos_message);
        }
    }

    private void showPermissionEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        emptyTitleText.setText(R.string.empty_permission_title);
        emptyMessageText.setText(R.string.empty_permission_message);
        emptyActionButton.setVisibility(View.VISIBLE);
        updatePhotoCount(0);
    }

    private void updatePhotoCount(int count) {
        photoCountText.setText(getString(R.string.photo_count_format, count));
    }

    private void notifyVisibleGridItemsChanged() {
        int itemCount = imageAdapter.getItemCount();
        if (itemCount > 0) {
            imageAdapter.notifyItemRangeChanged(0, itemCount);
        }
    }

    private void replaceItems(ArrayList<Object> newItems) {
        int oldSize = imageList.size();
        if (oldSize > 0) {
            imageList.clear();
            imageAdapter.notifyItemRangeRemoved(0, oldSize);
        }

        imageList.addAll(newItems);
        if (!newItems.isEmpty()) {
            imageAdapter.notifyItemRangeInserted(0, newItems.size());
        }
    }
}
