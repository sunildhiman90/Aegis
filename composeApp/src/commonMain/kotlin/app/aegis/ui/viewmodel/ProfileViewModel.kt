package app.aegis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aegis.domain.model.Incident
import app.aegis.domain.model.IncidentType
import app.aegis.domain.repository.IncidentRepository
import app.aegis.platform.DeviceIdProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Data class for privacy report stats
 */
data class PrivacyReportStats(
    val totalThreatsBlocked: Int = 0,
    val scamCallsBlocked: Int = 0,
    val phishingLinksBlocked: Int = 0,
    val dangerousAppsBlocked: Int = 0,
    val sextortionBlocked: Int = 0,
    val policeImpersonationBlocked: Int = 0,
    val dataProcessedMb: String = "0MB",
)

/**
 * ViewModel for Profile screen
 */
class ProfileViewModel(
    incidentRepository: IncidentRepository,
    deviceIdProvider: DeviceIdProvider,
) : ViewModel() {
    /**
     * Actual device ID from platform
     */
    val deviceId: String = deviceIdProvider.getDeviceId()

    /**
     * Privacy report stats calculated from incidents
     */
    val privacyStats: StateFlow<PrivacyReportStats> =
        incidentRepository
            .getAllIncidents()
            .map { incidents -> calculateStats(incidents) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = PrivacyReportStats(),
            )

    private fun calculateStats(incidents: List<Incident>): PrivacyReportStats {
        val blocked = incidents.filter { it.isBlocked }
        return PrivacyReportStats(
            totalThreatsBlocked = blocked.size,
            scamCallsBlocked = blocked.count { it.type == IncidentType.SCAM_CALL },
            phishingLinksBlocked = blocked.count { it.type == IncidentType.PHISHING_LINK },
            dangerousAppsBlocked = blocked.count { it.type == IncidentType.DANGEROUS_APP },
            sextortionBlocked = blocked.count { it.type == IncidentType.SEXTORTION },
            policeImpersonationBlocked = blocked.count { it.type == IncidentType.POLICE_IMPERSONATION },
            // Estimate data processed based on incidents scanned
            dataProcessedMb = "${(incidents.size * 0.5).toInt()}MB",
        )
    }
}
