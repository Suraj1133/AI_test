package com.example.musicplayer;

import android.net.Uri;

public final class Song {
    private final long mediaId;
    private final String title;
    private final String artist;
    private final Uri contentUri;
    private final long duration;
    private final long albumId;
    private final int trackNumber;

    public Song(long mediaId, String title, String artist, Uri contentUri,
                long duration, long albumId, int trackNumber) {
        this.mediaId = mediaId;
        this.title = title;
        this.artist = artist;
        this.contentUri = contentUri;
        this.duration = duration;
        this.albumId = albumId;
        this.trackNumber = trackNumber;
    }

    public long getMediaId() { return mediaId; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public Uri getContentUri() { return contentUri; }
    public long getDuration() { return duration; }
    public long getAlbumId() { return albumId; }
    public int getTrackNumber() { return trackNumber; }
}
