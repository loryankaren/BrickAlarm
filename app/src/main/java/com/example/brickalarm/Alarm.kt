package com.example.brickalarm

enum class AlarmMode {
    ONCE,
    DAILY,
    WEEKDAYS,
    CUSTOM_DAYS
}

data class Alarm(
    val hour: Int,
    val minute: Int,
    val mode: AlarmMode,
    val selectedDays: List<Int> = emptyList(), // Список выбранных дней недели
    var isOn: Boolean = true
)