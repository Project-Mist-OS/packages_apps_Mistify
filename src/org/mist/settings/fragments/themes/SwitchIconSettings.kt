/*
 * SPDX-FileCopyrightText: MistOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.mist.settings.fragments.themes

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import org.mist.settings.fragments.quicksettings.QsImageCropDialog
import org.mist.settings.preferences.SwitchThumbIconPickerPreference
import org.mist.settings.utils.SwitchThumbIconManager

/** Picks the icon shown inside switches while they are ON, system wide. */
class SwitchIconSettings : SettingsPreferenceFragment() {

    private var picker: SwitchThumbIconPickerPreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.mist_settings_switch_icon)

        picker = findPreference<SwitchThumbIconPickerPreference>(KEY_PICKER)?.also {
            it.onUploadRequested = ::pickImage
        }
    }

    private fun pickImage() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/webp"))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_PICK_IMAGE)
        } catch (e: Exception) {
            Toast.makeText(context, R.string.ax_qs_image_fill_needs_gallery, Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (requestCode != REQUEST_PICK_IMAGE || resultCode != Activity.RESULT_OK || uri == null) {
            return
        }
        val activity = activity ?: return

        // The icon is circular, so start with a square crop.
        QsImageCropDialog.show(activity, uri, QsImageCropDialog.INITIAL_ASPECT_SQUARE,
            object : QsImageCropDialog.Callback {
                override fun onCropped(bitmap: Bitmap) = saveCustomIcon(bitmap)

                override fun onFailed() {
                    if (isAdded) {
                        Toast.makeText(context, R.string.switch_icon_failed, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    private fun saveCustomIcon(bitmap: Bitmap) {
        val appContext = requireContext().applicationContext
        Thread {
            val saved = SwitchThumbIconManager.saveCustomAndApply(appContext, bitmap)
            bitmap.recycle()
            activity?.runOnUiThread {
                Toast.makeText(
                    appContext,
                    if (saved) R.string.switch_icon_custom_set else R.string.switch_icon_failed,
                    Toast.LENGTH_SHORT,
                ).show()
                if (isAdded) picker?.refresh()
            }
        }.start()
    }

    override fun getMetricsCategory(): Int = MetricsEvent.MIST

    private companion object {
        const val KEY_PICKER = "switch_icon_picker"
        const val REQUEST_PICK_IMAGE = 20002
    }
}
