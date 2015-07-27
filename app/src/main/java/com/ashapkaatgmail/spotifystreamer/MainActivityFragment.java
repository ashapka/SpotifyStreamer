package com.ashapkaatgmail.spotifystreamer;

import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;

public class MainActivityFragment extends Fragment {

    private static final int TRIGGER_SERACH = 1;
    private static final CharSequence WILDCARD = "*";
    private static final CharSequence DASH = "-";
    private final EntryHandler mHandler = new EntryHandler(this);

    private ProgressBar mSpinner;
    private EditText mSearchArtist;
    private ArtistAdapter mArtistAdapter;

    // settings
    private long mResponseDelayMs = 2000;
    private int mSearchLimit = 20;

    public MainActivityFragment() {
        // empty
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mSpinner = (ProgressBar)rootView.findViewById(R.id.progressBar);
        mSpinner.setVisibility(View.GONE);

        ListView artistsView = (ListView) rootView.findViewById(R.id.listview_search_result);
        artistsView.setOnItemClickListener(new AdapterView.OnItemClickListener()  {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            HashMap<String, String> map = mArtistAdapter.getData().get(position);
            String artistId = map.get(InfoKeys.KEY_ARTIST_ID);
            String artistName = map.get(InfoKeys.KEY_ARTIST_NAME);

            Intent topTracksIntent = new Intent(getActivity(), TopTracksActivity.class);
            topTracksIntent.putExtra(InfoKeys.KEY_ARTIST_ID, artistId);
            topTracksIntent.putExtra(InfoKeys.KEY_ARTIST_NAME, artistName);
            startActivity(topTracksIntent);

        }});

        mSearchArtist = (EditText) rootView.findViewById(R.id.edittext_search_artist);

        ArrayList<HashMap<String, String>> infoList = new ArrayList<>();

        if (savedInstanceState == null) {
            readSettings();
        } else {

            mResponseDelayMs = savedInstanceState.getLong("mResponseDelayMsSetting");
            mSearchLimit = savedInstanceState.getInt("mSearchLimitSetting");

            infoList = (ArrayList<HashMap<String, String>>)savedInstanceState.getSerializable("mArtistAdapter");
            if (infoList == null) {
                infoList = new ArrayList<>();
            }
        }

        mArtistAdapter = new ArtistAdapter(getActivity(), infoList);
        artistsView.setAdapter(mArtistAdapter);

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        // preserve the list values
        if (mArtistAdapter != null) {

            outState.putSerializable("mArtistAdapter", mArtistAdapter.getData());

            outState.putLong("mResponseDelayMsSetting", mResponseDelayMs);
            outState.putInt("mSearchLimitSetting", mSearchLimit);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSearchArtist.addTextChangedListener(new TextWatcherImpl(mResponseDelayMs, TRIGGER_SERACH));
    }

    private void searchArtists() {

        // append wildcard to the end of the query
        // note from spotify api docs:
        //      The asterisk (*) character can, with some limitations, be used as a wildcard
        //      (maximum: 2 per query). It will match a variable number of non-white-space characters.
        //      It cannot be used in a quoted phrase, in a field filter, when there is a dash ("-")
        //      in the query, or as the first character of the keyword string.

        EditText searchArtist = (EditText) getActivity().findViewById(R.id.edittext_search_artist);
        String query = searchArtist.getText().toString();
        if (!query.contains(DASH) && !query.contains(WILDCARD)) {
            query += WILDCARD;
        }

        FetchArtistsTask artistsTask = new FetchArtistsTask();
        artistsTask.execute(query, String.valueOf(mSearchLimit));
    }

    private void readSettings() {

        Resources res = getResources();

        try {
            mResponseDelayMs = res.getInteger(R.integer.textchanged_response_delay_ms);
        } catch (Resources.NotFoundException e) {
            mResponseDelayMs = 2000;
        }

        try {
            mSearchLimit = res.getInteger(R.integer.search_limit);
            if (mSearchLimit < 1 || mSearchLimit > 20) {
                mSearchLimit = 20;
            }
        } catch (Resources.NotFoundException e) {
            mSearchLimit = 20;
        }

    }

    /**
     * Trigger the search when user stops typing
     */
    private final class TextWatcherImpl implements TextWatcher {

        private final long mResponseDelayMs;
        private final int mHandlerMessageCode;

        public TextWatcherImpl(long responseDelayMs, int handlerMessageCode) {
            super();

            mResponseDelayMs = responseDelayMs;
            mHandlerMessageCode = handlerMessageCode;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // empty
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // empty
        }

        /**
         * Delay the search execution by queueing a request.
         */
        @Override
        public void afterTextChanged(Editable s) {
            mHandler.removeMessages(mHandlerMessageCode);
            mHandler.sendEmptyMessageDelayed(mHandlerMessageCode, mResponseDelayMs);
        }
    }

    private static class EntryHandler extends Handler {

        private final WeakReference<MainActivityFragment> mService;

        public EntryHandler(MainActivityFragment fragment) {
            mService = new WeakReference<MainActivityFragment>(fragment);

        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == TRIGGER_SERACH) {
                MainActivityFragment fragment = mService.get();
                fragment.searchArtists();
            }
        }
    }

    private final class FetchArtistsTask extends AsyncTask<String, Void, ArrayList<HashMap<String, String>>> {

        private final String LOG_TAG = FetchArtistsTask.class.getSimpleName();

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

            mArtistAdapter.clear();

            if (info != null && info.size() > 0) {
                mArtistAdapter.addAll(info);
            } else {
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
            int searchLimit;

            try {
                query = params[0];
                searchLimit = Integer.valueOf(params[1]);

                SpotifyApi spotifyApi = new SpotifyApi();
                SpotifyService spotifyService = spotifyApi.getService();
                ArtistsPager spotifyResults = spotifyService.searchArtists(query);

                ArrayList<HashMap<String, String>> result = new ArrayList<>();
                for (int i = 0; i < searchLimit; ++i) {
                    Artist artist = spotifyResults.artists.items.get(i);

                    HashMap<String, String> map = new HashMap<>();
                    map.put(InfoKeys.KEY_ARTIST_ID, artist.id);
                    map.put(InfoKeys.KEY_ARTIST_NAME, artist.name);

                    if (artist.images.size() > 0) {
                        Image image = artist.images.get(artist.images.size() - 1);
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
