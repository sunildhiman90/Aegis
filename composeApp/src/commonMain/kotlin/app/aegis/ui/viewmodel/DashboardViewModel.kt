package app.aegis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aegis.domain.model.Incident
import app.aegis.domain.repository.IncidentRepository
import app.aegis.domain.repository.TrustedContactRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for Dashboard screen
 */
class DashboardViewModel(
    incidentRepository: IncidentRepository,
    trustedContactRepository: TrustedContactRepository
) : ViewModel() {

    /**
     * Latest 3 incidents for dashboard preview
     */
    val latestIncidents: StateFlow<List<Incident>> = incidentRepository.getAllIncidents()
        .map { incidents -> incidents.take(3) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Count of trusted contacts
     */
    val trustedContactsCount: StateFlow<Int> = trustedContactRepository.getAllContacts()
        .map { contacts -> contacts.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
}
