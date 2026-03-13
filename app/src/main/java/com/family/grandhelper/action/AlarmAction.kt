package com.family.grandhelper.action

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.family.grandhelper.R
import com.family.grandhelper.intent.IntentResult

class AlarmAction(private val context: Context) {

    fun execute(alarm: IntentResult.Alarm): ActionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, alarm.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, alarm.minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            alarm.label?.let { putExtra(AlarmClock.EXTRA_MESSAGE, it) }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            ActionResult.Success(
                message = context.getString(R.string.alarm_done),
                subMessage = alarm.displayText
            )
        } catch (e: Exception) {
            ActionResult.Failure(context.getString(R.string.error_alarm_fail))
        }
    }
}
