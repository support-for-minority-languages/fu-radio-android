package com.udmspell.furadio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;

/**
 * Created by ggrigoryev on 21.05.15.
 */
public class OnlineRadioService extends Service {
    private NotificationManager notificationManager;
    private BroadcastReceiver controlReceiver;
    private MediaPlayer mediaPlayer;
    private SharedPreferences sharedPreferences;
    private boolean isPreparing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Consts.LOG_TAG, "OnlineRadioService: onCreate");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String stationUrl = sharedPreferences.getString(Consts.SAVED_URL, getString(R.string.start_radio_url));
        Log.d(Consts.LOG_TAG, "ControlReceiver: stationUrl=" + stationUrl);
        playRadio(stationUrl);

        controlReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String command = intent.getStringExtra(Consts.PLAYER_COMMAND);
                Log.d(Consts.LOG_TAG, "ControlReceiver: command=" + command);
                if (command == Consts.PlayerCommands.PAUSE) {
                        stopRadio();
                }
            }

        };
        IntentFilter intentFilter = new IntentFilter(Consts.RECEIVER_ACTION);
        this.registerReceiver(controlReceiver, intentFilter);

//        controlReceiver = new ControlReceiver();
//        IntentFilter intentFilter = new IntentFilter(Consts.RECEIVER_ACTION);
//        this.registerReceiver(controlReceiver, intentFilter);
        makeNotification();
    }

    private void playRadio(String stationUrl) {
        if (mediaPlayer != null && !isPreparing) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        sharedPreferences.edit().putString(Consts.PLAYER_COMMAND, Consts.PlayerCommands.PREPARE).apply();
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(stationUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                sharedPreferences.edit().putString(Consts.PLAYER_COMMAND, Consts.PlayerCommands.IS_PLAYING).apply();
                Intent intent = new Intent(Consts.RECEIVER_ACTION);
                intent.putExtra(Consts.PLAYER_COMMAND, Consts.PlayerCommands.IS_PLAYING);
                sendBroadcast(intent);
                mediaPlayer.start();
                isPreparing = false;
            }
        });
        mediaPlayer.prepareAsync();
        isPreparing = true;
    }

    public void stopRadio() {
        sharedPreferences.edit().putString(Consts.PLAYER_COMMAND, Consts.PlayerCommands.PAUSE).apply();
        if (mediaPlayer != null && !isPreparing) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRadio();
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
