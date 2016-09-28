package com.planckmail.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.helper.UserHelper;
import com.planckmail.web.response.nylas.wrapper.File;

import java.util.List;

/**
 * Created by Taras Matolinets on 08.09.15.
 */
public class AddFileRecycleAdapter extends RecyclerView.Adapter<AddFileRecycleAdapter.FileHolder> {

    public static final double SIZE_DIVIDER = 1024.0;
    private static final String MB = "mb";
    private static final String KB = "kb";
    private final Context mContext;
    private List<File> mData;

    public AddFileRecycleAdapter(Context context, List<File> list) {
        mContext = context;
        mData = list;
    }

    @Override
    public FileHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.elem_file, parent, false);
        return new FileHolder(view);
    }

    @Override
    public void onBindViewHolder(FileHolder holder, int position) {
        File file = mData.get(position);

        if (file.getFilename() != null)
            holder.tvAccountName.setText(file.getFilename());
        else
            holder.tvAccountName.setText(mContext.getString(R.string.noName));

        Drawable drawable = UserHelper.getImageFile(mContext, file.content_type);
        holder.tvFileSize.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);

        holder.tvFileType.setText(file.getContent_type());

        setFileSize(holder, file);
    }

    public void setFileSize(FileHolder holder, File file) {
        int size = (int) (file.size / SIZE_DIVIDER);
        if (size >= SIZE_DIVIDER) {
            size = (int) ((file.size / SIZE_DIVIDER) / SIZE_DIVIDER);
            holder.tvFileSize.setText(String.valueOf("(" + size + " " + MB + ")"));
        } else
            holder.tvFileSize.setText(String.valueOf("(" + size + " " + KB + ")"));
    }

    public void updateData(List<File> list) {
        mData = list;
    }


    @Override
    public int getItemCount() {
        return mData.size();
    }

    static class FileHolder extends RecyclerView.ViewHolder {
        private final TextView tvFileType;
        public TextView tvAccountName;
        public TextView tvFileSize;

        public FileHolder(View itemView) {
            super(itemView);
            tvAccountName = (TextView) itemView.findViewById(R.id.tvFileName);
            tvFileSize = (TextView) itemView.findViewById(R.id.tvFileSize);
            tvFileType = (TextView) itemView.findViewById(R.id.tvFileType);
        }
    }
}
