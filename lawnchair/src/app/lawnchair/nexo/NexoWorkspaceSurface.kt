/*
 * ESCATE NEXO — NX10A1 immersive independent desktop and visual folders.
 * Copyright 2026 ESCATE NEXO contributors. Apache-2.0.
 */
package app.lawnchair.nexo

import android.app.AlertDialog
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import app.lawnchair.LawnchairLauncher
import com.android.launcher3.R

/**
 * A separate, opt-in 4-column desktop for each NEXO space. Does not mutate,
 * migrate, reorder, or delete native Launcher3 workspace rows or widgets.
 *
 * NX10A supports independent app icons, folders and positional ordering.
 * Native widgets and drag/drop are explicit later milestones, not faked here.
 */
internal class NexoWorkspaceSurface(private val launcher: LawnchairLauncher) {
    private val foreground = Color.WHITE
    private val secondary = Color.rgb(180, 202, 216)
    private val accent = Color.rgb(65, 230, 201)

    val view = FrameLayout(launcher).apply {
        visibility = View.GONE
        setBackgroundColor(Color.TRANSPARENT)
    }

    private fun dp(value: Int) =
        (value * launcher.resources.displayMetrics.density + .5f).toInt()

    private fun title(value: String, size: Float = 13f, color: Int = foreground): TextView =
        TextView(launcher).apply {
            text = value
            textSize = size
            setTextColor(color)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    fun attach() {
        if (view.parent != null) return
        launcher.dragLayer.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        // Layout is changed after native views have measured and the NEXO
        // floating header's actual bottom is known.
        launcher.dragLayer.post { if (view.parent != null) position() }
    }

    fun detach() {
        (view.parent as? ViewGroup)?.removeView(view)
    }

    private fun position() {
        val frame = view.layoutParams as? FrameLayout.LayoutParams ?: return
        val layerRect = Rect()
        if (!launcher.dragLayer.getGlobalVisibleRect(layerRect)) return
        val header = launcher.nexoPanelScreenRect()
        val headerBottom = if (header != null) header.bottom - layerRect.top else dp(270)
        frame.topMargin = maxOf(dp(115), headerBottom + dp(5))
        // Keep bottom dock, search, and system navigation untouched.
        frame.bottomMargin = dp(166)
        view.layoutParams = frame
    }

    fun render() {
        if (view.parent == null) return
        position()
        view.removeAllViews()
        if (!NexoWorkspaceStore.isEnabled(launcher)) return

        val space = NexoSpaces.currentSpace(launcher)
        val snapshot = NexoWorkspaceStore.read(launcher, space.id)
        val root = LinearLayout(launcher).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(6), dp(10), dp(12))
            background = rounded(Color.argb(112, 15, 30, 46), 14)
        }

        val toolbar = LinearLayout(launcher).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        toolbar.addView(
            title(launcher.getString(R.string.nexo_workspace_title, launcher.getString(space.nameRes)), 13f)
                .apply { setTypeface(Typeface.DEFAULT, Typeface.BOLD) },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        fun tool(label: Int, click: () -> Unit) {
            toolbar.addView(title(launcher.getString(label), 12f, accent).apply {
                setPadding(dp(7), dp(10), dp(7), dp(10))
                isClickable = true
                setOnClickListener { click() }
            })
        }
        tool(R.string.nexo_workspace_add_app) { chooseApp(space.id) }
        tool(R.string.nexo_workspace_add_folder) { newFolder(space.id) }
        root.addView(toolbar)

        if (!snapshot.valid) {
            root.addView(
                title(launcher.getString(R.string.nexo_workspace_invalid), 13f, Color.YELLOW),
            )
        } else if (snapshot.tiles.isEmpty()) {
            root.addView(
                title(launcher.getString(R.string.nexo_workspace_empty), 13f, secondary).apply {
                    setPadding(dp(5), dp(35), dp(5), dp(35))
                    setOnClickListener { chooseApp(space.id) }
                },
            )
        } else {
            val scroll = ScrollView(launcher).apply {
                isVerticalScrollBarEnabled = false
                isFillViewport = true
                clipToPadding = false
            }
            val grid = GridLayout(launcher).apply {
                columnCount = 4
                orientation = GridLayout.HORIZONTAL
                setPadding(0, dp(14), 0, dp(20))
                alignmentMode = GridLayout.ALIGN_BOUNDS
            }
            val widthPx = launcher.resources.displayMetrics.widthPixels
            val itemWidth = maxOf(dp(56), (widthPx - dp(54)) / 4)

            snapshot.tiles.forEachIndexed { index, item ->
                val cell = LinearLayout(launcher).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(dp(3), dp(8), dp(3), dp(10))
                    isClickable = true
                    isLongClickable = true
                    setOnClickListener { open(item, space.id, index) }
                    setOnLongClickListener {
                        editTile(space.id, index)
                        true
                    }
                }
                val preview: View = if (item.kind == "folder") {
                    folderPreview(item, dp(48))
                } else {
                    ImageView(launcher).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageDrawable(appIcon(item.component))
                    }
                }
                cell.addView(preview, LinearLayout.LayoutParams(dp(48), dp(48)))
                cell.addView(title(item.label, 11f).apply {
                    gravity = Gravity.CENTER
                    maxLines = 1
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ))
                grid.addView(cell, GridLayout.LayoutParams().apply {
                    width = itemWidth
                    height = dp(103)
                })
            }
            scroll.addView(grid)
            root.addView(
                scroll,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
                ),
            )
        }
        view.addView(root, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))
    }

    private fun appIcon(componentId: String): Drawable? {
        val component = ComponentName.unflattenFromString(componentId)
        return try {
            if (component != null) launcher.packageManager.getActivityIcon(component)
            else launcher.getDrawable(android.R.drawable.sym_def_app_icon)
        } catch (_: PackageManager.NameNotFoundException) {
            launcher.getDrawable(android.R.drawable.sym_def_app_icon)
        }
    }

    /** Preview the first four children without changing the saved folder JSON. */
    private fun folderPreview(folder: NexoWorkspaceStore.Tile, size: Int): View {
        val frame = FrameLayout(launcher).apply {
            background = rounded(Color.rgb(44, 62, 82), 11)
            setPadding(dp(3), dp(3), dp(3), dp(3))
        }
        if (folder.children.isEmpty()) {
            frame.addView(ImageView(launcher).apply {
                setImageResource(android.R.drawable.ic_menu_agenda)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, FrameLayout.LayoutParams(size - dp(12), size - dp(12), Gravity.CENTER))
            return frame
        }
        val grid = GridLayout(launcher).apply {
            columnCount = 2
            rowCount = 2
        }
        val mini = (size - dp(6)) / 2
        folder.children.take(4).forEach { id ->
            grid.addView(
                ImageView(launcher).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageDrawable(appIcon(id))
                    setPadding(dp(1), dp(1), dp(1), dp(1))
                },
                GridLayout.LayoutParams().apply {
                    width = mini
                    height = mini
                },
            )
        }
        frame.addView(grid, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER,
        ))
        return frame
    }

    private fun save(spaceId: String, items: List<NexoWorkspaceStore.Tile>) {
        if (!NexoWorkspaceStore.save(launcher, spaceId, items)) {
            Toast.makeText(launcher, R.string.nexo_workspace_invalid, Toast.LENGTH_LONG).show()
            return
        }
        launcher.refreshNexoWorkspace()
    }

    private fun chooseApp(spaceId: String) {
        val snapshot = NexoWorkspaceStore.read(launcher, spaceId)
        if (!snapshot.valid) {
            Toast.makeText(launcher, R.string.nexo_workspace_invalid, Toast.LENGTH_LONG).show()
            return
        }
        val installed = NexoSpaceAppStore.installedLaunchableApps(launcher)
        val existing = snapshot.tiles.filter { it.kind == "app" }.map { it.component }.toSet()
        val options = installed.filter { it.id !in existing }
        if (options.isEmpty()) {
            Toast.makeText(launcher, R.string.nexo_workspace_no_apps, Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(launcher)
            .setTitle(R.string.nexo_workspace_add_app)
            .setItems(options.map { it.label }.toTypedArray()) { _, selected ->
                val current = NexoWorkspaceStore.read(launcher, spaceId)
                if (!current.valid) return@setItems
                val choice = options[selected]
                if (current.tiles.none { it.kind == "app" && it.component == choice.id }) {
                    save(spaceId, current.tiles + NexoWorkspaceStore.Tile(
                        kind = "app", label = choice.label.take(100), component = choice.id,
                    ))
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun open(item: NexoWorkspaceStore.Tile, spaceId: String, index: Int) {
        if (item.kind == "folder") {
            val choices = NexoSpaceAppStore.installedLaunchableApps(launcher)
            val items = item.children.mapNotNull { id -> choices.firstOrNull { it.id == id } }
            if (items.isEmpty()) {
                editFolder(spaceId, index)
            } else {
                AlertDialog.Builder(launcher)
                    .setTitle(item.label)
                    .setItems(items.map { it.label }.toTypedArray()) { _, which ->
                        NexoSpaces.launchApp(launcher, items[which])
                    }
                    .setNeutralButton(R.string.nexo_workspace_edit_folder) { _, _ ->
                        editFolder(spaceId, index)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        } else {
            val installed = NexoSpaceAppStore.installedLaunchableApps(launcher)
            val entry = installed.firstOrNull { it.id == item.component }
            if (entry != null) NexoSpaces.launchApp(launcher, entry)
            else Toast.makeText(launcher, R.string.nexo_app_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun editTile(spaceId: String, index: Int) {
        val snapshot = NexoWorkspaceStore.read(launcher, spaceId)
        if (!snapshot.valid || index !in snapshot.tiles.indices) return
        val item = snapshot.tiles[index]
        val choices = listOf(
            launcher.getString(R.string.nexo_workspace_move_left),
            launcher.getString(R.string.nexo_workspace_move_right),
            launcher.getString(R.string.nexo_workspace_remove),
        ) + if (item.kind == "folder") listOf(
            launcher.getString(R.string.nexo_workspace_edit_folder),
        ) else emptyList()

        AlertDialog.Builder(launcher)
            .setTitle(item.label)
            .setItems(choices.toTypedArray()) { _, which ->
                if (which == 3 && item.kind == "folder") {
                    editFolder(spaceId, index)
                    return@setItems
                }
                val current = NexoWorkspaceStore.read(launcher, spaceId)
                if (!current.valid || index !in current.tiles.indices) return@setItems
                val tiles = current.tiles.toMutableList()
                when (which) {
                    0 -> if (index > 0) {
                        val moved = tiles.removeAt(index)
                        tiles.add(index - 1, moved)
                    }
                    1 -> if (index < tiles.lastIndex) {
                        val moved = tiles.removeAt(index)
                        tiles.add(index + 1, moved)
                    }
                    2 -> tiles.removeAt(index)
                }
                save(spaceId, tiles)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun newFolder(spaceId: String) {
        val input = EditText(launcher).apply {
            setSingleLine(true)
            setText(launcher.getString(R.string.nexo_workspace_new_folder))
        }
        AlertDialog.Builder(launcher)
            .setTitle(R.string.nexo_workspace_add_folder)
            .setView(input)
            .setPositiveButton(R.string.nexo_workspace_add_folder) { _, _ ->
                val current = NexoWorkspaceStore.read(launcher, spaceId)
                if (!current.valid) return@setPositiveButton
                val folder = NexoWorkspaceStore.Tile(
                    "folder",
                    input.text.toString().trim().ifBlank {
                        launcher.getString(R.string.nexo_workspace_new_folder)
                    }.take(100),
                )
                save(spaceId, current.tiles + folder)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun editFolder(spaceId: String, index: Int) {
        val current = NexoWorkspaceStore.read(launcher, spaceId)
        if (!current.valid || index !in current.tiles.indices) return
        val folder = current.tiles[index]
        if (folder.kind != "folder") return
        val all = NexoSpaceAppStore.installedLaunchableApps(launcher)
        val selected = folder.children.toMutableSet()
        AlertDialog.Builder(launcher)
            .setTitle(launcher.getString(R.string.nexo_workspace_edit_folder) + " · " + folder.label)
            .setMultiChoiceItems(
                all.map { it.label }.toTypedArray(),
                BooleanArray(all.size) { i -> all[i].id in selected },
            ) { _, which, checked ->
                if (checked) selected.add(all[which].id) else selected.remove(all[which].id)
            }
            .setPositiveButton(R.string.nexo_save) { _, _ ->
                val latest = NexoWorkspaceStore.read(launcher, spaceId)
                if (!latest.valid || index !in latest.tiles.indices ||
                    latest.tiles[index].kind != "folder") return@setPositiveButton
                if (selected.size > 100) {
                    Toast.makeText(
                        launcher,
                        R.string.nexo_workspace_folder_limit,
                        Toast.LENGTH_LONG,
                    ).show()
                    return@setPositiveButton
                }
                val edited = latest.tiles.toMutableList()
                edited[index] = edited[index].copy(children = all.map { it.id }.filter { it in selected })
                save(spaceId, edited)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
