package com.example.brickalarm

import android.Manifest

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color

import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.AppCompatButton
import android.widget.GridLayout
import android.widget.NumberPicker
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import androidx.core.app.ActivityCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class MainActivity : AppCompatActivity() {

    private var selectedDaysForNewAlarm: List<Int> = emptyList() // Переменная для хранения выбранных дней

    private var selectedButton: AppCompatButton? = null

    private val lightGreen = Color.argb(255,184, 219, 199)

    private lateinit var addAlarmButton: FloatingActionButton
    private lateinit var toggleAllAlarmsButton: MaterialButton

    private lateinit var alarmManager: AlarmManager
    //private lateinit var alarmRecyclerView: RecyclerView
    private lateinit var alarmAdapter: AlarmAdapter
    private val alarms = mutableListOf<Alarm>()
    private lateinit var alarmGridLayout: GridLayout
    private lateinit var infoTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SCHEDULE_EXACT_ALARM), 0)
            }
        }

        setContentView(R.layout.activity_main)

        infoTextView = findViewById<TextView>(R.id.infoTextView)

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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение предоставлено
                Toast.makeText(this, "Разрешение предоставлено", Toast.LENGTH_SHORT).show()
                updateAlarms()
            } else {
                Toast.makeText(this, "Разрешение отклонено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateAlarmButtons() {
        for (i in 0 until alarmGridLayout.childCount) {
            val button = alarmGridLayout.getChildAt(i) as? AppCompatButton
            val alarm = alarms[i] // Получаем будильник по позиции
            button?.background = resources.getDrawable(if (alarm.isOn) R.drawable.alarm_on_background else R.drawable.alarm_off_background, null)
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

        val button = view as? AppCompatButton

        // Снимаем выделение с предыдущей кнопки
        selectedButton?.isSelected = false
        selectedButton?.setBackgroundResource(android.R.drawable.btn_default)

        // Выделяем текущую кнопку
        selectedButton = button
        selectedButton?.isSelected = true
        selectedButton?.setBackgroundResource(R.drawable.selected_button_background)

        val alarmModeText = when (alarm.mode) {
            AlarmMode.ONCE -> "Однократно"
            AlarmMode.DAILY -> "Ежедневно"
            AlarmMode.WEEKDAYS -> "По будням"
            AlarmMode.CUSTOM_DAYS -> {
                val daysOfWeekNames = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")
                alarm.selectedDays.joinToString(", ") { daysOfWeekNames[it - 1] }
            }
        }

        infoTextView.text = alarmModeText

    }

    private fun showAddAlarmDialog() {
        // Создаем layout для диалогового окна
        val dialogView = layoutInflater.inflate(R.layout.alarm_item, null)

        val modeRadioGroup = dialogView.findViewById<RadioGroup>(R.id.repeatModeRadioGroup)

        // Находим NumberPicker в layout
        val hourPicker = dialogView.findViewById<NumberPicker>(R.id.hourPicker)
        val minutePicker = dialogView.findViewById<NumberPicker>(R.id.minutePicker)

        // Настраиваем NumberPicker
        hourPicker.minValue = 0
        hourPicker.maxValue = 23
        minutePicker.minValue = 0
        minutePicker.maxValue = 59

        // Устанавливаем текущее время (необязательно)
        val calendar = Calendar.getInstance()
        hourPicker.value = calendar.get(Calendar.HOUR_OF_DAY)
        minutePicker.value = calendar.get(Calendar.MINUTE)

        AlertDialog.Builder(this)
            .setTitle("Выберите время будильника")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val hour = hourPicker.value
                val minute = minutePicker.value
                val mode = when (modeRadioGroup.checkedRadioButtonId) {
                    R.id.repeatOnce -> AlarmMode.ONCE
                    R.id.repeatDaily -> AlarmMode.DAILY
                    R.id.repeatWeekdays -> AlarmMode.WEEKDAYS
                    R.id.repeatCustomDays -> AlarmMode.CUSTOM_DAYS
                    else -> AlarmMode.ONCE
                }

                // Вложенная логика для каждого режима работы
                when (mode) {
                    AlarmMode.ONCE -> {
                        val newAlarm = createAlarm(hour, minute, AlarmMode.ONCE)
                        alarms.add(newAlarm)
                        scheduleAlarm(newAlarm)
                        addAlarmButton(newAlarm)
                    }
                    AlarmMode.DAILY -> {
                        val newAlarm = createAlarm(hour, minute, AlarmMode.DAILY)
                        alarms.add(newAlarm)
                        scheduleAlarm(newAlarm)
                        addAlarmButton(newAlarm)
                    }
                    AlarmMode.WEEKDAYS -> {
                        val newAlarm = createAlarm(hour, minute, AlarmMode.WEEKDAYS)
                        alarms.add(newAlarm)
                        scheduleAlarm(newAlarm)
                        addAlarmButton(newAlarm)
                    }
                    AlarmMode.CUSTOM_DAYS -> {
                        showDaysOfWeekPickerDialog(this) { context, selectedDays ->
                            val newAlarm = createAlarm(hour, minute, AlarmMode.CUSTOM_DAYS, selectedDays)
                            alarms.add(newAlarm)
                            (context as MainActivity).scheduleAlarm(newAlarm)
                            addAlarmButton(newAlarm)
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }


    private fun createAlarm(hour: Int, minute: Int, mode: AlarmMode, selectedDays: List<Int> = emptyList()): Alarm {
        return Alarm(hour, minute, mode, selectedDays)
    }

    private fun addAlarmButton(alarm: Alarm) {
        val newAlarmButton = AppCompatButton(this)
        newAlarmButton.setBackgroundResource(android.R.drawable.btn_default)
        val params = GridLayout.LayoutParams()
        params.setGravity(Gravity.CENTER)
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        newAlarmButton.layoutParams = params
        newAlarmButton.text = String.format("%02d:%02d", alarm.hour, alarm.minute)
        newAlarmButton.setOnClickListener {
            onAlarmClick(it)
        }
        alarmGridLayout.addView(newAlarmButton)

        newAlarmButton.post { // Установка listener после добавления в GridLayout
            newAlarmButton.setOnClickListener {
                onAlarmClick(it)
            }
        }
    }

    private fun showDaysOfWeekPickerDialog(context: Context, onDaysSelected: (Context, List<Int>) -> Unit) {
        val daysOfWeek = arrayOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")
        val checkedDays = booleanArrayOf(false, false, false, false, false, false, false)

        AlertDialog.Builder(this)
            .setTitle("Выберите дни недели")
            .setMultiChoiceItems(daysOfWeek, checkedDays) { _, which, isChecked ->
                checkedDays[which] = isChecked
            }
            .setPositiveButton("OK") { _, _ ->
                val selectedDays = mutableListOf<Int>()
                for (i in checkedDays.indices) {
                    if (checkedDays[i]) {
                        selectedDays.add(i + 1)
                    }
                }
                onDaysSelected(context, selectedDays) // Вызываем callback с выбранными днями
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

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
            AlarmMode.CUSTOM_DAYS -> {
                // Логика для расчета интервала для пользовательских дней (требует доработки)
                // ...
                0L // Пока возвращаем 0, чтобы не использовать setRepeating
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.SCHEDULE_EXACT_ALARM) == PackageManager.PERMISSION_GRANTED) {
                // Разрешение предоставлено
                if (alarmManager.canScheduleExactAlarms()) {
                    // Устройство поддерживает точные будильники
                    try {
                        if (repeatInterval > 0) {
                            alarmManager.setRepeating(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                repeatInterval,
                                pendingIntent
                            )
                        } else {
                            alarmManager.setAlarmClock(
                                AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
                                pendingIntent
                            )
                        }
                        Toast.makeText(this, "Будильник установлен на ${alarm.hour}:${alarm.minute}", Toast.LENGTH_SHORT).show()
                    } catch (e: SecurityException) {
                        // Обработка SecurityException, например, сообщить пользователю или использовать альтернативный подход
                        Toast.makeText(this, "Ошибка установки будильника: ${e.message}", Toast.LENGTH_LONG).show()
                        // Здесь можно использовать запасной вариант, например, неточные будильники или WorkManager
                    }
                } else {
                    // Устройство не поддерживает точные будильники
                    // Используйте альтернативный подход, например, WorkManager или setInexactRepeating
                    Toast.makeText(this, "Устройство не поддерживает точные будильники", Toast.LENGTH_LONG).show()
                    // Здесь нужно реализовать запасную логику
                }
            } else {
                // Разрешение не предоставлено, запросить его
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SCHEDULE_EXACT_ALARM), 0)
            }
        } else {
            // Для Android 11 и ниже разрешение не требуется
            if (repeatInterval > 0) {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    repeatInterval,
                    pendingIntent
                )
            } else {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
                    pendingIntent
                )
            }
            Toast.makeText(this, "Будильник установлен на ${alarm.hour}:${alarm.minute}", Toast.LENGTH_SHORT).show()
        }

        // Дополнительная логика для AlarmMode.CUSTOM_DAYS (требует доработки)
        // ...
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