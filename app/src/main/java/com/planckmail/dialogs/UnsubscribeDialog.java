package com.planckmail.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.AccountInfo;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.request.planck.BlockedMail;
import com.planckmail.web.response.nylas.wrapper.Participant;
import com.planckmail.web.restClient.RestPlankMail;
import com.planckmail.web.restClient.service.PlanckService;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Terry on 3/11/2016.
 */
public class UnsubscribeDialog extends DialogFragment implements OnClickListener {
    private IRemoveUnsubscribeThread mListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_unsubscribe, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View view) {
        Button btCancel = (Button) view.findViewById(R.id.btCancel);
        Button btUnsubscribe = (Button) view.findViewById(R.id.btUnsubscribe);
        TextView mSubTitle = (TextView) view.findViewById(R.id.tvSubTitle);

        String jsonParticipant = getArguments().getString(BundleKeys.PARTICIPANT);
        Participant participant = JsonUtilFactory.getJsonUtil().fromJson(jsonParticipant, Participant.class);
        mSubTitle.setText(getString(R.string.unsubscribeSubTitle, participant.getEmail()));
        btCancel.setOnClickListener(this);
        btUnsubscribe.setOnClickListener(this);
    }

    public void setListener(IRemoveUnsubscribeThread listener) {
        mListener = listener;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btCancel:
                dismiss();
                break;
            case R.id.btUnsubscribe:
                String jsonAccountInfo = getArguments().getString(BundleKeys.ACCOUNT);
                String jsonParticipant = getArguments().getString(BundleKeys.PARTICIPANT);
                AccountInfo accountInfo = JsonUtilFactory.getJsonUtil().fromJson(jsonAccountInfo, AccountInfo.class);
                Participant participant = JsonUtilFactory.getJsonUtil().fromJson(jsonParticipant, Participant.class);

                BlockedMail blockedMail = new BlockedMail();
                blockedMail.email = participant.email;
                if (!TextUtils.isEmpty(participant.name))
                    blockedMail.name = participant.name;
                else
                    blockedMail.name = getString(R.string.noName);

                RestPlankMail client = new RestPlankMail(RestPlankMail.BASE_URL3);
                PlanckService service = client.getPlankService();
                service.addToBlackList(accountInfo.getEmail(), participant.email, new Callback<String>() {
                    @Override
                    public void success(String s, Response response) {
                        Log.i(PlanckMailApplication.TAG, "response: " + s);
                        if (mListener != null) {
                            int position = getArguments().getInt(BundleKeys.POSITION);
                            mListener.removeUnsubscribeThread(position);
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Response r = error.getResponse();
                        if (r != null)
                            Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                    }
                });

                dismiss();
                break;

        }
    }

    public interface IRemoveUnsubscribeThread {
        void removeUnsubscribeThread(int position);
    }
}
