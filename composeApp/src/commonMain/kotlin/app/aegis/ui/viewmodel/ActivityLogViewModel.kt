package app.aegis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aegis.domain.model.Incident
import app.aegis.domain.repository.IncidentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Activity Log screen
 */
class ActivityLogViewModel(
    private val repository: IncidentRepository
) : ViewModel() {

    val incidents: StateFlow<List<Incident>> = repository.getAllIncidents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun refreshIncidents() {
        viewModelScope.launch {
            // Trigger refresh if needed
            // For now, Flow auto-updates
        }
    }

    fun deleteIncident(incidentId: String) {
        viewModelScope.launch {
            val incident = repository.getIncidentById(incidentId)
            incident?.let {
                repository.deleteIncident(it)
            }
        }
    }
}
