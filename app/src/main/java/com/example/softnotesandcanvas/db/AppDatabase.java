package com.example.softnotesandcanvas.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

/**
 * The main Room database class for the application.
 * This class is abstract and Room will generate the implementation.
 * It follows a singleton pattern to ensure only one instance of the
 * database exists at any time.
 */
@Database(entities = {Note.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    // This abstract method will be implemented by Room
    public abstract NoteDao noteDao();

    // Volatile ensures that the instance is immediately visible to all threads
    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "notes_database";

    /**
     * Gets the singleton instance of the AppDatabase.
     *
     * @param context The application context.
     * @return The singleton AppDatabase instance.
     */
    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            // Use a synchronized block to prevent race conditions
            // from multiple threads trying to create the instance
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            // NOTE: In a production app, you would need to implement
                            // a proper database migration strategy.
                            // For development, this just wipes and rebuilds the
                            // database on a version schema change.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
