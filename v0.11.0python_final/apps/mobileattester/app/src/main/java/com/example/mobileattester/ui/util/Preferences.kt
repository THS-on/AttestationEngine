package com.example.mobileattester.ui.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import com.example.mobileattester.BuildConfig.DEFAULTHOST
import com.example.mobileattester.BuildConfig.DEFAULTPORT

const val CONFIG = "Config"

class Preferences(
    private val context: Context,
) {
    // to make sure there's only one instance
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(CONFIG)
        private val enginesKey = stringSetPreferencesKey("addresses")
        private val currentEnginesKey = stringPreferencesKey("address")

        val defaultConfig = mutableSetOf("$DEFAULTHOST:$DEFAULTPORT")
    }

    // Access set of saved configs?
    val engines: Flow<SortedSet<String>> by lazy {
        context.dataStore.data.map { preferences ->
            if (preferences[enginesKey] == null || preferences[enginesKey]!!.isEmpty()) {
                defaultConfig.toSortedSet()
            } else {
                preferences[enginesKey]!!.ifEmpty { defaultConfig }.toSortedSet()
            }
        }
    }

    suspend fun saveEngines(engines: SortedSet<String>) {
        context.dataStore.edit { preferences ->
            preferences[enginesKey] = engines
        }
    }

    // Access current engine
    val engine: Flow<String> by lazy {
        context.dataStore.data.map { preferences ->
            if (preferences[currentEnginesKey] == null || preferences[currentEnginesKey]!!.isEmpty()) {
                defaultConfig.first()
            } else {
                preferences[currentEnginesKey]!!
            }
        }
    }

    suspend fun saveEngine(engine: String) {
        context.dataStore.edit { preferences ->
            preferences[currentEnginesKey] = engine
        }
    }
}

