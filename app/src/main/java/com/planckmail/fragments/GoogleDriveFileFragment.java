package com.planckmail.fragments;

import android.app.DownloadManager;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.util.Base64;
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

import com.google.android.gms.common.api.GoogleApiClient;
import com.planckmail.R;
import com.planckmail.activities.ComposeActivity;
import com.planckmail.activities.MenuActivity;
import com.planckmail.adapters.BaseGoogleDriveAdapter;
import com.planckmail.adapters.SimpleSectionedRecyclerViewAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.db.manager.AccountInfoDataManager;
import com.planckmail.data.db.manager.DataBaseManager;
import com.planckmail.enums.AccountType;
import com.planckmail.fragments.BaseFragment.OnBackPressed;
import com.planckmail.helper.UserHelper;
import com.planckmail.listeners.RecyclerItemClickListener;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.utils.MimeUtils;
import com.planckmail.web.response.dropBox.DropBoxFile;
import com.planckmail.web.response.dropBox.DropBoxFileMedia;
import com.planckmail.web.response.googleDrive.GoogleDriveFile;
import com.planckmail.web.response.googleDrive.GoogleDriveListFiles;
import com.planckmail.web.response.googleDrive.GoogleDriveParent;
import com.planckmail.web.response.oneDrive.OneDriveToken;
import com.planckmail.web.restClient.RestClientDropBoxClient;
import com.planckmail.web.restClient.RestClientGoogleDriveClient;
import com.planckmail.web.restClient.api.AuthDropBoxApi;
import com.planckmail.web.restClient.api.AuthGoogleDriveApi;
import com.planckmail.web.restClient.service.DropBoxService;
import com.planckmail.web.restClient.service.GoogleDriveService;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Terry on 1/21/2016.
 */
public class GoogleDriveFileFragment extends BaseFragment implements OnBackPressed, OnQueryTextListener {
    private static final String ROOT_PATH = "root";

    private static String ROOT_FOLDER_ID;
    private static String PARENT_PATH;
    private static final int UN_AUTHORISE = 401;
    private DownloadManager mDownloadManager;
    private long mDownloadReference;

    private RecyclerView mRecycleFiles;
    private BaseGoogleDriveAdapter mFileBaseRecycleAdapter;
    private ProgressBar mProgress;
    private TextView mTvNoFiles;
    private List<GoogleDriveFile> mListFiles = new ArrayList<>();
    private SimpleSectionedRecyclerViewAdapter mSectionedAdapter;
    private HashMap<String, String> mMapTreeFileId = new HashMap<>();
    private GoogleDriveFile mGoogleDriveFile;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_files, container, false);
        initViews(view);

        mDownloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        getActivity().registerReceiver(downloadReceiver, filter);

        setAdapter();
        setRecycleSettings();
        getGoogleDriveData(ROOT_PATH);
        return view;
    }

    public void setRecycleSettings() {
        mRecycleFiles.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                position = mSectionedAdapter.sectionedPositionToPosition(position);
                GoogleDriveFile file = mGoogleDriveFile = mListFiles.get(position);
                mMapTreeFileId.put(file.id, file.parents.get(0));

                String folderMimeType = "application/vnd.google-apps.folder";

                if (file.mimeType.equalsIgnoreCase(folderMimeType)) {
                    mProgress.setVisibility(View.GONE);
                    getGoogleDriveData(file.id);
                } else if (getActivity() instanceof MenuActivity) {
                    downloadFile(file);
                } else {
                    shareFiles(file);
                }
            }
        }));
        addDivider();
    }

    private void downloadFile(GoogleDriveFile file) {
        AccountType type = (AccountType) getArguments().getSerializable(BundleKeys.ACCOUNT_TYPE);
        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        AccountInfo accountInfo = manager.getAccountInfoByType(type);

        if (file.webContentLink == null) {
            Toast.makeText(getActivity(), R.string.failedOpenFile, Toast.LENGTH_SHORT).show();
            return;
        }
        String url = RestClientGoogleDriveClient.BASE_URL1 + "/drive/v3/files/" + file.id + "?alt=media";
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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

                filterAll();
            }
        });
    }

    private void shareFiles(final GoogleDriveFile file) {
        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        if (getActivity() instanceof ComposeActivity) {
            ((ComposeActivity) getActivity()).setDropBoxLink(file.alternateLink, file.title);
        }
    }

    private void addDivider() {
        mRecycleFiles.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity())
                .sizeResId(R.dimen.edge_tiny)
                .colorResId(R.color.gray_divider)
                .build());
    }

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (mDownloadReference == referenceId) {
                try {
                    ParcelFileDescriptor file = mDownloadManager.openDownloadedFile(mDownloadReference);
                    FileInputStream fileInputStream = new ParcelFileDescriptor.AutoCloseInputStream(file);

                    String fileName = mGoogleDriveFile.name;

                    UserHelper.copyInputStreamToFile(fileInputStream, fileName);
                } catch (java.io.IOException e) {
                    Log.e(PlanckMailApplication.TAG, e.toString());
                }
            }
        }
    };

    private void getGoogleDriveData(String folderName) {
        AccountType type = (AccountType) getArguments().getSerializable(BundleKeys.ACCOUNT_TYPE);
        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        AccountInfo accountInfo = manager.getAccountInfoByType(type);
        getActivity().setTitle(getString(R.string.googleDrive));
        ((MenuActivity)getActivity()).getToolbar().setSubtitle(accountInfo.email);
        getGoogleDriveMetaData(folderName, accountInfo);
    }

    private void getNewToken(final AccountInfo accountInfo) {
        RestClientGoogleDriveClient googleDriveClient = new RestClientGoogleDriveClient(RestClientGoogleDriveClient.BASE_URL1);
        GoogleDriveService oneDriveService = googleDriveClient.getGoogleDriveService();
        oneDriveService.refreshToken(getString(R.string.googleDriveClientId), getString(R.string.googleDriveClientSecret), accountInfo.getRefreshToken(), "refresh_token", new Callback<String>() {

            @Override
            public void success(String json, Response response) {
                OneDriveToken token = JsonUtilFactory.getJsonUtil().fromJson(json, OneDriveToken.class);
                setOneDriveAccountInfo(accountInfo, token);
                getGoogleDriveMetaData(ROOT_PATH, accountInfo);
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

        if (token.refresh_token != null)
            accountInfo.setRefreshToken(token.refresh_token);

        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        manager.createOrUpdateAccountInfo(accountInfo);
    }

    private void getGoogleDriveMetaData(final String folderId, final AccountInfo accountInfo) {
        String key1 = "q";
        String key2 = "orderBy";
        String key3 = "fields";

        HashMap<String, String> mapQuery = new HashMap<>();
        mapQuery.put(key1, "'" + folderId + "'" + " in parents");
        mapQuery.put(key2, "folder");
        mapQuery.put(key3, "files,nextPageToken,kind");

        GoogleDriveService googleDriveServer = AuthGoogleDriveApi.createService(GoogleDriveService.class, RestClientGoogleDriveClient.BASE_URL1, accountInfo.accessToken, "");
        googleDriveServer.getFilesList(mapQuery, new Callback<String>() {
            @Override
            public void success(String json, Response response) {
                mProgress.setVisibility(View.GONE);

                GoogleDriveListFiles fileList = JsonUtilFactory.getJsonUtil().fromJson(json, GoogleDriveListFiles.class);

                mListFiles = fileList.files;

                if (fileList.files.isEmpty()) {
                    mTvNoFiles.setVisibility(View.VISIBLE);
                } else {
                    if (folderId.equalsIgnoreCase(ROOT_PATH))
                        ROOT_FOLDER_ID = fileList.files.get(0).parents.get(0);
                    else
                        PARENT_PATH = fileList.files.get(0).parents.get(0);
                    mTvNoFiles.setVisibility(View.GONE);
                    setAccountSections(mListFiles);
                }
                mFileBaseRecycleAdapter.updateData(mListFiles);
            }

            @Override
            public void failure(RetrofitError error) {
                mProgress.setVisibility(View.GONE);

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

    private void setAccountSections(List<GoogleDriveFile> list) {
        List<SimpleSectionedRecyclerViewAdapter.Section> sections = new ArrayList<>();

        String files = getActivity().getResources().getString(R.string.files);
        String folders = getResources().getString(R.string.folders);
        boolean fileState = false;
        boolean emailState = false;

        for (int i = 0; i < list.size(); i++) {
            GoogleDriveFile file = list.get(i);
            String folderMimeType = "application/vnd.google-apps.folder";

            if (file.mimeType.equalsIgnoreCase(folderMimeType) && !emailState) {
                emailState = true;
                sections.add(new SimpleSectionedRecyclerViewAdapter.Section(i, folders));
            } else if (!file.mimeType.equals(folderMimeType) && !fileState) {
                fileState = true;
                sections.add(new SimpleSectionedRecyclerViewAdapter.Section(i, files));
            }
        }
        SimpleSectionedRecyclerViewAdapter.Section[] arraySections = new SimpleSectionedRecyclerViewAdapter.Section[sections.size()];
        mSectionedAdapter.setSections(sections.toArray(arraySections));
    }

    public void setAdapter() {
        mFileBaseRecycleAdapter = new BaseGoogleDriveAdapter(getActivity(), mListFiles);
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
        if (!mListFiles.isEmpty() && !mMapTreeFileId.isEmpty()) {
            String child = mListFiles.get(0).parents.get(0);
            String parentId = mMapTreeFileId.get(child);

            if (child.equalsIgnoreCase(ROOT_FOLDER_ID))
                getFragmentManager().popBackStackImmediate();
            else {
                getGoogleDriveData(parentId);
            }
        } else if (mGoogleDriveFile != null) {
            String parentId = mMapTreeFileId.get(mGoogleDriveFile.id);
            getGoogleDriveData(parentId);
        } else {
            getFragmentManager().popBackStackImmediate();
        }
    }

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

    private void filterAll() {
        if (PARENT_PATH != null)
            getGoogleDriveData(PARENT_PATH);
        else
            getGoogleDriveData(ROOT_PATH);
    }

    private void showFilterPopupMenu() {
        View view = ((MenuActivity) getActivity()).getToolbar();
        PopupMenu popupMenu = new PopupMenu(getActivity(), view, Gravity.END);
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
                List<GoogleDriveFile> listFiles = new ArrayList<>();

                for (GoogleDriveFile file : mListFiles) {
                    String fileFormat = MimeUtils.guessExtensionFromMimeType(file.mimeType);

                    if (fileFormat == null)
                        fileFormat = "";

                    if (fileFormat.equalsIgnoreCase("zip"))
                        listFiles.add(file);
                }
                mListFiles = listFiles;
                mSectionedAdapter.deleteAllSections();
                mFileBaseRecycleAdapter.updateData(listFiles);
                tvShowNoFileImage();
            }

            private void getPdf() {
                List<GoogleDriveFile> listFiles = new ArrayList<>();

                for (GoogleDriveFile file : mListFiles) {
                    String fileFormat = MimeUtils.guessExtensionFromMimeType(file.mimeType);

                    if (fileFormat == null)
                        fileFormat = "";

                    if (fileFormat.equalsIgnoreCase("pdf"))
                        listFiles.add(file);
                }
                mListFiles = listFiles;
                mSectionedAdapter.deleteAllSections();
                mFileBaseRecycleAdapter.updateData(listFiles);
                tvShowNoFileImage();
            }

            public void filterImage() {
                List<GoogleDriveFile> listFiles = new ArrayList<>();

                for (GoogleDriveFile file : mListFiles) {
                    String fileFormat = MimeUtils.guessExtensionFromMimeType(file.mimeType);

                    if (fileFormat == null)
                        fileFormat = "";

                    if (fileFormat.equalsIgnoreCase("png") || fileFormat.equalsIgnoreCase("jpeg") || fileFormat.equalsIgnoreCase("jpg"))
                        listFiles.add(file);
                }
                mListFiles = listFiles;
                mSectionedAdapter.deleteAllSections();
                mFileBaseRecycleAdapter.updateData(listFiles);
                tvShowNoFileImage();
            }

            private void getSlide() {
                List<GoogleDriveFile> listFiles = new ArrayList<>();

                for (GoogleDriveFile file : mListFiles) {
                    String fileFormat = MimeUtils.guessExtensionFromMimeType(file.mimeType);

                    if (fileFormat == null)
                        fileFormat = "";

                    if (fileFormat.equalsIgnoreCase("slide"))
                        listFiles.add(file);
                }
                mListFiles = listFiles;
                mSectionedAdapter.deleteAllSections();
                mFileBaseRecycleAdapter.updateData(listFiles);
                tvShowNoFileImage();
            }

            private void getDoc() {
                List<GoogleDriveFile> listFiles = new ArrayList<>();

                for (GoogleDriveFile file : mListFiles) {
                    String fileFormat = MimeUtils.guessExtensionFromMimeType(file.mimeType);

                    if (fileFormat == null)
                        fileFormat = "";

                    if (fileFormat.equalsIgnoreCase("doc") || fileFormat.equalsIgnoreCase("docx") || fileFormat.equalsIgnoreCase("docm"))
                        listFiles.add(file);
                }
                mListFiles = listFiles;
                mSectionedAdapter.deleteAllSections();
                mFileBaseRecycleAdapter.updateData(listFiles);
                tvShowNoFileImage();
            }
        });
        popupMenu.show();
    }

    private void tvShowNoFileImage() {
        if (mListFiles.isEmpty()) {
            mTvNoFiles.setVisibility(View.VISIBLE);
        } else {
            mTvNoFiles.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(final String newText) {
        if (TextUtils.isEmpty(newText))
            return false;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mProgress.setVisibility(View.VISIBLE);
                AccountType type = (AccountType) getArguments().getSerializable(BundleKeys.ACCOUNT_TYPE);
                AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
                AccountInfo accountInfo = manager.getAccountInfoByType(type);
                String key1 = "q";
                String value = "name=" + "'" + newText + "'";
                String key2 = "fields";
                String value2 = "files,kind,nextPageToken";

                HashMap<String, String> mapQuery = new HashMap<>();
                mapQuery.put(key1, value);
                mapQuery.put(key2, value2);

                GoogleDriveService googleDriveServer = AuthGoogleDriveApi.createService(GoogleDriveService.class, RestClientGoogleDriveClient.BASE_URL1, accountInfo.accessToken, "");
                googleDriveServer.getSearchFiles(mapQuery, new Callback<String>() {
                    @Override
                    public void success(String json, Response response) {
                        mProgress.setVisibility(View.GONE);

                        GoogleDriveListFiles fileList = JsonUtilFactory.getJsonUtil().fromJson(json, GoogleDriveListFiles.class);

                        mListFiles = fileList.files;

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
