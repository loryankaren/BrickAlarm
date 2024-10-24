package com.example.brickalarm

enum class AlarmMode {
    ONCE, DAILY, WEEKDAYS
}

data class Alarm(
    val hour: Int,
    val minute: Int,
    val mode: AlarmMode,
    var isOn: Boolean = true
)