package com.planckmail.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
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
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.response.nylas.Contact;

/**
 * Created by Taras Matolinets on 02.06.15.
 */
public class ContactDetailsFragment extends BaseFragment implements BaseFragment.OnBackPressed{

    private LinearLayout mLlContactDetails;
    private TextView mTvMainTitle;
    private ImageView mImageAvatar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact_details, container, false);

        initViews(view);
        fillView();

        return view;
    }

    private void fillView() {
        String json = getArguments().getString(BundleKeys.CONTACT);

        Contact contact = JsonUtilFactory.getJsonUtil().fromJson(json, Contact.class);

        if (!TextUtils.isEmpty(contact.getEmail())) {
            mTvMainTitle.setText(contact.getEmail());
        } else if (!TextUtils.isEmpty(contact.getName())) {
            mTvMainTitle.setText(contact.getName());
        }

        View flPhoneText = View.inflate(getActivity(), R.layout.elem_people_information, null);
        TextView tvContactDetails = (TextView) flPhoneText.findViewById(R.id.tvContact);
        tvContactDetails.setText(R.string.contactDetails);
        mLlContactDetails.addView(flPhoneText);

        if (!TextUtils.isEmpty(contact.getPhone())) {
            int marginTop = getResources().getDimensionPixelSize(R.dimen.edge_above);
            addContent(contact.getPhone(), R.string.phone, marginTop, getResources().getColor(R.color.blue));
        }

        if (!TextUtils.isEmpty(contact.getName())) {
            int marginTop = getResources().getDimensionPixelSize(R.dimen.edge_above);
            addContent(contact.getName(), R.string.name, marginTop, Color.BLACK);
        }

        if (!TextUtils.isEmpty(contact.getEmail())) {
            int marginTop = getResources().getDimensionPixelSize(R.dimen.edge_above);
            addContent(contact.getEmail(), R.string.email, marginTop, getResources().getColor(R.color.blue));
        }
    }

    private void addContent(String content, int name, int margin, int color) {
        View flPhoneText = View.inflate(getActivity(), R.layout.elem_people_information, null);
        TextView textPhoneText = (TextView) flPhoneText.findViewById(R.id.tvContact);
        textPhoneText.setText(name);
        textPhoneText.setTextColor(Color.BLACK);
        textPhoneText.setTypeface(null, Typeface.BOLD);

        mLlContactDetails.addView(flPhoneText);

        int marginLeft = getResources().getDimensionPixelSize(R.dimen.edge_above);
        View flNumber = View.inflate(getActivity(), R.layout.elem_people_information, null);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(marginLeft, 0, 0, margin);
        TextView number = (TextView) flNumber.findViewById(R.id.tvContact);
        number.setText(content);
        number.setTextColor(color);
        number.setLayoutParams(params);

        mLlContactDetails.addView(flNumber);
    }

    private void initViews(View view) {
        mLlContactDetails = (LinearLayout) view.findViewById(R.id.llContactDetails);
        mTvMainTitle = (TextView) view.findViewById(R.id.tvMainTitle);
        mImageAvatar = (ImageView) view.findViewById(R.id.ivAvatar);
    }

    @Override
    public void onBackPress() {
        getActivity().getFragmentManager().popBackStackImmediate();
    }


}
