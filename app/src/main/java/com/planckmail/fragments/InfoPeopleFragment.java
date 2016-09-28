package com.planckmail.fragments;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.activities.ComposeActivity;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.response.nylas.Contact;

import static com.planckmail.fragments.InfoPeopleFragment.PEOPLE_ACTION.MAIL;
import static com.planckmail.fragments.InfoPeopleFragment.PEOPLE_ACTION.PHONE;

/**
 * Created by Terry on 3/23/2016.
 */
public class InfoPeopleFragment extends BaseFragment implements View.OnClickListener {

    private LinearLayout mLayoutContacts;
    private PEOPLE_ACTION mPeopleAction;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_people_info, container, false);
        initViews(view);
        addContact();
        return view;
    }

    private void initViews(View view) {
        mLayoutContacts = (LinearLayout) view.findViewById(R.id.llPeopleInfo);
    }

    private void addContact() {
        String jsonContact = getArguments().getString(BundleKeys.CONTACT);
        Contact contact = JsonUtilFactory.getJsonUtil().fromJson(jsonContact, Contact.class);

        if (!TextUtils.isEmpty(contact.getPhone())) {
            mPeopleAction = PHONE;
            addContent(contact.getPhone(), R.drawable.ic_phone);
        }

        if (!TextUtils.isEmpty(contact.getEmail())) {
            mPeopleAction = MAIL;
            addContent(contact.getEmail(), R.drawable.ic_mail);
        }
    }

    private void addContent(String content, int drawable) {
        Drawable img = getActivity().getResources().getDrawable(drawable);
        View flNumber = View.inflate(getActivity(), R.layout.elem_people_information, null);

        FrameLayout flContact = (FrameLayout) flNumber.findViewById(R.id.flMailContact);
        TextView tvContact = (TextView) flNumber.findViewById(R.id.tvContact);
        tvContact.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
        tvContact.setText(content);
        tvContact.setTag(R.string.tag_content, content);
        flContact.setOnClickListener(this);

        if (mPeopleAction.equals(PHONE)) {
            FrameLayout flMessasage = (FrameLayout) flNumber.findViewById(R.id.flMessage);
            ImageView ivMessage = (ImageView) flNumber.findViewById(R.id.ivMessage);
            ivMessage.setVisibility(View.VISIBLE);
            flMessasage.setOnClickListener(this);
        }
        mLayoutContacts.addView(flNumber);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.flMailContact:
                String content = (String) v.getTag(R.string.tag_content);
                switch (mPeopleAction) {
                    case PHONE:
                        Intent callIntent = new Intent(Intent.ACTION_CALL);
                        callIntent.setData(Uri.parse("tel:" + content));
                        startActivity(callIntent);
                        break;
                    case MAIL:
                        Intent intent = new Intent(getActivity(), ComposeActivity.class);
                        intent.putExtra(BundleKeys.COMPOSE_TO, content);
                        startActivity(intent);
                        break;
                }
                break;
            case R.id.flMessage:
                Intent sendIntent = new Intent(Intent.ACTION_VIEW);
                sendIntent.setData(Uri.parse("sms:"));
                startActivity(sendIntent);
                break;
        }
    }

    public enum PEOPLE_ACTION {
        PHONE, MAIL
    }

}
