package com.planckmail.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.helper.UserHelper;
import com.planckmail.web.response.googleDrive.GoogleDriveFile;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Terry on 1/21/2016.
 */
public class BaseGoogleDriveAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String KB = "kb";
    public static final double SIZE_DIVIDER = 1024.0;
    private static final String MB = "mb";
    private final Context mContext;
    private List<GoogleDriveFile> mData = new ArrayList<>();

    public BaseGoogleDriveAdapter(Context context, List<GoogleDriveFile> list) {
        mContext = context;
        mData = list;
    }

    @Override
    public FileHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.elem_file, parent, false);
        return new FileHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        FileHolder fileHolder = (FileHolder) holder;
        GoogleDriveFile file = mData.get(position);
        String folderMimeType = "application/vnd.google-apps.folder";

        if (file.mimeType.equals(folderMimeType)) {
            //clear size if this is folder
            setDefaultImages(file, fileHolder);
            fileHolder.tvFileSize.setText("");
        } else {
            Target target = getTarget(file, fileHolder);
            Picasso.with(mContext).load(file.thumbnailLink).into(target);
            setFileSize(fileHolder, file);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.ss'Z'", Locale.getDefault());
        SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        try {
            Date d = sdf.parse(file.modifiedTime);
            String formattedTime = output.format(d);
            String lastModify = mContext.getResources().getString(R.string.fileModifyBy, formattedTime);
            fileHolder.tvFileType.setText(lastModify);
        } catch (ParseException e) {
            Log.d(PlanckMailApplication.TAG, e.toString());
        }

        fileHolder.tvAccountName.setText(file.name);
    }

    @NonNull
    private Target getTarget(final GoogleDriveFile file, final FileHolder fileHolder) {
        return new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                Drawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);
                drawable = UserHelper.resize(mContext, drawable);
                fileHolder.tvFileSize.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                setDefaultImages(file, fileHolder);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };
    }

    private void setDefaultImages(final GoogleDriveFile file, final FileHolder fileHolder) {
        String folderMimeType = "application/vnd.google-apps.folder";

        if (file.mimeType.equals(folderMimeType)) {
            fileHolder.tvFileSize.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(mContext, R.drawable.ic_folder_grey), null, null);
        } else {
            Drawable drawable = UserHelper.getImageFile(mContext, file.mimeType);
            fileHolder.tvFileSize.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
        }
    }

    private void setFileSize(FileHolder holder, GoogleDriveFile file) {
        int size = (int) (file.size / SIZE_DIVIDER);

        if (size >= SIZE_DIVIDER) {
            size = (int) ((file.size / SIZE_DIVIDER) / SIZE_DIVIDER);
            holder.tvFileSize.setText(String.valueOf("(" + size + " " + MB + ")"));
        } else
            holder.tvFileSize.setText(String.valueOf("(" + size + " " + KB + ")"));
    }

    public void updateData(List<GoogleDriveFile> list) {
        mData = list;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    static class FileHolder extends RecyclerView.ViewHolder {
        private final TextView tvFileType;
        private final TextView tvLastModiby;
        public TextView tvAccountName;
        public TextView tvFileSize;

        public FileHolder(View itemView) {
            super(itemView);
            tvLastModiby = (TextView) itemView.findViewById(R.id.tvModifyDate);
            tvAccountName = (TextView) itemView.findViewById(R.id.tvFileName);
            tvFileSize = (TextView) itemView.findViewById(R.id.tvFileSize);
            tvFileType = (TextView) itemView.findViewById(R.id.tvFileType);
        }
    }
}
