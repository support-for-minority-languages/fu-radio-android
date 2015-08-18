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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.udmspell.furadio.models.Song;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

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

        Intent intentPreparePlay = new Intent(Consts.RECEIVER_ACTION);
        intentPreparePlay.putExtra(Consts.PLAYER_COMMAND, Consts.PlayerCommands.PREPARE);
        sendBroadcast(intentPreparePlay);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String stationUrl = sharedPreferences.getString(Consts.SAVED_URL, getString(R.string.start_radio_url));
        playRadio(stationUrl);

        controlReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String command = intent.getStringExtra(Consts.PLAYER_COMMAND);
                if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                    stopSelf();
                } else {
                    if (command.equals(Consts.PlayerCommands.PAUSE)) {
                        stopRadio();
                    }
                }
            }

        };
        IntentFilter intentFilter = new IntentFilter(Consts.RECEIVER_ACTION);
        intentFilter.addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        this.registerReceiver(controlReceiver, intentFilter);

        setPhoneStateListener();

        makeNotification();
    }

    private void setPhoneStateListener() {
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    //Incoming call: Pause music
                    stopSelf();
                } else if(state == TelephonyManager.CALL_STATE_IDLE) {
                    //Not in call: Play music
                } else if(state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    //A call is dialing, active or on hold
                }
                super.onCallStateChanged(state, incomingNumber);
            }
        };
        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if(mgr != null) {
            mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private void playRadio(String stationUrl) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        sharedPreferences.edit().putString(Consts.PLAYER_COMMAND, Consts.PlayerCommands.PREPARE).apply();

//        new setSongInfo().execute(stationUrl);

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(stationUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                Log.d(Consts.LOG_TAG, "OnlineRadioService: onBufferingUpdate");
            }
        });
        mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                Log.d(Consts.LOG_TAG, "OnlineRadioService: onInfo: what=" + what + ", extra=" + extra);
                switch (what) {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        Intent intent = new Intent(Consts.RECEIVER_ACTION);
                        intent.putExtra(Consts.PLAYER_COMMAND, Consts.PlayerCommands.PREPARE);
                        sendBroadcast(intent);
                        break;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        intent = new Intent(Consts.RECEIVER_ACTION);
                        intent.putExtra(Consts.PLAYER_COMMAND, Consts.PlayerCommands.IS_PLAYING);
                        sendBroadcast(intent);
                        break;
                }
                return false;
            }
        });
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

//        String artist =  metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
//        String title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
//        Log.d(Consts.LOG_TAG, "OnlineRadioService: artist=" + artist + ", title=" + title);
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
        if (intent != null) {
            if (intent.getAction() != null && intent.getAction().equals(Consts.STOP_SERVICE)) {
                stopSelf();
            }
        } else {
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
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.play);
        Notification.Builder builder = new Notification.Builder(context)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.notif_icon)
                .setLargeIcon(largeIcon)
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

    private class setSongInfo extends AsyncTask<String, Void, Song> {

        @Override
        protected Song doInBackground(String... urls) {
            IcyStreamMeta icy = null;
            Song song = null;
            try {
                URL url = new URL(urls[0]);
                icy = new IcyStreamMeta(url);
                song = new Song(icy.getArtist(), icy.getTitle());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return song;
        }

        @Override
        protected void onPostExecute(Song song) {
            Log.d(Consts.LOG_TAG, "OnlineRadioService: artist=" + song.artist + ", title=" + song.title);
        }
    }

}
