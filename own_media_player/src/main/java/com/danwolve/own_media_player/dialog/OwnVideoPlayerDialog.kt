package com.danwolve.own_media_player.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentResolver
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.core.os.BundleCompat
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

class OwnVideoPlayerDialog : DialogFragment() {
    private var urlVideo : String? = null
    private var uriVideo : Uri? = null
    private var hasNotification : Boolean = false
    private var useLegacy : Boolean = false
    private var titleNoti : String? = null
    private var authorNoti : String? = null
    private var photoNoti : String? = null

    companion object{
        private const val TAG = "OwnVideoPlayerDialog"
        private const val VIDEO_URL = "URL"
        private const val VIDEO_URI = "URI"
        private const val ORIENTATION = "ORIENTATION"
        private const val HAS_NOTIFICATION = "HAS_NOTIFICATION"
        private const val PHOTO_NOTI = "PHOTO_NOTI"
        private const val TITLE_NOTI = "TITLE_NOTI"
        private const val AUTHOR_NOTI = "AUTHOR_NOTI"
        private const val USE_LEGACY = "USE_LEGACY"

        private fun newInstance(
            urlVideo : String? = null,
            uriVideo : Uri? = null,
            hasNotification : Boolean,
            useLegacy : Boolean,
            titleNoti : String? = null,
            authorNoti : String? = null,
            photoNoti : String? = null
        ): OwnVideoPlayerDialog{
            val args = Bundle().apply {
                urlVideo?.let { putString(VIDEO_URL,it) }
                uriVideo?.let { putParcelable(VIDEO_URI,it) }
                putBoolean(HAS_NOTIFICATION,hasNotification)
                putBoolean(USE_LEGACY,useLegacy)
                titleNoti?.let { putString(TITLE_NOTI,it) }
                authorNoti?.let { putString(AUTHOR_NOTI,it) }
                photoNoti?.let { putString(PHOTO_NOTI,it) }
            }
            val fragment = OwnVideoPlayerDialog().apply { arguments = args }
            return fragment
        }
    }

    class Builder private constructor(
        private val url : String? = null,
        private val uri : Uri? = null
    ){
        private var hasNotification = false
        private var useLegacy = false
        private var titleNoti : String? = null
        private var authorNoti : String? = null
        private var photoNoti : String? = null
        companion object{
            fun setUrl(url: String) : Builder = Builder(url = url)
            fun setUri(uri: Uri) : Builder = Builder( uri = uri)
            fun setRawRes(@RawRes redId: Int,packageName : String) : Builder =
                Builder(uri =
                    //Uri.parse("android.resource://${packageName}/$redId")
                    Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(packageName)
                    .appendPath("$redId")
                    .build()
                )
        }

        fun activeNotification(title : String? = null, author : String? = null,photo : String? = null) : Builder{
            this.hasNotification = true
            this.titleNoti = title
            this.authorNoti = author
            this.photoNoti = photo
            return this
        }

        fun useLegacy() : Builder{
            this.useLegacy = true
            return this
        }

        fun build() : OwnVideoPlayerDialog = newInstance(url,uri,hasNotification,useLegacy,titleNoti,authorNoti,photoNoti)
    }

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
    }

    private fun getBundles(){
        arguments?.let { bundle->
            urlVideo = bundle.getString(VIDEO_URL)
            uriVideo = BundleCompat.getParcelable(bundle,VIDEO_URI,Uri::class.java)
            hasNotification = bundle.getBoolean(HAS_NOTIFICATION) ?: false
            useLegacy = bundle.getBoolean(USE_LEGACY) ?: false
            titleNoti = bundle.getString(TITLE_NOTI)
            authorNoti = bundle.getString(AUTHOR_NOTI)
            photoNoti = bundle.getString(PHOTO_NOTI)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getBundles()
        setStyle(STYLE_NORMAL,R.style.GalleryDialogTheme)
        savedInstanceState.notNull {
            BundleCompat.getParcelable(it,VIDEO_URI,Uri::class.java)?.let { uri->
                this.uriVideo = uri
            }
            it.getString(VIDEO_URL).notNull { urlVideo-> this.urlVideo = urlVideo }
            it.getInt(ORIENTATION).let { startOrientation-> this.startOrientation = startOrientation }
        }
        if(startOrientation == null){
            startOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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

        if(activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
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
            if(hasNotification)
                setNotification(titleNoti,authorNoti,photoNoti)
            urlVideo?.let { setVideoUrl(it ,true) }
            uriVideo?.let { setVideoUri(it,true) }
            setFullScreenCallBack {
                hasRotationChange = true
            }
            setCloseCallBack {
                ownDismiss()
            }
            lifecycle.addObserver(this)
        }
    }

    private fun ownDismiss(){
        startOrientation.notNull { activity?.requestedOrientation = it }
        dismiss()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (manager.fragments.any { it is OwnVideoPlayerDialog }) return //PARA QUE AL RECREARSE EL FRAGMENT/ACTIVITY NO SE APILEN LOS DIALOGS
        super.show(manager, tag)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!hasRotationChange)
            startOrientation?.let { activity?.requestedOrientation = it }
    }

}