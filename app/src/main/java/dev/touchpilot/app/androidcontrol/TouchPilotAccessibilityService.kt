package dev.touchpilot.app.androidcontrol

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class TouchPilotAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityBridge.attach(this)
    }

    override fun onDestroy() {
        AccessibilityBridge.detach(this)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // The first spike is pull-based from the debug screen.
    }

    override fun onInterrupt() {
        // No long-running gesture is started in the initial spike.
    }

    fun observeScreen(): String {
        val root = rootInActiveWindow ?: return "No active window is available."
        return buildString {
            appendLine("TouchPilot screen snapshot")
            appendNode(root, depth = 0, maxDepth = 8)
        }
    }

    fun tapByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNode(root) { candidate ->
            val label = candidate.text?.toString()
                ?: candidate.contentDescription?.toString()
                ?: ""
            label.contains(text, ignoreCase = true)
        } ?: return false

        return clickNodeOrParent(node)
    }

    fun typeIntoFocusedField(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findNode(root) { it.isFocused }
            ?: return false

        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun scroll(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (forward) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }

        val scrollable = findNode(root) { candidate ->
            candidate.isScrollable || candidate.actionList.any { it.id == action }
        } ?: return false

        return scrollable.performAction(action)
    }

    fun pressBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun pressHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun waitForText(text: String, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs.coerceIn(250L, 30_000L)
        while (System.currentTimeMillis() < deadline) {
            val root = rootInActiveWindow
            if (root != null && containsText(root, text)) {
                return true
            }
            Thread.sleep(150L)
        }
        return false
    }

    private fun StringBuilder.appendNode(
        node: AccessibilityNodeInfo,
        depth: Int,
        maxDepth: Int
    ) {
        if (depth > maxDepth) return

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        append("  ".repeat(depth))
        append("- ")
        append(node.className ?: "Unknown")

        val text = node.text?.toString().orEmpty()
        val description = node.contentDescription?.toString().orEmpty()
        val viewId = node.viewIdResourceName.orEmpty()

        if (text.isNotBlank()) append(" text=\"$text\"")
        if (description.isNotBlank()) append(" desc=\"$description\"")
        if (viewId.isNotBlank()) append(" id=\"$viewId\"")
        if (node.isClickable) append(" clickable")
        if (node.isFocused) append(" focused")
        append(" bounds=$bounds")
        appendLine()

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            appendNode(child, depth + 1, maxDepth)
        }
    }

    private fun findNode(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (predicate(node)) return node

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            val found = findNode(child, predicate)
            if (found != null) return found
        }

        return null
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            current = current.parent
        }
        return false
    }

    private fun containsText(node: AccessibilityNodeInfo, text: String): Boolean {
        return findNode(node) { candidate ->
            val label = candidate.text?.toString()
                ?: candidate.contentDescription?.toString()
                ?: ""
            label.contains(text, ignoreCase = true)
        } != null
    }
}
