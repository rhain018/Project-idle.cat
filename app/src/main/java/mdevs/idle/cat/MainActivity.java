package mdevs.idle.cat;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import mdevs.idle.cat.R;

public class MainActivity extends AppCompatActivity {
    public static final String USERNAME = "mdevs.idle.cat.USERNAME";
    public static final String USERID = "mdevs.idle.cat.USERID";
    private static final String TAG = "MainActivity";

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String mUsername = "";
    private String mUID = "";
    private GoogleApiClient mGoogleApiClient;
    public static final String DEFAULT_USER = "Guest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        setContentView(R.layout.activity_main);


        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser != null) {
            mUsername = mFirebaseUser.getDisplayName();
            mUID = mFirebaseUser.getUid();
            Intent intent = new Intent(this, DisplayCatActivity.class);
            intent.putExtra(USERNAME, mUsername);
            intent.putExtra(USERID, mUID);
            startActivity(intent);
        }
    }
    /** Called when the user taps the Send button */
    public void GenerateCat(View view) {
        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
        } else {
            mUsername = mFirebaseUser.getDisplayName();
            mUID = mFirebaseUser.getUid();
            Intent intent = new Intent(this, DisplayCatActivity.class);
            intent.putExtra(USERNAME, mUsername);
            intent.putExtra(USERID, mUID);
            startActivity(intent);

        }
    }
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

}
