package com.astracut.v60.gpu

enum class LayerType { VIDEO_OES, IMAGE_BITMAP, TEXT_BITMAP }

data class Transform(
    val x: Float=.5f, val y: Float=.5f, val scaleX: Float=1f,
    val scaleY: Float=1f, val rotationDeg: Float=0f, val alpha: Float=1f
)

data class GpuLayer(val id: String, val type: LayerType, val z: Int, val transform: Transform)

data class OesFrame(val ptsUs: Long, val textureId: Int, val matrix: FloatArray)

interface EglRenderer {
    fun render(frame: OesFrame, layers: List<GpuLayer>, presentationTimeNs: Long): Boolean
}

class LayerGraph(private val renderer: EglRenderer) {
    fun render(frame: OesFrame, layers: List<GpuLayer>, timeUs: Long): Boolean =
        renderer.render(frame, layers.sortedBy { it.z }, timeUs*1000)
}
