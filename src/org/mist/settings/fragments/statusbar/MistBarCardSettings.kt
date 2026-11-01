/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.mist.settings.fragments.statusbar

import android.app.ActivityManager
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Switch
import androidx.preference.PreferenceGroup
import androidx.preference.Preference
import com.android.internal.util.mist.MistBarSettings
import com.android.settings.R
import org.json.JSONArray
import org.json.JSONObject

class MistBarCardSettings : MistBarPreferenceFragment() {
    private lateinit var card: MistBarSettings.Card
    private var ownerUser = 0
    private var existing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.mist_bar_card)
        requireActivity().title = preferenceScreen.title
        ownerUser = savedInstanceState?.getInt("owner_user") ?: ActivityManager.getCurrentUser()
        existing = arguments?.containsKey("card_id") == true
        val restored = savedInstanceState?.getString("card")?.let {
            MistBarSettings.parse("{\"cards\":[$it]}").cards.firstOrNull()
        }
        val stored = editor.config.cards.firstOrNull { it.id == arguments?.getString("card_id") }
        val type = arguments?.getString("type") ?: "shortcuts"
        card = restored ?: stored ?: MistBarSettings.Card.create(type, label(type))
        if (existing && stored == null) finish()
        refreshPreferences()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("card", card.toString())
        outState.putInt("owner_user", ownerUser)
        super.onSaveInstanceState(outState)
    }

    override fun refreshPreferences() {
        if (!::card.isInitialized) return
        if (ownerUser != ActivityManager.getCurrentUser() ||
            (existing && editor.config.cards.none { it.id == card.id })) {
            finish()
            return
        }
        val parent = requireNotNull(findPreference<PreferenceGroup>("card"))
        parent.removeAll()
        textPreference(parent, "title", "card_name", card.title, 80) { card = card.with("title", it) }
        cardOption(parent, MistBarOption("enabled", default = 1)).title = label("card_enabled")
        cardOption(parent, MistBarOption("lockscreen"))
        cardOption(parent, MistBarOption("mini_enabled", default = 1))
        cardOption(parent, MistBarOption("card_haptics", default = 1))
        when (card.type) {
            "shortcuts" -> {
                val entries = shortcuts()
                for (index in 0 until entries.length()) {
                    val entry = entries.optJSONObject(index) ?: continue
                    action(parent, "shortcut_$index", "delete") {
                        val latest = shortcuts()
                        latest.remove(index)
                        card = card.with("actions", latest.toString())
                        scheduleRefresh()
                    }.apply {
                        title = entry.optString("label")
                        summary = label("delete")
                    }
                }
                action(parent, "add_app") { chooseApp(::addShortcut) }.isEnabled = entries.length() < 24
                action(parent, "add_contact_action") { shortcutDialog() }.isEnabled = entries.length() < 24
                cardOption(parent, MistBarOption("columns", "slider", 4, 2, 6))
                cardOption(parent, MistBarOption("icon_scale", "slider", 100, 70, 150))
                cardOption(parent, MistBarOption("icon_shape", "choice", 2, choices = mistBarShapeNames))
                cardOption(parent, MistBarOption("themed_icons"))
                cardOption(parent, MistBarOption("close_on_tap", default = 1))
            }
            "barcode" -> {
                cardText(parent, "value", "barcode_value")
                cardText(parent, "label", "barcode_label")
                val formats = listOf("QR_CODE", "CODE_128", "CODE_39", "CODE_93", "CODABAR", "EAN_13",
                    "EAN_8", "ITF", "UPC_A", "UPC_E", "AZTEC", "DATA_MATRIX", "PDF_417")
                val preference = template(parent, "choice", "format", "barcode_format")
                bindOption(preference, MistBarOption("format", "choice", choices = formats),
                    formats.indexOf(card.text("format", "QR_CODE")).coerceAtLeast(0)) {
                    card = card.with("format", formats[it]); scheduleRefresh(); true
                }
                cardOption(parent, MistBarOption("rotate"))
                cardOption(parent, MistBarOption("height", "slider", 180, 64, 320))
            }
            "clock" -> {
                cardText(parent, "timezone", "timezone")
                cardOption(parent, MistBarOption("seconds"))
            }
            "timer" -> cardOption(parent, MistBarOption("minutes", "slider", 5, 1, 180))
            "battery" -> {
                cardOption(parent, MistBarOption("fahrenheit"))
                cardOption(parent, MistBarOption("battery_left", "choice", choices = listOf("battery_icon", "percentage", "temperature")))
                cardOption(parent, MistBarOption("battery_right", "choice", 1, choices = listOf("battery_icon", "percentage", "temperature")))
            }
            "widget" -> {
                cardOption(parent, MistBarOption("width", "slider", 360, 64, 480))
                cardOption(parent, MistBarOption("height", "slider", 180, 64, 480))
                cardOption(parent, MistBarOption("widget_theme", "choice", choices = listOf("follow_system", "light", "dark")))
                cardOption(parent, MistBarOption("close_on_tap"))
            }
            "volume", "brightness", "flashlight" -> {
                if (card.type == "volume") cardOption(parent, MistBarOption("stream", "choice",
                    choices = listOf("media_volume", "ring_volume", "notification_volume", "alarm_volume", "call_volume")))
                if (card.type != "flashlight") cardOption(parent, MistBarOption("hide_value"))
                cardOption(parent, MistBarOption("slider_style", "choice", choices = listOf("standard", "expressive", "vertical")))
                cardOption(parent, MistBarOption("toggle_shape", "choice", 2, choices = mistBarShapeNames))
                action(parent, "toggle_action") { chooseToggleAction() }.summary = label(card.text("toggle_action", "none"))
                cardText(parent, "toggle_label", "toggle_label")
                cardOption(parent, MistBarOption("hide_after", "slider", 0, 0, 60))
            }
        }
        buildColors()
        val actions = requireNotNull(findPreference<PreferenceGroup>("actions"))
        actions.removeAll()
        action(actions, "save") { saveCard() }.isEnabled = card.title.isNotBlank() &&
            (card.type != "barcode" || card.text("value", "").isNotBlank())
        action(actions, "cancel") { finish() }
        if (existing) {
            val index = editor.config.cards.indexOfFirst { it.id == card.id }
            action(actions, "move_up") { moveCard(-1) }.isEnabled = index > 0
            action(actions, "move_down") { moveCard(1) }.isEnabled = index < editor.config.cards.lastIndex
            action(actions, "delete") {
                editor.reload()
                if (changed(editor.cards(editor.config.cards.filterNot { it.id == card.id }))) finish()
            }
        }
    }

    private fun cardOption(parent: PreferenceGroup, option: MistBarOption, after: () -> Unit = {}): Preference {
        val value = if (option.kind == "switch") {
            if (card.enabled(option.key, option.default != 0)) 1 else 0
        } else card.integer(option.key, option.default, option.min,
            if (option.choices.isEmpty()) option.max else option.choices.lastIndex)
        return this.option(parent, option, value) {
            card = card.with(option.key, if (option.kind == "switch") it != 0 else it)
            after()
            scheduleRefresh()
            true
        }
    }

    private fun cardText(parent: PreferenceGroup, key: String, title: String) {
        textPreference(parent, key, title, card.text(key, "")) { card = card.with(key, it) }
    }

    private fun buildColors() {
        val parent = requireNotNull(findPreference<PreferenceGroup>("colors"))
        parent.removeAll()
        val parts = when (card.type) {
            "shortcuts" -> listOf("icon" to 2)
            "barcode" -> listOf("barcode" to 6)
            "clock" -> listOf("time" to 5, "date" to 0)
            "timer" -> listOf("time" to 5, "slider" to 5, "track" to 2, "button" to 5)
            "battery" -> listOf("battery" to 5, "details" to 2, "button" to 5)
            "volume", "brightness", "flashlight" -> listOf("slider" to 5, "track" to 2, "toggle" to 2)
            else -> emptyList()
        }
        for (suffix in listOf("day", "night")) {
            val theme = category(parent, suffix, if (suffix == "day") "light" else "dark")
            cardOption(theme, MistBarOption("palette_$suffix", "choice", choices = listOf("inherit") + mistBarPalettes)) {
                card = card.with("custom_colors_$suffix", false)
            }
            for (role in listOf("foreground", "background")) {
                val key = "${role}_$suffix"
                val fallback = if (role == "background") Color.BLACK else Color.WHITE
                color(theme, key, "card_$role", card.integer(key, fallback, Int.MIN_VALUE, Int.MAX_VALUE), fallback) {
                    card = card.with(key, it).with("custom_colors_$suffix", true)
                    scheduleRefresh(); true
                }
            }
            for ((part, fallbackPalette) in parts) {
                val group = category(theme, "${part}_$suffix", part)
                val paletteKey = "${part}_palette_$suffix"
                cardOption(group, MistBarOption(paletteKey, "choice",
                    card.integer("${part}_palette", fallbackPalette, 0, 20), choices = mistBarPalettes)).title = label("palette")
                val roles = if (part in listOf("slider", "track")) listOf("background") else listOf("background", "foreground")
                for (role in roles) {
                    val key = "${part}_${role}_$suffix"
                    val fallback = if (part == "barcode") {
                        if (role == "background") Color.WHITE else Color.BLACK
                    } else paletteColor(fallbackPalette, role == "foreground")
                    color(group, key, role, card.integer(key, fallback, Int.MIN_VALUE, Int.MAX_VALUE), fallback) {
                        card = card.with(key, it).with(paletteKey, 6)
                        scheduleRefresh(); true
                    }
                }
            }
        }
    }

    private fun saveCard() {
        if (ownerUser != ActivityManager.getCurrentUser()) { finish(); return }
        editor.reload()
        val latest = editor.config.cards
        if (existing && latest.none { it.id == card.id }) { finish(); return }
        if (!existing && latest.size >= MistBarSettings.MAX_CARDS) { changed(false); return }
        if (changed(editor.cards(if (existing) latest.map { if (it.id == card.id) card else it } else latest + card))) finish()
    }

    private fun moveCard(direction: Int) {
        editor.reload()
        val latest = editor.config.cards.toMutableList()
        val index = latest.indexOfFirst { it.id == card.id }
        if (index < 0 || index + direction !in latest.indices) return
        java.util.Collections.swap(latest, index, index + direction)
        changed(editor.cards(latest))
    }

    private fun shortcuts(): JSONArray = runCatching { JSONArray(card.text("actions", "[]")) }.getOrDefault(JSONArray())

    private fun addShortcut(label: String, action: String) {
        val entries = shortcuts()
        if (entries.length() >= 24) return
        entries.put(JSONObject().put("label", label).put("action", action))
        card = card.with("actions", entries.toString())
        scheduleRefresh()
    }

    private fun chooseToggleAction() {
        val actions = mistBarGestureActions.filterNot { it in listOf("expand", "next_card", "previous_card", "open_app") }
        choose(label("toggle_action"), (actions + "choose_app").map(::label), actions.indexOf(card.text("toggle_action", "none"))) { index ->
            if (index == actions.size) chooseApp { label, action ->
                card = card.with("toggle_action", action).with("toggle_label", label)
                scheduleRefresh()
            } else {
                card = card.with("toggle_action", actions[index])
                scheduleRefresh()
            }
        }
    }

    private fun shortcutDialog() {
        val view = layoutInflater.inflate(R.layout.mist_bar_shortcut_editor, null)
        val contact = view.findViewById<Switch>(R.id.mist_bar_contact)
        val title = view.findViewById<EditText>(R.id.mist_bar_shortcut_label)
        val value = view.findViewById<EditText>(R.id.mist_bar_shortcut_value)
        contact.setOnCheckedChangeListener { _, checked ->
            value.setHint(if (checked) R.string.mist_bar_phone_number else R.string.mist_bar_intent_uri)
        }
        val dialog = showDialog(AlertDialog.Builder(requireContext())
            .setTitle(R.string.mist_bar_add_contact_action).setView(view)
            .setPositiveButton(R.string.mist_bar_save) { _, _ ->
                addShortcut(title.text.toString(), (if (contact.isChecked) "contact:" else "intent:") + value.text.toString())
            }.setNegativeButton(R.string.mist_bar_cancel, null))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = title.text.isNotBlank() && value.text.isNotBlank()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        title.addTextChangedListener(watcher)
        value.addTextChangedListener(watcher)
    }
}
