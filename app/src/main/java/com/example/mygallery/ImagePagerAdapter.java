package com.example.mygallery;

import android.content.Context;
import android.net.Uri;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.chrisbanes.photoview.PhotoView;

import java.util.List;

public class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ViewHolder> {

    private final List<Uri> imageUris;
    private final Context context;

    public ImagePagerAdapter(Context context, List<Uri> imageUris) {
        this.context = context;
        this.imageUris = imageUris;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_fullscreen_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri uri = imageUris.get(position);
        holder.photoView.setImageURI(uri);
        holder.photoView.setMaximumScale(1000f);

        // ✅ 더블탭으로 '확대' 막고, '축소'만 허용
        holder.photoView.setOnDoubleTapListener(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (holder.photoView.getScale() > holder.photoView.getMinimumScale()) {
                    holder.photoView.setScale(holder.photoView.getMinimumScale(), e.getX(), e.getY(), true);
                }
                return true;
            }
        });

        holder.photoView.setOnScaleChangeListener((scaleFactor, focusX, focusY) -> {
            if (context instanceof FullscreenActivity) {
                FullscreenActivity activity = (FullscreenActivity) context;
                float scale = holder.photoView.getScale();
                float minScale = holder.photoView.getMinimumScale();

                // 확대된 상태면 ViewPager 스와이프 비활성화
                activity.getViewPager().setUserInputEnabled(scale <= minScale);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;

        ViewHolder(View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.fullscreenImageView);
        }
    }
}
