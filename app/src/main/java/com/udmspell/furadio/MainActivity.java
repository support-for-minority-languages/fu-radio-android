package com.udmspell.furadio;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.skyfishjy.library.RippleBackground;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements TagCloudView.TagCallback {
    private static final String STATION_TITLE = "station_title";
    private static final String SAVED_URL = "saved_url";

    private TagCloudView mTagCloudView;

    private boolean playing = false;
    private RippleBackground rippleBackground;
    private ImageView imageView;
    private SharedPreferences sPref;
    private BroadcastReceiver updateReceiver;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		LinearLayout container = (LinearLayout) findViewById(R.id.tagCloud);

		mTagCloudView = new TagCloudView(this, 720, 720, createTags(), 0, 40); // passing
		mTagCloudView.requestFocus();
		mTagCloudView.setFocusableInTouchMode(true);
		container.addView(mTagCloudView);

        rippleBackground=(RippleBackground)findViewById(R.id.content);
        imageView=(ImageView)findViewById(R.id.centerImage);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (playing) {
                    Intent intent = new Intent(Consts.RECEIVER_ACTION);
                    intent.putExtra(Consts.PLAYER_COMMAND, Consts.PlayerCommands.PAUSE);
                    sendBroadcast(intent);
                } else {
                    String savedUrl = sPref.getString(SAVED_URL, "http://radio.myudm.ru:10000/udm");
                    Intent intent = new Intent(Consts.RECEIVER_ACTION);
                    intent.putExtra(Consts.PLAYER_COMMAND, Consts.PlayerCommands.PREPARE);
                    intent.putExtra(Consts.STATION_URL, savedUrl);
                    sendBroadcast(intent);
                }
            }
        });

        sPref = getPreferences(MODE_PRIVATE);
        setTitle(sPref.getString(STATION_TITLE, "Радио"));

        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String command = intent.getStringExtra(Consts.PLAYER_COMMAND);

                switch (command) {
                    case Consts.PlayerCommands.PREPARE:
                        preparePlay();
                        break;
                    case Consts.PlayerCommands.PLAY:
                        startPlaying();
                        break;
                    case Consts.PlayerCommands.PAUSE:
                        stopPlaying();
                        break;
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(Consts.RECEIVER_ACTION);
        this.registerReceiver(updateReceiver, intentFilter);
        this.startService(new Intent(this, OnlineRadioService.class));
	}

    private void preparePlay() {
        playing = true;
        // индикация загрузки
        Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scalerepeat);
        imageView.startAnimation(scaleAnimation);
        imageView.setImageResource(R.drawable.pause);
    }

    private void startPlaying() {
        playing = true;
        // остановка индикации загрузки
        imageView.clearAnimation();
        // начало анимации воспроизвдения
        rippleBackground.startRippleAnimation();
    }

    private void stopPlaying() {
        playing = false;
        // остановка индикации загрузки
        imageView.clearAnimation();
        rippleBackground.stopRippleAnimation();
        imageView.setImageResource(R.drawable.play);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(updateReceiver);
    }

	private List<Tag> createTags() {
		// create the list of tags with popularity values and related url
		List<Tag> tempList = new ArrayList<>();

		tempList.add(new Tag("Моя Удмуртия", 7, "http://radio.myudm.ru:10010/mp3"));
		tempList.add(new Tag("Удмуртское радио", 6, "http://radio.myudm.ru:10000/udm"));
		tempList.add(new Tag("Марий Эл радио", 5, "http://r1.vmariel.ru:8000/mariel"));
		tempList.add(new Tag("Коми Народное радио", 5, "http://188.94.173.197:4321/kominarodnoeradio.mp3"));
		tempList.add(new Tag("RadioRock", 7, "http://rstream2.nelonenmedia.fi/Radiorock.mp3"));

		return tempList;
	}

    @Override
    public void onClick(String title, String url) {
        Toast.makeText(this, "Загрузка...", Toast.LENGTH_SHORT).show();
        setTitle(title);
//        stopPlaying();
//        startPlaying();
        Intent intent = new Intent(Consts.RECEIVER_ACTION);
        intent.putExtra(Consts.PLAYER_COMMAND, Consts.PlayerCommands.PREPARE);
        intent.putExtra(Consts.STATION_URL, url);
        this.sendBroadcast(intent);
        sPref.edit().putString(SAVED_URL, url).apply();
        sPref.edit().putString(STATION_TITLE, getTitle().toString()).apply();
    }

}

