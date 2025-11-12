package com.example.softnotesandcanvas.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration; // <-- Import this
import androidx.sqlite.db.SupportSQLiteDatabase; // <-- Import this

/**
 * The main Room database class for the application.
 * This class is abstract and Room will generate the implementation.
 * It follows a singleton pattern to ensure only one instance of the
 * database exists at any time.
 */
@Database(entities = {Note.class}, version = 2, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    // This abstract method will be implemented by Room
    public abstract NoteDao noteDao();

    // Volatile ensures that the instance is immediately visible to all threads
    private static volatile AppDatabase INSTANCE;

    // 2. Define the migration
    // This tells Room how to add the new columns without deleting data
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add the 'type' column, default all existing notes to "TEXT"
            database.execSQL("ALTER TABLE notes ADD COLUMN type TEXT NOT NULL DEFAULT '" + Note.TYPE_TEXT + "'");
            // Add the 'canvasImagePath' column
            database.execSQL("ALTER TABLE notes ADD COLUMN canvasImagePath TEXT DEFAULT NULL");
        }
    };
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
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
