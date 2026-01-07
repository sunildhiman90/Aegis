package app.aegis.data

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
}