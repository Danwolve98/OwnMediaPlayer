package com.danwolve.own_media_player.views

import android.app.Activity
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.MediaPlayer
import android.net.Uri
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.danwolve.own_media_player.R
import com.danwolve.own_media_player.databinding.CustomMediaPlayerBinding
import com.danwolve.own_media_player.extensions.animate
import com.danwolve.own_media_player.extensions.invisible
import com.danwolve.own_media_player.extensions.notNull
import com.danwolve.own_media_player.extensions.playAnimation
import com.danwolve.own_media_player.extensions.visible
import com.danwolve.own_media_player.service.MediaService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

/**
 * Clase creada para la reproducción de videos, es automático, lo único obligatorio a implementar es:
 * 1. Asignar la vista en el xml donde quieras ver el video
 * 2. En código acceder a esta vista y llamar a [setVideoUrl] o [setVideoPath]
 *
 * NOTA: Para dejar a [OwnMediaPlayer] tener el control a la hora de girar la pantalla importante especificar en la Actividad en el "AndroidManifest"
 * android:configChanges="orientation|screenSize"
 */
class OwnMediaPlayer @JvmOverloads constructor (
    contexto: Context,
    private val atributeSet : AttributeSet? = null)
    : FrameLayout(contexto,atributeSet), DefaultLifecycleObserver {

    companion object{
        private const val TAG = "OwnMediaPlayer"
        private const val MEDIA_SESSION_ID = "MEDIA_SESSION"

        const val MUTE = -1
        const val UNMUTE = 0

        const val PAUSED = -1
        const val PLAYING = 0
        const val FINISHED = 1
    }

    //PRO
    private var controllerFuture : ListenableFuture<MediaController>?  = null
    private var mediaController : MediaController? = null
    private var useLegacy = false
    private lateinit var mediaSessionServiceIntent : Intent
    //  BASICS
    private var activity: Activity? = null
    private lateinit var binding : CustomMediaPlayerBinding
    private lateinit var player : Player
    private lateinit var progressRunnable: Runnable

    //NOTIFICACIONES
    private var hasNoti = false
    private var titleNoti : String? = null
    private var authorNoti : String? = null
    private var photoNoti : Any? = null

    private lateinit var fullScreenCallBack : () -> Unit

    private lateinit var windowInsetsControllerCompat: WindowInsetsControllerCompat

    private val handlerProgress by lazy { Handler(Looper.getMainLooper()) }

    private val countDownTimer by lazy { object : CountDownTimer(4000,1000){
        override fun onTick(millisUntilFinished: Long) {}
        override fun onFinish() {
            hideOwnMediaPlayer()
            isHidden = true
        }
    }}

    private val videoClickListener = OnClickListener {
        if(isHidden){
            showOwnMediaPlayer()
        }
        else
            hideOwnMediaPlayer()
    }

    //  PARCELABLES
    private var videoProgress : Long? = null
    private var videoUrl : String? = null
    private var videoPath : String? = null
    private val orientation by lazy { activity?.requestedOrientation }
    private var isMuted = UNMUTE
    private var isPlaying = PLAYING

    //  PROPERTIES
    private var isHidden : Boolean = false
    /**
     * Indica si el video está a pantalla completa
     */


    // MIRAR PULSAR EN CUALQUIER ZONA RESETEAR TIMER, BOTON PANTALLA COMPLETA Y ESTILO DE LOS BOTONES
    init {
        init()
    }

    private fun init(){
        getAtributes()
        mediaSessionServiceIntent = Intent(context,MediaService::class.java)
        activity = getActivity()
        createView()
    }

    private fun getAtributes(){
        context.theme.obtainStyledAttributes(atributeSet,R.styleable.OwnMediaPlayer,0,0).run {
            try {
                hasNoti = getBoolean(R.styleable.OwnMediaPlayer_hasNotification,false)
                if(hasNoti){
                    titleNoti = getString(R.styleable.OwnMediaPlayer_titleNotification)
                    authorNoti = getString(R.styleable.OwnMediaPlayer_authorNotification)
                    photoNoti = getResourceId(R.styleable.OwnMediaPlayer_photoNotification,R.drawable.own_media_player)
                }
            }finally {
                recycle()
            }
        }
    }

    /**
     * Infla la vista
     */
    private fun createView(){
        binding = CustomMediaPlayerBinding.inflate(LayoutInflater.from(context),this,true)
        countDownTimer.start()
        hideOwnMediaPlayer(0f)
        if(orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            applyMargins()
    }

    private fun applyMargins(){
        ViewCompat.setOnApplyWindowInsetsListener(binding.ownSurfaceView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val insetApply = if(insets.left > insets.right) insets.left else insets.right
            v.updateLayoutParams<MarginLayoutParams> {
                updateMargins(getMargin(windowInsets.displayCutout?.safeInsetLeft?.plus(insetApply)),
                    0,
                    getMargin(windowInsets.displayCutout?.safeInsetLeft?.plus(insetApply)),
                    0)
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun getMargin(margin : Int?) =
        if(margin == null || margin == 0)
            resources.getDimension(R.dimen.min_margin).toInt()
        else
            margin


    /**
     * Cleans the [progressRunnable] if is is initialized
     */
    private fun cleanProgressRunnable(){
        if(this@OwnMediaPlayer::progressRunnable.isInitialized)
            handlerProgress.removeCallbacks(progressRunnable)
    }

    /**
     * Oculta la interfaz del video con una animación
     */
    private fun hideOwnMediaPlayer(duration : Float = 1f){
        isHidden = true
        with(binding){
            seekBar.animate(duracion = 0.4f*duration, alpha = 0f, y = -100f){ it.invisible() }
            btPlusTenSeconds.animate(duracion = 0.25f*duration, alpha = 0f,x=-50f){it.invisible()}
            btLessTenSeconds.animate(duracion = 0.25f*duration, alpha = 0f,x=50f){it.invisible()}
            btMute.animate(duracion = 0.25f*duration, alpha = 0f){it.invisible()}
            btFullScreenVideo.animate(duracion = 0.25f*duration, alpha = 0f){it.invisible()}
            tvTotalVideo.animate(duracion = 0.25f*duration, alpha = 0f){it.invisible()}
            tvCurrentVideo.animate(duracion = 0.25f*duration, alpha = 0f){it.invisible()}
            btPlayPause.animate(duracion = 0.15f*duration,alpha=0f, y = 100f, initVisible = true){it.invisible()}
        }
    }

    /**
     * Muestra la interfaz del video con una animación
     */
    private fun showOwnMediaPlayer(duration : Float = 1f){
        isHidden = false
        with(binding){
            seekBar.animate(duracion = 0.3f*duration, alpha = 1f,y = 0f, initVisible = true)
            btPlusTenSeconds.animate(duracion = 0.15f*duration, alpha = 1f,x=0f, initVisible = true)
            btLessTenSeconds.animate(duracion = 0.15f*duration, alpha = 1f,x=0f, initVisible = true)
            btMute.animate(duracion = 0.15f*duration, alpha = 1f, initVisible = true)
            btFullScreenVideo.animate(duracion = 0.15f*duration, alpha = 1f, initVisible = true)
            tvTotalVideo.animate(duracion = 0.15f*duration, alpha = 1f, initVisible = true)
            tvCurrentVideo.animate(duracion = 0.15f*duration, alpha = 1f, initVisible = true)
            btPlayPause.animate(duracion = 0.15f*duration,alpha=1f, y = 0f, initVisible = true)
        }
        countDownTimer.start()
    }

    /**
     * Formatea los milisegundos al formato [00:00]
     */
    private fun formatDuration(durationInMilis : Long) : String{
        val totalSeconds = durationInMilis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d", minutes,seconds)
    }

    fun setNotification(title : String?, author : String?, photo : String?){
        hasNoti = true
        this.titleNoti = title
        this.authorNoti = author
        this.photoNoti = photo ?: R.drawable.own_media_player
    }

    /**
     * Asigna la url del video a mostrar
     */
    fun setVideoUrl(urlVideo : String,autoPlay : Boolean = true){
        videoPath = null
        videoUrl = urlVideo
        Log.e("OwnMediaPlayer","VideoUrl: $videoUrl")
        if(hasNoti)
            prepareNotificationMedia()
        else
            prepareExoPlayer(autoPlay)
    }

    private fun prepareExoPlayer(autoPlay: Boolean){
        val exoPlayer = ExoPlayer.Builder(context).build()
        assignPlayer(exoPlayer)
        val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if(isPlaying == PLAYING)
            exoPlayer.play()
    }

    /**
     * Asigna la dirección del video a mostrar
     */
    fun setVideoPath(path : String,autoPlay : Boolean = true) {
        videoUrl = null
        videoPath = path
        //configUI(autoPlay)
    }

    fun setLegacy(useLegacy : Boolean){
        this.useLegacy = useLegacy
    }

    /**
     * Función para que funcione con ajustes de configuración en Manifest
     */
    /*override fun onConfigurationChanged(newConfig: Configuration?) {
        when(newConfig?.orientation){
            Configuration.ORIENTATION_PORTRAIT-> orientation = PORTRAIT
            Configuration.ORIENTATION_LANDSCAPE-> orientation = FULLSCREEN
        }

        cleanProgressRunnable()
        removeAllViews()
        init()
        configUI(player)
    }*/

    /**
     * Prepara el video para ser ejecutado
     */
    private fun assignListeners(){
        with(binding){
            seekBar.setOnTouchListener { v, event ->
                when(event.action){
                    MotionEvent.ACTION_DOWN->{
                        countDownTimer.cancel()
                        false
                    }
                    MotionEvent.ACTION_UP->{
                        countDownTimer.start()
                        false
                    }
                    else->false
                }
            }

            btMute.isChecked = isMuted == MUTE
            btFullScreenVideo.isChecked = orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            btPlayPause.isChecked = isPlaying == PAUSED

            ownSurfaceView.run {
                //PREPARED
                setOnPreparedListener {
                    createProgressRunnable()
                    showOwnMediaPlayer()
                    assignOwnMediaController(it)
                    binding.btPlayPause.icon = AppCompatResources.getDrawable(context,R.drawable.play_pause_selector)
                }
                //BUFFER
                setOnBufferingUpdateListener{player: Player, l: Long ->
                    loading.visible()
                    binding.seekBar.secondaryProgress = l.toInt()
                }

                //READY
                setOnReadyListener {
                    loading.invisible()
                }

                setOnErrorListener {player,playbackException->
                    Toast.makeText(context, playbackException.message, Toast.LENGTH_SHORT).show()
                }

                setOnCompletitionListener {
                    finish()
                    binding.btPlayPause.icon = AppCompatResources.getDrawable(context,R.drawable.ic_refresh)
                    binding.btPlayPause.setOnClickListener {
                        playPauseClick(true)
                    }
                }
            }

        }
    }

    private fun finish(){
        isPlaying = FINISHED
        countDownTimer.cancel()
        binding.run {
            btPlayPause.visible()
            loading.invisible()
            btMute.invisible()
            btLessTenSeconds.invisible()
            btPlusTenSeconds.invisible()
            btFullScreenVideo.invisible()
            seekBar.invisible()
            tvCurrentVideo.invisible()
            tvTotalVideo.invisible()
            ownSurfaceView.setOnClickListener {}
        }
    }

    /**
     * Asigna todas las funcionalidades del [Player] a las vistas
     */
    private fun assignOwnMediaController(player: Player){
        with(binding){
            //  MUTE
            if(isMuted == MUTE) player.volume = 1f
            //  FULL SCREEN
            activity.notNull {activity->
                btFullScreenVideo.setOnClickListener {
                    if(::fullScreenCallBack.isInitialized)
                        fullScreenCallBack()

                    activity.requestedOrientation = if(orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    else
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }

            //  MUTE
            btMute.setOnClickListener {
                countDownTimer.start()
                isMuted = if(btMute.isChecked){
                    player.volume = 0f
                    MUTE
                } else{
                    player.volume = 1f
                    UNMUTE
                }
            }

            // PLAY-PAUSE
            if(isPlaying == PAUSED) player.pause()
            btPlayPause.setOnClickListener{ playPauseClick() }

            //  PLUS-LESS
            btPlusTenSeconds.setOnClickListener {
                countDownTimer.start()
                it.playAnimation(R.anim.click_animation)
                player.seekTo(player.currentPosition+10000L)
            }

            btLessTenSeconds.setOnClickListener {
                countDownTimer.start()
                it.playAnimation(R.anim.click_animation)
                player.seekTo(player.currentPosition-10000L)
            }

            //HIDE-SHOW
            binding.ownSurfaceView.setOnClickListener(videoClickListener)

            //  PROGRESS
            tvTotalVideo.text = formatDuration(player.duration)
            seekBar.max = player.duration.toInt()

            //createProgressRunnable()

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if(!fromUser || isPlaying == FINISHED)
                        return
                    player.seekTo(progress.toLong())
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            videoProgress.notNull {
                player.seekTo(it)
            }
        }
    }

    private fun playPauseClick(reset : Boolean = false){
        if(reset)
            reset()
        isPlaying = if(binding.btPlayPause.isChecked){
            player.pause()
            PAUSED
        }else{
            player.play()
            PLAYING
        }
    }

    private fun reset(){
        binding.btPlayPause.run {
            icon = AppCompatResources.getDrawable(context,R.drawable.play_pause_selector)
            isChecked = false
            setOnClickListener { playPauseClick() }
        }
        binding.ownSurfaceView.setOnClickListener(videoClickListener)
        showOwnMediaPlayer()
    }

    /**
     * Devuelve la primera actividad que encuentra
     */
    private fun getActivity(): Activity? {
        var context = context
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

    /**
     * Desabilita el fullScreen del botón
     */
    fun disableFullScreenButton(){ binding.btFullScreenVideo.invisible() }

    /**
     * Habilita el fullScreen del botón
     */
    fun ableFullScreenButton(){ binding.btFullScreenVideo.visible() }

    private fun getScreenConfigs(): List<Int> {
        return ActivityInfo::class.java.declaredFields
            .filter { it.name.startsWith("SCREEN_ORIENTATION") }
            .map { it.getInt(null) }
    }

    /**
     * Crea un [Runnable] que va a ir actualizando los textos y el [SeekBar] conforme avance el video
     */
    private fun createProgressRunnable(){
        progressRunnable = Runnable {
            val currentPosition = player.currentPosition
            videoProgress = currentPosition
            binding.tvCurrentVideo.text = formatDuration(currentPosition)
            binding.seekBar.progress = currentPosition.toInt()
            handlerProgress.postDelayed(progressRunnable,100)
        }
        handlerProgress.post(progressRunnable)
    }

    /**
     * Saber si actualmente el video se está reproduciendo
     */
    fun isPlaying() : Boolean =
        if(!this::player.isInitialized)
            false
        else
            player.isPlaying

    internal fun setFullScreenCallBack(callBack : ()->Unit) {
        fullScreenCallBack = callBack
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        videoProgress.notNull { savedState.progressVideo = it }
        videoUrl.notNull { savedState.videoUrl = it }
        videoPath.notNull { savedState.videoPath = it }
        isMuted.notNull { savedState.isMuted = it }
        isPlaying.notNull { savedState.isPlaying = it }
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            videoProgress = state.progressVideo
            state.videoUrl.notNull { /*setVideoUrl(it,true)*/ }
            state.videoPath.notNull { setVideoPath(it) }
            state.isMuted.notNull { isMuted = it }
            state.isPlaying.notNull { isPlaying = it }
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    /**
     * Clase para guardar los estados del video
     */
    private class SavedState : BaseSavedState {
        var videoUrl : String? = null
        var videoPath : String? = null
        var progressVideo : Long = 0L
        var isMuted : Int = UNMUTE
        var isPlaying : Int = PLAYING
        var startOrientation : Int = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        constructor(superState: Parcelable?) : super(superState)
        constructor(parcel: Parcel) : super(parcel) {
            progressVideo = parcel.readLong()
            videoUrl = parcel.readString()
            videoPath = parcel.readString()
            isMuted = parcel.readInt()
            isPlaying = parcel.readInt()
            startOrientation = parcel.readInt()
        }
        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeLong(progressVideo)
            out.writeString(videoUrl)
            out.writeString(videoPath)
            out.writeInt(isMuted)
            out.writeInt(isPlaying)
            out.writeInt(startOrientation)
        }
        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cleanProgressRunnable()
    }

    internal fun setWindowInsetsController(windowInsetsControllerCompat: WindowInsetsControllerCompat){
        this.windowInsetsControllerCompat = windowInsetsControllerCompat
    }

    private fun prepareNotificationMedia(){
        val sessionToken = SessionToken(context, ComponentName(context, MediaService::class.java))

        controllerFuture = MediaController.Builder(context,sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            mediaController.notNull { startMediaController(it) }
        }, MoreExecutors.directExecutor())
    }

    private fun startMediaController(mediaController: MediaController){
        mediaController.run {
            assignPlayer(this)

            val mediaItem = MediaItem.Builder()
                .setMediaId(MEDIA_SESSION_ID)
                .setUri(videoUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(titleNoti ?: "")
                        .setArtist(authorNoti ?: "")
                        .setArtworkUri(getPhotoUri())
                        .build()
                ).build()

            setMediaItem(mediaItem)
            prepare()
            if(this@OwnMediaPlayer.isPlaying == PLAYING)
                play()
        }
    }

    private fun getPhotoUri() : Uri =
        if(photoNoti is Int)
            Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE).path((photoNoti as Int).toString()).build()
        else
            Uri.parse(photoNoti as String)

    private fun assignPlayer(player: Player){
        this.player = player
        assignListeners()
        binding.ownSurfaceView.setPlayer(player,orientation ?: ActivityInfo.SCREEN_ORIENTATION_SENSOR)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
            mediaController == null
        }
        controllerFuture = null
        activity?.stopService(mediaSessionServiceIntent)
    }
}

