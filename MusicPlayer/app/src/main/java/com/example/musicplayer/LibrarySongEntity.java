package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "library_songs")
public class LibrarySongEntity {
    @PrimaryKey @NonNull public String uri;
    public String title;
    public String artist;
    public String album;
    public long duration;
    public boolean favorite;
    public int playCount;
    public long lastPlayed;

    public LibrarySongEntity(@NonNull String uri, String title, String artist,
                             String album, long duration) {
        this.uri = uri;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
    }
}
