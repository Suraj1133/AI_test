package com.example.musicplayer;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
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

    void loadAlbumSongs(){

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String selection = MediaStore.Audio.Media.ALBUM_ID + "=?";

        String[] selectionArgs = {albumId};

        Cursor cursor = getContentResolver().query(
                uri,
                null,
                selection,
                selectionArgs,
                MediaStore.Audio.Media.TITLE + " ASC"
        );

        if(cursor != null){

            while(cursor.moveToNext()){

                String title = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));

                String artist = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));

                String path = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));

                long duration = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                int albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

                albumSongs.add(new Song(title, artist, path, duration,albumIdColumn));
            }

            cursor.close();
        }
    }
}
