package com.ashapkaatgmail.spotifystreamer.Adapters;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ashapkaatgmail.spotifystreamer.Helpers.HashMapWrapperParcelable;
import com.ashapkaatgmail.spotifystreamer.Helpers.InfoKeys;
import com.ashapkaatgmail.spotifystreamer.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class ArtistAdapter extends SpotifyAdapterBase {
    public ArtistAdapter(Activity context, ArrayList<HashMapWrapperParcelable<String, String>> data) {
        super(context, data, R.layout.list_item_artist);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();

            holder.thumbnail = (ImageView) view.findViewById(R.id.list_item_artist_thumbnail);
            holder.artistName = (TextView) view.findViewById(R.id.list_item_artist_name);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        HashMapWrapperParcelable<String, String> info = getData().get(position);

        holder.artistName.setText(info.get(InfoKeys.KEY_ARTIST_NAME));

        String imgUrl = info.get(InfoKeys.KEY_THUMB_URL);
        if (imgUrl != null) {
            Picasso.with(getContext()).load(imgUrl).into(holder.thumbnail);
        }

        return view;
    }

    private static class ViewHolder {
        ImageView thumbnail;
        TextView artistName;
    }
}
