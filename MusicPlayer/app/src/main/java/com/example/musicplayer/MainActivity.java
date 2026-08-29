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
import java.util.Locale;
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
            intent.putExtra("albumIds", album.getAlbumIds());
            intent.putExtra("albumName", album.name);
            startActivity(intent);
        });
        recyclerAlbums.setAdapter(albumAdapter);
        recyclerAlbums.setVisibility(View.GONE);

        Intent intent = getIntent();

        if (intent != null && intent.getBooleanExtra("playFromAlbum", false)) {
            long[] albumIds = intent.getLongArrayExtra("albumIds");
            String songPath = intent.getStringExtra("playSongPath");

            Log.d("PLAYER", "Album IDs received: "
                    + java.util.Arrays.toString(albumIds) + ", songPath: " + songPath);

            playList.clear();
            for (Song s : allSongs) {
                if (containsAlbumId(albumIds, s.albumId)) {
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
     * Groups tracks by normalized album title + artist instead of ALBUM_ID.
     * Some Android media scanners assign multiple IDs to one real album.
     */
    void loadAlbums() {
        Map<String, Album> uniqueAlbums = new LinkedHashMap<>();

        String[] projection = {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID
        };

        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                MediaStore.Audio.Media.ALBUM + " COLLATE NOCASE ASC"
        );

        if (cursor != null) {
            int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            while (cursor.moveToNext()) {
                String albumName = cleanMetadata(cursor.getString(albumColumn), "Unknown album");
                String artist = cleanMetadata(cursor.getString(artistColumn), "Unknown artist");
                String path = cursor.getString(pathColumn);
                long albumId = cursor.getLong(albumIdColumn);

                String albumKey = normalizeMetadata(albumName) + "\u0000"
                        + normalizeMetadata(artist);
                Album album = uniqueAlbums.get(albumKey);
                if (album == null) {
                    album = new Album(albumName, artist);
                    uniqueAlbums.put(albumKey, album);
                }
                album.addTrack(albumId, path);
            }
            cursor.close();
        }

        albums.clear();
        albums.addAll(uniqueAlbums.values());
        Log.d("ALBUM", "Loaded merged albums: " + albums.size());
    }

    private String cleanMetadata(String value, String fallback) {
        if (value == null || value.trim().isEmpty() || "<unknown>".equalsIgnoreCase(value.trim())) {
            return fallback;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeMetadata(String value) {
        return cleanMetadata(value, "").toLowerCase(Locale.ROOT);
    }

    private boolean containsAlbumId(long[] albumIds, long albumId) {
        if (albumIds == null) {
            return false;
        }
        for (long id : albumIds) {
            if (id == albumId) {
                return true;
            }
        }
        return false;
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
