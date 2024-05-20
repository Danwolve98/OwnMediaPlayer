package com.danwolve.own_media_player.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.danwolve.own_media_player.databinding.DiaVideoBinding
import com.danwolve.own_media_player.extensions.animate
import com.danwolve.own_media_player.extensions.notNull
import com.danwolve.own_media_player.views.OwnMediaPlayer

class OwnVideoPlayerDialog @JvmOverloads constructor(val url : String = "") : DialogFragment() {

    companion object{
        private const val TAG = "OwnVideoPlayerDialog"
    }

    private val VIDEO_URL = "URL"

    private lateinit var urlVideo : String

    private val ownMediaPlayer : OwnMediaPlayer by lazy { binding.ownMediaPlayer }

    private lateinit var binding: DiaVideoBinding

    init {
        isCancelable = true
    }

    fun setUrl(url : String){
        urlVideo = url
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState.notNull {
            it.getString(VIDEO_URL).notNull { url-> urlVideo = url }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            this.window.notNull {
                it.requestFeature(Window.FEATURE_NO_TITLE)
                it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(VIDEO_URL,urlVideo)
    }

    @SuppressLint("UseGetLayoutInflater")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DiaVideoBinding.inflate(LayoutInflater.from(context),null,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        dialog?.window.notNull {
            it.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        binding.root.animate(duracion = 0.5f, alpha = 1f, initAlpha = 0f)
        binding.root.setOnClickListener { dismiss() }

        if(savedInstanceState?.getString(VIDEO_URL) == null){
            ownMediaPlayer.setVideoUrl(urlVideo)
        }
    }

    override fun dismiss() {
        super.dismiss()
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


}