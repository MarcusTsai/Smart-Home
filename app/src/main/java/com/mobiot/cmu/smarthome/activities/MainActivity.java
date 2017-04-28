package com.mobiot.cmu.smarthome.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
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

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("1493192503547").child("1");
        Button facedetection = (Button) findViewById(R.id.facedetection);


        // Read from the database
        myRef.child("audio").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                try
                {
                    byte[] decoded = Base64.decode(value, 0);
                    File file2 = new File(Environment.getExternalStorageDirectory() + "/tmp.amr");
                    FileOutputStream os = new FileOutputStream(file2, true);
                    os.write(decoded);
                    os.close();
                    playAudio(file2);
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

        facedetection.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MultiTrackerActivity.class);
                startActivity(intent);
            }

        });

    }
    private void playAudio(File file) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "audio"); //文件类型
        startActivity(intent);
    }

}
