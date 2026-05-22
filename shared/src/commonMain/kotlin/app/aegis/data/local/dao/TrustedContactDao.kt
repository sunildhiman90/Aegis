package app.aegis.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.aegis.data.local.entities.TrustedContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedContactDao {
    @Query("SELECT * FROM trusted_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<TrustedContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: TrustedContactEntity) // Removing return Long for KMP compatibility/simplicity

    @Delete
    suspend fun deleteContact(contact: TrustedContactEntity)

    @Query("SELECT * FROM trusted_contacts WHERE id = :id")
    suspend fun getContactById(id: String): TrustedContactEntity?

    @Query("SELECT COUNT(*) FROM trusted_contacts WHERE name = :identifier OR phoneNumber = :identifier")
    suspend fun checkIsTrusted(identifier: String): Int
}
