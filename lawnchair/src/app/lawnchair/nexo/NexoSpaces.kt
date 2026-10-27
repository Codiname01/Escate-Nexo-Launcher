/*
 * ESCATE NEXO — experimental space selector.
 *
 * The selected logical space is persisted locally. Workspace isolation,
 * icon filtering and per-space layouts are deliberately NOT implemented here.
 *
 * Copyright 2026 ESCATE NEXO contributors
 * Licensed under the Apache License, Version 2.0.
 */
package app.lawnchair.nexo

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import com.android.launcher3.Launcher
import com.android.launcher3.R

/**
 * Small, reversible integration point for NEXO Spaces v0.1.
 *
 * This release only remembers the user's selected mode. It does not
 * move apps, hide apps, change the launcher database or replace pages.
 */
object NexoSpaces {
    private const val PREFS_NAME = "nexo_spaces_v1"
    private const val ACTIVE_SPACE_KEY = "active_space"

    private enum class Space(
        val id: String,
        @StringRes val nameRes: Int,
    ) {
        WORK("work", R.string.nexo_space_work),
        STUDY("study", R.string.nexo_space_study),
        CREATE("create", R.string.nexo_space_create),
        MUSIC("music", R.string.nexo_space_music),
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
                prefs.edit().putString(ACTIVE_SPACE_KEY, spaces[selectedIndex].id).apply()
                Toast.makeText(
                    launcher,
                    launcher.getString(R.string.nexo_space_selected, labels[selectedIndex]),
                    Toast.LENGTH_SHORT,
                ).show()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        return true
    }
}
