package com.astracut.v60.compat

data class ExportProfile(val width:Int,val height:Int,val fps:Int,val videoMime:String,val audioMime:String)

object Profiles {
    val HD=ExportProfile(1280,720,30,"video/avc","audio/mp4a-latm")
    val FULL_HD=ExportProfile(1920,1080,30,"video/avc","audio/mp4a-latm")
    val UHD=ExportProfile(3840,2160,30,"video/avc","audio/mp4a-latm")
}

interface CapabilityResolver {
    fun supports(profile:ExportProfile):Boolean
}
