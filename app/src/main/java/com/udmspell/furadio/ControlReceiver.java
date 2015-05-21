package com.udmspell.furadio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;

import java.io.IOException;

/**
 * Created by ggrigoryev on 21.05.15.
 */
public class ControlReceiver extends BroadcastReceiver {
    private MediaPlayer mediaPlayer;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        String command = intent.getStringExtra(Consts.PLAYER_COMMAND);
        switch (command) {
            case Consts.PlayerCommands.PREPARE:
                String stationUrl = intent.getStringExtra(Consts.STATION_URL);
                playRadio(stationUrl);
                break;
            case Consts.PlayerCommands.PAUSE:
                stopRadio();
                break;
        }
    }

    private void playRadio(String stationUrl) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(stationUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.prepareAsync();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Intent intent = new Intent(Consts.RECEIVER_ACTION);
                intent.putExtra(Consts.PLAYER_COMMAND, Consts.PlayerCommands.PLAY);
                context.sendBroadcast(intent);
                mediaPlayer.start();
            }
        });
    }

    public void stopRadio() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

}
