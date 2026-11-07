package com.example.shiftalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        c.startForegroundService(Intent(c, RingService::class.java))
        Scheduler.schedule(c) // ставим следующий рабочий день
    }
}
