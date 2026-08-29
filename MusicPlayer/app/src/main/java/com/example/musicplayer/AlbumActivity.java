package com.example.musicplayer;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AlbumActivity extends AppCompatActivity {
    private final List<Song> albumSongs = new ArrayList<>();
    private long[] albumIds;
    private MiniPlayerController miniPlayerController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        miniPlayerController = new MiniPlayerController(this);

        albumIds = getIntent().getLongArrayExtra("albumIds");
        RecyclerView recyclerView = findViewById(R.id.recyclerAlbumSongs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadAlbumSongs();

        recyclerView.setAdapter(new SongAdapter(albumSongs, song -> {
            Intent intent = new Intent(this, NowPlayingActivity.class);
            intent.putExtra("playSongPath", song.getPath());
            intent.putExtra("albumIds", albumIds);
            startActivity(intent);
        }));
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

    private void loadAlbumSongs() {
        if (albumIds == null || albumIds.length == 0) return;

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
        };

        StringBuilder placeholders = new StringBuilder();
        String[] selectionArgs = new String[albumIds.length];
        for (int i = 0; i < albumIds.length; i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
            selectionArgs[i] = String.valueOf(albumIds[i]);
        }

        String selection = MediaStore.Audio.Media.ALBUM_ID + " IN (" + placeholders + ")"
                + " AND " + MediaStore.Audio.Media.IS_MUSIC + " != 0";

        try (Cursor cursor = getContentResolver().query(
                uri, projection, selection, selectionArgs,
                MediaStore.Audio.Media.TRACK + " ASC, "
                        + MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"
        )) {
            if (cursor == null) return;
            int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            while (cursor.moveToNext()) {
                albumSongs.add(new Song(
                        cursor.getString(titleColumn),
                        cursor.getString(artistColumn),
                        cursor.getString(pathColumn),
                        cursor.getLong(durationColumn),
                        cursor.getLong(albumIdColumn)
                ));
            }
        }
    }
}
