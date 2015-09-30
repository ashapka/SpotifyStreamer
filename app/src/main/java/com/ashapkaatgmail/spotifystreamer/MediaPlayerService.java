package com.ashapkaatgmail.spotifystreamer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class MediaPlayerService extends Service
        implements MediaPlayer.OnPreparedListener,
                   MediaPlayer.OnCompletionListener,
                   MediaPlayer.OnErrorListener {

    private static final long PROGRESS_UPDATE_INTERNAL = 100;

    public interface ServiceClientListener {
        void mediaPlayerIsPreparing();
        void mediaPlayerOnPrepared(int durationMs);
        void mediaPlayerOnCompletion();
        void mediaPlayerOnProgressUpdated(int timeElapsedMs);
        boolean mediaPlayerOnError();
    }

    private WeakReference<ServiceClientListener> mClient;
    public void setServiceClient(ServiceClientListener client) {
        if (client == null) {
            mClient = null;
            return;
        }
        mClient = new WeakReference<ServiceClientListener>(client);
    }

    public class MediaPlayerBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    private MediaPlayer mMediaPlayer = null;
    private boolean mIsMediaPlayerPaused = false;
    private final IBinder mBinder = new MediaPlayerBinder();

    private final Handler mDurationHandler = new Handler();
    private int mTimeElapsedMs = 0;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mClient.get().mediaPlayerOnCompletion();
        resetTimeElapsed();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        int durationMs = mp.getDuration();
        mClient.get().mediaPlayerOnPrepared(durationMs);

        resetTimeElapsed();

        startTrack();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        mp.release();
        mMediaPlayer = null;

        resetTimeElapsed();

        return mClient.get().mediaPlayerOnError();
    }

    public void playTrack (String url) throws IOException {

        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
        }

        if (mIsMediaPlayerPaused) {
            startTrack();
        } else {
            mClient.get().mediaPlayerIsPreparing();

            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(url);
            mMediaPlayer.prepareAsync();
        }
    }

    public void pauseTrack() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mIsMediaPlayerPaused = true;

            mDurationHandler.removeCallbacks(mProgressUpdater);
        }
    }

    public void stopTrack() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mIsMediaPlayerPaused = false;

            resetTimeElapsed();
        }
    }

    private void startTrack() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            mIsMediaPlayerPaused = false;

            mTimeElapsedMs = getCurrentPosition();
            updateProgress(mTimeElapsedMs);
            mDurationHandler.postDelayed(mProgressUpdater, PROGRESS_UPDATE_INTERNAL);
        }
    }

    public void seekTrackTo(int toMs) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(toMs);
        }
    }

    public boolean isTrackPlaying() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    private int getCurrentPosition() {
        int result = 0;
        if (mMediaPlayer != null) {
            result = mMediaPlayer.getCurrentPosition();
        }
        if (result <= 0) {
            return 0;
        }

        return result;
    }

    private void updateProgress(int timeElapsedMs) {
        mClient.get().mediaPlayerOnProgressUpdated(timeElapsedMs);
    }

    private void resetTimeElapsed() {
        mDurationHandler.removeCallbacks(mProgressUpdater);
        mTimeElapsedMs = 0;
        mClient.get().mediaPlayerOnProgressUpdated(mTimeElapsedMs);
    }

    private final Runnable mProgressUpdater = new Runnable() {
        @Override
        public void run() {
            mTimeElapsedMs = getCurrentPosition();
            updateProgress(mTimeElapsedMs);
            mDurationHandler.postDelayed(this, PROGRESS_UPDATE_INTERNAL);
        }
    };

}