package com.danwolve.own_media_player.dialog

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentResolver
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.core.graphics.Insets
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.danwolve.own_media_player.R
import com.danwolve.own_media_player.databinding.DiaVideoBinding
import com.danwolve.own_media_player.extensions.AnimParams
import com.danwolve.own_media_player.extensions.animate
import com.danwolve.own_media_player.extensions.dp
import com.danwolve.own_media_player.views.OwnMediaPlayer
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class OwnVideoPlayerDialog : DialogFragment() {
    private var urlVideo : String? = null
    private var uriVideo : Uri? = null
    private var fullScreen : Boolean = false
    private var hasNotification : Boolean = false
    private var useLegacy : Boolean = false
    private var titleNoti : String? = null
    private var authorNoti : String? = null
    private var photoNoti : String? = null

    companion object{
        private const val TAG = "OwnVideoPlayerDialog"
        private const val VIDEO_URL = "URL"
        private const val VIDEO_URI = "URI"
        private const val FULLSCREEN = "FULLSCREEN"
        private const val ORIENTATION = "ORIENTATION"
        private const val HAS_NOTIFICATION = "HAS_NOTIFICATION"
        private const val PHOTO_NOTI = "PHOTO_NOTI"
        private const val TITLE_NOTI = "TITLE_NOTI"
        private const val AUTHOR_NOTI = "AUTHOR_NOTI"
        private const val USE_LEGACY = "USE_LEGACY"

        private fun newInstance(
            urlVideo : String? = null,
            uriVideo : Uri? = null,
            fullScreen: Boolean = false,
            hasNotification : Boolean,
            useLegacy : Boolean,
            titleNoti : String? = null,
            authorNoti : String? = null,
            photoNoti : String? = null
        ): OwnVideoPlayerDialog{
            val args = Bundle().apply {
                urlVideo?.let { putString(VIDEO_URL,it) }
                uriVideo?.let { putParcelable(VIDEO_URI,it) }
                putBoolean(FULLSCREEN,fullScreen)
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

    //BUILDER
    class Builder private constructor(
        private val url : String? = null,
        private val uri : Uri? = null
    ){
        private var hasNotification = false
        private var fullScreen : Boolean = false
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

        fun applyFullScreen(fullScreen: Boolean) : Builder {
            this.fullScreen = fullScreen
            return this
        }

        fun build() : OwnVideoPlayerDialog = newInstance(url,uri,fullScreen,hasNotification,useLegacy,titleNoti,authorNoti,photoNoti)
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
            fullScreen = bundle.getBoolean(FULLSCREEN,false)
            hasNotification = bundle.getBoolean(HAS_NOTIFICATION,false)
            useLegacy = bundle.getBoolean(USE_LEGACY,false)
            titleNoti = bundle.getString(TITLE_NOTI)
            authorNoti = bundle.getString(AUTHOR_NOTI)
            photoNoti = bundle.getString(PHOTO_NOTI)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getBundles()
        setStyle()
        savedInstanceState?.let {
            BundleCompat.getParcelable(it,VIDEO_URI,Uri::class.java)?.let { uri->
                this.uriVideo = uri
            }
            it.getString(VIDEO_URL)?.let { urlVideo-> this.urlVideo = urlVideo }
            it.getInt(ORIENTATION).let { startOrientation-> this.startOrientation = startOrientation }
        }

        if(startOrientation == null){
            startOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun setStyle(){
        if(fullScreen)
            setStyle(STYLE_NO_TITLE,R.style.FullScreenDialog)
        else
            setStyle(STYLE_NO_TITLE,R.style.GalleryDialogTheme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            this.window?.let {wdw->
                wdw.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
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
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        //ROOT
        binding.root.run {
            animate(duracion = 0.5f, initAnimParams = AnimParams(alpha = 0f), animParams = AnimParams(alpha = 1f))
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
            setShowCallBack {
                if(fullScreen)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                        windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
                    else
                        @Suppress("DEPRECATION")
                        dialog?.window?.apply {
                            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                            clearFlags(View.SYSTEM_UI_FLAG_FULLSCREEN)
                        }
            }
            setHideCallback {
                if(fullScreen)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
                    else
                        @Suppress("DEPRECATION")
                        dialog?.window?.decorView?.systemUiVisibility = (
                                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                )
            }
            lifecycle.addObserver(this)

            ViewCompat.setOnApplyWindowInsetsListener(this) { v,insets->
                val insetsBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                if(fullScreen)
                    animateTopSystemBars(insetsBars)

                //SECURE AREA
                animateSeekBarInsets(insetsBars)
                insets
            }

        }
    }

    private fun animateTopSystemBars(insetsBars: Insets){
        val topView = binding.ownMediaPlayer.binding.lyTop

        val start = if(insetsBars.top == 0) topView.marginTop else 0
        val end = if(insetsBars.top == 0) 0 else insetsBars.top

        currentTopInsetsAnimator?.cancel()
        currentTopInsetsAnimator = ValueAnimator.ofInt(start,end).apply {
            duration = 200
            addUpdateListener {
                topView.updateLayoutParams<ViewGroup.MarginLayoutParams>{
                    topMargin = it.animatedValue as Int
                }
            }
            start()
        }
    }

    private fun animateSeekBarInsets(insetsBars: Insets){
        val bottomView = binding.ownMediaPlayer.binding.seekBar
        val marginInsets = maxOf(insetsBars.right,insetsBars.bottom,insetsBars.left)

        val start = if(marginInsets == 0) maxOf(bottomView.marginBottom,bottomView.marginStart,bottomView.marginEnd) else 0.dp
        val end = if(marginInsets == 0) 0.dp else marginInsets

        currentBottomInsetsAnimator?.cancel()
        currentBottomInsetsAnimator = ValueAnimator.ofInt(start,end).apply {
            duration = 200
            addUpdateListener {
                if(activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
                    bottomView.updateLayoutParams<ViewGroup.MarginLayoutParams>{
                        marginStart = it.animatedValue as Int
                        marginEnd = it.animatedValue as Int
                    }
                    binding.ownMediaPlayer.binding.lyTop.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        marginStart = it.animatedValue as Int
                        marginEnd = it.animatedValue as Int
                    }
                }else
                    bottomView.updateLayoutParams<ViewGroup.MarginLayoutParams>{
                        bottomMargin = it.animatedValue as Int
                    }
            }
            start()
        }
    }

    private var currentTopInsetsAnimator : ValueAnimator? = null
    private var currentBottomInsetsAnimator : ValueAnimator? = null

    @OptIn(ExperimentalAtomicApi::class)
    private var a = 0

    private val rootWindowInsets by lazy { ViewCompat.getRootWindowInsets(binding.root)?.getInsets(WindowInsetsCompat.Type.systemBars()) }

    private fun ownDismiss(){
        startOrientation?.let { activity?.requestedOrientation = it }
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