package com.example.brickalarm

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.RadioGroup

import androidx.recyclerview.widget.RecyclerView

class AlarmAdapter(
    private val alarms: MutableList<Alarm>,
    private val onAlarmClick: (View) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val alarmItem: LinearLayout = itemView.findViewById(R.id.alarmItem)
        val hourPicker: NumberPicker = itemView.findViewById(R.id.hourPicker)
        val minutePicker: NumberPicker = itemView.findViewById(R.id.minutePicker)
        val repeatModeRadioGroup: RadioGroup = itemView.findViewById(R.id.repeatModeRadioGroup)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.alarm_item, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {

        val alarm = alarms[position]

        holder.hourPicker.value = alarm.hour
        holder.minutePicker.value = alarm.minute

        when (alarm.mode) {
            AlarmMode.ONCE -> holder.repeatModeRadioGroup.check(R.id.repeatOnce)
            AlarmMode.DAILY -> holder.repeatModeRadioGroup.check(R.id.repeatDaily)
            AlarmMode.WEEKDAYS -> holder.repeatModeRadioGroup.check(R.id.repeatWeekdays)
            AlarmMode.CUSTOM_DAYS -> holder.repeatModeRadioGroup.check(R.id.repeatCustomDays)
        }

        holder.alarmItem.setBackgroundColor(if (alarm.isOn) Color.GREEN else Color.LTGRAY)
        holder.alarmItem.setOnClickListener { onAlarmClick(it) }
    }

    override fun getItemCount(): Int {
        return alarms.size
    }
}