package com.stela.agent.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo

data class AccessibilityNodeMatch(
    val text: String?,
    val contentDescription: String?,
    val resourceId: String?,
    val className: String?,
    val clickable: Boolean,
    val enabled: Boolean,
    val focusable: Boolean,
    val visible: Boolean
)

class AccessibilityController(
    private val context: Context,
    private val service: AccessibilityService? = null
) {
    fun isServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains("${context.packageName}/.accessibility.StelaAccessibilityService")
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun findNode(identifier: String): AccessibilityNodeInfo? {
        val root = service?.rootInActiveWindow ?: return null
        return findNode(root, identifier)
    }

    fun clickNode(identifier: String): Boolean {
        val node = findNode(identifier) ?: return false
        return performAction(node) { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
    }

    fun longClickNode(identifier: String): Boolean {
        val node = findNode(identifier) ?: return false
        return performAction(node) { it.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK) }
    }

    fun setText(identifier: String, value: String): Boolean {
        val node = findNode(identifier) ?: return false
        return performAction(node) {
            val bundle = android.os.Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value) }
            it.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
        }
    }

    fun scroll(direction: String): Boolean {
        val root = service?.rootInActiveWindow ?: return false
        val action = when (direction.lowercase()) {
            "down" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            "up" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            else -> return false
        }
        return performAction(root) { it.performAction(action) }
    }

    fun goBack(): Boolean {
        val serviceLocal = service ?: return false
        return serviceLocal.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    fun readVisibleText(): String {
        val root = service?.rootInActiveWindow ?: return ""
        return collectVisibleText(root).joinToString(separator = " ")
    }

    private fun performAction(node: AccessibilityNodeInfo, action: (AccessibilityNodeInfo) -> Boolean): Boolean {
        if (node.isClickable || node.isEnabled) {
            val result = action(node)
            if (result) return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (performAction(child, action)) return true
        }
        return false
    }

    private fun findNode(node: AccessibilityNodeInfo, identifier: String): AccessibilityNodeInfo? {
        val normalized = identifier.trim()
        val resourceName = node.viewIdResourceName
        val text = node.text?.toString()
        val contentDescription = node.contentDescription?.toString()
        if (normalized.equals(resourceName, ignoreCase = true) ||
            normalized.equals(text, ignoreCase = true) ||
            normalized.equals(contentDescription, ignoreCase = true) ||
            normalized.equals(text?.lowercase()?.replace(" ", ""), ignoreCase = true)
        ) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val match = findNode(child, normalized)
            if (match != null) return match
        }
        return null
    }

    private fun collectVisibleText(node: AccessibilityNodeInfo): List<String> {
        val results = mutableListOf<String>()
        val text = node.text?.toString()
        if (!text.isNullOrBlank() && node.isVisibleToUser) results += text
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            results += collectVisibleText(child)
        }
        return results
    }
}
