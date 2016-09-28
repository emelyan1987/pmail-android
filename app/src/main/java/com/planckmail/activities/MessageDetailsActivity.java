package com.planckmail.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.planckmail.R;
import com.planckmail.adapters.RecycleMessageAdapter;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.enums.Folders;
import com.planckmail.utils.BundleKeys;
import com.planckmail.utils.JsonUtilFactory;
import com.planckmail.web.request.nylas.UpdateTag;
import com.planckmail.web.response.nylas.Message;
import com.planckmail.web.restClient.api.AuthNylasApi;
import com.planckmail.web.restClient.RestWebClient;
import com.planckmail.web.restClient.TypedJsonString;
import com.planckmail.web.restClient.service.NylasService;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedInput;

/**
 * Created by Taras Matolinets on 23.05.15.
 */
public class MessageDetailsActivity extends BaseActivity {
    public static final int REQUEST_CODE = 222;

    private RecyclerView mRecycleMessages;
    private Message mMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_message_details);

        initViews();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecycleMessages.setLayoutManager(layoutManager);
        mRecycleMessages.setHasFixedSize(true);

        String jsonMessage = getIntent().getStringExtra(BundleKeys.MESSAGES);

        mMessage = JsonUtilFactory.getJsonUtil().fromJson(jsonMessage, Message.class);

        List<Message> list = new ArrayList<>();
        list.add(mMessage);

        RecycleMessageAdapter mAdapter = new RecycleMessageAdapter(this, list);
        mRecycleMessages.setAdapter(mAdapter);

        enableActionBar();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                String text = data.getStringExtra(BundleKeys.TOAST_TEXT);
                Snackbar.make(mRecycleMessages, text, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void enableActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.message_details);
        }
    }

    private void initViews() {
        mRecycleMessages = (RecyclerView) findViewById(R.id.recycleMessageDetails);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_message_details, menu);

        int childId = getIntent().getIntExtra(BundleKeys.CHILD_ID, -1);

        if (childId != 0) {
            MenuItem delete = menu.getItem(0);
            delete.setVisible(false);
            MenuItem archive = menu.getItem(1);
            archive.setVisible(false);

        }
        return true;
    }

    public void moveMessage(final int tost, String tagJson) {
        String accessToken = getIntent().getStringExtra(BundleKeys.ACCESS_TOKEN);

        final NylasService nylasServer = AuthNylasApi.createService(NylasService.class, RestWebClient.BASE_URL, accessToken, "");

        final TypedInput in = new TypedJsonString(tagJson);
        nylasServer.updateRemoveThread(mMessage.thread_id, in, new Callback<Object>() {
            @Override
            public void success(Object o, Response response) {
                Log.d(PlanckMailApplication.TAG, response.getUrl());
                Intent intent = new Intent();
                intent.putExtra(BundleKeys.TOAST_TEXT, getResources().getString(tost));
                setResult(RESULT_OK, intent);
                finish();
            }

            @Override
            public void failure(RetrofitError error) {
                Response r = error.getResponse();
                if (r != null)
                    Log.e(PlanckMailApplication.TAG, "error: " + r.getReason());
            }
        });
    }

    private String updateTag(Folders currentTag) {
        UpdateTag tag = new UpdateTag();

        tag.add_tags.add(currentTag.toString());
        tag.remove_tags.add(Folders.INBOX.toString());

        return JsonUtilFactory.getJsonUtil().toJson(tag);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.delete:
                String tagJson = updateTag(Folders.TRASH);
                moveMessage(R.string.deleteMessage, tagJson);
                break;

            case R.id.archive:
                String tagArchived = updateTag(Folders.ALL_MAIL);
                moveMessage(R.string.archivedMessage, tagArchived);
                break;

        }
        return super.onOptionsItemSelected(item);
    }
}
