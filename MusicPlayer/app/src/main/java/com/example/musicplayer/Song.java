package com.example.musicplayer;

import android.net.Uri;

public final class Song {
    private final long mediaId;
    private final String title;
    private final String artist;
    private final String album;
    private final Uri contentUri;
    private final long duration;
    private final long albumId;
    private final int trackNumber;
    private final long dateAdded;
    private final String folderPath;

    public Song(long mediaId, String title, String artist, String album, Uri contentUri,
                long duration, long albumId, int trackNumber, long dateAdded,
                String folderPath) {
        this.mediaId = mediaId;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.contentUri = contentUri;
        this.duration = duration;
        this.albumId = albumId;
        this.trackNumber = trackNumber;
        this.dateAdded = dateAdded;
        this.folderPath = folderPath;
    }

    public long getMediaId() { return mediaId; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public Uri getContentUri() { return contentUri; }
    public long getDuration() { return duration; }
    public long getAlbumId() { return albumId; }
    public int getTrackNumber() { return trackNumber; }
    public long getDateAdded() { return dateAdded; }
    public String getFolderPath() { return folderPath; }
}
