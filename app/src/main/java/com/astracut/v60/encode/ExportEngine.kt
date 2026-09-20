package com.astracut.v60.encode

import com.astracut.v60.core.Project

data class VideoEncodeConfig(val width:Int,val height:Int,val fps:Int,val bitrate:Int,val mime:String="video/avc")
data class AudioEncodeConfig(val sampleRate:Int=48000,val channels:Int=2,val bitrate:Int=192000,val mime:String="audio/mp4a-latm")

data class EncodedSample(val data:ByteArray,val ptsUs:Long,val flags:Int,val trackType:Int)

interface VideoEncoder {
    fun inputSurface(): Any
    fun start()
    fun drain(): EncodedSample?
    fun signalEnd()
    fun stop()
    fun release()
}
interface AudioEncoder {
    fun start()
    fun encode(pcm:ShortArray,ptsUs:Long):EncodedSample?
    fun signalEnd()
    fun stop()
    fun release()
}
interface Mp4Muxer {
    fun addVideoTrack(format:Any):Int
    fun addAudioTrack(format:Any):Int
    fun start()
    fun writeSample(track:Int,sample:EncodedSample)
    fun stop()
    fun release()
}

enum class ExportState { IDLE, PREPARING, ENCODING, MUXING, FINALIZING, COMPLETED, CANCELLED, FAILED }

class ProductionExportEngine(
    private val video:VideoEncoder, private val audio:AudioEncoder, private val muxer:Mp4Muxer
){
    var state=ExportState.IDLE; private set
    fun prepare(){ check(state==ExportState.IDLE); state=ExportState.PREPARING; video.start();audio.start() }
    fun begin(){ check(state==ExportState.PREPARING);state=ExportState.ENCODING }
    fun beginMux(){state=ExportState.MUXING;muxer.start()}
    fun cancel(){state=ExportState.CANCELLED}
    fun finalizeExport(){
        if(state==ExportState.CANCELLED)return
        state=ExportState.FINALIZING
        runCatching{
            video.signalEnd();audio.signalEnd();muxer.stop();video.stop();audio.stop();state=ExportState.COMPLETED
        }.onFailure{state=ExportState.FAILED}
        .also{video.release();audio.release();muxer.release()}
    }
}
