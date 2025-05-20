package com.example.ownmediaplayer

import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.danwolve.own_media_player.dialog.OwnVideoPlayerDialog
import com.example.ownmediaplayer.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    companion object{
        private const val TAG = "MainActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val dialog = OwnVideoPlayerDialog.Builder
            //.setRawRes(R.raw.video,packageName)
            .setUri(File(Environment.getDownloadCacheDirectory(),"video.mp4").toUri())
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