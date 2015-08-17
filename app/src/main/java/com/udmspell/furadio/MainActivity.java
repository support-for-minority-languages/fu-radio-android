package com.udmspell.furadio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.skyfishjy.library.RippleBackground;
import com.udmspell.furadio.models.Station;
import com.udmspell.furadio.services.StationsService;

import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends AppCompatActivity implements TagCloudView.TagCallback {
    private TagCloudView mTagCloudView;

    private boolean playing = false;
    private RippleBackground rippleBackground;
    private ImageView centerImage;
    private ImageView centerImageLarge;
    private SharedPreferences sharedPreferences;
    private BroadcastReceiver updateReceiver;
    private TextView title;
    private Toolbar toolbar;
    private ViewGroup tagCloud;
    private boolean timeEscaped = false;
    private boolean stationsLoaded = false;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Fabric.with(this, new Crashlytics());
		setContentView(R.layout.main);
        toolbar = (Toolbar) findViewById(R.id.actionBar);
        setSupportActionBar(toolbar);
        title = (TextView) findViewById(R.id.title);
        tagCloud = (ViewGroup) findViewById(R.id.tagCloud);
		rippleBackground=(RippleBackground)findViewById(R.id.content);
        centerImageLarge =(ImageView)findViewById(R.id.centerImageLarge);
        centerImage =(ImageView)findViewById(R.id.centerImage);
        centerImage.setOnClickListener(new View.OnClickListener() {
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

        setStationTitle(sharedPreferences.getString(Consts.STATION_TITLE, getString(R.string.radio_title)));
        String command = sharedPreferences.getString(Consts.PLAYER_COMMAND, Consts.PlayerCommands.PAUSE);
        Log.d(Consts.LOG_TAG, "MainActivity: start command:" + command);
        if (command.equals(Consts.PlayerCommands.PAUSE)) {
            startStationsLoadAnim();
        } else {
            centerImageLarge.setVisibility(View.INVISIBLE);
            centerImage.setVisibility(View.VISIBLE);
            toolbar.setVisibility(View.VISIBLE);
            tagCloud.setVisibility(View.VISIBLE);
            setPlayerAnimation(command);
            onStartAfterClose();
        }

        StationsService service = getStationsService();
        service.getStations(new Callback<List<Station>>() {
            @Override
            public void success(List<Station> stations, Response response) {
                onLoadingSuccess(stations);
            }

            @Override
            public void failure(RetrofitError error) {
                Toast.makeText(MainActivity.this, getString(R.string.loading_stations_error), Toast.LENGTH_SHORT).show();
                onLoadingSuccess(createStations());
            }
        });

        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String command = intent.getStringExtra(Consts.PLAYER_COMMAND);
                if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                    command = Consts.PlayerCommands.PAUSE;
                }
                setPlayerAnimation(command);
            }
        };
        IntentFilter intentFilter = new IntentFilter(Consts.RECEIVER_ACTION);
        intentFilter.addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        this.registerReceiver(updateReceiver, intentFilter);
	}

    private void onLoadingSuccess(List<Station> stations) {

        List<Tag> savedTags = Tag.listAll(Tag.class);
        List<Tag> tags = new ArrayList<>();
        for (Station station : stations) {
            boolean findTag = false;
            Tag newTag = null;
            for (Tag tag: savedTags) {
                if (station.getStationId() == tag.getStationId()) {
                    findTag = true;
                    newTag = tag;
                    newTag.setText(station.getText());
                    newTag.setUrl(station.getUrl());
                    break;
                }
            }

            if (!findTag) {
                newTag = new Tag(station.getStationId(), station.getText(),
                        station.getPopularity(), station.getUrl());
                newTag.save();
            }
            tags.add(newTag);
        }

        stationsLoaded = true;
        if (timeEscaped) {
            stopStationsLoadAnim();
        }

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = (int) (size.x * Consts.CLOUD_WIDTH_SCALE);

        mTagCloudView = new TagCloudView(this, width, width, tags, 0, 30); // passing
        mTagCloudView.requestFocus();
        mTagCloudView.setFocusableInTouchMode(true);
        tagCloud.addView(mTagCloudView);
    }

    private void onStartAfterClose() {

        List<Tag> savedTags = Tag.listAll(Tag.class);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = (int) (size.x * Consts.CLOUD_WIDTH_SCALE);

        mTagCloudView = new TagCloudView(this, width, width, savedTags, 0, 30); // passing
        mTagCloudView.requestFocus();
        mTagCloudView.setFocusableInTouchMode(true);
        tagCloud.addView(mTagCloudView);
    }

    private void startStationsLoadAnim() {
        Toast.makeText(this, getString(R.string.loading_stations), Toast.LENGTH_LONG).show();

        Animation startAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.start_anim);
        centerImageLarge.startAnimation(startAnimation);

        Handler handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            public void run() {
                Animation scaleAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.scalerepeat);
                centerImageLarge.startAnimation(scaleAnimation);
            }
        }, 600L);

        Handler handler2 = new Handler();
        handler2.postDelayed(new Runnable() {
            public void run() {
                timeEscaped = true;
                if (stationsLoaded) {
                    stopStationsLoadAnim();
                }
            }
        }, Consts.START_DELAY);
    }

    private void stopStationsLoadAnim() {
        centerImageLarge.clearAnimation();

        Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.stations_load_scale);
        scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                centerImageLarge.setVisibility(View.INVISIBLE);
                centerImage.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        centerImageLarge.startAnimation(scaleAnimation);

        AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1f);
        alphaAnimation.setDuration(500L);
        alphaAnimation.setStartOffset(100L);
        alphaAnimation.setInterpolator(new DecelerateInterpolator());
        alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                toolbar.setVisibility(View.VISIBLE);
                tagCloud.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        tagCloud.startAnimation(alphaAnimation);
        toolbar.startAnimation(alphaAnimation);

    }

    private StationsService getStationsService() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(Consts.ENDPOINT)
                .build();

        return restAdapter.create(StationsService.class);
    }

    private void setStationTitle(String string) {
        title.setText(string);
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
        centerImage.startAnimation(scaleAnimation);
        centerImage.setImageResource(R.drawable.pause);
    }

    private void startPlayingAnimation() {
        playing = true;
        // остановка индикации загрузки
        centerImage.clearAnimation();
        centerImage.setImageResource(R.drawable.pause);
        // начало анимации воспроизвдения
        rippleBackground.startRippleAnimation();
    }

    private void stopPlayingAnimation() {
        playing = false;
        // остановка индикации загрузки
        centerImage.clearAnimation();
        rippleBackground.stopRippleAnimation();
        centerImage.setImageResource(R.drawable.play);
    }

    private void startRadioService() {
        startService(new Intent(this, OnlineRadioService.class));
    }

    private void stopRadioService() {
        stopPlayingAnimation();
        stopService(new Intent(this, OnlineRadioService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(updateReceiver);
    }

	private List<Station> createStations() {
		// create the list of tags with popularity values and related url
		List<Station> tempList = new ArrayList<>();

		tempList.add(new Station(1, "Моя Удмуртия", 7, "http://radio.myudm.ru:10010/mp3"));
		tempList.add(new Station(2, "Удмурт радио", 6, "http://radio.myudm.ru:10000/udm"));
		tempList.add(new Station(3, "Марий Эл радио", 5, "http://r1.vmariel.ru:8000/mariel"));
		tempList.add(new Station(4, "Коми Народное радио", 5, "http://188.94.173.197:4321/kominarodnoeradio.mp3"));
		tempList.add(new Station(5, "RadioRock", 7, "http://rstream2.nelonenmedia.fi/Radiorock.mp3"));

		return tempList;
	}

    @Override
    public void onClick(String title, String url) {
        stopRadioService();

        Toast.makeText(this, getString(R.string.loading), Toast.LENGTH_SHORT).show();
        setStationTitle(title);

        sharedPreferences.edit().putString(Consts.SAVED_URL, url).apply();
        sharedPreferences.edit().putString(Consts.STATION_TITLE, title).apply();

        startRadioService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
                return true;
            case R.id.exit:
                startRadioService();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

