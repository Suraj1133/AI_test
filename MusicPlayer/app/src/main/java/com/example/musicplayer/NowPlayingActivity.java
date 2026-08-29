package com.example.musicplayer;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NowPlayingActivity extends AppCompatActivity {
    private final List<Song> queue = new ArrayList<>();
    private final Handler progressHandler = new Handler(Looper.getMainLooper());

    private MediaPlayer mediaPlayer;
    private ImageView albumArtwork;
    private TextView songTitle;
    private TextView songArtist;
    private TextView elapsedTime;
    private TextView totalTime;
    private SeekBar seekBar;
    private Button playPauseButton;
    private int currentIndex = -1;
    private boolean userSeeking;

    private final Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && !userSeeking) {
                try {
                    int position = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(position);
                    elapsedTime.setText(formatTime(position));
                } catch (IllegalStateException ignored) {
                }
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

        loadQueue(getIntent().getLongArrayExtra("albumIds"));
        String selectedPath = getIntent().getStringExtra("playSongPath");
        currentIndex = findSongIndex(selectedPath);

        previousButton.setOnClickListener(v -> playPrevious());
        nextButton.setOnClickListener(v -> playNext());
        playPauseButton.setOnClickListener(v -> togglePlayPause());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) elapsedTime.setText(formatTime(progress));
            }

            @Override public void onStartTrackingTouch(SeekBar bar) {
                userSeeking = true;
            }

            @Override public void onStopTrackingTouch(SeekBar bar) {
                if (mediaPlayer != null) {
                    try {
                        mediaPlayer.seekTo(bar.getProgress());
                    } catch (IllegalStateException ignored) {
                    }
                }
                userSeeking = false;
            }
        });

        if (currentIndex >= 0) {
            playSong(queue.get(currentIndex));
        } else {
            Toast.makeText(this, "Unable to find this soundtrack", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadQueue(long[] albumIds) {
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
        if (path == null) return -1;
        for (int i = 0; i < queue.size(); i++) {
            if (path.equals(queue.get(i).getPath())) return i;
        }
        return -1;
    }

    private void playSong(Song song) {
        releasePlayer();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(song.getPath());
            mediaPlayer.setOnCompletionListener(player -> playNextAutomatically());
            mediaPlayer.prepare();
            mediaPlayer.start();

            songTitle.setText(song.getTitle());
            songArtist.setText(song.getArtist());
            seekBar.setMax(mediaPlayer.getDuration());
            seekBar.setProgress(0);
            elapsedTime.setText(formatTime(0));
            totalTime.setText(formatTime(mediaPlayer.getDuration()));
            playPauseButton.setText("Ⅱ");
            showArtwork(song.getPath());
        } catch (Exception exception) {
            Toast.makeText(this, "Could not play this soundtrack", Toast.LENGTH_SHORT).show();
        }
    }

    private void playNextAutomatically() {
        if (currentIndex < queue.size() - 1) {
            currentIndex++;
            playSong(queue.get(currentIndex));
        } else {
            playPauseButton.setText("▶");
            seekBar.setProgress(seekBar.getMax());
            elapsedTime.setText(totalTime.getText());
        }
    }

    private void playNext() {
        if (currentIndex < queue.size() - 1) {
            currentIndex++;
            playSong(queue.get(currentIndex));
        }
    }

    private void playPrevious() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.getCurrentPosition() > 5000) {
                    mediaPlayer.seekTo(0);
                    return;
                }
            } catch (IllegalStateException ignored) {
            }
        }
        if (currentIndex > 0) {
            currentIndex--;
            playSong(queue.get(currentIndex));
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                playPauseButton.setText("▶");
            } else {
                mediaPlayer.start();
                playPauseButton.setText("Ⅱ");
            }
        } catch (IllegalStateException ignored) {
        }
    }

    private void showArtwork(String path) {
        albumArtwork.setImageResource(R.drawable.player_art_placeholder);
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            byte[] bytes = retriever.getEmbeddedPicture();
            if (bytes != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                albumArtwork.setImageBitmap(bitmap);
            }
        } catch (Exception ignored) {
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    private String formatTime(long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours > 0
                ? String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
                : String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(null);
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        progressHandler.post(updateProgress);
    }

    @Override
    protected void onStop() {
        progressHandler.removeCallbacks(updateProgress);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        progressHandler.removeCallbacks(updateProgress);
        releasePlayer();
        super.onDestroy();
    }
}
