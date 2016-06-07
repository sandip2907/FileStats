package com.demoapps.filestats;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

/**
 * Created by sandip.pandey on 6/3/2016.
 */
public class FileListAdapter extends ArrayAdapter {

    boolean isFilesflag;

    private static class ViewHolder {
        TextView tV1;
        TextView tV2;
    }

    public FileListAdapter(Context context, int textViewResourceId, List<Map.Entry<String, Object>> objects ,final boolean flag) {
        super(context, textViewResourceId, objects);
        isFilesflag = flag;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.tV1 = (TextView) convertView.findViewById(android.R.id.text1);
            viewHolder.tV2 = (TextView) convertView.findViewById(android.R.id.text2);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        Map.Entry<String, Object> entry = (Map.Entry<String, Object>) this.getItem(position);

        if(isFilesflag){
            viewHolder.tV1.setText("File name: "+entry.getKey());
            viewHolder.tV2.setText("Size: "+ FileStatActivity.readableFileSize((Long)entry.getValue()));
        }else {
            viewHolder.tV1.setText("Extension: "+entry.getKey());
            viewHolder.tV2.setText("Count: "+entry.getValue().toString());
        }
        return convertView;
    }

}
