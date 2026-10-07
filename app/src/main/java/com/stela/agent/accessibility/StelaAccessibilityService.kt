package com.stela.agent.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class StelaAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No uncontrolled scanning loop is performed here; the service remains a safe entry point for future automation.
    }

    override fun onInterrupt() = Unit
}
