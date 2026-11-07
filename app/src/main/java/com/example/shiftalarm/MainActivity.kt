package com.example.shiftalarm

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : Activity() {
    private lateinit var p: SharedPreferences
    private lateinit var dateBtn: Button
    private lateinit var timeBtn: Button
    private lateinit var endBtn: Button
    private lateinit var sw: Switch
    private lateinit var endSw: Switch
    private lateinit var info: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        p = Scheduler.prefs(this)
        if (Build.VERSION.SDK_INT >= 33)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        fun label(t: String, size: Float = 16f) = TextView(this).apply {
            text = t; textSize = size; setPadding(0, 32, 0, 8)
        }
        fun picker(key: String, def: Int) = NumberPicker(this).apply {
            minValue = 1; maxValue = 14; wrapSelectorWheel = false
            value = p.getInt(key, def)
            setOnValueChangedListener { _, _, v ->
                p.edit().putInt(key, v).apply(); update()
            }
        }
        fun column(title: String, v: View) = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(TextView(context).apply { text = title; gravity = Gravity.CENTER })
            addView(v)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        root.addView(label("Будильник по графику", 28f))

        root.addView(label("График (дней работы / дней отдыха):"))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(column("Рабочих", picker("work", 2)))
        row.addView(column("Выходных", picker("off", 2)))
        root.addView(row)

        root.addView(label("Начало графика (первый рабочий день):"))
        dateBtn = Button(this).also { root.addView(it) }

        endSw = Switch(this).apply { text = "Задать дату окончания графика"; setPadding(0, 32, 0, 8) }
        root.addView(endSw)
        endBtn = Button(this).also { root.addView(it) }

        root.addView(label("Время подъёма в рабочие дни:"))
        timeBtn = Button(this).also { root.addView(it) }

        sw = Switch(this).apply { text = "Будильник включён"; setPadding(0, 48, 0, 48) }
        root.addView(sw)
        info = TextView(this).apply { textSize = 18f }
        root.addView(info)

        setContentView(ScrollView(this).apply { addView(root) })

        dateBtn.setOnClickListener {
            val d = startDate()
            DatePickerDialog(this, { _, y, m, day ->
                p.edit().putLong("start", LocalDate.of(y, m + 1, day).toEpochDay()).apply()
                update()
            }, d.year, d.monthValue - 1, d.dayOfMonth).show()
        }
        endBtn.setOnClickListener {
            val d = endDate()
            DatePickerDialog(this, { _, y, m, day ->
                p.edit().putLong("end", LocalDate.of(y, m + 1, day).toEpochDay()).apply()
                update()
            }, d.year, d.monthValue - 1, d.dayOfMonth).show()
        }
        timeBtn.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                p.edit().putInt("hour", h).putInt("minute", m).apply()
                update()
            }, p.getInt("hour", 7), p.getInt("minute", 0), true).show()
        }

        endSw.isChecked = p.getBoolean("hasEnd", false)
        endSw.setOnCheckedChangeListener { _, on ->
            val e = p.edit().putBoolean("hasEnd", on)
            if (on && !p.contains("end")) e.putLong("end", startDate().plusDays(90).toEpochDay())
            e.apply()
            update()
        }
        sw.isChecked = p.getBoolean("on", false)
        sw.setOnCheckedChangeListener { _, on ->
            p.edit().putBoolean("on", on).apply()
            update()
        }
        refresh()
    }

    private fun startDate() = LocalDate.ofEpochDay(p.getLong("start", LocalDate.now().toEpochDay()))
    private fun endDate() = LocalDate.ofEpochDay(p.getLong("end", startDate().plusDays(90).toEpochDay()))

    private fun update() {
        Scheduler.schedule(this)
        refresh()
    }

    private fun refresh() {
        val f = DateTimeFormatter.ofPattern("d MMMM yyyy")
        dateBtn.text = startDate().format(f)
        endBtn.text = endDate().format(f)
        endBtn.visibility = if (p.getBoolean("hasEnd", false)) View.VISIBLE else View.GONE
        timeBtn.text = "%02d:%02d".format(p.getInt("hour", 7), p.getInt("minute", 0))
        val next = Scheduler.nextAlarm(this)
        info.text = when {
            !p.getBoolean("on", false) -> "Будильник выключен"
            next == null -> "Звонков больше нет: график закончился"
            else -> "Следующий звонок: " + next.format(DateTimeFormatter.ofPattern("EEEE, d MMMM, HH:mm"))
        }
    }
}
