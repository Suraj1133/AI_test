package com.example.musicplayer;

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
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlaybackService extends MediaSessionService {
    private static final String PREFS = "playback_state";
    private static final String KEY_QUEUE = "queue";
    private static final String KEY_INDEX = "index";
    private static final String KEY_POSITION = "position";
    private static final String KEY_REPEAT = "repeat";
    private static final String KEY_SHUFFLE = "shuffle";
    private static final String KEY_SPEED = "speed";

    private final Handler stateHandler = new Handler(Looper.getMainLooper());
    private MediaSession mediaSession;
    private ExoPlayer player;

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
    };

    @Override
    public void onCreate() {
        super.onCreate();
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .build();
        player.addListener(stateListener);
        mediaSession = new MediaSession.Builder(this, player).build();

        restorePlaybackState();
        stateHandler.post(periodicSave);
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
        savePlaybackState();
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
