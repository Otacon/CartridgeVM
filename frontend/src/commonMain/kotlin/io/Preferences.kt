package io

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.serialization.decodeValueOrNull
import com.russhwolf.settings.serialization.encodeValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
class Preferences {

    private val settings = Settings()

    @OptIn(ExperimentalSettingsApi::class)
    var mappings: ControllerMappings?
        get() = settings.decodeValueOrNull(KEY_GAMEPAD_MAPPINGS)
        set(value) = settings.encodeValue(key = KEY_GAMEPAD_MAPPINGS, value = value)

    var videoFilter: VideoFilter
        get() = settings.getStringOrNull(KEY_VIDEO_FILTER)
            ?.let {
                runCatching { VideoFilter.valueOf(it) }.getOrNull()
            } ?: VideoFilter.NONE
        set(value) {
            settings.putString(key = KEY_VIDEO_FILTER, value = value.name)
        }

    companion object {
        private const val KEY_VIDEO_FILTER = "video-filter"
        private const val KEY_GAMEPAD_MAPPINGS = "gamepad-mappings"
    }
}

enum class VideoFilter {
    CRT, CAST_SHADOWS, NONE
}

@Serializable
data class ControllerMappings(
    @SerialName("controller") val controller: DeviceMappings,
    @SerialName("keyboard") val keyboard: DeviceMappings,
)

@Serializable
data class DeviceMappings(
    @SerialName("a") val a: String,
    @SerialName("b") val b: String,
    @SerialName("up") val up: String,
    @SerialName("down") val down: String,
    @SerialName("left") val left: String,
    @SerialName("right") val right: String,
)