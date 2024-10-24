package com.example.brickalarm

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TimePicker

import androidx.recyclerview.widget.RecyclerView

class AlarmAdapter(
    private val alarms: MutableList<Alarm>,
    private val onAlarmClick: (View) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val alarmItem: LinearLayout = itemView.findViewById(R.id.alarmItem)
        val timePicker: TimePicker = itemView.findViewById(R.id.timePicker)
        val repeatModeRadioGroup: RadioGroup = itemView.findViewById(R.id.repeatModeRadioGroup)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.alarm_item, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = alarms[position]

        holder.timePicker.hour = alarm.hour
        holder.timePicker.minute = alarm.minute

        when (alarm.mode) {
            AlarmMode.ONCE -> holder.repeatModeRadioGroup.check(R.id.repeatOnce)
            AlarmMode.DAILY -> holder.repeatModeRadioGroup.check(R.id.repeatDaily)
            AlarmMode.WEEKDAYS -> holder.repeatModeRadioGroup.check(R.id.repeatWeekdays)
        }

        holder.alarmItem.setBackgroundColor(if (alarm.isOn) Color.GREEN else Color.LTGRAY)
        holder.alarmItem.setOnClickListener { onAlarmClick(it) }
    }

    override fun getItemCount(): Int {
        return alarms.size
    }
}