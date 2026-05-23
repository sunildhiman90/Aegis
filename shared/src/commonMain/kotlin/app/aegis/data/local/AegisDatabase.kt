package app.aegis.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import app.aegis.data.local.dao.IncidentDao
import app.aegis.data.local.dao.TrustedContactDao
import app.aegis.data.local.entities.IncidentEntity
import app.aegis.data.local.entities.TrustedContactEntity

@Database(
    entities = [TrustedContactEntity::class, IncidentEntity::class],
    version = 1,
)
@ConstructedBy(AegisDatabaseConstructor::class)
abstract class AegisDatabase : RoomDatabase() {
    abstract fun trustedContactDao(): TrustedContactDao

    abstract fun incidentDao(): IncidentDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AegisDatabaseConstructor : RoomDatabaseConstructor<AegisDatabase> {
    override fun initialize(): AegisDatabase
}
