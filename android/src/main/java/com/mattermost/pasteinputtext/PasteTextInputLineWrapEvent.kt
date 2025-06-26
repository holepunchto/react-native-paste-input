package com.mattermost.pasteinputtext

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.Event

class PasteTextInputLineWrapEvent(
    surfaceId: Int,
    viewId: Int,
    private val mLineCount: Int,
    private val mIsWrapped: Boolean
) : Event<PasteTextInputLineWrapEvent>(surfaceId, viewId) {

    override fun getEventName(): String = "onLineWrap"

    override fun getCoalescingKey(): Short = 0

    override fun getEventData(): WritableMap {
        val eventData = Arguments.createMap()
        eventData.putInt("target", viewTag)
        eventData.putInt("lineCount", mLineCount)
        eventData.putBoolean("isWrapped", mIsWrapped)
        return eventData
    }
}

