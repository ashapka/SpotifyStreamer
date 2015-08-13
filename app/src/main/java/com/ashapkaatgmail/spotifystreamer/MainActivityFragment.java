package com.ashapkaatgmail.spotifystreamer;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;

public class MainActivityFragment extends Fragment {

    private static final CharSequence WILDCARD = "*";
    private static final CharSequence DASH = "-";

    private ProgressBar mSpinner;
    private TextView mEnterArtistLabel;

    private ArtistAdapter mArtistAdapter;

    private boolean mIsNewActivity;

    // settings
    private int mSearchLimit = 20;

    public MainActivityFragment() {
        // empty
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);

        // Get the SearchView and set the searchable configuration
        MenuItem menuItem = menu.findItem(R.id.action_search);

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setIconifiedByDefault(false);

        if (mIsNewActivity) {
            MenuItemCompat.expandActionView(menuItem);
            mIsNewActivity = false;
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_clear_search) {
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
                    SearchRecentSuggestionsProviderImpl.AUTHORITY, SearchRecentSuggestionsProviderImpl.MODE);
            suggestions.clearHistory();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mSpinner = (ProgressBar)rootView.findViewById(R.id.progressBar);
        mSpinner.setVisibility(View.GONE);

        ListView artistsView = (ListView) rootView.findViewById(R.id.listview_search_result);
        artistsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                HashMapWrapperParcelable<String, String> map = mArtistAdapter.getData().get(position);
                String artistId = map.get(InfoKeys.KEY_ARTIST_ID);
                String artistName = map.get(InfoKeys.KEY_ARTIST_NAME);

                Intent topTracksIntent = new Intent(getActivity(), TopTracksActivity.class);
                topTracksIntent.putExtra(InfoKeys.KEY_ARTIST_ID, artistId);
                topTracksIntent.putExtra(InfoKeys.KEY_ARTIST_NAME, artistName);
                startActivity(topTracksIntent);

            }
        });

        ArrayList<HashMapWrapperParcelable<String, String>> infoList = null;

        if (savedInstanceState == null) {
            readSettings();
        } else {
            mSearchLimit = savedInstanceState.getInt("mSearchLimitSetting");

            infoList = savedInstanceState.getParcelableArrayList("mArtistAdapter");
        }

        if (infoList == null) {
            infoList = new ArrayList<>();
        }

        mArtistAdapter = new ArtistAdapter(getActivity(), infoList);
        artistsView.setAdapter(mArtistAdapter);

        mEnterArtistLabel = (TextView)rootView.findViewById(R.id.enter_artist_label);
        if (infoList.size() == 0) {
            mEnterArtistLabel.setVisibility(View.VISIBLE);
        } else {
            mEnterArtistLabel.setVisibility(View.GONE);
        }


        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        if (mArtistAdapter != null) {
            outState.putParcelableArrayList("mArtistAdapter", mArtistAdapter.getData());

            outState.putInt("mSearchLimitSetting", mSearchLimit);
        }


    }

    public void searchArtists(String query) {

        // append wildcard to the end of the query
        // note from spotify api docs:
        //      The asterisk (*) character can, with some limitations, be used as a wildcard
        //      (maximum: 2 per query). It will match a variable number of non-white-space characters.
        //      It cannot be used in a quoted phrase, in a field filter, when there is a dash ("-")
        //      in the query, or as the first character of the keyword string.
        if (!query.contains(DASH) && !query.contains(WILDCARD)) {
            query += WILDCARD;
        }

        FetchArtistsTask artistsTask = new FetchArtistsTask();
        artistsTask.execute(query, String.valueOf(mSearchLimit));
    }

    private void readSettings() {

        Resources res = getResources();

        try {
            mSearchLimit = res.getInteger(R.integer.search_limit);
            if (mSearchLimit < 1 || mSearchLimit > 20) {
                mSearchLimit = 20;
            }
        } catch (Resources.NotFoundException e) {
            mSearchLimit = 20;
        }

        mIsNewActivity = true;

    }

    private final class FetchArtistsTask extends AsyncTask<String, Void, ArrayList<HashMapWrapperParcelable<String, String>>> {

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
        protected void onPostExecute(ArrayList<HashMapWrapperParcelable<String, String>> info) {

            mArtistAdapter.clear();

            if (info != null && info.size() > 0) {
                mArtistAdapter.addAll(info);
                mEnterArtistLabel.setVisibility(View.GONE);
            } else {
                mEnterArtistLabel.setVisibility(View.VISIBLE);
                Toast toast = Toast.makeText(getActivity(), getActivity().getString(R.string.result_is_empty), Toast.LENGTH_LONG);
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
            int searchLimit;

            try {
                query = params[0];
                searchLimit = Integer.valueOf(params[1]);

                SpotifyApi spotifyApi = new SpotifyApi();
                SpotifyService spotifyService = spotifyApi.getService();
                ArtistsPager spotifyResults = spotifyService.searchArtists(query);

                ArrayList<HashMapWrapperParcelable<String, String>> result = new ArrayList<>();
                for (int i = 0; i < searchLimit; ++i) {
                    Artist artist = spotifyResults.artists.items.get(i);

                    HashMapWrapperParcelable<String, String> map = new HashMapWrapperParcelable<>();
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
