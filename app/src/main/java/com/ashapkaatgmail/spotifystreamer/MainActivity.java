package com.ashapkaatgmail.spotifystreamer;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v7.app.AppCompatActivity;

import com.ashapkaatgmail.spotifystreamer.Helpers.HashMapWrapperParcelable;
import com.ashapkaatgmail.spotifystreamer.Helpers.InfoKeys;
import com.ashapkaatgmail.spotifystreamer.Helpers.SearchRecentSuggestionsProviderImpl;
import com.ashapkaatgmail.spotifystreamer.Helpers.UserLeaveHintCallbackInterface;


public class MainActivity extends AppCompatActivity implements MainActivityFragment.Callback {

    private static final String TOPTRACKS_FRAGMENT_TAG = "TTFTAG";
    private boolean mTwoPane;
    private UserLeaveHintCallbackInterface mUserLeaveHintCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getIntent() != null) {
            handleIntent(getIntent());
        }

        if (findViewById(R.id.activity_fragment_top_tracks) != null) {
            mTwoPane = true;

            TopTracksActivityFragment fragment = (TopTracksActivityFragment)getSupportFragmentManager().findFragmentByTag(TOPTRACKS_FRAGMENT_TAG);
            if (fragment == null) {
                fragment = new TopTracksActivityFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.activity_fragment_top_tracks, fragment, TOPTRACKS_FRAGMENT_TAG)
                        .commit();
            }
            mUserLeaveHintCallback = fragment;

        } else {
            mTwoPane = false;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        // Special processing of the incoming intent only occurs if the action specified
        // by the intent is ACTION_SEARCH.
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    SearchRecentSuggestionsProviderImpl.AUTHORITY, SearchRecentSuggestionsProviderImpl.MODE);
            suggestions.saveRecentQuery(query, null);

            if (mTwoPane) {
                TopTracksActivityFragment fragment = new TopTracksActivityFragment();
                mUserLeaveHintCallback = fragment;
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.activity_fragment_top_tracks, fragment, TOPTRACKS_FRAGMENT_TAG)
                        .commit();
            }

            // do the actual search on the fragment
            MainActivityFragment mainActivityFragment = (MainActivityFragment)
                    getSupportFragmentManager().findFragmentById(R.id.activity_fragment_main);

            mainActivityFragment.searchArtists(query);

        }
    }

    @Override
    public void onItemSelected(HashMapWrapperParcelable<String, String> artistData) {

        String artistId = artistData.get(InfoKeys.KEY_ARTIST_ID);
        String artistName = artistData.get(InfoKeys.KEY_ARTIST_NAME);

        if (mTwoPane) {
            Bundle args = new Bundle();
            args.putString(InfoKeys.KEY_ARTIST_ID, artistId);
            args.putString(InfoKeys.KEY_ARTIST_NAME, artistName);

            TopTracksActivityFragment fragment = new TopTracksActivityFragment();
            fragment.setArguments(args);

            mUserLeaveHintCallback = fragment;

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.activity_fragment_top_tracks, fragment, TOPTRACKS_FRAGMENT_TAG)
                    .commit();
        } else {

            Intent topTracksIntent = new Intent(this, TopTracksActivity.class);
            topTracksIntent.putExtra(InfoKeys.KEY_ARTIST_ID, artistId);
            topTracksIntent.putExtra(InfoKeys.KEY_ARTIST_NAME, artistName);

            startActivity(topTracksIntent);
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

