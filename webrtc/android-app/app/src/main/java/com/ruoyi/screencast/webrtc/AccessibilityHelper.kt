package com.ruoyi.screencast.webrtc

import com.ruoyi.screencast.service.AccessibilityControlService

object AccessibilityHelper {
    
    private var service: AccessibilityControlService? = null
    
    fun setService(accessibilityService: AccessibilityControlService?) {
        service = accessibilityService
    }
    
    fun performClick(x: Float, y: Float) {
        service?.performClickAt(x, y)
    }
    
    fun performSwipe(x1: Float, y1: Float, x2: Float, y2: Float) {
        service?.performSwipeGesture(x1, y1, x2, y2)
    }
    
    fun performBack() {
        service?.performBackAction()
    }
    
    fun performHome() {
        service?.performHomeAction()
    }
    
    fun performMenu() {
        service?.performMenuAction()
    }
}
