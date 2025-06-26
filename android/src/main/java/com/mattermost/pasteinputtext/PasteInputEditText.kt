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

@SuppressLint("ViewConstructor")
class PasteInputEditText(context: ThemedReactContext) : ReactEditText(context) {
    private lateinit var mOnPasteListener: IPasteInputListener
    private lateinit var mPasteEventDispatcher: EventDispatcher
    private var mDisabledCopyPaste: Boolean = false
    private var mSelectionWatcher: SelectionWatcher? = null
    private var mLineWrapWatcher: LineWrapWatcher? = null
    private var lastLineCount: Int = 1
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

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        mSelectionWatcher?.onSelectionChanged(selStart, selEnd)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        checkLineWrap()
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

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        val ic = super.onCreateInputConnection(outAttrs)

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

        return InputConnectionCompat.createWrapper(ic!!, outAttrs, callback)
    }
}
