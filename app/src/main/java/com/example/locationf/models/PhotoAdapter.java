package com.example.locationf.models;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.locationf.R;

import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    private List<Uri> photoUris;
    private OnRemovePhotoListener listener;

    public interface OnRemovePhotoListener {
        void onRemovePhoto(int position);
    }

    public PhotoAdapter(List<Uri> photoUris, OnRemovePhotoListener listener) {
        this.photoUris = photoUris;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Uri photoUri = photoUris.get(position);

        Glide.with(holder.itemView.getContext())
                .load(photoUri)
                .into(holder.imageViewPhoto);

        holder.btnRemovePhoto.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemovePhoto(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return photoUris.size();
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewPhoto;
        Button btnRemovePhoto;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewPhoto = itemView.findViewById(R.id.imageViewPhoto);
            btnRemovePhoto = itemView.findViewById(R.id.btnRemovePhoto);
        }
    }
}