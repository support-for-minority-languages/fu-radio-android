package com.udmspell.furadio;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.skyfishjy.library.RippleBackground;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements TagCloudView.TagCallback {
    private static final String STATION_TITLE = "station_title";
    private static final String SAVED_URL = "saved_url";
    private TagCloudView mTagCloudView;
	private final String TAG = "cloudtag";
    private MediaPlayer player;
    private boolean loading;
    private RippleBackground rippleBackground;
    private ImageView imageView;
    private SharedPreferences sPref;

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

                if (loading) {
                    stopPlaying();
                    return;
                }

                if (player != null) {
                    if (player.isPlaying()) {
                        stopPlaying();
                    } else {
                        String savedUrl = sPref.getString(SAVED_URL, "");
                        if (!savedUrl.equals("")) {
                            startPlaying(savedUrl);
                        }  else {
                            startPlaying("http://radio.myudm.ru:10000/udm");
                        }

                    }
                }
            }
        });

        sPref = getPreferences(MODE_PRIVATE);
        setTitle(sPref.getString(STATION_TITLE, "Радио"));

        player = new MediaPlayer();

        player.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
//                selectedView.setValue(percent);
                Log.i("Buffering", "" + percent);
            }
        });
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player.isPlaying()) {
            player.stop();
            player.release();
        }
    }

//	public boolean dispatchTouchEvent(MotionEvent e) {
//
//		boolean result = mTagCloudView.dispatchTouchEvent(e);
//		// boolean result = true;
//		Log.d(TAG, getTime() + "super dispatching ... result is [" + result + "] action is [" + e.getAction() + "]");
//		return result;
//	}

	private String getTime() {

		return "[" + System.currentTimeMillis() + "] ";
	}

//	public boolean onTouchEvent(MotionEvent e) {
//		Log.d(TAG, getTime() + "super movition:x=" + e.getX() + ",y=" + e.getY() + ",action is [" + e.getAction() + "]");
//		return mTagCloudView.onTouchEvent(e);
//	}

	private List<Tag> createTags() {
		// create the list of tags with popularity values and related url
		List<Tag> tempList = new ArrayList<Tag>();

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
        stopPlaying();
        startPlaying(url);
        sPref.edit().putString(SAVED_URL, url).commit();
        sPref.edit().putString(STATION_TITLE, getTitle().toString()).commit();
    }

    private void startPlaying(String stationUrl) {
        loading = true;
        try {
            player.setDataSource(stationUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.prepareAsync();
        imageView.setImageResource(R.drawable.pause);
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            public void onPrepared(MediaPlayer mp) {
                player.start();
                loading = false;
                rippleBackground.startRippleAnimation();
            }
        });

    }

    private void stopPlaying() {
        loading = false;
        if (player != null) {
            rippleBackground.stopRippleAnimation();
            imageView.setImageResource(R.drawable.play);

            player.stop();
            player.release();
            player = new MediaPlayer();
        }
    }

}

