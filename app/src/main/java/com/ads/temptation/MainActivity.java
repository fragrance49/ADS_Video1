package com.ads.temptation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.multidex.MultiDex;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity{

    private PlayerView playerView;
    private SimpleExoPlayer player;
    private ImaAdsLoader adsLoader;

    Vibrator vibrator;

    ArrayList<Model> models = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        MultiDex.install(this);

        setModel();

        playerView = findViewById(R.id.player_view);
        playerView.requestFocus();

        // Create an AdsLoader.
        adsLoader = new ImaAdsLoader.Builder(/* context= */ this).build();

    }
    private void releasePlayer() {
        adsLoader.setPlayer(null);
        playerView.setPlayer(null);
        player.release();
        player = null;
    }

    private void initializePlayer() {
        // Set up the factory for media sources, passing the ads loader and ad view providers.
        DataSource.Factory dataSourceFactory =
                new DefaultDataSourceFactory(this, Util.getUserAgent(this, getString(R.string.app_name)));

        MediaSourceFactory mediaSourceFactory =
                new DefaultMediaSourceFactory(dataSourceFactory)
                        .setAdsLoaderProvider(unusedAdTagUri -> adsLoader)
                        .setAdViewProvider(playerView);

        // Create a SimpleExoPlayer and set it as the player for content and ads.
        player = new SimpleExoPlayer.Builder(this).setMediaSourceFactory(mediaSourceFactory).build();
        playerView.setPlayer(player);
        adsLoader.setPlayer(player);

        // Create the MediaItem to play, specifying the content URI and ad tag URI.
        Uri contentUri=Uri.parse("android.resource://" + getPackageName() + "/" +
                R.raw.vid1);
        MediaItem mediaItem = new MediaItem.Builder().setUri(contentUri).build();

        // Prepare the content and ad to be played with the SimpleExoPlayer.
        player.setMediaItem(mediaItem);
        player.prepare();

        // Set PlayWhenReady. If true, content and ads will autoplay.
        player.setPlayWhenReady(false);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        player.addListener(new Player.DefaultEventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playWhenReady && playbackState == Player.STATE_READY) {

                    long currentTime = player.getCurrentPosition();
                    Log.i("Video==>", "play");

                    long[] vib_seq = {};
                    int i = 0;

                    while (i < models.size() && currentTime > models.get(i).time) i++;
                    if(i < models.size()){
                        vib_seq = new long[]{0, models.get(i).time - currentTime, models.get(i).level};
                        i++;
                        while (i < models.size()){
                            long[] toAppend = new long[]{models.get(i).time - models.get(i - 1).time - models.get(i - 1).level, models.get(i).level};
                            long[] tmp = new long[vib_seq.length + toAppend.length];
                            System.arraycopy(vib_seq, 0, tmp, 0, vib_seq.length);
                            System.arraycopy(toAppend, 0, tmp, vib_seq.length, toAppend.length);
                            vib_seq = tmp;
                            i++;
                        }
                        Log.i("vib_seq==>", String.valueOf(vib_seq.length));
                        vibrator.vibrate(vib_seq , -1);
                    }else {
                        vibrator.cancel();
                    }

                } else if (playWhenReady) {
                    Log.i("Video==>", "ready");
                    vibrator.cancel();
                    // might be idle (plays after prepare()),
                    // buffering (plays when data available)
                    // or ended (plays when seek away from end)
                } else {
                    vibrator.cancel();
                    Log.i("Video==>", "stop");
                    // player paused in any state
                }
            }
        });
    }
    private void setModel(){

        String jsonFileString = Utils.getJsonFromAssets(getApplicationContext(), "vid1.json");
        Log.i("data", jsonFileString);
        try {
            models.clear();
            JSONArray jsonArray = new JSONArray(jsonFileString);
            for(int i = 0; i < jsonArray.length(); i++){
                Model model = new Model();
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                model.time = jsonObject.getInt("time");
                model.level = jsonObject.getInt("level");
                models.add(model);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
            if (playerView != null) {
                playerView.onResume();
                Log.i("Video==>", "onStart");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer();
            if (playerView != null) {
                playerView.onResume();
                Log.i("Video==>", "onResume");
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            if (playerView != null) {
                playerView.onPause();
                Log.i("Video==>", "onPause");
            }
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            if (playerView != null) {
                playerView.onPause();
                Log.i("Video==>", "onStop");
            }
            releasePlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adsLoader.release();
        Log.i("Video==>", "onDestroy");
    }

}