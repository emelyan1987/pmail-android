package com.planckmail.views;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.web.response.nylas.Contact;
import com.tokenautocomplete.TokenCompleteTextView;

/**
 * Created by Taras Matolinets on 29.06.15.
 */
public class ContactsCompletionView extends TokenCompleteTextView<Contact> {

    public ContactsCompletionView(Context context) {
        super(context);
    }

    public ContactsCompletionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ContactsCompletionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected View getViewForObject(Contact contact) {
        LayoutInflater l = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        LinearLayout view = (LinearLayout) l.inflate(R.layout.contact_token, (ViewGroup) ContactsCompletionView.this.getParent(), false);
        ((TextView) view.findViewById(R.id.name)).setText(contact.getEmail());

        return view;
    }

    @Override
    protected Contact defaultObject(String completionText) {
        Contact contact = new Contact();
        contact.setEmail(completionText.replaceAll(" ,", ""));

        return contact;
    }

}

