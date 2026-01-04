package app.aegis.data

object TrustRepository {
    // In-memory set of trusted contact titles
    private val trustedContacts = mutableSetOf<String>()

    fun isTrusted(title: String): Boolean {
        return trustedContacts.contains(title)
    }

    fun trustContact(title: String) {
        if (title.isNotEmpty()) {
            trustedContacts.add(title)
            println("Aegis: Trusted contact added -> $title")
        }
    }

    fun clear() {
        trustedContacts.clear()
    }
}