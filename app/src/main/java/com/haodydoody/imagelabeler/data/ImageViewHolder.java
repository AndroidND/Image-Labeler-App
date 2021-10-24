package com.haodydoody.imagelabeler.data;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.haodydoody.imagelabeler.R;

public class ImageViewHolder extends RecyclerView.ViewHolder {
    public ImageView mImageView;
    public TextView mTextViewImageName;
    public TextView mTextViewImageType;

    public ImageViewHolder(@NonNull View itemView) {
        super(itemView);
        mImageView = itemView.findViewById(R.id.entry_image);
        mTextViewImageName = itemView.findViewById(R.id.file_name);
        mTextViewImageType = itemView.findViewById(R.id.mime_type);

    }
}
