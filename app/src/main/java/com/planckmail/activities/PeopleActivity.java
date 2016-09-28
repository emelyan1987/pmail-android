package com.planckmail.activities;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.adapters.PeopleViewPagerAdapter;
import com.planckmail.fragments.EventsPeopleFragment;
import com.planckmail.fragments.FilesPeopleFragment;
import com.planckmail.fragments.InfoPeopleFragment;
import com.planckmail.fragments.MailPeopleFragment;
import com.planckmail.listeners.OnTabPageChangeListener;

import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.views.MainTabLayout;
import com.planckmail.web.helper.UtilHelpers;
import com.planckmail.web.response.nylas.Contact;

import org.w3c.dom.Text;

/**
 * Created by Terry on 3/23/2016.
 */
public class PeopleActivity extends BaseActivity {
    private TextView mPersonName;
    private RelativeLayout mRlBackground;
    private TextView mTvPeopleFullName;
    private MainTabLayout mTabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_people);

        Toolbar toolbar = (Toolbar) findViewById(R.id.peopleToolbar);
        mPersonName = (TextView)findViewById(R.id.tvPeopleShortName);
        mTvPeopleFullName = (TextView)findViewById(R.id.tvPeopleFullName);
        mRlBackground = (RelativeLayout)findViewById(R.id.rlPeopleHeader);

        String jsonContact = getIntent().getStringExtra(BundleKeys.CONTACT);
        Contact contact = JsonUtilFactory.getJsonUtil().fromJson(jsonContact, Contact.class);

        changeStatusBarColor(contact);

        mTvPeopleFullName.setText(contact.getName());
        String shortTitle = getShortTitle(contact);

        mRlBackground.setBackgroundColor(contact.getColor());
        toolbar.setBackgroundColor(contact.getColor());
        int radius = 300;
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(radius);
        shape.setColor(Color.WHITE);
        mPersonName.setTextColor(contact.getColor());
        mPersonName.setText(shortTitle);
        mPersonName.setBackgroundDrawable(shape);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        final ViewPager viewPager = (ViewPager) findViewById(R.id.peopleViewPager);
        final PeopleViewPagerAdapter adapter = new PeopleViewPagerAdapter(getFragmentManager());

        Bundle bundle = new Bundle();
        bundle.putString(BundleKeys.CONTACT,jsonContact);

        InfoPeopleFragment infoFragment = new InfoPeopleFragment();
        infoFragment.setArguments(bundle);

        MailPeopleFragment mailFragment = new MailPeopleFragment();
        mailFragment.setArguments(bundle);
        EventsPeopleFragment eventsFragment = new EventsPeopleFragment();
        eventsFragment.setArguments(bundle);
        FilesPeopleFragment filesFragment = new FilesPeopleFragment();
        filesFragment.setArguments(bundle);

        adapter.addFragment(infoFragment, getString(R.string.infoPeople));
        adapter.addFragment(mailFragment, getString(R.string.mailPeople));
        adapter.addFragment(eventsFragment, getString(R.string.eventsPeople));
        adapter.addFragment(filesFragment, getString(R.string.filesPeople));

        viewPager.setAdapter(adapter);

        mTabLayout = (MainTabLayout) findViewById(R.id.peopleTabs);
        mTabLayout.setupWithViewPager(viewPager);


        viewPager.addOnPageChangeListener(new OnTabPageChangeListener(mTabLayout));

    }

    private void changeStatusBarColor(Contact contact) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
           window.setStatusBarColor(contact.getColor());
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
