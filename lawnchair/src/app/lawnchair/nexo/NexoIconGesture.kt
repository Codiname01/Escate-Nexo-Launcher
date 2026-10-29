/*
 * ESCATE NEXO — swipe-up action for app icons in the NEXO panel.
 * Copyright 2026 ESCATE NEXO contributors
 * Licensed under the Apache License, Version 2.0.
 */
package app.lawnchair.nexo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.ComponentName
import android.util.Log
import app.lawnchair.launcherNullable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Toast
import app.lawnchair.LawnchairLauncher
import com.android.launcher3.R
import kotlin.math.abs

/**
 * NEXO panel only. Tap opens the app, long press assigns a second app,
 * and swipe-up opens the assigned app. No new permissions or services.
 */
object NexoIconGesture {
    private const val PREFS_NAME = "nexo_icon_gestures_v1"

    private fun key(spaceId: String, componentId: String) =
        spaceId + "|" + componentId

    private fun key(spaceId: String, source: NexoSpaceAppStore.AppEntry) =
        key(spaceId, source.id)

    /**
     * Shared resolver for panel, workspace and dock icon swipes. Read the
     * active space for every lookup, so gestures switch immediately.
     * Native Lawnchair gestures retain precedence at the call site.
     */
    @JvmStatic
    fun hasMappedSwipeUp(context: Context, sourceComponent: ComponentName): Boolean {
        val id = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key(NexoSpaces.currentSpace(context).id, sourceComponent.flattenToString()), null)
        return !id.isNullOrBlank()
    }

    /**
     * Java BubbleTextView entrypoint. Also supports the icon views inside
     * ALL_APPS. No global overlay and no system accessibility privileges.
     */
    @JvmStatic
    fun launchMappedSwipeUpFromIcon(context: Context, sourceComponent: ComponentName): Boolean {
        val launcher = context.launcherNullable ?: run {
            Log.w("NEXO_GESTURE", "Cannot resolve launcher context")
            return false
        }
        return launchMappedSwipeUp(launcher, sourceComponent)
    }

    /** Returns true when a NEXO mapping existed (even if its app was removed). */
    fun launchMappedSwipeUp(launcher: LawnchairLauncher, sourceComponent: ComponentName): Boolean {
        val id = launcher.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key(NexoSpaces.currentSpace(launcher).id, sourceComponent.flattenToString()), null)
            ?: return false
        val app = NexoSpaceAppStore.installedLaunchableApps(launcher)
            .firstOrNull { it.id == id }
        Log.d("NEXO_GESTURE", "dispatch space=" + NexoSpaces.currentSpace(launcher).id +
            " source=" + sourceComponent.flattenToShortString() +
            " target=" + id + " found=" + (app != null))
        if (app == null) {
            Toast.makeText(launcher, R.string.nexo_app_unavailable, Toast.LENGTH_SHORT).show()
        } else {
            NexoSpaces.launchApp(launcher, app)
        }
        return true
    }

    private fun targetId(
        context: Context,
        spaceId: String,
        source: NexoSpaceAppStore.AppEntry,
    ): String? = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(key(spaceId, source), null)

    fun targetLabel(
        context: Context,
        spaceId: String,
        source: NexoSpaceAppStore.AppEntry,
    ): String? {
        val id = targetId(context, spaceId, source) ?: return null
        return NexoSpaceAppStore.installedLaunchableApps(context)
            .firstOrNull { it.id == id }
            ?.label
    }

    /**
     * Visible configuration fallback when long-press is unavailable (mouse,
     * accessibility, or launcher interception). Chooses a source icon first.
     */
    fun showActiveSpacePicker(launcher: LawnchairLauncher) {
        val space = NexoSpaces.currentSpace(launcher)
        // Include all installed launcher icons, even when the space panel is
        // empty: users can now configure apps on HOME and in the dock.
        val apps = NexoSpaceAppStore.installedLaunchableApps(launcher)
        if (apps.isEmpty()) {
            Toast.makeText(launcher, R.string.nexo_no_installed_apps, Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(launcher)
            .setTitle(R.string.nexo_gesture_choose_icon)
            .setItems(apps.map { it.label }.toTypedArray()) { _, which ->
                showPicker(launcher, space.id, apps[which])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun bind(
        view: View,
        launcher: LawnchairLauncher,
        spaceId: String,
        source: NexoSpaceAppStore.AppEntry,
    ) {
        val slop = ViewConfiguration.get(launcher).scaledTouchSlop.toFloat()
        val threshold = maxOf(slop * 2, dp(launcher, 32).toFloat())
        var startX = 0f
        var startY = 0f
        var longPressed = false

        view.isClickable = true
        view.setOnClickListener { NexoSpaces.launchApp(launcher, source) }
        view.setOnLongClickListener {
            showPicker(launcher, spaceId, source)
            true
        }

        // GestureDetector provides platform long-press timing.
        val detector = GestureDetector(
            launcher,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true
                override fun onLongPress(e: MotionEvent) {
                    longPressed = true
                    view.performLongClick()
                }
            },
        )

        view.setOnTouchListener { target, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                startX = event.x
                startY = event.y
                longPressed = false
                // Launcher3's drag layer can otherwise steal MOVE events,
                // cancelling swipes before this icon receives ACTION_UP.
                target.parent?.requestDisallowInterceptTouchEvent(true)
            }
            detector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> true
                MotionEvent.ACTION_UP -> {
                    target.parent?.requestDisallowInterceptTouchEvent(false)
                    val dx = event.x - startX
                    val dy = event.y - startY
                    if (!longPressed) {
                        if (-dy >= threshold && -dy > 1.3f * abs(dx)) {
                            launchConfigured(launcher, spaceId, source)
                        } else if (abs(dx) <= slop && abs(dy) <= slop) {
                            target.performClick()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    target.parent?.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> true
            }
        }
    }

    private fun launchConfigured(
        launcher: LawnchairLauncher,
        spaceId: String,
        source: NexoSpaceAppStore.AppEntry,
    ) {
        val id = targetId(launcher, spaceId, source)
        val target = id?.let { saved ->
            NexoSpaceAppStore.installedLaunchableApps(launcher)
                .firstOrNull { it.id == saved }
        }
        if (target == null) {
            Toast.makeText(launcher, R.string.nexo_gesture_unassigned, Toast.LENGTH_SHORT).show()
            return
        }
        NexoSpaces.launchApp(launcher, target)
    }

    private fun showPicker(
        launcher: LawnchairLauncher,
        spaceId: String,
        source: NexoSpaceAppStore.AppEntry,
    ) {
        val apps = NexoSpaceAppStore.installedLaunchableApps(launcher)
        val entries = arrayOf(launcher.getString(R.string.nexo_gesture_none)) +
            apps.map { it.label }.toTypedArray()
        val saved = targetId(launcher, spaceId, source)
        val selected = apps.indexOfFirst { it.id == saved }.let { index ->
            if (index < 0) 0 else index + 1
        }

        AlertDialog.Builder(launcher)
            .setTitle(launcher.getString(R.string.nexo_gesture_title, source.label))
            .setSingleChoiceItems(entries, selected) { dialog, index ->
                val chosen = if (index == 0) null else apps[index - 1].id
                launcher.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString(key(spaceId, source), chosen).apply()
                launcher.refreshNexoHomePanel()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()
}
