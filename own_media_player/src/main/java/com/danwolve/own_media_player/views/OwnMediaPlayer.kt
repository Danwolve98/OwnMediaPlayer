package com.danwolve.own_media_player.views

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import com.danwolve.own_media_player.R
import com.danwolve.own_media_player.databinding.CustomMediaPlayerBinding
import com.danwolve.own_media_player.extensions.animate
import com.danwolve.own_media_player.extensions.invisible
import com.danwolve.own_media_player.extensions.notNull
import com.danwolve.own_media_player.extensions.playAnimation
import com.danwolve.own_media_player.extensions.visible

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
    atributeSet : AttributeSet? = null,
    orientation : Int? = null)
    : ConstraintLayout(contexto,atributeSet) {
    companion object{
        private const val TAG = "OwnMediaPlayer"

        const val PORTRAIT = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        const val SENSOR = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        const val FULLSCREEN = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        const val MUTE = -1
        const val UNMUTE = 0

        const val PAUSED = -1
        const val PLAYING = 0
    }
    init {
        orientation
    }

    //  BASICS
    private var activity: Activity? = null
    private lateinit var binding : CustomMediaPlayerBinding
    private lateinit var mediaPlayer : MediaPlayer
    private lateinit var progressRunnable: Runnable

    private val handlerProgress by lazy { Handler(Looper.getMainLooper()) }
    private val countDownTimer by lazy { object : CountDownTimer(4000,1000){
        override fun onTick(millisUntilFinished: Long) {}
        override fun onFinish() {
            hideOwnMediaPlayer()
            isHidden = true
        }
    }}

    //  PARCELABLES
    private var videoProgress : Long? = null
    private var videoUrl : String? = null
    private var videoPath : String? = null
    private var orientation : Int = SENSOR
    private var startOrientation : Int? = null
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
        activity = getActivity()
        if(startOrientation == null)
            startOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_SENSOR
        createView()
        countDownTimer.start()
    }

    /**
     * Infla la vista
     */
    private fun createView(){
        binding = CustomMediaPlayerBinding.inflate(LayoutInflater.from(context),this)
        hideOwnMediaPlayer(0f)
    }

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
    }

    /**
     * Formatea los milisegundos al formato [00:00]
     */
    private fun formatDuration(durationInMilis : Int) : String{
        val totalSeconds = durationInMilis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d", minutes,seconds)
    }

    /**
     * OGLIGATORIO
     * Asigna la url del video a mostrar
     */
    fun setVideoUrl(urlVideo : String,autoPlay : Boolean = true){
        videoPath = null
        videoUrl = urlVideo
        prepareVideo(autoPlay)
    }

    /**
     * OGLIGATORIO
     * Asigna la dirección del video a mostrar
     */
    fun setVideoPath(path : String,autoPlay : Boolean = true) {
        videoUrl = null
        videoPath = path
        prepareVideo(autoPlay)
    }

    /**
     * Prepara el video para ser ejecutado
     */
    private fun prepareVideo(autoPlay : Boolean = true){
        with(binding){

            root.setOnTouchListener { _, event ->
                when(event.action){
                    MotionEvent.ACTION_DOWN->{
                        countDownTimer.cancel()
                        countDownTimer.start()
                        false
                    }
                    else->{
                        false
                    }
                }
            }

            btMute.isChecked = isMuted == MUTE
            btFullScreenVideo.isChecked = orientation == FULLSCREEN
            btPlayPause.isChecked = isPlaying == PAUSED

           videoUrl.notNull { ownTextureView.setVideo(it) }
           videoPath.notNull { ownTextureView.setVideo(it) }

            ownTextureView.setOnPreparedListener {
                loading.invisible()
                showOwnMediaPlayer()
                mediaPlayer = it
                assingOwnMediaController(it)
                if (autoPlay) it.start()
            }
            ownTextureView.setOnErrorListener {

            }
            ownTextureView.setOnInfoListener { _, what, _ ->
                if(what == MediaPlayer.MEDIA_INFO_BUFFERING_START)
                    loading.visible()
                else if(what == MediaPlayer.MEDIA_INFO_BUFFERING_END)
                    loading.invisible()
            }
            ownTextureView.setOnCompletitionListener {

            }
        }
    }

    /**
     * Asigna todas las funcionalidades del [MediaPlayer] a las vistas
     */
    private fun assingOwnMediaController(mediaPlayer: MediaPlayer){
        with(binding){
            //  MUTE
            if(isMuted == MUTE) mediaPlayer.setVolume(0f,0f)
            //  FULL SCREEN
            activity.notNull {activity->
                btFullScreenVideo.setOnClickListener {
                    orientation = if(btFullScreenVideo.isChecked)
                        FULLSCREEN
                    else
                        PORTRAIT
                    activity.requestedOrientation = orientation
                }
            }

            //  MUTE
            btMute.setOnClickListener {
                isMuted = if(btMute.isChecked){
                    mediaPlayer.setVolume(0f,0f)
                    MUTE
                } else{
                    mediaPlayer.setVolume(1.0f,1.0f)
                    UNMUTE
                }
            }

            // PLAY-PAUSE
            if(isPlaying == PAUSED) ownTextureView.pause()

            btPlayPause.setOnClickListener {
                isPlaying = if(btPlayPause.isChecked){
                    ownTextureView.pause()
                    PAUSED
                }else{
                    ownTextureView.play()
                    PLAYING
                }
            }

            //  PLUS-LESS
            btPlusTenSeconds.setOnClickListener {
                it.playAnimation(R.anim.click_animation)
                mediaPlayer.seekTo(mediaPlayer.currentPosition+10000L)
            }

            btLessTenSeconds.setOnClickListener {
                it.playAnimation(R.anim.click_animation)
                mediaPlayer.seekTo(mediaPlayer.currentPosition-10000L)
            }

            //HIDE-SHOW
            binding.root.setOnClickListener {
                if(isHidden){
                    showOwnMediaPlayer()
                }
                else
                    hideOwnMediaPlayer()
                isHidden = !isHidden
            }

            //  PROGRESS
            tvTotalVideo.text = formatDuration(mediaPlayer.duration)
            seekBar.max = mediaPlayer.duration

            createProgressRunnable()

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if(!fromUser)
                        return
                    mediaPlayer.seekTo(progress.toLong())
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            videoProgress.notNull {
                mediaPlayer.seekTo(it)
            }
        }
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
     * Configura la rotación especificada
     * [ActivityInfo]
     * @param orientation the orientation desired
     * @throws IllegalArgumentException if the [orientation] is not valid
     */
    fun setOrientation(orientation : Int){
        require(orientation in getScreenConfigs()){
           "U NEED TO SET A VALID ${ActivityInfo::class.simpleName} screen config"
        }
        activity?.requestedOrientation = orientation
    }

    fun setAutoPan(){

    }

    /**
     * Desabilita los giros de la pantalla en general, incluido el del botón
     */
    @SuppressLint("SourceLockedOrientationActivity")
    fun disableFullScreen(){
        orientation = PORTRAIT
        disableFullScreenButton()
        activity?.requestedOrientation = PORTRAIT
    }

    /**
     * Crea un [Runnable] que va a ir actualizando los textos y el [SeekBar] conforme avance el video
     */
    private fun createProgressRunnable(){
        progressRunnable = Runnable {
            val currentPosition = mediaPlayer.currentPosition
            videoProgress = currentPosition.toLong()
            binding.tvCurrentVideo.text = formatDuration(currentPosition)
            binding.seekBar.progress = currentPosition
            handlerProgress.postDelayed(progressRunnable,100)
        }
        handlerProgress.post(progressRunnable)
    }

    /**
     * Saber si actualmente el video se está reproduciendo
     */
    fun isPlaying() : Boolean =
        if(!this::mediaPlayer.isInitialized)
            false
        else
            mediaPlayer.isPlaying


    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        videoProgress.notNull { savedState.progressVideo = it }
        videoUrl.notNull { savedState.videoUrl = it }
        videoPath.notNull { savedState.videoPath = it }
        orientation.notNull { savedState.orientation = it }
        isMuted.notNull { savedState.isMuted = it }
        isPlaying.notNull { savedState.isPlaying = it }
        startOrientation.notNull { savedState.startOrientation = it }
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            videoProgress = state.progressVideo
            state.videoUrl.notNull { setVideoUrl(it) }
            state.videoPath.notNull { setVideoPath(it) }
            state.orientation.notNull { orientation = it }
            state.isMuted.notNull { isMuted = it }
            state.isPlaying.notNull { isPlaying = it }
            state.startOrientation.notNull { startOrientation = it }
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
        var orientation : Int = SENSOR
        var isMuted : Int = UNMUTE
        var isPlaying : Int = PLAYING
        var startOrientation : Int = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        constructor(superState: Parcelable?) : super(superState)
        constructor(parcel: Parcel) : super(parcel) {
            progressVideo = parcel.readLong()
            videoUrl = parcel.readString()
            videoPath = parcel.readString()
            orientation = parcel.readInt()
            isMuted = parcel.readInt()
            isPlaying = parcel.readInt()
            startOrientation = parcel.readInt()
        }
        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeLong(progressVideo)
            out.writeString(videoUrl)
            out.writeString(videoPath)
            out.writeInt(orientation)
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

    /**
     * Mueve el progreso del video en función de la versión del dispositivo
     */
    fun MediaPlayer.seekTo(progress : Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            this.seekTo(progress, MediaPlayer.SEEK_CLOSEST)
        else
            this.seekTo(progress.toInt())
    }
}

