package com.astracut.v60.diagnostics

data class EngineDiagnostics(
    var presented:Long=0,var held:Long=0,var dropped:Long=0,
    var audioUnderflows:Long=0,var audioOverflows:Long=0,
    var encodedVideo:Long=0,var encodedAudio:Long=0
)
data class CodecCapability(val mime:String,val encoder:Boolean,val hardware:Boolean,val maxWidth:Int?=null,val maxHeight:Int?=null)
data class DeviceReport(val codecs:List<CodecCapability>,val glEs:Int,val notes:List<String>)
