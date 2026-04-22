package com.execos.ui.navigation

object Routes {
    const val Home = "home"
    const val Focus = "focus"
    const val Decisions = "decisions"
    const val Reflection = "reflection"
    const val Weekly = "weekly"
    const val Energy = "energy"
    const val Account = "account"
    const val Goals = "goals"
    const val Usage = "usage"
    const val Calendar = "calendar"
    const val CalendarDayDetails = "calendar/day/{dateIso}"

    fun calendarDayDetails(dateIso: String): String = "calendar/day/$dateIso"
}
