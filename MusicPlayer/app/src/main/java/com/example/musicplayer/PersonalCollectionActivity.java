package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PersonalCollectionActivity extends BaseActivity {
    private final List<Song> songs = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String type;
    private long playlistId;
    private SongAdapter adapter;
    private PersonalLibraryRepository repository;
    private MiniPlayerController miniPlayerController;
    private TextView empty;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_personal_collection);

        type = getIntent().getStringExtra("collectionType");
        playlistId = getIntent().getLongExtra("playlistId", -1);
        repository = new PersonalLibraryRepository(this);
        miniPlayerController = new MiniPlayerController(this);
        ((TextView) findViewById(R.id.collectionTitle)).setText(
                getIntent().getStringExtra("collectionTitle"));
        empty = findViewById(R.id.collectionEmpty);

        RecyclerView recycler = findViewById(R.id.collectionSongs);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(songs, this::playSong, song -> SongActions.show(this, song));
        recycler.setAdapter(adapter);
    }

    private void loadSongs() {
        executor.execute(() -> {
            List<Song> loaded = repository.getCollection(type, playlistId);
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                songs.clear();
                songs.addAll(loaded);
                adapter.notifyDataSetChanged();
                empty.setVisibility(loaded.isEmpty()
                        ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }

    private void playSong(Song song) {
        Intent intent = new Intent(this, NowPlayingActivity.class);
        intent.putExtra("playSongUri", song.getContentUri().toString());
        intent.putExtra("personalCollection", type);
        intent.putExtra("playlistId", playlistId);
        startActivity(intent);
    }

    @Override protected void onResume() {
        super.onResume();
        loadSongs();
    }

    @Override protected void onStart() {
        super.onStart();
        miniPlayerController.connect();
    }

    @Override protected void onStop() {
        miniPlayerController.disconnect();
        super.onStop();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
