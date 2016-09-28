package com.planckmail.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.web.helper.UtilHelpers;
import com.planckmail.web.response.nylas.Contact;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


/**
 * Created by Taras Matolinets on 01.06.15.
 */
public class RecyclePeopleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder>, View.OnClickListener {
    private Context mContext;
    private List<Contact> mListContacts;
    private HashMap<Integer, Integer> mMailMapColor = new HashMap<>();

    public RecyclePeopleAdapter(Context context, List<Contact> list) {
        mContext = context;
        mListContacts = new ArrayList<>(list);
    }

    @Override
    public ViewHolderPeople onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.elem_contact, parent, false);

        return new ViewHolderPeople(view);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Contact contact = mListContacts.get(position);

        if (holder instanceof ViewHolderPeople) {
            int color = getColorForBox(position);

            int radius = 300;
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(radius);
            shape.setColor(color);

            String shortTitle = getShortTitle(contact);
            ((ViewHolderPeople) holder).llContact.setTag(contact);

            ((ViewHolderPeople) holder).tvShortName.setText(shortTitle);
            ((ViewHolderPeople) holder).tvShortName.setBackgroundDrawable(shape);

            ((ViewHolderPeople) holder).tvName.setText(contact.getName());
            ((ViewHolderPeople) holder).tvEmail.setText(contact.getEmail());
        }
    }

    private String getShortTitle(Contact contact) {
        String shortTitle = null;
        if (!TextUtils.isEmpty(contact.getName()))
            shortTitle = UtilHelpers.buildTitle(contact.name);
        else if (!TextUtils.isEmpty(contact.getEmail()))
            shortTitle = UtilHelpers.buildTitle(contact.email);
        return shortTitle;
    }

    public void updateData(List<Contact> list) {
        mListContacts = list;
    }

    @Override
    public int getItemCount() {
        return mListContacts.size();
    }

    @Override
    public long getHeaderId(int position) {
        String email = mListContacts.get((position)).email;
        String name = mListContacts.get((position)).name;

        if (email != null && !TextUtils.isEmpty(email))
            return email.toUpperCase().charAt(0);
        else
            return name.toUpperCase().charAt(0);
    }

    @Override
    public long getItemId(int position) {
        return mListContacts.size();
    }


    public int getColorForBox(int id) {
        int color;
        if (mMailMapColor.containsKey(id))
            color = mMailMapColor.get(id);
        else {
            Random rnd = new Random();
            color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
            mMailMapColor.put(id, color);
        }
        return color;
    }

    @Override
    public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup viewGroup) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.view_header_people, viewGroup, false);
        return new RecyclerView.ViewHolder(view) {
        };
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        TextView textView = (TextView) viewHolder.itemView;
        String email = mListContacts.get(i).getEmail();
        String name = mListContacts.get(i).getName();

        if (!TextUtils.isEmpty(email))
            textView.setText(String.valueOf(email.toUpperCase().charAt(0)));
        else
            textView.setText(String.valueOf(name.toUpperCase().charAt(0)));
    }

    @Override
    public void onClick(View v) {

    }

    public class ViewHolderPeople extends RecyclerView.ViewHolder {
        public TextView tvShortName;
        public TextView tvName;
        public TextView tvEmail;
        public LinearLayout llContact;
        public FrameLayout flPicture;

        public ViewHolderPeople(View itemView) {
            super(itemView);
            tvShortName = (TextView) itemView.findViewById(R.id.tvMailPicture);
            tvName = (TextView) itemView.findViewById(R.id.tvName);
            tvEmail = (TextView) itemView.findViewById(R.id.tvEmail);
            llContact = (LinearLayout) itemView.findViewById(R.id.llMainContent);
            flPicture = (FrameLayout) itemView.findViewById(R.id.flPicture);
        }
    }
}
