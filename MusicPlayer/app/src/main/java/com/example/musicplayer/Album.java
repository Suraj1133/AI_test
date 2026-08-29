package com.example.musicplayer;

import java.util.ArrayList;
import java.util.List;

public class Album {
    String name;
    String artist;
    final List<Long> albumIds = new ArrayList<>();
    final List<String> songPaths = new ArrayList<>();

    public Album(String name, String artist) {
        this.name = name;
        this.artist = artist;
    }

    void addTrack(long albumId, String songPath) {
        if (!albumIds.contains(albumId)) {
            albumIds.add(albumId);
        }
        if (songPath != null && !songPath.isEmpty()) {
            songPaths.add(songPath);
        }
    }

    long[] getAlbumIds() {
        long[] ids = new long[albumIds.size()];
        for (int i = 0; i < albumIds.size(); i++) {
            ids[i] = albumIds.get(i);
        }
        return ids;
    }
}
