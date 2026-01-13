package app.aegis.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.aegis.domain.model.TrustedContact

@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val addedAt: Long
)

// Mapper extension
fun TrustedContactEntity.toDomain() = TrustedContact(
    id = id,
    name = name,
    phoneNumber = phoneNumber,
    relationship = relationship,
    addedAt = addedAt
)

fun TrustedContact.toEntity() = TrustedContactEntity(
    id = id,
    name = name,
    phoneNumber = phoneNumber,
    relationship = relationship,
    addedAt = addedAt
)
