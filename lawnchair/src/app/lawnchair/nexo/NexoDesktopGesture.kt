/*
 * ESCATE NEXO NX09F: exclusive, opt-in handwriting on safe desktop background.
 * Copyright 2026 ESCATE NEXO contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package app.lawnchair.nexo

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
 * A transparent view above Launcher3, enabled only by the NEXO panel toggle.
 *
 * Unlike NX09E passive observation, ACTION_DOWN is reserved BEFORE native
 * workspace paging can consume the letter. Touches starting outside the
 * safe blank region return false and reach Launcher3 normally.
 *
 * While ON, standard page-swipes that begin IN the drawing region are
 * deliberately replaced by drawing. Disable the toggle to restore them.
 * This is not a global gesture handler, nor an accessibility overlay.
 */
internal class NexoDesktopGesture(
    private val launcher: LawnchairLauncher,
) : View(launcher) {
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
    private val hitRect = Rect()
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(65, 230, 201)
        strokeWidth = dp(4).toFloat()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private var consuming = false
    private var collecting = false
    private var startedAt = 0L

    init {
        isClickable = true
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setWillNotDraw(false)
        visibility = GONE
    }

    private fun dp(value: Int): Int =
        (value * launcher.resources.displayMetrics.density + .5f).toInt()

    private fun hits(view: View, x: Int, y: Int): Boolean =
        view.isShown && view.getGlobalVisibleRect(hitRect) && hitRect.contains(x, y)

    private fun intersectsItem(view: View, x: Int, y: Int): Boolean {
        if (!hits(view, x, y)) return false
        if (view is BubbleTextView || view is FolderIcon || view is AppWidgetHostView) {
            return true
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                if (intersectsItem(view.getChildAt(i), x, y)) return true
            }
        }
        return false
    }

    private fun safeBlankPoint(x: Int, y: Int): Boolean {
        if (!isEnabled(launcher) || launcher.stateManager.state != LauncherState.NORMAL) {
            return false
        }
        val metrics = launcher.resources.displayMetrics
        val edge = max(dp(44), (metrics.widthPixels * .08f).toInt())
        // Excludes status bar, NEXO panel, dock, search widget and system bottom edge.
        val top = max(dp(135), (metrics.heightPixels * .30f).toInt())
        val bottom = (metrics.heightPixels * .77f).toInt()
        if (x < edge || x > metrics.widthPixels - edge || y < top || y > bottom) {
            return false
        }
        val workspace = launcher.workspace ?: return false
        if (!hits(workspace, x, y)) return false
        if (hits(launcher.hotseat, x, y)) return false
        if (launcher.isNexoPanelAtScreenPoint(x, y)) return false
        if (intersectsItem(workspace, x, y)) return false
        return true
    }

    private fun reset() {
        consuming = false
        collecting = false
        points.clear()
        path.reset()
        invalidate()
        parent?.requestDisallowInterceptTouchEvent(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (collecting) canvas.drawPath(path, ink)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val x = event.rawX.toInt()
        val y = event.rawY.toInt()

        if (action == MotionEvent.ACTION_DOWN) {
            reset()
            if (!safeBlankPoint(x, y) || event.pointerCount != 1) return false
            consuming = true
            collecting = true
            startedAt = SystemClock.uptimeMillis()
            points.add(PointF(x.toFloat(), y.toFloat()))
            path.moveTo(event.x, event.y)
            invalidate()
            parent?.requestDisallowInterceptTouchEvent(true)
            Log.d("NEXO_DESKTOP", "capture_down x=$x y=$y")
            return true
        }

        if (!consuming) return false

        // Consume the whole stream even if an invalid gesture is cancelled.
        // Do not deliver unmatched MOVE / UP to the underlying launcher.
        if (action == MotionEvent.ACTION_CANCEL) {
            Log.d("NEXO_DESKTOP", "capture_cancel")
            reset()
            return true
        }
        if (event.pointerCount != 1 || action == MotionEvent.ACTION_POINTER_DOWN ||
            action == MotionEvent.ACTION_POINTER_UP) {
            collecting = false
            path.reset()
            points.clear()
            invalidate()
            return true
        }

        when (action) {
            MotionEvent.ACTION_MOVE -> {
                if (!collecting) return true
                if (!safeBlankPoint(x, y)) {
                    collecting = false
                    Log.d("NEXO_DESKTOP", "capture_left_safe_area")
                    path.reset()
                    invalidate()
                    return true
                }
                val last = points.lastOrNull()
                if (last == null || hypot(x.toFloat() - last.x, y.toFloat() - last.y) >= dp(3)) {
                    points.add(PointF(x.toFloat(), y.toFloat()))
                    path.lineTo(event.x, event.y)
                    invalidate()
                }
                if (points.size > 320 || SystemClock.uptimeMillis() - startedAt > 8000L) {
                    collecting = false
                    Log.d("NEXO_DESKTOP", "capture_limit")
                    path.reset()
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val ready = collecting && safeBlankPoint(x, y)
                val elapsed = SystemClock.uptimeMillis() - startedAt
                val stroke = if (ready) {
                    (points + PointF(x.toFloat(), y.toFloat())).toList()
                } else emptyList()
                reset()

                if (stroke.size < 6 || elapsed < 140 || elapsed > 8000L) return true
                val dx = stroke.maxOf { it.x } - stroke.minOf { it.x }
                val dy = stroke.maxOf { it.y } - stroke.minOf { it.y }
                if (dx < dp(50) || dy < dp(65)) {
                    Log.d("NEXO_DESKTOP", "capture_not_letter dx=$dx dy=$dy")
                    return true
                }
                Log.d("NEXO_DESKTOP", "capture_up points=${stroke.size} " +
                    "elapsed=${elapsed}ms dx=$dx dy=$dy")
                post {
                    if (!launcher.isDestroyed && isEnabled(launcher) &&
                        launcher.stateManager.state == LauncherState.NORMAL) {
                        NexoGestureStudio.performDesktopStroke(launcher, stroke)
                    }
                }
                return true
            }
            else -> return true
        }
    }
}
