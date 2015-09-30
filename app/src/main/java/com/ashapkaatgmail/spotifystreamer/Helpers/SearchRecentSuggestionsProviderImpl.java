package com.ashapkaatgmail.spotifystreamer.Helpers;

import android.content.SearchRecentSuggestionsProvider;


public class SearchRecentSuggestionsProviderImpl extends SearchRecentSuggestionsProvider{
    public final static String AUTHORITY = "com.ashapkaatgmail.spotifystreamer.Helpers.SearchRecentSuggestionsProviderImpl";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public SearchRecentSuggestionsProviderImpl() {
        setupSuggestions(AUTHORITY, MODE);
    }
}
