package com.example.musicplayer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaylistsActivity extends androidx.appcompat.app.AppCompatActivity {
    private final List<PlaylistEntity> playlists = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ArrayAdapter<String> adapter;
    private PersonalLibraryRepository repository;
    private MiniPlayerController miniPlayerController;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_playlists);
        repository = new PersonalLibraryRepository(this);
        miniPlayerController = new MiniPlayerController(this);

        ListView list = findViewById(R.id.playlistList);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            PlaylistEntity playlist = playlists.get(position);
            Intent intent = new Intent(this, PersonalCollectionActivity.class);
            intent.putExtra("collectionType", PersonalLibraryRepository.PLAYLIST);
            intent.putExtra("playlistId", playlist.id);
            intent.putExtra("collectionTitle", playlist.name);
            startActivity(intent);
        });
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            PlaylistEntity playlist = playlists.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Delete " + playlist.name + "?")
                    .setMessage("The songs will remain on your device.")
                    .setPositiveButton("Delete", (dialog, which) ->
                            executor.execute(() -> {
                                repository.deletePlaylist(playlist.id);
                                runOnUiThread(this::loadPlaylists);
                            }))
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
        ((Button) findViewById(R.id.createPlaylist)).setOnClickListener(v -> createPlaylist());
    }

    private void createPlaylist() {
        EditText input = new EditText(this);
        input.setHint("Playlist name");
        new AlertDialog.Builder(this)
                .setTitle("New playlist")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    executor.execute(() -> {
                        repository.createPlaylist(name);
                        runOnUiThread(this::loadPlaylists);
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadPlaylists() {
        executor.execute(() -> {
            List<PlaylistEntity> loaded = repository.getPlaylists();
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                playlists.clear();
                playlists.addAll(loaded);
                names.clear();
                for (PlaylistEntity item : loaded) names.add(item.name);
                adapter.notifyDataSetChanged();
            });
        });
    }

    @Override protected void onResume() {
        super.onResume();
        loadPlaylists();
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
