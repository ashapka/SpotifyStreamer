package com.ashapkaatgmail.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.HashMap;


public class SpotifyAdapterBase extends BaseAdapter {

    private final Activity mContext;
    private final LayoutInflater mInflater;
    private final int mResourceListItem;

    private ArrayList<HashMap<String, String>> mData;

    public SpotifyAdapterBase(Activity context, ArrayList<HashMap<String, String>> data, int resourceListItem) {
        mContext = context;
        mData = data;

        mResourceListItem = resourceListItem;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        int size = mData.size();
        return size;
    }

    @Override
    public Object getItem(int position) {
        Object item = mData.get(position);
        return item;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;

        if (convertView == null) {
            view = mInflater.inflate(mResourceListItem, null);
        }

        return view;
    }


    public void clear() {
        mData.clear();
        notifyDataSetChanged();
    }

    public void addAll(ArrayList<HashMap<String, String>> info) {
        mData = info;
        notifyDataSetChanged();
    }

    public ArrayList<HashMap<String, String>> getData() {
        return mData;
    }

    public Activity getContext(){
        return mContext;
    }
}
