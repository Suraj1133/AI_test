package com.example.musicplayer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST = 100;

    private final List<Album> albums = new ArrayList<>();
    private final List<Song> allSongs = new ArrayList<>();
    private final ExecutorService libraryExecutor = Executors.newSingleThreadExecutor();

    private MusicRepository repository;
    private SongAdapter songAdapter;
    private AlbumAdapter albumAdapter;
    private MiniPlayerController miniPlayerController;
    private boolean libraryLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        repository = new MusicRepository(getContentResolver());
        miniPlayerController = new MiniPlayerController(this);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Songs"));
        tabLayout.addTab(tabLayout.newTab().setText("Albums"));

        RecyclerView recyclerSongs = findViewById(R.id.recyclerSongs);
        RecyclerView recyclerAlbums = findViewById(R.id.recyclerAlbums);
        recyclerSongs.setLayoutManager(new LinearLayoutManager(this));
        recyclerAlbums.setLayoutManager(new LinearLayoutManager(this));

        songAdapter = new SongAdapter(allSongs, song -> openPlayer(song.getContentUri().toString()));
        albumAdapter = new AlbumAdapter(this, albums, album -> {
            Intent intent = new Intent(this, AlbumActivity.class);
            intent.putExtra("albumIds", album.getAlbumIds());
            intent.putExtra("albumName", album.name);
            startActivity(intent);
        });
        recyclerSongs.setAdapter(songAdapter);
        recyclerAlbums.setAdapter(albumAdapter);
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        if (hasAudioPermission()) {
            loadLibrary();
            requestNotificationPermissionIfNeeded();
        } else {
            requestRequiredPermissions();
        }
    }

    private void openPlayer(String songUri) {
        Intent intent = new Intent(this, NowPlayingActivity.class);
        intent.putExtra("playSongUri", songUri);
        startActivity(intent);
    }

    private boolean hasAudioPermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? android.Manifest.permission.READ_MEDIA_AUDIO
                : android.Manifest.permission.READ_EXTERNAL_STORAGE;
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? android.Manifest.permission.READ_MEDIA_AUDIO
                : android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS);
        }
        requestPermissions(permissions.toArray(new String[0]), PERMISSION_REQUEST);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    PERMISSION_REQUEST
            );
        }
    }

    private void loadLibrary() {
        if (libraryLoaded) return;
        libraryLoaded = true;

        libraryExecutor.execute(() -> {
            try {
                List<Song> songs = repository.getAllSongs();
                List<Album> loadedAlbums = repository.getAlbums();
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    allSongs.clear();
                    allSongs.addAll(songs);
                    albums.clear();
                    albums.addAll(loadedAlbums);
                    songAdapter.notifyDataSetChanged();
                    albumAdapter.notifyDataSetChanged();
                });
            } catch (SecurityException exception) {
                libraryLoaded = false;
                runOnUiThread(() -> Toast.makeText(
                        this, "Audio permission is required to load music",
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (hasAudioPermission()) {
                loadLibrary();
            } else {
                Toast.makeText(this,
                        "Allow audio access to display your music library",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        miniPlayerController.connect();
    }

    @Override
    protected void onStop() {
        miniPlayerController.disconnect();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        libraryExecutor.shutdownNow();
        super.onDestroy();
    }
}
