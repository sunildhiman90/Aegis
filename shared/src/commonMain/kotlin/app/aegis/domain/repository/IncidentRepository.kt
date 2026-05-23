package app.aegis.domain.repository

import app.aegis.domain.model.Incident
import kotlinx.coroutines.flow.Flow

interface IncidentRepository {
    fun getAllIncidents(): Flow<List<Incident>>
    suspend fun addIncident(incident: Incident)
    suspend fun deleteIncident(incident: Incident)
    suspend fun getIncidentById(id: String): Incident?
}
