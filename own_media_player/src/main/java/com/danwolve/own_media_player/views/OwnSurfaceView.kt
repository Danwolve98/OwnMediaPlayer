package com.danwolve.own_media_player.views

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player

internal class OwnSurfaceView : SurfaceView{

    private lateinit var player : Player
    private var audioSeassisonId : Int = 0
    private var playBackPosition : Long? = null

    private lateinit var audioManager : AudioManager
    private var audioAttributes : AudioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private var isSurfaceReady = false

    private var onPreparedListener: ((Player) -> Unit)? = null
    private var onReadyListener: ((Player) -> Unit)? = null
    private var onCompletitionListener: ((Player) -> Unit)? = null
    private var onErrorListener: ((Player,PlaybackException) -> Unit)? = null
    private var onBufferingUpdateListener: ((Player,Long) -> Unit)? = null
    private var onSeekCompleteListener: ((p:Player) -> Unit)? = null
    private var onInfoListener: ((Player,Int,Int) -> Unit)? = null

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

    fun setPlayer(player: Player,orientation : Int) {
        this.player = player
        initializeMediaPlayer(playBackPosition,orientation)
    }

    fun getAudioSessionId() = audioSeassisonId
    fun play() = player.play()
    fun pause() = player.pause()
    fun setOnPreparedListener(listener : (Player) -> Unit){ onPreparedListener = listener }
    fun setOnReadyListener(listener : (Player) -> Unit){ onReadyListener = listener }
    fun setOnErrorListener(listener : (Player,PlaybackException) -> Unit){ onErrorListener = listener }
    fun setOnBufferingUpdateListener(listener : (Player,Long) -> Unit){ onBufferingUpdateListener = listener }
    fun setOnSeekCompleteListener(listener : (Player) -> Unit){ onSeekCompleteListener = listener }
    fun setOnCompletitionListener(listener : (Player) -> Unit){ onCompletitionListener = listener }
    fun setOnInfoListener(listener : (p : Player,what : Int,extra : Int) -> Unit){ onInfoListener = listener }

    private fun initializeMediaPlayer(playBackPosition : Long?,orientation: Int) {
        player.setVideoSurfaceView(this)
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
                        onReadyListener?.invoke(player)
                        Log.d("OwnMediaPlayer","STATE_READY")
                        if(!isSurfaceReady){
                            isSurfaceReady = true
                            onPreparedListener?.invoke(player)
                            updateTextureViewSize(orientation)
                            playBackPosition?.let { player.seekTo(it) }
                        }
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
            }

            override fun onRenderedFirstFrame() {
                Log.d("OwnMediaPlayer","ONRENDERER_FIRST_FRAME")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d("OwnMediaPlayer","ISPLAYING_CHANGED $isPlaying")
            }
        })
    }

    private fun updateTextureViewSize(orientation: Int) {
        val videoWidth = player.videoSize.width.toFloat()
        val videoHeight = player.videoSize.height.toFloat()

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        if(!isLandscape(orientation)){
            var newHeightVideo = viewWidth * videoHeight / videoWidth

            if(newHeightVideo > viewHeight*0.75f){ //PORCENTAJE MAXIMO DE ALTURA PARA EL VIDEO 75% SOBRE LA ALTURA DEL MÓVIL
                newHeightVideo = viewHeight * 0.75f
            }

            layoutParams.height = newHeightVideo.toInt()
        }else if(viewHeight<videoHeight){
            val newWidthVideo = viewHeight * videoWidth / videoHeight
            layoutParams.width = newWidthVideo.toInt()
        }
        requestLayout()
    }

    private fun isLandscape(orientation: Int) = orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

}