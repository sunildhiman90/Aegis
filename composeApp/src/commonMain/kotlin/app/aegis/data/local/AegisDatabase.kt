package app.aegis.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import app.aegis.data.local.dao.IncidentDao
import app.aegis.data.local.dao.TrustedContactDao
import app.aegis.data.local.entities.IncidentEntity
import app.aegis.data.local.entities.TrustedContactEntity

@Database(
    entities = [TrustedContactEntity::class, IncidentEntity::class],
    version = 1
)
abstract class AegisDatabase : RoomDatabase() {
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun incidentDao(): IncidentDao
}
