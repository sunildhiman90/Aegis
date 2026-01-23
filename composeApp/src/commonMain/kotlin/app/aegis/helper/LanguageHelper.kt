package app.aegis.helper

/**
 * This interface provides helper functions for Multi Language Support
 */
interface LanguageHelper {
    /**
     * This method will update app language code in preferences
     * @param languageCode is the code of Language we are changing our app Language to
     */
    fun setLanguageCode(languageCode: String)

    /**
     * This method will switch app language at platform level
     * @param languageCode is the code of Language we are changing our app Language to
     */
    suspend fun changeLanguage(languageCode: String)

    /**
     * This method will return app language name by languageCode
     * @param languageCode is the code of Language
     */
    suspend fun getLanguageNameByCode(languageCode: String): String?

    /**
     * This method will return app current language
     */
    suspend fun getCurrentLanguage(): String
}
