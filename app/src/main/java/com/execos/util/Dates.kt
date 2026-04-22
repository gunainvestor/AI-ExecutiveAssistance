package com.execos.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object Dates {
    private val iso: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val isoDateTime: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val yearFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy")
    private val monthFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    fun todayIso(): String = LocalDate.now().format(iso)

    fun nowIso(): String = LocalDateTime.now().format(isoDateTime)

    fun yearKey(reference: LocalDate = LocalDate.now()): String =
        reference.format(yearFmt)

    fun monthKey(reference: LocalDate = LocalDate.now()): String =
        reference.format(monthFmt)

    fun quarterKey(reference: LocalDate = LocalDate.now()): String {
        val q = ((reference.monthValue - 1) / 3) + 1
        return "${reference.year}-Q$q"
    }

    fun weekStartIso(reference: LocalDate = LocalDate.now()): String =
        reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(iso)

    fun weekEndIso(weekStartIso: String): String =
        LocalDate.parse(weekStartIso, iso).plusDays(6).format(iso)

    fun millisUntilNextLocalMidnight(zone: ZoneId = ZoneId.systemDefault()): Long {
        val now = System.currentTimeMillis()
        val nextMidnight = ZonedDateTime.now(zone)
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        return (nextMidnight - now).coerceAtLeast(1_000L)
    }
}
