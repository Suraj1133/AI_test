package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FolderActivity extends androidx.appcompat.app.AppCompatActivity {
    private final List<Song> songs = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String folderPath;
    private SongAdapter adapter;
    private MusicRepository repository;
    private MiniPlayerController miniPlayerController;
    private ImageView artwork;
    private TextView count;
    private Button playButton;
    private Button shuffleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder);

        folderPath = getIntent().getStringExtra("folderPath");
        repository = new MusicRepository(getContentResolver());
        miniPlayerController = new MiniPlayerController(this);
        ((TextView) findViewById(R.id.folderDetailName)).setText(
                getIntent().getStringExtra("folderName"));
        artwork = findViewById(R.id.folderDetailArtwork);
        count = findViewById(R.id.folderDetailCount);
        playButton = findViewById(R.id.folderDetailPlay);
        shuffleButton = findViewById(R.id.folderDetailShuffle);

        RecyclerView recyclerView = findViewById(R.id.recyclerFolderSongs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(
                songs,
                song -> playSong(song, false),
                song -> SongActions.show(this, song)
        );
        recyclerView.setAdapter(adapter);

        playButton.setEnabled(false);
        shuffleButton.setEnabled(false);
        playButton.setOnClickListener(v -> playFirst(false));
        shuffleButton.setOnClickListener(v -> playFirst(true));
        loadSongs();
    }

    private void loadSongs() {
        executor.execute(() -> {
            try {
                List<Song> loaded = repository.getSongsForFolder(folderPath);
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    songs.clear();
                    songs.addAll(loaded);
                    adapter.notifyDataSetChanged();
                    count.setText(String.format(
                            Locale.getDefault(), "%d tracks", loaded.size()));
                    boolean hasSongs = !loaded.isEmpty();
                    playButton.setEnabled(hasSongs);
                    shuffleButton.setEnabled(hasSongs);
                    if (hasSongs) {
                        ArtworkLoader.loadInto(
                                this, artwork, loaded.get(0).getContentUri());
                    }
                });
            } catch (SecurityException exception) {
                runOnUiThread(() -> Toast.makeText(
                        this, "Unable to access this folder",
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private void playFirst(boolean shuffled) {
        if (!songs.isEmpty()) playSong(songs.get(0), shuffled);
    }

    private void playSong(Song song, boolean shuffled) {
        Intent intent = new Intent(this, NowPlayingActivity.class);
        intent.putExtra("playSongUri", song.getContentUri().toString());
        intent.putExtra("fromFolder", true);
        intent.putExtra("folderPath", folderPath);
        intent.putExtra("startShuffled", shuffled);
        startActivity(intent);
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
