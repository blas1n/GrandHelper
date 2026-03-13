package com.family.grandhelper.action

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.family.grandhelper.R
import com.family.grandhelper.intent.IntentResult

class NavigationAction(private val context: Context) {

    fun execute(nav: IntentResult.Navigation): ActionResult {
        if (nav.destination.isBlank()) {
            return ActionResult.Failure("목적지를 알 수 없어요")
        }

        // 네이버 지도 → 카카오내비 → 구글 지도 순으로 시도
        val attempts = listOf(
            NavApp("네이버 지도", buildNaverMapIntent(nav.destination)),
            NavApp("카카오내비", buildKakaoNaviIntent(nav.destination)),
            NavApp("구글 지도", buildGoogleMapsIntent(nav.destination)),
        )

        for (attempt in attempts) {
            try {
                context.startActivity(attempt.intent)
                return ActionResult.Success(
                    message = context.getString(R.string.navi_done),
                    subMessage = "${nav.destination} → ${attempt.name}"
                )
            } catch (_: ActivityNotFoundException) {
                continue
            } catch (_: Exception) {
                continue
            }
        }

        return ActionResult.Failure(context.getString(R.string.error_navi_no_app))
    }

    private fun buildNaverMapIntent(destination: String): Intent {
        val encoded = Uri.encode(destination)
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("nmap://search?query=$encoded&appname=com.family.grandhelper")
            `package` = "com.nhn.android.nmap"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    private fun buildKakaoNaviIntent(destination: String): Intent {
        val encoded = Uri.encode(destination)
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("kakaonavi://search?dest=$encoded")
            `package` = "com.locnall.KimGiSa"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    private fun buildGoogleMapsIntent(destination: String): Intent {
        val encoded = Uri.encode(destination)
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("google.navigation:q=$encoded")
            `package` = "com.google.android.apps.maps"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    private data class NavApp(val name: String, val intent: Intent)
}
