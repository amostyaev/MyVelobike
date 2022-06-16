package com.raistlin.myvelobike.util

import android.content.Context
import com.raistlin.myvelobike.R
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

fun Int.dp(context: Context) = ceil(context.resources.displayMetrics.density * this).toInt()

fun Long.asTimeString(context: Context): String {
    val zoned = (this * 1000).utcDate()
    val days = SimpleDateFormat("D", Locale.US).format(zoned).toInt() - 1
    val plural = if (days > 0) "'${context.resources.getQuantityString(R.plurals.history_days, days, days)}' " else ""
    return SimpleDateFormat("${plural}HH:mm:ss", Locale.US).format(zoned)
}

fun LocalDate.asString() = "${if (dayOfMonth < 10) "0$dayOfMonth" else dayOfMonth}.${if (monthNumber < 10) "0$monthNumber" else monthNumber}.$year"
fun LocalDateTime.asTimeString() = "${if (hour < 10) "0$hour" else hour}:${if (minute < 10) "0$minute" else minute}"
fun Long.utcDate() = Date(this - with(Calendar.getInstance()) { get(Calendar.ZONE_OFFSET) + get(Calendar.DST_OFFSET) })
