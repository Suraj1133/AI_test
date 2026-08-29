package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumActivity extends AppCompatActivity {
    private final List<Song> albumSongs = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private long[] albumIds;
    private SongAdapter adapter;
    private MusicRepository repository;
    private MiniPlayerController miniPlayerController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        repository = new MusicRepository(getContentResolver());
        miniPlayerController = new MiniPlayerController(this);
        albumIds = getIntent().getLongArrayExtra("albumIds");

        RecyclerView recyclerView = findViewById(R.id.recyclerAlbumSongs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(albumSongs, song -> {
            Intent intent = new Intent(this, NowPlayingActivity.class);
            intent.putExtra("playSongUri", song.getContentUri().toString());
            intent.putExtra("albumIds", albumIds);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
        loadAlbumSongs();
    }

    private void loadAlbumSongs() {
        executor.execute(() -> {
            try {
                List<Song> songs = repository.getSongsForAlbums(albumIds);
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    albumSongs.clear();
                    albumSongs.addAll(songs);
                    adapter.notifyDataSetChanged();
                });
            } catch (SecurityException exception) {
                runOnUiThread(() -> Toast.makeText(
                        this, "Unable to access this album",
                        Toast.LENGTH_LONG).show());
            }
        });
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
        executor.shutdownNow();
        super.onDestroy();
    }
}
