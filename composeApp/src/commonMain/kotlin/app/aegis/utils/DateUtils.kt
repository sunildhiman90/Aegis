package app.aegis.utils

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object DateUtils {

    fun getLocalDateTime(
        timeZone: TimeZone =
            TimeZone.currentSystemDefault(),
    ): String =
        Clock.System
            .now()
            .toLocalDateTime(timeZone)
            .toString()

    fun getCurrentTimestamp() = Clock.System.now().toEpochMilliseconds()
}
