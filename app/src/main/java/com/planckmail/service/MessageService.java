package com.planckmail.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.planckmail.R;
import com.planckmail.activities.MenuActivity;
import com.planckmail.application.PlanckMailApplication;
import com.planckmail.web.response.nylas.wrapper.Attributes;
import com.planckmail.web.response.nylas.wrapper.Participant;

import java.util.List;

/**
 * Created by Taras Matolinets on 07.07.15.
 */
public class MessageService extends GcmListenerService {

    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void onMessageReceived(String from, Bundle data) {
        String json = data.getString("data");
        Log.d(PlanckMailApplication.TAG, "++++++" + json);
        sendNotification(json);
//        List<Attributes> list = JsonUtilFactory.getJsonUtil().fromJsonArray(json, Attributes.class);
//        buildNotification(list);
    }

    public void buildNotification(List<Attributes> list) {
        Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
        PendingIntent pendingNotificationIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Attributes lastAttribute = list.get(list.size() - 1);

        String contextTitle = createContextMessage(list, lastAttribute);

        NotificationCompat.Builder builder = getBuilder(list, pendingNotificationIntent, lastAttribute, contextTitle);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        //default subject
        inboxStyle.setBigContentTitle(lastAttribute.attributes.subject);

        for (Attributes attribute : list)
            inboxStyle.addLine(attribute.attributes.snippet);

        builder.setStyle(inboxStyle);

        notificationManager.notify(0, builder.build());
    }

    private void sendNotification(String message) {
        Intent intent = new Intent(this, MenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_planck_notification_icon)
                .setContentTitle("GCM Message")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setColor(getResources().getColor(R.color.primaryGreen))
                .setLights(Color.GREEN, 500, 1000)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* THREAD_ID of notification */, notificationBuilder.build());
    }


    public NotificationCompat.Builder getBuilder(List<Attributes> list, PendingIntent pendingNotificationIntent, Attributes lastThread, String contextTitle) {
        return new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_planck_notification_icon)
                .setContentTitle(contextTitle)
                .setContentText(lastThread.attributes.subject)
                .setNumber(list.size())
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .setColor(getResources().getColor(R.color.primaryGreen))
                .setLights(Color.GREEN, 500, 1000)
                .setContentIntent(pendingNotificationIntent);
    }

    public String createContextMessage(List<Attributes> list, Attributes lastAttribute) {
        String contextTitle;
        if (!lastAttribute.attributes.participants.isEmpty()) {
            Participant participants = lastAttribute.attributes.participants.get(lastAttribute.attributes.participants.size() - 1);
            contextTitle = participants.email;
        } else {
            if (list.size() == 1)
                contextTitle = getResources().getString(R.string.newMessageReceive);
            else
                contextTitle = getResources().getString(R.string.newMessagesReceive);
        }
        return contextTitle;
    }
}
