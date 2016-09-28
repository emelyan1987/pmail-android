package com.planckmail.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.planckmail.R;
import com.planckmail.adapters.AutoCompleteContactAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.dialogs.DialogAvailableTime;
import com.planckmail.dialogs.DialogAvailableTime.ISelectTime;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.views.ContactsCompletionView;
import com.planckmail.web.response.nylas.Contact;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.service.NylasService;
import com.tokenautocomplete.TokenCompleteTextView.TokenListener;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Terry on 12/6/2015.
 */
public class CreateAvailabilityActivity extends BaseActivity implements View.OnClickListener, TextWatcher, ISelectTime, TokenListener {

    private static final int LIMIT_LOAD_MAILS = 600;
    public static final int RESULT_GET_LOCATION = 478;
    private EditText mTitle;
    private ContactsCompletionView mTakePeople;
    private TextView mSelectTime;
    private TextView mSelectLocation;
    private AutoCompleteContactAdapter mAdapter;
    private boolean isEnableCreateAvailability;
    private List<Contact> mListContacts = new ArrayList<>();
    private LinearLayout mllSelectTime;
    private LinearLayout mllLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_availability);
        initViews();
        setListeners();
        initActionBar();
        fillView();
        getContacts();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!mListContacts.isEmpty())
            getContacts();
    }

    private void initActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.createAvailability);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void initViews() {
        mTitle = (EditText) findViewById(R.id.etTitle);
        mTakePeople = (ContactsCompletionView) findViewById(R.id.etPeople);
        mSelectTime = (TextView) findViewById(R.id.tvSelectTime);
        mSelectLocation = (TextView) findViewById(R.id.tvAddress);
        mllSelectTime = (LinearLayout) findViewById(R.id.llSelectTime);
        mllLocation = (LinearLayout) findViewById(R.id.llLocation);
    }

    private void setListeners() {
        mllLocation.setOnClickListener(this);
        mllSelectTime.setOnClickListener(this);
        mTakePeople.addTextChangedListener(this);
        mTitle.addTextChangedListener(this);
        mTakePeople.setTokenListener(this);
    }

    private void fillView() {
        String jsonContact = getIntent().getStringExtra(BundleKeys.CONTACT);
        mListContacts = JsonUtilFactory.getJsonUtil().fromJsonArray(jsonContact, Contact.class);
        mAdapter = new AutoCompleteContactAdapter(this);

        mTakePeople.setAdapter(mAdapter);

        String title = getIntent().getStringExtra(BundleKeys.THREAD_SUBJECT);

        if (!TextUtils.isEmpty(title))
            mTitle.setText(title);

        ArrayList<String> listPeople = getIntent().getExtras().getStringArrayList(BundleKeys.EMAIL);

        if (listPeople != null) {
            for (String email : listPeople) {
                Contact contact = new Contact();
                contact.setEmail(email);
                mTakePeople.addObject(contact);
            }
        }
    }

    private void getContacts() {
        String accessToken = getIntent().getStringExtra(BundleKeys.ACCESS_TOKEN);

        NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accessToken, "");
        nylasServer.getAllContacts(LIMIT_LOAD_MAILS, new Callback<Object>() {
                    @Override
                    public void success(Object o, Response response) {
                        String json = (String) o;
                        Log.i(PlanckMailApplication.TAG, "url " + response.getUrl());
                        mListContacts = JsonUtilFactory.getJsonUtil().fromJsonArray(json, Contact.class);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Response r = error.getResponse();
                        if (r != null)
                            Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_create_availability, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.doneAvailability:

                if (isEnableCreateAvailability) {
                    Intent intent = grabIntentData();
                    setResult(RESULT_OK, intent);
                    finish();
                }
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean getCheckStatus() {
        boolean isTitleFilled = !TextUtils.isEmpty(mTitle.getText());
        boolean isTimeSelected = !TextUtils.isEmpty(mSelectTime.getText()) && !mSelectTime.getText().equals(getResources().getString(R.string.none));
        boolean isLocation = !TextUtils.isEmpty(mSelectLocation.getText()) && !mSelectLocation.getText().equals(getResources().getString(R.string.none));
        boolean isPeopleExist = !mTakePeople.getObjects().isEmpty();
        return isTitleFilled && isLocation && isPeopleExist && isTimeSelected;
    }

    private Intent grabIntentData() {
        Intent intent = new Intent();
        intent.putExtra(BundleKeys.THREAD_SUBJECT, mTitle.getText().toString());
        intent.putExtra(BundleKeys.DURATION, mSelectTime.getText().toString());
        intent.putExtra(BundleKeys.LOCATION, mSelectLocation.getText().toString());
        intent.putExtra(BundleKeys.DURATION, mSelectLocation.getText().toString());

        int seconds = 0;
        if (null != mSelectTime.getTag(R.string.tag_position)) {
            seconds = (int) mSelectTime.getTag(R.string.tag_seconds);
            intent.putExtra(BundleKeys.SECONDS_AVAILABILITY, seconds);
        }

        List<Contact> contacts = mTakePeople.getObjects();

        ArrayList<String> contactsList = new ArrayList<>();

        for (Contact contact : contacts) {
            contactsList.add(contact.email);
        }
        intent.putExtra(BundleKeys.EMAIL, contactsList);

        return intent;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.llLocation:
                Intent intent = new Intent(this, FindLocationActivity.class);
                startActivityForResult(intent, RESULT_GET_LOCATION);
                break;
            case R.id.llSelectTime:
                int position = 0;

                if (null != mSelectTime.getTag(R.string.tag_position)) {
                    position = (int) mSelectTime.getTag(R.string.tag_position);
                }
                Bundle bundle = new Bundle();
                bundle.putInt(BundleKeys.POSITION, position);

                DialogAvailableTime dialog = new DialogAvailableTime();
                dialog.setListener(this);
                dialog.setArguments(bundle);
                dialog.show(getFragmentManager(), dialog.getClass().toString());
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && resultCode == RESULT_OK) {
            switch (requestCode) {
                case RESULT_GET_LOCATION:
                    String date = data.getStringExtra(BundleKeys.LOCATION);
                    mSelectLocation.setText(date);
                    checkIfDataExist();
                    invalidateOptionsMenu();
                    break;
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem itemSave = menu.getItem(0);

        if (isEnableCreateAvailability)
            itemSave.setIcon(R.drawable.ic_check_white_action_bar);
        else
            itemSave.setIcon(R.drawable.ic_check_grey_action_bar);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

        List<Contact> filteredList = new ArrayList<>();
        String modifyString = s.toString().replaceAll("[,\\s]", "");

        if (modifyString.length() > 0) {
            for (Contact contact : mListContacts) {
                int contactLimit = 5;
                if (contact.getEmail() != null && contact.getEmail().contains(modifyString) && filteredList.size() < contactLimit)
                    filteredList.add(contact);
            }
        }
        mAdapter.setListData(filteredList);
        mAdapter.getFilter().filter(modifyString);

        checkIfDataExist();
    }

    private void checkIfDataExist() {
        boolean checkStatus = getCheckStatus();
        if (checkStatus) {
            isEnableCreateAvailability = true;
            invalidateOptionsMenu();
        } else {
            isEnableCreateAvailability = false;
            invalidateOptionsMenu();
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void selectTime(String time, int position, int seconds) {
        mSelectTime.setText(time);
        mSelectTime.setTag(R.string.tag_position, position);
        mSelectTime.setTag(R.string.tag_seconds, seconds);
    }

    @Override
    public void onTokenAdded(Object token) {
        checkIfDataExist();
    }

    @Override
    public void onTokenRemoved(Object token) {
        checkIfDataExist();
    }
}
