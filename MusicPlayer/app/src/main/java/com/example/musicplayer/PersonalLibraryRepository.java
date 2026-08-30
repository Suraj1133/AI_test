package com.example.musicplayer;

import android.content.Context;
import android.net.Uri;

import androidx.media3.common.MediaItem;

import java.util.ArrayList;
import java.util.List;

public class PersonalLibraryRepository {
    public static final String FAVORITES = "favorites";
    public static final String RECENT = "recent";
    public static final String MOST_PLAYED = "most_played";
    public static final String PLAYLIST = "playlist";

    private final PersonalLibraryDao dao;

    public PersonalLibraryRepository(Context context) {
        dao = PersonalLibraryDatabase.get(context).dao();
    }

    public boolean isFavorite(String uri) {
        LibrarySongEntity song = dao.getSong(uri);
        return song != null && song.favorite;
    }

    public void setFavorite(Song song, boolean favorite) {
        LibrarySongEntity existing = dao.getSong(song.getContentUri().toString());
        LibrarySongEntity entity = fromSong(song);
        if (existing != null) {
            entity.playCount = existing.playCount;
            entity.lastPlayed = existing.lastPlayed;
        }
        entity.favorite = favorite;
        dao.saveSong(entity);
    }

    public void ensureSong(Song song) {
        dao.insertSongIfMissing(fromSong(song));
    }

    public void recordPlayed(MediaItem item) {
        if (item == null || item.mediaId.isEmpty()) return;
        String title = item.mediaMetadata.title == null
                ? "Unknown soundtrack" : item.mediaMetadata.title.toString();
        String artist = item.mediaMetadata.artist == null
                ? "Unknown artist" : item.mediaMetadata.artist.toString();
        dao.insertSongIfMissing(new LibrarySongEntity(
                item.mediaId, title, artist, "Unknown album", 0));
        dao.recordPlay(item.mediaId, System.currentTimeMillis());
    }

    public List<Song> getCollection(String type, long playlistId) {
        List<LibrarySongEntity> entities;
        if (FAVORITES.equals(type)) entities = dao.getFavorites();
        else if (RECENT.equals(type)) entities = dao.getRecentlyPlayed();
        else if (MOST_PLAYED.equals(type)) entities = dao.getMostPlayed();
        else entities = dao.getPlaylistSongs(playlistId);

        List<Song> songs = new ArrayList<>();
        for (LibrarySongEntity entity : entities) songs.add(toSong(entity));
        return songs;
    }

    public List<PlaylistEntity> getPlaylists() { return dao.getPlaylists(); }

    public long createPlaylist(String name) {
        return dao.createPlaylist(new PlaylistEntity(name, System.currentTimeMillis()));
    }

    public void deletePlaylist(long id) {
        dao.deletePlaylistSongs(id);
        dao.deletePlaylist(id);
    }

    public void addToPlaylist(long playlistId, Song song) {
        ensureSong(song);
        dao.addToPlaylist(new PlaylistSongEntity(
                playlistId, song.getContentUri().toString(), System.currentTimeMillis()));
    }

    public void removeFromPlaylist(long playlistId, String uri) {
        dao.removeFromPlaylist(playlistId, uri);
    }

    private LibrarySongEntity fromSong(Song song) {
        return new LibrarySongEntity(
                song.getContentUri().toString(), song.getTitle(), song.getArtist(),
                song.getAlbum(), song.getDuration());
    }

    private Song toSong(LibrarySongEntity entity) {
        return new Song(
                -1, entity.title, entity.artist, entity.album, Uri.parse(entity.uri),
                entity.duration, -1, 0, 0, null);
    }
}
