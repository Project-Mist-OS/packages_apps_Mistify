/*
 * SPDX-FileCopyrightText: MistOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.mist.settings.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.os.Environment
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import com.android.settings.R
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Stores the icon shown inside switches while they are ON.
 *
 * The chosen icon is written as a small PNG to /sdcard/MistOS/SwitchThumbIcon and its path is kept in
 * secure settings together with an enabled flag. SettingsLib (SwitchThumbIconDrawable) watches those
 * settings in every process, so the icon survives a reboot or a SystemUI restart and updates live.
 * OFF switches are never affected: with no icon selected the stock check mark is used.
 *
 * All functions that touch disk or settings must be called off the main thread.
 */
object SwitchThumbIconManager {

    private const val TAG = "SwitchThumbIconManager"

    // The first two are read by SettingsLib, keep them in sync with SwitchThumbIconStore.
    const val SETTING_ENABLED = "mist_switch_thumb_icon_enabled"
    const val SETTING_PATH = "mist_switch_thumb_icon_path"
    private const val SETTING_CHOICE = "mist_switch_thumb_icon_choice"

    const val CHOICE_DEFAULT = "default"
    const val CHOICE_CUSTOM = "custom"

    private const val ICON_DIR = "MistOS/SwitchThumbIcon"
    private const val CUSTOM_FILE = "custom.png"
    private const val ACTIVE_PREFIX = "active_"
    private const val ICON_SIZE_PX = 128

    enum class Preset(val key: String, @StringRes val label: Int, @DrawableRes val drawable: Int) {
        BAT("bat", R.string.switch_icon_bat, R.drawable.switch_icon_bat),
        SHIELD("shield", R.string.switch_icon_shield, R.drawable.switch_icon_shield),
        POKEBALL("pokeball", R.string.switch_icon_pokeball, R.drawable.switch_icon_pokeball),
        SPIDER("spider", R.string.switch_icon_spider, R.drawable.switch_icon_spider),
        SHARINGAN("sharingan", R.string.switch_icon_sharingan, R.drawable.switch_icon_sharingan);

        companion object {
            fun fromKey(key: String?): Preset? = entries.firstOrNull { it.key == key }
        }
    }

    /** The saved choice: [CHOICE_DEFAULT], [CHOICE_CUSTOM] or a [Preset] key. */
    fun getChoice(context: Context): String {
        val cr = context.contentResolver
        if (Settings.Secure.getIntForUser(cr, SETTING_ENABLED, 0, UserHandle.USER_CURRENT) == 0) {
            return CHOICE_DEFAULT
        }
        val choice = Settings.Secure.getStringForUser(cr, SETTING_CHOICE, UserHandle.USER_CURRENT)
        return if (choice == CHOICE_CUSTOM || Preset.fromKey(choice) != null) choice else CHOICE_DEFAULT
    }

    fun getChoiceLabel(context: Context): String {
        val choice = getChoice(context)
        return when {
            choice == CHOICE_CUSTOM -> context.getString(R.string.switch_icon_custom)
            Preset.fromKey(choice) != null ->
                context.getString(Preset.fromKey(choice)!!.label)
            else -> context.getString(R.string.switch_icon_default)
        }
    }

    @WorkerThread
    fun hasCustom(): Boolean = File(iconDir(), CUSTOM_FILE).isFile

    @WorkerThread
    fun loadCustom(): Bitmap? =
        try {
            BitmapFactory.decodeFile(File(iconDir(), CUSTOM_FILE).path)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read custom switch icon", e)
            null
        }

    /** Decodes a bundled icon at roughly [sizePx], without loading the full source image. */
    @WorkerThread
    fun loadPreset(context: Context, preset: Preset, sizePx: Int = ICON_SIZE_PX): Bitmap? =
        try {
            val source = ImageDecoder.createSource(context.resources, preset.drawable)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = max(info.size.width, info.size.height)
                if (longest > sizePx) {
                    val ratio = sizePx.toFloat() / longest
                    decoder.setTargetSize(
                        (info.size.width * ratio).toInt().coerceAtLeast(1),
                        (info.size.height * ratio).toInt().coerceAtLeast(1),
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to decode preset ${preset.key}", e)
            null
        }

    /** Back to the stock icon. Keeps the uploaded custom icon so it can be picked again. */
    @WorkerThread
    fun applyDefault(context: Context) {
        val cr = context.contentResolver
        Settings.Secure.putIntForUser(cr, SETTING_ENABLED, 0, UserHandle.USER_CURRENT)
        Settings.Secure.putStringForUser(cr, SETTING_PATH, "", UserHandle.USER_CURRENT)
        Settings.Secure.putStringForUser(cr, SETTING_CHOICE, CHOICE_DEFAULT, UserHandle.USER_CURRENT)
        deleteActiveCopies(keep = null)
    }

    @WorkerThread
    fun applyPreset(context: Context, preset: Preset): Boolean {
        val bitmap = loadPreset(context, preset) ?: return false
        return activate(context, bitmap, preset.key)
    }

    @WorkerThread
    fun applyCustom(context: Context): Boolean {
        val bitmap = loadCustom() ?: return false
        return activate(context, bitmap, CHOICE_CUSTOM)
    }

    /** Saves a user supplied image as the custom icon and makes it the active one. */
    @WorkerThread
    fun saveCustomAndApply(context: Context, source: Bitmap): Boolean {
        val icon = toSquareIcon(source)
        val dir = iconDir()
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "Failed to create ${dir.absolutePath}")
            return false
        }
        if (!writePng(icon, File(dir, CUSTOM_FILE))) return false
        return activate(context, icon, CHOICE_CUSTOM)
    }

    private fun activate(context: Context, icon: Bitmap, choice: String): Boolean {
        val dir = iconDir()
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "Failed to create ${dir.absolutePath}")
            return false
        }
        // A new file name every time, so the settings change is seen even for the same choice.
        val target = File(dir, "$ACTIVE_PREFIX${System.currentTimeMillis()}.png")
        if (!writePng(toSquareIcon(icon), target)) return false

        // Order matters: only enable once the path points at a fully written file.
        val cr = context.contentResolver
        Settings.Secure.putStringForUser(cr, SETTING_CHOICE, choice, UserHandle.USER_CURRENT)
        Settings.Secure.putStringForUser(cr, SETTING_PATH, target.absolutePath, UserHandle.USER_CURRENT)
        Settings.Secure.putIntForUser(cr, SETTING_ENABLED, 1, UserHandle.USER_CURRENT)
        deleteActiveCopies(keep = target)
        return true
    }

    /** Center crops to a square and scales to [ICON_SIZE_PX], keeping transparency. */
    private fun toSquareIcon(source: Bitmap): Bitmap {
        val side = min(source.width, source.height)
        val srcRect = RectF(
            (source.width - side) / 2f,
            (source.height - side) / 2f,
            (source.width + side) / 2f,
            (source.height + side) / 2f,
        )
        val out = Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        Canvas(out).drawBitmap(
            source,
            android.graphics.Rect(
                srcRect.left.toInt(), srcRect.top.toInt(), srcRect.right.toInt(), srcRect.bottom.toInt(),
            ),
            RectF(0f, 0f, ICON_SIZE_PX.toFloat(), ICON_SIZE_PX.toFloat()),
            paint,
        )
        return out
    }

    // Written to a temporary name first so a half written file is never picked up.
    private fun writePng(bitmap: Bitmap, target: File): Boolean {
        val temp = File(target.parentFile, "${target.name}.tmp")
        return try {
            val written = temp.outputStream().use { os ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os).also { os.flush() }
            }
            if (!written || !temp.renameTo(target)) {
                temp.delete()
                false
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save switch icon", e)
            temp.delete()
            false
        }
    }

    private fun deleteActiveCopies(keep: File?) {
        val files = iconDir().listFiles { _, name -> name.startsWith(ACTIVE_PREFIX) } ?: return
        for (file in files) {
            if (file != keep && !file.delete()) {
                Log.w(TAG, "Failed to delete old switch icon ${file.name}")
            }
        }
    }

    private fun iconDir() = File(Environment.getExternalStorageDirectory(), ICON_DIR)
}
