package com.planckmail.adapters;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.RecyclerSwipeAdapter;
import com.planckmail.R;
import com.planckmail.activities.BaseActivity;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.dialogs.SwipeDialog;
import com.planckmail.dialogs.SwipeDialog.IPickedCustomDate;
import com.planckmail.dialogs.UnsubscribeDialog;
import com.planckmail.dialogs.UnsubscribeDialog.IRemoveUnsubscribeThread;
import com.planckmail.enums.AccountType;
import com.planckmail.enums.Folders;
import com.planckmail.dialogs.SwipeConfirmationDialog;
import com.planckmail.fragments.ThreadFragment;
import com.planckmail.helper.UserHelper;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.helper.UtilHelpers;
import com.planckmail.web.request.nylas.UpdateFolder;
import com.planckmail.web.request.nylas.UpdateLabel;
import com.planckmail.web.response.nylas.Message;
import com.planckmail.web.response.nylas.Thread;
import com.planckmail.web.response.nylas.wrapper.Folder;
import com.planckmail.web.response.nylas.wrapper.Participant;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.RestPlankMail;
import com.planckmail.web.restClient.TypedJsonString;
import com.planckmail.web.restClient.service.NylasService;
import com.planckmail.web.restClient.service.PlanckService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedInput;

/**
 * Created by Taras Matolinets on 07.05.15.
 */
public class RecycleThreadAdapter extends RecyclerSwipeAdapter<RecyclerView.ViewHolder> {

    private static final long TIME_STAMP = 1000;
    private static final int VIEW_PROGRESS = 0;
    private static final String GMAIL = "gmail";
    private static final int VIEW_ITEM = 1;
    private boolean mStopLoading = true;

    private int mChildId;
    private Context mContext;
    private final Calendar mCalendar;
    private OnElementClickListener mListener;

    private List<AccountInfo> mAccountInfoList;
    private List<Thread> mListDate = new ArrayList<>();
    private HashMap<Integer, Integer> mMailMapColor = new HashMap<>();
    private boolean mShowUnsubscribe;

    public RecycleThreadAdapter(Context context, List<Thread> list, OnElementClickListener listener) {
        mCalendar = Calendar.getInstance();
        mListDate = list;
        mContext = context;
        mListener = listener;
        mAccountInfoList = UserHelper.getEmailAccountList(true);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        RecyclerView.ViewHolder mViewHolder;
        switch (viewType) {
            case VIEW_PROGRESS:
                View progress = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.progress_item, viewGroup, false);
                mViewHolder = new ProgressViewHolder(progress);
                break;
            default:
                View viewItem = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.elem_inbox_list, viewGroup, false);
                mViewHolder = new ContentViewHolder(viewItem);
                break;
        }

        return mViewHolder;
    }

    public List<Thread> getListDate() {
        return mListDate;
    }

    @Override
    public int getItemViewType(int position) {
        //1 - show progress when load more mails
        int offset = 1;

        if (position + offset > mListDate.size())
            return VIEW_PROGRESS;
        else
            return VIEW_ITEM;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof ContentViewHolder) {
            if (!mListDate.isEmpty()) {
                final Thread thread = mListDate.get(position);
                ContentViewHolder contentViewHolder = (ContentViewHolder) viewHolder;
                setContent(contentViewHolder, thread, position);
            }
        } else if (viewHolder instanceof ProgressViewHolder) {
            if (mStopLoading)
                ((ProgressViewHolder) viewHolder).progressBar.setVisibility(View.GONE);
            else {
                ((ProgressViewHolder) viewHolder).progressBar.setVisibility(View.VISIBLE);
                ((ProgressViewHolder) viewHolder).progressBar.setIndeterminate(true);
            }
        }
    }

    public void stopLoading(boolean state) {
        mStopLoading = state;
    }

    public boolean getStopLoading() {
        return mStopLoading;
    }

    @SuppressWarnings("deprecation")
    private void setContent(ContentViewHolder viewHolder, Thread thread, int position) {
        if (mShowUnsubscribe) {
            viewHolder.btUnsubscribe.setTag(R.string.tag_thread, thread);
            viewHolder.btUnsubscribe.setVisibility(View.VISIBLE);
        } else
            viewHolder.btUnsubscribe.setVisibility(View.GONE);

        setBackground(thread, viewHolder, position);
        setTitle(thread, viewHolder);
        setMessageCount(viewHolder, thread);
        setTag(viewHolder, thread, position);
        setThreadInformation(viewHolder, thread);
        replied(viewHolder, thread);

        if (mChildId != 0)
            viewHolder.swipeInbox.setSwipeEnabled(false);
    }

    private int getColorForBox(int id) {
        int color;
        if (mMailMapColor.containsKey(id))
            color = mMailMapColor.get(id);
        else {
            Random rnd = new Random();
            color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
            mMailMapColor.put(id, color);
        }
        return color;
    }

    private void replied(ContentViewHolder viewHolder, Thread thread) {
        boolean isReplied = isReplyParticipant(thread);

        if (isReplied)
            viewHolder.ivReply.setVisibility(View.VISIBLE);
        else
            viewHolder.ivReply.setVisibility(View.GONE);
    }

    private void setMessageCount(ContentViewHolder viewHolder, Thread thread) {
        int size = thread.message_ids.size();

        int minMessage = 1;
        if (size > minMessage) {
            viewHolder.messageCount.setVisibility(View.VISIBLE);
            viewHolder.messageCount.setText(String.valueOf(size));
        } else
            viewHolder.messageCount.setVisibility(View.GONE);
    }

    private void setThreadInformation(ContentViewHolder viewHolder, Thread thread) {
        viewHolder.date.setText(parseDate(thread.last_message_timestamp));
        Typeface typeFace = getTypeFace(thread);
        viewHolder.title.setTypeface(typeFace);

        if (!TextUtils.isEmpty(thread.subject)) {
            viewHolder.subject.setText(thread.subject);
        } else {
            viewHolder.subject.setText("-");
        }
        viewHolder.snippet.setSingleLine();

        if (!TextUtils.isEmpty(thread.snippet))
            viewHolder.snippet.setText(thread.snippet);
        else
            viewHolder.snippet.setText("-");
    }

    private void setTag(ContentViewHolder viewHolder, Thread thread, int position) {
        viewHolder.llTitle.setTag(R.string.tag_progress, viewHolder.progressLoadText);
        viewHolder.llTitle.setTag(R.string.tag_snippet, viewHolder.snippet);
        viewHolder.llTitle.setTag(R.string.tag_showOriginal, viewHolder.showOriginal);
        viewHolder.llTitle.setTag(R.string.tag_thread, thread);

        viewHolder.layoutLeftSnooze.setTag(R.string.tag_thread, thread);
        viewHolder.layoutLeftSnooze.setTag(R.string.tag_swipe, viewHolder.swipeInbox);

        viewHolder.layoutLeftNotify.setTag(R.string.tag_thread, thread);
        viewHolder.layoutLeftNotify.setTag(R.string.tag_swipe, viewHolder.swipeInbox);

        viewHolder.swipeInbox.setTag(R.string.tag_position, position);
        viewHolder.snippet.setTag(thread);

        viewHolder.showOriginal.setTag(R.string.tag_snippet, viewHolder.snippet);
        viewHolder.showOriginal.setTag(R.string.tag_thread, thread);
    }

    private void setTitle(Thread thread, final ContentViewHolder holder) {
        final String title = UtilHelpers.getParticipants(thread.participants);
        String shortName = UtilHelpers.buildTitle(title);
        holder.title.setText(title);
        holder.tvMailPicture.setText(shortName);
    }

    private void setBackground(Thread thread, ContentViewHolder holder, int position) {
        int color = getColorForBox(position);

        int radius = 300;
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(radius);
        shape.setColor(color);

        holder.tvMailPicture.setBackgroundDrawable(shape);

        int backgroundColor = getColorBackground(thread);
        holder.swipeInbox.setBackgroundColor(backgroundColor);

        int resId = getActionPicture(thread);
        if (resId != 0) {
            holder.ivShowFile.setBackgroundResource(resId);
            holder.ivShowFile.setVisibility(View.VISIBLE);
        } else
            holder.ivShowFile.setVisibility(View.GONE);
    }

    private int getColorBackground(Thread thread) {
//        if (thread.unread) {
//            return mContext.getResources().getColor(R.color.gray_email);
//        }
        return Color.WHITE;
    }

    private Typeface getTypeFace(Thread thread) {
        if (thread.unread) {
            return Typeface.DEFAULT_BOLD;
        }
        return Typeface.DEFAULT;
    }

    private boolean isReplyParticipant(Thread thread) {
        boolean isCopyDetected = false;

        AccountInfo accountInfo = getAccount(thread);

        int counter = 0;
        if (accountInfo != null) {
            for (Participant p : thread.participants) {
                if (p.email.equalsIgnoreCase(accountInfo.getEmail()))
                    counter++;
            }
        }
        if (counter > 1)
            isCopyDetected = true;

        return isCopyDetected;
    }

    private int getActionPicture(Thread thread) {
        if (thread.has_attachments) {
            return R.drawable.ic_attachment_black;
        }
        return 0;
    }

    private String parseDate(long millis) {
        String formattedDate;

        Date date = new Date(millis * TIME_STAMP);
        SimpleDateFormat formatter;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        boolean day = calendar.get(Calendar.DAY_OF_MONTH) != mCalendar.get(Calendar.DAY_OF_MONTH);
        boolean month = calendar.get(Calendar.MONTH) != mCalendar.get(Calendar.MONTH);
        boolean year = calendar.get(Calendar.YEAR) != mCalendar.get(Calendar.YEAR);

        if (day && month && year) {
            formatter = new SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault());
            formattedDate = formatter.format(date);
        } else {
            formatter = new SimpleDateFormat("HH:mm a", java.util.Locale.getDefault());
            formattedDate = formatter.format(date);
        }
        return formattedDate;
    }

    public void updateData(List<Thread> list) {
        mListDate = list;
    }

    @Override
    public int getItemCount() {
        return mListDate.size() + 1;
    }

    @Override
    public int getSwipeLayoutResourceId(int i) {
        return R.id.swipeInbox;
    }

    private AccountInfo getAccount(Thread thread) {
        // set account for getting contacts
        for (AccountInfo accountInfo : mAccountInfoList) {
            if (accountInfo.getAccountId().equalsIgnoreCase(thread.account_id)) {
                return accountInfo;
            }
        }
        return null;
    }

    public void setChildId(int childId) {
        mChildId = childId;
    }

    public void showUnsubscribeButton(boolean showUnsubscribe) {
        mShowUnsubscribe = showUnsubscribe;
    }

    public class ContentViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            SwipeLayout.SwipeListener, View.OnLongClickListener, IPickedCustomDate, SwipeConfirmationDialog.OnConfirmButtonClick, IRemoveUnsubscribeThread {

        private final ImageView ivShowFile;
        private TextView tvMailPicture;
        public ProgressBar progressLoadText;
        public LinearLayout llTitle;
        public TextView title;
        public TextView subject;
        public TextView snippet;
        public TextView showOriginal;
        public TextView date;
        public ImageView ivReply;
        public TextView messageCount;
        public SwipeLayout swipeInbox;
        public FrameLayout layoutLeftSnooze;
        public FrameLayout layoutLeftNotify;
        public LinearLayout llLeftSwipe;
        public FrameLayout llRightSwipe;
        public Button btUnsubscribe;

        public ContentViewHolder(View v) {
            super(v);
            tvMailPicture = (TextView) itemView.findViewById(R.id.tvMailPicture);
            llTitle = (LinearLayout) v.findViewById(R.id.llTitle);
            ivReply = (ImageView) v.findViewById(R.id.ivReply);
            ivShowFile = (ImageView) v.findViewById(R.id.ivShowFile);
            showOriginal = (TextView) v.findViewById(R.id.tvShowOriginal);
            title = (TextView) v.findViewById(R.id.tvName);
            btUnsubscribe = (Button) v.findViewById(R.id.btUnsubcribe);
            subject = (TextView) v.findViewById(R.id.tvSubject);
            date = (TextView) v.findViewById(R.id.tvDate);
            snippet = (TextView) v.findViewById(R.id.tvSnippet);
            messageCount = (TextView) v.findViewById(R.id.tvCountMessages);
            llLeftSwipe = (LinearLayout) v.findViewById(R.id.llLeft);
            layoutLeftSnooze = (FrameLayout) v.findViewById(R.id.flLeftSnooze);
            layoutLeftNotify = (FrameLayout) v.findViewById(R.id.flLeftNotify);
            llRightSwipe = (FrameLayout) v.findViewById(R.id.flRight);
            swipeInbox = (SwipeLayout) v.findViewById(R.id.swipeInbox);
            swipeInbox.addDrag(SwipeLayout.DragEdge.Left, llLeftSwipe);
            swipeInbox.addDrag(SwipeLayout.DragEdge.Right, llRightSwipe);
            progressLoadText = (ProgressBar) v.findViewById(R.id.progressLoadText);
            setListener();


            /**
             * Set component style
             */

            Typeface typeface = Typeface.createFromAsset(mContext.getAssets(), "fonts/Helvetica.ttf");
            btUnsubscribe.setTypeface(typeface);
        }

        /**
         * @param swipeType  type of swipe
         * @param jsonObject converted Json Object
         */
        @NonNull
        private Bundle getBundle(SWIPE_TYPE swipeType, String jsonObject) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(BundleKeys.SWIPE_TYPE, swipeType);
            bundle.putString(BundleKeys.THREAD, jsonObject);
            bundle.putBoolean(BundleKeys.IS_THREAD, true);
            return bundle;
        }

        private void setListener() {
            llTitle.setOnLongClickListener(this);
            llTitle.setOnClickListener(this);
            layoutLeftNotify.setOnClickListener(this);
            layoutLeftSnooze.setOnClickListener(this);
            swipeInbox.addSwipeListener(this);
            showOriginal.setOnClickListener(this);
            btUnsubscribe.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.tvShowOriginal:
                    Thread thread = (Thread) v.getTag(R.string.tag_thread);
                    TextView tvSnippet = (TextView) v.getTag(R.string.tag_snippet);
                    tvSnippet.setSingleLine(true);
                    tvSnippet.setTextSize(12);
                    tvSnippet.setTypeface(Typeface.DEFAULT);
                    tvSnippet.setText(thread.snippet);

                    v.setVisibility(View.GONE);
                    break;
                case R.id.llTitle:
                    if (mListener != null)
                        mListener.onElementClicked(getTargetPosition(getAdapterPosition()));
                    break;
                case R.id.flLeftSnooze:
                    showDialog(SWIPE_TYPE.SNOOZE, v);
                    break;
                case R.id.flLeftNotify:
                    showDialog(SWIPE_TYPE.NOTIFY, v);
                    break;
                case R.id.btUnsubcribe:
                    Thread threadForUnscubscribe = (Thread) v.getTag(R.string.tag_thread);
                    unsubscribe(threadForUnscubscribe);
                    break;
            }
        }

        private void unsubscribe(Thread threadForUnscubscribe) {

            AccountInfo accountInfo = getAccount(threadForUnscubscribe);
            Participant participant = threadForUnscubscribe.participants.get(0);

            String jsonAccountInfo = JsonUtilFactory.getJsonUtil().toJson(accountInfo);
            String jsonParticipantInfo = JsonUtilFactory.getJsonUtil().toJson(participant);

            if (accountInfo != null) {
                Bundle bundle = new Bundle();
                bundle.putString(BundleKeys.ACCOUNT, jsonAccountInfo);
                bundle.putString(BundleKeys.PARTICIPANT, jsonParticipantInfo);
                bundle.putInt(BundleKeys.POSITION, getAdapterPosition());
                UnsubscribeDialog dialog = new UnsubscribeDialog();
                dialog.setListener(this);
                dialog.setArguments(bundle);
                dialog.show(((Activity) mContext).getFragmentManager(), dialog.getClass().getName());
            }
        }

        private void showDialog(SWIPE_TYPE swipeType, View v) {
            Thread thread1 = (Thread) v.getTag(R.string.tag_thread);
            SwipeLayout swipeLayout = (SwipeLayout) v.getTag(R.string.tag_swipe);

            String threadJson = JsonUtilFactory.getJsonUtil().toJson(thread1);

            Bundle bundle = getBundle(swipeType, threadJson);
            runSwipeDialog(swipeLayout, bundle);
        }

        @Override
        public void onStartOpen(SwipeLayout layout) {

        }

        @Override
        public void onOpen(SwipeLayout swipeLayout) {
            if (swipeLayout.getDragEdge() == SwipeLayout.DragEdge.Right) {
                getFolders(swipeLayout);
            }
        }

        @Override
        public void onStartClose(SwipeLayout layout) {

        }

        @Override
        public void onClose(SwipeLayout layout) {

        }

        @Override
        public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset) {

        }

        @Override
        public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {

        }

        @Override
        public boolean onLongClick(View v) {
            switch (v.getId()) {
                case R.id.llTitle:
                    final Thread thread = (Thread) v.getTag(R.string.tag_thread);
                    final TextView tvSnippet = (TextView) v.getTag(R.string.tag_snippet);
                    final ProgressBar tvProgress = (ProgressBar) v.getTag(R.string.tag_progress);
                    final TextView tvShowOriginal = (TextView) v.getTag(R.string.tag_showOriginal);
                    tvProgress.setVisibility(View.VISIBLE);

                    AccountInfo accountInfo = getAccount(thread);
                    if (accountInfo != null) {
                        NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
                        nylasServer.getMessages(thread.id, new Callback<Object>() {
                            @Override
                            public void success(Object o, Response response) {
                                String json = (String) o;
                                List<Message> list = JsonUtilFactory.getJsonUtil().fromJsonArray(json, Message.class);

                                Message latestMessage = list.get(list.size() - 1);
                                RestPlankMail client = new RestPlankMail(RestPlankMail.BASE_URL1);
                                PlanckService service = client.getPlankService();

                                service.getKeyphrases(latestMessage.subject, latestMessage.body, new Callback<String>() {
                                    @Override
                                    public void success(String s, Response response) {
                                        List<String> list = JsonUtilFactory.getJsonUtil().fromJsonArray(s, String.class);
                                        StringBuilder builder = new StringBuilder();

                                        for (int i = 0; i < list.size(); i++) {
                                            builder.append(list.get(i));

                                            if (i != list.size() - 1)
                                                builder.append(", ");

                                        }
                                        tvShowOriginal.setVisibility(View.VISIBLE);
                                        tvProgress.setVisibility(View.GONE);
                                        tvSnippet.setSingleLine(false);
                                        tvSnippet.setTextSize(14);
                                        tvSnippet.setTypeface(Typeface.DEFAULT_BOLD);
                                        tvSnippet.setText(builder);
                                    }

                                    @Override
                                    public void failure(RetrofitError error) {
                                        Response r = error.getResponse();
                                        if (r != null) {
                                            Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                                        }
                                    }
                                });
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                Response r = error.getResponse();
                                if (r != null) {
                                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                                }
                            }
                        });
                    }
                    break;
            }
            return false;
        }

        private void runSwipeDialog(SwipeLayout swipeLayout, Bundle bundle) {
            SwipeDialog dialog = new SwipeDialog();
            dialog.setListener(this);
            dialog.setArguments(bundle);
            dialog.setSwipeLayout(swipeLayout);
            dialog.show(((Activity) mContext).getFragmentManager(), null);
        }

        private void getFolders(final SwipeLayout swipeLayout) {
            final int positionAdapter = getTargetPosition(getAdapterPosition());
            final Thread thread = mListDate.get(positionAdapter);

            mListDate.remove(positionAdapter);
            notifyItemRemoved(positionAdapter);

            AccountInfo accountInfo = getAccount(thread);

            final NylasService nylasServer;
            if (accountInfo != null) {
                AccountType type = accountInfo.accountType;
                String folderLabel;

                if (type.toString().equalsIgnoreCase(AccountType.GMAIL.toString()))
                    folderLabel = "labels";
                else
                    folderLabel = "folders";

                nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
                nylasServer.getFolders(folderLabel, new Callback<Object>() {
                    @Override
                    public void success(Object o, Response response) {
                        String json = (String) o;
                        List<Folder> listFolder = JsonUtilFactory.getJsonUtil().fromJsonArray(json, Folder.class);
                        int text = R.string.archivingMessage;
                        showStatus(text);

                        moveThreadToFolder(positionAdapter, swipeLayout, thread, listFolder, Folders.ALL_MAIL);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        int posAdapter = getTargetPosition(positionAdapter);
                        backToOriginalState(posAdapter, swipeLayout, error, thread);
                    }
                });
            }
        }

        private int getTargetPosition(int position) {
            Fragment fragment = ((Activity) mContext).getFragmentManager().findFragmentById(R.id.mainContainer);

            if (fragment instanceof ThreadFragment) {
                ThreadFragment threadFragment = (ThreadFragment) fragment;
                ThreadFragment.TARGET_FOLDER folder = threadFragment.getTargetFolder();

                if (folder.equals(ThreadFragment.TARGET_FOLDER.FOLLOW_UP)) {
                    SimpleSectionedRecyclerViewAdapter adapter = threadFragment.getSectionPosition();
                    position = adapter.sectionedPositionToPosition(position);
                }
            }
            return position;
        }

        private void backToOriginalState(int position, SwipeLayout swipeLayout, RetrofitError error, Thread thread) {
            mListDate.add(position, thread);
            notifyItemInserted(position);

            swipeLayout.close();

            Response r = error.getResponse();
            if (r != null) {
                Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        }

        @Override
        public void pickedDate(SwipeLayout swipeLayout, SWIPE_TYPE swipeType, long millis) {
            Thread thread = mListDate.get(getTargetPosition(getAdapterPosition()));
            String jsonThread = JsonUtilFactory.getJsonUtil().toJson(thread);

            Bundle bundle = new Bundle();
            bundle.putString(BundleKeys.THREAD, jsonThread);
            bundle.putLong(BundleKeys.TIME, millis);
            bundle.putInt(BundleKeys.POSITION, getTargetPosition(getAdapterPosition()));

            if (swipeType == SWIPE_TYPE.NOTIFY)
                bundle.putBoolean(BundleKeys.IS_NOTIFY, true);

            bundle.putSerializable(BundleKeys.SWIPE_TYPE, swipeType);

            SwipeConfirmationDialog fragment = new SwipeConfirmationDialog();
            fragment.setSwipeLayout(swipeLayout);
            fragment.setArguments(bundle);
            fragment.setListener(this);
            FragmentManager manager = ((BaseActivity) mContext).getFragmentManager();

            fragment.show(manager, fragment.getClass().getName());
        }

        @Override
        public void pickedDate(String threadId, long millis) {
        }

        @Override
        public void confirmSwipeActionClick(SWIPE_TYPE swipeType, SwipeLayout swipe, Thread thread, long millis, int position, boolean checkRemind) {
            final AccountInfo accountInfo = getAccount(thread);

            mListDate.remove(position);
            notifyItemRemoved(position);

            leftSwipe(position, thread, swipe, millis, accountInfo, swipeType);
        }

        @Override
        public void confirmSwipeSnoozeClick(Thread thread, long millis) {
        }

        private void notifyMe(final Thread thread, final SwipeLayout swipe, final long millis, final AccountInfo accountInfo) {
            RestPlankMail client = new RestPlankMail(RestPlankMail.BASE_URL2);
            PlanckService service = client.getPlankService();

            service.add_thread_to_notify(accountInfo.email, thread.id, thread.message_ids.get(0), 1, thread.subject, millis, new Callback<String>() {
                @Override
                public void success(String s, Response response) {
                    Log.i(PlanckMailApplication.TAG, "response notify me " + s);
                    int text = R.string.reminderSet;
                    showStatus(text);
                }

                @Override
                public void failure(RetrofitError error) {
                    swipe.close();
                    Response r = error.getResponse();
                    if (r != null) {
                        Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                    }
                }
            });
        }

        private void sendSnoozeTime(final AccountInfo accountInfo, final Thread thread, final long millis) {
            RestPlankMail client = new RestPlankMail(RestPlankMail.BASE_URL2);
            PlanckService service = client.getPlankService();
            service.add_thread_to_snooze(accountInfo.email, thread.id, "abc", accountInfo.accessToken, millis, new Callback<String>() {
                @Override
                public void success(String s, Response response) {
                    Log.i(PlanckMailApplication.TAG, "response snooze " + s);

                    int text = R.string.emailSnoozed;
                    showStatus(text);
                }

                @Override
                public void failure(RetrofitError error) {
                    Response r = error.getResponse();
                    if (r != null) {
                        Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                    }
                }
            });
        }

        private void showStatus(int text) {
            BaseActivity activity = (BaseActivity) mContext;
            Fragment fragment = activity.getCurrentFragment(R.id.mainContainer);
            if (fragment instanceof ThreadFragment) {
                View view = fragment.getView();

                Snackbar snackbar = Snackbar.make(view, mContext.getString(text), Snackbar.LENGTH_SHORT);
                View snackBarView = snackbar.getView();
                snackBarView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.lightGreen));
                snackbar.show();
            }
        }

        private void leftSwipe(final int position, final Thread thread, final SwipeLayout swipe, final long millis, final AccountInfo accountInfo, final SWIPE_TYPE swipeType) {
            String folderLabel;
            AccountType type = accountInfo.accountType;

            if (type.toString().equalsIgnoreCase(AccountType.GMAIL.toString()))
                folderLabel = "labels";
            else
                folderLabel = "folders";

            final NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
            nylasServer.getFolders(folderLabel, new Callback<Object>() {
                @Override
                public void success(Object o, Response response) {
                    String json = (String) o;
                    List<Folder> listFolder = JsonUtilFactory.getJsonUtil().fromJsonArray(json, Folder.class);
                    moveThreadToFolder(position, swipe, thread, listFolder, Folders.FOLLOW_UP);

                    if (swipeType.equals(SWIPE_TYPE.SNOOZE)) {
                        int text = R.string.snoozingEmail;
                        showStatus(text);
                        sendSnoozeTime(accountInfo, thread, millis);
                    } else {
                        int text = R.string.reminderEmail;
                        showStatus(text);
                        notifyMe(thread, swipe, millis, accountInfo);
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    backToOriginalState(position, swipe, error, thread);
                }
            });
        }

        public void moveThreadToFolder(final int position, final SwipeLayout swipe, final Thread thread, final List<Folder> listFolder, final Folders searchedFolder) {
            AccountInfo accountInfo = getAccount(thread);
            String jsonResult = getFolder(listFolder, searchedFolder, accountInfo, thread.labels);

            if (accountInfo != null) {
                final NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
                final TypedInput in = new TypedJsonString(jsonResult);

                nylasServer.updateRemoveThread(thread.id, in, new Callback<Object>() {
                    @Override
                    public void success(Object o, Response response) {
                        updateCacheData();
                        Log.i(PlanckMailApplication.TAG, " email moved successfully");

                        if (swipe.getDragEdge() == SwipeLayout.DragEdge.Right) {
                            int text = R.string.archivedMessage;
                            showStatus(text);
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        swipe.close();
                        Response r = error.getResponse();
                        if (r != null) {
                            Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                            Toast.makeText(mContext, r.getReason(), Toast.LENGTH_SHORT).show();

                            mListDate.add(position, thread);
                            notifyItemInserted(position);
                        }
                    }
                });
            }
        }

        public void updateCacheData() {
            String json = JsonUtilFactory.getJsonUtil().toJson(mListDate);

            Fragment fragment = ((Activity) mContext).getFragmentManager().findFragmentById(R.id.mainContainer);
            if (fragment instanceof ThreadFragment) {
                ((ThreadFragment) fragment).saveDataToCache(json);
            }
        }

        public String getFolder(List<Folder> listFolder, Folders searchedFolder, AccountInfo accountInfo, List<Folder> threadLabels) {
            String searchedName = searchedFolder.toString();
            String result;

            if (accountInfo.accountType.equals(AccountType.GMAIL))
                result = getConvertedLabel(listFolder, searchedName, threadLabels);
            else
                result = getConvertedFolder(listFolder, searchedName);

            return result;
        }

        private String getConvertedFolder(List<Folder> listFolder, String searchedName) {
            UpdateFolder folder = new UpdateFolder();
            for (Folder newLabel : listFolder) {
                if (newLabel.display_name.equalsIgnoreCase(searchedName)) {
                    folder.folder_id = newLabel.id;
                }
            }
            return JsonUtilFactory.getJsonUtil().toJson(folder);
        }

        private String getConvertedLabel(List<Folder> listFolder, String searchedName, List<Folder> threadLabels) {
            UpdateLabel folder = new UpdateLabel();

            for (Folder label : listFolder) {
                for (Folder innerFolder : threadLabels) {
                    boolean isSentEmail = innerFolder.display_name.equalsIgnoreCase(Folders.SENT_MAIL.toString());

                    if (isSentEmail && !folder.label_ids.contains(innerFolder.id)) {
                        folder.label_ids.add(innerFolder.id);
                        break;
                    }
                }
                if (label.display_name.equals(searchedName)) {
                    folder.label_ids.add(label.id);
                }
            }
            return JsonUtilFactory.getJsonUtil().toJson(folder);
        }

        @Override
        public void removeUnsubscribeThread(int position) {
            mListDate.remove(position);
            notifyItemRemoved(position);
        }
    }

    public static class ProgressViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progressBar;

        public ProgressViewHolder(View v) {
            super(v);
            progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        }
    }

    public interface OnElementClickListener {
        void onElementClicked(int position);
    }

    public enum SWIPE_TYPE {
        SNOOZE, NOTIFY
    }

}
