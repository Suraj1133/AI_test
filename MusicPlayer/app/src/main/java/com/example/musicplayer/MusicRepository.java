package com.example.musicplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MusicRepository {
    private final ContentResolver resolver;
    private static final Uri MUSIC_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

    public MusicRepository(ContentResolver resolver) {
        this.resolver = resolver;
    }

    public List<Song> getAllSongs() {
        return querySongs(
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"
        );
    }

    public List<Song> getSongsForAlbums(long[] albumIds) {
        if (albumIds == null || albumIds.length == 0) {
            return new ArrayList<>();
        }

        StringBuilder placeholders = new StringBuilder();
        String[] args = new String[albumIds.length];
        for (int i = 0; i < albumIds.length; i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
            args[i] = String.valueOf(albumIds[i]);
        }

        return querySongs(
                MediaStore.Audio.Media.IS_MUSIC + " != 0 AND "
                        + MediaStore.Audio.Media.ALBUM_ID + " IN (" + placeholders + ")",
                args,
                MediaStore.Audio.Media.TRACK + " ASC, "
                        + MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"
        );
    }

    public List<Album> getAlbums() {
        Map<String, Album> grouped = new LinkedHashMap<>();
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID
        };

        try (Cursor cursor = resolver.query(
                MUSIC_URI,
                projection,
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                MediaStore.Audio.Media.ALBUM + " COLLATE NOCASE ASC"
        )) {
            if (cursor == null) return new ArrayList<>();

            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            while (cursor.moveToNext()) {
                long mediaId = cursor.getLong(idColumn);
                String albumName = clean(cursor.getString(albumColumn), "Unknown album");
                String artist = clean(cursor.getString(artistColumn), "Unknown artist");
                String key = normalize(albumName) + "\u0000" + normalize(artist);

                Album album = grouped.get(key);
                if (album == null) {
                    album = new Album(albumName, artist);
                    grouped.put(key, album);
                }
                album.addTrack(
                        cursor.getLong(albumIdColumn),
                        ContentUris.withAppendedId(MUSIC_URI, mediaId)
                );
            }
        }

        return new ArrayList<>(grouped.values());
    }

    private List<Song> querySongs(String selection, String[] args, String sortOrder) {
        List<Song> songs = new ArrayList<>();
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.TRACK
        };

        try (Cursor cursor = resolver.query(
                MUSIC_URI, projection, selection, args, sortOrder
        )) {
            if (cursor == null) return songs;

            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
            int trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK);

            while (cursor.moveToNext()) {
                long mediaId = cursor.getLong(idColumn);
                songs.add(new Song(
                        mediaId,
                        clean(cursor.getString(titleColumn), "Unknown soundtrack"),
                        clean(cursor.getString(artistColumn), "Unknown artist"),
                        ContentUris.withAppendedId(MUSIC_URI, mediaId),
                        cursor.getLong(durationColumn),
                        cursor.getLong(albumIdColumn),
                        cursor.getInt(trackColumn)
                ));
            }
        }
        return songs;
    }

    private String clean(String value, String fallback) {
        if (value == null || value.trim().isEmpty()
                || "<unknown>".equalsIgnoreCase(value.trim())) {
            return fallback;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalize(String value) {
        return clean(value, "").toLowerCase(Locale.ROOT);
    }
}
