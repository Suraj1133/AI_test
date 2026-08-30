package com.example.musicplayer;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {LibrarySongEntity.class, PlaylistEntity.class, PlaylistSongEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class PersonalLibraryDatabase extends RoomDatabase {
    private static volatile PersonalLibraryDatabase instance;

    public abstract PersonalLibraryDao dao();

    public static PersonalLibraryDatabase get(Context context) {
        if (instance == null) {
            synchronized (PersonalLibraryDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            PersonalLibraryDatabase.class,
                            "personal_library.db"
                    ).build();
                }
            }
        }
        return instance;
    }
}
