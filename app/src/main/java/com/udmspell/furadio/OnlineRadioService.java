package com.udmspell.furadio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

/**
 * Created by ggrigoryev on 21.05.15.
 */
public class OnlineRadioService extends Service {
    ControlReceiver controlReceiver;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        controlReceiver = new ControlReceiver();
        IntentFilter intentFilter = new IntentFilter(Consts.RECEIVER_ACTION);
        this.registerReceiver(controlReceiver, intentFilter);
        makeNotification();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        controlReceiver.stopRadio();
        unregisterReceiver(controlReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void makeNotification() {
        int icon = R.drawable.icon; // icon from resources
        CharSequence tickerText = getString(R.string.app_name); // ticker-text
        long when = System.currentTimeMillis(); // notification time
        Context context = getApplicationContext(); // application Context
        CharSequence contentText = getString(R.string.app_name); // expanded

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification;
        Notification.Builder builder = new Notification.Builder(context);
        builder.setContentIntent(contentIntent)
                .setSmallIcon(icon)
                .setWhen(when)
                .setContentTitle(contentText);
        notification = builder.build();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Consts.NOTIFICATION_ID, notification);
    }

}
