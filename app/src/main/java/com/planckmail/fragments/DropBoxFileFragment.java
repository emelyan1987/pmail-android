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
import android.support.v7.widget.SearchView.OnQueryTextListener;
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
import com.planckmail.adapters.BaseFileDropBoxAdapter;
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
import com.planckmail.web.response.dropBox.DropBoxFile;
import com.planckmail.web.response.dropBox.DropBoxFileMedia;
import com.planckmail.web.response.dropBox.DropBoxGetFiles;
import com.planckmail.web.response.dropBox.DropBoxShareFile;
import com.planckmail.web.restClient.RestClientDropBoxClient;
import com.planckmail.web.restClient.api.AuthDropBoxApi;
import com.planckmail.web.restClient.service.DropBoxService;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static com.planckmail.fragments.BaseFragment.OnBackPressed;

/**
 * Created by Terry on 1/3/2016.
 */
public class DropBoxFileFragment extends BaseFragment implements OnBackPressed, OnQueryTextListener {
    public static final String ROOT_PATH = "/";

    private DownloadManager mDownloadManager;
    private long mDownloadReference;
    private boolean isFirstInit;
    private RecyclerView mRecycleFiles;
    private BaseFileDropBoxAdapter mFileBaseRecycleAdapter;
    private ProgressBar mProgress;
    private TextView mTvNoFiles;
    private List<DropBoxFile> mListFiles = new ArrayList<>();
    private SimpleSectionedRecyclerViewAdapter mSectionedAdapter;
    private String mPath;
    private DropBoxFile mDropBoxFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mDownloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        getActivity().registerReceiver(downloadReceiver, filter);
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
                getDropBoxFiles(mPath);
            }
        });
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_files, container, false);

        initViews(view);
        setAdapter();
        setRecycleSettings();
        getDropBoxFiles(ROOT_PATH);

        return view;
    }

    public void setRecycleSettings() {
        mRecycleFiles.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                position = mSectionedAdapter.sectionedPositionToPosition(position);
                DropBoxFile file = mDropBoxFile = mListFiles.get(position);
                if (file.is_dir) {
                    mProgress.setVisibility(View.GONE);
                    getDropBoxFiles(file.path);
                } else if (getActivity() instanceof MenuActivity) {
                    downloadFile(file);
                } else {
                    shareFiles(file);
                }

            }
        }));
        addDivider();
    }

    private void shareFiles(final DropBoxFile file) {
        AccountInfo accountInfo = getAccountInfo();
        DropBoxService dropBoxServer = AuthDropBoxApi.createService(DropBoxService.class, RestClientDropBoxClient.BASE_URL1, accountInfo.accessToken, "");
        dropBoxServer.shares(file.getPath(), new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                String json = (String) o;
                DropBoxShareFile shareFiles = JsonUtilFactory.getJsonUtil().fromJson(json, DropBoxShareFile.class);
                String url = shareFiles.url;

                getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                String[] split = file.getPath().split("/");
                if (getActivity() instanceof ComposeActivity) {
                    ((ComposeActivity) getActivity()).setDropBoxLink(url, split[split.length - 1]);
                }
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

                    String[] split = mDropBoxFile.getPath().split("/");
                    String fileName = split[split.length - 1];
                    UserHelper.copyInputStreamToFile(fileInputStream, fileName);

                } catch (java.io.IOException e) {
                    Log.e(PlanckMailApplication.TAG, e.toString());
                }
            }
        }
    };

    private void downloadFile(final DropBoxFile file) {
        final AccountInfo accountInfo = getAccountInfo();
        DropBoxService dropBoxServer = AuthDropBoxApi.createService(DropBoxService.class, RestClientDropBoxClient.BASE_URL1, accountInfo.accessToken, "");
        dropBoxServer.getFileMedia(file.getPath(), new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                String json = (String) o;

                DropBoxFileMedia media = JsonUtilFactory.getJsonUtil().fromJson(json, DropBoxFileMedia.class);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(media.url));
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                request.setTitle(getResources().getString(R.string.downloadingFile));
                String[] split = file.getPath().split("/");
                String fileName = split[split.length - 1];
                request.setDescription(fileName);

                request.setAllowedOverRoaming(false);
                request.setDestinationInExternalFilesDir(getActivity(), Environment.getExternalStorageState() + PlanckMailApplication.PLANK_MAIL_FILES, fileName);

                mDownloadReference = mDownloadManager.enqueue(request);
            }

            @Override
            public void failure(RetrofitError error) {
            }
        });
    }

    private void getDropBoxFiles(String path) {
        AccountInfo accountInfo = getAccountInfo();
        DropBoxService dropBoxServer = AuthDropBoxApi.createService(DropBoxService.class, RestClientDropBoxClient.BASE_URL1, accountInfo.accessToken, "");
        dropBoxServer.getFiles(path, new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                mRecycleFiles.setVisibility(View.VISIBLE);
                mProgress.setVisibility(View.GONE);
                String json = (String) o;
                DropBoxGetFiles fileList = JsonUtilFactory.getJsonUtil().fromJson(json, DropBoxGetFiles.class);

                Collections.sort(fileList.contents);
                mListFiles = fileList.contents;

                mFileBaseRecycleAdapter.updateData(fileList.contents);

                mPath = fileList.path;
                if (fileList.contents.isEmpty()) {
                    mTvNoFiles.setVisibility(View.VISIBLE);
                } else {
                    mTvNoFiles.setVisibility(View.GONE);
                    setAccountSections(fileList.contents);
                }
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

    private void tvShowNoFileImage() {
        if (mListFiles.isEmpty()) {
            mTvNoFiles.setVisibility(View.VISIBLE);
        } else {
            mTvNoFiles.setVisibility(View.GONE);
        }
    }

    private void setAccountSections(List<DropBoxFile> list) {
        List<SimpleSectionedRecyclerViewAdapter.Section> sections = new ArrayList<>();

        String files = getResources().getString(R.string.files);
        String folders = getResources().getString(R.string.folders);
        boolean fileState = false;
        boolean emailState = false;

        for (int i = 0; i < list.size(); i++) {
            DropBoxFile file = list.get(i);

            if (file.is_dir && !emailState) {
                emailState = true;
                sections.add(new SimpleSectionedRecyclerViewAdapter.Section(i, folders));
            } else if (!file.is_dir && !fileState) {
                fileState = true;
                sections.add(new SimpleSectionedRecyclerViewAdapter.Section(i, files));
            }
        }

        SimpleSectionedRecyclerViewAdapter.Section[] arraySections = new SimpleSectionedRecyclerViewAdapter.Section[sections.size()];
        mSectionedAdapter.setSections(sections.toArray(arraySections));
    }

    public void setAdapter() {
        AccountInfo accountInfo = getAccountInfo();
        getActivity().setTitle(getString(R.string.dropBox));
        ((MenuActivity)getActivity()).getToolbar().setSubtitle(accountInfo.email);

        mFileBaseRecycleAdapter = new BaseFileDropBoxAdapter(getActivity(), mListFiles, accountInfo);
        mSectionedAdapter = new SimpleSectionedRecyclerViewAdapter(getActivity(), R.layout.elem_follow_up_section, R.id.section_text, mFileBaseRecycleAdapter);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecycleFiles.setLayoutManager(mLayoutManager);
        mRecycleFiles.setHasFixedSize(true);
        mRecycleFiles.setAdapter(mSectionedAdapter);
    }

    private AccountInfo getAccountInfo() {
        AccountType type = (AccountType) getArguments().getSerializable(BundleKeys.ACCOUNT_TYPE);
        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        return manager.getAccountInfoByType(type);
    }

    private void initViews(View view) {
        mRecycleFiles = (RecyclerView) view.findViewById(R.id.recycleFile);
        mProgress = (ProgressBar) view.findViewById(R.id.progressLoadFile);
        mTvNoFiles = (TextView) view.findViewById(R.id.tvNoFiles);
    }

    @Override
    public void onBackPress() {
        if (mPath.equals(ROOT_PATH)) {
            getFragmentManager().popBackStackImmediate();
        } else {
            mProgress.setVisibility(View.VISIBLE);
            buildPath();
        }
    }

    private void buildPath() {
        StringBuilder pathBuilder;

        String[] split = mPath.split("/");
        if (split.length > 1) {
            pathBuilder = getStringBuilder(split, 1);
            mPath = pathBuilder.toString();
        } else {
            mPath = split[0];
        }
        getDropBoxFiles(mPath);
    }

    @NonNull
    private StringBuilder getStringBuilder(String[] split, int elem) {
        StringBuilder pathBuilder = new StringBuilder();
        for (int i = 0; i < split.length - elem; i++) {
            pathBuilder.append("/");
            pathBuilder.append(split[i]);
        }
        return pathBuilder;
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
                List<DropBoxFile> listFiles = new ArrayList<>();

                for (DropBoxFile file : mListFiles) {
                    String fileFormat = MimeUtils.guessExtensionFromMimeType(file.mime_type);

                    if (file.is_dir || fileFormat == null)
                        fileFormat = "";

                    if (fileFormat.equalsIgnoreCase("zip"))
                        listFiles.add(file);
                }
                mListFiles = listFiles;
                mFileBaseRecycleAdapter.updateData(listFiles);
                tvShowNoFileImage();
            }

            private void getPdf() {
                List<DropBoxFile> listFiles = new ArrayList<>();

                for (DropBoxFile file : mListFiles) {
                    String fileFormat = MimeUtils.guessExtensionFromMimeType(file.mime_type);

                    if (file.is_dir || fileFormat == null)
                        fileFormat = "";

                    if (fileFormat.equalsIgnoreCase("pdf"))
                        listFiles.add(file);
                }
                mListFiles = listFiles;
                mSectionedAdapter.deleteAllSections();
                mFileBaseRecycleAdapter.updateData(listFiles);
                tvShowNoFileImage();
            }

            public void filterAll() {
                getDropBoxFiles(ROOT_PATH);
            }

            public void filterImage() {
                List<DropBoxFile> listFiles = new ArrayList<>();

                for (DropBoxFile file : mListFiles) {
                    String fileFormat = MimeUtils.guessExtensionFromMimeType(file.mime_type);

                    if (file.is_dir || fileFormat == null)
                        fileFormat = "";

                    if (fileFormat.equalsIgnoreCase("png") || fileFormat.equalsIgnoreCase("jpeg") || fileFormat.equalsIgnoreCase("jpg"))
                        listFiles.add(file);
                }
                mListFiles = listFiles;
                mSectionedAdapter.deleteAllSections();
                mFileBaseRecycleAdapter.updateData(listFiles);
                tvShowNoFileImage();
            }

            public void getSlide() {
                List<DropBoxFile> listFiles = new ArrayList<>();

                for (DropBoxFile file : mListFiles) {
                    String fileFormat = MimeUtils.guessExtensionFromMimeType(file.mime_type);

                    if (file.is_dir || fileFormat == null)
                        fileFormat = "";

                    if (fileFormat.equalsIgnoreCase("slide"))
                        listFiles.add(file);
                }
                mListFiles = listFiles;
                mSectionedAdapter.deleteAllSections();
                mFileBaseRecycleAdapter.updateData(listFiles);
                tvShowNoFileImage();
            }

            public void getDoc() {
                List<DropBoxFile> listFiles = new ArrayList<>();

                for (DropBoxFile file : mListFiles) {
                    String fileFormat = MimeUtils.guessExtensionFromMimeType(file.mime_type);

                    if (file.is_dir || fileFormat == null)
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

    @Override
    public boolean onQueryTextChange(final String newText) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mRecycleFiles.setVisibility(View.GONE);
                mProgress.setVisibility(View.VISIBLE);

                AccountInfo accountInfo = getAccountInfo();
                DropBoxService dropBoxServer = AuthDropBoxApi.createService(DropBoxService.class, RestClientDropBoxClient.BASE_URL1, accountInfo.accessToken, "");
                dropBoxServer.searchFile(newText, new Callback<Object>() {
                    @Override
                    public void success(Object o, Response response) {
                        mRecycleFiles.setVisibility(View.VISIBLE);
                        mProgress.setVisibility(View.GONE);

                        String json = (String) o;
                        List<DropBoxFile> listFile = JsonUtilFactory.getJsonUtil().fromJsonArray(json, DropBoxFile.class);
                        mSectionedAdapter.deleteAllSections();
                        mFileBaseRecycleAdapter.updateData(listFile);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        mRecycleFiles.setVisibility(View.VISIBLE);
                        mProgress.setVisibility(View.GONE);

                        Response r = error.getResponse();
                        if (r != null)
                            Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                    }
                });
            }
        }, 600);
        return false;
    }
}
