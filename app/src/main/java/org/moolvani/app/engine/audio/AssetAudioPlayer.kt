package org.moolvani.app.engine.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

class AssetAudioPlayer(private val context: Context) {

    companion object {
        private const val TAG = "AssetAudioPlayer"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var playingState = false
    private val mainHandler = Handler(Looper.getMainLooper())

    fun playAsset(assetPath: String, playbackSpeed: Float = 1.0f, onCompletion: (() -> Unit)? = null): Boolean {
        stop()
        return try {
            val afd = context.assets.openFd(assetPath)
            val mp = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    playbackParams = playbackParams.setSpeed(playbackSpeed.coerceIn(0.5f, 2.0f))
                }
                setOnCompletionListener {
                    this@AssetAudioPlayer.playingState = false
                    stop()
                    mainHandler.post { onCompletion?.invoke() }
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    this@AssetAudioPlayer.playingState = false
                    stop()
                    mainHandler.post { onCompletion?.invoke() }
                    true
                }
                start()
            }
            mediaPlayer = mp
            playingState = true
            Log.d(TAG, "Playing pre-rendered clear asset: $assetPath")
            true
        } catch (e: Exception) {
            Log.d(TAG, "Asset not found or cannot open: $assetPath (${e.message})")
            playingState = false
            false
        }
    }

    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.reset()
                it.release()
            }
        } catch (ignored: Exception) {}
        mediaPlayer = null
        playingState = false
    }

    fun isCurrentlyPlaying(): Boolean = playingState
}
