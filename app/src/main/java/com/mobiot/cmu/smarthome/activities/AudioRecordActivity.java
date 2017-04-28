package com.mobiot.cmu.smarthome.activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mobiot.cmu.smarthome.R;
import com.mobiot.cmu.smarthome.adapter.AudioAdapter;
import com.mobiot.cmu.smarthome.model.AudioFile;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AudioRecordActivity extends AppCompatActivity {

    private Button mStartBtn;
    private Button mStopBtn;
    private Button mUploadBtn;
    private File mAudioFile;
    private File mAudioPath;
    private MediaRecorder mediaRecorder;
    private String strTempFile = "radio_";// 音频文件名的前缀
    private ListView listView;
    private AudioAdapter adapter;
    public static List<AudioFile> list;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);
        list = new ArrayList<>();
        initFilePath();
        initList();
        initButton();
    }

    private void initFilePath() {
        String path;
        if (isSDCardValid()) {
            path = Environment.getExternalStorageDirectory().toString()
                    + File.separator + "recordAudio";
            System.out.println(path);
        } else {
            path = Environment.getRootDirectory().toString()
                    + File.separator + "recordAudio";
        }
        mAudioPath = new File(path);
        if (!mAudioPath.exists()) {
            mAudioPath.mkdirs();
        }
    }

    private boolean isSDCardValid() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            Toast.makeText(getBaseContext(), "No SD card", Toast.LENGTH_LONG).show();
        }
        return false;
    }
    private void initList() {
        listView = (ListView) findViewById(R.id.audio_listView);
        setListEmptyView();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AudioFile audioFile = list.get(position);
                playAudio(audioFile.getAudioFile());
            }
        });
        adapter = new AudioAdapter(AudioRecordActivity.this);
        listView.setAdapter(adapter);
    }
    private void setListEmptyView() {
        View emptyView = findViewById(R.id.empty);
        listView.setEmptyView(emptyView);
    }
    private void playAudio(File file) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "audio"); //文件类型
        startActivity(intent);
    }

    private void initButton() {
        mStartBtn = (Button) findViewById(R.id.AudioStartBtn);
        mStopBtn = (Button) findViewById(R.id.AudioStopBtn);
        mUploadBtn =(Button) findViewById(R.id.AudioUploadBtn);
        mStartBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mStartBtn.setEnabled(false);
                mStopBtn.setEnabled(true);
                mHandler.sendEmptyMessage(MSG_RECORD);
            }
        });
        mStopBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mStartBtn.setEnabled(true);
                mStopBtn.setEnabled(false);
                mHandler.sendEmptyMessage(MSG_STOP);
            }
        });
        mUploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Write a message to the database
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference("1493192503547").child("1");
                if(list.isEmpty()) return;
                else {
                    myRef.child("audio").setValue(audioEncode(list.get(0).getAudioFile()));
                }

            }
        });
        mStartBtn.setEnabled(true);
        mStopBtn.setEnabled(false);
    }
    private static final int MSG_RECORD = 0;
    private static final int MSG_STOP = 1;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RECORD:
                    startRecord();
                    break;
                case MSG_STOP:
                    stopRecord();
                    break;
                default:
                    break;
            }
        };
    };

    private void startRecord() {
        try {
            mStartBtn.setEnabled(false);
            mStopBtn.setEnabled(true);
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            try {
                mAudioFile = File.createTempFile(strTempFile, ".amr", mAudioPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaRecorder.setOutputFile(mAudioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void stopRecord() {
        if (mAudioFile != null) {
            mStartBtn.setEnabled(true);
            mStopBtn.setEnabled(false);
            mediaRecorder.stop();
            if(list.isEmpty()) list.add(new AudioFile(mAudioFile, GetFileBuildTime(mAudioFile), GetFilePlayTime(mAudioFile)));
            else {
                for(AudioFile file : list) {
                    file.getAudioFile().delete();
                }
                list.removeAll(list);
                list.add(new AudioFile(mAudioFile, GetFileBuildTime(mAudioFile), GetFilePlayTime(mAudioFile)));
            }
            adapter.updateData();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }
    private String GetFilePlayTime(File file){
        Date date;
        SimpleDateFormat sy1;
        String dateFormat = "error";
        try {
            sy1 = new SimpleDateFormat("HH:mm:ss");//设置为时分秒的格式
            MediaPlayer mediaPlayer;//使用媒体库获取播放时间
            mediaPlayer = MediaPlayer.create(getBaseContext(), Uri.parse(file.toString()));
            //使用Date格式化播放时间mediaPlayer.getDuration()
            date = sy1.parse("00:00:00");
            date.setTime(mediaPlayer.getDuration() + date.getTime());//用消除date.getTime()时区差
            dateFormat = sy1.format(date);

            mediaPlayer.release();

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dateFormat;
    }
    private String GetFileBuildTime(File file) {
        Date date = new Date(file.lastModified());//最后更新的时间
        String t;
        SimpleDateFormat sy2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置年月日时分秒
        t = sy2.format(date);
        return t;
    }

    private String audioEncode(File file) {
        byte[] bytes = new byte[0];
        try {
            bytes = FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String encoded = Base64.encodeToString(bytes, 0);
        return encoded;
    }


}
