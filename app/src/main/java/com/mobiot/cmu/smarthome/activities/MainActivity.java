package com.mobiot.cmu.smarthome.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mobiot.cmu.smarthome.R;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import static android.R.string.no;
import static android.R.string.yes;

public class MainActivity extends AppCompatActivity {
    private TextToSpeech t1;
    private int count = 1;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private String instanceID = null;
    private DatabaseReference myRef = null;
    private TextView temperatureText;
    private TextView humidityText;
    private String message = null;
    private String audio = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final FirebaseDatabase database = FirebaseDatabase.getInstance();

        temperatureText = (TextView) findViewById(R.id.tempText);
        humidityText = (TextView) findViewById(R.id.humidText);
        Button registerButton = (Button) findViewById(R.id.register);
        t1 = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });

        Bundle bundle = getIntent().getExtras();

        if(bundle !=null && bundle.get("instanceID") != null) {
           instanceID = bundle.get("instanceID").toString();
           System.out.println("instanceID:" + instanceID);
           setReference();
        } else if(instanceID == null) {
            AlertDialog diag = new AlertDialog.Builder(this)
                .setTitle("Initial Set Up")
                .setMessage("Please Register at First")
                .setPositiveButton(yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(t1 !=null){
                            t1.stop();
                            t1.shutdown();
                        }
                        Intent intent = new Intent(MainActivity.this, QRActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
//                .setNegativeButton(no, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//
//                    }
//                })
                .setIcon(R.drawable.common_google_signin_btn_icon_dark_normal).show();

        }

        registerButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(t1 !=null){
                    t1.stop();
                    t1.shutdown();
                }
                Intent intent = new Intent(MainActivity.this, QRActivity.class);
                startActivity(intent);
                finish();
            }

        });

    }

    private void setReference () {
        // Read from the database
        final DatabaseReference myRef = database.getReference(instanceID).child("1").child("current");
        myRef.child("clientAudio").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                audio = dataSnapshot.getValue(String.class);
                if(!audio.equals("")) {
                    playClientAudio();
                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
            }
        });

        myRef.child("message").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.


                message = dataSnapshot.getValue(String.class);
                System.out.println("datasnapshot:" + dataSnapshot);
                if(!message.equals("")) {
                    textTospeaker();
                }
                //myRef.child("message").setValue("");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
            }
        });


        myRef.child("sensors").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                try {
                    JSONObject config = new JSONObject((HashMap) dataSnapshot.getValue());
                    System.out.println("config:" + config);

                    JSONObject configtemp = config.getJSONObject("tempHumidity");
                    String temp = configtemp.getString("temp");
                    temperatureText.setText(temp + "'C");

                    String humid = configtemp.getString("humidity");
                    humidityText.setText(humid + "%");

                    JSONObject configmotion = config.getJSONObject("proximityWarning");
                    Boolean motiondetect = configmotion.getBoolean("isClose");
                    if(motiondetect.equals(true)) {
                        Intent intent = new Intent(MainActivity.this, MultiTrackerActivity.class);
                        startActivity(intent);

                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Json Download Error", Toast.LENGTH_LONG).show();
                    System.out.println("env:" + dataSnapshot);
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void textTospeaker() {
        t1.speak(message, TextToSpeech.QUEUE_FLUSH, null, "" + (count++));
        myRef.child("message").setValue("");
    }

    private void playClientAudio() {
        try
        {
            byte[] decoded = Base64.decode(audio, 0);
            File file = new File(Environment.getExternalStorageDirectory() + "/tmp.amr");
            if(file.exists()) file.delete();
            file = new File(Environment.getExternalStorageDirectory() + "/tmp.amr");
            FileOutputStream os = new FileOutputStream(file, true);
            os.write(decoded);
            os.close();
            playAudio(file);
            myRef.child("audio").setValue("");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void playAudio(File file) {
        MediaPlayer player = new MediaPlayer();
        String path = file.getAbsolutePath();
        try {
            player.setDataSource(path);
            player.prepare();
            player.start();
            myRef.child("clientAudio").setValue("");
        } catch (IOException e) {
            e.printStackTrace();
        }
//        Intent intent = new Intent();
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        intent.setAction(android.content.Intent.ACTION_VIEW);
//        intent.setDataAndType(Uri.fromFile(file), "audio"); //文件类型
//        startActivity(intent);
    }

}
