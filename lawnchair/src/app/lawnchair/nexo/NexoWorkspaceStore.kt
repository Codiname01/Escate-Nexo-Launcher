/*
 * ESCATE NEXO — NX10A independent workspace tile persistence.
 * Copyright 2026 ESCATE NEXO contributors. Apache-2.0.
 */
package app.lawnchair.nexo

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Isolated space-specific shortcuts/folders. Launcher3 Favorites database and
 * existing NEXO space-app preferences are never written by this store.
 *
 * Future native widget tiles can use the reserved kind "widget" in v2.
 */
internal object NexoWorkspaceStore {
    private const val PREFS = "nexo_workspace_layouts_v1"
    private const val MODE_PREFS = "nexo_independent_workspaces_v1"
    private const val MODE_KEY = "enabled"
    private const val MAX_TILES = 24

    data class Tile(
        val kind: String,
        val label: String,
        val component: String = "",
        val children: List<String> = emptyList(),
    ) {
        init {
            require(kind == "app" || kind == "folder") { "Unsupported tile kind" }
        }
    }

    data class Snapshot(val tiles: List<Tile>, val valid: Boolean)

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(MODE_PREFS, Context.MODE_PRIVATE)
            .getBoolean(MODE_KEY, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(MODE_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(MODE_KEY, enabled).apply()
        Log.d("NEXO_WORKSPACE", "independent_enabled=$enabled")
    }

    private fun validSpace(spaceId: String): Boolean =
        NexoSpaces.Space.entries.any { it.id == spaceId }

    fun read(context: Context, spaceId: String): Snapshot {
        if (!validSpace(spaceId)) return Snapshot(emptyList(), false)
        return try {
            val pref = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val raw = pref.getString("layout_$spaceId", null)
                ?: return Snapshot(emptyList(), true)
            val root = JSONObject(raw)
            if (root.getInt("version") != 1) return Snapshot(emptyList(), false)
            val items = root.getJSONArray("tiles")
            if (items.length() > MAX_TILES) return Snapshot(emptyList(), false)
            val parsed = (0 until items.length()).map { i ->
                val node = items.getJSONObject(i)
                val kind = node.getString("kind")
                val label = node.getString("label")
                val component = node.optString("component", "")
                val ids = node.optJSONArray("children") ?: JSONArray()
                val children = (0 until ids.length()).map { ids.getString(it) }
                if (kind !in listOf("app", "folder") || label.length > 100 ||
                    component.length > 400 || children.size > 100) {
                    throw IllegalArgumentException("Invalid NX10A tile")
                }
                Tile(kind, label, component, children)
            }
            Snapshot(parsed, true)
        } catch (error: Exception) {
            Log.e("NEXO_WORKSPACE", "Invalid saved layout for $spaceId; preserved", error)
            Snapshot(emptyList(), false)
        }
    }

    /**
     * Never overwrite corrupt/unknown layouts or unknown spaces; false means
     * nothing was saved. Use commit for durability before updating the UI.
     */
    fun save(context: Context, spaceId: String, tiles: List<Tile>): Boolean {
        if (!validSpace(spaceId) || !read(context, spaceId).valid ||
            tiles.size > MAX_TILES ||
            tiles.any { it.label.length > 100 || it.component.length > 400 ||
                it.children.size > 100 }) return false
        val payload = JSONObject().apply {
            put("version", 1)
            put("tiles", JSONArray().apply {
                tiles.forEach { tile ->
                    put(JSONObject().apply {
                        put("kind", tile.kind)
                        put("label", tile.label)
                        put("component", tile.component)
                        put("children", JSONArray(tile.children))
                    })
                }
            })
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("layout_$spaceId", payload.toString()).commit()
    }

    fun update(context: Context, spaceId: String, transform: (List<Tile>) -> List<Tile>): Boolean {
        val current = read(context, spaceId)
        if (!current.valid) return false
        return save(context, spaceId, transform(current.tiles))
    }
}
