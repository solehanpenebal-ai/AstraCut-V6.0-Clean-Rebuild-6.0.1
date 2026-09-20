package com.astracut.v60.audio

data class PcmBlock(val ptsUs: Long, val sampleRate: Int, val channels: Int, val samples: ShortArray)

interface AudioDecoder {
    fun decode(): PcmBlock?
    fun seekToUs(positionUs: Long)
    fun release()
}

class PcmRingBuffer(private val capacity: Int) {
    private val data = ShortArray(capacity)
    private var r=0; private var w=0; private var size=0
    var underflows=0L; private set
    var overflows=0L; private set
    @Synchronized fun offer(input: ShortArray): Int {
        var n=0
        for (s in input) {
            if (size==capacity) { overflows++; break }
            data[w]=s; w=(w+1)%capacity; size++; n++
        }
        return n
    }
    @Synchronized fun poll(out: ShortArray): Int {
        var n=0
        for (i in out.indices) {
            if (size==0) { underflows++; break }
            out[i]=data[r]; r=(r+1)%capacity; size--; n++
        }
        return n
    }
    @Synchronized fun clear(){r=0;w=0;size=0}
}

data class MixControl(val gain:Float=1f,val pan:Float=0f,val fade:Float=1f,val muted:Boolean=false)

class PcmMixer {
    fun mix(inputs: List<Pair<ShortArray,MixControl>>, out: ShortArray) {
        java.util.Arrays.fill(out,0)
        for ((s,c) in inputs) {
            if(c.muted) continue
            val g=(c.gain*c.fade).coerceAtLeast(0f)
            val p=c.pan.coerceIn(-1f,1f)
            val l=g*if(p>0)1-p else 1f
            val r=g*if(p<0)1+p else 1f
            var i=0
            while(i+1<out.size && i+1<s.size){
                out[i]=(out[i]+s[i]*l).toInt().coerceIn(-32768,32767).toShort()
                out[i+1]=(out[i+1]+s[i+1]*r).toInt().coerceIn(-32768,32767).toShort()
                i+=2
            }
        }
    }
}

interface AudioSink {
    fun start()
    fun write(pcm: ShortArray): Int
    fun positionUs(): Long
    fun stop()
    fun release()
}
