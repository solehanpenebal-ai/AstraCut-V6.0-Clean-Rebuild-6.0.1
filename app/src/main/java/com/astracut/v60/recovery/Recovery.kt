package com.astracut.v60.recovery

import com.astracut.v60.encode.ExportState

data class Checkpoint(val exportId:String,val state:ExportState,val positionUs:Long,val output:String,val updatedAtMs:Long=System.currentTimeMillis())
interface CheckpointStore { fun save(c:Checkpoint);fun load(id:String):Checkpoint?;fun delete(id:String) }
class MemoryCheckpointStore:CheckpointStore{
    private val m=HashMap<String,Checkpoint>()
    override fun save(c:Checkpoint){m[c.exportId]=c}
    override fun load(id:String)=m[id]
    override fun delete(id:String){m.remove(id)}
}
