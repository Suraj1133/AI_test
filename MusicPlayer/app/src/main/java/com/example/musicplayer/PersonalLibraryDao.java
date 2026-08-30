package com.example.musicplayer;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PersonalLibraryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertSongIfMissing(LibrarySongEntity song);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveSong(LibrarySongEntity song);

    @Query("SELECT * FROM library_songs WHERE uri=:uri LIMIT 1")
    LibrarySongEntity getSong(String uri);

    @Query("UPDATE library_songs SET favorite=:favorite WHERE uri=:uri")
    void setFavorite(String uri, boolean favorite);

    @Query("UPDATE library_songs SET playCount=playCount+1, lastPlayed=:time WHERE uri=:uri")
    void recordPlay(String uri, long time);

    @Query("SELECT * FROM library_songs WHERE favorite=1 ORDER BY title COLLATE NOCASE")
    List<LibrarySongEntity> getFavorites();

    @Query("SELECT * FROM library_songs WHERE lastPlayed>0 ORDER BY lastPlayed DESC LIMIT 100")
    List<LibrarySongEntity> getRecentlyPlayed();

    @Query("SELECT * FROM library_songs WHERE playCount>0 ORDER BY playCount DESC, lastPlayed DESC LIMIT 100")
    List<LibrarySongEntity> getMostPlayed();

    @Insert
    long createPlaylist(PlaylistEntity playlist);

    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE")
    List<PlaylistEntity> getPlaylists();

    @Query("DELETE FROM playlists WHERE id=:playlistId")
    void deletePlaylist(long playlistId);

    @Query("DELETE FROM playlist_songs WHERE playlistId=:playlistId")
    void deletePlaylistSongs(long playlistId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addToPlaylist(PlaylistSongEntity item);

    @Query("DELETE FROM playlist_songs WHERE playlistId=:playlistId AND songUri=:uri")
    void removeFromPlaylist(long playlistId, String uri);

    @Query("SELECT s.* FROM library_songs s INNER JOIN playlist_songs p ON s.uri=p.songUri WHERE p.playlistId=:playlistId ORDER BY p.addedAt")
    List<LibrarySongEntity> getPlaylistSongs(long playlistId);
}
