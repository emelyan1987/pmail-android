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
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
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

import com.planckmail.R;
import com.planckmail.activities.ComposeActivity;
import com.planckmail.activities.MenuActivity;
import com.planckmail.adapters.BaseDriveBoxAdapter;
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
import com.planckmail.web.request.box.RequestBoxShareLink;
import com.planckmail.web.request.box.RequestInnerBoxShareLink;
import com.planckmail.web.response.boxDrive.BoxDriveFile;
import com.planckmail.web.response.boxDrive.BoxDriveFolder;
import com.planckmail.web.response.dropBox.DropBoxFile;
import com.planckmail.web.response.oneDrive.OneDriveFileValue;
import com.planckmail.web.response.oneDrive.OneDriveToken;
import com.planckmail.web.restClient.RestClientBoxDriveClient;
import com.planckmail.web.restClient.RestClientDropBoxClient;
import com.planckmail.web.restClient.RestClientOneDriveClient;
import com.planckmail.web.restClient.api.AutOneDriveApi;
import com.planckmail.web.restClient.api.AuthDropBoxApi;
import com.planckmail.web.restClient.service.BoxDriveService;
import com.planckmail.web.restClient.service.DropBoxService;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedString;

import static android.support.v7.widget.SearchView.*;

/**
 * Created by Terry on 1/26/2016.
 */
public class BoxDriveFileFragment extends BaseFragment implements OnBackPressed, OnQueryTextListener {
    public static final String FILE = "file";
    public static long ROOT_FOLDER_ID = 0;
    private static final int UN_AUTHORISE = 401;
    private RecyclerView mRecycleFiles;
    private BaseDriveBoxAdapter mFileBaseRecycleAdapter;
    private ProgressBar mProgress;
    private TextView mTvNoFiles;
    private List<BoxDriveFile> mListFiles = new ArrayList<>();
    private SimpleSectionedRecyclerViewAdapter mSectionedAdapter;
    private HashMap<Long, Long> mMapTreeFileId = new HashMap<>();
    private BoxDriveFile mBoxDriveFile;
    private int mBackCounter;
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
        getBoxDriveData(ROOT_FOLDER_ID);

        return view;
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

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (mDownloadReference == referenceId) {
                try {
                    ParcelFileDescriptor file = mDownloadManager.openDownloadedFile(mDownloadReference);
                    FileInputStream fileInputStream = new ParcelFileDescriptor.AutoCloseInputStream(file);

                    UserHelper.copyInputStreamToFile(fileInputStream, mBoxDriveFile.name);
                } catch (java.io.IOException e) {
                    Log.e(PlanckMailApplication.TAG, e.toString());
                }
            }
        }
    };


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
                loadFileList();
            }
        });
    }

    private void loadFileList() {
        if (mBoxDriveFile == null)
            getBoxDriveData(ROOT_FOLDER_ID);
        else
            getBoxDriveData(mBoxDriveFile.id);
    }

    public void setRecycleSettings() {
        mRecycleFiles.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                position = mSectionedAdapter.sectionedPositionToPosition(position);
                BoxDriveFile file = mBoxDriveFile = mListFiles.get(position);

                mMapTreeFileId.put(file.id, file.parent.id);

                if (file.type.equalsIgnoreCase(BaseDriveBoxAdapter.BOX_FILE_TYPE.FOLDER.toString())) {
                    mProgress.setVisibility(View.GONE);
                    getBoxDriveData(file.id);
                } else {
                    if (file.shared_link == null)
                        createShareFiles(file);
                    else if (getActivity() instanceof MenuActivity) {
                        downloadFile(file);
                    } else {
                        share(file);
                    }
                }
            }
        }));
        addDivider();
    }

    private void downloadFile(BoxDriveFile file) {
        AccountType type = (AccountType) getArguments().getSerializable(BundleKeys.ACCOUNT_TYPE);
        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        AccountInfo accountInfo = manager.getAccountInfoByType(type);

        String url = RestClientBoxDriveClient.BASE_URL1 + "/files/" + file.id + "/content";
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

    private void createShareFiles(final BoxDriveFile file) {
        AccountType type = (AccountType) getArguments().getSerializable(BundleKeys.ACCOUNT_TYPE);
        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        AccountInfo accountInfo = manager.getAccountInfoByType(type);
        RequestBoxShareLink boxSharedLink = new RequestBoxShareLink();
        boxSharedLink.shared_link = new RequestInnerBoxShareLink();

        String jsonSharedBoxFile = JsonUtilFactory.getJsonUtil().toJson(boxSharedLink);
        TypedString body = new TypedString(jsonSharedBoxFile);

        BoxDriveService dropBoxServer = AutOneDriveApi.createService(BoxDriveService.class, RestClientBoxDriveClient.BASE_URL1, accountInfo.accessToken, "");
        dropBoxServer.getShareLink(file.id, body, new Callback<String>() {
            @Override
            public void success(String json, Response response) {
                mProgress.setVisibility(View.GONE);

                BoxDriveFile fileShareLink = JsonUtilFactory.getJsonUtil().fromJson(json, BoxDriveFile.class);
                share(fileShareLink);
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });
    }

    private void share(BoxDriveFile fileShareLink) {
        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        if (getActivity() instanceof ComposeActivity) {
            ((ComposeActivity) getActivity()).setDropBoxLink(fileShareLink.shared_link.url, fileShareLink.name);
        }
    }

    private void addDivider() {
        mRecycleFiles.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity())
                .sizeResId(R.dimen.edge_tiny)
                .colorResId(R.color.gray_divider)
                .build());
    }

    private void getBoxDriveData(long folderName) {
        AccountType type = (AccountType) getArguments().getSerializable(BundleKeys.ACCOUNT_TYPE);
        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        AccountInfo accountInfo = manager.getAccountInfoByType(type);
        getActivity().setTitle(getString(R.string.box));
        ((MenuActivity)getActivity()).getToolbar().setSubtitle(accountInfo.email);
        getBoxDriveMetaData(folderName, accountInfo);
    }

    private void getNewToken(final AccountInfo accountInfo) {
        RestClientBoxDriveClient oneDriveApi = new RestClientBoxDriveClient(RestClientBoxDriveClient.BASE_URL);
        BoxDriveService oneDriveService = oneDriveApi.getBoxDriveService();
        oneDriveService.refreshToken(getString(R.string.boxAppKey), getString(R.string.boxClientSecret),
                accountInfo.getRefreshToken(), "refresh_token", new Callback<String>() {

                    @Override
                    public void success(String json, Response response) {
                        OneDriveToken token = JsonUtilFactory.getJsonUtil().fromJson(json, OneDriveToken.class);
                        setOneDriveAccountInfo(accountInfo, token);
                        getBoxDriveMetaData(ROOT_FOLDER_ID, accountInfo);
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

    private void getBoxDriveMetaData(long folderId, final AccountInfo accountInfo) {
        Map<String, String> mapFields = new HashMap<>();
        mapFields.put("fields", "size,name,modified_at,shared_link,id,parent,created_by");

        BoxDriveService dropBoxServer = AutOneDriveApi.createService(BoxDriveService.class, RestClientBoxDriveClient.BASE_URL1, accountInfo.accessToken, "");
        dropBoxServer.getFolderList(folderId, mapFields, new Callback<String>() {
            @Override
            public void success(String json, Response response) {
                mProgress.setVisibility(View.GONE);

                BoxDriveFolder fileList = JsonUtilFactory.getJsonUtil().fromJson(json, BoxDriveFolder.class);

                mListFiles = fileList.entries;
                mFileBaseRecycleAdapter.updateData(mListFiles);

                if (fileList.entries.isEmpty()) {
                    mTvNoFiles.setVisibility(View.VISIBLE);
                } else {
                    mTvNoFiles.setVisibility(View.GONE);
                    setAccountSections(fileList.entries);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                mProgress.setVisibility(View.GONE);

                Response r = error.getResponse();
                if (r != null && r.getStatus() == UN_AUTHORISE)
                    getNewToken(accountInfo);
                else if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });
    }

    private void setAccountSections(List<BoxDriveFile> list) {
        List<SimpleSectionedRecyclerViewAdapter.Section> sections = new ArrayList<>();

        String files = getResources().getString(R.string.files);
        String folders = getResources().getString(R.string.folders);
        boolean fileState = false;
        boolean emailState = false;

        for (int i = 0; i < list.size(); i++) {
            BoxDriveFile file = list.get(i);

            if (file.type.equalsIgnoreCase(BaseDriveBoxAdapter.BOX_FILE_TYPE.FOLDER.toString()) && !emailState) {
                emailState = true;
                sections.add(new SimpleSectionedRecyclerViewAdapter.Section(i, folders));
            } else if (file.type.equalsIgnoreCase(BaseDriveBoxAdapter.BOX_FILE_TYPE.FILE.toString()) && !fileState) {
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

        mFileBaseRecycleAdapter = new BaseDriveBoxAdapter(getActivity(), mListFiles, accountInfo);
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
            long parent = mListFiles.get(0).parent.id;

            long parentId = 0;
            if (mMapTreeFileId.containsKey(parent))
                parentId = mMapTreeFileId.get(parent);

            if (parentId == 0)
                mBackCounter++;

            int countBackOnRootFolderClick = 2;
            if (parentId == ROOT_FOLDER_ID && mBackCounter >= countBackOnRootFolderClick)
                getFragmentManager().popBackStackImmediate();
            else {
                getBoxDriveData(parentId);
            }
        } else if (mBoxDriveFile != null) {
            long childId = mMapTreeFileId.get(mBoxDriveFile.id);
            getBoxDriveData(childId);
        } else {
            getFragmentManager().popBackStackImmediate();
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
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
                                                     List<BoxDriveFile> listFiles = new ArrayList<>();

                                                     for (BoxDriveFile file : mListFiles) {
                                                         if (file.type.equalsIgnoreCase(BaseDriveBoxAdapter.BOX_FILE_TYPE.FILE.toString()) && file.name.contains("zip")) {
                                                             listFiles.add(file);
                                                         }
                                                         mListFiles = listFiles;
                                                         mFileBaseRecycleAdapter.updateData(listFiles);
                                                         showTvNoImage();
                                                     }
                                                 }

                                                 private void getPdf() {
                                                     List<BoxDriveFile> listFiles = new ArrayList<>();

                                                     for (BoxDriveFile file : mListFiles) {
                                                         if (file.name.equalsIgnoreCase(BaseDriveBoxAdapter.BOX_FILE_TYPE.FILE.toString()) && file.name.contains("pdf")) {

                                                             listFiles.add(file);
                                                         }
                                                         mListFiles = listFiles;
                                                         mSectionedAdapter.deleteAllSections();
                                                         mFileBaseRecycleAdapter.updateData(listFiles);
                                                         showTvNoImage();
                                                     }
                                                 }

                                                 public void filterAll() {
                                                     loadFileList();
                                                 }

                                                 public void filterImage() {
                                                     List<BoxDriveFile> listFiles = new ArrayList<>();

                                                     for (BoxDriveFile file : mListFiles) {
                                                         if (file.type.equalsIgnoreCase(BaseDriveBoxAdapter.BOX_FILE_TYPE.FILE.toString())) {
                                                             if (file.name.contains("png") || file.name.contains("jpg") || file.name.contains("jpeg")) {
                                                                 listFiles.add(file);
                                                             }
                                                         }
                                                         mListFiles = listFiles;
                                                         mSectionedAdapter.deleteAllSections();
                                                         mFileBaseRecycleAdapter.updateData(listFiles);
                                                         showTvNoImage();
                                                     }
                                                 }

                                                 public void getSlide() {
                                                     List<BoxDriveFile> listFiles = new ArrayList<>();

                                                     for (BoxDriveFile file : mListFiles) {
                                                         if (file.type.equalsIgnoreCase(BaseDriveBoxAdapter.BOX_FILE_TYPE.FILE.toString()) && file.name.contains("slide")) {
                                                             listFiles.add(file);
                                                         }
                                                         mListFiles = listFiles;
                                                         mSectionedAdapter.deleteAllSections();
                                                         mFileBaseRecycleAdapter.updateData(listFiles);
                                                         showTvNoImage();
                                                     }
                                                 }

                                                 public void getDoc() {
                                                     List<BoxDriveFile> listFiles = new ArrayList<>();

                                                     for (BoxDriveFile file : mListFiles) {
                                                         if (file.type.equalsIgnoreCase(BaseDriveBoxAdapter.BOX_FILE_TYPE.FILE.toString())) {

                                                             if (file.name.contains("doc") || file.name.contains("docx") || file.name.contains("docm")) {
                                                                 listFiles.add(file);
                                                             }

                                                             mSectionedAdapter.deleteAllSections();
                                                             mFileBaseRecycleAdapter.updateData(listFiles);
                                                             showTvNoImage();
                                                         }
                                                     }
                                                 }
                                             }
        );
        popupMenu.show();
    }

    private void showTvNoImage() {
        if (mListFiles.isEmpty()) {
            mTvNoFiles.setVisibility(View.VISIBLE);
        } else
            mTvNoFiles.setVisibility(View.GONE);
    }

    @Override
    public boolean onQueryTextChange(final String newText) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mProgress.setVisibility(View.VISIBLE);
                Map<String, String> map = new HashMap<>();
                map.put("query", newText);

                AccountType type = (AccountType) getArguments().getSerializable(BundleKeys.ACCOUNT_TYPE);
                AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
                AccountInfo accountInfo = manager.getAccountInfoByType(type);
                BoxDriveService dropBoxServer = AutOneDriveApi.createService(BoxDriveService.class, RestClientBoxDriveClient.BASE_URL1, accountInfo.accessToken, "");
                dropBoxServer.searchList(map, new Callback<Object>() {
                    @Override
                    public void success(Object o, Response response) {
                        String json = (String)o;
                        mProgress.setVisibility(View.GONE);

                        BoxDriveFolder fileList = JsonUtilFactory.getJsonUtil().fromJson(json, BoxDriveFolder.class);

                        mListFiles = fileList.entries;

                        if (fileList.entries.isEmpty()) {
                            mTvNoFiles.setVisibility(View.VISIBLE);
                        } else {
                            mTvNoFiles.setVisibility(View.GONE);
                            setAccountSections(mListFiles);
                        }
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
