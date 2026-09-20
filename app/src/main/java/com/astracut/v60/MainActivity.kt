package com.astracut.v60

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import java.io.File
import java.io.FileInputStream
import java.util.Locale

@OptIn(UnstableApi::class)
class MainActivity : Activity() {
    private var player: ExoPlayer? = null
    private var currentUri: Uri? = null
    private var transformer: Transformer? = null
    private var exporting = false

    private lateinit var playerView: PlayerView
    private lateinit var seekBar: SeekBar
    private lateinit var timeLabel: TextView
    private lateinit var statusLabel: TextView
    private lateinit var exportButton: Button

    private var trimInMs = 0L
    private var trimOutMs = 0L
    private val OPEN_VIDEO_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OPEN_VIDEO_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                runCatching {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                loadVideo(uri)
            }
        }
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
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "video/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }
                startActivityForResult(intent, OPEN_VIDEO_REQUEST)
            }
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
                    if (fromUser) player?.let {
                        if (it.duration > 0L) it.seekTo(it.duration * p / 1000L)
                    }
                }
                override fun onStartTrackingTouch(s: SeekBar) = Unit
                override fun onStopTrackingTouch(s: SeekBar) = Unit
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
                if (trimOutMs > 0L && trimInMs >= trimOutMs) trimOutMs = player?.duration ?: 0L
                updateTrimStatus()
            }
        }, LinearLayout.LayoutParams(0, 52, 1f))

        controls.addView(Button(this).apply {
            text = "SET OUT"
            setOnClickListener {
                trimOutMs = player?.currentPosition ?: 0L
                if (trimOutMs <= trimInMs) {
                    statusLabel.text = "OUT harus lebih besar dari IN."
                    return@setOnClickListener
                }
                updateTrimStatus()
            }
        }, LinearLayout.LayoutParams(0, 52, 1f))

        root.addView(controls)

        exportButton = Button(this).apply {
            text = "EXPORT MP4"
            isEnabled = false
            setOnClickListener { exportTrimmedVideo() }
        }
        root.addView(exportButton, LinearLayout.LayoutParams(-1, 52))

        setContentView(root)
    }

    private fun loadVideo(uri: Uri) {
        player?.release()
        currentUri = uri
        trimInMs = 0L
        trimOutMs = 0L
        exportButton.isEnabled = false

        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo
            exo.setMediaItem(MediaItem.fromUri(uri))
            exo.prepare()
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        trimInMs = 0L
                        trimOutMs = exo.duration.coerceAtLeast(0L)
                        statusLabel.text = "Video siap — \${format(exo.duration)}"
                        exportButton.isEnabled = exo.duration > 0L
                        updateProgress()
                    }
                }
            })
        }
        statusLabel.text = "Memuat video…"
    }

    private fun updateTrimStatus() {
        val duration = player?.duration ?: 0L
        val end = if (trimOutMs > 0L) trimOutMs else duration
        statusLabel.text = "IN \${format(trimInMs)}  •  OUT \${format(end)}"
    }

    private fun exportTrimmedVideo() {
        val uri = currentUri ?: return
        val duration = player?.duration ?: 0L
        val end = if (trimOutMs > 0L) trimOutMs else duration

        if (duration <= 0L || trimInMs < 0L || end <= trimInMs || end > duration) {
            statusLabel.text = "Rentang trim tidak valid."
            return
        }
        if (exporting) return

        exporting = true
        exportButton.isEnabled = false
        statusLabel.text = "Mengekspor MP4…"

        val outputFile = File(cacheDir, "astracut_export_\${System.currentTimeMillis()}.mp4")
        val clipping = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(trimInMs)
            .setEndPositionMs(end)
            .build()
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(clipping)
            .build()
        val edited = EditedMediaItem.Builder(mediaItem).build()

        transformer = Transformer.Builder(this)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, result: ExportResult) {
                    runOnUiThread { saveExportToMovies(outputFile) }
                }

                override fun onError(
                    composition: Composition,
                    result: ExportResult,
                    exception: ExportException
                ) {
                    runOnUiThread {
                        exporting = false
                        exportButton.isEnabled = true
                        outputFile.delete()
                        statusLabel.text = "Export gagal: \${exception.getErrorCodeName()}"
                    }
                }
            })
            .build()

        runCatching {
            transformer?.start(edited, outputFile.absolutePath)
        }.onFailure {
            exporting = false
            exportButton.isEnabled = true
            outputFile.delete()
            statusLabel.text = "Export gagal: \${it.message ?: "kesalahan tidak diketahui"}"
        }
    }

    private fun saveExportToMovies(source: File) {
        val name = "AstraCut_\${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/AstraCut")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val outputUri = contentResolver.insert(collection, values)

        if (outputUri == null) {
            exporting = false
            exportButton.isEnabled = true
            source.delete()
            statusLabel.text = "Export selesai tetapi gagal menyimpan ke Galeri."
            return
        }

        try {
            contentResolver.openOutputStream(outputUri)?.use { out ->
                FileInputStream(source).use { input -> input.copyTo(out) }
            } ?: error("Tidak dapat membuka file output.")

            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            contentResolver.update(outputUri, values, null, null)
            statusLabel.text = "Export selesai — tersimpan di Movies/AstraCut."
        } catch (t: Throwable) {
            contentResolver.delete(outputUri, null, null)
            statusLabel.text = "Gagal menyimpan hasil: \${t.message ?: "kesalahan tidak diketahui"}"
        } finally {
            exporting = false
            exportButton.isEnabled = true
            source.delete()
        }
    }

    private fun updateProgress() {
        val exo = player ?: return
        val duration = exo.duration
        if (duration > 0L) {
            seekBar.progress = (exo.currentPosition * 1000L / duration).toInt().coerceIn(0, 1000)
            timeLabel.text = "\${format(exo.currentPosition)} / \${format(duration)}"
        }
        seekBar.postDelayed({ updateProgress() }, 250)
    }

    private fun format(ms: Long): String {
        val total = (ms / 1000L).coerceAtLeast(0L)
        return String.format(Locale.US, "%02d:%02d", total / 60L, total % 60L)
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        transformer?.cancel()
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
