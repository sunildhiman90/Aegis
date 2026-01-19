package app.aegis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aegis.domain.model.Incident
import app.aegis.domain.model.IncidentType
import app.aegis.domain.repository.IncidentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlinx.datetime.Clock as KClock

/**
 * Filter types for activity log
 */
enum class ActivityFilter {
    ALL,
    THREATS,
    SAFE_SCANS,
}

/**
 * Grouped incidents by date
 */
data class GroupedIncidents(
    val dateLabel: String,
    val incidents: List<Incident>,
)

/**
 * ViewModel for Activity Log screen
 */
@OptIn(kotlin.time.ExperimentalTime::class)
class ActivityLogViewModel(
    private val repository: IncidentRepository,
) : ViewModel() {
    private val _selectedFilter = MutableStateFlow(ActivityFilter.ALL)
    val selectedFilter: StateFlow<ActivityFilter> = _selectedFilter.asStateFlow()

    private val allIncidents: StateFlow<List<Incident>> =
        repository
            .getAllIncidents()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    val filteredIncidents: StateFlow<List<Incident>> =
        combine(
            allIncidents,
            _selectedFilter,
        ) { incidents, filter ->
            when (filter) {
                ActivityFilter.ALL -> incidents
                ActivityFilter.THREATS -> incidents.filter { it.isBlocked || it.type in threatTypes }
                ActivityFilter.SAFE_SCANS -> incidents.filter { !it.isBlocked && it.type !in threatTypes }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val groupedIncidents: StateFlow<List<GroupedIncidents>> =
        filteredIncidents
            .combine(MutableStateFlow(Unit)) { incidents, _ ->
                groupByDate(incidents)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    fun setFilter(filter: ActivityFilter) {
        _selectedFilter.value = filter
    }

    fun deleteIncident(incidentId: String) {
        viewModelScope.launch {
            val incident = repository.getIncidentById(incidentId)
            incident?.let {
                repository.deleteIncident(it)
            }
        }
    }

    private fun groupByDate(incidents: List<Incident>): List<GroupedIncidents> {
        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(timeZone).date
        val yesterday =
            kotlin.time.Instant
                .fromEpochMilliseconds(now.toEpochMilliseconds() - 86400000)
                .toLocalDateTime(timeZone)
                .date

        val grouped =
            incidents.groupBy { incident ->
                val incidentDate =
                    Instant
                        .fromEpochMilliseconds(incident.timestamp)
                        .toLocalDateTime(timeZone)
                        .date
                when (incidentDate) {
                    today -> "TODAY"
                    yesterday -> "YESTERDAY"
                    else -> incidentDate.toString().uppercase()
                }
            }

        // Sort groups: TODAY first, then YESTERDAY, then dates in descending order
        return grouped.entries
            .sortedWith { a, b ->
                when {
                    a.key == "TODAY" -> -1
                    b.key == "TODAY" -> 1
                    a.key == "YESTERDAY" -> -1
                    b.key == "YESTERDAY" -> 1
                    else -> b.key.compareTo(a.key)
                }
            }.map { GroupedIncidents(it.key, it.value) }
    }

    companion object {
        private val threatTypes =
            setOf(
                IncidentType.SCAM_CALL,
                IncidentType.PHISHING_LINK,
                IncidentType.DANGEROUS_APP,
                IncidentType.POLICE_IMPERSONATION,
                IncidentType.SEXTORTION,
            )
    }
}
