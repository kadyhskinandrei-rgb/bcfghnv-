package com.example.shiftalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object Scheduler {
    fun prefs(c: Context): SharedPreferences =
        c.getSharedPreferences("alarm", Context.MODE_PRIVATE)

    // work дней работаем, затем off дней отдыхаем, считая от даты start
    fun isWorkDay(start: LocalDate, d: LocalDate, work: Int, off: Int): Boolean {
        if (d.isBefore(start)) return false
        return Math.floorMod(ChronoUnit.DAYS.between(start, d), (work + off).toLong()) < work
    }

    fun nextAlarm(c: Context): LocalDateTime? {
        val p = prefs(c)
        if (!p.getBoolean("on", false)) return null
        val start = LocalDate.ofEpochDay(p.getLong("start", LocalDate.now().toEpochDay()))
        val end = if (p.getBoolean("hasEnd", false)) LocalDate.ofEpochDay(p.getLong("end", 0)) else null
        val work = p.getInt("work", 2)
        val off = p.getInt("off", 2)
        val h = p.getInt("hour", 7)
        val m = p.getInt("minute", 0)
        val now = LocalDateTime.now()
        var d = now.toLocalDate()
        repeat(800) {
            if (end != null && d.isAfter(end)) return null
            val dt = d.atTime(h, m)
            if (isWorkDay(start, d, work, off) && dt.isAfter(now)) return dt
            d = d.plusDays(1)
        }
        return null
    }

    fun schedule(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            c, 0, Intent(c, AlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        am.cancel(pi)
        val next = nextAlarm(c) ?: return
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val show = PendingIntent.getActivity(
            c, 1, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(millis, show), pi)
    }
}
