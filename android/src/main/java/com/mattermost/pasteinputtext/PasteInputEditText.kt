package com.mattermost.pasteinputtext

import android.annotation.SuppressLint
import android.os.Build
import android.text.TextWatcher
import android.text.Editable
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.events.EventDispatcher
import com.facebook.react.views.textinput.ReactEditText
import java.lang.Exception
import android.view.KeyEvent
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import android.view.inputmethod.InputConnectionWrapper

@SuppressLint("ViewConstructor")
class PasteInputEditText(context: ThemedReactContext) : ReactEditText(context) {
    private lateinit var mOnPasteListener: IPasteInputListener
    private lateinit var mPasteEventDispatcher: EventDispatcher
    private var mDisabledCopyPaste: Boolean = false
    private var mSelectionWatcher: SelectionWatcher? = null
    private var mLineWrapWatcher: LineWrapWatcher? = null
    private var lastLineCount: Int = 1
    private var mNativeEventCount: Int = 0
    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            post {
                checkLineWrap()
            }
        }
    }

    init {
        addTextChangedListener(textWatcher)
    }

    private fun checkLineWrap() {
        val layout = layout ?: return
        val currentLineCount = layout.lineCount
        if (currentLineCount != lastLineCount) {
            val isWrapped = currentLineCount > 1
            mLineWrapWatcher?.onLineWrapChanged(currentLineCount, isWrapped)
            lastLineCount = currentLineCount
        }
    }

    interface SelectionWatcher {
        fun onSelectionChanged(selStart: Int, selEnd: Int)
    }

    interface LineWrapWatcher {
        fun onLineWrapChanged(lineCount: Int, isWrapped: Boolean)
    }

    fun setSelectionWatcher(selectionWatcher: SelectionWatcher?) {
        mSelectionWatcher = selectionWatcher
    }

    fun setLineWrapWatcher(lineWrapWatcher: LineWrapWatcher?) {
        mLineWrapWatcher = lineWrapWatcher
    }
     fun setNativeEventCount(count: Int) {
        mNativeEventCount = count
    }
    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        mSelectionWatcher?.onSelectionChanged(selStart, selEnd)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        checkLineWrap()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            dispatchKeyPressEvent(event)
        }
        return super.dispatchKeyEvent(event)
    }

    private fun dispatchKeyPressEvent(event: KeyEvent) {
        val key = getKeyString(event) ?: return
        
        val eventData = Arguments.createMap().apply {
            putString("key", key)
            putInt("eventCount", ++mNativeEventCount)
        }

        val reactContext = context as? ReactContext
        reactContext?.getJSModule(RCTEventEmitter::class.java)
            ?.receiveEvent(id, "topKeyPress", eventData)
    }

    private fun getKeyString(event: KeyEvent): String? {
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DEL -> "Backspace"
            KeyEvent.KEYCODE_ENTER -> "Enter"
            KeyEvent.KEYCODE_TAB -> "Tab"
            KeyEvent.KEYCODE_ESCAPE -> "Escape"
            KeyEvent.KEYCODE_SPACE -> " "
            KeyEvent.KEYCODE_DPAD_LEFT -> "ArrowLeft"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "ArrowRight"
            KeyEvent.KEYCODE_DPAD_UP -> "ArrowUp"
            KeyEvent.KEYCODE_DPAD_DOWN -> "ArrowDown"
            else -> {
                val unicodeChar = event.unicodeChar
                if (unicodeChar > 0) {
                    unicodeChar.toChar().toString()
                } else {
                    null
                }
            }
        }
    }

    fun setDisableCopyPaste(disabled: Boolean) {
        this.mDisabledCopyPaste = disabled
    }

    fun setOnPasteListener(listener: IPasteInputListener, event: EventDispatcher?) {
        mOnPasteListener = listener
        if (event != null) {
            mPasteEventDispatcher = event
        }
    }

    fun getOnPasteListener() : IPasteInputListener {
        return mOnPasteListener
    }
    private fun dispatchKeyPressForChar(key: String) {
    val eventData = Arguments.createMap().apply {
        putString("key", key)
        putInt("eventCount", ++mNativeEventCount)
    }

    val reactContext = context as? ReactContext
    reactContext?.getJSModule(RCTEventEmitter::class.java)
        ?.receiveEvent(id, "topKeyPress", eventData)
    }
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val ic = super.onCreateInputConnection(outAttrs) ?: return null

        EditorInfoCompat.setContentMimeTypes(outAttrs, arrayOf("image/gif", "image/jpg", "image/jpeg", "image/png", "image/webp", "image/*", "*/*"))

        val callback = InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, _ ->
            val lacksPermission = (flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && lacksPermission) {
                try {
                    inputContentInfo.requestPermission()
                } catch (e: Exception) {
                    return@OnCommitContentListener false
                }
            }

            if (!mDisabledCopyPaste) {
                getOnPasteListener().onPaste(inputContentInfo.contentUri, mPasteEventDispatcher)
            }

            true
        }

        val wrappedIc = InputConnectionCompat.createWrapper(ic, outAttrs, callback)
        
        // Wrap again to intercept text input for key press events
        return object : InputConnectionWrapper(wrappedIc, true) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                text?.forEach { char ->
                    dispatchKeyPressForChar(char.toString())
                }
                return super.commitText(text, newCursorPosition)
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                if (beforeLength > 0) {
                    dispatchKeyPressForChar("Backspace")
                }
                return super.deleteSurroundingText(beforeLength, afterLength)
            }
        }
    }
}
