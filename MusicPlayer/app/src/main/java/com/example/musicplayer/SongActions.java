package com.example.musicplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SongActions {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private SongActions() {}

    public static void show(Activity activity, Song song) {
        PersonalLibraryRepository repository = new PersonalLibraryRepository(activity);
        EXECUTOR.execute(() -> {
            boolean favorite = repository.isFavorite(song.getContentUri().toString());
            activity.runOnUiThread(() -> {
                if (activity.isDestroyed()) return;
                String favoriteLabel = favorite
                        ? "Remove from favorites" : "Add to favorites";
                new AlertDialog.Builder(activity)
                        .setTitle(song.getTitle())
                        .setItems(new String[]{favoriteLabel, "Add to playlist"},
                                (dialog, which) -> {
                                    if (which == 0) {
                                        toggleFavorite(activity, repository, song, !favorite);
                                    } else {
                                        choosePlaylist(activity, repository, song);
                                    }
                                })
                        .show();
            });
        });
    }

    private static void toggleFavorite(
            Activity activity, PersonalLibraryRepository repository,
            Song song, boolean favorite) {
        EXECUTOR.execute(() -> {
            repository.setFavorite(song, favorite);
            activity.runOnUiThread(() -> Toast.makeText(
                    activity,
                    favorite ? "Added to favorites" : "Removed from favorites",
                    Toast.LENGTH_SHORT).show());
        });
    }

    private static void choosePlaylist(
            Activity activity, PersonalLibraryRepository repository, Song song) {
        EXECUTOR.execute(() -> {
            List<PlaylistEntity> playlists = repository.getPlaylists();
            activity.runOnUiThread(() -> {
                String[] choices = new String[playlists.size() + 1];
                choices[0] = "＋ New playlist";
                for (int i = 0; i < playlists.size(); i++) {
                    choices[i + 1] = playlists.get(i).name;
                }
                new AlertDialog.Builder(activity)
                        .setTitle("Add to playlist")
                        .setItems(choices, (dialog, which) -> {
                            if (which == 0) createAndAdd(activity, repository, song);
                            else add(activity, repository, playlists.get(which - 1).id, song);
                        })
                        .show();
            });
        });
    }

    private static void createAndAdd(
            Activity activity, PersonalLibraryRepository repository, Song song) {
        EditText input = new EditText(activity);
        input.setHint("Playlist name");
        new AlertDialog.Builder(activity)
                .setTitle("New playlist")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    EXECUTOR.execute(() -> {
                        long id = repository.createPlaylist(name);
                        repository.addToPlaylist(id, song);
                        activity.runOnUiThread(() -> Toast.makeText(
                                activity, "Added to " + name, Toast.LENGTH_SHORT).show());
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void add(
            Activity activity, PersonalLibraryRepository repository,
            long playlistId, Song song) {
        EXECUTOR.execute(() -> {
            repository.addToPlaylist(playlistId, song);
            activity.runOnUiThread(() -> Toast.makeText(
                    activity, "Added to playlist", Toast.LENGTH_SHORT).show());
        });
    }
}
