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
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.response.oneDrive.OneDriveFileValue;
import com.planckmail.web.response.oneDrive.OneDriveThumbnail;
import com.planckmail.web.restClient.RestClientOneDriveClient;
import com.planckmail.web.restClient.api.AutOneDriveApi;
import com.planckmail.web.restClient.service.OneDriveService;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Terry on 1/17/2016.
 */
public class BaseOneDriveAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String KB = "kb";
    private final Context mContext;
    private final AccountInfo mAccountInfo;
    private List<OneDriveFileValue> mData;
    public static final double SIZE_DIVIDER = 1024.0;
    private static final String MB = "mb";

    public BaseOneDriveAdapter(Context context, List<OneDriveFileValue> list, AccountInfo accountInfo) {
        mContext = context;
        mData = list;
        mAccountInfo = accountInfo;
    }

    @Override
    public FileHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.elem_file, parent, false);
        return new FileHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        FileHolder fileHolder = (FileHolder) holder;
        OneDriveFileValue file = mData.get(position);

        if (file.folder != null) {
            //clear size if this is folder
            setDefaultImages(file, fileHolder);
            fileHolder.tvFileSize.setText("");
        } else {
            setFileThumbnail(fileHolder, file);
            setFileSize(fileHolder, file);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.ss'Z'", Locale.getDefault());
        SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        try {
            Date d = sdf.parse(file.lastModifiedDateTime);
            String formattedTime = output.format(d);
            String lastModify = mContext.getResources().getString(R.string.fileModifyBy, formattedTime);
            fileHolder.tvFileType.setText(lastModify);
        } catch (ParseException e) {
            Log.d(PlanckMailApplication.TAG, e.toString());
        }

        fileHolder.tvAccountName.setText(file.name);
    }

    private void setFileThumbnail(final FileHolder fileHolder, final OneDriveFileValue file) {
        OneDriveService dropBoxServer = AutOneDriveApi.createService(OneDriveService.class, RestClientOneDriveClient.BASE_URL1, mAccountInfo.accessToken, "");
        dropBoxServer.getOneDriveThumbnail(file.id, new Callback<Object>() {

            @Override
            public void success(Object o, Response response) {
                String json = (String) o;
                OneDriveThumbnail thumbnail = JsonUtilFactory.getJsonUtil().fromJson(json, OneDriveThumbnail.class);
                Target target = getTarget(file, fileHolder);
                if (!thumbnail.value.isEmpty()) {
                    String url = thumbnail.value.get(0).medium.url + "/" + file.name;
                    Picasso.with(mContext).load(url).into(target);
                } else {
                    setDefaultImages(file, fileHolder);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                setDefaultImages(file, fileHolder);
            }
        });
    }

    private void setFileSize(FileHolder fileHolder, OneDriveFileValue file) {
        int size = (int) (file.size / SIZE_DIVIDER);

        if (size >= SIZE_DIVIDER) {
            size = (int) ((file.size / SIZE_DIVIDER) / SIZE_DIVIDER);
            fileHolder.tvFileSize.setText(String.valueOf("(" + size + " " + MB + ")"));
        } else
            fileHolder.tvFileSize.setText(String.valueOf("(" + size + " " + KB + ")"));
    }

    @NonNull
    private Target getTarget(final OneDriveFileValue file, final FileHolder fileHolder) {
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

    private void setDefaultImages(final OneDriveFileValue file, final FileHolder fileHolder) {
        if (file.folder != null) {
            fileHolder.tvFileSize.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(mContext, R.drawable.ic_folder_grey), null, null);
        } else {
            Drawable drawable = UserHelper.getImageFile(mContext, file.name);
            fileHolder.tvFileSize.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
        }
    }

    public void updateData(List<OneDriveFileValue> list) {
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
