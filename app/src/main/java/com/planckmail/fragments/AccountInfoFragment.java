package com.planckmail.fragments;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.activities.LoginActivity;
import com.planckmail.activities.MenuActivity;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.PlanckMailSharePreferences;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.data.db.beans.MessageDB;
import com.planckmail.data.db.beans.ParticipantDB;
import com.planckmail.data.db.beans.ThreadDB;
import com.planckmail.data.db.manager.AccountInfoDataManager;
import com.planckmail.data.db.manager.DataBaseManager;
import com.planckmail.helper.InternetConnection;
import com.planckmail.helper.UserHelper;
import com.planckmail.receiver.MessageReceiver;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.response.oneDrive.OneDriveToken;
import com.planckmail.web.restClient.RestClientBoxDriveClient;
import com.planckmail.web.restClient.RestClientDropBoxClient;
import com.planckmail.web.restClient.RestClientGoogleDriveClient;
import com.planckmail.web.restClient.RestPlankMail;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.api.AutOneDriveApi;
import com.planckmail.web.restClient.api.AuthDropBoxApi;
import com.planckmail.web.restClient.api.AuthGoogleDriveApi;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.service.BoxDriveService;
import com.planckmail.web.restClient.service.DropBoxService;
import com.planckmail.web.restClient.service.GoogleDriveService;
import com.planckmail.web.restClient.service.NylasService;
import com.planckmail.web.restClient.service.PlanckService;

import java.util.List;

import io.fabric.sdk.android.services.concurrency.AsyncTask;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Taras Matolinets on 15.05.15.
 */
public class AccountInfoFragment extends BaseFragment implements View.OnClickListener {

    private TextView mTvDeleteAccount;
    private ProgressBar mLoading;
    private int UN_AUTHORISE = 401;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_account_info, container, false);
        initViews(view);
        setListeners();

        return view;
    }

    private void setListeners() {
        mTvDeleteAccount.setOnClickListener(this);
    }

    private void initViews(View view) {
        mTvDeleteAccount = (TextView) view.findViewById(R.id.tvRemoveAccount);
        mLoading = (ProgressBar) view.findViewById(R.id.prLoading);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvRemoveAccount:
                mTvDeleteAccount.setVisibility(View.GONE);
                AsyncDeleteEmail task = new AsyncDeleteEmail();
                task.execute();
                break;
        }
    }

    private class AsyncDeleteEmail extends AsyncTask<Void, Void, AccountInfo> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoading.setVisibility(View.VISIBLE);
        }

        @Override
        protected AccountInfo doInBackground(Void[] objects) {
            Bundle bundle = getArguments();
            String id = bundle.getString(BundleKeys.KEY_ACCOUNT_ID);
            AccountInfoDataManager manager = (AccountInfoDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MAIL_ACCOUNT_MANAGER);

            final AccountInfo accountInfo = manager.getAccountInfoById(id);
            PlanckMailSharePreferences.setDataToSharePreferences(BundleKeys.SAVE_DATA_IN_DB, true, PlanckMailSharePreferences.SHARE_PREFERENCES_TYPE.BOOLEAN);

            manager.deleteAccountInfo(accountInfo);

            if (accountInfo != null) {
                if (accountInfo.isEmailAccount) {
                    removeTokenNylas(accountInfo);
                    removeTokenPlankLabs(accountInfo);
                } else {
                    removeFilesAccount(accountInfo);
                }
            }
            return accountInfo;
        }

        @Override
        protected void onPostExecute(AccountInfo accountInfo) {
            mLoading.setVisibility(View.GONE);
            //clean db for avoid duplications element
            cleanDb();
            List<AccountInfo> list = UserHelper.getEmailAccountList(true);

            if (!list.isEmpty()) {
                Intent intent = new Intent(getActivity(), MenuActivity.class);

                if (!accountInfo.isEmailAccount)
                    intent.putExtra(BundleKeys.LOAD_FILE_SECTION, true);

                startActivity(intent);
            } else {
                cancelAlarm();

                PlanckMailApplication app = (PlanckMailApplication) getActivity().getApplication();
                app.setLoadDB(false);

                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
            }
        }
    }

    private void removeFilesAccount(AccountInfo accountInfo) {
        switch (accountInfo.accountType) {
            case DROP_BOX:
                revokeDropBoxAccount(accountInfo);
                break;
            case GOOGLE_DRIVE:
                revokeGoogleDriveAccount(accountInfo);
                break;
            case BOX:
                revokeBoxAccount(accountInfo);
                break;
        }
    }

    private void revokeDropBoxAccount(AccountInfo accountInfo) {
        DropBoxService dropBoxServer = AuthDropBoxApi.createService(DropBoxService.class, RestClientDropBoxClient.BASE_URL1, accountInfo.accessToken, "");
        dropBoxServer.disableAccessToken(new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                Log.i(PlanckMailApplication.TAG, "token  revoked: dropBox");
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error token not revoked: dropBox " + r.getReason());
            }
        });
    }

    private void revokeGoogleDriveAccount(final AccountInfo accountInfo) {
        GoogleDriveService googleDriveServer = AuthGoogleDriveApi.createService(GoogleDriveService.class, RestClientGoogleDriveClient.BASE_URL, accountInfo.accessToken, "");
        googleDriveServer.revokeAccessToken(accountInfo.getAccessToken(), new Callback<String>() {
            @Override
            public void success(String s, Response response) {
                Log.i(PlanckMailApplication.TAG, "token  revoked: google drive");
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null && r.getStatus() == UN_AUTHORISE) {
                    getNewGoogleDriveToken(accountInfo);
                } else if (r != null) {
                    Log.e(PlanckMailApplication.TAG, "error token not revoked: google drive " + r.getReason());
                }
            }
        });
    }

    private void getNewGoogleDriveToken(final AccountInfo accountInfo) {
        RestClientGoogleDriveClient googleDriveClient = new RestClientGoogleDriveClient(RestClientGoogleDriveClient.BASE_URL1);
        GoogleDriveService oneDriveService = googleDriveClient.getGoogleDriveService();
        oneDriveService.refreshToken(getString(R.string.googleDriveClientId), getString(R.string.googleDriveClientSecret), accountInfo.getRefreshToken(), "refresh_token", new Callback<String>() {

            @Override
            public void success(String json, Response response) {
                OneDriveToken token = JsonUtilFactory.getJsonUtil().fromJson(json, OneDriveToken.class);
                accountInfo.setAccessToken(token.access_token);
                accountInfo.setRefreshToken(token.refresh_token);
                revokeGoogleDriveAccount(accountInfo);
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error taken new googleDrive token" + r.getReason());
            }
        });
    }

    private void revokeBoxAccount(final AccountInfo accountInfo) {
        BoxDriveService boxDriveServer = AutOneDriveApi.createService(BoxDriveService.class, RestClientBoxDriveClient.BASE_URL, accountInfo.accessToken, "");
        boxDriveServer.revokeToken(getString(R.string.boxAppKey), getString(R.string.boxClientSecret), accountInfo.accessToken, new Callback<String>() {
            @Override
            public void success(String s, Response response) {
                Log.i(PlanckMailApplication.TAG, "token  revoked: box");
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null && r.getStatus() == UN_AUTHORISE)
                    getNewBoxToken(accountInfo);
                else if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error token not revoked: box " + r.getReason());
            }
        });
    }

    private void getNewBoxToken(final AccountInfo accountInfo) {
        RestClientBoxDriveClient oneDriveApi = new RestClientBoxDriveClient(RestClientBoxDriveClient.BASE_URL);
        BoxDriveService oneDriveService = oneDriveApi.getBoxDriveService();
        oneDriveService.refreshToken(getString(R.string.boxAppKey), getString(R.string.boxClientSecret),
                accountInfo.getRefreshToken(), "refresh_token", new Callback<String>() {

                    @Override
                    public void success(String json, Response response) {
                        OneDriveToken token = JsonUtilFactory.getJsonUtil().fromJson(json, OneDriveToken.class);
                        accountInfo.setAccessToken(token.access_token);
                        accountInfo.setRefreshToken(token.refresh_token);
                        revokeBoxAccount(accountInfo);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Response r = error.getResponse();
                        if (r != null)
                            Log.e(PlanckMailApplication.TAG, "error taken new token " + r.getReason());
                    }
                });
    }

    private void removeTokenNylas(AccountInfo accountInfo) {
        if (InternetConnection.isNetworkConnected(getActivity())) {
            //delete token to planck DB
            final NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accountInfo.getAccessToken(), "");
            nylasServer.revokeAccessToken(new Callback<Object>() {
                @Override
                public void success(Object o, Response response) {
                    Log.i(PlanckMailApplication.TAG, "token  revoked: nylas");
                }

                @Override
                public void failure(RetrofitError error) {
                    Response r = error.getResponse();
                    if (r != null)
                        Log.e(PlanckMailApplication.TAG, "error token not revoked: " + r.getReason());
                }
            });
        }
    }

    private void cancelAlarm() {
        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Activity.ALARM_SERVICE);

        Intent myIntent = new Intent(getActivity(), MessageReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), PlanckMailApplication.ALARM_PENDING_CODE, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.cancel(pendingIntent);
    }

    /**
     * store token to planck DB
     *
     * @param accountInfo account for store token
     */
    private void removeTokenPlankLabs(final AccountInfo accountInfo) {
        if (InternetConnection.isNetworkConnected(getActivity())) {
            //delete token to planck DB
            RestPlankMail client = new RestPlankMail(RestPlankMail.BASE_URL2);
            PlanckService service = client.getPlankService();

            service.deleteToken(accountInfo.getEmail(), "planck_test", new Callback<Object>() {
                @Override
                public void success(Object o, Response response) {
                    Log.i(PlanckMailApplication.TAG, "token deleted planckLabs");
                }

                @Override
                public void failure(RetrofitError error) {
                    Response r = error.getResponse();
                    if (r != null)
                        Log.e(PlanckMailApplication.TAG, "error token deleted planckLabs " + r.getReason());
                }
            });
        }
    }

    public void cleanDb() {
        DataBaseManager.getInstanceDataManager().cleanTable(ThreadDB.class);
        DataBaseManager.getInstanceDataManager().cleanTable(MessageDB.class);
        DataBaseManager.getInstanceDataManager().cleanTable(ParticipantDB.class);
    }
}
