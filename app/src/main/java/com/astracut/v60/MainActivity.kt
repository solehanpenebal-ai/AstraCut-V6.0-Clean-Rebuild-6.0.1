package com.astracut.v60

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.app.Activity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.util.Locale

class MainActivity : Activity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var seekBar: SeekBar
    private lateinit var timeLabel: TextView
    private lateinit var statusLabel: TextView
    private lateinit var exportButton: Button
    private var trimInMs = 0L
    private var trimOutMs = 0L

    private val OPEN_VIDEO_REQUEST = 1001

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OPEN_VIDEO_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                loadVideo(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 16)
        }
        root.addView(TextView(this).apply {
            text = "AstraCut Pro V6.0"
            textSize = 24f
            gravity = Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(-1, 56))

        statusLabel = TextView(this).apply {
            text = "Siap — pilih video untuk memulai."
            textSize = 14f
        }
        root.addView(statusLabel, LinearLayout.LayoutParams(-1, 48))

        root.addView(Button(this).apply {
            text = "＋  IMPORT VIDEO"
            setOnClickListener {\n                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {\n                    addCategory(Intent.CATEGORY_OPENABLE)\n                    type = "video/*"\n                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)\n                }\n                startActivityForResult(intent, OPEN_VIDEO_REQUEST)\n            }
        }, LinearLayout.LayoutParams(-1, 52))

        playerView = PlayerView(this).apply {
            useController = true
            controllerAutoShow = true
        }
        root.addView(playerView, LinearLayout.LayoutParams(-1, 0, 1f))

        timeLabel = TextView(this).apply {
            text = "00:00 / 00:00"
            gravity = Gravity.CENTER
            textSize = 13f
        }
        root.addView(timeLabel, LinearLayout.LayoutParams(-1, 32))

        seekBar = SeekBar(this).apply {
            max = 1000
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                    if (fromUser) player?.let { if (it.duration > 0) it.seekTo(it.duration * p / 1000L) }
                }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) {}
            })
        }
        root.addView(seekBar, LinearLayout.LayoutParams(-1, 42))

        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(Button(this).apply {
            text = "PLAY"
            setOnClickListener { player?.let { if (it.isPlaying) it.pause() else it.play() } }
        }, LinearLayout.LayoutParams(0, 52, 1f))
        controls.addView(Button(this).apply {
            text = "SET IN"
            setOnClickListener {
                trimInMs = player?.currentPosition ?: 0L
                updateTrimStatus()
            }
        }, LinearLayout.LayoutParams(0, 52, 1f))
        controls.addView(Button(this).apply {
            text = "SET OUT"
            setOnClickListener {
                trimOutMs = player?.currentPosition ?: 0L
                updateTrimStatus()
            }
        }, LinearLayout.LayoutParams(0, 52, 1f))
        root.addView(controls)

        exportButton = Button(this).apply {
            text = "EXPORT"
            isEnabled = false
            setOnClickListener {
                statusLabel.text = "Media loaded. Render/export engine is the next implementation stage."
            }
        }
        root.addView(exportButton, LinearLayout.LayoutParams(-1, 52))
        setContentView(root)
    }

    private fun loadVideo(uri: Uri) {
        player?.release()
        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo
            exo.setMediaItem(MediaItem.fromUri(uri))
            exo.prepare()
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        trimInMs = 0L
                        trimOutMs = exo.duration.coerceAtLeast(0L)
                        statusLabel.text = "Video siap — ${format(exo.duration)}"
                        exportButton.isEnabled = true
                        updateProgress()
                    }
                }
            })
        }
        statusLabel.text = "Memuat video…"
    }

    private fun updateTrimStatus() {
        val end = if (trimOutMs > 0) trimOutMs else (player?.duration ?: 0L)
        statusLabel.text = "IN ${format(trimInMs)}  •  OUT ${format(end)}"
    }

    private fun updateProgress() {
        val exo = player ?: return
        val duration = exo.duration
        if (duration > 0) {
            seekBar.progress = (exo.currentPosition * 1000L / duration).toInt().coerceIn(0, 1000)
            timeLabel.text = "${format(exo.currentPosition)} / ${format(duration)}"
        }
        seekBar.postDelayed({ updateProgress() }, 250)
    }

    private fun format(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d", total / 60, total % 60)
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
