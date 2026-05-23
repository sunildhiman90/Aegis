package app.aegis.data.local.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import app.aegis.domain.model.Incident
import app.aegis.domain.model.IncidentSeverity
import app.aegis.domain.model.IncidentType

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey val id: String,
    val type: IncidentType,
    val description: String,
    val timestamp: Long,
    val isBlocked: Boolean,
    val severity: IncidentSeverity
)

// Mapper extension
fun IncidentEntity.toDomain() = Incident(
    id = id,
    type = type,
    description = description,
    timestamp = timestamp,
    isBlocked = isBlocked,
    severity = severity
)

fun Incident.toEntity() = IncidentEntity(
    id = id,
    type = type,
    description = description,
    timestamp = timestamp,
    isBlocked = isBlocked,
    severity = severity
)
