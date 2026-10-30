/*
 * ESCATE NEXO — NX09B Gesture Studio QA controls.
 * Copyright 2026 ESCATE NEXO contributors.
 * Licensed under the Apache License, Version 2.0.
 */
package app.lawnchair.nexo

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import app.lawnchair.LawnchairLauncher
import com.android.launcher3.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Opt-in gesture pad. NX09A deliberately does not intercept Launcher3 global
 * navigation, icon touches or app-drawer scrolling. Its gesture mappings are
 * independent from NX08C icon swipe preferences.
 */
internal object NexoGestureStudio {
    private const val PREFS = "nexo_gesture_studio_v1"
    private const val KEY_PRACTICE = "practice_only"
    private const val KEY_SENSITIVITY = "shape_sensitivity"

    private fun shapeTolerance(context: Context): Float = when (
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_SENSITIVITY, 1)
    ) {
        0 -> 0.065f // Precise; fewer false positives.
        2 -> 0.140f // Relaxed; for difficult touchscreen strokes.
        else -> 0.095f
    }

    private enum class Gesture(val id: String, val titleRes: Int, val fallback: String) {
        PINCH("pinch", R.string.nexo_studio_pinch, "system:select_space"),
        SPREAD("spread", R.string.nexo_studio_spread, "system:favorites"),
        ROTATE("rotate", R.string.nexo_studio_rotate, "system:next_space"),
        DRAW_M("draw_m", R.string.nexo_studio_draw_m, "space:music"),
        DRAW_T("draw_t", R.string.nexo_studio_draw_t, "space:work"),
    }

    private data class Action(val key: String, val label: String)

    private fun getAction(context: Context, gesture: Gesture): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(gesture.id, gesture.fallback) ?: gesture.fallback

    private fun actionOptions(launcher: LawnchairLauncher): List<Action> {
        val builtIn = listOf(
            Action("none", launcher.getString(R.string.nexo_studio_no_action)),
            Action("system:select_space", launcher.getString(R.string.nexo_studio_select_space)),
            Action("system:next_space", launcher.getString(R.string.nexo_studio_next_space)),
            Action("system:favorites", launcher.getString(R.string.nexo_studio_favorites)),
        )
        val spaces = NexoSpaces.Space.entries.map { space ->
            Action("space:" + space.id, launcher.getString(space.nameRes))
        }
        val apps = NexoSpaceAppStore.installedLaunchableApps(launcher).map { app ->
            Action("app:" + app.id, app.label)
        }
        return builtIn + spaces + apps
    }

    fun show(launcher: LawnchairLauncher) {
        val root = LinearLayout(launcher).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(launcher, 18), dp(launcher, 10), dp(launcher, 18), dp(launcher, 10))
        }
        val explanation = text(launcher, launcher.getString(R.string.nexo_studio_description), 13f)
        root.addView(explanation)

        val studioPrefs = launcher.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var practiceOnly = studioPrefs.getBoolean(KEY_PRACTICE, true)
        val practiceSwitch = text(launcher, "", 14f).apply {
            setPadding(0, dp(launcher, 12), 0, dp(launcher, 9))
        }
        fun refreshPractice() {
            practiceSwitch.text = launcher.getString(
                if (practiceOnly) R.string.nexo_studio_practice_on
                else R.string.nexo_studio_practice_off
            )
        }
        refreshPractice()
        practiceSwitch.setOnClickListener {
            practiceOnly = !practiceOnly
            studioPrefs.edit().putBoolean(KEY_PRACTICE, practiceOnly).apply()
            refreshPractice()
        }
        root.addView(practiceSwitch)

        val sensitivityRow = text(launcher, "", 13f).apply {
            setPadding(0, dp(launcher, 4), 0, dp(launcher, 10))
        }
        fun refreshSensitivity() {
            val sensitivity = studioPrefs.getInt(KEY_SENSITIVITY, 1).coerceIn(0, 2)
            sensitivityRow.text = launcher.getString(R.string.nexo_studio_sensitivity) +
                ": " + launcher.resources.getStringArray(R.array.nexo_studio_sensitivity_options)[sensitivity]
        }
        refreshSensitivity()
        sensitivityRow.setOnClickListener {
            val options = launcher.resources.getStringArray(R.array.nexo_studio_sensitivity_options)
            AlertDialog.Builder(launcher)
                .setTitle(R.string.nexo_studio_sensitivity)
                .setSingleChoiceItems(options, studioPrefs.getInt(KEY_SENSITIVITY, 1).coerceIn(0, 2)) { chooser, index ->
                    studioPrefs.edit().putInt(KEY_SENSITIVITY, index).apply()
                    refreshSensitivity()
                    chooser.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        root.addView(sensitivityRow)

        val status = text(launcher, launcher.getString(R.string.nexo_studio_ready), 13f).apply {
            setTextColor(Color.rgb(25, 132, 121))
            setPadding(0, dp(launcher, 8), 0, dp(launcher, 8))
        }
        root.addView(status)

        var dialog: AlertDialog? = null
        val pad = GesturePad(
            launcher,
            recognized = { gesture ->
                val message = launcher.getString(R.string.nexo_studio_detected, launcher.getString(gesture.titleRes))
                status.text = if (practiceOnly) {
                    launcher.getString(R.string.nexo_studio_practice_result, message)
                } else {
                    message
                }
                val action = getAction(launcher, gesture)
                Log.d("NEXO_STUDIO", "gesture=" + gesture.id + " practice=" + practiceOnly + " action=" + action)
                if (!practiceOnly && action != "none") {
                    dialog?.dismiss()
                    perform(launcher, action)
                }
            },
            rejected = { reason ->
                status.text = launcher.getString(R.string.nexo_studio_no_match)
                Log.d("NEXO_STUDIO", "unrecognized=" + reason)
            },
            threshold = { shapeTolerance(launcher) },
        ).apply {
            contentDescription = launcher.getString(R.string.nexo_studio_canvas_hint)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.rgb(20, 32, 47))
                cornerRadius = dp(launcher, 14).toFloat()
            }
        }
        root.addView(pad, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(launcher, 240)
        ))

        val header = text(launcher, launcher.getString(R.string.nexo_studio_assignments), 14f)
        header.setPadding(0, dp(launcher, 13), 0, dp(launcher, 4))
        root.addView(header)

        Gesture.entries.forEach { gesture ->
            val row = text(launcher, "", 14f)
            fun refresh() {
                val options = actionOptions(launcher)
                val action = getAction(launcher, gesture)
                val display = options.firstOrNull { it.key == action }?.label
                    ?: launcher.getString(R.string.nexo_studio_missing_app)
                row.text = launcher.getString(gesture.titleRes) + "  →  " + display
            }
            refresh()
            row.setPadding(dp(launcher, 8), dp(launcher, 10), dp(launcher, 8), dp(launcher, 10))
            row.isClickable = true
            row.setOnClickListener {
                val options = actionOptions(launcher)
                val current = getAction(launcher, gesture)
                val checked = options.indexOfFirst { it.key == current }
                AlertDialog.Builder(launcher)
                    .setTitle(launcher.getString(gesture.titleRes))
                    .setSingleChoiceItems(options.map { it.label }.toTypedArray(), checked) { picker, which ->
                        launcher.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .edit().putString(gesture.id, options[which].key).apply()
                        picker.dismiss()
                        refresh()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            root.addView(row)
        }

        val scroll = ScrollView(launcher).apply {
            addView(root)
        }
        dialog = AlertDialog.Builder(launcher)
            .setTitle(R.string.nexo_studio_title)
            .setView(scroll)
            .setNegativeButton(R.string.nexo_close, null)
            .show()
    }

    private fun perform(launcher: LawnchairLauncher, action: String) {
        when {
            action == "system:select_space" -> NexoSpaces.showSelector(launcher)
            action == "system:next_space" -> {
                val entries = NexoSpaces.Space.entries
                val index = entries.indexOf(NexoSpaces.currentSpace(launcher))
                NexoSpaces.activateSpace(launcher, entries[(index + 1) % entries.size])
            }
            action == "system:favorites" -> NexoSpaces.openActiveSpacePanel(launcher)
            action.startsWith("space:") -> {
                val id = action.removePrefix("space:")
                val space = NexoSpaces.Space.entries.firstOrNull { it.id == id }
                if (space != null) NexoSpaces.activateSpace(launcher, space)
            }
            action.startsWith("app:") -> {
                val id = action.removePrefix("app:")
                val app = NexoSpaceAppStore.installedLaunchableApps(launcher)
                    .firstOrNull { it.id == id }
                if (app == null) {
                    Toast.makeText(launcher, R.string.nexo_studio_missing_app, Toast.LENGTH_SHORT).show()
                } else {
                    NexoSpaces.launchApp(launcher, app)
                }
            }
        }
    }

    private fun text(context: Context, value: String, sp: Float) = TextView(context).apply {
        text = value
        textSize = sp
    }

    private fun dp(context: Context, value: Int) =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()

    /**
     * Traces one continuous stroke for M / T, or two pointers for
     * pinch / spread / rotation. This is an exploratory pad, not an
     * accessibility gesture detector or system-level listener.
     */
    private class GesturePad(
        context: Context,
        private val recognized: (Gesture) -> Unit,
        private val rejected: (String) -> Unit,
        private val threshold: () -> Float,
    ) : View(context) {
        private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(65, 230, 201)
            strokeWidth = dp(context, 4).toFloat()
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val path = Path()
        private val points = mutableListOf<PointF>()
        private var multi = false
        private var pairActive = false
        private var firstDistance = 0f
        private var firstAngle = 0f

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawPath(path, ink)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    points.clear()
                    path.reset()
                    multi = false
                    pairActive = false
                    path.moveTo(event.x, event.y)
                    points.add(PointF(event.x, event.y))
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount >= 2) {
                        multi = true
                        pairActive = true
                        firstDistance = distance(event)
                        firstAngle = angle(event)
                        points.clear()
                        path.reset()
                        invalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (multi) return true
                    val x = event.x
                    val y = event.y
                    if (points.isEmpty() || hypot(x - points.last().x, y - points.last().y) >= 3f) {
                        points.add(PointF(x, y))
                        path.lineTo(x, y)
                        invalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    if (pairActive && event.pointerCount >= 2) {
                        val lastDistance = distance(event)
                        val angleDelta = signedAngle(angle(event) - firstAngle)
                        val scale = if (firstDistance > 0.01f) lastDistance / firstDistance else 1f
                        val gesture = when {
                            abs(angleDelta) >= 42f && scale in 0.78f..1.28f -> Gesture.ROTATE
                            scale <= 0.72f -> Gesture.PINCH
                            scale >= 1.38f -> Gesture.SPREAD
                            else -> null
                        }
                        if (gesture != null) recognized(gesture)
                        else rejected("multi scale=$scale angle=$angleDelta")
                    }
                    pairActive = false
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!multi) {
                        val detected = recognizeStroke(points)
                        if (detected != null) recognized(detected)
                        else rejected("single points=" + points.size)
                    }
                    parent?.requestDisallowInterceptTouchEvent(false)
                    path.reset()
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    path.reset()
                    points.clear()
                    pairActive = false
                    return true
                }
            }
            return true
        }

        private fun distance(event: MotionEvent): Float = hypot(
            event.getX(1) - event.getX(0),
            event.getY(1) - event.getY(0),
        )

        private fun angle(event: MotionEvent): Float = Math.toDegrees(atan2(
            event.getY(1) - event.getY(0),
            event.getX(1) - event.getX(0),
        ).toDouble()).toFloat()

        private fun signedAngle(angle: Float): Float {
            var adjusted = angle
            while (adjusted > 180f) adjusted -= 360f
            while (adjusted < -180f) adjusted += 360f
            return adjusted
        }

        private fun recognizeStroke(input: List<PointF>): Gesture? {
            if (input.size < 6) return null
            val rangeX = input.maxOf { it.x } - input.minOf { it.x }
            val rangeY = input.maxOf { it.y } - input.minOf { it.y }
            if (max(rangeX, rangeY) < dp(context, 75)) return null

            // One continuous pen path. T: left-to-right, return to centre,
            // then down. M: bottom-left, top-left, trough, peak, bottom-right.
            val m = listOf(
                PointF(0f, 1f), PointF(0f, 0f), PointF(0.45f, 0.85f),
                PointF(0.7f, 0.05f), PointF(1f, 1f),
            )
            val t = listOf(
                PointF(0f, 0f), PointF(1f, 0f),
                PointF(0.5f, 0f), PointF(0.5f, 1f),
            )
            val actual = normalize(resample(input, 48))
            val scoreM = shapeError(actual, normalize(resample(m, 48)))
            val scoreT = shapeError(actual, normalize(resample(t, 48)))
            val result = min(scoreM, scoreT)
            Log.d("NEXO_STUDIO", "stroke M=" + scoreM + " T=" + scoreT)
            return when {
                result > threshold() -> null
                scoreM < scoreT -> Gesture.DRAW_M
                else -> Gesture.DRAW_T
            }
        }

        private fun shapeError(a: List<PointF>, b: List<PointF>): Float =
            a.indices.map { i ->
                val dx = a[i].x - b[i].x
                val dy = a[i].y - b[i].y
                dx * dx + dy * dy
            }.average().toFloat()

        private fun normalize(points: List<PointF>): List<PointF> {
            val minX = points.minOf { it.x }
            val minY = points.minOf { it.y }
            val dx = max(1f, points.maxOf { it.x } - minX)
            val dy = max(1f, points.maxOf { it.y } - minY)
            return points.map { PointF((it.x - minX) / dx, (it.y - minY) / dy) }
        }

        private fun resample(input: List<PointF>, count: Int): List<PointF> {
            if (input.size == 1) return List(count) { input[0] }
            val cumulative = MutableList(input.size) { 0f }
            for (i in 1 until input.size) {
                cumulative[i] = cumulative[i - 1] + hypot(
                    input[i].x - input[i - 1].x,
                    input[i].y - input[i - 1].y,
                )
            }
            val total = cumulative.last()
            if (total < 0.001f) return List(count) { input.first() }
            return (0 until count).map { step ->
                val target = total * step / (count - 1)
                var index = 1
                while (index < cumulative.lastIndex && cumulative[index] < target) index++
                val previous = cumulative[index - 1]
                val segment = max(0.0001f, cumulative[index] - previous)
                val fraction = ((target - previous) / segment).coerceIn(0f, 1f)
                PointF(
                    input[index - 1].x + (input[index].x - input[index - 1].x) * fraction,
                    input[index - 1].y + (input[index].y - input[index - 1].y) * fraction,
                )
            }
        }
    }
}
