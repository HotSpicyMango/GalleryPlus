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
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private ArrayList<Object> imageList = new ArrayList<>();
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                int appearance = isDarkTheme() ? 0 : WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
                insetsController.setSystemBarsAppearance(appearance, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            if (isDarkTheme()) {
                decor.setSystemUiVisibility(0); // 기본 상태바 (아이콘 흰색)
            } else {
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // 아이콘 어둡게
            }
        }

        recyclerView = findViewById(R.id.recyclerView);
        SwitchCompat switchSort = findViewById(R.id.switchSort);
        TextView titleText = findViewById(R.id.titleText);


        layoutManager = new GridLayoutManager(this, spanCount);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (imageAdapter != null && imageAdapter.getItemViewType(position) == 0) {
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

        switchSort.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isDescending = isChecked;
            switchSort.setText(isChecked ? "최신순" : "오래된순");
            loadImages();
        });

        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scale = detector.getScaleFactor();

                if (scale > 1.01f && spanCount > MIN_SPAN) {
                    spanCount--;
                    layoutManager.setSpanCount(spanCount);
                    recyclerView.scheduleLayoutAnimation();
                    imageAdapter.notifyDataSetChanged();
                } else if (scale < 0.99f && spanCount < MAX_SPAN) {
                    spanCount++;
                    layoutManager.setSpanCount(spanCount);
                    recyclerView.scheduleLayoutAnimation();
                    imageAdapter.notifyDataSetChanged();
                }
                return true;
            }
        });

        recyclerView.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            return false;
        });

        // ✅ Android 13 이상 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }

        // ✅ 권한이 있으면 바로 이미지 로드
        loadImages();
    }

    private void loadImages() {
        imageList.clear();

        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED
        };
        String sortOrder = isDescending
                ? MediaStore.Images.Media.DATE_ADDED + " DESC"
                : MediaStore.Images.Media.DATE_ADDED + " ASC";

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
                int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    long dateSeconds = cursor.getLong(dateColumn);
                    long dateMillis = dateSeconds * 1000L;

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

                    grouped.putIfAbsent(date, new ArrayList<>());
                    grouped.get(date).add(uri);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "이미지를 불러오는 중 오류 발생: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        for (Map.Entry<Date, List<Uri>> entry : grouped.entrySet()) {
            String dateKey = sdf.format(entry.getKey());
            imageList.add(dateKey);
            imageList.addAll(entry.getValue());
        }

        imageAdapter.notifyDataSetChanged();
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
                Toast.makeText(this, "사진을 불러오기 위해 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean fromLock = getIntent().getBooleanExtra("from_lock", false);

        if (fromLock) {
            getIntent().removeExtra("from_lock");
            return; // 인증 직후는 통과
        }

        // 무조건 재인증 유도
        Intent intent = new Intent(this, LockActivity.class);
        intent.putExtra("reauth", true);
        startActivity(intent);
        finish();
    }

    private boolean isDarkTheme() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}