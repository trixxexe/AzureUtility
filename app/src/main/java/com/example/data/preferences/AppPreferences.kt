package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "azure_settings")

class AppPreferences(private val context: Context) {

    companion object {
        val LAST_ACTIVE_TAB = intPreferencesKey("last_active_tab")
        val CODE_EDITOR_FONT_SIZE = intPreferencesKey("code_editor_font_size")
        val CODE_EDITOR_WORD_WRAP = booleanPreferencesKey("code_editor_word_wrap")
        val TEXTPAD_LINE_NUMBERS = booleanPreferencesKey("textpad_line_numbers")
        val MARKITDOWN_SYNC_SCROLL = booleanPreferencesKey("markitdown_sync_scroll")
        val QRFORGE_LAST_TYPE = stringPreferencesKey("qrforge_last_type")
    }

    val lastActiveTab: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[LAST_ACTIVE_TAB] ?: 0
    }

    suspend fun saveLastActiveTab(tab: Int) {
        context.dataStore.edit { preferences ->
            preferences[LAST_ACTIVE_TAB] = tab
        }
    }

    val codeEditorFontSize: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CODE_EDITOR_FONT_SIZE] ?: 13
    }

    suspend fun saveCodeEditorFontSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[CODE_EDITOR_FONT_SIZE] = size
        }
    }

    val codeEditorWordWrap: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[CODE_EDITOR_WORD_WRAP] ?: false
    }

    suspend fun saveCodeEditorWordWrap(wrap: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CODE_EDITOR_WORD_WRAP] = wrap
        }
    }

    val textpadLineNumbers: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[TEXTPAD_LINE_NUMBERS] ?: false
    }

    suspend fun saveTextpadLineNumbers(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TEXTPAD_LINE_NUMBERS] = show
        }
    }

    val markitdownSyncScroll: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MARKITDOWN_SYNC_SCROLL] ?: true
    }

    suspend fun saveMarkitdownSyncScroll(sync: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MARKITDOWN_SYNC_SCROLL] = sync
        }
    }

    val qrforgeLastType: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[QRFORGE_LAST_TYPE] ?: "QR CODE"
    }

    suspend fun saveQrforgeLastType(type: String) {
        context.dataStore.edit { preferences ->
            preferences[QRFORGE_LAST_TYPE] = type
        }
    }
}
