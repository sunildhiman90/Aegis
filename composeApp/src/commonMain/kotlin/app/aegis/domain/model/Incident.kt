package app.aegis.domain.model

import app.aegis.utils.DateUtils
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
data class Incident constructor(
    val id: String,
    val type: IncidentType,
    val description: String,
    val timestamp: Long = DateUtils.getCurrentTimestamp(),
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
