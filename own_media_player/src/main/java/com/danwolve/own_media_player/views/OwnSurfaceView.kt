package com.danwolve.own_media_player.views

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player

internal class OwnSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr) {

    private var player: Player? = null
    private var playBackPosition: Long? = null

    private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private var isSurfaceReady = false

    private var onPreparedListener: ((Player) -> Unit)? = null
    private var onLoadingListener : ((Boolean) -> Unit)? = null
    private var onPlayingListener : ((Boolean) -> Unit)? = null
    private var onReadyListener: ((Player) -> Unit)? = null
    private var onCompletionListener: (() -> Unit)? = null
    private var onErrorListener: ((PlaybackException) -> Unit)? = null
    private var onBufferingUpdateListener: ((Player, Long) -> Unit)? = null
    private var onSeekCompleteListener: ((Player) -> Unit)? = null
    private var onInfoListener: ((Player, Int, Int) -> Unit)? = null

    private var playerListener: Player.Listener? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            setSurfaceLifecycle(SURFACE_LIFECYCLE_FOLLOWS_ATTACHMENT)
        }
    }

    fun setPlayer(player: Player, orientation: Int) {
        playerListener?.let { this.player?.removeListener(it) }
        this.player = player
        initializeMediaPlayer(orientation)
    }

    fun releasePlayer() {
        playerListener?.let { this.player?.removeListener(it) }
        player = null
    }

    fun play() = player?.play()
    fun pause() = player?.pause()

    fun setOnIsLoading(listener: (Boolean) -> Unit) { onLoadingListener = listener }
    fun setOnPlayingListener(listener: (Boolean) -> Unit) { onPlayingListener = listener }
    fun setOnPreparedListener(listener: (Player) -> Unit) { onPreparedListener = listener }
    fun setOnReadyListener(listener: (Player) -> Unit) { onReadyListener = listener }
    fun setOnErrorListener(listener: (PlaybackException) -> Unit) { onErrorListener = listener }
    fun setOnBufferingUpdateListener(listener: (Player, Long) -> Unit) { onBufferingUpdateListener = listener }
    fun setOnSeekCompleteListener(listener: (Player) -> Unit) { onSeekCompleteListener = listener }
    fun setOnCompletionListener(listener: () -> Unit) { onCompletionListener = listener }
    fun setOnInfoListener(listener: (Player, Int, Int) -> Unit) { onInfoListener = listener }

    private fun initializeMediaPlayer(orientation: Int) {
        player?.let { p ->
            p.setVideoSurfaceView(this)
            p.setAudioAttributes(audioAttributes, true)

            playerListener = object : Player.Listener {

                override fun onPlayerError(error: PlaybackException) {
                    onErrorListener?.invoke(error)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            onReadyListener?.invoke(p)
                            Log.d("OwnMediaPlayer", "STATE_READY")
                            if (!isSurfaceReady) {
                                isSurfaceReady = true
                                onPreparedListener?.invoke(p)
                                updateTextureViewSize(orientation)
                                playBackPosition?.let { p.seekTo(it) }
                            }
                        }
                        Player.STATE_ENDED -> {
                            Log.d("OwnMediaPlayer", "STATE_ENDED")
                            onCompletionListener?.invoke()
                        }
                        Player.STATE_BUFFERING -> {
                            Log.d("OwnMediaPlayer", "STATE_BUFFERING")
                            onBufferingUpdateListener?.invoke(p, p.bufferedPosition)
                        }
                        Player.STATE_IDLE -> {
                            Log.d("OwnMediaPlayer", "STATE_IDLE")
                        }
                    }
                }

                override fun onIsLoadingChanged(isLoading: Boolean) {
                    Log.d("OwnMediaPlayer", "ISLOADING_CHANGED $isLoading")
                    onLoadingListener?.invoke(isLoading)
                }

                override fun onRenderedFirstFrame() {
                    Log.d("OwnMediaPlayer", "ONRENDERED_FIRST_FRAME")
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    onPlayingListener?.invoke(isPlaying)
                    Log.d("OwnMediaPlayer", "ISPLAYING_CHANGED $isPlaying")
                }
            }

            playerListener?.let { p.addListener(it) }
        }
    }

    private fun updateTextureViewSize(orientation: Int) {
        val player = this.player ?: return
        val videoWidth = player.videoSize.width.takeIf { it > 0 } ?: return
        val videoHeight = player.videoSize.height.takeIf { it > 0 } ?: return

        val videoAspectRatio = videoWidth.toFloat() / videoHeight.toFloat()

        val viewWidth = width
        val viewHeight = height

        Log.d("VIEWS_HA","VIDEOWIDTH=$videoWidth \n VIDEOHEIGHT=$videoHeight \n VIEWWIDTH = $viewWidth \n VIEWHEIGHT = $viewHeight")

        val isLandScape = isLandscape(orientation)

        val expectedWidth: Int
        val expectedHeight: Int

        if (!isLandScape) {
            expectedHeight = (viewWidth / videoAspectRatio).toInt().coerceAtMost(viewHeight)
            expectedWidth = viewWidth
        } else {
            expectedWidth = (viewHeight * videoAspectRatio).toInt().coerceAtMost(viewWidth)
            expectedHeight = viewHeight
        }

        val layoutParams = layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width = expectedWidth
        layoutParams.height = expectedHeight

        // Centrar el SurfaceView dentro del contenedor
        val horizontalMargin = (viewWidth - expectedWidth) / 2
        val verticalMargin = (viewHeight - expectedHeight) / 2

        layoutParams.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin)

        setLayoutParams(layoutParams)
        requestLayout()
    }



    private fun isLandscape(orientation: Int): Boolean = orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
}
