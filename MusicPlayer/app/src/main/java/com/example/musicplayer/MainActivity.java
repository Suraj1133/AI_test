package com.example.musicplayer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private final List<Album> albums = new ArrayList<>();
    private final List<Song> allSongs = new ArrayList<>();
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        loadSongs();
        loadAlbums();

        tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Songs"));
        tabLayout.addTab(tabLayout.newTab().setText("Albums"));

        RecyclerView recyclerSongs = findViewById(R.id.recyclerSongs);
        RecyclerView recyclerAlbums = findViewById(R.id.recyclerAlbums);
        recyclerSongs.setLayoutManager(new LinearLayoutManager(this));
        recyclerAlbums.setLayoutManager(new LinearLayoutManager(this));

        recyclerSongs.setAdapter(new SongAdapter(allSongs, song -> openPlayer(song.getPath(), null)));
        recyclerAlbums.setAdapter(new AlbumAdapter(this, albums, album -> {
            Intent intent = new Intent(this, AlbumActivity.class);
            intent.putExtra("albumIds", album.getAlbumIds());
            intent.putExtra("albumName", album.name);
            startActivity(intent);
        }));
        recyclerAlbums.setVisibility(View.GONE);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean songsSelected = tab.getPosition() == 0;
                recyclerSongs.setVisibility(songsSelected ? View.VISIBLE : View.GONE);
                recyclerAlbums.setVisibility(songsSelected ? View.GONE : View.VISIBLE);
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        requestAudioPermissionIfNeeded();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void openPlayer(String songPath, long[] albumIds) {
        Intent intent = new Intent(this, NowPlayingActivity.class);
        intent.putExtra("playSongPath", songPath);
        if (albumIds != null) {
            intent.putExtra("albumIds", albumIds);
        }
        startActivity(intent);
    }

    private void requestAudioPermissionIfNeeded() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? android.Manifest.permission.READ_MEDIA_AUDIO
                : android.Manifest.permission.READ_EXTERNAL_STORAGE;
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, 1);
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

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"
        )) {
            if (cursor == null) return;
            int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            while (cursor.moveToNext()) {
                allSongs.add(new Song(
                        cursor.getString(titleColumn),
                        cursor.getString(artistColumn),
                        cursor.getString(pathColumn),
                        cursor.getLong(durationColumn),
                        cursor.getLong(albumIdColumn)
                ));
            }
        }
        Log.d("SONG", "Loaded songs: " + allSongs.size());
    }

    void loadAlbums() {
        Map<String, Album> uniqueAlbums = new LinkedHashMap<>();
        String[] projection = {
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID
        };

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                MediaStore.Audio.Media.ALBUM + " COLLATE NOCASE ASC"
        )) {
            if (cursor == null) return;
            int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            while (cursor.moveToNext()) {
                String albumName = cleanMetadata(cursor.getString(albumColumn), "Unknown album");
                String artist = cleanMetadata(cursor.getString(artistColumn), "Unknown artist");
                String key = normalizeMetadata(albumName) + "\u0000" + normalizeMetadata(artist);
                Album album = uniqueAlbums.get(key);
                if (album == null) {
                    album = new Album(albumName, artist);
                    uniqueAlbums.put(key, album);
                }
                album.addTrack(cursor.getLong(albumIdColumn), cursor.getString(pathColumn));
            }
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
}
