package app.aegis.data.repository

import app.aegis.data.local.dao.IncidentDao
import app.aegis.data.local.entities.toDomain
import app.aegis.data.local.entities.toEntity
import app.aegis.domain.model.Incident
import app.aegis.domain.repository.IncidentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IncidentRepositoryImpl(
    private val dao: IncidentDao
) : IncidentRepository {

    override fun getAllIncidents(): Flow<List<Incident>> {
        return dao.getAllIncidents().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addIncident(incident: Incident) {
        dao.insertIncident(incident.toEntity())
    }

    override suspend fun deleteIncident(incident: Incident) {
        dao.deleteIncident(incident.toEntity())
    }

    override suspend fun getIncidentById(id: String): Incident? {
        return dao.getIncidentById(id)?.toDomain()
    }
}
