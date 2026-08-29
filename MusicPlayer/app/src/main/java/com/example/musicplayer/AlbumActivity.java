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

    RecyclerView recyclerView;
    List<Song> albumSongs = new ArrayList<>();
    String albumId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        recyclerView = findViewById(R.id.recyclerAlbumSongs);
        albumId = getIntent().getStringExtra("albumId");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadAlbumSongs();

        SongAdapter adapter = new SongAdapter(albumSongs, song -> {
            Intent intent = new Intent(AlbumActivity.this, MainActivity.class);
            intent.putExtra("playSongPath", song.getPath());
            intent.putExtra("playFromAlbum", true);
            intent.putExtra("albumId", song.albumId);
            startActivity(intent);
            finish();
        });

        recyclerView.setAdapter(adapter);
    }

    void loadAlbumSongs() {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
        };

        String selection = MediaStore.Audio.Media.ALBUM_ID + "=?"
                + " AND " + MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String[] selectionArgs = {albumId};

        Cursor cursor = getContentResolver().query(
                uri,
                projection,
                selection,
                selectionArgs,
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
                long actualAlbumId = cursor.getLong(albumIdColumn);

                albumSongs.add(new Song(
                        title,
                        artist,
                        path,
                        duration,
                        actualAlbumId
                ));
            }

            cursor.close();
        }
    }
}
