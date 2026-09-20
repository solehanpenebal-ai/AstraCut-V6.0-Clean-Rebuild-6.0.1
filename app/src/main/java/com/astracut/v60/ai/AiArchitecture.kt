package com.astracut.v60.ai

enum class AiMode { TEXT_TO_VIDEO, IMAGE_TO_VIDEO, VIDEO_TO_VIDEO, AI_EXTEND, BACKGROUND_REMOVAL, AUTO_CAPTION, AI_ENHANCE }

data class AiRequest(val id:String,val mode:AiMode,val prompt:String?,val inputUri:String?,val options:Map<String,String> = emptyMap())
enum class AiJobState { QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED }
data class AiJob(val id:String,val request:AiRequest,val state:AiJobState,val resultUri:String?=null,val error:String?=null)

interface AiVideoProvider { suspend fun submit(request:AiRequest):AiJob; suspend fun status(job:AiJob):AiJob }
class SecureBackendAiProvider(private val endpoint:String):AiVideoProvider {
    override suspend fun submit(request:AiRequest):AiJob =
        AiJob(request.id,request,AiJobState.QUEUED)
    override suspend fun status(job:AiJob):AiJob = job
}
