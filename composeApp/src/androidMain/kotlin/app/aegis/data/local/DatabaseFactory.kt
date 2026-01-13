package app.aegis.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AegisDatabase> {
    val dbFile = context.getDatabasePath("aegis.db")
    return Room.databaseBuilder<AegisDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath
    )
}
