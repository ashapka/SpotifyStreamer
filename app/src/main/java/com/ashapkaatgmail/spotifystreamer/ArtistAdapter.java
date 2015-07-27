package com.ashapkaatgmail.spotifystreamer;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;


public class ArtistAdapter extends SpotifyAdapterBase {
    public ArtistAdapter(Activity context, ArrayList<HashMap<String, String>> data) {
        super(context, data, R.layout.list_item_artist);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        ImageView thumbnail = (ImageView) view.findViewById(R.id.list_item_artist_thumbnail);
        TextView artistName = (TextView) view.findViewById(R.id.list_item_artist_name);

        HashMap<String, String> info = getData().get(position);

        artistName.setText(info.get(InfoKeys.KEY_ARTIST_NAME));

        String imgUrl = info.get(InfoKeys.KEY_THUMB_URL);
        if (imgUrl != null) {
            Picasso.with(getContext()).load(imgUrl).into(thumbnail);
        }

        return view;
    }
}
