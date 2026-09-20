package com.astracut.v60

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.astracut.v60.core.EngineInfo

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = EngineInfo.display()
            textSize = 17f
            setPadding(40, 60, 40, 40)
        })
    }
}
