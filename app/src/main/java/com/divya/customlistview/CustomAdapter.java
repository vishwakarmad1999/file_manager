package com.divya.customlistview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;

/**
 * Created by Dell on 15-03-2019.
 */


public class CustomAdapter extends BaseAdapter {
    ArrayList<Integer> imageResource;
    ArrayList<String> fName, dateModified;
    Context context;

    public CustomAdapter(Context context, ArrayList<Integer> imageResource, ArrayList<String> fName, ArrayList<String> dateModified) {
        this.imageResource = imageResource;
        this.fName = fName;
        this.dateModified = dateModified;
        this.context = context;
    }

    @Override
    public int getCount() {
        return imageResource.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        convertView = inflater.inflate(R.layout.row_layout, null);

        ImageView imageView = (ImageView) convertView.findViewById(R.id.fImage);
        TextView fileName = (TextView) convertView.findViewById(R.id.fname);
        TextView date = (TextView) convertView.findViewById(R.id.dateModified);

        imageView.setImageResource(imageResource.get(i));

        if (fName.get(i).length() > 40) {
            fileName.setText(fName.get(i).substring(0, 40) + "...");
        } else {
            fileName.setText(fName.get(i));
        }
        date.setText(dateModified.get(i));

        return convertView;
    }
}

