package com.example.musicplayer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.database.Cursor;
import android.provider.MediaStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import android.util.Log;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {

    MediaPlayer mediaPlayer;
    TextView currentSongTitle;
    Button btnPlayPause, btnNext, btnPrev;
    SeekBar songSeekBar;
    boolean isTracking = false;

    SongAdapter songAdapter;
    List<Album> albums = new ArrayList<>();
    int currentSongIndex = -1;

    TabLayout tabLayout;
    List<Song> allSongs = new ArrayList<>();
    List<Song> playList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        loadSongs();
        loadAlbums();

        currentSongTitle = findViewById(R.id.currentSongTitle);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        songSeekBar = findViewById(R.id.songSeekBar);
        tabLayout = findViewById(R.id.tabLayout);

        tabLayout.addTab(tabLayout.newTab().setText("Songs"));
        tabLayout.addTab(tabLayout.newTab().setText("Albums"));

        RecyclerView recyclerView = findViewById(R.id.recyclerSongs);
        RecyclerView recyclerAlbums = findViewById(R.id.recyclerAlbums);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerAlbums.setLayoutManager(new LinearLayoutManager(this));

        AlbumAdapter albumAdapter = new AlbumAdapter(this, albums, album -> {
            Intent intent = new Intent(MainActivity.this, AlbumActivity.class);
            intent.putExtra("albumId", album.albumId);
            intent.putExtra("albumName", album.name);
            startActivity(intent);
        });
        recyclerAlbums.setAdapter(albumAdapter);
        recyclerAlbums.setVisibility(View.GONE);

        Intent intent = getIntent();

        if (intent != null && intent.getBooleanExtra("playFromAlbum", false)) {
            long albumId = intent.getLongExtra("albumId", -1);
            String songPath = intent.getStringExtra("playSongPath");

            Log.d("PLAYER", "AlbumId received: " + albumId + ", songPath: " + songPath);

            playList.clear();
            for (Song s : allSongs) {
                if (s.albumId == albumId) {
                    playList.add(s);
                }
            }

            for (int i = 0; i < playList.size(); i++) {
                if (playList.get(i).getPath().equals(songPath)) {
                    currentSongIndex = i;
                    break;
                }
            }
        }

        SongAdapter adapter = new SongAdapter(playList, song -> {
            currentSongIndex = playList.indexOf(song);
            playSong(song);
        });

        recyclerView.setAdapter(adapter);

        btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.start();
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentSongIndex < playList.size() - 1) {
                currentSongIndex++;
                playSong(playList.get(currentSongIndex));
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (currentSongIndex > 0) {
                currentSongIndex--;
                playSong(playList.get(currentSongIndex));
            }
        });

        songSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    try {
                        mediaPlayer.seekTo(progress);
                    } catch (IllegalStateException ignored) {
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTracking = false;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTracking = true;
            }
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    recyclerView.setVisibility(View.VISIBLE);
                    recyclerAlbums.setVisibility(View.GONE);
                } else {
                    recyclerView.setVisibility(View.GONE);
                    recyclerAlbums.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{android.Manifest.permission.READ_MEDIA_AUDIO},
                    1
            );
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (currentSongIndex >= 0 && currentSongIndex < playList.size()) {
            playSong(playList.get(currentSongIndex));
        }
    }

    private void loadSongs() {
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
        };

        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"
        );

        if (cursor != null) {
            int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            while (cursor.moveToNext()) {
                String title = cursor.getString(titleColumn);
                String artist = cursor.getString(artistColumn);
                String path = cursor.getString(pathColumn);
                long duration = cursor.getLong(durationColumn);
                long albumId = cursor.getLong(albumIdColumn);

                allSongs.add(new Song(title, artist, path, duration, albumId));
            }
            cursor.close();
        }

        playList = new ArrayList<>(allSongs);
        Log.d("SONG", "Loaded songs: " + allSongs.size());
    }

    /**
     * Builds the album list from the same music-track dataset used by the Songs tab.
     * This avoids relying on MediaStore.Audio.Albums rows, which can contain
     * duplicate metadata records or omit an album for an otherwise indexed track.
     */
    void loadAlbums() {
        Map<Long, Album> uniqueAlbums = new LinkedHashMap<>();

        for (Song song : allSongs) {
            if (song.albumId < 0 || uniqueAlbums.containsKey(song.albumId)) {
                continue;
            }

            String albumName = "Unknown album";
            String artist = song.getArtist();
            String albumArt = null;

            Cursor cursor = getContentResolver().query(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    new String[]{
                            MediaStore.Audio.Albums.ALBUM,
                            MediaStore.Audio.Albums.ARTIST,
                            MediaStore.Audio.Albums.ALBUM_ART
                    },
                    MediaStore.Audio.Albums._ID + "=?",
                    new String[]{String.valueOf(song.albumId)},
                    null
            );

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    albumName = cursor.getString(
                            cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
                    String albumArtist = cursor.getString(
                            cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST));
                    if (albumArtist != null && !albumArtist.trim().isEmpty()) {
                        artist = albumArtist;
                    }
                    albumArt = cursor.getString(
                            cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART));
                }
                cursor.close();
            }

            uniqueAlbums.put(
                    song.albumId,
                    new Album(albumName, artist, String.valueOf(song.albumId), albumArt)
            );
        }

        albums.clear();
        albums.addAll(uniqueAlbums.values());
        Log.d("ALBUM", "Loaded unique albums: " + albums.size());
    }

    void playSong(Song song) {
        try {
            isTracking = false;

            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(song.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            currentSongTitle.setText(song.getTitle());
            songSeekBar.setMax(mediaPlayer.getDuration());
            songSeekBar.setProgress(0);
            isTracking = true;

            new Thread(() -> {
                while (isTracking && mediaPlayer != null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    try {
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            runOnUiThread(() -> {
                                if (!songSeekBar.isPressed() && mediaPlayer != null) {
                                    try {
                                        songSeekBar.setProgress(mediaPlayer.getCurrentPosition());
                                    } catch (IllegalStateException ignored) {
                                    }
                                }
                            });
                        }
                    } catch (IllegalStateException e) {
                        break;
                    }
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Bitmap getAlbumArt(String path) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(path);
            byte[] art = retriever.getEmbeddedPicture();
            retriever.release();

            if (art != null) {
                return BitmapFactory.decodeByteArray(art, 0, art.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        isTracking = false;
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}
