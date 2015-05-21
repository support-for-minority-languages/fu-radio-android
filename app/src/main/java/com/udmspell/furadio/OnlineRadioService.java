package com.udmspell.furadio;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

/**
 * Created by ggrigoryev on 21.05.15.
 */
public class OnlineRadioService extends Service {
    ControlReceiver controlReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        controlReceiver = new ControlReceiver();
        IntentFilter intentFilter = new IntentFilter(Consts.RECEIVER_ACTION);
        this.registerReceiver(controlReceiver, intentFilter);
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


}
