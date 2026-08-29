package com.example.musicplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
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
        if (albumIds == null || albumIds.length == 0) return new ArrayList<>();
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

    public List<Song> getSongsForFolder(String folderPath) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || folderPath == null) {
            return getAllSongs();
        }
        return querySongs(
                MediaStore.Audio.Media.IS_MUSIC + " != 0 AND "
                        + MediaStore.Audio.Media.RELATIVE_PATH + "=?",
                new String[]{folderPath},
                MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"
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
                MUSIC_URI, projection, MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null, MediaStore.Audio.Media.ALBUM + " COLLATE NOCASE ASC"
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
                album.addTrack(cursor.getLong(albumIdColumn),
                        ContentUris.withAppendedId(MUSIC_URI, mediaId));
            }
        }
        return new ArrayList<>(grouped.values());
    }

    public List<MusicFolder> getFolders() {
        Map<String, MusicFolder> grouped = new LinkedHashMap<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            MusicFolder folder = new MusicFolder("Music Library", null);
            for (Song song : getAllSongs()) folder.addTrack(song.getContentUri());
            if (folder.trackCount > 0) grouped.put(folder.name, folder);
            return new ArrayList<>(grouped.values());
        }

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.RELATIVE_PATH
        };
        try (Cursor cursor = resolver.query(
                MUSIC_URI, projection, MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null, MediaStore.Audio.Media.RELATIVE_PATH + " COLLATE NOCASE ASC"
        )) {
            if (cursor == null) return new ArrayList<>();
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH);
            while (cursor.moveToNext()) {
                String rawPath = cursor.getString(pathColumn);
                String path = rawPath == null || rawPath.isEmpty() ? "Music/" : rawPath;
                MusicFolder folder = grouped.get(path);
                if (folder == null) {
                    folder = new MusicFolder(folderName(path), path);
                    grouped.put(path, folder);
                }
                folder.addTrack(ContentUris.withAppendedId(
                        MUSIC_URI, cursor.getLong(idColumn)));
            }
        }
        return new ArrayList<>(grouped.values());
    }

    private List<Song> querySongs(String selection, String[] args, String sortOrder) {
        List<Song> songs = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        columns.add(MediaStore.Audio.Media._ID);
        columns.add(MediaStore.Audio.Media.TITLE);
        columns.add(MediaStore.Audio.Media.ARTIST);
        columns.add(MediaStore.Audio.Media.ALBUM);
        columns.add(MediaStore.Audio.Media.DURATION);
        columns.add(MediaStore.Audio.Media.ALBUM_ID);
        columns.add(MediaStore.Audio.Media.TRACK);
        columns.add(MediaStore.Audio.Media.DATE_ADDED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            columns.add(MediaStore.Audio.Media.RELATIVE_PATH);
        }

        try (Cursor cursor = resolver.query(
                MUSIC_URI, columns.toArray(new String[0]), selection, args, sortOrder
        )) {
            if (cursor == null) return songs;
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
            int trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK);
            int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);
            int folderColumn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ? cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH) : -1;

            while (cursor.moveToNext()) {
                long mediaId = cursor.getLong(idColumn);
                songs.add(new Song(
                        mediaId,
                        clean(cursor.getString(titleColumn), "Unknown soundtrack"),
                        clean(cursor.getString(artistColumn), "Unknown artist"),
                        clean(cursor.getString(albumColumn), "Unknown album"),
                        ContentUris.withAppendedId(MUSIC_URI, mediaId),
                        cursor.getLong(durationColumn),
                        cursor.getLong(albumIdColumn),
                        cursor.getInt(trackColumn),
                        cursor.getLong(dateColumn),
                        folderColumn >= 0 ? cursor.getString(folderColumn) : null
                ));
            }
        }
        return songs;
    }

    private String clean(String value, String fallback) {
        if (value == null || value.trim().isEmpty()
                || "<unknown>".equalsIgnoreCase(value.trim())) return fallback;
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalize(String value) {
        return clean(value, "").toLowerCase(Locale.ROOT);
    }

    private String folderName(String path) {
        String normalized = path.endsWith("/")
                ? path.substring(0, path.length() - 1) : path;
        int slash = normalized.lastIndexOf('/');
        String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return name.isEmpty() ? "Music" : name;
    }
}
