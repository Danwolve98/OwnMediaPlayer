package com.danwolve.own_media_player.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.danwolve.own_media_player.R
import com.danwolve.own_media_player.databinding.DiaVideoBinding
import com.danwolve.own_media_player.extensions.animate
import com.danwolve.own_media_player.extensions.notNull
import com.danwolve.own_media_player.views.OwnMediaPlayer

class OwnVideoPlayerDialog @JvmOverloads constructor(val url : String = "") : DialogFragment() {

    companion object{
        private const val TAG = "OwnVideoPlayerDialog"
        private const val VIDEO_URL = "URL"
        private const val ORIENTATION = "ORIENTATION"
    }

    private lateinit var urlVideo : String
    private val ownMediaPlayer : OwnMediaPlayer by lazy { binding.ownMediaPlayer }
    private lateinit var binding: DiaVideoBinding

    private var startOrientation : Int? = null

    private var hasRotationChange = false
    private val windowInsetsController: WindowInsetsControllerCompat?
        get() =
            if (dialog?.window != null)
                WindowCompat.getInsetsController(dialog!!.window!!, dialog!!.window!!.decorView)
                    .apply {
                        systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
            else
                null

    //CLOSE
    private var canClose = false
    private val timer = object : CountDownTimer(2000,1000){
        override fun onTick(millisUntilFinished: Long) {}
        override fun onFinish() {
            canClose = false
        }
    }

    init {
        isCancelable = true
        if(startOrientation == null)
            startOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    fun setUrl(url : String){
        urlVideo = url
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL,R.style.GalleryDialogTheme)
        savedInstanceState.notNull {
            it.getString(VIDEO_URL).notNull { urlVideo-> this.urlVideo = urlVideo }
            it.getInt(ORIENTATION).let { startOrientation-> this.startOrientation = startOrientation }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            this.window.notNull {wdw->
                wdw.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                wdw.requestFeature(Window.FEATURE_NO_TITLE)
                wdw.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                wdw.setWindowAnimations(R.style.DialogAnimation)
                WindowCompat.setDecorFitsSystemWindows(wdw, false)

            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(VIDEO_URL,urlVideo)
        startOrientation?.let { outState.putInt(ORIENTATION,it) }
    }

    @SuppressLint("UseGetLayoutInflater")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DiaVideoBinding.inflate(LayoutInflater.from(context),null,false)

        if(activity?.requestedOrientation == OwnMediaPlayer.FULLSCREEN)
            windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.window.notNull {
            it.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        //ROOT
        binding.root.run {
            animate(duracion = 0.5f, alpha = 1f, initAlpha = 0f)
            setOnClickListener {
                if(canClose)
                    ownDismiss()
                else{
                    canClose = true
                    timer.start()
                    Toast.makeText(requireContext(), getString(R.string.presiona_para_salir), Toast.LENGTH_SHORT).show()
                }
            }
        }

        //OWNMEDIAPLAYER
        binding.ownMediaPlayer.run {
            if(savedInstanceState?.getString(VIDEO_URL) == null){
                setVideoUrl(urlVideo)
            }
            setFullScreenCallBack {
                hasRotationChange = true
            }
        }

    }

    private fun ownDismiss(){
        startOrientation.notNull { activity?.requestedOrientation = it }
        dismiss()
    }

    /**
     * Ejecuta esta función para que el dialog con el video se gire automaticamente conforme giras la pantalla
     */
    fun setAutoPan(){
        binding.ownMediaPlayer.setAutoPan()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (manager.fragments.any { it is OwnVideoPlayerDialog }) return
        super.show(manager, tag)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!hasRotationChange)
            startOrientation?.let { activity?.requestedOrientation = it }
    }

}