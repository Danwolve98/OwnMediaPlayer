package com.example.ownmediaplayer

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.danwolve.own_media_player.dialog.OwnVideoPlayerDialog
import com.example.ownmediaplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    companion object{
        private const val TAG = "MainActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //OWM_MEDIA_PLAYER
        showOwnMediaPlayer()

        //OWM_VIDEO_PLAYER_DIALOG
        //showOwnVideoPlayerDialog()
    }

    private fun showOwnMediaPlayer() = with(binding.ownMediaPlayer){
        visibility = View.VISIBLE
        autoLoop(false)
        setFullScreen(true)
        showFullScreenButton(false)
        setVideoUrl("https://connecta.vlci.valencia.es/apps/escoles/sites/default/files/videos/2024-02/GENT%20MENUDA.mp4")
        //setRawRes(R.raw.video,packageName)
    }

    private fun showOwnVideoPlayerDialog(){
        val dialog = OwnVideoPlayerDialog.Builder
            //.setRawRes(R.raw.video,packageName)
            .setUrl("https://museusvalenciapre.grupotecopy.es/sites/default/files/2024-10/BEACON%2050%20%2B%20%20Audio%20Benlliure%2042%20%2B%20IMG.mp4")
            //.setUrl("https://connecta.vlci.valencia.es/apps/escoles/sites/default/files/videos/2024-02/GENT%20MENUDA.mp4")
            .applyFullScreen(true)
            //.setUri(File(Environment.getDownloadCacheDirectory(),"video.mp4").toUri())
            //.setUrl("https://museusvalenciades.grupotecopy.es/sites/default/files/2024-09/HISTORIA_video-1_SUB-VAL.mp4")
            //.setUrl("https://museusvalenciades.grupotecopy.es/sites/default/files/2024-06/Against%20The%20Current%20-%20silent%20stranger%20%28Official%20Music%20Video%29.mp4")
            .activeNotification(
                "OwnMediaPlayer",
                "CR7",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRdhHe79aHGHO5SfYZ01rniGOn7--_yPBXC4HIlynkunrmLLU3rli-La4uyaHQq76-ywBUL6RDQ_qzZ4FxW39LM4ERCN9balNn4FJwRUQ")
            .build()

        dialog.show(supportFragmentManager, TAG)
    }
}