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
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.helper.UserHelper;
import com.planckmail.web.response.dropBox.DropBoxFile;
import com.planckmail.web.restClient.CustomOkHttpDownloader;
import com.planckmail.web.restClient.RestClientDropBoxClient;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Terry on 1/3/2016.
 */
public class BaseFileDropBoxAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context mContext;
    private final AccountInfo mAccountInfo;
    private List<DropBoxFile> mData;

    public BaseFileDropBoxAdapter(Context context, List<DropBoxFile> list, AccountInfo accountInfo) {
        mAccountInfo = accountInfo;
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
        DropBoxFile file = mData.get(position);
        if (file.is_dir) {
            //clear size if this is folder
            setDefaultImages(file, fileHolder);
            fileHolder.tvFileSize.setText("");
        } else {
            loadPreview(file, fileHolder);
            fileHolder.tvFileSize.setText(file.size);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault());
        SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        try {
            Date d = sdf.parse(file.modified);
            String formattedTime = output.format(d);
            String lastModify = mContext.getResources().getString(R.string.fileModifyBy, formattedTime);
            fileHolder.tvFileType.setText(lastModify);
        } catch (ParseException e) {
            Log.d(PlanckMailApplication.TAG, e.toString());
        }

        String[] split = file.getPath().split("/");

        fileHolder.tvAccountName.setText(split[split.length - 1]);
    }

    private void loadPreview(final DropBoxFile file, final FileHolder fileHolder) {
        Picasso picasso = new Picasso.Builder(mContext).downloader(new CustomOkHttpDownloader(mContext, mAccountInfo)).build();

        Target target = getTarget(file, fileHolder);

        String url = RestClientDropBoxClient.BASE_URL2 + "/thumbnails/auto/" + file.getPath();
        picasso.load(url).into(target);
    }

    @NonNull
    private Target getTarget(final DropBoxFile file, final FileHolder fileHolder) {
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

    private void setDefaultImages(final DropBoxFile file, final FileHolder fileHolder) {
        if (file.is_dir) {
            fileHolder.tvFileSize.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(mContext, R.drawable.ic_folder_grey), null, null);
        } else {
            Drawable drawable = UserHelper.getImageFile(mContext, file.mime_type);
            fileHolder.tvFileSize.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
        }
    }

    public void updateData(List<DropBoxFile> list) {
        mData = list;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    static class FileHolder extends RecyclerView.ViewHolder {
        private final TextView tvFileType;
        public TextView tvAccountName;
        public TextView tvLastModify;
        public TextView tvFileSize;

        public FileHolder(View itemView) {
            super(itemView);
            tvLastModify = (TextView) itemView.findViewById(R.id.tvModifyDate);
            tvAccountName = (TextView) itemView.findViewById(R.id.tvFileName);
            tvFileSize = (TextView) itemView.findViewById(R.id.tvFileSize);
            tvFileType = (TextView) itemView.findViewById(R.id.tvFileType);
        }
    }
}