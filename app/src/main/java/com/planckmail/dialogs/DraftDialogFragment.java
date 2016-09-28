package com.planckmail.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.planckmail.R;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.request.nylas.DraftVersion;
import com.planckmail.web.response.nylas.Draft;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.TypedJsonString;
import com.planckmail.web.restClient.service.NylasService;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedInput;

/**
 * Created by Taras Matolinets on 13.07.15.
 */
public class DraftDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private Activity mActivity;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.draftSaveDraft)
                .setPositiveButton(R.string.draftSave, this)
                .setNeutralButton(R.string.draftDiscard, this)
                .setNegativeButton(R.string.draftCancel, this);

        return adb.create();
    }

    public void setActivity(Activity activity) {
        mActivity = activity;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        String accessToken = getArguments().getString(BundleKeys.ACCESS_TOKEN);
        String draftJson = getArguments().getString(BundleKeys.DRAFT);
        boolean isUpdateDraft = getArguments().getBoolean(BundleKeys.DRAFT_UPDATE);

        NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accessToken, "");

        switch (which) {
            case Dialog.BUTTON_POSITIVE:
                if (isUpdateDraft)
                    updateDraft(draftJson, nylasServer);
                else
                    saveDraft(draftJson, nylasServer);
                break;
            case Dialog.BUTTON_NEGATIVE:
                dismiss();
                break;

            case Dialog.BUTTON_NEUTRAL:
                Draft draft = JsonUtilFactory.getJsonUtil().fromJson(draftJson, Draft.class);
                DraftVersion version = draftVersion(draft.version);

                String messageJson = JsonUtilFactory.getJsonUtil().toJson(version);
                final TypedInput in = new TypedJsonString(messageJson);

                nylasServer.deleteDraft(draft.id, in, new Callback<Object>() {
                    @Override
                    public void success(Object o, Response response) {
                        Intent intent = new Intent();
                        intent.putExtra(BundleKeys.TOAST_TEXT, mActivity.getResources().getText(R.string.draftDeleted));

                        mActivity.setResult(Activity.RESULT_OK, intent);
                        mActivity.finish();
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Response r = error.getResponse();
                        if (r != null) {
                            Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                        }
                        mActivity.finish();
                    }
                });
                break;
        }
    }

    private void updateDraft(String draftJson, NylasService nylasServer) {
        Draft draft = JsonUtilFactory.getJsonUtil().fromJson(draftJson, Draft.class);
        draft.setVersion(draft.version);

        String json = JsonUtilFactory.getJsonUtil().toJson(draft);
        final TypedInput in = new TypedJsonString(json);

        nylasServer.updateDraft(draft.id, in, new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                Log.d(PlanckMailApplication.TAG, "response :" + response.getUrl());
                Intent intent = new Intent();
                intent.putExtra(BundleKeys.TOAST_TEXT, mActivity.getResources().getText(R.string.draftUpdate));

                mActivity.setResult(Activity.RESULT_OK, intent);

                mActivity.finish();
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

    private DraftVersion draftVersion(int version) {
        DraftVersion draftVersion = new DraftVersion();
        draftVersion.version = version;

        return draftVersion;
    }

    public void saveDraft(String messageJson, NylasService nylasServer) {
        final TypedInput in = new TypedJsonString(messageJson);
        nylasServer.createDraft(in, new Callback<Object>() {

            @Override
            public void success(Object o, Response response) {
                Intent intent = new Intent();
                intent.putExtra(BundleKeys.TOAST_TEXT, mActivity.getResources().getText(R.string.draftSaved));

                mActivity.setResult(Activity.RESULT_OK, intent);

                mActivity.finish();
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
}
