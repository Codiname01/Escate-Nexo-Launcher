/*
 * ESCATE NEXO — locally configured app shortcuts per logical space.
 *
 * Copyright 2026 ESCATE NEXO contributors
 * Licensed under the Apache License, Version 2.0.
 */
package app.lawnchair.nexo

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import app.lawnchair.LawnchairLauncher
import androidx.annotation.StringRes
import com.android.launcher3.Launcher
import com.android.launcher3.R

/**
 * NEXO Spaces v0.1: choose a space, curate its apps, and launch those apps.
 * The underlying Lawnchair/Launcher3 workspace and its widgets are unchanged.
 */
object NexoSpaces {
    private const val PREFS_NAME = "nexo_spaces_v1"
    private const val ACTIVE_SPACE_KEY = "active_space"

    internal enum class Space(
        val id: String,
        @StringRes val nameRes: Int,
    ) {
        WORK("work", R.string.nexo_space_work),
        STUDY("study", R.string.nexo_space_study),
        CREATE("create", R.string.nexo_space_create),
        MUSIC("music", R.string.nexo_space_music),
    }

    internal fun currentSpace(context: Context): Space {
        val id = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(ACTIVE_SPACE_KEY, Space.WORK.id)
        return Space.entries.firstOrNull { it.id == id } ?: Space.WORK
    }

    internal fun activateSpace(launcher: LawnchairLauncher, space: Space) {
        launcher.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(ACTIVE_SPACE_KEY, space.id).apply()
        launcher.refreshNexoHomePanel()
    }

    internal fun editActiveSpace(launcher: LawnchairLauncher) =
        showAppPicker(launcher, currentSpace(launcher))

    internal fun openActiveSpacePanel(launcher: LawnchairLauncher) =
        showSpaceApps(launcher, currentSpace(launcher))

    internal fun launchApp(launcher: Launcher, app: NexoSpaceAppStore.AppEntry) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = app.component
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            launcher.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(launcher, R.string.nexo_app_unavailable, Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(launcher, R.string.nexo_app_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    fun showSelector(launcher: Launcher): Boolean {
        val prefs = launcher.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val spaces = Space.entries
        val currentId = prefs.getString(ACTIVE_SPACE_KEY, Space.WORK.id)
        val checkedIndex = spaces.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        val labels = spaces.map { launcher.getString(it.nameRes) }.toTypedArray()

        AlertDialog.Builder(launcher)
            .setTitle(R.string.nexo_spaces_dialog_title)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, selectedIndex ->
                val space = spaces[selectedIndex]
                prefs.edit().putString(ACTIVE_SPACE_KEY, space.id).apply()
                (launcher as? LawnchairLauncher)?.refreshNexoHomePanel()
                dialog.dismiss()
                showSpaceApps(launcher, space)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        return true
    }

    private fun showSpaceApps(launcher: Launcher, space: Space) {
        val selected = NexoSpaceAppStore.selectedIds(launcher, space.id)
        val apps = NexoSpaceAppStore.installedLaunchableApps(launcher)
            .filter { it.id in selected }

        val title = launcher.getString(
            R.string.nexo_space_apps_title,
            launcher.getString(space.nameRes),
        )
        val builder = AlertDialog.Builder(launcher)
            .setTitle(title)
            .setPositiveButton(R.string.nexo_edit_apps) { _, _ ->
                showAppPicker(launcher, space)
            }
            .setNeutralButton(R.string.nexo_change_space) { _, _ ->
                showSelector(launcher)
            }
            .setNegativeButton(R.string.nexo_close, null)

        if (apps.isEmpty()) {
            builder.setMessage(R.string.nexo_no_apps_in_space)
        } else {
            builder.setItems(apps.map { it.label }.toTypedArray()) { _, which ->
                launchApp(launcher, apps[which])
            }
        }
        builder.show()
    }

    private fun showAppPicker(launcher: Launcher, space: Space) {
        val allApps = NexoSpaceAppStore.installedLaunchableApps(launcher)
        if (allApps.isEmpty()) {
            Toast.makeText(launcher, R.string.nexo_no_installed_apps, Toast.LENGTH_LONG).show()
            showSpaceApps(launcher, space)
            return
        }
        val selectedIds = NexoSpaceAppStore.selectedIds(launcher, space.id)
        val labels = allApps.map { it.label }.toTypedArray()
        val checks = BooleanArray(allApps.size) { index -> allApps[index].id in selectedIds }

        AlertDialog.Builder(launcher)
            .setTitle(R.string.nexo_edit_apps)
            .setMultiChoiceItems(labels, checks) { _, which, isChecked ->
                checks[which] = isChecked
            }
            .setPositiveButton(R.string.nexo_save) { _, _ ->
                val chosen = allApps.indices
                    .filter { checks[it] }
                    .map { allApps[it].id }
                    .toSet()
                NexoSpaceAppStore.saveSelected(launcher, space.id, chosen)
                (launcher as? LawnchairLauncher)?.refreshNexoHomePanel()
                showSpaceApps(launcher, space)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                showSpaceApps(launcher, space)
            }
            .show()
    }
}
