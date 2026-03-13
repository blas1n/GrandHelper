package com.family.grandhelper.action

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.family.grandhelper.R
import com.family.grandhelper.config.ContactAliasConfig
import com.family.grandhelper.intent.IntentResult
import com.family.grandhelper.service.VoiceAccessibilityService

class KakaoTalkAction(private val context: Context) {

    companion object {
        private const val KAKAO_PACKAGE = "com.kakao.talk"
        private const val STEP_DELAY = 1500L
        private const val TOTAL_TIMEOUT = 20000L
    }

    private val handler = Handler(Looper.getMainLooper())

    fun execute(kakao: IntentResult.KakaoTalk, onResult: (ActionResult) -> Unit) {
        val service = VoiceAccessibilityService.instance
        if (service == null) {
            onResult(ActionResult.Failure("접근성 서비스가 필요해요.\n설정에서 활성화해 주세요."))
            return
        }

        val contactName = ContactAliasConfig.resolve(kakao.contactAlias)
            ?: kakao.contactAlias
        val message = kakao.message

        if (message.isBlank()) {
            onResult(ActionResult.Failure("보낼 메시지가 없어요"))
            return
        }

        // 타임아웃 설정
        val timeoutRunnable = Runnable {
            cleanup(service)
            onResult(ActionResult.Failure("카톡 전송에 시간이 너무 오래 걸려요"))
        }
        handler.postDelayed(timeoutRunnable, TOTAL_TIMEOUT)

        // Step 1: 카카오톡 실행
        launchKakaoTalk { launched ->
            if (!launched) {
                handler.removeCallbacks(timeoutRunnable)
                onResult(ActionResult.Failure("카카오톡 앱을 찾지 못했어요"))
                return@launchKakaoTalk
            }

            // Step 2: 검색으로 대화상대 찾기
            handler.postDelayed({
                openSearch(service) { searchOpened ->
                    if (!searchOpened) {
                        handler.removeCallbacks(timeoutRunnable)
                        cleanup(service)
                        onResult(ActionResult.Failure("카카오톡 검색을 열지 못했어요"))
                        return@openSearch
                    }

                    // Step 3: 검색어 입력
                    handler.postDelayed({
                        typeSearchQuery(service, contactName) { typed ->
                            if (!typed) {
                                handler.removeCallbacks(timeoutRunnable)
                                cleanup(service)
                                onResult(ActionResult.Failure("검색어를 입력하지 못했어요"))
                                return@typeSearchQuery
                            }

                            // Step 4: 검색 결과에서 대화상대 선택
                            handler.postDelayed({
                                selectContact(service, contactName) { selected ->
                                    if (!selected) {
                                        handler.removeCallbacks(timeoutRunnable)
                                        cleanup(service)
                                        onResult(ActionResult.Failure("${kakao.contactAlias}을(를) 찾지 못했어요"))
                                        return@selectContact
                                    }

                                    // Step 5: 메시지 입력 & 전송
                                    handler.postDelayed({
                                        sendMessage(service, message) { sent ->
                                            handler.removeCallbacks(timeoutRunnable)
                                            cleanup(service)
                                            if (sent) {
                                                onResult(
                                                    ActionResult.Success(
                                                        message = context.getString(R.string.kakao_done),
                                                        subMessage = "${kakao.contactAlias}에게 전송 완료"
                                                    )
                                                )
                                            } else {
                                                onResult(ActionResult.Failure("메시지를 보내지 못했어요"))
                                            }
                                        }
                                    }, STEP_DELAY)
                                }
                            }, STEP_DELAY)
                        }
                    }, STEP_DELAY)
                }
            }, STEP_DELAY)
        }
    }

    private fun launchKakaoTalk(callback: (Boolean) -> Unit) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(KAKAO_PACKAGE)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                context.startActivity(intent)
                callback(true)
            } else {
                callback(false)
            }
        } catch (e: Exception) {
            callback(false)
        }
    }

    private fun openSearch(service: VoiceAccessibilityService, callback: (Boolean) -> Unit) {
        // 카카오톡 상단 검색 아이콘 찾기
        // viewId 기반 시도 → text 기반 폴백
        val searchNode = service.findNodeById("$KAKAO_PACKAGE:id/search_button")
            ?: service.findNodeByText("검색")
            ?: service.findNodeByClassName("android.widget.ImageButton") { node ->
                node.contentDescription?.toString()?.contains("검색") == true
            }

        if (searchNode != null) {
            service.clickNode(searchNode)
            callback(true)
        } else {
            callback(false)
        }
    }

    private fun typeSearchQuery(
        service: VoiceAccessibilityService,
        query: String,
        callback: (Boolean) -> Unit
    ) {
        // 검색 입력 필드 찾기
        val editText = service.findNodeById("$KAKAO_PACKAGE:id/search_edit_text")
            ?: service.findNodeByClassName("android.widget.EditText")

        if (editText != null) {
            service.setNodeText(editText, query)
            callback(true)
        } else {
            callback(false)
        }
    }

    private fun selectContact(
        service: VoiceAccessibilityService,
        name: String,
        callback: (Boolean) -> Unit
    ) {
        // 검색 결과에서 이름과 매칭되는 항목 클릭
        val contactNode = service.findNodeByText(name)
        if (contactNode != null) {
            service.clickNode(contactNode)
            callback(true)
        } else {
            callback(false)
        }
    }

    private fun sendMessage(
        service: VoiceAccessibilityService,
        message: String,
        callback: (Boolean) -> Unit
    ) {
        // 메시지 입력 필드 찾기
        val inputField = service.findNodeById("$KAKAO_PACKAGE:id/message_edit_text")
            ?: service.findNodeByClassName("android.widget.MultiAutoCompleteTextView")
            ?: service.findNodeByClassName("android.widget.EditText")

        if (inputField == null) {
            callback(false)
            return
        }

        // 텍스트 입력
        if (!service.setNodeText(inputField, message)) {
            callback(false)
            return
        }

        // 전송 버튼 찾기 & 클릭
        handler.postDelayed({
            val sendButton = service.findNodeById("$KAKAO_PACKAGE:id/media_send_button")
                ?: service.findNodeByText("전송")
                ?: service.findNodeByClassName("android.widget.ImageButton") { node ->
                    node.contentDescription?.toString()?.contains("전송") == true
                }

            if (sendButton != null) {
                service.clickNode(sendButton)
                // 전송 후 뒤로가기로 복귀
                handler.postDelayed({
                    service.pressBack()
                    handler.postDelayed({
                        service.pressBack()
                    }, 300)
                }, 500)
                callback(true)
            } else {
                callback(false)
            }
        }, 500)
    }

    private fun cleanup(service: VoiceAccessibilityService) {
        service.setWindowChangeListener(null)
    }
}
