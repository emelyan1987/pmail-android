package com.planckmail.fragments;

import android.app.DownloadManager;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.activities.ComposeActivity;
import com.planckmail.adapters.AddFileRecycleAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.db.manager.AccountInfoDataManager;
import com.planckmail.data.db.manager.DataBaseManager;
import com.planckmail.enums.AccountType;
import com.planckmail.helper.UserHelper;
import com.planckmail.listeners.RecyclerItemClickListener;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.utils.MimeUtils;
import com.planckmail.web.response.nylas.Contact;
import com.planckmail.web.response.nylas.Message;
import com.planckmail.web.response.nylas.wrapper.File;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.service.NylasService;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Terry on 3/23/2016.
 */
public class FilesPeopleFragment extends BaseFragment {
    public static final String FILE = "file";
    public static final String SEPARATOR = "-";
    public static final String DOT = ".";

    private RecyclerView mRecycleFiles;
    private AddFileRecycleAdapter mFileRecycleAdapter;
    private ProgressBar mProgress;
    private TextView mTvNoFiles;
    private ArrayList<File> mListFiles = new ArrayList<>();
    private DownloadManager mDownloadManager;
    private long mDownloadReference;
    private File mDownloadFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDownloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        getActivity().registerReceiver(mDownloadReceiver, filter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_people_files, container, false);

        initViews(view);
        setAdapter();
        setRecycleSettings();

        String jsonContact = getArguments().getString(BundleKeys.CONTACT);
        Contact contact = JsonUtilFactory.getJsonUtil().fromJson(jsonContact, Contact.class);
        if (contact.getEmail() != null)
            getData();
        else {
            mProgress.setVisibility(View.GONE);
            mTvNoFiles.setVisibility(View.VISIBLE);
            mTvNoFiles.setText(getString(R.string.peopleNoFiles, contact.getName()));
        }

        return view;
    }

    public void setRecycleSettings() {
        mRecycleFiles.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                File file = mListFiles.get(position);
                downloadFile(file);

                sendCachedData();
            }
        }));
        addDivider();
    }

    private void addDivider() {
        mRecycleFiles.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity())
                .sizeResId(R.dimen.edge_tiny)
                .colorResId(R.color.gray_divider)
                .build());
    }

    private void downloadFile(File file) {
        mDownloadFile = file;
        AccountInfo accountInfo = getAccountInfo();

        String url = RestWebClient.BASE_URL + "/files/" + file.getId() + "/download";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        final String credentials = accountInfo.accessToken + ":" + "";

        String endedCredential = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        request.addRequestHeader("Authorization", endedCredential);
        request.setTitle(getResources().getString(R.string.downloadingFile));

        if (file.getFilename() != null)
            request.setDescription(file.getFilename());

        request.setAllowedOverRoaming(false);
        request.setDestinationInExternalFilesDir(getActivity(), Environment.getExternalStorageState() + PlanckMailApplication.PLANK_MAIL_FILES, FILE + DOT + file.getContent_type());

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mDownloadReceiver);
    }

    public void sendCachedData() {
        Intent intent1 = new Intent(ComposeActivity.CACHING_ACTION);
        String listJson = JsonUtilFactory.getJsonUtil().toJson(mListFiles);
        intent1.putExtra(BundleKeys.CACHED_DATA, listJson);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent1);
    }

    private void getData() {
        AccountInfo accountInfo = getAccountInfo();
        String jsonContact = getArguments().getString(BundleKeys.CONTACT);
        Contact contact = JsonUtilFactory.getJsonUtil().fromJson(jsonContact, Contact.class);

        if (accountInfo != null) {
            if (mListFiles.isEmpty())
                mProgress.setVisibility(View.VISIBLE);

            HashMap<String, String> map = new HashMap<>();
            map.put("any_email", contact.getEmail());
            NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
            nylasServer.getMessagesFromMail(map, new Callback<Object>() {
                @Override
                public void success(Object o, Response response) {
                    mProgress.setVisibility(View.GONE);
                    String json = (String) o;

                    if (json.equalsIgnoreCase("[]")) {
                        mTvNoFiles.setVisibility(View.VISIBLE);
                    } else {
                        List<Message> listMessage = JsonUtilFactory.getJsonUtil().fromJsonArray(json, Message.class);
                        for (Message message : listMessage) {
                            if (!message.files.isEmpty())
                                mListFiles.addAll(message.files);
                        }
                        if (!mListFiles.isEmpty()) {
                            mFileRecycleAdapter.updateData(mListFiles);
                            mFileRecycleAdapter.notifyDataSetChanged();
                        } else
                            mTvNoFiles.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    Response r = error.getResponse();
                    if (r != null)
                        Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                }
            });
        }
    }

    @Nullable
    private AccountInfo getAccountInfo() {
        String jsonContact = getArguments().getString(BundleKeys.CONTACT);
        Contact contact = JsonUtilFactory.getJsonUtil().fromJson(jsonContact, Contact.class);

        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        List<AccountInfo> list = manager.getAllAccountInfoList();
        AccountInfo accountInfo = null;
        for (AccountInfo a : list) {
            if (contact.account_id.equalsIgnoreCase(a.accountId))
                accountInfo = a;
        }
        return accountInfo;
    }

    public void setAdapter() {
        String data = getArguments().getString(BundleKeys.CACHED_DATA);

        if (!TextUtils.isEmpty(data)) {
            mListFiles = JsonUtilFactory.getJsonUtil().fromJsonArray(data, File.class);
            mProgress.setVisibility(View.GONE);
        }

        mFileRecycleAdapter = new AddFileRecycleAdapter(getActivity(), mListFiles);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecycleFiles.setLayoutManager(mLayoutManager);
        mRecycleFiles.setHasFixedSize(true);
        mRecycleFiles.setAdapter(mFileRecycleAdapter);
    }

    private void initViews(View view) {
        mRecycleFiles = (RecyclerView) view.findViewById(R.id.recycleFile);
        mProgress = (ProgressBar) view.findViewById(R.id.progressLoadFile);
        mTvNoFiles = (TextView) view.findViewById(R.id.tvNoFiles);
    }

}
