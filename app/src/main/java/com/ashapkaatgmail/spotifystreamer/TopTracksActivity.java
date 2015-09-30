package com.ashapkaatgmail.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.ashapkaatgmail.spotifystreamer.Helpers.InfoKeys;
import com.ashapkaatgmail.spotifystreamer.Helpers.Strings;
import com.ashapkaatgmail.spotifystreamer.Helpers.UserLeaveHintCallbackInterface;


public class TopTracksActivity extends AppCompatActivity {

    private final String TOP_TRACK_FRAGMENT_TAG = "TOP_TRACK_FRAGMENT";

    private UserLeaveHintCallbackInterface mUserLeaveHintCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_tracks);

        FragmentManager fragmentManager = getSupportFragmentManager();
        TopTracksActivityFragment topTracksActivityFragment = (TopTracksActivityFragment) fragmentManager.findFragmentByTag(TOP_TRACK_FRAGMENT_TAG);
        if (topTracksActivityFragment == null) {
            topTracksActivityFragment = new TopTracksActivityFragment();
            fragmentManager.beginTransaction()
                    .add(R.id.activity_fragment_top_tracks, topTracksActivityFragment, TOP_TRACK_FRAGMENT_TAG)
                    .commit();
        }

        mUserLeaveHintCallback = topTracksActivityFragment;

        if (savedInstanceState == null) {

            String artistId = Strings.EMPTY_STRING;
            String artistName = Strings.EMPTY_STRING;

            Intent intent = getIntent();
            if (intent != null) {
                if (intent.hasExtra(InfoKeys.KEY_ARTIST_ID)) {
                    artistId = intent.getStringExtra(InfoKeys.KEY_ARTIST_ID);
                }
                if (intent.hasExtra(InfoKeys.KEY_ARTIST_NAME)) {
                    artistName = intent.getStringExtra(InfoKeys.KEY_ARTIST_NAME);
                }
            }

            Bundle args = new Bundle();
            args.putString(InfoKeys.KEY_ARTIST_ID, artistId);
            args.putString(InfoKeys.KEY_ARTIST_NAME, artistName);

            topTracksActivityFragment.setArguments(args);
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();

        if (mUserLeaveHintCallback != null) {
            mUserLeaveHintCallback.onUserLeaveHintCallback();
        }
    }
}
