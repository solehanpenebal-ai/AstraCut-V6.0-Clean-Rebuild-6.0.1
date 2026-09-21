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
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import com.astracut.v60.edit.Clip
import com.astracut.v60.edit.EditHistory
import com.astracut.v60.edit.ProjectSnapshot
import java.io.File
import java.io.FileInputStream
import java.util.Locale

@OptIn(UnstableApi::class)
class MainActivity : Activity() {
    private var player: ExoPlayer? = null
    private var transformer: Transformer? = null
    private var exporting = false
    private lateinit var playerView: PlayerView
    private lateinit var seekBar: SeekBar
    private lateinit var timeLabel: TextView
    private lateinit var statusLabel: TextView
    private lateinit var timeline: LinearLayout
    private lateinit var exportButton: Button
    private lateinit var undoButton: Button
    private lateinit var redoButton: Button
    private lateinit var splitButton: Button
    private val clips = mutableListOf<Clip>()
    private val durations = mutableMapOf<Uri, Long>()
    private val history = EditHistory()
    private var selectedIndex = -1
    private val OPEN_VIDEO_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != OPEN_VIDEO_REQUEST || resultCode != RESULT_OK) return
        val uris = mutableListOf<Uri>()
        data?.clipData?.let { cd -> for (i in 0 until cd.itemCount) uris += cd.getItemAt(i).uri }
        data?.data?.let { if (uris.isEmpty()) uris += it }
        if (uris.isNotEmpty()) importUris(uris)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
        }
        root.addView(TextView(this).apply {
            text = "AstraCut Pro V6.0"
            textSize = 23f
            gravity = Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(-1, 52))
        statusLabel = TextView(this).apply {
            text = "Pilih satu atau beberapa video untuk membuat project."
            textSize = 13f
        }
        root.addView(statusLabel, LinearLayout.LayoutParams(-1, 44))
        root.addView(Button(this).apply {
            text = "＋  IMPORT VIDEO / MULTI-CLIP"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "video/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }
                startActivityForResult(intent, OPEN_VIDEO_REQUEST)
            }
        }, LinearLayout.LayoutParams(-1, 50))
        playerView = PlayerView(this).apply {
            useController = true
            controllerAutoShow = true
        }
        root.addView(playerView, LinearLayout.LayoutParams(-1, 0, 1f))
        timeLabel = TextView(this).apply {
            text = "00:00 / 00:00"
            gravity = Gravity.CENTER
        }
        root.addView(timeLabel, LinearLayout.LayoutParams(-1, 28))
        seekBar = SeekBar(this).apply {
            max = 1000
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                    if (fromUser) player?.let { if (it.duration > 0) it.seekTo(it.duration * p / 1000L) }
                }
                override fun onStartTrackingTouch(s: SeekBar) = Unit
                override fun onStopTrackingTouch(s: SeekBar) = Unit
            })
        }
        root.addView(seekBar, LinearLayout.LayoutParams(-1, 40))
        val scroll = HorizontalScrollView(this)
        timeline = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        scroll.addView(timeline)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 82))
        val editRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        undoButton = actionButton("UNDO") { undo() }
        redoButton = actionButton("REDO") { redo() }
        splitButton = actionButton("SPLIT") { splitSelected() }
        editRow.addView(undoButton, LinearLayout.LayoutParams(0, 48, 1f))
        editRow.addView(redoButton, LinearLayout.LayoutParams(0, 48, 1f))
        editRow.addView(splitButton, LinearLayout.LayoutParams(0, 48, 1f))
        root.addView(editRow)
        val trimRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        trimRow.addView(actionButton("SET IN") { setIn() }, LinearLayout.LayoutParams(0, 48, 1f))
        trimRow.addView(actionButton("SET OUT") { setOut() }, LinearLayout.LayoutParams(0, 48, 1f))
        trimRow.addView(actionButton("RESET") { resetSelectedClip() }, LinearLayout.LayoutParams(0, 48, 1f))
        root.addView(trimRow)
        exportButton = Button(this).apply {
            text = "EXPORT PROJECT MP4"
            isEnabled = false
            setOnClickListener { exportProject() }
        }
        root.addView(exportButton, LinearLayout.LayoutParams(-1, 50))
        setContentView(root)
        refreshUi()
    }

    private fun actionButton(title: String, click: () -> Unit) = Button(this).apply {
        text = title
        setOnClickListener { click() }
    }

    private fun importUris(uris: List<Uri>) {
        saveHistory()
        clips.clear()
        clips += uris.distinct().mapIndexed { i, uri -> Clip(uri, 0L, -1L, "Clip " + (i + 1)) }
        selectedIndex = 0
        statusLabel.text = "Memuat " + clips.size + " clip…"
        loadSelectedClip()
        refreshUi()
    }

    private fun loadSelectedClip() {
        val clip = clips.getOrNull(selectedIndex) ?: return
        player?.release()
        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo
            exo.setMediaItem(MediaItem.fromUri(clip.uri))
            exo.prepare()
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        durations[clip.uri] = exo.duration.coerceAtLeast(0L)
                        if (clip.endMs <= 0L || clip.endMs > exo.duration) clip.endMs = exo.duration
                        statusLabel.text = "Clip " + (selectedIndex + 1) + " siap — " + format(clip.lengthMs(exo.duration))
                        refreshUi()
                        updateProgress()
                    }
                }
            })
        }
        statusLabel.text = "Memuat clip…"
    }

    private fun setIn() {
        val exo = player ?: return
        val clip = clips.getOrNull(selectedIndex) ?: return
        val pos = exo.currentPosition.coerceAtLeast(0L)
        if (pos >= clip.effectiveEnd(exo.duration)) {
            statusLabel.text = "IN harus berada sebelum OUT."
            return
        }
        saveHistory()
        clip.startMs = pos
        refreshUi()
        statusLabel.text = "Clip " + (selectedIndex + 1) + ": IN " + format(pos)
    }

    private fun setOut() {
        val exo = player ?: return
        val clip = clips.getOrNull(selectedIndex) ?: return
        val pos = exo.currentPosition
        if (pos <= clip.startMs) {
            statusLabel.text = "OUT harus lebih besar dari IN."
            return
        }
        saveHistory()
        clip.endMs = pos
        refreshUi()
        statusLabel.text = "Clip " + (selectedIndex + 1) + ": OUT " + format(pos)
    }

    private fun resetSelectedClip() {
        val clip = clips.getOrNull(selectedIndex) ?: return
        val duration = durations[clip.uri] ?: player?.duration ?: return
        saveHistory()
        clip.startMs = 0L
        clip.endMs = duration
        refreshUi()
        statusLabel.text = "Clip dikembalikan ke durasi penuh."
    }

    private fun splitSelected() {
        val clip = clips.getOrNull(selectedIndex) ?: return
        val duration = durations[clip.uri] ?: player?.duration ?: return
        val splitAt = player?.currentPosition ?: clip.startMs
        if (splitAt <= clip.startMs + 50L || splitAt >= clip.effectiveEnd(duration) - 50L) {
            statusLabel.text = "Playhead terlalu dekat dengan ujung clip."
            return
        }
        saveHistory()
        val first = clip.copy(endMs = splitAt, label = clip.label + " A")
        val second = clip.copy(startMs = splitAt, endMs = clip.effectiveEnd(duration), label = clip.label + " B")
        clips[selectedIndex] = first
        clips.add(selectedIndex + 1, second)
        refreshUi()
        statusLabel.text = "Clip di-split menjadi 2 bagian."
    }

    private fun selectClip(index: Int) {
        if (index !in clips.indices || index == selectedIndex) return
        selectedIndex = index
        loadSelectedClip()
        refreshUi()
    }

    private fun saveHistory() {
        if (clips.isNotEmpty()) history.push(ProjectSnapshot(clips.map { it.copy() }, selectedIndex))
    }

    private fun restore(snapshot: ProjectSnapshot) {
        clips.clear()
        clips += snapshot.clips.map { it.copy() }
        selectedIndex = snapshot.selected.coerceIn(-1, clips.lastIndex)
        if (selectedIndex >= 0) loadSelectedClip()
        refreshUi()
    }

    private fun undo() {
        val snapshot = history.undo(ProjectSnapshot(clips.map { it.copy() }, selectedIndex)) ?: return
        restore(snapshot)
        statusLabel.text = "Undo."
    }

    private fun redo() {
        val snapshot = history.redo(ProjectSnapshot(clips.map { it.copy() }, selectedIndex)) ?: return
        restore(snapshot)
        statusLabel.text = "Redo."
    }

    private fun refreshUi() {
        timeline.removeAllViews()
        clips.forEachIndexed { index, clip ->
            val duration = durations[clip.uri] ?: if (index == selectedIndex) player?.duration ?: 0L else 0L
            val button = Button(this).apply {
                text = (index + 1).toString() + "\n" + format(clip.lengthMs(duration))
                isAllCaps = false
                setOnClickListener { selectClip(index) }
            }
            timeline.addView(button, LinearLayout.LayoutParams(150, 72).apply { setMargins(4, 4, 4, 4) })
        }
        undoButton.isEnabled = history.canUndo
        redoButton.isEnabled = history.canRedo
        splitButton.isEnabled = selectedIndex >= 0
        exportButton.isEnabled = clips.isNotEmpty() && !exporting
    }

    private fun exportProject() {
        if (clips.isEmpty() || exporting) return
        exporting = true
        exportButton.isEnabled = false
        statusLabel.text = "Mengekspor " + clips.size + " clip menjadi satu MP4…"
        val outputFile = File(cacheDir, "astracut_project_" + System.currentTimeMillis() + ".mp4")
        val editedItems = clips.map { clip ->
            val duration = durations[clip.uri] ?: clip.endMs
            val end = clip.effectiveEnd(duration)
            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(clip.startMs)
                .setEndPositionMs(end)
                .build()
            EditedMediaItem.Builder(
                MediaItem.Builder().setUri(clip.uri).setClippingConfiguration(clipping).build()
            ).build()
        }
        val sequence = EditedMediaItemSequence.Builder().addItems(editedItems).build()
        val composition = Composition.Builder(sequence).build()
        transformer = Transformer.Builder(this)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, result: ExportResult) {
                    runOnUiThread { saveExportToMovies(outputFile) }
                }
                override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {
                    runOnUiThread {
                        exporting = false
                        outputFile.delete()
                        statusLabel.text = "Export gagal: " + exception.getErrorCodeName()
                        refreshUi()
                    }
                }
            }).build()
        runCatching { transformer?.start(composition, outputFile.absolutePath) }.onFailure {
            exporting = false
            outputFile.delete()
            statusLabel.text = "Export gagal: " + (it.message ?: "kesalahan tidak diketahui")
            refreshUi()
        }
    }

    private fun saveExportToMovies(source: File) {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "AstraCut_" + System.currentTimeMillis() + ".mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/AstraCut")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val outputUri = contentResolver.insert(collection, values)
        if (outputUri == null) {
            source.delete()
            exporting = false
            statusLabel.text = "Gagal membuat file di Galeri."
            refreshUi()
            return
        }
        try {
            contentResolver.openOutputStream(outputUri)?.use { out ->
                FileInputStream(source).use { input -> input.copyTo(out) }
            } ?: error("Output stream tidak tersedia.")
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            contentResolver.update(outputUri, values, null, null)
            statusLabel.text = "Export selesai — Movies/AstraCut."
        } catch (t: Throwable) {
            contentResolver.delete(outputUri, null, null)
            statusLabel.text = "Gagal menyimpan hasil: " + (t.message ?: "kesalahan tidak diketahui")
        } finally {
            source.delete()
            exporting = false
            refreshUi()
        }
    }

    private fun updateProgress() {
        val exo = player ?: return
        val duration = exo.duration
        if (duration > 0) {
            seekBar.progress = (exo.currentPosition * 1000L / duration).toInt().coerceIn(0, 1000)
            timeLabel.text = format(exo.currentPosition) + " / " + format(duration)
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
