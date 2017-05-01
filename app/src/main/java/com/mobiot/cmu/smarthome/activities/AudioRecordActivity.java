package com.mobiot.cmu.smarthome.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mobiot.cmu.smarthome.R;
import com.mobiot.cmu.smarthome.adapter.AudioAdapter;
import com.mobiot.cmu.smarthome.model.AudioFile;

import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class AudioRecordActivity extends AppCompatActivity {

    private Button mStartBtn;
    private Button mRetakeBtn;
    private Button mUploadBtn;
    private ImageView imageView;
    private Bitmap bm;
    private View mProgressView;
    private File imageFile;
    private File mAudioFile;
    private File mAudioPath;
    private MediaRecorder mediaRecorder;
    private String strTempFile = "radio_";
    private ListView listView;
    private AudioAdapter adapter;
    public static List<AudioFile> list;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);
        Bundle bundle = getIntent().getExtras();
        imageFile =  (File)bundle.getSerializable("imageFile");
        imageView = (ImageView) findViewById(R.id.image_view);
//        System.out.println(imageFile.getAbsolutePath());
        bm = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

        imageView.setImageBitmap(bm);
        imageView.setScaleX(-1);
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
        File[] tmp = mAudioPath.listFiles();
        for(File file : tmp) file.delete();
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

    private void initButton() {
        mProgressView = findViewById(R.id.progressBar);
        mStartBtn = (Button) findViewById(R.id.AudioBtn);
        mRetakeBtn = (Button) findViewById(R.id.ImageRetakeBtn);
        mUploadBtn =(Button) findViewById(R.id.UploadBtn);
        mStartBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if(mStartBtn.getText().toString().equals("Start Record")) {
                    mStartBtn.setText("Stop");
                    mHandler.sendEmptyMessage(MSG_RECORD);
                } else {
                    mStartBtn.setText("Start Record");
                    mHandler.sendEmptyMessage(MSG_STOP);
                }
            }
        });
        mRetakeBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                imageFile.delete();
                if (mAudioFile != null) mAudioFile.delete();
                Intent intent = new Intent();
                intent.setClass(AudioRecordActivity.this, MultiTrackerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
        mUploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgress(true);
                UploadFileTask task = new UploadFileTask();
                task.execute((Void) null);
                // Write a message to the database
//                FirebaseDatabase database = FirebaseDatabase.getInstance();
//                DatabaseReference myRef = database.getReference("deviceTest").child("1");
//                if(list.isEmpty()) {
//                    myRef.child("audio").removeValue();
//                    myRef.child("picture").setValue(imageEncode(bm));
//                } else {
//                    myRef.child("audio").setValue(audioEncode(list.get(0).getAudioFile()));
//                    myRef.child("picture").setValue(imageEncode(bm));
//                }

            }
        });
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

    private String imageEncode(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        String imageEncoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        return imageEncoded;
    }

    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mUploadBtn.setVisibility(show ? View.GONE : View.VISIBLE);
            mUploadBtn.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mUploadBtn.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mUploadBtn.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public class UploadFileTask extends AsyncTask<Void, Void, Boolean> {
//        private Timer timer = new Timer();
        private final int DELAY_NEXT = (int)(2 * 1000);
        @Override
        protected Boolean doInBackground(Void... params) {
            boolean success = true;
            upload();
            return success;
        }

        private void upload() {
            Map<String, Object> updateCurrent= new HashMap<>();
            Map<String, Object> updateHistory= new HashMap<>();
            Calendar mCalendar= Calendar.getInstance(Locale.US);
            Date mydate=new Date();
            mCalendar.setTime(mydate);
            String timestamp = mCalendar.getTimeInMillis() + "";
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("deviceTest").child("1");
            if(list.isEmpty()) {
                updateCurrent.put("audio", "");
                updateCurrent.put("picture", imageEncode(bm));
                updateCurrent.put("timestamp", timestamp);
                updateHistory.put("audio", "");
                updateHistory.put("picture", imageEncode(bm));
                myRef.child("current").updateChildren(updateCurrent);
                myRef.child("history").child(timestamp).updateChildren(updateHistory);
            } else {
                updateCurrent.put("audio", audioEncode(list.get(0).getAudioFile()));
                updateCurrent.put("picture", imageEncode(bm));
                updateCurrent.put("timestamp", timestamp);
                updateHistory.put("audio", audioEncode(list.get(0).getAudioFile()));
                updateHistory.put("picture", imageEncode(bm));
                myRef.child("current").updateChildren(updateCurrent);
                myRef.child("history").child(timestamp).updateChildren(updateHistory);
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            showProgress(false);

            if (success) {
                for(AudioFile file : list) {
                    file.getAudioFile().delete();
                }
                list.removeAll(list);
                imageFile.delete();
                Toast toast = Toast.makeText(AudioRecordActivity.this, "File uploaded successfully!", Toast.LENGTH_SHORT);
                toast.show();
//                timer.schedule(
//                        new TimerTask() {
//                            @Override
//                            public void run() {
////                                Intent intent = new Intent();
////                                intent.setClass(AudioRecordActivity.this, MainActivity.class);
////                                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);//设置不要刷新将要跳到的界面
////                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//它可以关掉所要到的界面中间的activity
////                                startActivity(intent);
////                                setResult(RESULT_OK);
//                                AudioRecordActivity.this.finish();
//                            }
//                        }, DELAY_NEXT
//                );
                Handler handler = new Handler();

                handler.postDelayed(new Runnable() {
                    public void run() {
                        finish();
                    }
                }, DELAY_NEXT);
            } else {
                Toast toast = Toast.makeText(AudioRecordActivity.this, "Failed!", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        @Override
        protected void onCancelled() {
            showProgress(false);
        }
    }

}
