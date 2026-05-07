package com.nuvio.app.core.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput

actual fun Modifier.desktopHorizontalDragScroll(state: LazyListState): Modifier =
    this.pointerInput(state) {
        var prevX: Float? = null
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val pointer = event.changes.firstOrNull() ?: continue
                if (pointer.type != PointerType.Mouse) continue
                when (event.type) {
                    PointerEventType.Press -> {
                        prevX = pointer.position.x
                    }
                    PointerEventType.Move -> {
                        val prev = prevX
                        if (prev != null && pointer.pressed) {
                            val delta = prev - pointer.position.x
                            if (kotlin.math.abs(delta) > 2f) {
                                state.dispatchRawDelta(delta)
                                pointer.consume()
                            }
                            prevX = pointer.position.x
                        }
                    }
                    PointerEventType.Release -> {
                        prevX = null
                    }
                }
            }
        }
    }
