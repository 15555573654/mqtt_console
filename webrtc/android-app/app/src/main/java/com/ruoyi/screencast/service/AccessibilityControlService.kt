package com.ruoyi.screencast.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.ruoyi.screencast.webrtc.AccessibilityHelper

class AccessibilityControlService : AccessibilityService() {
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityHelper.setService(this)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }
    
    override fun onInterrupt() {
    }
    
    fun performClickAt(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        
        dispatchGesture(gesture, null, null)
    }
    
    fun performSwipeGesture(x1: Float, y1: Float, x2: Float, y2: Float) {
        val path = Path()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()
        
        dispatchGesture(gesture, null, null)
    }
    
    fun performBackAction() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }
    
    fun performHomeAction() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }
    
    fun performMenuAction() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        AccessibilityHelper.setService(null)
    }
}
