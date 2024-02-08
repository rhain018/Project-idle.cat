package mdevs.idle.cat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.Timer;
import mdevs.idle.cat.R;
import mdevs.idle.cat.chat.ChatMessage;

import android.os.Bundle;

public class DisplayCatActivity extends AppCompatActivity implements RewardedVideoAdListener {
    private static final String TAG = "DisplayCatActivity";
    private static final Integer UPDATE_FREQUENCY = 10 * 1000;


    //chat
    private FirebaseListAdapter<ChatMessage> adapter;

    // Firebase instance variables
    private FirebaseAnalytics mFirebaseAnalytics;
    private DatabaseReference mFirebaseDatabaseReference;
    private RewardedVideoAd mRewardedAd;
    private AdView mBannerAdView;

    // Activity items
    private TextView mAgeTextView;
    private TextView mCatHungryTextView;
    private TextView mCatIntimacyTextView;
    private TextView mCatFoodTextView;
    private TextView mCatChatTextView;

    private ImageView cat;
    private Timer mCatTimer;
    private CatStatusHolder mCatStatus;
    private CatSchedule mCatSchedule;
    private CatChat mCatChat;

    // User info
    private String mUsername = "";
    private String mUID = "";
    public RelativeLayout mlayout;

    // System info
    private boolean inited = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_cat);

        if (!isConnected(this)) {
            showInternetDialog();
        }
       
        // Init user info.
        Intent intent = getIntent();
        mUsername = intent.getStringExtra(MainActivity.USERNAME);
        mUID = intent.getStringExtra(MainActivity.USERID);
        TextView textView = (TextView) findViewById(R.id.username_text_view);
        textView.setText("Hello, " + mUsername + "!");

        //chat
       // mlayout = (RelativeLayout) findViewById(R.id.message_layout);
        //displayChatMessages();

        // Init cat status.
        mCatStatus = null;
        mCatHungryTextView = (TextView) findViewById(R.id.cat_hungry_text_view);
        mCatIntimacyTextView = (TextView) findViewById(R.id.cat_intimacy_text_view);
        mCatFoodTextView = (TextView) findViewById(R.id.cat_food_text_view);
        mAgeTextView = (TextView) findViewById(R.id.age_text_view);

        // Init cat chat.
        mCatChat = new CatChat();
        mCatChatTextView = (TextView) findViewById(R.id.cat_chat_text_view);

        cat = (ImageView) findViewById(R.id.cat_image_view);
        Glide.with(this)
                .load(R.drawable.kat_walk)
                .into(cat);

        // Init Firebase Analytics.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle payload = new Bundle();
        payload.putString(FirebaseAnalytics.Param.VALUE, mUsername);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN,
                payload);

        // Firebase database codes.
        mCatTimer = new Timer();
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference(mUsername);
        mFirebaseDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get init value. null indicates it's a new user.
                CatStatus s = dataSnapshot.getValue(CatStatus.class);
                if (s == null) {
                    // New user initate.
                    mCatStatus= new CatStatusHolder(100, 100);
                    mFirebaseDatabaseReference.setValue(mCatStatus.getStatus());
                } else {
                    mCatStatus = new CatStatusHolder();
                    mCatStatus.initStatus(s);
                }
                // Update UI
                updateCatStatusTextViews();
                // Start timer.
                mCatSchedule = new CatSchedule(mFirebaseDatabaseReference, mCatStatus);
                mCatTimer.schedule(mCatSchedule, 1000, UPDATE_FREQUENCY);
                inited = true;
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read init value.", error.toException());
                Toast.makeText(DisplayCatActivity.this, "Failed to read init value. " + error.toException(), Toast.LENGTH_SHORT).show();
            }
        });
        mFirebaseDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                CatStatus s = dataSnapshot.getValue(CatStatus.class);
                if (s == null) {
                    Log.e(TAG, "If " + mUsername + "is not a new user, database is broken...");
                    Toast.makeText(DisplayCatActivity.this, "If " + mUsername + "is not a new user, database is broken...", Toast.LENGTH_SHORT).show();
                    return;
                }
                mCatStatus.setStatus(s);
                updateCatStatusTextViews();
                Log.d(TAG, "Value is: " + mCatStatus.getHungry());
                //Toast.makeText(DisplayCatActivity.this, "Value is: " + mCatStatus.getHungry(), Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
                Toast.makeText(DisplayCatActivity.this, "Failed to read value. " + error.toException(), Toast.LENGTH_SHORT).show();
            }
        });

        // Init Ads.
        mRewardedAd = MobileAds.getRewardedVideoAdInstance(this);
        mRewardedAd.setRewardedVideoAdListener(this);
        loadRewardedVideoAd();
        mBannerAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest
                .Builder()
                .build();
        mBannerAdView.loadAd(adRequest);
    }
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DisplayCatActivity.super.onBackPressed();
                        finish();
                        System.exit(0);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
    private void showInternetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        View view = LayoutInflater.from(this).inflate(R.layout.dialog, findViewById(R.id.no_internet_layout));
        view.findViewById(R.id.try_again).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isConnected(DisplayCatActivity.this)) {
                    showInternetDialog();
                } else {
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                }
            }
        });

        builder.setView(view);

        AlertDialog alertDialog = builder.create();

        alertDialog.show();

    }

    private boolean isConnected(DisplayCatActivity displayCatActivity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) displayCatActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        return (wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected());
    }
    public void feedMyCat(View view) {
        if (!inited) return;
        boolean success = mCatStatus.feed();
        mFirebaseDatabaseReference.setValue(mCatStatus.getStatus());
        if (success) {
            if (mCatStatus.isFull()) {
                mCatChatTextView.setText(getString(R.string.full_chat));
            } else {
                mCatChatTextView.setText(getString(R.string.eating_chat));
            }
            Glide.with(this)
                    .load(R.drawable.pusheen_eat)
                    .into(cat);

        } else {
            mCatChatTextView.setText(getString(R.string.no_food_chat));
        }
    }

    public void chatnow(View view) {
        Intent intent = new Intent(DisplayCatActivity.this, StartChatActivity.class);
        startActivity(intent);
    }

    public void chatWithCat(View view) {
        if (!inited) { return; }
        Resources res = getResources();
        String[] chat_array;
        Integer pat;
        Integer age = mCatStatus.getAge();

        // Bypass age for now.
        age = 20;


        if (mCatStatus.getHungry() > 70) {
            if (age <= 3) {
                chat_array = res.getStringArray(R.array.happy_chat_array_1);
            } else if (age <= 10) {
                chat_array = res.getStringArray(R.array.happy_chat_array_2);
            } else {
                chat_array = res.getStringArray(R.array.happy_chat_array_3);
            }
            Glide.with(this)
                    .load(R.drawable.pusheen_happy_1)
                    .into(cat);

            pat = 3;
        } else if (mCatStatus.getHungry() > 30) {
            if (age <= 3) {
                chat_array = res.getStringArray(R.array.normal_chat_array_1);
            } else if (age <= 10) {
                chat_array = res.getStringArray(R.array.normal_chat_array_2);
            } else {
                chat_array = res.getStringArray(R.array.normal_chat_array_3);
            }
            double v = Math.random();
            if (v > 2/3) {
                Glide.with(this)
                        .load(R.drawable.pusheen_normal_1)
                        .into(cat);
            } else if (v > 1/3) {
                Glide.with(this)
                        .load(R.drawable.pusheen_normal_2)
                        .into(cat);
            } else {
                Glide.with(this)
                        .load(R.drawable.pusheen_normal_3)
                        .into(cat);
            }
            pat = 1;
        } else {
            if (age <= 3) {
                chat_array = res.getStringArray(R.array.hungry_chat_array_1);
            } else if (age <= 10) {
                chat_array = res.getStringArray(R.array.hungry_chat_array_2);
            } else {
                chat_array = res.getStringArray(R.array.hungry_chat_array_3);
            }
            Glide.with(this)
                    .load(R.drawable.pusheen_hungry)
                    .into(cat);

            pat = 0;
        }
        mCatChatTextView.setText(chat_array[mCatChat.chat(chat_array.length)]);
        mCatStatus.pat(pat);

        updateCatStatusTextViews();
        mFirebaseDatabaseReference.setValue(mCatStatus.getStatus());


    }

    public void watchAds(View view) {
        if (mRewardedAd.isLoaded()) {
            mRewardedAd.show();
        } else {
            mCatChatTextView.setText(getString(R.string.no_ads));
        }
    }


    // Rewarded video functions
    private void loadRewardedVideoAd() {
        mRewardedAd.loadAd(
                "ca-app-pub-9969027511473485/5141425350",
                new AdRequest.Builder()
                        .build());
    }

    @Override
    public void onRewarded(RewardItem reward) {
        mCatChatTextView.setText(getString(R.string.on_rewarded));
        mCatStatus.pat(reward.getAmount());
        mCatStatus.increaseFood();
        Toast.makeText(this, "Intimacy increased.", Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Got 1 food.", Toast.LENGTH_SHORT).show();
        updateCatStatusTextViews();
    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int errorCode) {
        Log.d(TAG, "Cannot load rewarded ads.");
        Toast.makeText(this, "Our grocery store cannot be stock up.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRewardedVideoAdLeftApplication() {
        Toast.makeText(this, "onRewardedVideoAdLeftApplication",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRewardedVideoAdClosed() {
        // Toast.makeText(this, "onRewardedVideoAdClosed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRewardedVideoAdLoaded() {
        Toast.makeText(this, "More food in grocery store!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRewardedVideoAdOpened() {
        // Toast.makeText(this, "onRewardedVideoAdOpened", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRewardedVideoStarted() {
        // Toast.makeText(this, "onRewardedVideoStarted", Toast.LENGTH_SHORT).show();
    }

    private void updateCatStatusTextViews() {
        mCatHungryTextView.setText(mCatStatus.getHungry().toString());
        mCatIntimacyTextView.setText(mCatStatus.getIntimacy().toString());
        mCatFoodTextView.setText(mCatStatus.getFood().toString());
        mAgeTextView.setText(mCatStatus.getAgeString());
    }


}
