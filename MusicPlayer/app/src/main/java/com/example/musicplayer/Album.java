package com.example.musicplayer;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public final class Album {
    final String name;
    final String artist;
    final List<Long> albumIds = new ArrayList<>();
    final List<Uri> songUris = new ArrayList<>();

    public Album(String name, String artist) {
        this.name = name;
        this.artist = artist;
    }

    void addTrack(long albumId, Uri songUri) {
        if (!albumIds.contains(albumId)) {
            albumIds.add(albumId);
        }
        if (songUri != null) {
            songUris.add(songUri);
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
