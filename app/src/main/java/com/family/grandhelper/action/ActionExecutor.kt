package com.family.grandhelper.action

import android.content.Context
import com.family.grandhelper.R
import com.family.grandhelper.intent.IntentResult

class ActionExecutor(private val context: Context) {

    fun execute(intentResult: IntentResult): ActionResult {
        return when (intentResult) {
            is IntentResult.Alarm -> AlarmAction(context).execute(intentResult)
            is IntentResult.Call -> {
                // Phase 2
                ActionResult.Failure("전화 기능은 아직 준비 중이에요")
            }
            is IntentResult.Navigation -> {
                // Phase 3
                ActionResult.Failure("길 안내 기능은 아직 준비 중이에요")
            }
            is IntentResult.KakaoTalk -> {
                // Phase 4
                ActionResult.Failure("카톡 기능은 아직 준비 중이에요")
            }
            is IntentResult.Unknown -> {
                ActionResult.Failure(context.getString(R.string.error_unknown_command))
            }
        }
    }
}
