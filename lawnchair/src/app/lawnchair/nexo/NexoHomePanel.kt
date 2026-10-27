/*
 * ESCATE NEXO — home dashboard for activity-based application shortcuts.
 * Copyright 2026 ESCATE NEXO contributors
 * Licensed under the Apache License, Version 2.0.
 */
package app.lawnchair.nexo

import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import app.lawnchair.LawnchairLauncher
import com.android.launcher3.R

/**
 * Reversible, on-home overlay. The original Launcher3 database, pages, dock,
 * widgets and app drawer are deliberately left intact.
 */
internal class NexoHomePanel(private val launcher: LawnchairLauncher) {
    private val foreground = Color.WHITE
    private val secondary = Color.rgb(176, 200, 212)
    private val accent = Color.rgb(65, 230, 201)

    val view = LinearLayout(launcher).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(12), dp(12), dp(12))
        background = rounded(Color.rgb(20, 32, 47), 20, Color.rgb(55, 82, 96))
        elevation = dp(8).toFloat()
    }

    fun attach() {
        if (view.parent != null) return
        launcher.dragLayer.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            ).apply {
                topMargin = dp(112)
                leftMargin = dp(16)
                rightMargin = dp(16)
            },
        )
        render()
    }

    fun detach() {
        (view.parent as? android.view.ViewGroup)?.removeView(view)
    }

    fun render() {
        view.removeAllViews()
        val selectedSpace = NexoSpaces.currentSpace(launcher)

        val titleBar = LinearLayout(launcher).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        val title = text(
            launcher.getString(R.string.nexo_home_header, launcher.getString(selectedSpace.nameRes)),
            15f,
            foreground,
            bold = true,
        ).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleBar.addView(title)
        val edit = text(launcher.getString(R.string.nexo_edit_apps), 11f, accent, bold = true)
        edit.setPadding(dp(8), dp(8), dp(4), dp(8))
        edit.setOnClickListener { NexoSpaces.editActiveSpace(launcher) }
        titleBar.addView(edit)
        view.addView(titleBar)

        val scroll = HorizontalScrollView(launcher).apply {
            isHorizontalScrollBarEnabled = false
            fillViewport = false
        }
        val modes = LinearLayout(launcher).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        NexoSpaces.Space.entries.forEach { space ->
            val active = space.id == selectedSpace.id
            val chip = text(
                launcher.getString(space.nameRes),
                12f,
                if (active) Color.rgb(12, 35, 40) else secondary,
                bold = active,
            ).apply {
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = rounded(if (active) accent else Color.rgb(38, 53, 69), 18)
                setOnClickListener { NexoSpaces.activateSpace(launcher, space) }
            }
            modes.addView(
                chip,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { rightMargin = dp(6) },
            )
        }
        scroll.addView(modes)
        view.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8); bottomMargin = dp(8) },
        )

        val selectedIds = NexoSpaceAppStore.selectedIds(launcher, selectedSpace.id)
        val apps = NexoSpaceAppStore.installedLaunchableApps(launcher)
            .filter { it.id in selectedIds }

        if (apps.isEmpty()) {
            val empty = text(launcher.getString(R.string.nexo_home_empty), 12f, secondary)
            empty.setPadding(dp(4), dp(14), dp(4), dp(14))
            empty.setOnClickListener { NexoSpaces.editActiveSpace(launcher) }
            view.addView(empty)
        } else {
            val grid = GridLayout(launcher).apply {
                columnCount = 4
                orientation = GridLayout.HORIZONTAL
            }
            val cellWidth = minOf(dp(80), (launcher.resources.displayMetrics.widthPixels - dp(64)) / 4)
            apps.take(8).forEach { app ->
                val cell = LinearLayout(launcher).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    setPadding(dp(1), dp(6), dp(1), dp(2))
                    setOnClickListener { NexoSpaces.launchApp(launcher, app) }
                    contentDescription = app.label
                }
                val image = ImageView(launcher).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    val icon = try {
                        launcher.packageManager.getActivityIcon(app.component)
                    } catch (_: PackageManager.NameNotFoundException) {
                        launcher.getDrawable(android.R.drawable.sym_def_app_icon)
                    }
                    setImageDrawable(icon)
                }
                cell.addView(image, LinearLayout.LayoutParams(dp(42), dp(42)))
                val caption = text(app.label, 11f, foreground).apply {
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    gravity = Gravity.CENTER
                }
                cell.addView(
                    caption,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { topMargin = dp(4) },
                )
                grid.addView(cell, GridLayout.LayoutParams().apply {
                    width = cellWidth
                    height = dp(78)
                })
            }
            view.addView(grid)
            if (apps.size > 8) {
                val more = text(launcher.getString(R.string.nexo_home_more, apps.size - 8), 11f, accent)
                more.setPadding(dp(5), dp(4), dp(5), dp(4))
                more.setOnClickListener { NexoSpaces.openActiveSpacePanel(launcher) }
                view.addView(more)
            }
        }
    }

    private fun text(value: String, sizeSp: Float, color: Int, bold: Boolean = false): TextView =
        TextView(launcher).apply {
            text = value
            textSize = sizeSp
            setTextColor(color)
            if (bold) setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        }

    private fun rounded(color: Int, radiusDp: Int, stroke: Int? = null) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun dp(value: Int): Int = (value * launcher.resources.displayMetrics.density + 0.5f).toInt()
}
