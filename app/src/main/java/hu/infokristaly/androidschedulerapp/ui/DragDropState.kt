package hu.infokristaly.androidschedulerapp.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

class DragDropState(
    val lazyListState: LazyListState,
    var onMove: (fromIndex: Int, toIndex: Int) -> Unit
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    var draggingItemOffsetY by mutableFloatStateOf(0f)
        private set

    fun onDragStart(index: Int) {
        draggingItemIndex = index
        draggingItemOffsetY = 0f
    }

    fun onDrag(deltaY: Float) {
        var currIndex = draggingItemIndex ?: return
        draggingItemOffsetY += deltaY

        var moved = true
        while (moved) {
            moved = false
            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            val currentItem = visibleItems.firstOrNull { it.index == currIndex } ?: break
            val currentCenter = currentItem.offset + currentItem.size / 2f + draggingItemOffsetY

            val prevItem = visibleItems.firstOrNull { it.index == currIndex - 1 }
            if (currIndex > 0 && prevItem != null) {
                val prevCenter = prevItem.offset + prevItem.size / 2f
                if (currentCenter < prevCenter) {
                    val targetIndex = currIndex - 1
                    onMove(currIndex, targetIndex)
                    draggingItemOffsetY += prevItem.size.toFloat()
                    currIndex = targetIndex
                    draggingItemIndex = currIndex
                    moved = true
                    continue
                }
            }

            val totalItems = lazyListState.layoutInfo.totalItemsCount
            val nextItem = visibleItems.firstOrNull { it.index == currIndex + 1 }
            if (currIndex < totalItems - 1 && nextItem != null) {
                val nextCenter = nextItem.offset + nextItem.size / 2f
                if (currentCenter > nextCenter) {
                    val targetIndex = currIndex + 1
                    onMove(currIndex, targetIndex)
                    draggingItemOffsetY -= nextItem.size.toFloat()
                    currIndex = targetIndex
                    draggingItemIndex = currIndex
                    moved = true
                    continue
                }
            }
        }

        // Auto-scroll when dragging near top/bottom edges of the viewport
        val currIdx = draggingItemIndex ?: return
        val currentItemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currIdx }
        if (currentItemInfo != null) {
            val currentCenter = currentItemInfo.offset + currentItemInfo.size / 2f + draggingItemOffsetY
            val viewportStart = lazyListState.layoutInfo.viewportStartOffset.toFloat()
            val viewportEnd = lazyListState.layoutInfo.viewportEndOffset.toFloat()
            val autoScrollThreshold = 60f

            val distanceFromTop = currentCenter - viewportStart
            val distanceFromBottom = viewportEnd - currentCenter

            if (distanceFromTop < autoScrollThreshold && lazyListState.canScrollBackward) {
                val scrollDelta = (distanceFromTop - autoScrollThreshold) * 0.2f
                lazyListState.dispatchRawDelta(scrollDelta)
            } else if (distanceFromBottom < autoScrollThreshold && lazyListState.canScrollForward) {
                val scrollDelta = (autoScrollThreshold - distanceFromBottom) * 0.2f
                lazyListState.dispatchRawDelta(scrollDelta)
            }
        }
    }

    fun onDragEnd() {
        draggingItemIndex = null
        draggingItemOffsetY = 0f
    }

    fun onDragInterrupted() {
        onDragEnd()
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState = rememberLazyListState(),
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
): DragDropState {
    val state = remember(lazyListState) {
        DragDropState(lazyListState = lazyListState, onMove = onMove)
    }
    state.onMove = onMove
    return state
}

@Composable
fun Modifier.dragHandleGesture(
    dragDropState: DragDropState,
    index: Int
): Modifier {
    val currentIndex by rememberUpdatedState(index)
    return this.pointerInput(dragDropState) {
        detectDragGestures(
            onDragStart = { dragDropState.onDragStart(currentIndex) },
            onDrag = { change, dragAmount ->
                change.consume()
                dragDropState.onDrag(dragAmount.y)
            },
            onDragEnd = { dragDropState.onDragEnd() },
            onDragCancel = { dragDropState.onDragInterrupted() }
        )
    }
}
