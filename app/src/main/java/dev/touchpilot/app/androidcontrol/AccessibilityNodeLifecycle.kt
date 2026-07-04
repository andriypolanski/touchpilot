package dev.touchpilot.app.androidcontrol

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Helpers for releasing [AccessibilityNodeInfo] instances obtained from
 * [AccessibilityService.rootInActiveWindow], [AccessibilityNodeInfo.getChild],
 * [AccessibilityNodeInfo.findFocus], and [AccessibilityNodeInfo.parent].
 *
 * Each acquired node holds native Binder resources; failing to recycle them
 * during repeated agent tree walks causes memory pressure and can disconnect
 * the accessibility service.
 */
internal inline fun <T> AccessibilityService.useActiveRoot(
    block: (AccessibilityNodeInfo) -> T
): T? {
    val root = rootInActiveWindow ?: return null
    return try {
        block(root)
    } finally {
        root.recycleSafely()
    }
}

internal fun AccessibilityNodeInfo.recycleSafely() {
    runCatching { recycle() }
}

/**
 * Walk from [start] toward the root via [AccessibilityNodeInfo.parent],
 * recycling every parent node acquired along the way.
 */
internal inline fun walkUpFrom(
    start: AccessibilityNodeInfo,
    block: (AccessibilityNodeInfo) -> Boolean
): Boolean {
    var current: AccessibilityNodeInfo? = start
    val parentsToRecycle = mutableListOf<AccessibilityNodeInfo>()
    try {
        while (current != null) {
            if (block(current)) return true
            val parent = current.parent ?: break
            parentsToRecycle += parent
            current = parent
        }
        return false
    } finally {
        parentsToRecycle.forEach { it.recycleSafely() }
    }
}
