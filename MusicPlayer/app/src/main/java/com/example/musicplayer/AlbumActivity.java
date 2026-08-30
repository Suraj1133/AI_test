package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumActivity extends BaseActivity {
    private final List<Song> albumSongs = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private long[] albumIds;
    private SongAdapter adapter;
    private MusicRepository repository;
    private MiniPlayerController miniPlayerController;
    private ImageView artwork;
    private TextView trackCount;
    private Button playButton;
    private Button shuffleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        repository = new MusicRepository(getContentResolver());
        miniPlayerController = new MiniPlayerController(this);
        albumIds = getIntent().getLongArrayExtra("albumIds");

        artwork = findViewById(R.id.albumDetailArtwork);
        trackCount = findViewById(R.id.albumDetailCount);
        playButton = findViewById(R.id.albumDetailPlay);
        shuffleButton = findViewById(R.id.albumDetailShuffle);
        ((TextView) findViewById(R.id.albumDetailName)).setText(
                getIntent().getStringExtra("albumName"));
        ((TextView) findViewById(R.id.albumDetailArtist)).setText(
                getIntent().getStringExtra("albumArtist"));

        RecyclerView recyclerView = findViewById(R.id.recyclerAlbumSongs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(
                albumSongs,
                song -> playSong(song, false),
                song -> SongActions.show(this, song)
        );
        recyclerView.setAdapter(adapter);

        playButton.setEnabled(false);
        shuffleButton.setEnabled(false);
        playButton.setOnClickListener(v -> playFirst(false));
        shuffleButton.setOnClickListener(v -> playFirst(true));
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
                    trackCount.setText(String.format(
                            Locale.getDefault(), "%d tracks", songs.size()));
                    boolean hasSongs = !songs.isEmpty();
                    playButton.setEnabled(hasSongs);
                    shuffleButton.setEnabled(hasSongs);
                    if (hasSongs) {
                        ArtworkLoader.loadInto(
                                this, artwork, songs.get(0).getContentUri());
                    }
                });
            } catch (SecurityException exception) {
                runOnUiThread(() -> Toast.makeText(
                        this, "Unable to access this album",
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private void playFirst(boolean shuffled) {
        if (!albumSongs.isEmpty()) playSong(albumSongs.get(0), shuffled);
    }

    private void playSong(Song song, boolean shuffled) {
        Intent intent = new Intent(this, NowPlayingActivity.class);
        intent.putExtra("playSongUri", song.getContentUri().toString());
        intent.putExtra("albumIds", albumIds);
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
