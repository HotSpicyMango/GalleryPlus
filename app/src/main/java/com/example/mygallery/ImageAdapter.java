package com.example.mygallery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_IMAGE = 1;

    private final Context context;
    private final List<Object> items;

    public ImageAdapter(Context context, List<Object> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof String) ? TYPE_HEADER : TYPE_IMAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
            return new ImageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            String dateText = (String) items.get(position);
            ((HeaderViewHolder) holder).headerText.setText(dateText);
        } else {
            Uri imageUri = (Uri) items.get(position);
            ImageViewHolder imageHolder = (ImageViewHolder) holder;

            Glide.with(context)
                    .load(imageUri)
                    .centerCrop()
                    .into(imageHolder.imageView);

            float targetScale = spanCountToScale(MainActivity.spanCount); // MainActivity에서 spanCount를 참조
            imageHolder.itemView.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .setDuration(150)
                    .start();

            imageHolder.imageView.setOnTouchListener((v, event) -> {
                if (imageHolder.imageView instanceof com.github.chrisbanes.photoview.PhotoView) {
                    com.github.chrisbanes.photoview.PhotoView photoView = (com.github.chrisbanes.photoview.PhotoView) imageHolder.imageView;
                    if (photoView.getScale() > photoView.getMinimumScale()) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    } else {
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }
                return false;
            });

            imageHolder.itemView.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    ArrayList<Uri> uriList = new ArrayList<>();
                    for (Object obj : items) {
                        if (obj instanceof Uri) uriList.add((Uri) obj);
                    }
                    int imageIndex = uriList.indexOf(imageUri);

                    Intent intent = new Intent(context, FullscreenActivity.class);
                    intent.putParcelableArrayListExtra("image_uris", uriList);
                    intent.putExtra("start_position", imageIndex);

                    if (context instanceof Activity) {
                        context.startActivity(intent);
                    } else {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.getApplicationContext().startActivity(intent);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.headerText);
        }
    }

    private float spanCountToScale(int spanCount) {
        switch (spanCount) {
            case 2: return 1.3f;
            case 3: return 1.0f;
            case 4: return 0.85f;
            case 5: return 0.75f;
            case 6: return 0.65f;
            default: return 1.0f;
        }
    }
}
