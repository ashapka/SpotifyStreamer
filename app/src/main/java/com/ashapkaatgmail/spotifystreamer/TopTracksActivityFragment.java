package com.ashapkaatgmail.spotifystreamer;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ashapkaatgmail.spotifystreamer.Adapters.TrackAdapter;
import com.ashapkaatgmail.spotifystreamer.Helpers.HashMapWrapperParcelable;
import com.ashapkaatgmail.spotifystreamer.Helpers.InfoKeys;
import com.ashapkaatgmail.spotifystreamer.Helpers.Strings;
import com.ashapkaatgmail.spotifystreamer.Helpers.UserLeaveHintCallbackInterface;
import com.ashapkaatgmail.spotifystreamer.Helpers.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;


public class TopTracksActivityFragment extends Fragment
        implements UserLeaveHintCallbackInterface {

    private final String MEDIA_PLAYER_FRAGMENT_TAG = "MEDIA_PLAYER_FRAGMENT";

    private String mArtistId = Strings.EMPTY_STRING;
    private String mArtistName = Strings.EMPTY_STRING;

    private UserLeaveHintCallbackInterface mUserLeaveHintCallback;

    private int mTopTracksSize;
    private TrackAdapter mTrackAdapter;

    private ProgressBar mSpinner;

    private boolean mNeedLoadTracks = false;

    private boolean mIsLargeLayout;

    public TopTracksActivityFragment() {
        // empty
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsLargeLayout = getResources().getBoolean(R.bool.large_layout);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        MediaPlayerActivityFragment mediaPlayerActivityFragment = (MediaPlayerActivityFragment) fragmentManager.findFragmentByTag(MEDIA_PLAYER_FRAGMENT_TAG);
        if (mediaPlayerActivityFragment != null) {
            mUserLeaveHintCallback = mediaPlayerActivityFragment;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_top_tracks, container, false);

        mSpinner = (ProgressBar)rootView.findViewById(R.id.progressBarTopTracks);
        mSpinner.setVisibility(View.GONE);

        Bundle args = getArguments();
        if (args != null) {
            mArtistId = args.getString(InfoKeys.KEY_ARTIST_ID, Strings.EMPTY_STRING);
            mArtistName = args.getString(InfoKeys.KEY_ARTIST_NAME, Strings.EMPTY_STRING);
        }


        ListView topTracksView = (ListView) rootView.findViewById(R.id.listview_toptracks);
        topTracksView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArrayList<HashMapWrapperParcelable<String, String>> topTracks = mTrackAdapter.getData();

                showMediaPlayer(topTracks, position);
            }

        });

        ArrayList<HashMapWrapperParcelable<String, String>> infoList = null;

        if (savedInstanceState != null) {
            mNeedLoadTracks = false;

            infoList = savedInstanceState.getParcelableArrayList("mTrackAdapter");

            mTopTracksSize = savedInstanceState.getInt("mTopTracksSize");
            setTitle();

        } else {
            mNeedLoadTracks = true;
        }

        if (infoList == null) {
            infoList = new ArrayList<>();
        }

        mTrackAdapter = new TrackAdapter(getActivity(), infoList);
        topTracksView.setAdapter(mTrackAdapter);

        return rootView;
    }

    private void setTitle() {

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        switch (mTopTracksSize) {
            case 0:
                activity.setTitle(getString(R.string.title_activity_top_tracks_empty));
                break;

            case 1:
                activity.setTitle(getString(R.string.title_activity_top_one_track));
                break;

            default:
                activity.setTitle(String.format(getString(R.string.title_activity_top_tracks_custom), mTopTracksSize));
                break;
        }

        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setSubtitle(mArtistName);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mTrackAdapter != null) {
            outState.putParcelableArrayList("mTrackAdapter", mTrackAdapter.getData());
            outState.putInt("mTopTracksSize", mTopTracksSize);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNeedLoadTracks && mArtistId.length() > 0) {
            if (Utils.isNetworkAvailable(getActivity())) {
                FetchTopTracksTask topTracksTask = new FetchTopTracksTask();
                topTracksTask.execute(mArtistId);
            } else {
                new AlertDialog.Builder(getActivity())
                        .setTitle("Error")
                        .setMessage("Network is not available.")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        }
    }

    private void showMediaPlayer(ArrayList<HashMapWrapperParcelable<String, String>> topTracks, int currentPosition) {

        Bundle args = new Bundle();
        args.putParcelableArrayList(InfoKeys.KEY_TOP_TRACKS_LIST, topTracks);
        args.putInt(InfoKeys.KEY_SELECTED_TRACK_POSITION, currentPosition);
        args.putString(InfoKeys.KEY_ARTIST_NAME, mArtistName);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        MediaPlayerActivityFragment mediaPlayerActivityFragment = new MediaPlayerActivityFragment();
        mediaPlayerActivityFragment.setArguments(args);

        mUserLeaveHintCallback = mediaPlayerActivityFragment;

        if (mIsLargeLayout) {
            // The device is using a large layout, so show the fragment as a dialog
            mediaPlayerActivityFragment.show(fragmentManager, MEDIA_PLAYER_FRAGMENT_TAG);
        } else {
            // The device is smaller, so show the fragment fullscreen
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            // For a little polish, specify a transition animation
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            // To make it fullscreen, use the 'content' root view as the container
            // for the fragment, which is always the root view for the activity
            transaction.add(android.R.id.content, mediaPlayerActivityFragment, MEDIA_PLAYER_FRAGMENT_TAG)
                    .addToBackStack(null).commit();
        }
    }

    @Override
    public void onUserLeaveHintCallback() {
        if (mUserLeaveHintCallback != null) {
            mUserLeaveHintCallback.onUserLeaveHintCallback();
        }
    }

    private final class FetchTopTracksTask extends AsyncTask<String, Void, ArrayList<HashMapWrapperParcelable<String, String>>> {

        private final String LOG_TAG = FetchTopTracksTask.class.getSimpleName();

        @Override
        protected void onPreExecute() {
            mSpinner.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onCancelled() {
            mSpinner.setVisibility(View.GONE);
        }

        @Override
        protected void onPostExecute(ArrayList<HashMapWrapperParcelable<String, String>> info) {

            mTrackAdapter.clear();

            mTopTracksSize = 0;
            if (info != null) {
                mTopTracksSize = info.size();
            }
            setTitle();

            if (mTopTracksSize > 0) {
                mTrackAdapter.addAll(info);
            } else {
                Toast toast = Toast.makeText(getActivity(), getActivity().getString(R.string.top_tracks_result_is_empty), Toast.LENGTH_LONG);
                toast.show();
            }

            mSpinner.setVisibility(View.GONE);
        }

        @Override
        protected ArrayList<HashMapWrapperParcelable<String, String>> doInBackground(String... params) {

            if (params == null || params.length == 0) {
                return null;
            }

            String query;

            try {
                query = params[0];

                Map<String, Object> options = new HashMap<>();
                options.put("country", getString(R.string.spotify_option_country));

                SpotifyApi spotifyApi = new SpotifyApi();
                SpotifyService spotifyService = spotifyApi.getService();
                Tracks topTracks = spotifyService.getArtistTopTrack(query, options);

                ArrayList<HashMapWrapperParcelable<String, String>> result = new ArrayList<>();

                for (int i = 0; i < topTracks.tracks.size(); ++i) {
                    Track track = topTracks.tracks.get(i);

                    HashMapWrapperParcelable<String, String> map = new HashMapWrapperParcelable<>();
                    map.put(InfoKeys.KEY_ALBUM_NAME, track.album.name);
                    map.put(InfoKeys.KEY_TRACK_NAME, track.name);
                    map.put(InfoKeys.KEY_TRACK_PREVIEW_URL, track.preview_url);
                    map.put(InfoKeys.KEY_TRACK_DURATION_MS, String.valueOf(track.duration_ms));

                    if (track.album.images.size()>0) {
                        Image image = track.album.images.get(track.album.images.size() - 1);
                        map.put(InfoKeys.KEY_THUMB_URL, image.url);

                        image = track.album.images.get(0);
                        map.put(InfoKeys.KEY_THUMB_LARGE_URL, image.url);
                    } else {
                        map.put(InfoKeys.KEY_THUMB_URL, null);
                        map.put(InfoKeys.KEY_THUMB_LARGE_URL, null);
                    }
                    result.add(map);
                }

                return result;

            } catch (Exception e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            }
        }
    }
}
