package com.mobiot.cmu.smarthome.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import com.mobiot.cmu.smarthome.R;
import com.mobiot.cmu.smarthome.sharedpreference.IDSharedPreferences;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ScanQRCodeActivity extends AppCompatActivity {
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private IDSharedPreferences prefs;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scan_qrcode);

        mContentView = findViewById(R.id.fullscreen_content);
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.post(mHideRunnable);
        prefs = IDSharedPreferences.getInstance(getApplicationContext());
        String instanceID = prefs.getInstanceID();

        if (instanceID != null && !instanceID.equals("")) {
            Intent intentNext = new Intent(ScanQRCodeActivity.this, MainActivity.class);
            intentNext.putExtra("instanceID", instanceID);
            startActivity(intentNext);
            finish();
        }
        ImageButton imageButton = (ImageButton) findViewById(R.id.qrcode);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScanQRCodeActivity.this, QRActivity.class);
                startActivity(intent);
            }
        });
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mHideHandler.post(mHidePart2Runnable);
    }

}
