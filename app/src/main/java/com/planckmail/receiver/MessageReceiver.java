package com.planckmail.receiver;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.planckmail.activities.MenuActivity;
import com.planckmail.application.PlanckMailApplication;
//import com.planckmail.service.MessageService;
import com.planckmail.utils.BundleKeys;

import java.util.ArrayList;

/**
 * Created by Taras Matolinets on 07.07.15.
 */
public class MessageReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
//        if (intent.getAction() != null && intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
//            Intent myIntent = new Intent(context, MessageReceiver.class);
//
//            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
//            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
//            alarmManager.setInexactRepeating(AlarmManager.RTC, SystemClock.elapsedRealtime(), 5000, pendingIntent);
//        }
//        Intent intentService = new Intent(context, MessageService.class);
//        context.startService(intentService);
    }
}
