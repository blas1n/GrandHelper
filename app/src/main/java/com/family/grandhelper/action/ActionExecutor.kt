package com.family.grandhelper.action

import android.content.Context
import com.family.grandhelper.R
import com.family.grandhelper.intent.IntentResult

class ActionExecutor(private val context: Context) {

    /**
     * 동기 실행 (알람, 전화, 네비게이션)
     */
    fun execute(intentResult: IntentResult): ActionResult {
        return when (intentResult) {
            is IntentResult.Alarm -> AlarmAction(context).execute(intentResult)
            is IntentResult.Call -> CallAction(context).execute(intentResult)
            is IntentResult.Navigation -> NavigationAction(context).execute(intentResult)
            is IntentResult.KakaoTalk -> {
                // KakaoTalk은 비동기로 처리해야 함 → executeAsync 사용
                ActionResult.Failure("카톡은 executeAsync를 사용하세요")
            }
            is IntentResult.Unknown -> {
                ActionResult.Failure(context.getString(R.string.error_unknown_command))
            }
        }
    }

    /**
     * 비동기 실행 (카카오톡 — Accessibility 자동화에 딜레이 필요)
     */
    fun executeAsync(intentResult: IntentResult, onResult: (ActionResult) -> Unit) {
        when (intentResult) {
            is IntentResult.KakaoTalk -> {
                KakaoTalkAction(context).execute(intentResult, onResult)
            }
            else -> {
                onResult(execute(intentResult))
            }
        }
    }
}
