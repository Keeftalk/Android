package com.keeftalk.chat.ui.screens.editor.signal

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.text.InputType
import android.util.AttributeSet
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatEditText
import com.keeftalk.chat.ui.screens.editor.signal.model.EditorElement
import com.keeftalk.chat.ui.screens.editor.signal.renderers.MultiLineTextRenderer
import java.util.*

/**
 * Ported from Signal Android (AGPL-3.0)
 */
class HiddenEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var currentTextEditorElement: EditorElement? = null
    private var currentTextEntity: MultiLineTextRenderer? = null
    private var onEndEdit: Runnable? = null
    private var onEditOrSelectionChange: OnEditOrSelectionChange? = null
    private val textFilters = LinkedList<TextFilter>()

    init {
        alpha = 0f
        layoutParams = FrameLayout.LayoutParams(1, 1, Gravity.TOP or Gravity.START)
        isClickable = false
        isFocusable = true
        isFocusableInTouchMode = true
        setBackgroundColor(Color.TRANSPARENT)
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 1f)
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        clearFocus()
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        currentTextEntity?.let { entity ->
            var filtered = text.toString()
            for (filter in textFilters) filtered = filter.filter(filtered)
            entity.setText(filtered)
            postEditOrSelectionChange()
        }
    }

    override fun onEditorAction(actionCode: Int) {
        super.onEditorAction(actionCode)
        if (actionCode == EditorInfo.IME_ACTION_DONE && currentTextEntity != null) {
            currentTextEntity?.setFocused(false)
            endEdit()
        }
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        currentTextEntity?.let {
            it.setFocused(focused)
            if (!focused) endEdit()
        }
    }

    fun addTextFilter(filter: TextFilter) { textFilters.add(filter) }
    fun addTextFilters(filters: Collection<TextFilter>) { textFilters.addAll(filters) }
    fun removeTextFilter(filter: TextFilter) { textFilters.remove(filter) }

    private fun endEdit() { onEndEdit?.run() }

    private fun postEditOrSelectionChange() {
        if (currentTextEditorElement != null && currentTextEntity != null) {
            onEditOrSelectionChange?.onChange(currentTextEditorElement!!, currentTextEntity!!)
        }
    }

    fun getCurrentTextEntity(): MultiLineTextRenderer? = currentTextEntity
    fun getCurrentTextEditorElement(): EditorElement? = currentTextEditorElement

    fun setCurrentTextEditorElement(currentTextEditorElement: EditorElement?) {
        if (currentTextEditorElement?.renderer is MultiLineTextRenderer) {
            this.currentTextEditorElement = currentTextEditorElement
            setCurrentTextEntity(currentTextEditorElement.renderer as MultiLineTextRenderer)
        } else {
            this.currentTextEditorElement = null
            setCurrentTextEntity(null)
        }
        postEditOrSelectionChange()
    }

    private fun setCurrentTextEntity(currentTextEntity: MultiLineTextRenderer?) {
        if (this.currentTextEntity != currentTextEntity) {
            this.currentTextEntity?.setFocused(false)
            this.currentTextEntity = currentTextEntity
            if (currentTextEntity != null) {
                val t = currentTextEntity.getText()
                setText(t)
                setSelection(t.length)
            } else {
                setText("")
            }
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        currentTextEntity?.let {
            it.setSelection(selStart, selEnd)
            postEditOrSelectionChange()
        }
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        val focus = super.requestFocus(direction, previouslyFocusedRect)
        if (currentTextEntity != null && focus) {
            currentTextEntity?.setFocused(true)
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            if (!imm.isAcceptingText) {
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY)
            }
        }
        return focus
    }

    fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    fun setIncognitoKeyboardEnabled(enabled: Boolean) {
        imeOptions = if (enabled) {
            imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        } else {
            imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING.inv()
        }
    }

    fun setOnEndEdit(onEndEdit: Runnable?) { this.onEndEdit = onEndEdit }
    fun setOnEditOrSelectionChange(listener: OnEditOrSelectionChange?) { this.onEditOrSelectionChange = listener }

    interface OnEditOrSelectionChange {
        fun onChange(editorElement: EditorElement, textRenderer: MultiLineTextRenderer)
    }

    interface TextFilter {
        fun filter(text: String): String
    }
}
