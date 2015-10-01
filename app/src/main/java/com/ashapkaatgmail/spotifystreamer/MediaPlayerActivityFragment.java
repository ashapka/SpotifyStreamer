package com.ashapkaatgmail.spotifystreamer;


import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ashapkaatgmail.spotifystreamer.Helpers.HashMapWrapperParcelable;
import com.ashapkaatgmail.spotifystreamer.Helpers.InfoKeys;
import com.ashapkaatgmail.spotifystreamer.Helpers.Strings;
import com.ashapkaatgmail.spotifystreamer.Helpers.UserLeaveHintCallbackInterface;
import com.ashapkaatgmail.spotifystreamer.Helpers.Utils;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MediaPlayerActivityFragment extends DialogFragment
        implements MediaPlayerService.ServiceClientListener,
                    UserLeaveHintCallbackInterface{

    private boolean mIsMediaPlayerServiceBound = false;
    private boolean mIsFragmentFirstTimeCreated = false;
    private boolean mPleaseStopMediaPlayer = false;
    private MediaPlayerService mService = null;

    private String mPreviewUrl;
    private String mDurationMsString = Strings.EMPTY_STRING;
    private int mTimeElapsedMs = 0;


    private ImageButton mButtonPause;
    private ImageButton mButtonPlay;
    private TextView mTextViewDuration;
    private TextView mTextViewProgress;
    private ProgressBar mPrgrsBarLoadingMedia;
    private SeekBar mSeekBar;

    private int mCurrentTrackListPosition = 0;
    private String mArtistName;
    private ArrayList<HashMapWrapperParcelable<String, String>> mTopTracks;
    private ArrayList<HashMap<String, String>> mTopTracksCopy;


    public MediaPlayerActivityFragment() {
        // empty
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.MediaPlayerDialog);
        setCancelable(true);

        final FragmentManager fm = getFragmentManager();
        fm.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                int backCount = fm.getBackStackEntryCount();

                if (backCount == 0) {
                    mPleaseStopMediaPlayer = true;
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_media_player, container, false);

        if (savedInstanceState != null) {
            mCurrentTrackListPosition = savedInstanceState.getInt("mCurrentTrackListPosition");
            mDurationMsString = savedInstanceState.getString("mDurationMsString");
            mTimeElapsedMs = savedInstanceState.getInt("mTimeElapsedMs");
        }

        Bundle args = getArguments();
        if (args != null) {
            if (savedInstanceState == null) {
                mCurrentTrackListPosition = args.getInt(InfoKeys.KEY_SELECTED_TRACK_POSITION);
                mIsFragmentFirstTimeCreated = true;
            }
            mTopTracks = args.getParcelableArrayList(InfoKeys.KEY_TOP_TRACKS_LIST);
            mArtistName = args.getString(InfoKeys.KEY_ARTIST_NAME);

            // something weird is going on with the mTopTracks in the following scenario:
            // 1. Press a device Home button;
            // 2. Select an application from a list of apps that are on a background using a device button/feature
            // 3. Only onStart is fired but values in the mTopTracks are valid which is expected
            // 4. Press on any MediaPlayer navigation buttons -- the mTopTracks data are gone.
            // The following is a hack when we copy data over to a local variable. It works.
            // Perhaps something is wrong with the HashMapWrapperParcelable implementation or how a framework is holding references to it.
            mTopTracksCopy = new ArrayList<HashMap<String, String>>();
            for(HashMapWrapperParcelable <String, String> topTrackMapEntry : mTopTracks) {
                HashMap<String, String> map = topTrackMapEntry.getHashMap();
                mTopTracksCopy.add(map);
            }

            loadRootView(rootView);

            // start playback as soon as the dialog is launched
            if (savedInstanceState == null) {
                playTrack();
            }
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        makeActionBarVisible(false);
    }

    @Override
    public void onStart() {
        super.onStart();

        Intent intent = new Intent(getActivity(), MediaPlayerService.class);
        getActivity().startService(intent);
        getActivity().bindService(intent, mMediaPlayerConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        mPleaseStopMediaPlayer = true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("mCurrentTrackListPosition", mCurrentTrackListPosition);
        outState.putString("mDurationMsString", mDurationMsString);
        outState.putInt("mTimeElapsedMs", mTimeElapsedMs);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unbindServiceGracefully();
        makeActionBarVisible(true);
    }

    private void makeActionBarVisible(boolean makeVisible) {

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                if (makeVisible) {
                    actionBar.show();
                } else {
                    actionBar.hide();
                }
            }
        }
    }

    private void unbindServiceGracefully() {
        if (mIsMediaPlayerServiceBound) {
            if (mPleaseStopMediaPlayer) {
                mService.stopTrack();
            }

            FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.unbindService(mMediaPlayerConnection);
                mIsMediaPlayerServiceBound = false;
                if (activity.isFinishing()) {
                    Intent intent = new Intent(activity, MediaPlayerService.class);
                    activity.stopService(intent);
                }
            }
        }
    }

    private void loadRootView(View view) {

        mSeekBar = (SeekBar) view.findViewById(R.id.seekBar);

        TextView textViewArtistName = (TextView) view.findViewById(R.id.text_artist_name);
        textViewArtistName.setText(mArtistName);

        String albumName = mTopTracksCopy.get(mCurrentTrackListPosition).get(InfoKeys.KEY_ALBUM_NAME);
        TextView textViewAlbumName = (TextView) view.findViewById(R.id.text_album_name);
        textViewAlbumName.setText(albumName);

        String albumArtworkUrl = mTopTracksCopy.get(mCurrentTrackListPosition).get(InfoKeys.KEY_THUMB_LARGE_URL);
        ImageView imageViewAlbumArtwork = (ImageView) view.findViewById(R.id.image_album_artwork);
        if (albumArtworkUrl != null) {
            Picasso.with(getActivity()).load(albumArtworkUrl).into(imageViewAlbumArtwork);
        }

        String trackName = mTopTracksCopy.get(mCurrentTrackListPosition).get(InfoKeys.KEY_TRACK_NAME);
        TextView textViewTrackName = (TextView) view.findViewById(R.id.text_track_name);
        textViewTrackName.setText(
                String.format("%d. %s", mCurrentTrackListPosition + 1, trackName));

        mTextViewDuration = (TextView) view.findViewById(R.id.text_track_duration);

        int durationMs;
        if (mDurationMsString.length() == 0) {
            mDurationMsString = mTopTracksCopy.get(mCurrentTrackListPosition).get(InfoKeys.KEY_TRACK_DURATION_MS);
        }
        durationMs = Integer.parseInt(mDurationMsString);
        setDuration(durationMs);

        mTextViewProgress = (TextView) view.findViewById(R.id.text_track_progress);
        mTextViewProgress.setText(Utils.formatMillis(mTimeElapsedMs));

        mPreviewUrl = mTopTracksCopy.get(mCurrentTrackListPosition).get(InfoKeys.KEY_TRACK_PREVIEW_URL);

        ImageButton buttonNext = (ImageButton) view.findViewById(R.id.button_media_next);
        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadNextTrack(view);
            }
        });

        ImageButton buttonPrevious = (ImageButton) view.findViewById(R.id.button_media_previous);
        buttonPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadPreviousTrack(view);
            }
        });

        mButtonPlay = (ImageButton) view.findViewById(R.id.button_media_play);
        mButtonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playTrack();
            }
        });

        mButtonPause = (ImageButton) view.findViewById(R.id.button_media_pause);
        mButtonPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseTrack();
            }
        });

        mPrgrsBarLoadingMedia = (ProgressBar)view.findViewById(R.id.loadingMediaPanel);


        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mTextViewProgress.setText(Utils.formatMillis(progress));
                    mService.seekTrackTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // do nothing
            }
        });

    }

    private void loadNextTrack(View view)
    {
        if (mIsMediaPlayerServiceBound) {
            mService.stopTrack();
        }

        unhidePlayButton();

        // move to the first track in case the last one is currently selected
        if (mCurrentTrackListPosition == mTopTracksCopy.size() - 1) {
            mCurrentTrackListPosition = 0;
        } else {
            ++mCurrentTrackListPosition;
        }

        mDurationMsString = Strings.EMPTY_STRING;
        loadRootView(view.getRootView());

        playTrack();
    }

    private void loadPreviousTrack(View view)
    {
        if (mIsMediaPlayerServiceBound) {
            mService.stopTrack();
        }

        unhidePlayButton();

        // move to the last track in case the first one is currently selected
        if (mCurrentTrackListPosition == 0) {
            mCurrentTrackListPosition = mTopTracksCopy.size() - 1;
        } else {
            --mCurrentTrackListPosition;
        }

        mDurationMsString = Strings.EMPTY_STRING;
        loadRootView(view.getRootView());

        playTrack();
    }

    private void playTrack() {

        hidePlayButton();

        if (mIsMediaPlayerServiceBound) {
            try {
                mService.playTrack(mPreviewUrl);
            } catch (IOException e) {
                new AlertDialog.Builder(getActivity())
                        .setTitle("Error")
                        .setMessage("Cannot preview the tack due to the URL error or connection issue.")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                unhidePlayButton();
            }
        }
    }

    private void pauseTrack() {
        unhidePlayButton();

        if (mIsMediaPlayerServiceBound) {
            mService.pauseTrack();
        }
    }

    private void hidePlayButton() {
        mButtonPlay.setVisibility(View.INVISIBLE);
        mButtonPause.setVisibility(View.VISIBLE);
    }

    private void unhidePlayButton() {
        mButtonPlay.setVisibility(View.VISIBLE);
        mButtonPause.setVisibility(View.INVISIBLE);
    }

    private void setDuration(int durationMs) {
        mTextViewDuration.setText(Utils.formatMillis(durationMs));
        mSeekBar.setMax(durationMs);
    }

    private final ServiceConnection mMediaPlayerConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mService = ((MediaPlayerService.MediaPlayerBinder)service).getService();
            mService.setServiceClient(MediaPlayerActivityFragment.this);
            mIsMediaPlayerServiceBound = true;
            if (mService.isTrackPlaying()) {
                hidePlayButton();
            }
            if (mIsFragmentFirstTimeCreated) {
                playTrack();
                mIsFragmentFirstTimeCreated = false;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService.setServiceClient(null);
            mIsMediaPlayerServiceBound = false;
            mService = null;
        }
    };

    @Override
    public void mediaPlayerIsPreparing() {
        mPrgrsBarLoadingMedia.setVisibility(View.VISIBLE);
    }

    @Override
    public void mediaPlayerOnPrepared(int durationMs) {

        setDuration(durationMs);
        mDurationMsString = String.valueOf(durationMs);
        mPrgrsBarLoadingMedia.setVisibility(View.INVISIBLE);
    }

    @Override
    public void mediaPlayerOnCompletion() {
        unhidePlayButton();
        mSeekBar.setProgress(0);
        mTextViewProgress.setText(Utils.formatMillis(0));
    }

    @Override
    public void mediaPlayerOnProgressUpdated(int timeElapsedMs) {
        mTimeElapsedMs = timeElapsedMs;
        mSeekBar.setProgress(timeElapsedMs);
        mTextViewProgress.setText(Utils.formatMillis(timeElapsedMs));
    }

    @Override
    public boolean mediaPlayerOnError() {

        new AlertDialog.Builder(getActivity())
                .setTitle("Error")
                .setMessage("MediaPlayer Error. Please try again.")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

        return true;
    }

    @Override
    public void onUserLeaveHintCallback() {
        mPleaseStopMediaPlayer = true;
        unhidePlayButton();
        unbindServiceGracefully();
    }
}
