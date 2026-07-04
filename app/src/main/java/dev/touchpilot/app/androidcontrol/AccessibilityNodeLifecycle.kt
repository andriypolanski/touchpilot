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

/**
 * Depth-first search that recycles sibling branches that do not contain a match.
 */
internal fun AccessibilityNodeInfo.findNodeRecycling(
    predicate: (AccessibilityNodeInfo) -> Boolean
): AccessibilityNodeInfo? {
    if (predicate(this)) return this

    for (index in 0 until childCount) {
        val child = getChild(index) ?: continue
        val found = try {
            child.findNodeRecycling(predicate)
        } catch (e: Exception) {
            child.recycleSafely()
            throw e
        }
        if (found != null) return found
        child.recycleSafely()
    }

    return null
}

/**
 * Depth-first collection that recycles child subtrees with no matches.
 *
 * @return `true` when [this] node or any descendant matched [predicate].
 */
internal fun AccessibilityNodeInfo.collectNodesRecycling(
    predicate: (AccessibilityNodeInfo) -> Boolean,
    result: MutableList<AccessibilityNodeInfo>
): Boolean {
    val matchesSelf = predicate(this)
    if (matchesSelf) result.add(this)

    var subtreeHasMatch = matchesSelf
    for (index in 0 until childCount) {
        val child = getChild(index) ?: continue
        val childHasMatch = child.collectNodesRecycling(predicate, result)
        if (!childHasMatch) {
            child.recycleSafely()
        }
        subtreeHasMatch = subtreeHasMatch || childHasMatch
    }
    return subtreeHasMatch
}
