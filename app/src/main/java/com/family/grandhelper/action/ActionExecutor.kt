package com.family.grandhelper.action

import android.content.Context
import com.family.grandhelper.R
import com.family.grandhelper.intent.IntentResult

class ActionExecutor(private val context: Context) {

    fun execute(intentResult: IntentResult): ActionResult {
        return when (intentResult) {
            is IntentResult.Alarm -> AlarmAction(context).execute(intentResult)
            is IntentResult.Call -> CallAction(context).execute(intentResult)
            is IntentResult.Navigation -> NavigationAction(context).execute(intentResult)
            is IntentResult.Unknown -> {
                ActionResult.Failure(context.getString(R.string.error_unknown_command))
            }
        }
    }
}
