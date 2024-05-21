package com.danwolve.own_media_player.views

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.widget.Toast

internal class OwnTextureView : TextureView,
    TextureView.SurfaceTextureListener{

    private lateinit var mediaPlayer : MediaPlayer
    private lateinit var videoUrl : String
    private lateinit var surface: Surface
    private var audioSeassisonId : Int = 0

    private lateinit var audioManager : AudioManager
    private var audioAttributes : AudioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()

    private var currentOrientation : Int = OwnMediaPlayer.PORTRAIT

    private var onPreparedListener: ((MediaPlayer) -> Unit)? = null
    private var onCompletitionListener: ((MediaPlayer) -> Unit)? = null
    private var onErrorListener: ((MediaPlayer) -> Unit)? = null
    private var onBufferingUpdateListener: ((MediaPlayer,Int) -> Unit)? = null
    private var onSeekCompleteListener: ((mp:MediaPlayer) -> Unit)? = null
    private var onInfoListener: ((MediaPlayer,Int,Int) -> Unit)? = null

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize()
    }

    private fun initialize() {
        surfaceTextureListener = this
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun setVideo(video: String) {
        videoUrl = video
        if(this::surface.isInitialized)
            initializeMediaPlayer()
    }

    fun getAudioSessionId() = audioSeassisonId

    fun play() = mediaPlayer.start()
    fun pause() = mediaPlayer.pause()
    fun setOnPreparedListener(listener : (MediaPlayer) -> Unit){ onPreparedListener = listener }
    fun setOnErrorListener(listener : (MediaPlayer) -> Unit){ onErrorListener = listener }
    fun setOnBufferingUpdateListener(listener : (MediaPlayer,Int) -> Unit){ onBufferingUpdateListener = listener }
    fun setOnSeekCompleteListener(listener : (MediaPlayer) -> Unit){ onSeekCompleteListener = listener }
    fun setOnCompletitionListener(listener : (MediaPlayer) -> Unit){ onCompletitionListener = listener }
    fun setOnInfoListener(listener : (mp:MediaPlayer,what:Int,extra:Int) -> Unit){ onInfoListener = listener }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        when(newConfig?.orientation){
            Configuration.ORIENTATION_PORTRAIT-> currentOrientation = OwnMediaPlayer.PORTRAIT
            Configuration.ORIENTATION_LANDSCAPE-> currentOrientation = OwnMediaPlayer.FULLSCREEN
        }
    }


    private fun initializeMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setSurface(surface)
            setAudioAttributes(audioAttributes)
            setDataSource(videoUrl)
            setOnPreparedListener {
                it.setVolume(1f,1f)
                onPreparedListener?.invoke(it)
                updateTextureViewSize()
            }
            setOnErrorListener { mp, what, extras ->
                onErrorListener?.invoke(mp)
                Toast.makeText(context, "WHAT: $what, EXTRAS: $extras", Toast.LENGTH_SHORT).show()
                false
            }
            setOnSeekCompleteListener { onSeekCompleteListener?.invoke(it) }
            setOnBufferingUpdateListener { mp, percent -> onBufferingUpdateListener?.invoke(mp,percent) }
            setOnCompletionListener{ onCompletitionListener?.invoke(it) }
            setOnInfoListener { mp, what, extra ->
                onInfoListener?.invoke(mp,what,extra)
                true
            }
        }
        mediaPlayer.prepareAsync()
    }

    private fun updateTextureViewSize() {
        if(currentOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT){
            val videoWidth = mediaPlayer.videoWidth.toFloat()
            val videoHeight = mediaPlayer.videoHeight.toFloat()
            val viewWidth = width.toFloat()

            val newHeightVideo = viewWidth * videoHeight / videoWidth
            layoutParams.height = newHeightVideo.toInt()
            requestLayout()
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        this.surface = Surface(surface)
        if(this::videoUrl.isInitialized){
            initializeMediaPlayer()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        if(::mediaPlayer.isInitialized)
            mediaPlayer.release()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

}