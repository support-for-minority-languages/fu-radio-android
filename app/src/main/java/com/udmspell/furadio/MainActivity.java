package com.udmspell.furadio;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.skyfishjy.library.RippleBackground;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends Activity implements TagCloudView.TagCallback {
    private TagCloudView mTagCloudView;

    private boolean playing = false;
    private RippleBackground rippleBackground;
    private ImageView imageView;
    private SharedPreferences sharedPreferences;
    private BroadcastReceiver updateReceiver;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

        StationsService service = getStationsService();
        loadingStationsAnimation();
        service.getStations(new Callback<List<Tag>>() {
            @Override
            public void success(List<Tag> tags, Response response) {
                onLoadingSuccess(tags);
            }

            @Override
            public void failure(RetrofitError error) {
                Toast.makeText(MainActivity.this, getString(R.string.loading_stations_error), Toast.LENGTH_SHORT).show();
                onLoadingSuccess(createTags());
            }
        });

		rippleBackground=(RippleBackground)findViewById(R.id.content);
        imageView=(ImageView)findViewById(R.id.centerImage);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (playing) {
                    stopRadioService();
                } else {
                    startRadioService();
                }
            }
        });

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String command = intent.getStringExtra(Consts.PLAYER_COMMAND);

                setPlayerAnimation(command);
            }
        };
        IntentFilter intentFilter = new IntentFilter(Consts.RECEIVER_ACTION);
        this.registerReceiver(updateReceiver, intentFilter);
	}

    private void onLoadingSuccess(List<Tag> tags) {
        LinearLayout container = (LinearLayout) findViewById(R.id.tagCloud);
        container.setVisibility(View.VISIBLE);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        mTagCloudView = new TagCloudView(this, width, width, tags, 0, 40); // passing
        mTagCloudView.requestFocus();
        mTagCloudView.setFocusableInTouchMode(true);
        container.addView(mTagCloudView);

        setTitle(sharedPreferences.getString(Consts.STATION_TITLE, getString(R.string.radio_title)));

        String command = sharedPreferences.getString(Consts.PLAYER_COMMAND, Consts.PlayerCommands.PAUSE);
        setPlayerAnimation(command);
    }

    private void loadingStationsAnimation() {
        Toast.makeText(this, getString(R.string.loading_stations), Toast.LENGTH_SHORT).show();
    }

    private StationsService getStationsService() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(Consts.ENDPOINT)
                .build();

        return restAdapter.create(StationsService.class);
    }

    private void setPlayerAnimation(String command) {
        switch (command) {
            case Consts.PlayerCommands.PREPARE:
                preparePlayAnimation();
                break;
            case Consts.PlayerCommands.IS_PLAYING:
                startPlayingAnimation();
                break;
            case Consts.PlayerCommands.PAUSE:
                stopPlayingAnimation();
                break;
        }
    }

    private void preparePlayAnimation() {
        playing = true;
        // индикация загрузки
        Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scalerepeat);
        imageView.startAnimation(scaleAnimation);
        imageView.setImageResource(R.drawable.pause);
    }

    private void startPlayingAnimation() {
        playing = true;
        // остановка индикации загрузки
        imageView.clearAnimation();
        imageView.setImageResource(R.drawable.pause);
        // начало анимации воспроизвдения
        rippleBackground.startRippleAnimation();
    }

    private void stopPlayingAnimation() {
        playing = false;
        // остановка индикации загрузки
        imageView.clearAnimation();
        rippleBackground.stopRippleAnimation();
        imageView.setImageResource(R.drawable.play);
    }

    private void startRadioService() {
        Log.d(Consts.LOG_TAG, "MainActivity: startRadioService");
        startService(new Intent(this, OnlineRadioService.class));
    }

    private void stopRadioService() {
        Log.d(Consts.LOG_TAG, "MainActivity: stopRadioService");
        stopService(new Intent(this, OnlineRadioService.class));
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
        stopRadioService();

        Toast.makeText(this, getString(R.string.loading), Toast.LENGTH_SHORT).show();
        setTitle(title);

        sharedPreferences.edit().putString(Consts.SAVED_URL, url).apply();
        sharedPreferences.edit().putString(Consts.STATION_TITLE, getTitle().toString()).apply();

        startRadioService();
    }

}

