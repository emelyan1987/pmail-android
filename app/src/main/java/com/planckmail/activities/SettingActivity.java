package com.planckmail.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.planckmail.R;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.PlanckMailSharePreferences;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.db.beans.MessageDB;
import com.planckmail.data.db.beans.ParticipantDB;
import com.planckmail.data.db.beans.ThreadDB;
import com.planckmail.data.db.manager.AccountInfoDataManager;
import com.planckmail.data.db.manager.DataBaseManager;
import com.planckmail.enums.AccountType;
import com.planckmail.fragments.AccountInfoFragment;
import com.planckmail.helper.UserHelper;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.request.nylas.CreateFolder;
import com.planckmail.web.response.nylas.NameSpace;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.RestPlankMail;
import com.planckmail.web.restClient.TypedJsonString;
import com.planckmail.web.restClient.service.NylasService;
import com.planckmail.web.restClient.service.PlanckService;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedInput;

/**
 * Created by Taras Matolinets on 08.05.15.
 */
public class SettingActivity extends BaseActivity implements View.OnClickListener {
    private static final String FOLLOW_UP = "FollowUp";

    private WebView mWebAddTypeAccount;
    private String mToken;
    private LinearLayout mLayoutScroll;
    private ProgressBar mProgress;
    private ImageView mIvLogo;
    private FloatingActionButton mAddAccount;
    private FloatingActionButton mAddFileAccount;
    private FloatingActionMenu mFabMenu;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setting);

        initViews();
        setListeners();
        setActionBar();
        //update setting layout
        createSettingLayout();
        setWebSetting();
        setWebClient();
    }

    private void setListeners() {
        mAddAccount.setOnClickListener(this);
        mAddFileAccount.setOnClickListener(this);
    }

    private void setActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.action_settings));
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void createSettingLayout() {
        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        List<AccountInfo> accountInfoList = manager.getAllAccountInfoList();

        String version = getVersionName();

        addAccountToLayout(version, R.layout.elem_setting, R.id.tvContent);
        addAccountToLayout(getResources().getString(R.string.account_settings), R.layout.elem_setting_header, R.id.tvName);

        for (AccountInfo a : accountInfoList) {
            addEmail(a);
        }
        addAccountToLayout(getResources().getString(R.string.defaultSettings), R.layout.elem_setting_header, R.id.tvName);

        addAccountToLayout(getResources().getString(R.string.calendar), R.layout.elem_setting, R.id.tvContent);
        addAccountToLayout(getResources().getString(R.string.mail), R.layout.elem_setting, R.id.tvContent);
        addAccountToLayout(getResources().getString(R.string.signature), R.layout.elem_setting, R.id.tvContent);
        addAccountToLayout(getResources().getString(R.string.swipe_options), R.layout.elem_setting, R.id.tvContent);
        addAccountToLayout(getResources().getString(R.string.sharePlanckMail), R.layout.elem_setting, R.id.tvContent);
    }

    private String getVersionName() {
        String version = "";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
        return version;
    }

    private void addEmail(AccountInfo a) {
        View view = View.inflate(this, R.layout.elem_select_account, null);
        TextView addAccount = (TextView) view.findViewById(R.id.tvAddMail);
        Drawable drawable = UserHelper.getAccountDrawable(this, a.getAccountType());
        addAccount.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        addAccount.setOnClickListener(this);
        addAccount.setText(a.getEmail());
        addAccount.setTag(a);

        mLayoutScroll.addView(view);
    }

    private void addAccountToLayout(String text, int viewId, int tvView) {
        View view = View.inflate(this, viewId, null);
        TextView settingOption = (TextView) view.findViewById(tvView);
        settingOption.setOnClickListener(this);
        settingOption.setText(text);

        mLayoutScroll.addView(view);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setWebSetting() {
        mWebAddTypeAccount.getSettings().setJavaScriptEnabled(true);
    }

    private void setWebClient() {
        mWebAddTypeAccount.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mProgress.setVisibility(View.GONE);
                mIvLogo.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                getAccessToken(view, url);
                return true;
            }

            @Override
            public void onReceivedHttpAuthRequest(WebView view, @NonNull HttpAuthHandler handler, String host, String realm) {
                getAccountInfo();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getAccountInfo() {
        NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, mToken, "");
        nylasServer.getAccounts(new Callback<Object>() {
            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }

            @Override
            public void success(Object obj, Response response) {
                String json = (String) obj;

                Toast.makeText(SettingActivity.this, R.string.successful_login, Toast.LENGTH_SHORT).show();

                Log.i(PlanckMailApplication.TAG, "response: " + json);

                final NameSpace nameSpace = JsonUtilFactory.getJsonUtil().fromJson(json, NameSpace.class);

                createFolder();
                AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
                if (!manager.ifAccountExist(nameSpace.email_address)) {
                    createNewEmailAccount(nameSpace);

                    PlanckMailApplication app = (PlanckMailApplication) getApplicationContext();
                    app.setLoadDB(false);

                    storeToken(nameSpace);
                    addUserToPriorityList(nameSpace);

                    //clean db for avoid duplications element
                    cleanDb();
                    PlanckMailSharePreferences.setDataToSharePreferences(BundleKeys.SAVE_DATA_IN_DB, true, PlanckMailSharePreferences.SHARE_PREFERENCES_TYPE.BOOLEAN);
                }
                finish();
            }
        });
    }

    private void storeToken(NameSpace nameSpace) {
        RestPlankMail client = new RestPlankMail(RestPlankMail.BASE_URL2);
        PlanckService service = client.getPlankService();
        service.storeToken(nameSpace.email_address, mToken, "planck_test", new Callback<String>() {
            @Override
            public void success(String s, Response response) {
                Log.i(PlanckMailApplication.TAG, "stored token successfully");
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error store token: " + r.getReason());
            }
        });
    }

    private void addUserToPriorityList(final NameSpace nameSpace) {
        RestPlankMail client = new RestPlankMail(RestPlankMail.BASE_URL2);
        PlanckService service = client.getPlankService();
        service.addUserToPriorityToken(nameSpace.email_address, mToken, new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                Log.i(PlanckMailApplication.TAG, "user was added to priority list successfully");
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });
    }


    public void cleanDb() {
        DataBaseManager.getInstanceDataManager().cleanTable(ThreadDB.class);
        DataBaseManager.getInstanceDataManager().cleanTable(MessageDB.class);
        DataBaseManager.getInstanceDataManager().cleanTable(ParticipantDB.class);
    }

    private void getAccessToken(WebView view, String url) {
        if (url.contains("access_token")) {
            try {
                Map<String, String> map = UserHelper.splitQuery(new URL(url));
                mToken = map.get("access_token");
            } catch (UnsupportedEncodingException | MalformedURLException e) {
                Log.e(PlanckMailApplication.TAG, e.toString());
            }
            Toast.makeText(this, R.string.act_setting_access_token, Toast.LENGTH_SHORT).show();
        }
        view.loadUrl(url);
    }

    private void createNewEmailAccount(NameSpace nameSpace) {
        AccountInfo account = new AccountInfo();

        AccountType accountType = UserHelper.getEmailAccountType(nameSpace.email_address);

        account.setAccountId(nameSpace.account_id);
        account.setEmail(nameSpace.email_address);
        account.setName(nameSpace.name);
        account.setAccountType(accountType);
        account.setAccessToken(mToken);
        account.setObject(nameSpace.object);
        account.setProvider(nameSpace.provider);
        account.setIsEmailAccount(true);
        int color = UserHelper.getLightColor();
        account.setCalendarColor(color);

        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        manager.createOrUpdateAccountInfo(account);
    }


    private void initViews() {
        mWebAddTypeAccount = (WebView) findViewById(R.id.webAddAccount);
        mLayoutScroll = (LinearLayout) findViewById(R.id.llScroll);
        mProgress = (ProgressBar) findViewById(R.id.prLoadSetting);
        mIvLogo = (ImageView) findViewById(R.id.ivLogo);
        mAddAccount = (FloatingActionButton) findViewById(R.id.abAddEmailAccount);
        mAddFileAccount = (FloatingActionButton) findViewById(R.id.abAddFileAccount);
        mFabMenu = (FloatingActionMenu) findViewById(R.id.menuAccountsAction);
    }

    private void callAuthorization() {
        //token ,email required params for authorization. Look(https://www.nylas.com/docs/knowledgebase#client-side-implicit-flow)
        RestWebClient service = new RestWebClient();
        service.getNylasService().authorization(getString(R.string.app_id), "token", "email", getString(R.string.nylasRedirectUri), new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                String url = response.getUrl();
                mWebAddTypeAccount.loadUrl(url);
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: callAuthorization" + r.getReason());
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.abAddEmailAccount:
                mProgress.setVisibility(View.VISIBLE);
                mIvLogo.setVisibility(View.VISIBLE);
                callAuthorization();
                mWebAddTypeAccount.setVisibility(View.VISIBLE);
                mFabMenu.close(true);
                break;
            case R.id.abAddFileAccount:
                Intent intent = new Intent(this, SelectFileAccountActivity.class);
                startActivity(intent);
                break;
            case R.id.tvAddMail:
                AccountInfo accountInfo = (AccountInfo) v.getTag();

                Bundle bundle = new Bundle();
                bundle.putString(BundleKeys.KEY_ACCOUNT_ID, accountInfo.getAccountId());
                replace(AccountInfoFragment.class, R.id.flMainSetting, bundle, true);
                break;
        }
    }

    private void createFolder() {
        String json = createRequestBody();
        TypedInput in = new TypedJsonString(json);

        NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, mToken, "");
        nylasServer.createFolder(in, new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                Log.i(PlanckMailApplication.TAG, "folder created");
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error folder: " + r.getReason());
            }
        });
    }

    private String createRequestBody() {
        CreateFolder folder = new CreateFolder();
        folder.display_name = FOLLOW_UP;
        return JsonUtilFactory.getJsonUtil().toJson(folder);
    }
}
