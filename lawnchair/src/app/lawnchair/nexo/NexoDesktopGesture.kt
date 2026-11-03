/*
 * ESCATE NEXO NX09E: opt-in, passive desktop handwriting.
 * Copyright 2026 ESCATE NEXO contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package app.lawnchair.nexo

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import app.lawnchair.LawnchairLauncher
import com.android.launcher3.BubbleTextView
import com.android.launcher3.LauncherState
import com.android.launcher3.folder.FolderIcon
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Observes touch events AFTER Launcher3 dispatch; it never consumes them.
 *
 * A stroke is eligible only if it starts inside the actual workspace, away
 * from Android system edges, hotseat, NEXO panel, icons, folders and widgets.
 * The feature is OFF by default. No global overlays / accessibility service.
 */
internal class NexoDesktopGesture(private val launcher: LawnchairLauncher) {
    companion object {
        private const val PREFS = "nexo_desktop_gestures_v1"
        private const val ENABLED = "enabled"

        fun isEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(ENABLED, false)

        fun setEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(ENABLED, enabled).apply()
            Log.d("NEXO_DESKTOP", "enabled=" + enabled)
        }
    }

    private val points = ArrayList<PointF>(128)
    private val rect = Rect()
    private var tracking = false
    private var startedAt = 0L

    private fun dp(value: Int) =
        (value * launcher.resources.displayMetrics.density + .5f).toInt()

    private fun isInside(view: View, x: Int, y: Int): Boolean =
        view.isShown && view.getGlobalVisibleRect(rect) && rect.contains(x, y)

    private fun hasItemAt(view: View, x: Int, y: Int): Boolean {
        if (!isInside(view, x, y)) return false
        if (view is BubbleTextView || view is FolderIcon || view is AppWidgetHostView) {
            return true
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                if (hasItemAt(view.getChildAt(i), x, y)) return true
            }
        }
        return false
    }

    private fun safeEmptyWorkspacePoint(x: Int, y: Int): Boolean {
        if (launcher.stateManager.state != LauncherState.NORMAL) return false
        val metrics = launcher.resources.displayMetrics
        val minX = max(dp(40), (metrics.widthPixels * .06f).toInt())
        val minY = max(dp(125), (metrics.heightPixels * .30f).toInt())
        val maxY = (metrics.heightPixels * .79f).toInt()
        if (x < minX || x > metrics.widthPixels - minX || y < minY || y > maxY) {
            return false
        }
        val workspace = launcher.workspace ?: return false
        if (!isInside(workspace, x, y)) return false
        if (isInside(launcher.hotseat, x, y)) return false
        if (hasItemAt(workspace, x, y)) return false

        // NEXO's panel is a separate overlay in the DragLayer, not a workspace child.
        if (launcher.isNexoPanelAtScreenPoint(x, y)) return false
        return true
    }

    private fun reset() {
        tracking = false
        points.clear()
    }

    /**
     * Call only from LawnchairLauncher.dispatchTouchEvent, after super.
     * Raw coordinates match getGlobalVisibleRect on the current display.
     */
    fun observe(event: MotionEvent) {
        if (!isEnabled(launcher)) {
            if (tracking) reset()
            return
        }
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_CANCEL ||
            action == MotionEvent.ACTION_POINTER_DOWN ||
            action == MotionEvent.ACTION_POINTER_UP ||
            event.pointerCount != 1) {
            reset()
            return
        }

        val x = event.rawX.toInt()
        val y = event.rawY.toInt()
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                reset()
                if (!safeEmptyWorkspacePoint(x, y)) return
                tracking = true
                startedAt = SystemClock.uptimeMillis()
                points.add(PointF(x.toFloat(), y.toFloat()))
                Log.d("NEXO_DESKTOP", "down x=$x y=$y")
            }
            MotionEvent.ACTION_MOVE -> {
                if (!tracking) return
                if (!safeEmptyWorkspacePoint(x, y)) {
                    reset()
                    return
                }
                val prev = points.lastOrNull()
                if (prev == null || hypot(x.toFloat() - prev.x, y.toFloat() - prev.y) >= dp(3)) {
                    points.add(PointF(x.toFloat(), y.toFloat()))
                }
                if (points.size > 320 ||
                    SystemClock.uptimeMillis() - startedAt > 8000L) {
                    reset()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!tracking) return
                if (!safeEmptyWorkspacePoint(x, y)) {
                    reset()
                    return
                }
                val elapsed = SystemClock.uptimeMillis() - startedAt
                points.add(PointF(x.toFloat(), y.toFloat()))
                val stroke = points.toList()
                reset()

                if (stroke.size < 6 || elapsed < 140 || elapsed > 8000) return
                val extentX = stroke.maxOf { it.x } - stroke.minOf { it.x }
                val extentY = stroke.maxOf { it.y } - stroke.minOf { it.y }
                // Ignore straight swipes, taps and small accidental movements.
                if (extentX < dp(50) || extentY < dp(65)) return
                Log.d("NEXO_DESKTOP", "up points=${stroke.size} elapsed=${elapsed}ms " +
                    "extentX=$extentX extentY=$extentY")
                launcher.dragLayer.post {
                    if (!launcher.isDestroyed && isEnabled(launcher) &&
                        launcher.stateManager.state == LauncherState.NORMAL) {
                        NexoGestureStudio.performDesktopStroke(launcher, stroke)
                    }
                }
            }
        }
    }
}
