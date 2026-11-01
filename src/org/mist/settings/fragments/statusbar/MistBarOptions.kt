/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mist.settings.fragments.statusbar

internal data class MistBarOption(
    val key: String,
    val kind: String = "switch",
    val default: Int = 0,
    val min: Int = 0,
    val max: Int = 100,
    val choices: List<String> = emptyList(),
    val system: Boolean = false,
    val values: List<Int> = emptyList(),
)

internal val mistBarPalettes =
    listOf(
        "surface",
        "primary_container",
        "secondary_container",
        "tertiary_container",
        "black",
        "primary",
        "custom",
        "surface_light",
        "surface_dark",
        "primary_container_light",
        "primary_container_dark",
        "secondary_container_light",
        "secondary_container_dark",
        "tertiary_container_light",
        "tertiary_container_dark",
        "primary_light",
        "primary_dark",
        "tertiary",
        "tertiary_light",
        "tertiary_dark",
        "tonal",
    )

internal val mistBarShapeNames =
    listOf(
        "none",
        "system",
        "circle",
        "star",
        "rounded_corner",
        "square",
        "hexagon",
        "four_corner",
        "rhombus",
        "triangle",
        "triangle_round",
        "rectangle",
        "arrow",
        "sunny",
        "very_sunny",
        "diamond",
        "pentagon",
        "semicircle",
        "arch",
        "ghost",
        "pill",
        "burst",
        "soft_burst",
        "clover8",
        "clover4",
        "flower",
        "clamshell",
        "cookie",
        "fan",
        "pixel_circle",
        "bun",
        "heart",
    )

internal val mistBarGestureActions =
    listOf(
        "none",
        "expand",
        "open_app",
        "next_card",
        "previous_card",
        "play_pause",
        "next_track",
        "previous_track",
        "flashlight",
        "notifications",
        "quick_settings",
        "home",
        "back",
        "recents",
        "lock",
        "screenshot",
        "settings",
        "brightness_settings",
        "volume_settings",
        "wifi_settings",
        "bluetooth_settings",
        "internet_settings",
        "location_settings",
        "mute",
        "auto_brightness",
    )

internal val mistBarAppearanceOptions =
    listOf(
        MistBarOption("theme", "choice", choices = listOf("follow_system", "light", "dark")),
        MistBarOption("mini_blur"),
        MistBarOption("capsule_blur"),
        MistBarOption("mini_shape", "choice", 2, choices = mistBarShapeNames),
        MistBarOption("card_icon_shape", "choice", 2, choices = mistBarShapeNames),
        MistBarOption("card_button_shape", "choice", 20, choices = mistBarShapeNames),
        MistBarOption("mini_alignment", "choice", choices = listOf("center", "left", "right")),
        MistBarOption("mini_scale", "slider", 100, 70, 200),
        MistBarOption(
            "mini_scale_alignment",
            "choice",
            choices = listOf("center", "top", "bottom"),
        ),
        MistBarOption(
            "status_bar_dynamic_island_width_scale",
            "slider",
            100,
            70,
            140,
            system = true,
        ),
        MistBarOption(
            "status_bar_dynamic_island_height_scale",
            "slider",
            100,
            70,
            140,
            system = true,
        ),
        MistBarOption("offset_x", "slider", 0, -200, 200),
        MistBarOption("offset_y", "slider", 0, -24, 100),
        MistBarOption(
            "expanded_mode",
            "choice",
            default = 1,
            choices = listOf("camera_attached", "independent"),
        ),
        MistBarOption(
            "expanded_width",
            "choice",
            default = 1,
            choices = listOf("full_width", "content_width"),
        ),
        MistBarOption(
            "expanded_shape",
            "choice",
            default = 1,
            choices = listOf("pill", "rounded_corner", "squircle"),
        ),
        MistBarOption("custom_radius", default = 1),
        MistBarOption("camera_width", "slider", 25, 0, 120),
        MistBarOption("camera_height", "slider", 24, 8, 64),
        MistBarOption("corner_radius", "slider", 42, 0, 64),
        MistBarOption("outline_width", "slider", 10, 0, 30),
        MistBarOption("shadow_elevation", "slider", 12, 0, 32),
        MistBarOption("collapsed_opacity", "slider", 0, 0, 100),
        MistBarOption(
            "collapsed_border",
            "choice",
            choices = listOf("filled", "one_dp", "two_dp", "three_dp"),
        ),
        MistBarOption("companion", default = 1),
        MistBarOption("always_visible"),
    )

internal val mistBarMotionOptions =
    listOf(
        MistBarOption("animation_style", "choice", choices = listOf("fruit", "robot")),
        MistBarOption("animation_speed", "slider", 100, 50, 200),
        MistBarOption(
            "animation_bounce",
            "choice",
            1,
            choices = listOf("no_bounce", "normal_bounce", "high_bounce"),
        ),
        MistBarOption("status_glow", default = 1),
        MistBarOption("reduce_motion"),
        MistBarOption("haptics", default = 1),
        MistBarOption("mini_fade", default = 1),
    )

internal val mistBarMediaOptions =
    listOf(
        MistBarOption(
            "media_background",
            "choice",
            default = 2,
            choices = listOf("none", "cover", "blurred_cover", "waves", "gradient"),
        ),
        MistBarOption("artwork_shape", "choice", 4, choices = mistBarShapeNames),
        MistBarOption("media_button_shape", "choice", 2, choices = mistBarShapeNames),
        MistBarOption("player_icon", "choice", choices = listOf("app_icon", "app_name", "none")),
        MistBarOption("media_haptics", default = 1),
        MistBarOption("media_artwork_colors"),
        MistBarOption("rotate_artwork", default = 1),
        MistBarOption("equalizer", default = 1),
        MistBarOption("media_slider", default = 1),
        MistBarOption("media_thin_border", default = 1),
        MistBarOption("media_hide_paused"),
        MistBarOption("media_hide_delay", "slider", 0, 0, 300),
        MistBarOption("media_mini", default = 1),
        MistBarOption("media_mini_custom"),
        MistBarOption("media_mini_left_shape", "choice", 2, choices = mistBarShapeNames),
        MistBarOption("media_mini_right_shape", "choice", 2, choices = mistBarShapeNames),
        MistBarOption(
            "media_mini_right",
            "choice",
            1,
            choices = listOf("app_icon", "equalizer"),
        ),
        MistBarOption("media_mini_artwork_colors"),
        MistBarOption("status_bar_dynamic_island_lyrics", system = true),
    )

internal val mistBarFeatureKeys =
    listOf(
        "battery",
        "unlock",
        "bluetooth",
        "calls",
        "notifications",
        "ongoing_activities",
        "hotspot",
        "vpn",
        "ringer",
        "timer",
        "audio_recording",
        "now_playing",
        "media_controls",
        "screen_recording",
        "alarms",
        "flashlight",
        "stopwatch",
        "live_scores",
        "lockscreen",
        "disable_in_landscape",
    )

internal val mistBarFeatureOptions =
    mistBarFeatureKeys.map {
        MistBarOption(
            "status_bar_dynamic_island_$it",
            default =
                if (
                    it in
                        listOf(
                            "media_controls",
                            "screen_recording",
                            "alarms",
                            "flashlight",
                            "stopwatch",
                            "live_scores",
                        )
                )
                    1
                else 0,
            system = true,
        )
    }
