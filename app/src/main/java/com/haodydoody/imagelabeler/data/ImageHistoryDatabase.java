package com.haodydoody.imagelabeler.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ImageHistory.class}, version = 1)
public abstract class ImageHistoryDatabase extends RoomDatabase {
    private static final String DB_NAME = "ImageHistoryDatabase";
    public abstract ImageHistoryDao getImageHistoryDao();

    private static volatile ImageHistoryDatabase INSTANCE;
    static ImageHistoryDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ImageHistoryDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            ImageHistoryDatabase.class,
                            DB_NAME
                    ).build();
                }
            }
        }
        return INSTANCE;
    }

}
