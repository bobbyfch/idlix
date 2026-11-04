package com.idlix.tv.data.local

import android.content.Context
import com.idlix.tv.IdlixTvApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Local-only saved collection slugs; no account, tracking or remote writes. */
object SavedCollectionsStore {
    private const val PREFS = "idlix_saved_collections"
    private const val KEY = "saved_slugs"
    private val validSlug = Regex("^[a-z0-9-]{1,100}$")
    private val preferences by lazy {
        IdlixTvApp.appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
    private val _saved by lazy {
        MutableStateFlow(preferences.getStringSet(KEY, emptySet())?.toSet().orEmpty())
    }

    val saved: StateFlow<Set<String>> get() = _saved

    @Synchronized
    fun toggle(slug: String) {
        if (!validSlug.matches(slug)) return
        val next = _saved.value.toMutableSet().apply {
            if (!add(slug)) remove(slug)
        }.toSet()
        preferences.edit().putStringSet(KEY, next).apply()
        _saved.value = next
    }
}
