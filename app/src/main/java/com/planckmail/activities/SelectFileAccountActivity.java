package com.planckmail.activities;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.planckmail.R;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.db.manager.AccountInfoDataManager;
import com.planckmail.data.db.manager.DataBaseManager;
import com.planckmail.enums.AccountType;
import com.planckmail.fragments.DropBoxFileFragment;
import com.planckmail.fragments.GoogleDriveFileFragment;
import com.planckmail.fragments.OneDriveFileFragment;
import com.planckmail.helper.UserHelper;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.response.boxDrive.BoxDriveServerToken;
import com.planckmail.web.response.boxDrive.BoxDriveUser;
import com.planckmail.web.response.dropBox.DropBoxAccount;
import com.planckmail.web.response.googleDrive.GoogleDriveAbout;
import com.planckmail.web.response.googleDrive.GoogleDriveAccessToken;
import com.planckmail.web.response.oneDrive.OneDriveToken;
import com.planckmail.web.response.oneDrive.OneDriveUserInfo;
import com.planckmail.web.restClient.RestClientBoxDriveClient;
import com.planckmail.web.restClient.RestClientDropBoxClient;
import com.planckmail.web.restClient.RestClientGoogleDriveClient;
import com.planckmail.web.restClient.api.AutOneDriveApi;
import com.planckmail.web.restClient.RestClientOneDriveClient;
import com.planckmail.web.restClient.api.AuthDropBoxApi;
import com.planckmail.web.restClient.api.AuthGoogleDriveApi;
import com.planckmail.web.restClient.service.BoxDriveService;
import com.planckmail.web.restClient.service.DropBoxService;
import com.planckmail.web.restClient.service.GoogleDriveService;
import com.planckmail.web.restClient.service.OneDriveService;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Terry on 12/24/2015.
 */
public class SelectFileAccountActivity extends BaseActivity implements View.OnClickListener {
    private TextView mTvDropBox;
    private TextView mTvBox;
    private TextView mTvOneDrive;
    private TextView mTvGoogleDrive;
    private WebView mWebFileAccount;
    private LinearLayout mLayoutFileAccount;
    private ProgressBar mProgress;
    private FileAccountType mType = null;
    private String mToken;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_account);
        initViews();
        initListeners();
        setWebSetting();
        setWebClient();
    }

    private void initViews() {
        mTvDropBox = (TextView) findViewById(R.id.tvDropBox);
        mTvOneDrive = (TextView) findViewById(R.id.tvOneDrive);
        mTvBox = (TextView) findViewById(R.id.tvBox);
        mTvGoogleDrive = (TextView) findViewById(R.id.tvGoogleDrive);
        mWebFileAccount = (WebView) findViewById(R.id.wbLoadFileAccounts);
        mProgress = (ProgressBar) findViewById(R.id.prLoadFileAccount);
        mLayoutFileAccount = (LinearLayout) findViewById(R.id.llFilesAccount);
    }

    private void initListeners() {
        mTvBox.setOnClickListener(this);
        mTvDropBox.setOnClickListener(this);
        mTvOneDrive.setOnClickListener(this);
        mTvGoogleDrive.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvDropBox:
                mType = FileAccountType.DROP_BOX;
                clearCookie();
                showProgress();
                setupDropBox();
                break;
            case R.id.tvBox:
                mType = FileAccountType.BOX;
                clearCookie();
                showProgress();
                setupBoxDrive();
                break;
            case R.id.tvOneDrive:
                mType = FileAccountType.ONE_DRIVE;
                clearCookie();
                showProgress();
                setupOneDrive();
                break;
            case R.id.tvGoogleDrive:
                mType = FileAccountType.GOOGLE_DRIVE;
                clearCookie();
                showProgress();
                setUpGoogleDrive();
                break;
        }
    }

    private void setupBoxDrive() {
        String state = getString(R.string.boxRedirectUri);
        RestClientBoxDriveClient boxDriveClient = new RestClientBoxDriveClient(RestClientBoxDriveClient.BASE_URL);
        BoxDriveService service = boxDriveClient.getBoxDriveService();
        service.authorization(getString(R.string.boxAppKey), "code", getString(R.string.boxRedirectUri), "PlanckLabsState", new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                String url = response.getUrl();
                mWebFileAccount.loadUrl(url);
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });
    }

    private void showProgress() {
        mLayoutFileAccount.setVisibility(View.GONE);
        mProgress.setVisibility(View.VISIBLE);
    }

    private void setUpGoogleDrive() {
        String scope = "https://www.googleapis.com/auth/drive";
        String accessType = "offline";
        String consent = "consent";
        RestClientGoogleDriveClient googleDriveClient = new RestClientGoogleDriveClient(RestClientGoogleDriveClient.BASE_URL);
        GoogleDriveService service = googleDriveClient.getGoogleDriveService();
        service.authorization(getString(R.string.googleDriveClientId), "code", getString(R.string.googleDriveRedirectUri), scope, accessType, consent, new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                String url = response.getUrl();
                mWebFileAccount.loadUrl(url);
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });
    }

    private void setupOneDrive() {
        //https://dev.onedrive.com/auth/msa_oauth.htm#authentication-scopes
        String scope = "onedrive.readonly" + " " + "wl.offline_access";
        RestClientOneDriveClient oneDriveApi = RestClientOneDriveClient.getInstance();
        OneDriveService oneDriveService = oneDriveApi.getOneDriveService();
        oneDriveService.authorization(getString(R.string.oneDriveAppKey), scope, "code", getString(R.string.oneDriveRedirectUri), new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                String url = response.getUrl();
                mWebFileAccount.loadUrl(url);
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setWebSetting() {
        mWebFileAccount.getSettings().setJavaScriptEnabled(true);
    }

    private void setWebClient() {
        mWebFileAccount.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mProgress.setVisibility(View.GONE);
                mWebFileAccount.setVisibility(View.VISIBLE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                switch (mType) {
                    case DROP_BOX:
                        getCodeDropBoxFileAccount(view, url);
                        break;
                    case ONE_DRIVE:
                    case GOOGLE_DRIVE:
                    case BOX:
                        getCodeAccount(view, url);
                        break;
                }
                return true;
            }

            @Override
            public void onReceivedHttpAuthRequest(WebView view, @NonNull HttpAuthHandler handler, String host, String realm) {
            }
        });
    }

    private void getGoogleDriveToken(String code) {
        String grantType = "authorization_code";
        RestClientGoogleDriveClient googleDriveClient = new RestClientGoogleDriveClient(RestClientGoogleDriveClient.BASE_URL1);
        GoogleDriveService service = googleDriveClient.getGoogleDriveService();
        service.getAccessToken(code, getString(R.string.googleDriveClientId), getString(R.string.googleDriveClientSecret), getString(R.string.googleDriveRedirectUri), grantType, new Callback<String>() {
            @Override
            public void success(String json, Response response) {
                GoogleDriveAccessToken token = JsonUtilFactory.getJsonUtil().fromJson(json, GoogleDriveAccessToken.class);
                getGoogleDriveAccountInfo(token);
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });
    }

    private void getGoogleDriveAccountInfo(final GoogleDriveAccessToken token) {
        GoogleDriveService dropBoxServer = AuthGoogleDriveApi.createService(GoogleDriveService.class, RestClientGoogleDriveClient.BASE_URL1, token.access_token, "");
        dropBoxServer.about(new Callback<String>() {
            @Override
            public void success(String json, Response response) {
                GoogleDriveAbout about = JsonUtilFactory.getJsonUtil().fromJson(json, GoogleDriveAbout.class);

                AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
                if (!manager.ifAccountExist(about.name))
                    setGoogleDriveAccountInfo(about, token);

                Intent intent = new Intent(SelectFileAccountActivity.this, MenuActivity.class);
                intent.putExtra(BundleKeys.LOAD_FILE_SECTION, true);
                startActivity(intent);
                Toast.makeText(SelectFileAccountActivity.this, R.string.act_setting_access_token, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });
    }

    private void setGoogleDriveAccountInfo(GoogleDriveAbout about, GoogleDriveAccessToken token) {
        AccountInfo accountInfo = new AccountInfo();
        AccountType accountType = UserHelper.getFileAccountType(mType);

        accountInfo.setAccessToken(token.access_token);

        if (token.refresh_token != null)
            accountInfo.setRefreshToken(token.refresh_token);
        accountInfo.setAccountId(about.user.permissionId);
        accountInfo.setAccountType(accountType);
        accountInfo.setEmail(about.user.emailAddress);
        accountInfo.setName(about.name);

        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        manager.createOrUpdateAccountInfo(accountInfo);
    }

    private void getCodeAccount(WebView view, String url) {
        String accessToken = "code=";
        int start = url.indexOf(accessToken);
        if (start > -1) {
            String code = url.substring(start + accessToken.length(), url.length());
            mWebFileAccount.setVisibility(View.GONE);
            mProgress.setVisibility(View.VISIBLE);
            switch (mType) {
                case ONE_DRIVE:
                    getOneDriveToken(code);
                    break;
                case GOOGLE_DRIVE:
                    getGoogleDriveToken(code);
                    break;
                case BOX:
                    getBoxDriveToken(code);
                    break;
            }
        } else
            view.loadUrl(url);
    }

    private void getBoxDriveToken(String code) {
        RestClientBoxDriveClient boxDriveClient = new RestClientBoxDriveClient(RestClientBoxDriveClient.BASE_URL);
        BoxDriveService service = boxDriveClient.getBoxDriveService();
        service.getAccessToken(code, getString(R.string.boxAppKey), getString(R.string.boxClientSecret),
                getString(R.string.boxRedirectUri), "authorization_code", new Callback<String>() {

                    @Override
                    public void success(String json, Response response) {
                        BoxDriveServerToken token = JsonUtilFactory.getJsonUtil().fromJson(json, BoxDriveServerToken.class);

                        setBoxDriveAccountInfo(token);
                        Toast.makeText(SelectFileAccountActivity.this, R.string.act_setting_access_token, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Response r = error.getResponse();
                        if (r != null)
                            Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                    }
                });
    }

    private void setBoxDriveAccountInfo(final BoxDriveServerToken token) {
        BoxDriveService dropBoxServer = AuthGoogleDriveApi.createService(BoxDriveService.class, RestClientBoxDriveClient.BASE_URL1, token.access_token, "");
        dropBoxServer.getCurrentUser(new Callback<String>() {
            @Override
            public void success(String json, Response response) {
                BoxDriveUser user = JsonUtilFactory.getJsonUtil().fromJson(json, BoxDriveUser.class);

                AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);

                if (!manager.ifAccountExist(user.name))
                    saveBoxDriveAccount(token, user);

                Intent intent = new Intent(SelectFileAccountActivity.this, MenuActivity.class);
                intent.putExtra(BundleKeys.LOAD_FILE_SECTION, true);
                startActivity(intent);
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });

    }

    private void saveBoxDriveAccount(BoxDriveServerToken token, BoxDriveUser user) {
        AccountInfo accountInfo = new AccountInfo();
        AccountType accountType = UserHelper.getFileAccountType(mType);

        accountInfo.setAccessToken(token.access_token);
        accountInfo.setRefreshToken(token.refresh_token);
        accountInfo.setAccountId(user.id);
        accountInfo.setAccountType(accountType);
        accountInfo.setEmail(user.login);
        accountInfo.setName(user.name);

        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        manager.createOrUpdateAccountInfo(accountInfo);
    }

    private void getOneDriveToken(String code) {
        RestClientOneDriveClient oneDriveApi = RestClientOneDriveClient.getInstance();
        OneDriveService oneDriveService = oneDriveApi.getOneDriveService();
        oneDriveService.authorization(getString(R.string.oneDriveAppKey),
                getString(R.string.oneDriveRedirectUri), getString(R.string.oneDriveClientSecret), code, "authorization_code", new Callback<Object>() {

                    @Override
                    public void success(Object o, Response response) {
                        String json = (String) o;
                        OneDriveToken token = JsonUtilFactory.getJsonUtil().fromJson(json, OneDriveToken.class);
                        getOneDriveAccountInfo(token);
                        Toast.makeText(SelectFileAccountActivity.this, R.string.act_setting_access_token, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Response r = error.getResponse();
                        if (r != null)
                            Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                    }
                });
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getFragmentManager().findFragmentById(android.R.id.content);

        if (fragment instanceof DropBoxFileFragment)
            ((DropBoxFileFragment) fragment).onBackPress();
        else if (fragment instanceof OneDriveFileFragment)
            ((OneDriveFileFragment) fragment).onBackPress();
        else if (fragment instanceof GoogleDriveFileFragment)
            ((GoogleDriveFileFragment) fragment).onBackPress();
        else
            super.onBackPressed();
    }

    private void getCodeDropBoxFileAccount(WebView view, String url) {
        String accessToken = "#access_token=";
        String tokenType = "&token_type";

        int start = url.indexOf(accessToken);
        int end = url.indexOf(tokenType);

        if (start > -1) {
            mToken = url.substring(start + accessToken.length(), end);
            mWebFileAccount.setVisibility(View.GONE);
            mProgress.setVisibility(View.VISIBLE);
            getDropBoxAccountInfo();
            Toast.makeText(this, R.string.act_setting_access_token, Toast.LENGTH_SHORT).show();
        } else
            view.loadUrl(url);
    }

    private void getOneDriveAccountInfo(final OneDriveToken token) {
        OneDriveService oneDriveServer = AutOneDriveApi.createService(OneDriveService.class, RestClientOneDriveClient.BASE_URL1, token.access_token, "");
        oneDriveServer.getDriveUserInfo(new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                String json = (String) o;

                OneDriveUserInfo account = JsonUtilFactory.getJsonUtil().fromJson(json, OneDriveUserInfo.class);
                AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);

                if (!manager.ifAccountExist(account.owner.user.displayName))
                    setOneDriveAccountInfo(token, account);

                Intent intent = new Intent(SelectFileAccountActivity.this, MenuActivity.class);
                intent.putExtra(BundleKeys.LOAD_FILE_SECTION, true);
                startActivity(intent);
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });
    }

    private void getDropBoxAccountInfo() {
        DropBoxService dropBoxServer = AuthDropBoxApi.createService(DropBoxService.class, RestClientDropBoxClient.BASE_URL1, mToken, "");
        dropBoxServer.getAccountInfo(new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                String json = (String) o;

                DropBoxAccount account = JsonUtilFactory.getJsonUtil().fromJson(json, DropBoxAccount.class);
                AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
                if (!manager.ifAccountExist(account.email))
                    setDropBoxAccountInfo(account);

                Intent intent = new Intent(SelectFileAccountActivity.this, MenuActivity.class);
                intent.putExtra(BundleKeys.LOAD_FILE_SECTION, true);
                startActivity(intent);
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });
    }

    private void setDropBoxAccountInfo(DropBoxAccount dropBoxUser) {
        AccountInfo accountInfo = new AccountInfo();
        AccountType accountType = UserHelper.getFileAccountType(mType);

        accountInfo.setAccessToken(mToken);
        accountInfo.setAccountId(dropBoxUser.uid);
        accountInfo.setAccountType(accountType);
        accountInfo.setEmail(dropBoxUser.email);
        accountInfo.setName(dropBoxUser.display_name);

        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        manager.createOrUpdateAccountInfo(accountInfo);
    }

    private void setOneDriveAccountInfo(OneDriveToken token, OneDriveUserInfo userInfo) {
        AccountInfo accountInfo = new AccountInfo();
        AccountType accountType = UserHelper.getFileAccountType(mType);

        accountInfo.setAccessToken(token.access_token);
        accountInfo.setRefreshToken(token.refresh_token);
        accountInfo.setAccountType(accountType);
        accountInfo.setEmail(userInfo.owner.user.displayName);
        accountInfo.setName(userInfo.owner.user.displayName);

        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        manager.createOrUpdateAccountInfo(accountInfo);
    }

    private void clearCookie() {
        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
    }

    private void setupDropBox() {
        DropBoxService dropBoxServer = RestClientDropBoxClient.getInstance().getDropBoxService();
        dropBoxServer.authorization(getString(R.string.dropBoxAppKey), "token", getString(R.string.dropBoxRedirectUri), new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                String url = response.getUrl();
                mWebFileAccount.loadUrl(url);
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });
    }

    public enum FileAccountType {
        DROP_BOX, BOX, ONE_DRIVE, GOOGLE_DRIVE
    }
}
