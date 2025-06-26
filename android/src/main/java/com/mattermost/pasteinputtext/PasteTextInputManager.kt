package com.mattermost.pasteinputtext

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

    pasteInputEditText.setOnPasteListener(PasteInputListener(pasteInputEditText, reactContext.surfaceId), eventDispatcher)
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
        .build()
  }

  companion object {
    const val NAME = "PasteTextInput"
    const val CACHE_DIR_NAME = "mmPasteInput"
  }
}
