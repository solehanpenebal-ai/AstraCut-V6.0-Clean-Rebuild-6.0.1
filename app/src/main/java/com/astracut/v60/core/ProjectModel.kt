package com.astracut.v60.core

data class Project(
    val id: String,
    val name: String,
    val durationUs: Long,
    val videoTracks: List<VideoClip> = emptyList(),
    val audioTracks: List<AudioClip> = emptyList(),
    val overlays: List<Overlay> = emptyList(),
    val transitions: List<Transition> = emptyList()
)

data class VideoClip(
    val id: String, val uri: String,
    val sourceInUs: Long, val sourceOutUs: Long,
    val timelineStartUs: Long, val timelineEndUs: Long,
    val z: Int = 0
)

data class AudioClip(
    val id: String, val uri: String,
    val sourceInUs: Long, val sourceOutUs: Long,
    val timelineStartUs: Long, val timelineEndUs: Long,
    val gain: Float = 1f, val pan: Float = 0f,
    val muted: Boolean = false,
    val fadeInUs: Long = 0, val fadeOutUs: Long = 0
)

enum class OverlayType { TEXT, IMAGE }
data class Overlay(
    val id: String, val type: OverlayType,
    val startUs: Long, val endUs: Long, val z: Int,
    val x: Float = .5f, val y: Float = .5f,
    val scale: Float = 1f, val rotation: Float = 0f,
    val opacity: Float = 1f
)

data class Transition(
    val fromClipId: String, val toClipId: String,
    val durationUs: Long, val type: String = "crossfade"
)
