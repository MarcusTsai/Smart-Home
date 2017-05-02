package com.mobiot.cmu.smarthome.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.mobiot.cmu.smarthome.R;
import com.mobiot.cmu.smarthome.sharedpreference.IDSharedPreferences;

public class QRActivity extends AppCompatActivity {
//    private static String androidID = null;
//    private Button scanButton;
//    private TextView instTextView;
    private String instanceID = null;
    private IDSharedPreferences prefs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);
        prefs = IDSharedPreferences.getInstance(getApplicationContext());
//        instTextView = (TextView) findViewById(R.id.instIDTextView);
//        scanButton = (Button) findViewById(R.id.scanButton);

//        scanButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                //Propagate Main Activity to IntentIntegrator
//                IntentIntegrator intentIntegrator = new IntentIntegrator(QRActivity.this);
//                intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
//                intentIntegrator.setPrompt("Scan");
//                intentIntegrator.setCameraId(0);
//                intentIntegrator.setBeepEnabled(false);
//                intentIntegrator.setBarcodeImageEnabled(false);
//                intentIntegrator.initiateScan();
//            }
//        });
        IntentIntegrator intentIntegrator = new IntentIntegrator(QRActivity.this);
        intentIntegrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        intentIntegrator.setPrompt("Scan");
        intentIntegrator.setCameraId(0);
        intentIntegrator.setBeepEnabled(false);
        intentIntegrator.setBarcodeImageEnabled(false);
        intentIntegrator.initiateScan();

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

        if (result != null) {
            if (result.getContents() == null) {
                Log.d("Main Activity Scan", "Cancel Scan ====================================");
                Toast.makeText(this, "Canceled", Toast.LENGTH_LONG).show();
            } else {
                // get result from Zxing's lib
                Log.d("Main Activity Scan", "Scanned ====================================");
                // start google search activity immediately
                System.out.println("result: " + result.getContents());

                instanceID = result.getContents();
                System.out.println("URL: " + instanceID);
//                instTextView.setText(instanceID);
                if(!instanceID.isEmpty()) {
                    prefs.setInstanceID(instanceID);
                    Intent intentNext = new Intent(QRActivity.this, MainActivity.class);
                    intentNext.putExtra("instanceID", instanceID);
                    startActivity(intentNext);
                }
            }
        } else {
            /**
             * Repeatedly call
             */
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }
}
