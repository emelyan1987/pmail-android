package com.planckmail.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.v7.widget.CardView;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.activities.BaseActivity;
import com.planckmail.activities.ComposeActivity;
import com.planckmail.activities.MessageActivity;
import com.planckmail.activities.MessageDetailsActivity;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.helper.UserHelper;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.utils.MimeUtils;
import com.planckmail.web.helper.UtilHelpers;
import com.planckmail.web.request.nylas.ReadMessage;
import com.planckmail.web.response.nylas.Message;
import com.planckmail.web.response.nylas.wrapper.File;
import com.planckmail.web.response.nylas.wrapper.Participant;
import com.planckmail.web.restClient.RestPlankMail;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.TypedJsonString;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.service.NylasService;
import com.planckmail.web.restClient.service.PlanckService;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedInput;

/**
 * Created by Taras Matolinets on 21.05.15.
 */
public class RecycleMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String FILE = "file";
    public static final String SEPARATOR = "-";
    public static final String DOT = ".";
    private static final int VIEW_HEADER = 0;
    private static final int VIEW_ITEM = 1;
    private static final String KB = "kb";
    public static final double SIZE_DIVIDER = 1024.0;
    private static final String MB = "mb";
    private boolean mExpandAllViews;
    private boolean mShowAllSummarizeText;

    private final Context mContext;
    private DownloadManager mDownloadManager;
    private long mDownloadReference;
    private List<Message> mListData = new ArrayList<>();
    private HashMap<String, String> mMailSummarizeWords = new HashMap<>();
    private HashMap<String, Boolean> mMapExpand = new HashMap<>();
    private List<AccountInfo> mAllAccountInfoList;
    private File mDownloadFile;

    public RecycleMessageAdapter(Context context, List<Message> list) {
        mContext = context;
        mListData = list;
        mAllAccountInfoList = UserHelper.getEmailAccountList(true);

        mDownloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        mContext.registerReceiver(mDownloadReceiver, filter);
    }

    private void setTextSummarizeWords(List<Message> listData) {
        final int[] counter = new int[1];
        for (int i = 0; i < listData.size(); i++) {
            final Message message = listData.get(i);
            Spanned text = Html.fromHtml(message.body);

            RestPlankMail client = new RestPlankMail(RestPlankMail.BASE_URL1);
            PlanckService service = client.getPlankService();
            service.getBasicSummarize("keyphrase", text.toString(), new Callback<String>() {
                @Override
                public void success(String s, Response response) {
                    List<String> list = JsonUtilFactory.getJsonUtil().fromJsonArray(s, String.class);

                    String replaceSpaces = list.get(0).replaceAll("\\n+", "\n");
                    String[] split = replaceSpaces.split("\n");

                    for (String s1 : split)
                        message.body = message.body.replace(s1, "<span style=\"background-color: #FFFF00\">" + s1 + "</span>");

                    mMailSummarizeWords.put(message.id, message.body);
                    counter[0]++;

                    if (counter[0] == mListData.size())
                        notifyDataSetChanged();
                }

                @Override
                public void failure(RetrofitError error) {
                    Response r = error.getResponse();
                    if (r != null)
                        Log.e(PlanckMailApplication.TAG, "server error: " + r.getReason());
                }
            });
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        RecyclerView.ViewHolder mainViewHolder = null;
        switch (viewType) {
            case VIEW_HEADER:
                View viewHeader = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.elem_message_header, viewGroup, false);
                mainViewHolder = new HeaderViewHolder(viewHeader);
                break;
            case VIEW_ITEM:
                View viewItem = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.elem_message_item, viewGroup, false);
                mainViewHolder = new ItemViewHolder(viewItem);
                break;
        }
        return mainViewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

        if (viewHolder instanceof HeaderViewHolder) {
            if (!mListData.isEmpty()) {
                Message message = mListData.get(position);
                if (!TextUtils.isEmpty(message.subject)) {
                    ((HeaderViewHolder) viewHolder).mTvHeader.setText(message.subject);
                } else {
                    ((HeaderViewHolder) viewHolder).mTvHeader.setText(mContext.getString(R.string.subject));
                }
            }
        } else if (viewHolder instanceof ItemViewHolder) {
            if (position <= mListData.size()) {
                Message message = mListData.get(position - 1);

                fillItem(position - 1, (ItemViewHolder) viewHolder, message);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void fillItem(int position, ItemViewHolder viewHolder, Message message) {
        setTitle(position, viewHolder, message);
        showSearchedText(viewHolder.mWebDesc, message);

        viewHolder.ivMessageDetails.setTag(R.string.tag_message, message);
        viewHolder.tvDetails.setTag(R.string.tag_message, message);
        viewHolder.tvDetails.setTag(R.string.tag_root_view, viewHolder.cardMessage);
        viewHolder.cardMessage.setTag(R.string.tag_message, message);
        viewHolder.ivReplyBack.setTag(R.string.tag_message, message);
        viewHolder.mTvShowOriginal.setTag(R.string.tag_message, message);

        Activity activity = ((BaseActivity) mContext);

        if (activity instanceof MessageDetailsActivity)
            viewHolder.ivMessageDetails.setVisibility(View.GONE);

        if (position + 1 == mListData.size() || mExpandAllViews) {
            mMapExpand.put(message.id, true);
            openLastMessagePreview(viewHolder, message, View.VISIBLE);
        } else if (!mMapExpand.containsKey(message.id)) {
            mMapExpand.put(message.id, false);
            setBackGroundColor(viewHolder, message);
        }
        boolean isExpand = mMapExpand.get(message.id);

        setInfoFile(viewHolder, message);

        if (isExpand) {
            expandView(viewHolder);
        } else {
            hideView(viewHolder);
        }
    }

    private void setTitle(int position, ItemViewHolder viewHolder, Message message) {
        final String title = UtilHelpers.getParticipants(message.from);

        setWebSettings(viewHolder.mWebDesc, viewHolder.mProgressLoadDetails);

        //don't fill view snippet for last child in list
        if (position != mListData.size())
            viewHolder.tvSnippet.setText(message.snippet);

        viewHolder.mTvSender.setText(title);
    }

    private void setBackGroundColor(ItemViewHolder viewHolder, Message message) {
        if (message.unread) {
            viewHolder.cardMessage.setCardBackgroundColor(mContext.getResources().getColor(R.color.gray_email));
        } else
            viewHolder.cardMessage.setCardBackgroundColor(Color.WHITE);
    }

    private void hideView(ItemViewHolder viewHolder) {
        viewHolder.llDetails.setVisibility(View.GONE);
        viewHolder.flLoadWeb.setVisibility(View.GONE);
        viewHolder.ivReplyBack.setVisibility(View.GONE);
        viewHolder.llFooterMail.setVisibility(View.GONE);
        viewHolder.viewDivider.setVisibility(View.GONE);
        viewHolder.llMailFile.setVisibility(View.GONE);
    }

    private void expandView(ItemViewHolder viewHolder) {
        viewHolder.ivReplyBack.setVisibility(View.VISIBLE);
        viewHolder.llFooterMail.setVisibility(View.VISIBLE);
        viewHolder.viewDivider.setVisibility(View.VISIBLE);
        viewHolder.flLoadWeb.setVisibility(View.VISIBLE);
        viewHolder.llDetails.setVisibility(View.VISIBLE);

        if (mShowAllSummarizeText)
            viewHolder.mTvShowOriginal.setVisibility(View.VISIBLE);
    }

    private String getParticipants(List<Participant> list, boolean type) {
        StringBuilder builder = new StringBuilder();

        if (!list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                Participant p = list.get(i);

                if (type) {
                    if (!TextUtils.isEmpty(p.name)) {
                        getName(list, builder, i, p);
                    } else {
                        getEmail(list, builder, i, p);
                    }
                } else {
                    getEmail(list, builder, i, p);
                }
            }
        }
        return builder.toString();
    }

    private void getName(List<Participant> list, StringBuilder builder, int i, Participant p) {
        builder.append(p.name);
        if (i == list.size() - 1)
            builder.append(" ");
        else
            builder.append(", ");
    }

    private void getEmail(List<Participant> list, StringBuilder builder, int i, Participant p) {
        builder.append(p.email);

        if (i == list.size() - 1)
            builder.append(" ");
        else
            builder.append(", ");
    }

    private void setInfoFile(ItemViewHolder viewHolder, Message message) {
        //remove all view for prevent duplication
        viewHolder.llMailFile.removeAllViews();

        if (!message.files.isEmpty()) {
            viewHolder.llMailFile.setVisibility(View.VISIBLE);
            viewHolder.viewDividerFile.setVisibility(View.VISIBLE);

            for (File f : message.files) {
                showFile(viewHolder.llMailFile, f);
            }
        } else {
            viewHolder.llMailFile.setVisibility(View.GONE);
            viewHolder.viewDividerFile.setVisibility(View.GONE);
        }
    }

    private void openLastMessagePreview(ItemViewHolder holder, Message message, int state) {
        if (message != null) {
            holder.ivReplyBack.setVisibility(state);
            holder.llFooterMail.setVisibility(state);
            holder.viewDivider.setVisibility(state);
            holder.tvDetails.setVisibility(state);
            holder.llDetails.setVisibility(state);

            String formattedDate = getParsedDate(message);
            holder.tvDate.setText(formattedDate);

            holder.flLoadWeb.setVisibility(state);

            String to = getParticipants(message.to, true);
            holder.tvSnippet.setText(to);

            if (message.unread) {
                holder.cardMessage.setCardBackgroundColor(mContext.getResources().getColor(R.color.gray_email));
                markMessageAsRead(message, holder.cardMessage);
            } else
                holder.cardMessage.setCardBackgroundColor(Color.WHITE);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return VIEW_HEADER;
        else
            return VIEW_ITEM;
    }

    @Override
    public int getItemCount() {
        //+ 1 because we have header
        return mListData.size() + 1;
    }

    public void openNewMessage(Message message) {

        String accessToken = ((MessageActivity) mContext).getIntent().getStringExtra(BundleKeys.ACCESS_TOKEN);
        int childId = ((MessageActivity) mContext).getIntent().getIntExtra(BundleKeys.CHILD_ID, -1);
        final String messageJson = JsonUtilFactory.getJsonUtil().toJson(message);

        Intent intent = new Intent(mContext, MessageDetailsActivity.class);

        intent.putExtra(BundleKeys.ACCESS_TOKEN, accessToken);
        intent.putExtra(BundleKeys.MESSAGES, messageJson);
        intent.putExtra(BundleKeys.CHILD_ID, childId);

        ((Activity) (mContext)).startActivityForResult(intent, MessageDetailsActivity.REQUEST_CODE);
    }

    public void replyBack(View view, final Message message) {
        PopupMenu popupMenu = new PopupMenu(mContext, view);
        popupMenu.inflate(R.menu.menu_message_reply);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.itemReply:
                        startComposeActivity(0, message);
                        break;
                    case R.id.itemReplyAll:
                        startComposeActivity(1, message);
                        break;
                    case R.id.itemForward:
                        startComposeActivity(2, message);
                        break;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    public void cardOpenMessageAction(View view, Message message) {
        TextView tvDetails = (TextView) view.findViewById(R.id.tvDetails);
        FrameLayout flLoadWeb = (FrameLayout) view.findViewById(R.id.flWeb);
        ImageView ivReplyBack = (ImageView) view.findViewById(R.id.ivReplyBack);
        View viewDivider = view.findViewById(R.id.divider);
        View viewDividerFile = view.findViewById(R.id.dividerFile);
        TextView tvDate = (TextView) view.findViewById(R.id.tvDate);
        LinearLayout llFooterMail = (LinearLayout) view.findViewById(R.id.llFooterMail);
        LinearLayout llHiddenDetails = (LinearLayout) view.findViewById(R.id.llHideDetails);
        LinearLayout llDetails = (LinearLayout) view.findViewById(R.id.llDetails);

        TextView toDescription = (TextView) view.findViewById(R.id.tvToDescription);
        TextView tvSnippet = (TextView) view.findViewById(R.id.tvSnippet);
        CardView cardMessage = (CardView) view.findViewById(R.id.cardMessage);

        LinearLayout llMailFile = (LinearLayout) view.findViewById(R.id.llMailFile);
        //remove all view for prevent duplication
        llMailFile.removeAllViews();

        boolean isExpand = mMapExpand.get(message.id);

        if (isExpand) {
            mMapExpand.put(message.id, false);

            tvDetails.setVisibility(View.GONE);
            flLoadWeb.setVisibility(View.GONE);
            ivReplyBack.setVisibility(View.GONE);
            viewDivider.setVisibility(View.GONE);
            viewDividerFile.setVisibility(View.GONE);

            llFooterMail.setVisibility(View.GONE);
            tvDate.setVisibility(View.GONE);
            llHiddenDetails.setVisibility(View.GONE);
            llDetails.setVisibility(View.GONE);
            llMailFile.setVisibility(View.GONE);

            tvSnippet.setText(message.snippet);
        } else {
            if (message.unread) {
                cardMessage.setCardBackgroundColor(mContext.getResources().getColor(R.color.gray_email));
                markMessageAsRead(message, cardMessage);
            } else {
                cardMessage.setCardBackgroundColor(Color.WHITE);
            }
            mMapExpand.put(message.id, true);

            String to = getParticipants(message.to, true);
            tvSnippet.setText(to);
            viewDividerFile.setVisibility(View.VISIBLE);
            tvDetails.setVisibility(View.VISIBLE);
            flLoadWeb.setVisibility(View.VISIBLE);
            ivReplyBack.setVisibility(View.VISIBLE);
            viewDivider.setVisibility(View.VISIBLE);
            llFooterMail.setVisibility(View.VISIBLE);
            llDetails.setVisibility(View.VISIBLE);
            tvDate.setVisibility(View.VISIBLE);

            String formattedDate = getParsedDate(message);
            tvDate.setText(formattedDate);

            showMailParticipants(message, toDescription);

            if (!message.files.isEmpty()) {
                llMailFile.setVisibility(View.VISIBLE);
                for (File f : message.files) {
                    showFile(llMailFile, f);
                }
            }
        }
    }

    private void showMailParticipants(Message message, TextView toDescription) {
        StringBuilder sb = new StringBuilder();

        for (Participant p : message.to) {
            sb.append(p.email);
            sb.append(" ");
        }
        toDescription.setText(sb);
    }

    public void setListData(List<Message> listData) {
        mListData = listData;
        setTextSummarizeWords(listData);
    }

    private void markMessageAsRead(final Message message, final CardView cardMessage) {
        String accessToken = ((MessageActivity) mContext).getIntent().getStringExtra(BundleKeys.ACCESS_TOKEN);

        String json = createRequestBody();
        TypedInput in = new TypedJsonString(json);

        final NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accessToken, "");
        nylasServer.markReadMessage(message.id, in, new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                Log.i(PlanckMailApplication.TAG, "response url " + response.getUrl());

                String json = (String) o;
                cardMessage.setCardBackgroundColor(Color.WHITE);
                Message m = JsonUtilFactory.getJsonUtil().fromJson(json, Message.class);

                ImageView ivMessageDetails = (ImageView) cardMessage.findViewById(R.id.ivOpenMessage);
                ivMessageDetails.setTag(R.string.tag_message, m);

                //replace current message with new information
                for (int i = 0; i < mListData.size(); i++) {
                    if (message.id.equalsIgnoreCase(m.id)) {
                        mListData.set(i, m);
                        return;
                    }
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "server error: " + r.getReason());
            }
        });
    }

    private String createRequestBody() {
        ReadMessage body = new ReadMessage();
        body.setRead(false);
        return JsonUtilFactory.getJsonUtil().toJson(body);
    }

    /**
     * @param type    type message reply = 0, replyAll = 1,forward  = 2;
     * @param message message from sender
     */
    private void startComposeActivity(int type, Message message) {
        String accessToken = ((MessageActivity) mContext).getIntent().getStringExtra(BundleKeys.ACCESS_TOKEN);

        String json = JsonUtilFactory.getJsonUtil().toJson(mListData);
        Intent intent = new Intent(mContext, ComposeActivity.class);
        intent.putExtra(BundleKeys.MESSAGES, json);
        intent.putExtra(BundleKeys.MESSAGE_TYPE, type);
        intent.putExtra(BundleKeys.ACCESS_TOKEN, accessToken);
        intent.putExtra(BundleKeys.GROUP_ID, ((MessageActivity) mContext).getIntent().getIntExtra(BundleKeys.GROUP_ID, -1));

        String singleJsonMessage = JsonUtilFactory.getJsonUtil().toJson(message);
        intent.putExtra(BundleKeys.SINGLE_MESSAGE, singleJsonMessage);

        (mContext).startActivity(intent);
    }

    private void showFile(final LinearLayout llMailFile, final File f) {
        View viewShowFile = View.inflate(mContext, R.layout.elem_file_message, null);

        LinearLayout llMainfiles = (LinearLayout) viewShowFile.findViewById(R.id.llMailFile);
        TextView tvFileSize = (TextView) viewShowFile.findViewById(R.id.tvFileSize);
        TextView tvFileName = (TextView) viewShowFile.findViewById(R.id.tvFileName);
        ImageView ivFile = (ImageView) viewShowFile.findViewById(R.id.ivFile);
        ivFile.setImageResource(R.drawable.ic_attachment_black);
        llMainfiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.llMailFile:
                        downloadFile(f);
                        break;
                }
            }
        });

        if (TextUtils.isEmpty(f.filename))
            tvFileName.setText(R.string.noName);
        else
            tvFileName.setText(f.filename);

        int size = (int) (f.size / SIZE_DIVIDER);

        if (size >= SIZE_DIVIDER) {
            size = (int) ((f.size / SIZE_DIVIDER) / SIZE_DIVIDER);
            tvFileSize.setText(String.valueOf("(" + size + " " + MB + ")"));
        } else
            tvFileSize.setText(String.valueOf("(" + size + " " + KB + ")"));

        llMailFile.addView(viewShowFile);
    }

    private void showDetailsView(Message message, View view) {
        TextView tvDetails = (TextView) view.findViewById(R.id.tvDetails);
        FrameLayout flLoadWeb = (FrameLayout) view.findViewById(R.id.flWeb);
        TextView tvDate = (TextView) view.findViewById(R.id.tvDate);
        LinearLayout llHiddenDetails = (LinearLayout) view.findViewById(R.id.llHideDetails);
        TextView tvSnippet = (TextView) view.findViewById(R.id.tvSnippet);
        TextView toDescription = (TextView) view.findViewById(R.id.tvToDescription);
        TextView ccDescription = (TextView) view.findViewById(R.id.tvCcDescription);
        LinearLayout llCc = (LinearLayout) view.findViewById(R.id.llCc);
        TextView dateMail = (TextView) view.findViewById(R.id.tvDateMailDescription);
        LinearLayout llDetails = (LinearLayout) view.findViewById(R.id.llDetails);

        if (llHiddenDetails.getVisibility() == View.GONE) {
            llDetails.setVisibility(View.VISIBLE);
            tvDetails.setText(R.string.hideDetails);

            flLoadWeb.setVisibility(View.VISIBLE);
            tvDate.setVisibility(View.GONE);

            String from = getParticipants(message.from, false);
            tvSnippet.setText(from);

            llHiddenDetails.setVisibility(View.VISIBLE);

            Date date = new Date(message.date * 1000);
            SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMM dd, h:mm a", java.util.Locale.getDefault());

            dateMail.setText(formatter.format(date));

            String getDetailToEmail = getDetailToEmail(message.to);
            toDescription.setText(getDetailToEmail);

            String from1 = getParticipants(message.from, false);
            tvSnippet.setText(from1);

            if (!message.cc.isEmpty()) {
                llCc.setVisibility(View.VISIBLE);
                String cc = getParticipants(message.cc, false);
                ccDescription.setText(cc);
            } else
                llCc.setVisibility(View.GONE);

        } else if (llHiddenDetails.getVisibility() == View.VISIBLE) {
            tvDetails.setText(R.string.details);
            tvSnippet.setText(message.snippet);
            tvDate.setVisibility(View.VISIBLE);

            String to = getParticipants(message.to, true);
            tvSnippet.setText(to);

            llHiddenDetails.setVisibility(View.GONE);
        }
    }

    private String getDetailToEmail(List<Participant> list) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            Participant p = list.get(i);
            builder.append(p.email);

            if (i == list.size() - 1)
                builder.append(" ");
            else
                builder.append("\n");
        }
        return builder.toString();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setWebSettings(final WebView webView, final ProgressBar progressBar) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void showSearchedText(final WebView webView, final Message message) {
        String result = message.body;

        if (mExpandAllViews && mShowAllSummarizeText && mMailSummarizeWords.containsKey(message.id)) {
            String summerizeWordsList = mMailSummarizeWords.get(message.id);

//            for (String s1 : summerizeWordsList)
//                builder.append(s1);

            webView.loadDataWithBaseURL("", summerizeWordsList, "text/html", "UTF-8", "");
        } else if (mMailSummarizeWords.containsKey(message.id)) {
            String summerizeWordsList = mMailSummarizeWords.get(message.id);
            webView.loadDataWithBaseURL("", summerizeWordsList, "text/html", "UTF-8", "");
        } else {
            webView.loadDataWithBaseURL("", result, "text/html", "UTF-8", "");
        }
    }

//    private String getResult(Message message, List<String> summerizeWordsList) {
//        String html = message.body;
//        StringBuilder builder = new StringBuilder();
//
//        for (String res : summerizeWordsList) {
//            Highlighter hl = new Highlighter(res, html);
//            String newHtmlString = hl.getHighlightedHtml();
//            builder.append(newHtmlString);
//        }
//        return builder.toString();
//    }

    public void setExpandAllViews(boolean state) {
        mExpandAllViews = state;
    }

    public void setShowSummarizeText(boolean state) {
        mShowAllSummarizeText = state;
    }

    public boolean getShowSummarizeText() {
        return mShowAllSummarizeText;
    }

    public boolean getExpandAllViewState() {
        return mExpandAllViews;
    }

    private void downloadFile(File file) {
        String accessToken = ((MessageActivity) mContext).getIntent().getStringExtra(BundleKeys.ACCESS_TOKEN);

        mDownloadFile = file;

        String url = RestWebClient.BASE_URL + "/files/" + file.getId() + "/download";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

        final String credentials = accessToken + ":" + "";

        String endedCredential = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        request.addRequestHeader("Authorization", endedCredential);
        request.setTitle(mContext.getResources().getString(R.string.downloadingFile));

        if (file.getFilename() != null)
            request.setDescription(file.getFilename());

        request.setAllowedOverRoaming(false);
        request.setDestinationInExternalFilesDir(mContext, Environment.getExternalStorageState() + PlanckMailApplication.PLANK_MAIL_FILES, FILE + DOT + file.getContent_type());

        mDownloadReference = mDownloadManager.enqueue(request);
    }

    private BroadcastReceiver mDownloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (mDownloadReference == referenceId) {
                try {
                    ParcelFileDescriptor file = mDownloadManager.openDownloadedFile(mDownloadReference);
                    FileInputStream fileInputStream = new ParcelFileDescriptor.AutoCloseInputStream(file);

                    DateTime date = new DateTime();
                    DateTimeFormatter formatterDateStart = DateTimeFormat.forPattern("hh-mm-ss");
                    String fileFormat = MimeUtils.guessExtensionFromMimeType(mDownloadFile.getContent_type());

                    String fileName = FILE + SEPARATOR + formatterDateStart.print(date) + DOT + fileFormat;

                    UserHelper.copyInputStreamToFile(fileInputStream, fileName);
                } catch (java.io.IOException e) {
                    Log.e(PlanckMailApplication.TAG, e.toString());
                }
            }
        }
    };

    public void unregisterReceiver() {
        mContext.unregisterReceiver(mDownloadReceiver);
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final View viewDividerFile;
        private final ImageView ivMessageDetails;
        public LinearLayout llDetails;
        public WebView mWebDesc;
        public TextView mTvShowOriginal;
        public TextView tvDate;
        public ImageView ivReplyBack;
        public TextView tvDetails;
        public LinearLayout llFooterMail;
        public TextView tvSnippet;
        public TextView mTvSender;
        public LinearLayout llHiddenDetails;
        public LinearLayout llMain;
        public TextView dateMail;
        public TextView ccDescription;
        public TextView toDescription;
        public ProgressBar mProgressLoadDetails;
        public FrameLayout flLoadWeb;
        public CardView cardMessage;
        public LinearLayout llCc;
        public View viewDivider;
        public LinearLayout llMailFile;

        public ItemViewHolder(View view) {
            super(view);

            llMain = (LinearLayout) view.findViewById(R.id.rlMain);
            llCc = (LinearLayout) view.findViewById(R.id.llCc);
            llHiddenDetails = (LinearLayout) view.findViewById(R.id.llHideDetails);
            llFooterMail = (LinearLayout) view.findViewById(R.id.llFooterMail);
            flLoadWeb = (FrameLayout) view.findViewById(R.id.flWeb);
            cardMessage = (CardView) view.findViewById(R.id.cardMessage);
            llDetails = (LinearLayout) view.findViewById(R.id.llDetails);

            mTvShowOriginal = (TextView) view.findViewById(R.id.tvShowOriginal);
            mTvSender = (TextView) view.findViewById(R.id.tvName);
            tvSnippet = (TextView) view.findViewById(R.id.tvSnippet);
            tvDetails = (TextView) view.findViewById(R.id.tvDetails);
            tvDate = (TextView) view.findViewById(R.id.tvDate);
            viewDivider = view.findViewById(R.id.divider);
            mProgressLoadDetails = (ProgressBar) view.findViewById(R.id.prLoadDetails);
            mWebDesc = (WebView) view.findViewById(R.id.webViewMessageDesc);
            toDescription = (TextView) view.findViewById(R.id.tvToDescription);
            ccDescription = (TextView) view.findViewById(R.id.tvCcDescription);
            dateMail = (TextView) view.findViewById(R.id.tvDateMailDescription);
            ivReplyBack = (ImageView) view.findViewById(R.id.ivReplyBack);
            ivMessageDetails = (ImageView) view.findViewById(R.id.ivOpenMessage);
            llMailFile = (LinearLayout) view.findViewById(R.id.llMailFile);
            viewDividerFile = view.findViewById(R.id.dividerFile);
            setListeners();
        }

        private void setListeners() {
            tvDetails.setOnClickListener(this);
            cardMessage.setOnClickListener(this);
            ivReplyBack.setOnClickListener(this);
            mTvShowOriginal.setOnClickListener(this);
            ivMessageDetails.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            final Message message = mListData.get(getAdapterPosition() - 1);
            switch (view.getId()) {
                case R.id.ivReplyBack:
                    replyBack(view, message);
                    break;
                case R.id.tvDetails:
                    CardView cardView = (CardView) view.getTag(R.string.tag_root_view);
                    showDetailsView(message, cardView);
                    break;
                case R.id.cardMessage:
                    cardOpenMessageAction(view, message);
                    break;
                case R.id.ivOpenMessage:
                    openNewMessage(message);
                    break;

                case R.id.tvShowOriginal:
                    View view1 = view.getRootView();

                    WebView webView = (WebView) view1.findViewById(R.id.webViewMessageDesc);
                    view.setVisibility(View.GONE);

                    if (mMailSummarizeWords.containsKey(message.id)) {
                        String summerizeWordsList = mMailSummarizeWords.get(message.id);
                        webView.loadDataWithBaseURL("", summerizeWordsList, "text/html", "UTF-8", "");
                    }
                    break;
            }
        }
    }

    public String getParsedDate(Message message) {
        Date date = new Date(message.date * 1000);
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd", java.util.Locale.getDefault());
        return formatter.format(date);
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public TextView mTvHeader;

        public HeaderViewHolder(View v) {
            super(v);
            mTvHeader = (TextView) v.findViewById(R.id.tvSubject);
        }
    }
}
