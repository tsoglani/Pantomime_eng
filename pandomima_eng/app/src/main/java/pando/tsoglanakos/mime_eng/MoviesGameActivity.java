package pando.tsoglanakos.mime_eng;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import pando.tsoglanakos.mime_eng.MainActivity;
import pando.tsoglanakos.mime_eng.R;


public class MoviesGameActivity extends AppCompatActivity implements RewardedVideoAdListener {

    private boolean isPlayerOne = true;
    private int gameRounds = 5;
    private int totalTime;
    private int curentTimer = 0;
    private int curentRound = 0;
    private int playerAScore = 0;
    private int playerBScore = 0;
    private int curentScore = 0;

    private ArrayList<String> movies = new ArrayList();
//    private int totalSkip = 1;

    private int playerACountSkips = 1, playerBCountSkips = 1;


    private ImageView imageView, game_image_view;
    private TextView pandomima_text, count_down, player_a_scores, player_b_scores, show_round_text, skips_text_view;
    private Button start_game_button, skip_button;
    private RelativeLayout game_center_layout;
    private Button correct_button, wrong_button;
    private Thread timerThread;
    private boolean isCountingDown = false;
    private pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps curentsStep;
    private LinearLayout scoreView;
    private MediaPlayer mp;
    private String skips_textString;
    private PowerManager.WakeLock wakeLock;
    private String currentPhrase = null;
    private boolean playerAHaveOneMoreChance = true, playerBHaveOneMoreChance = true;
    private int hiddenTextColor= R.color.transparent_white_percent_40;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movies_game);

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");

        imageView = (ImageView) findViewById(R.id.player_icons);
        start_game_button = (Button) findViewById(R.id.start_game_button);
        correct_button = (Button) findViewById(R.id.correct_button);
        wrong_button = (Button) findViewById(R.id.wrong_button);

        game_center_layout = (RelativeLayout) findViewById(R.id.game_center_layout);
        totalTime = getValue(MainActivity.TIME, 120);
        gameRounds = getValue(MainActivity.ROUNDS, 5);

//        totalSkip = (gameRounds / 5) * 4;


        skip_button = (Button) findViewById(R.id.skip_button);
        game_image_view = (ImageView) findViewById(R.id.game_image_view);
        pandomima_text = (TextView) findViewById(R.id.pandomima_text);
        show_round_text = (TextView) findViewById(R.id.show_round_text);
        skips_text_view = (TextView) findViewById(R.id.skips_text_view);
        count_down = (TextView) findViewById(R.id.count_down);
        curentsStep = pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player1_ready_to_play;

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup.LayoutParams default_layout_params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        scoreView = (LinearLayout) inflater.inflate(R.layout.score_layout, null);
        addContentView(scoreView, default_layout_params);
        init();


        initListeners();
        initComponents();
        scoreView.setVisibility(View.INVISIBLE);
        player_a_scores = (TextView) scoreView.getChildAt(0);
        player_b_scores = (TextView) scoreView.getChildAt(1);

        Typeface tf = Typeface.createFromAsset(this.getAssets(), "madame_cosmetic.ttf");
        player_a_scores.setTypeface(tf);
        player_b_scores.setTypeface(tf);


        Typeface tfpandomima_text = Typeface.createFromAsset(this.getAssets(), "Gecko_PersonalUseOnly.ttf");
        player_a_scores.setTypeface(tf);

        pandomima_text.setTypeface(tfpandomima_text);

        if (MainActivity.selectedType == MainActivity.TYPE.aisthisiakes) {
            start_game_button.setBackgroundResource(R.drawable.aisthisiakes_start_button);
            pandomima_text.setBackgroundResource(R.drawable.aisthisiakes_text_bc);
            game_image_view.setBackgroundResource(R.drawable.aisthisiakes_center);
            skip_button.setBackgroundResource(R.drawable.aisthisiakes_hint);
            imageView.setBackgroundResource(R.drawable.aisthisiakes_player_one);
        }else{

            try {
                start_game_button.getLayoutParams().width = pxFromDp(150);
                start_game_button.getLayoutParams().height = pxFromDp(80);
            }catch (Exception e){
                e.printStackTrace();
            }
        }


        skip_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

                    runOnUiThread(new Thread() {
                        @Override
                        public void run() {
                            if (MainActivity.selectedType == MainActivity.TYPE.aisthisiakes) {
                                skip_button.setBackgroundResource(R.drawable.aisthisiakes_hint_pressed);
                            } else
                                skip_button.setBackgroundResource(R.drawable.hint2_preview);
                        }
                    });
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    runOnUiThread(new Thread() {
                        @Override
                        public void run() {
                            runOnUiThread(new Thread() {
                                @Override
                                public void run() {
                                    if (MainActivity.selectedType == MainActivity.TYPE.aisthisiakes) {
                                        skip_button.setBackgroundResource(R.drawable.aisthisiakes_hint);
                                    } else
                                        skip_button.setBackgroundResource(R.drawable.hint1_preview);
                                }
                            });

                            if (rect.contains(view.getLeft() + (int) event.getX(), view.getTop() + (int) event.getY())) {

                                // User moved inside bounds

                                if (isPlayerOne) {
                                    if (playerACountSkips >0) {

                                        createBanner();

                                        playerACountSkips--;
                                        skips_textString = ( playerACountSkips) + " Remain";
                                        if (( playerACountSkips) <= 0) {
                                            skips_textString = "You have to see a video, for one last hint..";
                                        }

                                    } else if (playerAHaveOneMoreChance) {
                                        createReward();
                                        return;
                                    } else {


                                        if (playerAHaveOneMoreChance) {
                                            runOnUiThread(new Thread() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(pando.tsoglanakos.mime_eng.MoviesGameActivity.this, "You have to see a video for one last hint..", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } else {
                                            runOnUiThread(new Thread() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(pando.tsoglanakos.mime_eng.MoviesGameActivity.this, "You have no more Hints", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                        return;
                                    }
                                } else {
                                    if (playerBCountSkips >0) {
                                        playerBCountSkips--;
                                        skips_textString = ( playerBCountSkips) + " Remain";
                                        createBanner();
                                        if (( playerBCountSkips) <= 0) {
                                            skips_textString = "You have to see the video for one last hint..";
                                        }

                                    } else if (playerBHaveOneMoreChance) {
                                        createReward();
                                        return;

                                    } else {

                                        if (playerAHaveOneMoreChance) {
                                            runOnUiThread(new Thread() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(pando.tsoglanakos.mime_eng.MoviesGameActivity.this, "You have to see the video for one last hint..", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } else {
                                            runOnUiThread(new Thread() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(pando.tsoglanakos.mime_eng.MoviesGameActivity.this, "You have no more Hints", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                        return;
                                    }

                                }
                                runOnUiThread(new Thread() {
                                    @Override
                                    public void run() {
                                        pandomima_text.setText(getAPandomimaText());
                                        skips_text_view.setText(skips_textString);
                                    }
                                });
                            }
                        }
                    });
                }

                return true;
            }
        });

        try {
            MobileAds.initialize(this, appID);

//        AdView  mAdView = (AdView) findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().build();
//        mAdView.loadAd(adRequest);
            mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
            mRewardedVideoAd.setRewardedVideoAdListener(this);
            loadRewardedVideoAd();

            mInterstitialAd = new InterstitialAd(this);
            mInterstitialAd.setAdUnitId(AD_UNIT_ID2);
            mInterstitialAd.loadAd(new AdRequest.Builder().build());

            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    // Load the next interstitial.
                    super.onAdClosed();
                    try {
                        mInterstitialAd.loadAd(new AdRequest.Builder().build());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }//                Toast.makeText(MoviesGameActivity.this, "onAdClosed", Toast.LENGTH_SHORT).show();
                }


                @Override
                public void onAdFailedToLoad(int i) {
                    super.onAdFailedToLoad(i);
                    try {
                        mInterstitialAd.loadAd(new AdRequest.Builder().build());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                public void onAdLoaded() {
                    // Load the next interstitial.
                    super.onAdLoaded();
                    try {
                        mInterstitialAd.loadAd(new AdRequest.Builder().build());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //                Toast.makeText(MoviesGameActivity.this, "onAdLoaded", Toast.LENGTH_SHORT).show();
                }
//
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private RewardedVideoAd mRewardedVideoAd;

    private InterstitialAd mInterstitialAd;
    private static String appID = "ca-app-pub-6197752096190071~2241123642"; //"ca-app-pub-6197752096190071~2087164686";
    private static final String AD_UNIT_REWARD_ID = "ca-app-pub-6197752096190071/7877180434",
            AD_UNIT_ID2 = "ca-app-pub-6197752096190071/6708349242";


    //user id reward   ->  ca-app-pub-6197752096190071/7877180434

    //"ca-app-pub-6197752096190071/6708349242"; // admob id
// testing reward -> ca-app-pub-3940256099942544/5224354917
// ******************************** For Admob
//    boolean isVideoFailedToOpen=false;
    private void loadRewardedVideoAd() {
        mRewardedVideoAd.loadAd(AD_UNIT_REWARD_ID,
                new AdRequest.Builder().build());
    }

    @Override
    public void onRewarded(RewardItem reward) {

        // Reward the user.
    }

    @Override
    public void onRewardedVideoAdLeftApplication() {

    }

    @Override
    public void onRewardedVideoAdClosed() {
        loadRewardedVideoAd();

    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int errorCode) {
    }

    @Override
    public void onRewardedVideoAdLoaded() {
    }

    @Override
    public void onRewardedVideoAdOpened() {
    }

    @Override
    public void onRewardedVideoStarted() {
    }

    @Override
    public void onRewardedVideoCompleted() {

        if (isPlayerOne) {

            playerAHaveOneMoreChance = false;
        } else {
            playerBHaveOneMoreChance = false;

        }
        runOnUiThread(new Thread() {
            @Override
            public void run() {
                pandomima_text.setText(getAPandomimaText());
                skips_textString = "You have no more Hints";
                skips_text_view.setText(skips_textString);
            }
        });
    }

    private void createBanner() {
        try {
            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
//        mInterstitialAd.loadAd(new AdRequest.Builder().build());


            } else {
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createReward() {
        try {
            if (mRewardedVideoAd.isLoaded()) {
                mRewardedVideoAd.show();
            } else {
                loadRewardedVideoAd();
                runOnUiThread(new Thread() {
                    @Override
                    public void run() {

                        Toast.makeText(pando.tsoglanakos.mime_eng.MoviesGameActivity.this, "Temporarily not available", Toast.LENGTH_SHORT).show();


                    }
                });

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public  float dpFromPx( final float px) {
        return px / getResources().getDisplayMetrics().density;
    }

    public  int pxFromDp(final float dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }
    private void showScore() {
        isCountingDown = false;
        runOnUiThread(new Thread() {
            @Override
            public void run() {
                correct_button.setVisibility(View.INVISIBLE);
                wrong_button.setVisibility(View.INVISIBLE);
                scoreView.setVisibility(View.VISIBLE);
                pandomima_text.setVisibility(View.INVISIBLE);
                count_down.setVisibility(View.INVISIBLE);

                if (isPlayerOne) {
                    playerAScore += curentScore;

                } else {
                    playerBScore += curentScore;
                }
                player_a_scores.setText(Integer.toString(playerAScore));
                player_b_scores.setText(Integer.toString(playerBScore));

            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    private void startCountdown() {
        isCountingDown = true;
        if (timerThread == null || !timerThread.isAlive()) {
            curentTimer = totalTime;

            if (MainActivity.selectedType == MainActivity.TYPE.aisthisiakes) {
//Indigo
                runOnUiThread(new Thread() {
                    @Override
                    public void run() {
                        count_down.setTextColor(getResources().getColor(R.color.black));


                    }
                });
            }


            timerThread = new Thread() {
                @Override
                public void run() {
                    while (curentTimer >= 0 && isCountingDown) {
                        try {

                            int[] times = splitToComponentTimes(curentTimer);
                            final int min = times[0];
                            final int sec = times[1];
                            runOnUiThread(new Thread() {
                                @Override
                                public void run() {
                                    String minText, secText;

                                    if (min < 10) {
                                        minText = "0" + min;

                                    } else {
                                        minText = Integer.toString(min);
                                    }
                                    if (sec < 10) {
                                        secText = "0" + sec;

                                    } else {
                                        secText = Integer.toString(sec);
                                    }


                                    count_down.setText(minText + "m  " + secText + "s");
                                }
                            });

                            sleep(1000);
                            curentTimer--;
                            if (curentTimer < 0) {

                                try {

                                    runOnUiThread(new Thread() {
                                        @Override
                                        public void run() {
                                            try {
                                                playSound("finishtime");
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });

                                } catch (Exception e) {

                                }

                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                    timerThread = null;
                }
            };
            timerThread.setDaemon(true);
            timerThread.start();
        }
    }

    private void playSound2(String str) {
        AssetFileDescriptor afd;
        try {
            afd = getAssets().openFd(str);

            MediaPlayer player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
            player.prepare();
            player.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void playFinishSound() {
        try {
            mp.start();

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    protected void playSound(String fname) {
        int resID = getResources().getIdentifier(fname, "raw", getPackageName());

        MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), resID);
        mediaPlayer.start();
    }


    private static int[] splitToComponentTimes(int longVal) {


        int mins = longVal / 60;
        int remainder = 0;

        remainder = longVal - mins * 60;
        int secs = remainder;

        int[] ints = {mins, secs};
        return ints;
    }

    private Rect rect;    // Variable rect to hold the bounds of the view
    private final String texnInvisibleString="Hide/Show text";
    private void initListeners() {

        pandomima_text.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    if (curentsStep == pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player2_game||curentsStep == pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player1_game) {
                        runOnUiThread(new Thread(){
                            @Override
                            public void run() {
                                Typeface tf = Typeface.createFromAsset(getAssets(), "Gecko_PersonalUseOnly.ttf");
                                pandomima_text.setTextColor(getResources().getColor(R.color.Chalk));

                                pandomima_text.setTypeface(tf);
                                pandomima_text.setText(currentPhrase);
                            }
                        });

                    }
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {

                    if (curentsStep == pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player2_game||curentsStep == pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player1_game) {
                        runOnUiThread(new Thread(){
                            @Override
                            public void run() {


                                Typeface tf = Typeface.createFromAsset(getAssets(), "ACBlur.ttf");
                                pandomima_text.setTypeface(tf);
                                pandomima_text.setTextColor(getResources().getColor(hiddenTextColor));
                                pandomima_text.setText(texnInvisibleString);
                            }
                        });
                    }
                }
                return true;
            }
        });
        start_game_button.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                    runOnUiThread(new Thread() {
                        @Override
                        public void run() {
                            if (MainActivity.selectedType == MainActivity.TYPE.aisthisiakes) {
                                start_game_button.setBackgroundResource(R.drawable.aisthisiakes_start_button_pressed);
                            } else
                                start_game_button.setBackgroundResource(R.drawable.newgamebutton1_preview);
                        }
                    });
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    runOnUiThread(new Thread() {
                        @Override
                        public void run() {
                            if (MainActivity.selectedType == MainActivity.TYPE.aisthisiakes) {
                                start_game_button.setBackgroundResource(R.drawable.aisthisiakes_start_button);
                            } else
                                start_game_button.setBackgroundResource(R.drawable.start_game_button);
                        }
                    });

                    if (rect.contains(view.getLeft() + (int) event.getX(), view.getTop() + (int) event.getY())) {
                        // User moved outside bounds
                        next();
                    }

                }
                return true;
            }
        });
        scoreView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });


        wrong_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });
        wrong_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

                    runOnUiThread(new Thread() {
                        @Override
                        public void run() {
                            wrong_button.setBackgroundResource(R.drawable.error_pressed);
                        }
                    });
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    runOnUiThread(new Thread() {
                        @Override
                        public void run() {
                            runOnUiThread(new Thread() {
                                @Override
                                public void run() {
                                    wrong_button.setBackgroundResource(R.drawable.error);
                                }
                            });

                            if (rect.contains(view.getLeft() + (int) event.getX(), view.getTop() + (int) event.getY())) {

                                // User moved inside bounds
                                next();

                            }
                        }
                    });
                }

                return true;
            }
        });


        correct_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());

                    runOnUiThread(new Thread() {
                        @Override
                        public void run() {
                            correct_button.setBackgroundResource(R.drawable.correct_pressed);
                        }
                    });
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    runOnUiThread(new Thread() {
                        @Override
                        public void run() {
                            runOnUiThread(new Thread() {
                                @Override
                                public void run() {
                                    correct_button.setBackgroundResource(R.drawable.right);
                                }
                            });

                            if (rect.contains(view.getLeft() + (int) event.getX(), view.getTop() + (int) event.getY())) {

                                // User moved inside bounds


                                curentScore += 10 + (int) (((float) curentTimer / totalTime * 10.0));
                                next();

                            }
                        }
                    });
                }

                return true;
            }
        });

    }


    private enum Steps {
        player1_ready_to_play, player1_text, player1_game, scoreAfterPlayer1, player2_ready_to_play,
        player2_text, player2_game, scoreAfterPlayer2
    }

    private void enumPlusPlus() {

        if (curentsStep == pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player1_ready_to_play) {
            curentsStep = pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player1_text;
            runOnUiThread(new Thread() {
                @Override
                public void run() {
                    start_game_button.setVisibility(View.VISIBLE);
                    skip_button.setVisibility(View.VISIBLE);
                    skips_text_view.setVisibility(View.VISIBLE);
                    skips_text_view.setText((playerACountSkips) + " Remains");

                }
            });
        } else if (curentsStep == pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player1_text) {

            curentsStep = pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player1_game;

            runOnUiThread(new Thread() {
                @Override
                public void run() {

                    start_game_button.setVisibility(View.INVISIBLE);
                    skip_button.setVisibility(View.INVISIBLE);
                    skips_text_view.setVisibility(View.INVISIBLE);
                    correct_button.setVisibility(View.VISIBLE);
                    wrong_button.setVisibility(View.VISIBLE);
                    startGame();
                    Typeface tf = Typeface.createFromAsset(getAssets(), "ACBlur.ttf");
                    pandomima_text.setTypeface(tf);
                    pandomima_text.setTextColor(getResources().getColor(hiddenTextColor));
                    pandomima_text.setText(texnInvisibleString);

                }
            });
        } else if (curentsStep == pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player1_game) {
            curentsStep = pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.scoreAfterPlayer1;
            runOnUiThread(new Thread(){
                @Override
                public void run() {
                    Typeface tf = Typeface.createFromAsset(getAssets(), "Gecko_PersonalUseOnly.ttf");
                    pandomima_text.setTypeface(tf);
                    pandomima_text.setTextColor(getResources().getColor(R.color.Chalk));                }
            });
            playerACountSkips++;
            showScore();
        } else if (curentsStep == pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.scoreAfterPlayer1) {
            curentsStep = pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player2_ready_to_play;
            runOnUiThread(new Thread() {
                @Override
                public void run() {
                    scoreView.setVisibility(View.INVISIBLE);
                    pandomima_text.setVisibility(View.VISIBLE);
                    imageView.setVisibility(View.VISIBLE);

                    if (MainActivity.selectedType == MainActivity.TYPE.aisthisiakes) {
                        imageView.setBackgroundResource(R.drawable.aisthisiakes_player_two);

                    } else
                        imageView.setBackgroundResource(R.drawable.player_two);
                }
            });

            isPlayerOne = false;

        } else if (curentsStep == pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player2_ready_to_play) {
            curentsStep = pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player2_text;
            runOnUiThread(new Thread() {
                @Override
                public void run() {
                    start_game_button.setVisibility(View.VISIBLE);
                    skip_button.setVisibility(View.VISIBLE);
                    skips_text_view.setText((playerBCountSkips) + " Remains");
                    skips_text_view.setVisibility(View.VISIBLE);

                }
            });

        } else if (curentsStep == pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player2_text) {
            curentsStep = pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player2_game;
            runOnUiThread(new Thread() {
                @Override
                public void run() {
                    start_game_button.setVisibility(View.INVISIBLE);
                    skip_button.setVisibility(View.INVISIBLE);
                    skips_text_view.setVisibility(View.INVISIBLE);
                    correct_button.setVisibility(View.VISIBLE);
                    wrong_button.setVisibility(View.VISIBLE);
                    startGame();
                    playerBCountSkips++;
                    Typeface tf = Typeface.createFromAsset(getAssets(), "ACBlur.ttf");
                    pandomima_text.setTypeface(tf);
                    pandomima_text.setTextColor(getResources().getColor(hiddenTextColor));
                    pandomima_text.setText(texnInvisibleString);

                }
            });
        } else if (curentsStep == pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player2_game) {
            curentsStep = pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.scoreAfterPlayer2;

            runOnUiThread(new Thread(){
                @Override
                public void run() {
                    Typeface tf = Typeface.createFromAsset(getAssets(), "Gecko_PersonalUseOnly.ttf");
                    pandomima_text.setTypeface(tf);
                    pandomima_text.setTextColor(getResources().getColor(R.color.Chalk));                }
            });
            showScore();
        } else if (curentsStep == pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.scoreAfterPlayer2) {
            curentsStep = pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player1_ready_to_play;
            curentRound++;
            if (curentRound >= gameRounds) {
///// if game ends
                gameOver();
            }
            runOnUiThread(new Thread() {
                @Override
                public void run() {
                    scoreView.setVisibility(View.INVISIBLE);
                    pandomima_text.setVisibility(View.VISIBLE);
                    imageView.setVisibility(View.VISIBLE);

                    if (MainActivity.selectedType == MainActivity.TYPE.aisthisiakes) {
                        imageView.setBackgroundResource(R.drawable.aisthisiakes_player_one);

                    } else
                        imageView.setBackgroundResource(R.drawable.player_one);
                    show_round_text.setText("Round " + (curentRound + 1));

                }
            });
            isPlayerOne = true;


        }


    }

    private void gameOver() {
        runOnUiThread(new Thread() {
            @Override
            public void run() {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final ViewGroup.LayoutParams default_layout_params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                final RelativeLayout finalView = (RelativeLayout) inflater.inflate(R.layout.final_view, null);
                finalView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Intent intent = new Intent(pando.tsoglanakos.mime_eng.MoviesGameActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        runOnUiThread(new Thread() {
                            @Override
                            public void run() {
                                startActivity(intent);
                            }
                        });
                    }
                });
                if (playerAScore > playerBScore) {
                    runOnUiThread(new Thread() {
                        @Override
                        public void run() {
                            addContentView(finalView, default_layout_params);
                            if (MainActivity.selectedType == MainActivity.TYPE.aisthisiakes) {
                                finalView.setBackgroundResource(R.drawable.aisthisiakes_player_one_won);

                            } else
                                finalView.setBackgroundResource(R.drawable.player_one_won);
                        }
                    });
                } else if (playerAScore < playerBScore) {
                    runOnUiThread(new Thread() {
                        @Override
                        public void run() {
                            addContentView(finalView, default_layout_params);
                            if (MainActivity.selectedType == MainActivity.TYPE.aisthisiakes) {
                                finalView.setBackgroundResource(R.drawable.aisthisiakes_player_two_won);

                            } else
                                finalView.setBackgroundResource(R.drawable.player_two_won);

                        }
                    });
                } else {
                    runOnUiThread(new Thread() {
                        @Override
                        public void run() {
                            addContentView(finalView, default_layout_params);
                            if (MainActivity.selectedType == MainActivity.TYPE.aisthisiakes) {
                                finalView.setBackgroundResource(R.drawable.aisthisiakes_draw);

                            } else
                                finalView.setBackgroundResource(R.drawable.draw);

                        }
                    });
                }
            }
        });


    }

    private void startGame() {
        curentScore = 0;
        startCountdown();

        runOnUiThread(new Thread() {
            @Override
            public void run() {

                count_down.setVisibility(View.VISIBLE);
            }
        });

    }

    private void next() {
        runOnUiThread(new Thread() {
            @Override
            public void run() {
                enumPlusPlus();// allazei state
                if (curentsStep == (pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player1_text) || curentsStep == (pando.tsoglanakos.mime_eng.MoviesGameActivity.Steps.player2_text)) {


                    pandomima_text.setText(getAPandomimaText());
                }
            }
        });


    }


    private String getAPandomimaText() {

        int randomTextId = (int) (Math.random() * movies.size());
        currentPhrase = movies.remove(randomTextId);

        return currentPhrase;
    }

    private void initComponents() {
        mp = MediaPlayer.create(getApplicationContext(), R.raw.finishtime);// the song is a filename which i have pasted inside a folder **raw** created under the **res** folder.//

    }

    private void init() {
        String[] myResArray = null;
        if (MainActivity.selectedType == MainActivity.TYPE.tainies) {
            myResArray = getResources().getStringArray(R.array.movies);
        } else if (MainActivity.selectedType == MainActivity.TYPE.seires) {
            myResArray = getResources().getStringArray(R.array.seires);
        } else if (MainActivity.selectedType == MainActivity.TYPE.diafimiseis) {
            myResArray = getResources().getStringArray(R.array.diafimiseis);
        } else if (MainActivity.selectedType == MainActivity.TYPE.aisthisiakes) {
            myResArray = getResources().getStringArray(R.array.aisthisiakes);
        } else if (MainActivity.selectedType == MainActivity.TYPE.paroimies) {
            myResArray = getResources().getStringArray(R.array.paroimies);
        }


        if (MainActivity.selectedType == MainActivity.TYPE.mix) {


            List<String> myResArrayList = new ArrayList<>();
            myResArrayList.addAll(Arrays.asList(getResources().getStringArray(R.array.paroimies)));
            myResArrayList.addAll(Arrays.asList(getResources().getStringArray(R.array.movies)));
            myResArrayList.addAll(Arrays.asList(getResources().getStringArray(R.array.seires)));
            myResArrayList.addAll(Arrays.asList(getResources().getStringArray(R.array.diafimiseis)));

            movies = new ArrayList<String>(myResArrayList);
            movies = removeDuplicates(movies);


        } else {

            List<String> myResArrayList = (Arrays.asList(myResArray));
            movies = new ArrayList<String>(myResArrayList);
            movies = removeDuplicates(movies);

        }
    }


    static ArrayList<String> removeDuplicates(ArrayList<String> list) {

        // Store unique items in result.
        ArrayList<String> result = new ArrayList<>();

        // Record encountered Strings in HashSet.
        HashSet<String> set = new HashSet<>();

        // Loop over argument list.
        for (String item : list) {

            // If String is not in set, add it to the list and the set.
            if (!set.contains(item)) {
                result.add(item);
                set.add(item);
            }
        }
        return result;
    }

    public void addMovie(String movie) {
        if (!movies.contains(movie)) {
            movies.add(movie);
        }
    }

    public void player_icons(final View v) {

        runOnUiThread(new Thread() {
            @Override
            public void run() {
                ImageView b = (ImageView) v;
                b.setVisibility(View.INVISIBLE);
            }
        });
        next();

    }

    private int getValue(String key, int defaultValue) {


        SharedPreferences sp = getSharedPreferences("your_prefs", Activity.MODE_PRIVATE);
        int myIntValue = sp.getInt(key, defaultValue);
        return myIntValue;
    }


    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onResume() {
        mRewardedVideoAd.resume(this);
        wakeLock.acquire();
        super.onResume();
    }

    @Override
    public void onPause() {
        mRewardedVideoAd.pause(this);
        this.wakeLock.release();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mRewardedVideoAd.destroy(this);
        super.onDestroy();
        isCountingDown = false;
    }


    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            isCountingDown = false;

            finish();
            System.gc();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        runOnUiThread(new Thread() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Click BACK again to go to menu", Toast.LENGTH_SHORT).show();
            }
        });

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (Integer.parseInt(android.os.Build.VERSION.SDK) > 5
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            onBackPressed();
            return true;
        }


        return super.onKeyDown(keyCode, event);
    }


}
