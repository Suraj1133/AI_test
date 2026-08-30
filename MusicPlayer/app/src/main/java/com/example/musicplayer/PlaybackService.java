package com.example.musicplayer;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaybackService extends MediaSessionService {
    private static final String PREFS = "playback_state";
    private static final String KEY_QUEUE = "queue";
    private static final String KEY_INDEX = "index";
    private static final String KEY_POSITION = "position";
    private static final String KEY_REPEAT = "repeat";
    private static final String KEY_SHUFFLE = "shuffle";
    private static final String KEY_SPEED = "speed";

    private final Handler stateHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService historyExecutor = Executors.newSingleThreadExecutor();
    private MediaSession mediaSession;
    private ExoPlayer player;
    private PersonalLibraryRepository personalRepository;
    private boolean recordedCurrentItem;

    private final Runnable recordPlay = () -> {
        if (player == null || !player.isPlaying() || recordedCurrentItem) return;
        MediaItem item = player.getCurrentMediaItem();
        if (item == null) return;
        recordedCurrentItem = true;
        historyExecutor.execute(() -> personalRepository.recordPlayed(item));
    };

    private final Runnable periodicSave = new Runnable() {
        @Override
        public void run() {
            savePlaybackState();
            stateHandler.postDelayed(this, 5000);
        }
    };

    private final Player.Listener stateListener = new Player.Listener() {
        @Override
        public void onEvents(Player player, Player.Events events) {
            savePlaybackState();
        }

        @Override
        public void onMediaItemTransition(MediaItem item, int reason) {
            recordedCurrentItem = false;
            schedulePlayRecording();
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            if (isPlaying) schedulePlayRecording();
            else stateHandler.removeCallbacks(recordPlay);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        personalRepository = new PersonalLibraryRepository(this);
        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .build();
        player.addListener(stateListener);

        Intent nowPlayingIntent = new Intent(this, NowPlayingActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent sessionActivity = PendingIntent.getActivity(
                this,
                0,
                nowPlayingIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        mediaSession = new MediaSession.Builder(this, player)
                .setSessionActivity(sessionActivity)
                .build();
        DefaultMediaNotificationProvider notificationProvider =
                new DefaultMediaNotificationProvider.Builder(this).build();
        notificationProvider.setSmallIcon(R.drawable.ic_notification_music);
        setMediaNotificationProvider(notificationProvider);

        restorePlaybackState();
        stateHandler.post(periodicSave);
    }

    private void schedulePlayRecording() {
        stateHandler.removeCallbacks(recordPlay);
        if (player != null && player.isPlaying() && !recordedCurrentItem) {
            stateHandler.postDelayed(recordPlay, 30000);
        }
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    private void savePlaybackState() {
        if (player == null || player.getMediaItemCount() == 0) return;

        try {
            JSONArray queue = new JSONArray();
            for (int i = 0; i < player.getMediaItemCount(); i++) {
                MediaItem item = player.getMediaItemAt(i);
                JSONObject json = new JSONObject();
                json.put("id", item.mediaId);
                json.put("title", item.mediaMetadata.title == null
                        ? "" : item.mediaMetadata.title.toString());
                json.put("artist", item.mediaMetadata.artist == null
                        ? "" : item.mediaMetadata.artist.toString());
                queue.put(json);
            }

            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString(KEY_QUEUE, queue.toString())
                    .putInt(KEY_INDEX, player.getCurrentMediaItemIndex())
                    .putLong(KEY_POSITION, player.getCurrentPosition())
                    .putInt(KEY_REPEAT, player.getRepeatMode())
                    .putBoolean(KEY_SHUFFLE, player.getShuffleModeEnabled())
                    .putFloat(KEY_SPEED, player.getPlaybackParameters().speed)
                    .apply();
        } catch (Exception ignored) {
        }
    }

    private void restorePlaybackState() {
        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        String savedQueue = preferences.getString(KEY_QUEUE, null);
        if (savedQueue == null) return;

        try {
            JSONArray array = new JSONArray(savedQueue);
            List<MediaItem> items = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.getJSONObject(i);
                String id = json.getString("id");
                MediaMetadata metadata = new MediaMetadata.Builder()
                        .setTitle(json.optString("title", "Unknown soundtrack"))
                        .setArtist(json.optString("artist", "Unknown artist"))
                        .build();
                items.add(new MediaItem.Builder()
                        .setMediaId(id)
                        .setUri(Uri.parse(id))
                        .setMediaMetadata(metadata)
                        .build());
            }
            if (items.isEmpty()) return;

            int index = Math.max(0, Math.min(
                    preferences.getInt(KEY_INDEX, 0), items.size() - 1));
            long position = Math.max(0, preferences.getLong(KEY_POSITION, 0));

            player.setMediaItems(items, index, position);
            player.setRepeatMode(preferences.getInt(KEY_REPEAT, Player.REPEAT_MODE_OFF));
            player.setShuffleModeEnabled(preferences.getBoolean(KEY_SHUFFLE, false));
            player.setPlaybackParameters(new PlaybackParameters(
                    preferences.getFloat(KEY_SPEED, 1f)));
            player.prepare();
            player.pause();
        } catch (Exception exception) {
            preferences.edit().clear().apply();
        }
    }

    @Override
    public void onDestroy() {
        stateHandler.removeCallbacks(periodicSave);
        stateHandler.removeCallbacks(recordPlay);
        savePlaybackState();
        historyExecutor.shutdown();
        if (mediaSession != null) {
            player.removeListener(stateListener);
            player.release();
            mediaSession.release();
            player = null;
            mediaSession = null;
        }
        super.onDestroy();
    }
}
