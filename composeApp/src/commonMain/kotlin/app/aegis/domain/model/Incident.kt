package app.aegis.domain.model

import kotlinx.datetime.Clock

data class Incident(
    val id: String,
    val type: IncidentType,
    val description: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val isBlocked: Boolean = false,
    val severity: IncidentSeverity = IncidentSeverity.MEDIUM
)

enum class IncidentType {
    SCAM_CALL,
    PHISHING_LINK,
    DANGEROUS_APP,
    POLICE_IMPERSONATION,
    SEXTORTION,
    OTHER
}

enum class IncidentSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}
