package com.example.musicplayer;

import android.content.ComponentName;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NowPlayingActivity extends androidx.appcompat.app.AppCompatActivity {
    private final List<Song> queue = new ArrayList<>();
    private final Handler progressHandler = new Handler(Looper.getMainLooper());

    private ImageView albumArtwork;
    private TextView songTitle;
    private TextView songArtist;
    private TextView elapsedTime;
    private TextView totalTime;
    private SeekBar seekBar;
    private Button playPauseButton;
    private boolean userSeeking;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;

    private final Player.Listener playerListener = new Player.Listener() {
        @Override public void onMediaMetadataChanged(MediaMetadata metadata) { refreshPlayerUi(); }
        @Override public void onIsPlayingChanged(boolean isPlaying) { refreshPlayerUi(); }
        @Override public void onPlaybackStateChanged(int state) { refreshPlayerUi(); }
        @Override public void onMediaItemTransition(MediaItem item, int reason) { refreshPlayerUi(); }
    };

    private final Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (controller != null && !userSeeking) {
                long position = controller.getCurrentPosition();
                seekBar.setProgress((int) Math.min(position, Integer.MAX_VALUE));
                elapsedTime.setText(formatTime(position));
            }
            progressHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        albumArtwork = findViewById(R.id.nowPlayingArtwork);
        songTitle = findViewById(R.id.nowPlayingTitle);
        songArtist = findViewById(R.id.nowPlayingArtist);
        elapsedTime = findViewById(R.id.elapsedTime);
        totalTime = findViewById(R.id.totalTime);
        seekBar = findViewById(R.id.nowPlayingSeekBar);
        Button previousButton = findViewById(R.id.nowPlayingPrevious);
        playPauseButton = findViewById(R.id.nowPlayingPlayPause);
        Button nextButton = findViewById(R.id.nowPlayingNext);

        previousButton.setOnClickListener(v -> playPrevious());
        nextButton.setOnClickListener(v -> {
            if (controller != null && controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem();
            }
        });
        playPauseButton.setOnClickListener(v -> {
            if (controller == null) return;
            if (controller.isPlaying()) controller.pause(); else controller.play();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) elapsedTime.setText(formatTime(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { userSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                if (controller != null) controller.seekTo(bar.getProgress());
                userSeeking = false;
            }
        });

        connectController();
    }

    private void connectController() {
        SessionToken token = new SessionToken(
                this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                controller.addListener(playerListener);

                String selectedPath = getIntent().getStringExtra("playSongPath");
                if (selectedPath != null) {
                    loadQueue(getIntent().getLongArrayExtra("albumIds"));
                    int selectedIndex = findSongIndex(selectedPath);
                    if (selectedIndex >= 0) {
                        startQueue(selectedIndex);
                    } else {
                        Toast.makeText(this, "Unable to find this soundtrack",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                refreshPlayerUi();
                progressHandler.post(updateProgress);
            } catch (Exception exception) {
                Toast.makeText(this, "Unable to connect to the player",
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startQueue(int selectedIndex) {
        List<MediaItem> mediaItems = new ArrayList<>();
        for (Song song : queue) {
            byte[] artwork = getArtwork(song.getPath());
            MediaMetadata.Builder metadata = new MediaMetadata.Builder()
                    .setTitle(song.getTitle())
                    .setArtist(song.getArtist());
            if (artwork != null) {
                metadata.setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER);
            }
            mediaItems.add(new MediaItem.Builder()
                    .setMediaId(song.getPath())
                    .setUri(song.getPath())
                    .setMediaMetadata(metadata.build())
                    .build());
        }

        controller.setMediaItems(mediaItems, selectedIndex, 0);
        controller.prepare();
        controller.play();
    }

    private void loadQueue(long[] albumIds) {
        queue.clear();
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String[] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC";

        if (albumIds != null && albumIds.length > 0) {
            StringBuilder placeholders = new StringBuilder();
            selectionArgs = new String[albumIds.length];
            for (int i = 0; i < albumIds.length; i++) {
                if (i > 0) placeholders.append(",");
                placeholders.append("?");
                selectionArgs[i] = String.valueOf(albumIds[i]);
            }
            selection += " AND " + MediaStore.Audio.Media.ALBUM_ID
                    + " IN (" + placeholders + ")";
            sortOrder = MediaStore.Audio.Media.TRACK + " ASC, "
                    + MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC";
        }

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, sortOrder
        )) {
            if (cursor == null) return;
            int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            while (cursor.moveToNext()) {
                queue.add(new Song(
                        cursor.getString(titleColumn),
                        cursor.getString(artistColumn),
                        cursor.getString(pathColumn),
                        cursor.getLong(durationColumn),
                        cursor.getLong(albumIdColumn)
                ));
            }
        }
    }

    private int findSongIndex(String path) {
        for (int i = 0; i < queue.size(); i++) {
            if (path.equals(queue.get(i).getPath())) return i;
        }
        return -1;
    }

    private void playPrevious() {
        if (controller == null) return;
        if (controller.getCurrentPosition() > 5000) {
            controller.seekTo(0);
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem();
        }
    }

    private void refreshPlayerUi() {
        if (controller == null || controller.getMediaItemCount() == 0) return;

        MediaMetadata metadata = controller.getMediaMetadata();
        songTitle.setText(metadata.title == null ? "Unknown soundtrack" : metadata.title);
        songArtist.setText(metadata.artist == null ? "Unknown artist" : metadata.artist);
        playPauseButton.setText(controller.isPlaying() ? "Ⅱ" : "▶");

        long duration = controller.getDuration();
        if (duration > 0) {
            seekBar.setMax((int) Math.min(duration, Integer.MAX_VALUE));
            totalTime.setText(formatTime(duration));
        }

        if (metadata.artworkData != null) {
            albumArtwork.setImageBitmap(BitmapFactory.decodeByteArray(
                    metadata.artworkData, 0, metadata.artworkData.length));
        } else {
            albumArtwork.setImageResource(R.drawable.player_art_placeholder);
        }
    }

    private byte[] getArtwork(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            return retriever.getEmbeddedPicture();
        } catch (Exception ignored) {
            return null;
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }
    }

    private String formatTime(long milliseconds) {
        long safeValue = Math.max(milliseconds, 0);
        long totalSeconds = safeValue / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours > 0
                ? String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
                : String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        progressHandler.removeCallbacks(updateProgress);
        if (controller != null) {
            controller.removeListener(playerListener);
            controller = null;
        }
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            controllerFuture = null;
        }
        super.onDestroy();
    }
}
