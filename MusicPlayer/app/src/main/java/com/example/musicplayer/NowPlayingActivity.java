package com.example.musicplayer;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NowPlayingActivity extends androidx.appcompat.app.AppCompatActivity {
    private static final float[] SPEEDS = {
            0.75f, 1f, 1.1f, 1.2f, 1.3f, 1.4f,
            1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2f
    };

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService queueExecutor = Executors.newSingleThreadExecutor();

    private ImageView albumArtwork;
    private TextView songTitle;
    private TextView songArtist;
    private TextView elapsedTime;
    private TextView totalTime;
    private SeekBar seekBar;
    private Button playPauseButton;
    private Button shuffleButton;
    private Button repeatButton;
    private Button speedButton;
    private boolean userSeeking;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;
    private MusicRepository repository;

    private final Player.Listener playerListener = new Player.Listener() {
        @Override public void onMediaMetadataChanged(MediaMetadata metadata) { refreshPlayerUi(); }
        @Override public void onIsPlayingChanged(boolean isPlaying) { refreshPlayerUi(); }
        @Override public void onPlaybackStateChanged(int state) { refreshPlayerUi(); }
        @Override public void onMediaItemTransition(MediaItem item, int reason) { refreshPlayerUi(); }
        @Override public void onShuffleModeEnabledChanged(boolean enabled) { refreshPlayerUi(); }
        @Override public void onRepeatModeChanged(int repeatMode) { refreshPlayerUi(); }
        @Override public void onPlaybackParametersChanged(PlaybackParameters parameters) {
            refreshPlayerUi();
        }
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
        repository = new MusicRepository(getContentResolver());

        albumArtwork = findViewById(R.id.nowPlayingArtwork);
        songTitle = findViewById(R.id.nowPlayingTitle);
        songArtist = findViewById(R.id.nowPlayingArtist);
        elapsedTime = findViewById(R.id.elapsedTime);
        totalTime = findViewById(R.id.totalTime);
        seekBar = findViewById(R.id.nowPlayingSeekBar);
        Button previousButton = findViewById(R.id.nowPlayingPrevious);
        playPauseButton = findViewById(R.id.nowPlayingPlayPause);
        Button nextButton = findViewById(R.id.nowPlayingNext);
        shuffleButton = findViewById(R.id.nowPlayingShuffle);
        repeatButton = findViewById(R.id.nowPlayingRepeat);
        speedButton = findViewById(R.id.nowPlayingSpeed);
        Button queueButton = findViewById(R.id.nowPlayingQueue);

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
        shuffleButton.setOnClickListener(v -> {
            if (controller != null) {
                controller.setShuffleModeEnabled(!controller.getShuffleModeEnabled());
            }
        });
        repeatButton.setOnClickListener(v -> cycleRepeatMode());
        speedButton.setOnClickListener(v -> cyclePlaybackSpeed());
        queueButton.setOnClickListener(v ->
                startActivity(new Intent(this, QueueActivity.class)));

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

                String selectedUri = getIntent().getStringExtra("playSongUri");
                if (selectedUri != null) {
                    prepareQueue(
                            selectedUri,
                            getIntent().getLongArrayExtra("albumIds"),
                            getIntent().getBooleanExtra("fromFolder", false),
                            getIntent().getStringExtra("folderPath"),
                            getIntent().getStringExtra("personalCollection"),
                            getIntent().getLongExtra("playlistId", -1)
                    );
                } else {
                    refreshPlayerUi();
                }
                progressHandler.post(updateProgress);
            } catch (Exception exception) {
                Toast.makeText(this, "Unable to connect to the player",
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void prepareQueue(
            String selectedUri, long[] albumIds, boolean fromFolder, String folderPath,
            String personalCollection, long playlistId) {
        queueExecutor.execute(() -> {
            try {
                List<Song> queue;
                if (personalCollection != null) {
                    queue = new PersonalLibraryRepository(this)
                            .getCollection(personalCollection, playlistId);
                } else if (albumIds != null) {
                    queue = repository.getSongsForAlbums(albumIds);
                } else if (fromFolder) {
                    queue = repository.getSongsForFolder(folderPath);
                } else {
                    queue = repository.getAllSongs();
                }
                List<MediaItem> mediaItems = new ArrayList<>();
                int selectedIndex = -1;

                for (int i = 0; i < queue.size(); i++) {
                    Song song = queue.get(i);
                    String uri = song.getContentUri().toString();
                    if (selectedUri.equals(uri)) selectedIndex = i;

                    MediaMetadata metadata = new MediaMetadata.Builder()
                            .setTitle(song.getTitle())
                            .setArtist(song.getArtist())
                            .build();
                    mediaItems.add(new MediaItem.Builder()
                            .setMediaId(uri)
                            .setUri(song.getContentUri())
                            .setMediaMetadata(metadata)
                            .build());
                }

                int startIndex = selectedIndex;
                runOnUiThread(() -> startQueue(mediaItems, startIndex));
            } catch (SecurityException exception) {
                runOnUiThread(() -> Toast.makeText(
                        this, "Audio permission is required for playback",
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private void startQueue(List<MediaItem> mediaItems, int selectedIndex) {
        if (controller == null || isDestroyed()) return;
        if (selectedIndex < 0 || mediaItems.isEmpty()) {
            Toast.makeText(this, "Unable to find this soundtrack",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        controller.setMediaItems(mediaItems, selectedIndex, 0);
        controller.setShuffleModeEnabled(
                getIntent().getBooleanExtra("startShuffled", false));
        controller.prepare();
        controller.play();
    }

    private void playPrevious() {
        if (controller == null) return;
        if (controller.getCurrentPosition() > 5000) {
            controller.seekTo(0);
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem();
        }
    }

    private void cycleRepeatMode() {
        if (controller == null) return;
        int current = controller.getRepeatMode();
        if (current == Player.REPEAT_MODE_OFF) {
            controller.setRepeatMode(Player.REPEAT_MODE_ALL);
        } else if (current == Player.REPEAT_MODE_ALL) {
            controller.setRepeatMode(Player.REPEAT_MODE_ONE);
        } else {
            controller.setRepeatMode(Player.REPEAT_MODE_OFF);
        }
    }

    private void cyclePlaybackSpeed() {
        if (controller == null) return;
        float current = controller.getPlaybackParameters().speed;
        int nextIndex = 0;
        for (int i = 0; i < SPEEDS.length; i++) {
            if (Math.abs(current - SPEEDS[i]) < 0.01f) {
                nextIndex = (i + 1) % SPEEDS.length;
                break;
            }
        }
        controller.setPlaybackSpeed(SPEEDS[nextIndex]);
    }

    private void refreshPlayerUi() {
        if (controller == null || controller.getMediaItemCount() == 0) return;

        MediaMetadata metadata = controller.getMediaMetadata();
        songTitle.setText(metadata.title == null ? "Unknown soundtrack" : metadata.title);
        songArtist.setText(metadata.artist == null ? "Unknown artist" : metadata.artist);
        playPauseButton.setText(controller.isPlaying() ? "Ⅱ" : "▶");
        shuffleButton.setText(controller.getShuffleModeEnabled() ? "Shuffle On" : "Shuffle");

        int repeatMode = controller.getRepeatMode();
        repeatButton.setText(repeatMode == Player.REPEAT_MODE_ONE
                ? "Repeat 1"
                : repeatMode == Player.REPEAT_MODE_ALL ? "Repeat All" : "Repeat Off");
        speedButton.setText(String.format(
                Locale.getDefault(), "%.2g×", controller.getPlaybackParameters().speed));

        long duration = controller.getDuration();
        if (duration > 0) {
            seekBar.setMax((int) Math.min(duration, Integer.MAX_VALUE));
            totalTime.setText(formatTime(duration));
        }

        MediaItem item = controller.getCurrentMediaItem();
        if (item != null && !item.mediaId.isEmpty()) {
            ArtworkLoader.loadInto(this, albumArtwork, Uri.parse(item.mediaId));
        } else {
            albumArtwork.setImageResource(R.drawable.player_art_placeholder);
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
        queueExecutor.shutdownNow();
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
