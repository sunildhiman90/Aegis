package app.aegis.domain.model

data class TrustedContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val addedAt: Long
)
