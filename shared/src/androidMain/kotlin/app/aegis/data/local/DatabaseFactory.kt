package app.aegis.data.local

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AegisDatabase> {
    val dbFile = context.getDatabasePath("aegis.db")
    return Room.databaseBuilder<AegisDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath
    )
}
