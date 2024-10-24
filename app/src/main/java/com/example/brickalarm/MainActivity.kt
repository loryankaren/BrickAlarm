package com.example.brickalarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color

import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
// import android.widget.Button // Закомментируйте эту строку
import androidx.appcompat.widget.AppCompatButton // Или import com.google.android.material.button.MaterialButton
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import com.google.android.material.button.MaterialButton
//import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class MainActivity : AppCompatActivity() {

    private val lightGreen = Color.argb(255,184, 219, 199)

    private lateinit var addAlarmButton: FloatingActionButton
    private lateinit var toggleAllAlarmsButton: MaterialButton

    private lateinit var alarmManager: AlarmManager
    //private lateinit var alarmRecyclerView: RecyclerView
    private lateinit var alarmAdapter: AlarmAdapter
    private val alarms = mutableListOf<Alarm>()
    private lateinit var alarmGridLayout: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        alarmGridLayout = findViewById<GridLayout>(R.id.alarmGridLayout)
        //alarmRecyclerView = findViewById(R.id.alarmGrid)
        addAlarmButton = findViewById(R.id.addAlarmButton)

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmAdapter = AlarmAdapter(alarms) { position -> onAlarmClick(position) }
        //alarmRecyclerView.adapter = alarmAdapter

        // Загрузка сохраненных будильников
        loadAlarms()

        findViewById<FloatingActionButton>(R.id.addAlarmButton).setOnClickListener {
            showAddAlarmDialog()
        }

        toggleAllAlarmsButton = findViewById<MaterialButton>(R.id.toggleAllAlarmsButton)

        toggleAllAlarmsButton.setOnClickListener {
            if (toggleAllAlarmsButton.text == "Откл. все") {
                // Выключаем все будильники
                alarms.forEach { it.isOn = false }
                toggleAllAlarmsButton.text = "Вкл. все"
            } else {
                // Включаем все будильники
                alarms.forEach { it.isOn = true }
                toggleAllAlarmsButton.text = "Откл. все"
            }

            // Обновляем UI и состояние будильников
            updateAlarmButtons()
            updateAlarms()
        }

    }

    private fun updateAlarmButtons() {
        for (i in 0 until alarmGridLayout.childCount) {
            val button = alarmGridLayout.getChildAt(i) as? AppCompatButton
            val alarm = alarms[i] // Получаем будильник по позиции
            button?.backgroundTintList = ColorStateList.valueOf(if (alarm.isOn) lightGreen else Color.LTGRAY)
        }
    }

    private fun updateAlarms() {
        alarms.forEachIndexed { index, alarm ->
            if (alarm.isOn) {
                scheduleAlarm(alarm)
            } else {
                cancelAlarm(alarm)
            }
        }
    }

    private fun onAlarmClick(view: View) {
        val position = alarmGridLayout.indexOfChild(view) // Получаем позицию кнопки
        val alarm = alarms[position] // Получаем будильник по позиции
        alarm.isOn = !alarm.isOn

        // Находим кнопку в GridLayout по позиции
        val button = alarmGridLayout.getChildAt(position) as? AppCompatButton
        button?.backgroundTintList = ColorStateList.valueOf(if (alarm.isOn) lightGreen else Color.LTGRAY)

        if (alarm.isOn) {
            scheduleAlarm(alarm)
        } else {
            Toast.makeText(this, "Будильник на ${alarm.hour}:${alarm.minute} отключен", Toast.LENGTH_SHORT).show()
            cancelAlarm(alarm)
        }
    }

    private fun showAddAlarmDialog() {
        val materialTimePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
            .setMinute(Calendar.getInstance().get(Calendar.MINUTE))
            .setTitleText("Выберите время будильника")
            .build()

        materialTimePicker.addOnPositiveButtonClickListener {
            val hour = materialTimePicker.hour
            val minute = materialTimePicker.minute

            val alarmModes = arrayOf("Однократно", "Ежедневно", "По будням")
            var selectedMode = AlarmMode.ONCE

            AlertDialog.Builder(this)
                .setTitle("Выберите режим работы")
                .setItems(alarmModes) { _, which ->
                    selectedMode = when (which) {
                        0 -> AlarmMode.ONCE
                        1 -> AlarmMode.DAILY
                        2 -> AlarmMode.WEEKDAYS
                        else -> AlarmMode.ONCE
                    }
                    val newAlarm = Alarm(hour, minute, selectedMode)
                    alarms.add(newAlarm)
                    scheduleAlarm(newAlarm)

                    val newAlarmButton = AppCompatButton(this)
                    val params = GridLayout.LayoutParams()
                    params.setGravity(Gravity.CENTER)
                    params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    newAlarmButton.layoutParams = params
                    newAlarmButton.text = String.format("%02d:%02d", newAlarm.hour, newAlarm.minute)
                    newAlarmButton.backgroundTintList = ColorStateList.valueOf(if (newAlarm.isOn) lightGreen else Color.LTGRAY)
                    alarmGridLayout.addView(newAlarmButton)

                    newAlarmButton.post { // Установка listener после добавления в GridLayout
                        newAlarmButton.setOnClickListener {
                            onAlarmClick(it)
                        }
                    }

                }
                .show()
        }

        materialTimePicker.show(supportFragmentManager, "timePicker")

    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleAlarm(alarm: Alarm) {
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, alarm.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val repeatInterval = when (alarm.mode) {
            AlarmMode.ONCE -> 0L // Не повторять
            AlarmMode.DAILY -> AlarmManager.INTERVAL_DAY
            AlarmMode.WEEKDAYS -> AlarmManager.INTERVAL_DAY * 7 // Приблизительно, нужно уточнить логику для будних дней
        }

        if (repeatInterval > 0) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                repeatInterval,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }

        Toast.makeText(this, "Будильник установлен на ${alarm.hour}:${alarm.minute}", Toast.LENGTH_SHORT).show()
    }

    private fun cancelAlarm(alarm: Alarm) {
        // ... (код остается прежним) ...
    }

    private fun loadAlarms() {
        // Загрузка будильников из SharedPreferences или базы данных
        // ...
    }

    private fun saveAlarms() {
        // Сохранение будильников в SharedPreferences или базу данных
        // ...
    }

    override fun onStop() {
        super.onStop()
        saveAlarms() // Сохранение будильников при закрытии приложения
    }
}