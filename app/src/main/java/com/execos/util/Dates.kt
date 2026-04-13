package com.execos.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object Dates {
    private val iso: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayIso(): String = LocalDate.now().format(iso)

    fun weekStartIso(reference: LocalDate = LocalDate.now()): String =
        reference.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(iso)

    fun weekEndIso(weekStartIso: String): String =
        LocalDate.parse(weekStartIso, iso).plusDays(6).format(iso)
}
