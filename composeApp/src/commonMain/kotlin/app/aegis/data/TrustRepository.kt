package app.aegis.data

import co.touchlab.kermit.Logger

object TrustRepository {
    // 1. In-Memory Sets (Lost on App Restart)
    private val userTrustedCache = mutableSetOf<String>() // User manually clicked "Trust"

    // No init() needed anymore since we aren't loading from disk.

    /**
     * General check: Is this safe to ignore?
     * Returns TRUE if it exists in EITHER list.
     */
    fun isTrusted(contactName: String): Boolean {
        return userTrustedCache.contains(contactName)
    }

    /**
     * 🟢 MANUAL TRUST: User clicked "I trust this person"
     */
    fun trustContact(contactName: String) {
        userTrustedCache.add(contactName)
        // TODO: Later, insert into Room DB here
    }


    /**
     * Debug/UI Helper
     */
    fun getUserTrustedList(): List<String> = userTrustedCache.toList()

    fun clearCache() {
        userTrustedCache.clear()
    }

    // Temporary counter for current session
    private val sessionTrustScores = mutableMapOf<String, Int>()

    fun increaseTrustScore(contactName: String) {
        // If already trusted, ignore
        if (isTrusted(contactName)) return

        val currentScore: Int = sessionTrustScores.getOrElse(contactName) { 0 } + 1
        sessionTrustScores[contactName] = currentScore

        Logger.withTag("Aegis").d( "Trust Score for '$contactName': $currentScore/3")

        // 🎯 THRESHOLD: 3 Safe Messages
        // If a named contact sends 3 normal messages without triggering Scam Regex,
        // we assume they are safe and whitelist them permanently.
        if (currentScore >= 3) {
            Logger.withTag("Aegis").d(" '$contactName' has earned our trust. Whitelisting.")
            trustContact(contactName) // Saves to Trusted List
            sessionTrustScores.remove(contactName)
        }
    }
}