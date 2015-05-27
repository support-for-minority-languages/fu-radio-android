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
import android.os.Build;
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

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Consts.LOG_TAG, "OnlineRadioService: onCreate");

        Intent intentPreparePlay = new Intent(Consts.RECEIVER_ACTION);
        intentPreparePlay.putExtra(Consts.PLAYER_COMMAND, Consts.PlayerCommands.PREPARE);
        sendBroadcast(intentPreparePlay);

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

        makeNotification();
    }

    private void playRadio(String stationUrl) {
        if (mediaPlayer != null) {
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
            }
        });
        mediaPlayer.prepareAsync();
    }

    public void stopRadio() {
        sharedPreferences.edit().putString(Consts.PLAYER_COMMAND, Consts.PlayerCommands.PAUSE).apply();
        if (mediaPlayer != null) {
            Log.d(Consts.LOG_TAG, "OnlineRadioService: player stoped");
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

    }

    @Override
    public void onDestroy() {
        Log.d(Consts.LOG_TAG, "OnlineRadioService: onDestroy");
        super.onDestroy();
        stopRadio();

        Intent receiverIntent = new Intent(Consts.RECEIVER_ACTION);
        receiverIntent.putExtra(Consts.PLAYER_COMMAND, Consts.PlayerCommands.PAUSE);
        sendBroadcast(receiverIntent);

        unregisterReceiver(controlReceiver);
        notificationManager.cancel(Consts.NOTIFICATION_ID);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() == Consts.STOP_SERVICE) {
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void makeNotification() {
        Context context = getApplicationContext();
        String notificationTitle = getString(R.string.app_name);
        String station = sharedPreferences.getString(Consts.STATION_TITLE, getString(R.string.radio_title));

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Intent serviceIntent = new Intent(this, OnlineRadioService.class);
        serviceIntent.setAction(Consts.STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(context, 0, serviceIntent, 0);
        Notification.Builder builder = new Notification.Builder(context)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.play)
                .setContentTitle(notificationTitle)
                .setContentText(station)
                .setDeleteIntent(stopPendingIntent)
                .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setStyle(new Notification.MediaStyle()
                            .setShowActionsInCompactView(0));
        }
        Notification notification = builder.build();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Consts.NOTIFICATION_ID, notification);
    }


}
