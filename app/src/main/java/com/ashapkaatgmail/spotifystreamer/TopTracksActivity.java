package com.ashapkaatgmail.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;


public class TopTracksActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_tracks);


        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(InfoKeys.KEY_ARTIST_NAME)) {
            String artistName = intent.getStringExtra(InfoKeys.KEY_ARTIST_NAME);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setSubtitle(artistName);
            }
        }

    }
}
