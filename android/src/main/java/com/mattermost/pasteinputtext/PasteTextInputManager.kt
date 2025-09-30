package com.mattermost.pasteinputtext

import android.util.Log
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.facebook.react.common.MapBuilder
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.EventDispatcher
import com.facebook.react.views.textinput.ReactEditText
import com.facebook.react.views.textinput.ReactTextInputManager

@ReactModule(name = "PasteTextInput")
class PasteTextInputManager(context: ReactApplicationContext) : ReactTextInputManager() {
  private var disableCopyPaste: Boolean = false
  private val mContext = context

  override fun getName(): String = NAME

  @ReactProp(name = "disableCopyPaste", defaultBoolean = false)
  fun setDisableCopyPaste(editText: PasteInputEditText, disabled: Boolean) {
    disableCopyPaste = disabled
    val eventDispatcher = getEventDispatcher(mContext, editText)
    editText.customInsertionActionModeCallback = PasteInputActionCallback(editText, disabled, eventDispatcher)
    editText.customSelectionActionModeCallback = PasteInputActionCallback(editText, disabled, eventDispatcher)
    editText.setDisableCopyPaste(disabled)
  }

  private fun getEventDispatcher(reactContext: ReactContext, editText: ReactEditText): EventDispatcher? {
    return UIManagerHelper.getEventDispatcherForReactTag(reactContext, editText.id)
  }

  override fun createViewInstance(context: ThemedReactContext): PasteInputEditText {
    val editText = PasteInputEditText(context)

    editText.returnKeyType = "done"
    val eventDispatcher = getEventDispatcher(mContext, editText)
    editText.customInsertionActionModeCallback = PasteInputActionCallback(editText, disableCopyPaste, eventDispatcher)
    editText.customSelectionActionModeCallback = PasteInputActionCallback(editText, disableCopyPaste, eventDispatcher)

    return editText
  }

  override fun addEventEmitters(reactContext: ThemedReactContext, editText: ReactEditText) {
    super.addEventEmitters(reactContext, editText)

    val pasteInputEditText = editText as PasteInputEditText
    val eventDispatcher = getEventDispatcher(reactContext, editText)

    pasteInputEditText.setSelectionWatcher(object : PasteInputEditText.SelectionWatcher {
      override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        eventDispatcher?.dispatchEvent(
          PasteTextInputSelectionChangeEvent(
            UIManagerHelper.getSurfaceId(reactContext),
            pasteInputEditText.id,
            selStart,
            selEnd
          )
        )
      }
    })

    pasteInputEditText.setLineWrapWatcher(object : PasteInputEditText.LineWrapWatcher {
      override fun onLineWrapChanged(lineCount: Int, isWrapped: Boolean) {
        eventDispatcher?.dispatchEvent(
          PasteTextInputLineWrapEvent(
            UIManagerHelper.getSurfaceId(reactContext),
            pasteInputEditText.id,
            lineCount,
            isWrapped
          )
        )
      }
    })

    pasteInputEditText.setOnPasteListener(
      PasteInputListener(pasteInputEditText, reactContext.surfaceId),
      eventDispatcher
    )
  }

  override fun getCommandsMap(): Map<String, Int> =
    MapBuilder.of("setTextAndSelection", CMD_SET_TEXT_AND_SELECTION)

  override fun receiveCommand(view: ReactEditText, commandId: Int, args: ReadableArray?) {
    if (commandId == CMD_SET_TEXT_AND_SELECTION) {
      handleSetTextAndSelection(view, args)
      return
    }
    super.receiveCommand(view, commandId, args)
  }

  override fun receiveCommand(view: ReactEditText, commandId: String, args: ReadableArray?) {
    if (commandId == "setTextAndSelection") {
      handleSetTextAndSelection(view, args)
      return
    }
    super.receiveCommand(view, commandId, args)
  }

  private fun handleSetTextAndSelection(view: ReactEditText, args: ReadableArray?) {
    if (args == null) return
    // args = [eventCount:int, text:string|null, start:int, end:int]
    try {
      // val eventCount = safeGetInt(args, 0) // kept for logging, not used here
      val text = if (!args.isNull(1)) args.getString(1) else null
      val start = safeGetInt(args, 2, -1)
      val end = safeGetInt(args, 3, -1)

      // Log.d(TAG, "setTextAndSelection ec=$eventCount textLen=${text?.length} sel=$start-$end")

      // Run on UI thread to be safe
      view.post {
        if (text != null && view.text?.toString() != text) {
          // Keep state if possible, avoids janky cursor jumps
          view.setText(text)
        }

        val len = view.text?.length ?: 0
        val s = if (start >= 0) start.coerceIn(0, len) else -1
        val e = if (end >= 0) end.coerceIn(0, len) else -1
        if (s >= 0 && e >= 0 && s <= e) {
          try {
            view.setSelection(s, e)
          } catch (t: Throwable) {
            Log.w(TAG, "Invalid selection range s=$s e=$e len=$len", t)
          }
        }
      }
    } catch (t: Throwable) {
      Log.e(TAG, "handleSetTextAndSelection error", t)
    }
  }

  private fun safeGetInt(arr: ReadableArray, index: Int, def: Int = 0): Int {
    return try {
      if (index < arr.size() && !arr.isNull(index)) arr.getInt(index) else def
    } catch (_: Throwable) {
      def
    }
  }

  override fun getExportedCustomBubblingEventTypeConstants(): MutableMap<String, Any> {
    val map = super.getExportedCustomBubblingEventTypeConstants()!!
    map["onPaste"] = MapBuilder.of(
      "phasedRegistrationNames",
      MapBuilder.of("bubbled", "onPaste")
    )

    return map
  }

  override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> {
    return MapBuilder.builder<String, Any>()
        .put(
            "onSelectionChange",
            MapBuilder.of("registrationName", "onSelectionChange")
        )
        .put(
            "onContentSizeChange",
            MapBuilder.of("registrationName", "onContentSizeChange")
        )
        .put(
            "onScroll",
            MapBuilder.of("registrationName", "onScroll")
        )
        .put(
            "onLineWrap",
            MapBuilder.of("registrationName", "onLineWrap")
        )
        .put(
            "topKeyPress",
            MapBuilder.of("registrationName", "onKeyPress")
        )
      .build()
  }

  companion object {
    private const val TAG = "PasteTextInput"
    const val NAME = "PasteTextInput"
    const val CACHE_DIR_NAME = "mmPasteInput"
    const val CMD_SET_TEXT_AND_SELECTION = 1
  }
}
