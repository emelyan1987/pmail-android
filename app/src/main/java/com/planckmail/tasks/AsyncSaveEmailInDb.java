package com.planckmail.tasks;

import android.util.Log;

import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.MessageDB;
import com.planckmail.data.db.beans.ParticipantDB;
import com.planckmail.data.db.beans.ThreadDB;
import com.planckmail.data.db.manager.DataBaseManager;
import com.planckmail.data.db.manager.MessageDataManager;
import com.planckmail.data.db.manager.ParticipantDataManager;
import com.planckmail.data.db.manager.ThreadDataManager;

import java.util.List;

import io.fabric.sdk.android.services.concurrency.AsyncTask;

/**
 * Created by Taras Matolinets on 24.09.15.
 */
public class AsyncSaveEmailInDb extends AsyncTask {
    List<ThreadDB> list;

    public AsyncSaveEmailInDb(List<ThreadDB> list) {
        this.list = list;
    }

    @Override
    protected Object doInBackground(Object[] objects) {
        saveThreadInDB(list);
        return null;
    }
    public void saveThreadInDB(List<ThreadDB> list) {
        ParticipantDataManager participantManager = (ParticipantDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.PARTICIPANT_MANAGER);
        ThreadDataManager threadManager = (ThreadDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.THREAD_MANAGER);

        for (ThreadDB thread : list) {
            threadManager.createThread(thread);

            for (ParticipantDB p : thread.getParticipants()) {
                p.setThreadDB(thread);
                participantManager.createParticipant(p);
            }

            createMessageObject(participantManager, thread);
        }
        List<ThreadDB> listDb = threadManager.getThreadList();
        Log.d(PlanckMailApplication.TAG, "size db " + String.valueOf(listDb.size()));
    }

    private void createMessageObject(ParticipantDataManager participantManager, ThreadDB thread) {
        MessageDataManager messageManager = (MessageDataManager) DataBaseManager.getInstanceDataManager().getCurrentManager(DataBaseManager.DataManager.MESSAGER_MANAGER);

        for (MessageDB m : thread.getMessageList()) {

            m.setThreadDB(thread);
            messageManager.createMessage(m);

            for (ParticipantDB p : m.getBcc()) {
                p.setMessageDB(m);
                participantManager.createParticipant(p);
            }

            for (ParticipantDB p : m.getCc()) {
                p.setMessageDB(m);
                participantManager.createParticipant(p);
            }

            for (ParticipantDB p : m.getTo()) {
                p.setMessageDB(m);
                participantManager.createParticipant(p);
            }

            for (ParticipantDB p : m.getFrom()) {
                p.setMessageDB(m);
                participantManager.createParticipant(p);
            }
        }
    }
}
