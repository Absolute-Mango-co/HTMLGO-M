package com.absolutemango.htmlgom.commonMain.utils

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.util.fastFirstOrNull

/**
 * A robust gesture inspector that detects drags while allowing taps to pass through
 * to parent hitboxes if no significant movement occurs.
 */
suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    awaitEachGesture {
        // 1. Wait for the initial down event. We don't consume it here.
        val down = awaitFirstDown(requireUnconsumed = false)
        onDragStart(down)

        // 2. Start tracking the movement
        val upEvent = dragLoop(pointerId = down.id, onDrag = onDrag)
        
        if (upEvent == null) {
            onDragCancel()
        } else {
            onDragEnd(upEvent)
        }
    }
}

private suspend inline fun AwaitPointerEventScope.dragLoop(
    pointerId: PointerId,
    onDrag: (PointerInputChange, Offset) -> Unit
): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        
        if (change.isConsumed) {
            return null
        }
        
        if (change.changedToUpIgnoreConsumed()) {
            return change
        }

        val dragAmount = change.positionChange()
        if (dragAmount != Offset.Zero) {
            onDrag(change, dragAmount)
        }
        
        pointer = change.id
    }
}
