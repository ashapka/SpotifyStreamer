package com.ashapkaatgmail.spotifystreamer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;


public class TopTracksActivityFragment extends Fragment {

    private String mArtistId;
    private TrackAdapter mTrackAdapter;

    private ProgressBar mSpinner;

    private boolean mNeedLoadTracks = false;


    public TopTracksActivityFragment() {
        // empty
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_top_tracks, container, false);

        mSpinner = (ProgressBar)rootView.findViewById(R.id.progressBarTopTracks);
        mSpinner.setVisibility(View.GONE);

        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra(InfoKeys.KEY_ARTIST_ID)) {
            mArtistId = intent.getStringExtra(InfoKeys.KEY_ARTIST_ID);
        }

        ListView topTracksView = (ListView) rootView.findViewById(R.id.listview_toptracks);
        ArrayList<HashMap<String, String>> infoList = new ArrayList<>();

        if (savedInstanceState != null) {
            mNeedLoadTracks = false;
            infoList = (ArrayList<HashMap<String, String>>) savedInstanceState.getSerializable("mTrackAdapter");
            if (infoList == null) {
                infoList = new ArrayList<>();
            }
        } else {
            mNeedLoadTracks = true;
        }

        mTrackAdapter = new TrackAdapter(getActivity(), infoList);
        topTracksView.setAdapter(mTrackAdapter);

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        if (mTrackAdapter != null) {
            outState.putSerializable("mTrackAdapter", mTrackAdapter.getData());
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNeedLoadTracks) {
            FetchTopTracksTask topTracksTask = new FetchTopTracksTask();
            topTracksTask.execute(mArtistId);
        }
    }

    private final class FetchTopTracksTask extends AsyncTask<String, Void, ArrayList<HashMap<String, String>>> {

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
        protected void onPostExecute(ArrayList<HashMap<String, String>> info) {

            mTrackAdapter.clear();

            if (info != null && info.size() > 0) {
                getActivity().setTitle(String.format(getString(R.string.title_activity_top_tracks_custom), info.size()));
                mTrackAdapter.addAll(info);
            } else {
                getActivity().setTitle(getString(R.string.title_activity_top_tracks_empty));
                Toast toast = Toast.makeText(getActivity(), getActivity().getString(R.string.result_is_empty), Toast.LENGTH_LONG);
                toast.show();
            }

            mSpinner.setVisibility(View.GONE);
        }

        @Override
        protected ArrayList<HashMap<String, String>> doInBackground(String... params) {

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

                ArrayList<HashMap<String, String>> result = new ArrayList<>();

                for (int i = 0; i < topTracks.tracks.size(); ++i) {
                    Track track = topTracks.tracks.get(i);

                    HashMap<String, String> map = new HashMap<>();
                    map.put(InfoKeys.KEY_ALBUM_NAME, track.album.name);
                    map.put(InfoKeys.KEY_TRACK_NAME, track.name);

                    if (track.album.images.size()>0) {
                        Image image = track.album.images.get(track.album.images.size() - 1);
                        map.put(InfoKeys.KEY_THUMB_URL, image.url);
                    } else {
                        map.put(InfoKeys.KEY_THUMB_URL, null);
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