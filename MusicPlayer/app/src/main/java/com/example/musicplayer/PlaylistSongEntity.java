package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "playlist_songs", primaryKeys = {"playlistId", "songUri"})
public class PlaylistSongEntity {
    public long playlistId;
    @NonNull public String songUri;
    public long addedAt;

    public PlaylistSongEntity(long playlistId, @NonNull String songUri, long addedAt) {
        this.playlistId = playlistId;
        this.songUri = songUri;
        this.addedAt = addedAt;
    }
}
