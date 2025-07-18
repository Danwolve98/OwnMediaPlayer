package com.danwolve.own_media_player.views

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.CountDownTimer
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.RawRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.danwolve.own_media_player.R
import com.danwolve.own_media_player.databinding.CustomMediaPlayerBinding
import com.danwolve.own_media_player.extensions.AnimParams
import com.danwolve.own_media_player.extensions.animate
import com.danwolve.own_media_player.extensions.invisible
import com.danwolve.own_media_player.extensions.playAnimation
import com.danwolve.own_media_player.extensions.visible
import com.danwolve.own_media_player.service.MediaService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Clase creada para la reproducción de videos, es automático, lo único obligatorio a implementar es:
 * 1. Asignar la vista en el xml donde quieras ver el video
 * 2. En código acceder a esta vista y llamar a [setVideoUrl] o [setVideoUri]
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
    internal lateinit var binding : CustomMediaPlayerBinding
    private lateinit var player : Player
    private var exoPlayer : ExoPlayer? = null

    //NOTIFICACIONES
    private var hasNoti = false
    private var titleNoti : String? = null
    private var authorNoti : String? = null
    private var photoNoti : Any? = null

    private var fullScreen : Boolean = false

    private var closeCallBack : (()->Unit)? = null
    private var fullScreenCallBack : (() -> Unit)? = null
    private var onShowCallBack : (() -> Unit)? = null
    private var onHideCallBack : (() -> Unit)? = null

    private var showFullScreenButton = true

    private lateinit var windowInsetsControllerCompat: WindowInsetsControllerCompat
    /**
     * Habilita o desabilita el uso de controles para el video
     */
    private var mediaControlsEnabled = true
        set(value) {
            field = value
            if(::binding.isInitialized.not())
                return
            binding.run {
                btPlayPause.visible(value)
                btMute.visible(value)
                btFullScreenVideo.visible(value && showFullScreenButton)
                btClose.visible(value && closeCallBack != null)
                seekBar.visible(value)
                btLessTenSeconds.visible(value)
                tvCurrentVideo.visible(value)
                tvTotalVideo.visible(value)
            }
            if(value)
                setSurfaceViewClickListener(videoClickListener)
            else
                setSurfaceViewClickListener(null)
        }

    private fun setSurfaceViewClickListener(clickListener: OnClickListener?){
        binding.ownSurfaceView.setOnClickListener(clickListener)
        binding.root.setOnClickListener(clickListener)
    }

    private val countDownTimer by lazy { object : CountDownTimer(4000,1000){
        override fun onTick(millisUntilFinished: Long) {
            //NADA
        }
        override fun onFinish() {
            hideOwnMediaPlayer()
            isHidden = true
        }
    }}

    private val videoClickListener = OnClickListener {
        if(isHidden)
            showOwnMediaPlayer()
        else
            hideOwnMediaPlayer()
    }

    //  PARCELABLES
    private var videoProgress : Long? = null
    private var videoUrl : String? = null
    private var videoUri : Uri? = null
    private val orientation by lazy { activity?.requestedOrientation }
    private var isMuted = UNMUTE
    private var isPlaying = PLAYING

    //  PROPERTIES
    private var isHidden : Boolean = false

    init {
        init()
    }

    private fun init(){
        getAtributes()
        mediaSessionServiceIntent = Intent(context,MediaService::class.java)
        activity = getActivity()
        createView()
    }

    private var autoLoop = false

    fun autoLoop(autoLoop: Boolean) {
        this.autoLoop = autoLoop
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

    private var bufferingJob : Job? = null
    private fun startBufferingJob(){
        bufferingJob = binding.root.findViewTreeLifecycleOwner()?.lifecycleScope?.launch{
            while (true) {
                if (player.bufferedPosition.toInt() >= binding.seekBar.max){
                    cancel()
                    bufferingJob = null
                }

                binding.seekBar.secondaryProgress = player.bufferedPosition.toInt()
                delay(1000)
            }
        }
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
     * Oculta la interfaz del video con una animación
     */
    private fun hideOwnMediaPlayer(duration : Float = 1f){
        onHideCallBack?.invoke()
        isHidden = true
        with(binding){
            if(closeCallBack != null)
                btClose.animate(duracion = 0.25f*duration, animParams = AnimParams(alpha = 0f),){it.invisible()}

            vBlack.animate(duracion = 0.4f*duration, animParams = AnimParams(alpha = 0f)){ it.invisible() }
            seekBar.animate(duracion = 0.4f*duration, animParams = AnimParams(alpha = 0f, y = -100f)){ it.invisible() }
            btPlusTenSeconds.animate(duracion = 0.25f*duration, animParams = AnimParams(alpha = 0f, x = -50f)){it.invisible()}
            btLessTenSeconds.animate(duracion = 0.25f*duration, animParams = AnimParams(alpha = 0f, x = 50f)){it.invisible()}
            btMute.animate(duracion = 0.25f*duration, animParams = AnimParams(alpha = 0f)){it.invisible()}
            if(showFullScreenButton)
                btFullScreenVideo.animate(duracion = 0.25f*duration, animParams = AnimParams(alpha = 0f)){it.invisible()}
            tvTotalVideo.animate(duracion = 0.25f*duration, animParams = AnimParams(alpha = 0f)){it.invisible()}
            tvCurrentVideo.animate(duracion = 0.25f*duration, animParams = AnimParams(alpha = 0f)){it.invisible()}
            btPlayPause.animate(duracion = 0.15f*duration,animParams = AnimParams(alpha = 0f, y = 100f), initVisible = true){it.invisible()}
        }
    }

    /**
     * Muestra la interfaz del video con una animación
     */
    private fun showOwnMediaPlayer(duration : Float = 1f){
        onShowCallBack?.invoke()

        if(!mediaControlsEnabled)
            return

        isHidden = false
        with(binding){
            if(closeCallBack != null)
                btClose.animate(duracion = 0.15f*duration,animParams = AnimParams(alpha = 1f), initVisible = true)

            vBlack.animate(duracion = 0.3f*duration, animParams = AnimParams(alpha = 0.4f,y=0f), initVisible = true)
            seekBar.animate(duracion = 0.3f*duration, animParams = AnimParams(alpha = 1f,y=0f), initVisible = true)
            btPlusTenSeconds.animate(duracion = 0.15f*duration, animParams = AnimParams(alpha = 1f, x = 0f), initVisible = true)
            btLessTenSeconds.animate(duracion = 0.15f*duration, animParams = AnimParams(alpha = 1f, x = 0f), initVisible = true)
            btMute.animate(duracion = 0.15f*duration,animParams = AnimParams(alpha = 1f), initVisible = true)
            if(showFullScreenButton)
                btFullScreenVideo.animate(duracion = 0.15f*duration, animParams = AnimParams(alpha = 1f), initVisible = true)
            tvTotalVideo.animate(duracion = 0.15f*duration,animParams = AnimParams(alpha = 1f), initVisible = true)
            tvCurrentVideo.animate(duracion = 0.15f*duration, animParams = AnimParams(alpha = 1f), initVisible = true)
            btPlayPause.animate(duracion = 0.15f*duration,animParams = AnimParams(alpha = 1f, y = 0f), initVisible = true)
        }

        countDownTimer.start()
    }

    fun setFullScreen(fullScreen: Boolean){
        binding.ownSurfaceView.setFullScreen(fullScreen)
    }

    /**
     * Formatea los milisegundos al formato [00:00]
     */
    private fun formatDuration(durationInMilis : Long) : String{
        val totalSeconds = durationInMilis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return String.format(Locale.getDefault(),"%02d:%02d", minutes,seconds)
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
        videoUri = null
        videoUrl = urlVideo
        if(hasNoti)
            prepareNotificationMedia()
        else
            prepareExoPlayer(autoPlay)
    }

    /**
     * Asigna la url del video a mostrar
     */
    fun setVideoUri(uriVideo : Uri, autoPlay : Boolean = true){
        videoUri = uriVideo
        videoUrl = null
        if(hasNoti)
            prepareNotificationMedia()
        else
            prepareExoPlayer(autoPlay)
    }

    /**
     * Asigna el resource RAW del video a mostrar
     */
    fun setRawRes(@RawRes redId: Int,packageName : String,autoPlay: Boolean = true){
        videoUri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(packageName)
            .appendPath("$redId")
            .build()
        videoUrl = null

        if(hasNoti)
            prepareNotificationMedia()
        else
            prepareExoPlayer(autoPlay)
    }


    @OptIn(UnstableApi::class)
    private fun prepareExoPlayer(autoPlay: Boolean){
        exoPlayer = ExoPlayer.Builder(context)
            .setRenderersFactory(DefaultRenderersFactory(context).setEnableDecoderFallback(true))
            .build()

        exoPlayer?.let { assignPlayer(it) }

        val mediaItem = if(videoUrl != null)
            MediaItem.fromUri(Uri.parse(videoUrl))
        else if(videoUri != null)
            MediaItem.fromUri(videoUri!!)
        else
            MediaItem.fromUri(Uri.parse(""))

        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        if(isPlaying == PLAYING)
            exoPlayer?.play()
    }

    fun setLegacy(useLegacy : Boolean){
        this.useLegacy = useLegacy
    }

    fun setMediaControlsVisible(visible: Boolean){
        mediaControlsEnabled = visible
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

    private var resetAutoLoop = false
    /**
     * Prepara el video para ser ejecutado
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun assignListeners(){
        with(binding){
            seekBar.setOnTouchListener { _, event ->
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
                    showOwnMediaPlayer()
                    assignOwnMediaController(it)
                    binding.btPlayPause.icon = AppCompatResources.getDrawable(context,R.drawable.play_pause_selector)
                }

                //BUFFER
                setOnBufferingUpdateListener{_, _ ->
                    startBufferingJob()

                    if(resetAutoLoop)
                        resetAutoLoop = false
                    else
                        loading.visible()
                }

                //READY
                setOnReadyListener {
                    loading.invisible()
                }

                setOnErrorListener {playbackException->
                    Toast.makeText(context, playbackException.message, Toast.LENGTH_SHORT).show()
                }

                setOnCompletionListener {
                    if(autoLoop){
                        player.seekTo(0)
                        resetAutoLoop = true
                    }
                    else
                        finish()
                }

                setOnPlayingListener {
                    if(it)
                        startProgressJob()
                    else{
                        progressJob?.cancel()
                        progressJob = null
                    }
                }
            }

        }
    }

    private fun finish(){
        isPlaying = FINISHED

        showOwnMediaPlayer()
        countDownTimer.cancel()

        binding.run {
            //PLAY PAUSE
            btPlayPause.icon = AppCompatResources.getDrawable(context,R.drawable.ic_refresh)
            btPlayPause.setOnClickListener {
                playPauseClick(true)
            }
            btPlayPause.alpha = 1f

            tvCurrentVideo.text = formatDuration(player.duration)
            seekBar.progress = player.duration.toInt()
        }
    }

    fun setCloseCallBack(callBack: () -> Unit){
        closeCallBack = callBack
    }

    private val dPlayPause by lazy {
        AppCompatResources.getDrawable(this.context,R.drawable.play_pause_selector)
    }

    /**
     * Asigna todas las funcionalidades del [Player] a las vistas
     */
    private fun assignOwnMediaController(player: Player){
        with(binding){
            //  MUTE
            if(isMuted == MUTE) player.volume = 1f
            //  FULL SCREEN
            activity?.let {activity->
                btFullScreenVideo.setOnClickListener {
                    fullScreenCallBack?.invoke()
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

            //CLOSE
            btClose.setOnClickListener {
                closeCallBack?.invoke()
            }

            // PLAY-PAUSE
            if(isPlaying == PAUSED) player.pause()
            btPlayPause.setOnClickListener{ playPauseClick() }

            //  PLUS-LESS
            btPlusTenSeconds.setOnClickListener {
                val newPos = player.currentPosition.plus(10_000L).coerceAtMost(player.duration)
                countDownTimer.start()
                it.playAnimation(R.anim.click_animation)

                if(binding.seekBar.progress >= newPos)
                    return@setOnClickListener

                binding.tvCurrentVideo.text = formatDuration(newPos)
                binding.seekBar.progress = newPos.toInt()
                player.seekTo(newPos)
            }

            btLessTenSeconds.setOnClickListener {
                val newPos = player.currentPosition.minus(10_000L).coerceAtLeast(0)
                countDownTimer.start()
                it.playAnimation(R.anim.click_animation)

                if(binding.seekBar.progress == 0)
                    return@setOnClickListener

                binding.tvCurrentVideo.text = formatDuration(newPos)
                binding.seekBar.progress = newPos.toInt()
                player.seekTo(newPos)
            }

            //HIDE-SHOW
            if(mediaControlsEnabled)
                setSurfaceViewClickListener(videoClickListener)

            //  PROGRESS
            tvTotalVideo.text = formatDuration(player.duration)
            seekBar.max = player.duration.toInt()

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if(!fromUser)
                        return

                    binding.tvCurrentVideo.text = formatDuration(progress.toLong())
                    player.seekTo(progress.toLong())
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            videoProgress?.let {
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
        player.seekTo(0)
        binding.btPlayPause.run {
            icon = AppCompatResources.getDrawable(context,R.drawable.play_pause_selector)
            isChecked = false
            setOnClickListener { playPauseClick() }
        }
        setSurfaceViewClickListener(videoClickListener)
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
    fun showFullScreenButton(show: Boolean){
        showFullScreenButton = show
        binding.btFullScreenVideo.visible(show)
    }

    private fun getScreenConfigs(): List<Int> {
        return ActivityInfo::class.java.declaredFields
            .filter { it.name.startsWith("SCREEN_ORIENTATION") }
            .map { it.getInt(null) }
    }

    private var progressJob : Job? = null
    /**
     * Crea un [Job] que va a ir actualizando los textos y el [SeekBar] conforme avance el video
     */
    private fun startProgressJob(){
        progressJob = binding.root.findViewTreeLifecycleOwner()?.lifecycleScope?.launch{
            while (true){
                val currentPosition = player.currentPosition
                videoProgress = currentPosition
                binding.tvCurrentVideo.text = formatDuration(currentPosition)
                binding.seekBar.progress = currentPosition.toInt()
                delay(1000)
            }
        }
    }

    /**
     * Saber si actualmente el video se está reproduciendo
     */
    fun isPlaying() : Boolean =
        if(!this::player.isInitialized)
            false
        else
            player.isPlaying

    internal fun setShowCallBack(callBack : ()->Unit) {
        onShowCallBack = callBack
    }

    internal fun setHideCallback(callBack : ()->Unit) {
        onHideCallBack = callBack
    }

    internal fun setFullScreenCallBack(callBack : ()->Unit) {
        fullScreenCallBack = callBack
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.mediaControlsEnabled = if(mediaControlsEnabled) 1 else 0
        savedState.fullScreen = if(fullScreen) 1 else 0
        videoProgress?.let { savedState.progressVideo = it }
        videoUrl?.let { savedState.videoUrl = it }
        videoUri?.let { savedState.videoUri = it }
        isMuted.let { savedState.isMuted = it }
        isPlaying.let { savedState.isPlaying = it }
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            fullScreen = state.fullScreen == 1
            mediaControlsEnabled = state.mediaControlsEnabled == 1
            videoProgress = state.progressVideo
            state.videoUrl?.let { videoUrl = it/*setVideoUrl(it,true)*/ }
            state.videoUri?.let { videoUri = it/*setVideoUri(it)*/ }
            isMuted = state.isMuted
            isPlaying = state.isPlaying
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    /**
     * Clase para guardar los estados del video
     */
    private class SavedState : BaseSavedState {
        var fullScreen : Int = 0
        var mediaControlsEnabled = 1
        var videoUrl : String? = null
        var videoUri : Uri? = null
        var progressVideo : Long = 0L
        var isMuted : Int = UNMUTE
        var isPlaying : Int = PLAYING
        var startOrientation : Int = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        constructor(superState: Parcelable?) : super(superState)
        constructor(parcel: Parcel) : super(parcel) {
            fullScreen = parcel.readInt()
            mediaControlsEnabled = parcel.readInt()
            videoUrl = parcel.readString()
            videoUri = Uri.parse(parcel.readString())
            progressVideo = parcel.readLong()
            isMuted = parcel.readInt()
            isPlaying = parcel.readInt()
            startOrientation = parcel.readInt()
        }
        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(fullScreen)
            out.writeInt(mediaControlsEnabled)
            out.writeString(videoUrl)
            out.writeString(videoUri.toString())
            out.writeLong(progressVideo)
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

    internal fun setWindowInsetsController(windowInsetsControllerCompat: WindowInsetsControllerCompat){
        this.windowInsetsControllerCompat = windowInsetsControllerCompat
    }

    private fun prepareNotificationMedia(){
        val sessionToken = SessionToken(context, ComponentName(context, MediaService::class.java))

        controllerFuture = MediaController.Builder(context,sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            mediaController?.let { startMediaController(it) }
        }, MoreExecutors.directExecutor())
    }

    private fun startMediaController(mediaController: MediaController){
        mediaController.run {
            assignPlayer(this)
            mediaController.addListener(object : Player.Listener{
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if(isPlaying && binding.btPlayPause.icon != dPlayPause){
                        binding.btPlayPause.icon = dPlayPause
                        binding.btPlayPause.setOnClickListener { playPauseClick() }
                    }

                    binding.btPlayPause.isChecked = !isPlaying
                }
            })

            val mediaItem = MediaItem.Builder()
                .setMediaId(MEDIA_SESSION_ID)
                .setUri(videoUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(titleNoti ?: "")
                        .setArtist(authorNoti ?: "")
                        .setArtworkUri(getPhotoUri())
                        .build()
                )

            videoUri?.let { mediaItem.setUri(it) }
            videoUrl?.let { mediaItem.setUri(it) }

            setMediaItem(mediaItem.build())
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
        exoPlayer?.release()
        bufferingJob?.cancel()
        progressJob?.cancel()

        bufferingJob = null
        progressJob = null
        exoPlayer = null

        controllerFuture?.let {
            MediaController.releaseFuture(it)
            mediaController == null
        }
        controllerFuture = null
        activity?.stopService(mediaSessionServiceIntent)
    }
}

