package com.family.grandhelper.action

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import com.family.grandhelper.R
import com.family.grandhelper.config.ContactAliasConfig
import com.family.grandhelper.intent.IntentResult

class CallAction(private val context: Context) {

    fun execute(call: IntentResult.Call): ActionResult {
        // 1. 별명으로 실제 이름 찾기
        val realName = call.resolvedName
            ?: ContactAliasConfig.resolve(call.contactAlias)

        // 2. 연락처에서 전화번호 찾기
        val phoneNumber = call.phoneNumber
            ?: realName?.let { lookupPhoneNumber(it) }
            ?: lookupPhoneNumber(call.contactAlias)

        if (phoneNumber == null) {
            return ActionResult.Failure(
                "${call.contactAlias}의 ${context.getString(R.string.error_call_no_contact)}"
            )
        }

        // 3. 전화 걸기
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            ActionResult.Success(
                message = context.getString(R.string.call_done),
                subMessage = call.contactAlias
            )
        } catch (e: SecurityException) {
            ActionResult.Failure("전화 권한이 필요해요")
        } catch (e: Exception) {
            ActionResult.Failure("전화를 걸지 못했어요")
        }
    }

    private fun lookupPhoneNumber(name: String): String? {
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$name%"),
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        } catch (e: SecurityException) {
            // READ_CONTACTS 권한 없음
        } finally {
            cursor?.close()
        }
        return null
    }
}
