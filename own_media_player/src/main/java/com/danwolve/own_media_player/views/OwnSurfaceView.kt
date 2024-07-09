package com.danwolve.own_media_player.views

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player

internal class OwnSurfaceView : SurfaceView,
    SurfaceHolder.Callback{

    private lateinit var player : Player
    private lateinit var surface: Surface
    private var audioSeassisonId : Int = 0

    private lateinit var audioManager : AudioManager
    private var audioAttributes : AudioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private var currentOrientation : Int = OwnMediaPlayer.PORTRAIT

    private var onReadyListener: ((Player) -> Unit)? = null
    private var onCompletitionListener: ((Player) -> Unit)? = null
    private var onErrorListener: ((Player,PlaybackException) -> Unit)? = null
    private var onBufferingUpdateListener: ((Player,Long) -> Unit)? = null
    private var onSeekCompleteListener: ((p:Player) -> Unit)? = null
    private var onInfoListener: ((Player,Int,Int) -> Unit)? = null

    init {
        holder.addCallback(this)
    }

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
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            setSurfaceLifecycle(SURFACE_LIFECYCLE_FOLLOWS_ATTACHMENT)
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun setPlayer(player: Player) {
        this.player = player
        if(this::surface.isInitialized)
            initializeMediaPlayer()
    }

    fun getAudioSessionId() = audioSeassisonId

    fun play() = player.play()
    fun pause() = player.pause()
    fun setOnReadyListener(listener : (Player) -> Unit){ onReadyListener = listener }
    fun setOnErrorListener(listener : (Player,PlaybackException) -> Unit){ onErrorListener = listener }
    fun setOnBufferingUpdateListener(listener : (Player,Long) -> Unit){ onBufferingUpdateListener = listener }
    fun setOnSeekCompleteListener(listener : (Player) -> Unit){ onSeekCompleteListener = listener }
    fun setOnCompletitionListener(listener : (Player) -> Unit){ onCompletitionListener = listener }
    fun setOnInfoListener(listener : (p : Player,what : Int,extra : Int) -> Unit){ onInfoListener = listener }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        when(newConfig?.orientation){
            Configuration.ORIENTATION_PORTRAIT-> currentOrientation = OwnMediaPlayer.PORTRAIT
            Configuration.ORIENTATION_LANDSCAPE-> currentOrientation = OwnMediaPlayer.FULLSCREEN
        }
    }

    private fun initializeMediaPlayer() {
        player.setVideoSurface(surface)
        player.setAudioAttributes(audioAttributes,true)
        player.addListener(object : Player.Listener{
            override fun onPlayerError(error: PlaybackException) {
                onErrorListener?.invoke(player,error)
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                super.onIsLoadingChanged(isLoading)
                Log.d("OwnMediaPlayer","ISLOADING_CHANGED $isLoading")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when(playbackState){
                    Player.STATE_READY->{
                        Log.d("OwnMediaPlayer","STATE_READY")
                        onReadyListener?.invoke(player)
                        updateTextureViewSize()
                    }
                    Player.STATE_ENDED->{
                        Log.d("OwnMediaPlayer","STATE_ENDED")
                        onCompletitionListener?.invoke(player)
                    }
                    Player.STATE_IDLE-> {
                        Log.d("OwnMediaPlayer","STATE_IDLE")
                    }
                    Player.STATE_BUFFERING-> {
                        Log.d("OwnMediaPlayer","STATE_BUFFERING")
                        onBufferingUpdateListener?.invoke(player,player.bufferedPosition)
                    }
                }

                super.onPlaybackStateChanged(playbackState)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                Log.d("OwnMediaPlayer","ISPLAYING_CHANGED $isPlaying")
            }
        })
        /*mediaPlayer = MediaPlayer().apply {
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
        mediaPlayer.prepareAsync()*/
    }

    private fun updateTextureViewSize() {
        if(currentOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT){
            val videoWidth = player.videoSize.width.toFloat()
            val videoHeight = player.videoSize.height.toFloat()
            val viewWidth = width.toFloat()

            val newHeightVideo = viewWidth * videoHeight / videoWidth
            layoutParams.height = newHeightVideo.toInt()
            requestLayout()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        this.surface = holder.surface
        if(this::player.isInitialized){
            initializeMediaPlayer()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if(::player.isInitialized)
            player.release()
    }

}