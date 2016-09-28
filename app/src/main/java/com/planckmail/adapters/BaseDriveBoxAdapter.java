package com.planckmail.adapters;

import android.app.Activity;
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
import com.planckmail.utils.MimeUtils;
import com.planckmail.web.response.boxDrive.BoxDriveFile;
import com.planckmail.web.response.dropBox.DropBoxFile;
import com.planckmail.web.restClient.CustomOkHttpDownloader;
import com.planckmail.web.restClient.RestClientBoxDriveClient;
import com.planckmail.web.restClient.RestClientDropBoxClient;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Terry on 1/27/2016.
 */
public class BaseDriveBoxAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String KB = "kb";
    public static final double SIZE_DIVIDER = 1024.0;
    private static final String MB = "mb";

    private final Context mContext;
    private final AccountInfo mAccountInfo;
    private List<BoxDriveFile> mData = new ArrayList<>();

    public BaseDriveBoxAdapter(Context context, List<BoxDriveFile> list,AccountInfo accountInfo) {
        mContext = context;
        mData = list;
        mAccountInfo=accountInfo;
    }

    @Override
    public FileHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.elem_file, parent, false);
        return new FileHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        FileHolder fileHolder = (FileHolder) holder;
        BoxDriveFile file = mData.get(position);

        if (file.type.equalsIgnoreCase(BOX_FILE_TYPE.FOLDER.toString())) {
            //clear size if this is folder
            setDefaultImages(file, fileHolder);
            fileHolder.tvFileSize.setText("");
        } else {
            Picasso picasso = new Picasso.Builder(mContext).downloader(new CustomOkHttpDownloader(mContext, mAccountInfo)).build();
            Target target = getTarget(file, fileHolder);

            String url = RestClientBoxDriveClient.BASE_URL1 + "/files/" + file.id+"/thumbnail.png?min_height=128&min_width=128";
            picasso.load(url).into(target);
            setFileSize(fileHolder, file);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        try {
            Date d = sdf.parse(file.modified_at);
            String formattedTime = output.format(d);
            String lastModify = mContext.getResources().getString(R.string.fileModifyBy, formattedTime);
            fileHolder.tvFileType.setText(lastModify);
        } catch (ParseException e) {
            Log.d(PlanckMailApplication.TAG, e.toString());
        }

        fileHolder.tvAccountName.setText(file.name);
    }

    @NonNull
    private Target getTarget(final BoxDriveFile file, final FileHolder fileHolder) {
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

    private void setDefaultImages(final BoxDriveFile file, final FileHolder fileHolder) {
        if (file.type.equalsIgnoreCase(BOX_FILE_TYPE.FOLDER.toString())) {
            fileHolder.tvFileSize.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(mContext, R.drawable.ic_folder_grey), null, null);
        } else {
            String mimeType = MimeUtils.guessMimeTypeFromExtension(file.name);

            Drawable drawable = UserHelper.getImageFile(mContext, mimeType);
            fileHolder.tvFileSize.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
        }
    }

    private void setFileSize(FileHolder holder, BoxDriveFile file) {
        int size = (int) (file.size / SIZE_DIVIDER);

        if (size >= SIZE_DIVIDER) {
            size = (int) ((file.size / SIZE_DIVIDER) / SIZE_DIVIDER);
            holder.tvFileSize.setText(String.valueOf("(" + size + " " + MB + ")"));
        } else
            holder.tvFileSize.setText(String.valueOf("(" + size + " " + KB + ")"));
    }

    public void updateData(List<BoxDriveFile> list) {
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

    public enum BOX_FILE_TYPE {
        FOLDER, FILE
    }
}
