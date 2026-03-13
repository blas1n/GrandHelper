package com.family.grandhelper.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class VoiceAccessibilityService : AccessibilityService() {

    companion object {
        var instance: VoiceAccessibilityService? = null
            private set

        fun isAvailable(): Boolean = instance != null
    }

    private var onWindowChanged: ((String?) -> Unit)? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                onWindowChanged?.invoke(event.packageName?.toString())
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        onWindowChanged = null
        super.onDestroy()
    }

    fun setWindowChangeListener(listener: ((String?) -> Unit)?) {
        onWindowChanged = listener
    }

    /**
     * viewIdResourceName으로 노드 찾기
     */
    fun findNodeById(viewId: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        return nodes?.firstOrNull()
    }

    /**
     * 텍스트로 노드 찾기
     */
    fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return nodes?.firstOrNull()
    }

    /**
     * className + 조건으로 노드 찾기 (재귀 탐색)
     */
    fun findNodeByClassName(
        className: String,
        predicate: (AccessibilityNodeInfo) -> Boolean = { true }
    ): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return findNodeRecursive(root, className, predicate)
    }

    private fun findNodeRecursive(
        node: AccessibilityNodeInfo,
        className: String,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (node.className?.toString() == className && predicate(node)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeRecursive(child, className, predicate)
            if (found != null) return found
        }
        return null
    }

    /**
     * 모든 매칭 노드 찾기 (재귀)
     */
    fun findAllNodesByClassName(className: String): List<AccessibilityNodeInfo> {
        val root = rootInActiveWindow ?: return emptyList()
        val results = mutableListOf<AccessibilityNodeInfo>()
        collectNodesRecursive(root, className, results)
        return results
    }

    private fun collectNodesRecursive(
        node: AccessibilityNodeInfo,
        className: String,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.className?.toString() == className) {
            results.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectNodesRecursive(child, className, results)
        }
    }

    /**
     * 노드 클릭
     */
    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        // 부모를 타고 올라가며 클릭 가능한 노드 찾기
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) {
                return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            parent = parent.parent
        }
        return false
    }

    /**
     * 노드에 텍스트 입력
     */
    fun setNodeText(node: AccessibilityNodeInfo, text: String): Boolean {
        val bundle = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
    }

    /**
     * 뒤로가기
     */
    fun pressBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }
}
