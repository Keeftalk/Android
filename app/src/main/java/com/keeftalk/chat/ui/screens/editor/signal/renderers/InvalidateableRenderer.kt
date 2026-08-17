package com.keeftalk.chat.ui.screens.editor.signal.renderers

import com.keeftalk.chat.ui.screens.editor.signal.Renderer
import com.keeftalk.chat.ui.screens.editor.signal.RendererContext
import java.lang.ref.WeakReference

/**
 * Ported from Signal Android (AGPL-3.0)
 */
abstract class InvalidateableRenderer : Renderer {
    private var invalidate = WeakReference<RendererContext.Invalidate>(null)

    override fun render(rendererContext: RendererContext) {
        setInvalidate(rendererContext.invalidate)
    }

    private fun setInvalidate(invalidate: RendererContext.Invalidate) {
        if (invalidate !== this.invalidate.get()) {
            this.invalidate = WeakReference(invalidate)
        }
    }

    protected fun invalidate() {
        invalidate.get()?.onInvalidate(this)
    }
}
