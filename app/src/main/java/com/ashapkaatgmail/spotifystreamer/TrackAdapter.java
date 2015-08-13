package com.ashapkaatgmail.spotifystreamer;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class TrackAdapter extends SpotifyAdapterBase {
    public TrackAdapter(Activity context, ArrayList<HashMapWrapperParcelable<String, String>> data) {
        super(context, data, R.layout.list_item_track);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        ImageView thumbnail = (ImageView) view.findViewById(R.id.list_item_track_thumbnail);
        TextView albumName = (TextView) view.findViewById(R.id.list_item_track_album_name);
        TextView trackName = (TextView) view.findViewById(R.id.list_item_track_name);

        HashMapWrapperParcelable<String, String> info = getData().get(position);

        albumName.setText(info.get(InfoKeys.KEY_ALBUM_NAME));
        trackName.setText(info.get(InfoKeys.KEY_TRACK_NAME));

        String imgUrl = info.get(InfoKeys.KEY_THUMB_URL);
        if (imgUrl != null) {
            Picasso.with(getContext()).load(imgUrl).into(thumbnail);
        }

        return view;
    }
}
