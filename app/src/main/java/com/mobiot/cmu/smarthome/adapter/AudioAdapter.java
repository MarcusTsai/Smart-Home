package com.mobiot.cmu.smarthome.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mobiot.cmu.smarthome.R;
import com.mobiot.cmu.smarthome.activities.AudioRecordActivity;
import com.mobiot.cmu.smarthome.model.AudioFile;

/**
 * Created by mingchia on 4/27/17.
 */

public class AudioAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private Context mContext;

    class AudioListItem {
        private TextView audioFileNameTextView;
        private TextView audioBuildTimeTextView;
        private TextView audioLengthTextView;
        private ImageButton imageButton;
    }

    public AudioAdapter(Context context) {
        // Cache the LayoutInflate to avoid asking for a new one each time.
        mContext = context;
        mInflater = LayoutInflater.from(context);
    }
    @Override
    public int getCount() {
        return AudioRecordActivity.list.size();
    }

    @Override
    public Object getItem(int position) {
        return AudioRecordActivity.list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final AudioListItem item;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.audio_item, null);
            item = new AudioListItem();
            item.audioFileNameTextView = (TextView) convertView.findViewById(R.id.audio_file_name);
            item.audioBuildTimeTextView = (TextView) convertView.findViewById(R.id.build_time);
            item.audioLengthTextView = (TextView) convertView.findViewById(R.id.audio_length);
            item.imageButton = (ImageButton) convertView.findViewById(R.id.btnDelete);

            convertView.setTag(item);
        }
        else {
            item = (AudioListItem) convertView.getTag();
        }
        final AudioFile audioFile = AudioRecordActivity.list.get(position);
        item.audioFileNameTextView.setText(audioFile.getFileName());
        item.audioBuildTimeTextView.setText(audioFile.getBuildTime());
        item.audioLengthTextView.setText(audioFile.getAudioLength());
        item.imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioFile.getAudioFile().delete();
                AudioRecordActivity.list.remove(position);
                notifyDataSetChanged();
            }
        });
        return convertView;
    }

    public void updateData() {
        notifyDataSetChanged();
    }
}
