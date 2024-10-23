package com.example.brickalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

// ... другие импорты ...

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "Будильник сработал!", Toast.LENGTH_LONG).show()
        // ... логика для воспроизведения звука, отображения уведомления и т.д. ...

        // Получение информации о будильнике (например, из intent.extras)
        // val alarmId = intent.getIntExtra("alarmId", 0)
        // val alarmMode = intent.getSerializableExtra("alarmMode") as AlarmMode

        // if (alarmMode == AlarmMode.ONCE) {
        //     // Отключение будильника, если он однократный
        //     // ...
        // }
    }
}