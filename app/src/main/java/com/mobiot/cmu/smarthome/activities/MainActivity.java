package com.mobiot.cmu.smarthome.activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mobiot.cmu.smarthome.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextToSpeech t1;
    private int count = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("deviceTest").child("1").child("current");
        Button facedetection = (Button) findViewById(R.id.facedetection);
        t1 = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });

        // Read from the database
        myRef.child("clientAudio").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                try
                {
                    byte[] decoded = Base64.decode(value, 0);
                    File file = new File(Environment.getExternalStorageDirectory() + "/tmp.amr");
                    FileOutputStream os = new FileOutputStream(file, true);
                    os.write(decoded);
                    os.close();
                    playAudio(file);
//                    file.delete();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
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


                String value = dataSnapshot.getValue(String.class);
                t1.speak(value, TextToSpeech.QUEUE_FLUSH, null, "" + (count++));

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
            }
        });

        facedetection.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MultiTrackerActivity.class);
                startActivity(intent);
            }

        });

    }
    private void playAudio(File file) {
        MediaPlayer player = new MediaPlayer();
        String path = file.getAbsolutePath();
        try {
            player.setDataSource(path);
            player.prepare();
            player.start();
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
