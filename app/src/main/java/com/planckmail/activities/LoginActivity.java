package com.planckmail.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.planckmail.R;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.PlanckMailSharePreferences;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.db.manager.AccountInfoDataManager;
import com.planckmail.data.db.manager.DataBaseManager;
import com.planckmail.enums.AccountType;
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
 * Created by Taras Matolinets on 03.05.15.
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {
    private static final String FOLLOW_UP = "FollowUp";

    private Button mButtonLogin;
    private WebView mWebViewLogin;
    private String mToken;
    private ProgressBar mProgress;
    private TextView mTvTitle;
    private TextView mTvPrivatePolice;
    private ImageView mIvLogo;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        runMenuActivity();
        initViews();
        setListeners();
        setSettingsToWebView();
        setWebClient();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setSettingsToWebView() {
        mWebViewLogin.getSettings().setJavaScriptEnabled(true);
        mWebViewLogin.getSettings().setSupportMultipleWindows(true);
        mWebViewLogin.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
    }

    private void setListeners() {
        mButtonLogin.setOnClickListener(this);
    }

    private void initViews() {
        mWebViewLogin = (WebView) findViewById(R.id.webViewLogin);
        mProgress = (ProgressBar) findViewById(R.id.prLoadMain);
        mTvTitle = (TextView) findViewById(R.id.tvTitle);
        mTvPrivatePolice = (TextView) findViewById(R.id.tvPrivatePolice);
        mButtonLogin = (Button) findViewById(R.id.btLogin);
        mIvLogo = (ImageView) findViewById(R.id.ivLogo);
    }

    private void runMenuActivity() {
        List<AccountInfo> list = UserHelper.getEmailAccountList(true);
        if (!list.isEmpty()) {
            Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
            startActivity(intent);
        }
    }

    private void setWebClient() {
        mWebViewLogin.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mProgress.setVisibility(View.GONE);
                mIvLogo.setVisibility(View.GONE);

                mWebViewLogin.setVisibility(View.VISIBLE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                getAccessToken(view, url);
                return true;
            }

            @Override
            public void onReceivedHttpAuthRequest(WebView view, @NonNull HttpAuthHandler handler, String host, String realm) {
                NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, mToken, "");
                nylasServer.getAccounts(new Callback<Object>() {
                    @Override
                    public void success(Object obj, Response response) {
                        Toast.makeText(LoginActivity.this, R.string.successful_login, Toast.LENGTH_SHORT).show();
                        PlanckMailSharePreferences.setDataToSharePreferences(BundleKeys.SPLASH_LOADED, true, PlanckMailSharePreferences.SHARE_PREFERENCES_TYPE.BOOLEAN);
                        setMailAccount((String) obj);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Response r = error.getResponse();
                        if (r != null)
                            Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                    }
                });
            }
        });
    }

    private void setMailAccount(String json) {

        Log.i(PlanckMailApplication.TAG, "response: " + json);

        final NameSpace nameSpace = JsonUtilFactory.getJsonUtil().fromJson(json, NameSpace.class);

        createFolder();
        storeTokenInPlanckServer(nameSpace);
        addUserToPriorityList(nameSpace);

        AccountInfo account = new AccountInfo();

        AccountType accountType = UserHelper.getEmailAccountType(nameSpace.email_address);

        account.setAccountId(nameSpace.account_id);
        account.setEmail(nameSpace.email_address);
        account.setAccountType(accountType);
        account.setAccessToken(mToken);
        account.setName(nameSpace.name);
        account.setObject(nameSpace.object);
        account.setProvider(nameSpace.provider);
        account.setIsEmailAccount(true);
        account.setCalendarColor(UserHelper.getLightColor());

        setData(account);

        Intent intent = new Intent(this, MenuActivity.class);
        startActivity(intent);
    }

    private void storeTokenInPlanckServer(final NameSpace nameSpace) {
        //store token to planck DB
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
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });
    }

    private void createFolder() {
        String json = createRequestBody();
        TypedInput in = new TypedJsonString(json);

        NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, mToken, "");
        nylasServer.createFolder(in, new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                Log.i(PlanckMailApplication.TAG, "folder created successful");
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "Reject: folder already created" + r.getReason());
            }
        });
    }

    private String createRequestBody() {
        CreateFolder folder = new CreateFolder();
        folder.display_name = FOLLOW_UP;
        return JsonUtilFactory.getJsonUtil().toJson(folder);
    }

    private void addUserToPriorityList(final NameSpace nameSpace) {
        RestPlankMail client = new RestPlankMail(RestPlankMail.BASE_URL2);
        PlanckService service = client.getPlankService();

        service.addUserToPriorityToken(nameSpace.email_address, mToken, new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                //do nothing
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });
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

    private void setData(AccountInfo account) {
        AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);
        manager.createOrUpdateAccountInfo(account);
    }

    private void callAuthorization() {
        //token ,email required params for authorization. Look(https://www.nylas.com/docs/knowledgebase#client-side-implicit-flow)
        RestWebClient service = new RestWebClient();

        service.getNylasService().authorization(getString(R.string.app_id), "token", "email", getString(R.string.nylasRedirectUri), callbackAuthorization);
    }

    private Callback<Object> callbackAuthorization = new Callback<Object>() {
        @Override
        public void failure(RetrofitError error) {
            Response r = error.getResponse();
            if (r != null)
                Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
        }

        @Override
        public void success(Object o, Response response) {
            String url = response.getUrl();
            mWebViewLogin.loadUrl(url);
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btLogin:
                callAuthorization();
                mProgress.setVisibility(View.VISIBLE);
                mButtonLogin.setVisibility(View.GONE);
                mTvPrivatePolice.setVisibility(View.GONE);
                mTvTitle.setVisibility(View.GONE);
                break;
        }
    }
}
