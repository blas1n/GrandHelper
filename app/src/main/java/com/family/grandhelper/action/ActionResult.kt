package com.family.grandhelper.action

sealed class ActionResult {
    data class Success(val message: String, val subMessage: String) : ActionResult()
    data class Failure(val message: String) : ActionResult()
}
