package io

import com.russhwolf.settings.Settings

class Preferences {

    private val settings = Settings()

    var videoFilter: VideoFilter
        get() = settings.getStringOrNull(KEY_VIDEO_FILTER)
            ?.let {
                runCatching { VideoFilter.valueOf(it) }.getOrNull()
            } ?: VideoFilter.NONE
        set(value) {
            settings.putString(key = KEY_VIDEO_FILTER, value = value.name)
        }

    companion object {
        const val KEY_VIDEO_FILTER = "video-filter"
    }
}

enum class VideoFilter {
    CRT, CAST_SHADOWS, NONE
}