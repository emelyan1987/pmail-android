package com.planckmail.data.db.manager;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.data.db.beans.ParticipantDB;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Taras Matolinets on 24.07.15.
 */
public class ParticipantDataManager extends DataBaseManager {

    private final Context mContext;

    public ParticipantDataManager(Context ctx) {
        mContext = ctx;
    }

    public void createParticipant(ParticipantDB participant) {
        try {
            mHelper.getParticipantDB().create(participant);
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
    }

    public void updateParticipantList(ParticipantDB participantDB) {
        try {
            mHelper.getParticipantDB().update(participantDB);
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
    }

    public List<ParticipantDB> getParticipantList() {
        List<ParticipantDB> list = new ArrayList<>();
        try {
            list = mHelper.getParticipantDB().queryForAll();
        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
        return list;
    }


    public void deleteParticipant() {
        List<ParticipantDB> listParticipant;
        try {
            Dao<ParticipantDB, Integer> daoAccountInfoList = mHelper.getParticipantDB();
            listParticipant = daoAccountInfoList.queryForAll();

            daoAccountInfoList.delete(listParticipant);

        } catch (SQLException e) {
            Log.e(PlanckMailApplication.TAG, e.toString());
        }
    }

}
