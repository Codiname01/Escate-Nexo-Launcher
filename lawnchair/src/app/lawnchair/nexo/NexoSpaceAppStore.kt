/*
 * ESCATE NEXO — local app lists for logical spaces.
 * Copyright 2026 ESCATE NEXO contributors
 * Licensed under the Apache License, Version 2.0.
 */
package app.lawnchair.nexo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.util.Locale

/**
 * Only manages explicit app shortcuts by component; never edits Launcher3's
 * workspace database. Apps may belong to multiple spaces.
 */
internal object NexoSpaceAppStore {
    private const val PREFS_NAME = "nexo_space_apps_v1"

    data class AppEntry(
        val component: ComponentName,
        val label: String,
    ) {
        val id: String get() = component.flattenToString()
    }

    fun selectedIds(context: Context, spaceId: String): Set<String> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(spaceId, emptySet())
            ?.toSet()
            .orEmpty()

    fun saveSelected(context: Context, spaceId: String, componentIds: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(spaceId, componentIds.toSet())
            .apply()
    }

    @Suppress("DEPRECATION")
    fun installedLaunchableApps(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .filter { it.activityInfo != null && it.activityInfo.packageName != context.packageName }
            .map { activity ->
                val info = activity.activityInfo
                AppEntry(
                    component = ComponentName(info.packageName, info.name),
                    label = activity.loadLabel(pm).toString().ifBlank { info.packageName },
                )
            }
            .distinctBy { it.id }
            .sortedWith(compareBy({ it.label.lowercase(Locale.getDefault()) }, { it.id }))
            .toList()
    }
}
