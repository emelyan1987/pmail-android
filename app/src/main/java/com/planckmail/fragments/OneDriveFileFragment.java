package com.planckmail.fragments;

import android.app.DownloadManager;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.planckmail.R;
import com.planckmail.activities.ComposeActivity;
import com.planckmail.activities.MenuActivity;
import com.planckmail.adapters.BaseOneDriveAdapter;
import com.planckmail.adapters.SimpleSectionedRecyclerViewAdapter;
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
import com.planckmail.web.response.googleDrive.GoogleDriveFile;
import com.planckmail.web.response.oneDrive.OneDriveFile;
import com.planckmail.web.response.oneDrive.OneDriveListFile;
import com.planckmail.web.response.oneDrive.OneDriveFileValue;
import com.planckmail.web.response.oneDrive.OneDriveToken;
import com.planckmail.web.restClient.RestClientGoogleDriveClient;
import com.planckmail.web.restClient.api.AutOneDriveApi;
import com.planckmail.web.restClient.RestClientOneDriveClient;
import com.planckmail.web.restClient.service.OneDriveService;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Terry on 1/17/2016.
 */
public class OneDriveFileFragment extends BaseFragment implements BaseFragment.OnBackPressed, SearchView.OnQueryTextListener {
    public static final String FILE = "file";
    public static final String SEPARATOR = "/";
    public static final String ROOT_PATH = "drive/root:";
    private static final int UN_AUTHORISE = 401;
    private RecyclerView mRecycleFiles;
    private BaseOneDriveAdapter mFileBaseRecycleAdapter;
    private ProgressBar mProgress;
    private TextView mTvNoFiles;
    private List<OneDriveFileValue> mListFiles = new ArrayList<>();
    private SimpleSectionedRecyclerViewAdapter mSectionedAdapter;
    private String mOneDriveChildPath;
    private DownloadManager mDownloadManager;
    private long mDownloadReference;
    private OneDriveFileValue mOneDriveFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mDownloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        getActivity().registerReceiver(downloadReceiver, filter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_files, container, false);

        initViews(view);
        setAdapter();
        setRecycleSettings();
        getOneDriveData(ROOT_PATH);

        return view;
    }

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (mDownloadReference == referenceId) {
                try {
                    ParcelFileDescriptor file = mDownloadManager.openDownloadedFile(mDownloadReference);
                    FileInputStream fileInputStream = new ParcelFileDescriptor.AutoCloseInputStream(file);

                    UserHelper.copyInputStreamToFile(fileInputStream, mOneDriveFile.name);
                } catch (java.io.IOException e) {
                    Log.e(PlanckMailApplication.TAG, e.toString());
                }
            }
        }
    };

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem item = menu.getItem(0);
        item.setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_filter:
                showFilterPopupMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void tvShowNoFileImage() {
        if (mListFiles.isEmpty()) {
            mTvNoFiles.setVisibility(View.VISIBLE);
        } else {
            mTvNoFiles.setVisibility(View.GONE);
        }
    }

    private void showFilterPopupMenu() {
        View view = ((MenuActivity) getActivity()).getToolbar();
        PopupMenu popupMenu = new PopupMenu(getActivity(), view, Gravity.RIGHT);
        popupMenu.inflate(R.menu.menu_search_files);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.filterAll:
                        filterAll();
                        break;
                    case R.id.filterImage:
                        filterImage();
                        break;
                    case R.id.filterSlide:
                        getSlide();
                        break;
                    case R.id.filterDoc:
                        getDoc();
                        break;
                    case R.id.filterPdf:
                        getPdf();
                        break;
                    case R.id.filterZip:
                        getZip();
                        break;
                }
                return false;
            }

            private void getZip() {
                List<OneDriveFileValue> listFiles = new ArrayList<>();

                for (OneDriveFileValue value : mListFiles) {
                    if (value.folder == null) {
                        String fileFormat = MimeUtils.guessExtensionFromMimeType(value.file.mimeType);

                        if (fileFormat.equalsIgnoreCase("zip"))
                            listFiles.add(value);
                    }
                    mListFiles = listFiles;
                    mSectionedAdapter.deleteAllSections();
                    mFileBaseRecycleAdapter.updateData(listFiles);
                    tvShowNoFileImage();
                }
            }

            private void getPdf() {
                List<OneDriveFileValue> listFiles = new ArrayList<>();

                for (OneDriveFileValue value : mListFiles) {
                    if (value.folder == null) {
                        String fileFormat = MimeUtils.guessExtensionFromMimeType(value.file.mimeType);

                        if (fileFormat.equalsIgnoreCase("pdf"))
                            listFiles.add(value);
                    }
                    mListFiles = listFiles;
                    mSectionedAdapter.deleteAllSections();
                    mFileBaseRecycleAdapter.updateData(listFiles);
                    tvShowNoFileImage();
                }
            }

            public void filterAll() {
                loadAllFiles();
            }

            public void filterImage() {
                List<OneDriveFileValue> listFiles = new ArrayList<>();

                for (OneDriveFileValue value : mListFiles) {
                    if (value.folder == null) {
                        String fileFormat = MimeUtils.guessExtensionFromMimeType(value.file.mimeType);

                        if (fileFormat.equalsIgnoreCase("png") || fileFormat.equalsIgnoreCase("jpeg") || fileFormat.equalsIgnoreCase("jpg"))
                            listFiles.add(value);
                    }
                }
                mListFiles = listFiles;
                mSectionedAdapter.deleteAllSections();
                mFileBaseRecycleAdapter.updateData(listFiles);
                tvShowNoFileImage();
            }

            public void getSlide() {
                List<OneDriveFileValue> listFiles = new ArrayList<>();

                for (OneDriveFileValue value : mListFiles) {
                    if (value.folder == null) {
                        String fileFormat = MimeUtils.guessExtensionFromMimeType(value.file.mimeType);

                        if (fileFormat.equalsIgnoreCase("slide"))
                            listFiles.add(value);
                    }
                    mListFiles = listFiles;
                    mSectionedAdapter.deleteAllSections();
                    mFileBaseRecycleAdapter.updateData(listFiles);
                    tvShowNoFileImage();
                }
            }

            public void getDoc() {
                List<OneDriveFileValue> listFiles = new ArrayList<>();

                for (OneDriveFileValue value : mListFiles) {
                    if (value.folder == null) {
                        String fileFormat = MimeUtils.guessExtensionFromMimeType(value.file.mimeType);

                        if (fileFormat.equalsIgnoreCase("doc") || fileFormat.equalsIgnoreCase("docx") || fileFormat.equalsIgnoreCase("docm"))
                            listFiles.add(value);
                    }
                    mListFiles = listFiles;
                    mSectionedAdapter.deleteAllSections();
                    mFileBaseRecycleAdapter.updateData(listFiles);
                    tvShowNoFileImage();
                }
            }
        });
        popupMenu.show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_drop_box_search, menu);
        final MenuItem mSearchMenu = menu.findItem(R.id.action_search);

        SearchManager manager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);

        final SearchView search = (SearchView) menu.findItem(R.id.action_search).getActionView();
        closeButtonListener(mSearchMenu, search);
        search.setSearchableInfo(manager.getSearchableInfo(getActivity().getComponentName()));
        search.setOnQueryTextListener(this);
    }

    public void closeButtonListener(final MenuItem mSearchMenu, final SearchView search) {
        // Get the search close button image view
        ImageView closeButton = (ImageView) search.findViewById(R.id.search_close_btn);
        // Set on click listener
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Find EditText view
                EditText et = (EditText) search.findViewById(R.id.search_src_text);
                //Clear the text from EditText view
                et.setText("");
                //Clear query
                search.setQuery("", false);
                //Collapse the action view
                search.onActionViewCollapsed();
                //Collapse the search widget
                mSearchMenu.collapseActionView();

                loadAllFiles();

            }
        });
    }

    private void loadAllFiles() {
        if (mOneDriveChildPath == null)
            getOneDriveData(ROOT_PATH);
        else
            getOneDriveData(mOneDriveChildPath);
    }

    public void setRecycleSettings() {
        mRecycleFiles.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                position = mSectionedAdapter.sectionedPositionToPosition(position);
                OneDriveFileValue file = mOneDriveFile = mListFiles.get(position);
                if (file.folder != null) {
                    mProgress.setVisibility(View.GONE);
                    String folderPath = file.parentReference.path + SEPARATOR + file.name;
                    //avoid extra separator
                    mOneDriveChildPath = folderPath = folderPath.substring(1);
                    getOneDriveData(folderPath);
                } else if (getActivity() instanceof MenuActivity) {
                    downloadFile(file);
                } else {
                    shareFiles(file);
                }

            }
        }));
        addDivider();
    }

    private void downloadFile(OneDriveFileValue file) {
        AccountType type = (AccountType) getArguments().getSerializable(BundleKeys.ACCOUNT_TYPE);
        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        AccountInfo accountInfo = manager.getAccountInfoByType(type);

        String url = RestClientOneDriveClient.BASE_URL1 + "/drive/items/" + file.id + "/content";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle(getResources().getString(R.string.downloadingFile));
        request.setDescription(file.name);

        String endedCredential = "Bearer " + accountInfo.accessToken;
        request.addRequestHeader("Authorization", endedCredential);

        request.setAllowedOverRoaming(false);

        request.setDestinationInExternalFilesDir(getActivity(), Environment.getExternalStorageState() + PlanckMailApplication.PLANK_MAIL_FILES, file.name);

        mDownloadReference = mDownloadManager.enqueue(request);
    }

    private void shareFiles(final OneDriveFileValue file) {
        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        if (getActivity() instanceof ComposeActivity) {
            ((ComposeActivity) getActivity()).setDropBoxLink(file.webUrl, file.name);
        }
    }

    private void addDivider() {
        mRecycleFiles.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity())
                .sizeResId(R.dimen.edge_tiny)
                .colorResId(R.color.gray_divider)
                .build());
    }

    private void getOneDriveData(String folderName) {
        AccountType type = (AccountType) getArguments().getSerializable(BundleKeys.ACCOUNT_TYPE);
        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        AccountInfo accountInfo = manager.getAccountInfoByType(type);
        getActivity().setTitle(getString(R.string.oneDrive));
        ((MenuActivity)getActivity()).getToolbar().setSubtitle(accountInfo.email);
        getOneDriveMetaData(folderName, accountInfo);
    }

    private void getNewToken(final AccountInfo accountInfo) {
        RestClientOneDriveClient oneDriveApi = RestClientOneDriveClient.getInstance();
        OneDriveService oneDriveService = oneDriveApi.getOneDriveService();
        oneDriveService.getNewRefreshToken(getString(R.string.oneDriveAppKey),
                getString(R.string.oneDriveRedirectUri), getString(R.string.oneDriveClientSecret), accountInfo.getRefreshToken(), "refresh_token", new Callback<Object>() {

                    @Override
                    public void success(Object o, Response response) {
                        String json = (String) o;
                        OneDriveToken token = JsonUtilFactory.getJsonUtil().fromJson(json, OneDriveToken.class);
                        setOneDriveAccountInfo(accountInfo, token);
                        getOneDriveMetaData(ROOT_PATH, accountInfo);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Response r = error.getResponse();
                        if (r != null)
                            Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                    }
                });
    }

    private void setOneDriveAccountInfo(AccountInfo accountInfo, OneDriveToken token) {
        accountInfo.setAccessToken(token.access_token);
        accountInfo.setRefreshToken(token.refresh_token);

        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        manager.createOrUpdateAccountInfo(accountInfo);
    }

    private void getOneDriveMetaData(String folderName, final AccountInfo accountInfo) {
        OneDriveService dropBoxServer = AutOneDriveApi.createService(OneDriveService.class, RestClientOneDriveClient.BASE_URL1, accountInfo.accessToken, "");
        dropBoxServer.getOneDriveMetaData(folderName, new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                mProgress.setVisibility(View.GONE);
                String json = (String) o;
                OneDriveListFile fileList = JsonUtilFactory.getJsonUtil().fromJson(json, OneDriveListFile.class);

                mListFiles = fileList.children;
                mFileBaseRecycleAdapter.updateData(mListFiles);

                if (fileList.children.isEmpty()) {
                    mTvNoFiles.setVisibility(View.VISIBLE);
                } else {
                    mTvNoFiles.setVisibility(View.GONE);
                    setAccountSections(fileList.children);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null && r.getStatus() == UN_AUTHORISE) {
                    mProgress.setVisibility(View.VISIBLE);

                    getNewToken(accountInfo);
                } else if (r != null) {
                    mProgress.setVisibility(View.GONE);
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                }
            }
        });
    }

    private void setAccountSections(List<OneDriveFileValue> list) {
        List<SimpleSectionedRecyclerViewAdapter.Section> sections = new ArrayList<>();

        String files = getResources().getString(R.string.files);
        String folders = getResources().getString(R.string.folders);
        boolean fileState = false;
        boolean emailState = false;

        for (int i = 0; i < list.size(); i++) {
            OneDriveFileValue file = list.get(i);

            if (file.folder != null && !emailState) {
                emailState = true;
                sections.add(new SimpleSectionedRecyclerViewAdapter.Section(i, folders));
            } else if (file.folder == null && !fileState) {
                fileState = true;
                sections.add(new SimpleSectionedRecyclerViewAdapter.Section(i, files));
            }
        }

        SimpleSectionedRecyclerViewAdapter.Section[] arraySections = new SimpleSectionedRecyclerViewAdapter.Section[sections.size()];
        mSectionedAdapter.setSections(sections.toArray(arraySections));
    }

    public void setAdapter() {
        AccountType type = (AccountType) getArguments().getSerializable(BundleKeys.ACCOUNT_TYPE);
        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        AccountInfo accountInfo = manager.getAccountInfoByType(type);

        mFileBaseRecycleAdapter = new BaseOneDriveAdapter(getActivity(), mListFiles, accountInfo);
        mSectionedAdapter = new SimpleSectionedRecyclerViewAdapter(getActivity(), R.layout.elem_follow_up_section, R.id.section_text, mFileBaseRecycleAdapter);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecycleFiles.setLayoutManager(mLayoutManager);
        mRecycleFiles.setHasFixedSize(true);
        mRecycleFiles.setAdapter(mSectionedAdapter);
    }

    private void initViews(View view) {
        mRecycleFiles = (RecyclerView) view.findViewById(R.id.recycleFile);
        mProgress = (ProgressBar) view.findViewById(R.id.progressLoadFile);
        mTvNoFiles = (TextView) view.findViewById(R.id.tvNoFiles);
    }

    @Override
    public void onBackPress() {
        if (TextUtils.isEmpty(mOneDriveChildPath) || mOneDriveChildPath.equals(ROOT_PATH)) {
            getFragmentManager().popBackStackImmediate();
        } else {
            mProgress.setVisibility(View.VISIBLE);
            buildPath();
        }
    }

    private void buildPath() {
        StringBuilder pathBuilder;

        String[] split = mOneDriveChildPath.split("/");
        if (split.length > 1) {
            pathBuilder = getStringBuilder(split, 1);
            //avoid extra separator
            mOneDriveChildPath = pathBuilder.toString().substring(1);
        } else {
            mOneDriveChildPath = ROOT_PATH;
        }
        getOneDriveData(mOneDriveChildPath);
    }

    @NonNull
    private StringBuilder getStringBuilder(String[] split, int elem) {
        StringBuilder pathBuilder = new StringBuilder();
        for (int i = 0; i < split.length - elem; i++) {
            pathBuilder.append(SEPARATOR);
            pathBuilder.append(split[i]);

            if (i == split.length - 1)
                pathBuilder.append(SEPARATOR);
        }
        return pathBuilder;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(final String newText) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mProgress.setVisibility(View.VISIBLE);
                AccountType type = (AccountType) getArguments().getSerializable(BundleKeys.ACCOUNT_TYPE);
                AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
                AccountInfo accountInfo = manager.getAccountInfoByType(type);
                String key1 = "q";

                HashMap<String, String> mapQuery = new HashMap<>();
                mapQuery.put(key1, newText);

                OneDriveService dropBoxServer = AutOneDriveApi.createService(OneDriveService.class, RestClientOneDriveClient.BASE_URL1, accountInfo.accessToken, "");
                dropBoxServer.searchFiles(mapQuery, new Callback<Object>() {
                    @Override
                    public void success(Object o, Response response) {
                        String json = (String) o;
                        mProgress.setVisibility(View.GONE);

                        OneDriveListFile fileList = JsonUtilFactory.getJsonUtil().fromJson(json, OneDriveListFile.class);

                        mListFiles = fileList.value;

                        if (mListFiles.isEmpty())
                            mTvNoFiles.setVisibility(View.VISIBLE);
                        else
                            mTvNoFiles.setVisibility(View.GONE);

                        mSectionedAdapter.deleteAllSections();
                        mFileBaseRecycleAdapter.updateData(mListFiles);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        mProgress.setVisibility(View.GONE);

                        Response r = error.getResponse();
                        if (r != null)
                            Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                    }
                });
            }
        }, 800);
        return false;
    }
}
