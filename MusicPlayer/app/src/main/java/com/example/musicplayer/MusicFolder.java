package com.example.musicplayer;

import android.net.Uri;

public final class MusicFolder {
    final String name;
    final String path;
    int trackCount;
    Uri artworkUri;

    MusicFolder(String name, String path) {
        this.name = name;
        this.path = path;
    }

    void addTrack(Uri uri) {
        trackCount++;
        if (artworkUri == null) artworkUri = uri;
    }
}
