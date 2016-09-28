package com.planckmail.tasks;

import com.planckmail.data.db.beans.MessageDB;
import com.planckmail.data.db.beans.ParticipantDB;
import com.planckmail.data.db.beans.ThreadDB;
import com.planckmail.data.db.manager.DataBaseManager;
import com.planckmail.data.db.manager.ThreadDataManager;
import com.planckmail.web.response.nylas.Message;
import com.planckmail.web.response.nylas.Thread;
import com.planckmail.web.response.nylas.wrapper.Participant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.fabric.sdk.android.services.concurrency.AsyncTask;

/**
 * Created by Taras Matolinets on 24.09.15.
 */
public class AsyncLoadThreadDB extends AsyncTask<Void, Void, List<Thread>> {
    private ILoadedThread mListener;

    public AsyncLoadThreadDB(ILoadedThread listener) {
        mListener = listener;
    }

    @Override
    protected List<Thread> doInBackground(Void... params) {
        return showThreadsFromDb();
    }

    @Override
    protected void onPostExecute(List<Thread> list) {
        if (mListener != null)
            mListener.loadedListThreads(list);
    }

    public List<Thread> showThreadsFromDb() {
        ThreadDataManager manager = (ThreadDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.THREAD_MANAGER);
        List<ThreadDB> list = manager.getThreadList();

        final ArrayList<Thread> mListThreadDb = new ArrayList<>();

        for (ThreadDB t : list) {
            Collection<ParticipantDB> listParticipantDb = t.getParticipants();
            List<Participant> listParticipant = createParticipantsList(listParticipantDb);
            List<Message> listMessage = createMessageDbList(t.getMessageList());

            Thread thread = createThread(t);

            thread.setListMessage(listMessage);
            thread.setParticipants(listParticipant);
            mListThreadDb.add(thread);
        }

        return mListThreadDb;
    }

    public Thread createThread(ThreadDB t) {
        Thread thread = new Thread();
        thread.snippet = t.snippet;
        thread.first_message_timestamp = t.first_message_timestamp;
        thread.last_message_timestamp = t.last_message_timestamp;
        thread.object = t.object;
        thread.id = t.id;
        thread.version = t.version;
        thread.subject = t.subject;
        thread.account_id = t.account_id;

        return thread;
    }

    public List<Participant> createParticipantsList(Collection<ParticipantDB> listParticipantDb) {
        List<Participant> listParticipant = new ArrayList<>();
        for (ParticipantDB p : listParticipantDb) {
            Participant participant = new Participant();

            participant.email = p.email;
            participant.name = p.name;

            listParticipant.add(participant);
        }
        return listParticipant;
    }

    private List<Message> createMessageDbList(Collection<MessageDB> list) {
        List<Message> listMessage = new ArrayList<>();

        for (MessageDB messageDB : list) {
            Message message = new Message();

            message.from = createParticipantsList(messageDB.getFrom());
            message.replyTo = createParticipantsList(messageDB.getReplyTo());
            message.to = createParticipantsList(messageDB.getTo());
            message.bcc = createParticipantsList(messageDB.getBcc());
            message.cc = createParticipantsList(messageDB.getCc());

            message.id = messageDB.getId();
            message.object = messageDB.getObject();
            message.thread_id = messageDB.getThread_id();
            message.snippet = messageDB.getSnippet();
            message.subject = messageDB.getSubject();
            message.date = messageDB.getDate();
            message.body = messageDB.getBody();
            message.unread = messageDB.isUnread();

            listMessage.add(message);
        }
        return listMessage;
    }

    public interface ILoadedThread {
        void loadedListThreads(List<Thread> listThread);
    }
}
