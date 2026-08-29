package com.example.musicplayer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.database.Cursor;
import android.provider.MediaStore;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {

    MediaPlayer mediaPlayer;
//    List<Song> songList = new ArrayList<>();
    TextView currentSongTitle;
    Button btnPlayPause, btnNext, btnPrev;
    SeekBar songSeekBar;
    boolean isTracking = false;

    SongAdapter songAdapter;

    List<Album> albums = new ArrayList<>();

    int currentSongIndex = -1;

    TabLayout tabLayout;
    List<Song> allSongs = new ArrayList<>();
    List<Song> playList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        loadSongs();
        loadAlbums();

        currentSongTitle = findViewById(R.id.currentSongTitle);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        songSeekBar = findViewById(R.id.songSeekBar);

        tabLayout = findViewById(R.id.tabLayout);

        tabLayout.addTab(tabLayout.newTab().setText("Songs"));
        tabLayout.addTab(tabLayout.newTab().setText("Albums"));
        RecyclerView recyclerView = findViewById(R.id.recyclerSongs);
        RecyclerView recyclerAlbums = findViewById(R.id.recyclerAlbums);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerAlbums.setLayoutManager(new LinearLayoutManager(this));

        AlbumAdapter albumAdapter = new AlbumAdapter(this, albums, album -> {

            Intent intent = new Intent(MainActivity.this, AlbumActivity.class);

            intent.putExtra("albumId", album.albumId);
            intent.putExtra("albumName", album.name);

            startActivity(intent);

        });
        recyclerAlbums.setAdapter(albumAdapter);
        recyclerAlbums.setVisibility(View.GONE);

        Intent intent = getIntent();

        if (intent != null && intent.getBooleanExtra("playFromAlbum", false)) {

            long albumId = intent.getLongExtra("albumId", -1);
//            long albumId = Long.parseLong(albumIdStr);
            Log.d("PLAYER", "AlbumId received: " + albumId);
            String songPath = intent.getStringExtra("playSongPath");
            Log.d("albumId", "songPath: " + albumId + "============="+ songPath);
            playList.clear();

            for (Song s : allSongs) {
                Log.d("PLAYER", "Song albumId: " + s.albumId);
                if (s.albumId == albumId) {
                    playList.add(s);
                }
            }

            Log.d("PLAYER", "Playlist size: " + playList.size());

            for (int i = 0; i < playList.size(); i++) {
                if (playList.get(i).getPath().equals(songPath)) {
                    currentSongIndex = i;
//                    playSong(playList.get(i));
                    break;
                }
            }
        }

        SongAdapter adapter = new SongAdapter(playList, song -> {

            currentSongIndex = playList.indexOf(song);
            playSong(song);

        });

        recyclerView.setAdapter(adapter);
        // PLAY / PAUSE
        btnPlayPause.setOnClickListener(v -> {

            if(mediaPlayer != null){

                if(mediaPlayer.isPlaying()){
                    mediaPlayer.pause();
                }else{
                    mediaPlayer.start();
                }

            }

        });


        // NEXT SONG
        btnNext.setOnClickListener(v -> {

            if(currentSongIndex < playList.size() - 1){
                currentSongIndex++;
                playSong(playList.get(currentSongIndex));
            }

        });


        // PREVIOUS SONG
        btnPrev.setOnClickListener(v -> {

            if(currentSongIndex > 0){
                currentSongIndex--;
                playSong(playList.get(currentSongIndex));
            }

        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                if(tab.getPosition() == 0){
                    recyclerView.setVisibility(View.VISIBLE);
                    recyclerAlbums.setVisibility(View.GONE);
                }else{
                    recyclerView.setVisibility(View.GONE);
                    recyclerAlbums.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });



        if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{android.Manifest.permission.READ_MEDIA_AUDIO},
                    1
            );
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if(currentSongIndex >= 0 && currentSongIndex < playList.size()){
            playSong(playList.get(currentSongIndex));
        }
    }

    private void loadSongs() {

        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Audio.Media.IS_MUSIC + "!= 0",
                null,
                null
        );

        if (cursor != null) {

            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int pathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);

            while (cursor.moveToNext()) {

                String title = cursor.getString(titleColumn);
                String artist = cursor.getString(artistColumn);
                String path = cursor.getString(pathColumn);
                long duration = cursor.getLong(durationColumn);
                long albumId = cursor.getLong(albumIdColumn);

                Song song = new Song(title, artist, path, duration,albumId);
                allSongs.add(song);
                playList = new ArrayList<>(allSongs);
                Log.d("SONG", title + " - " + artist);
            }

            cursor.close();
        }
    }

    void playSong(Song song){

        try {

            isTracking = false;  // stop previous thread
            if(mediaPlayer != null && mediaPlayer.isPlaying()){
                mediaPlayer.stop();
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(song.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            currentSongTitle.setText(song.getTitle());

            songSeekBar.setMax(mediaPlayer.getDuration());

            isTracking = true;

            new Thread(() -> {

                while(isTracking && mediaPlayer != null){

                    try{
                        Thread.sleep(1000);
                    }catch(Exception e){}

                    try {

                        if(mediaPlayer.isPlaying()){

                            runOnUiThread(() ->
                                    songSeekBar.setProgress(mediaPlayer.getCurrentPosition())
                            );

                        }

                    } catch (IllegalStateException e){
                        break;
                    }

                }

            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //Album Art Function
    Bitmap getAlbumArt(String path){

        try{

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(path);

            byte[] art = retriever.getEmbeddedPicture();

            if(art != null){
                return BitmapFactory.decodeByteArray(art, 0, art.length);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

    void loadAlbums(){

        android.net.Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;

        Cursor cursor = getContentResolver().query(
                uri,
                null,
                null,
                null,
                MediaStore.Audio.Albums.ALBUM + " ASC"
        );

        if(cursor != null){

            while(cursor.moveToNext()){

                String albumName = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));

                String artist = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST));

                String albumId = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID));

                String albumArt = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART));

                albums.add(new Album(albumName, artist, albumId, albumArt));
                Log.d("ALBUM", "Album: " + albumName);
            }

            cursor.close();
        }
    }
}