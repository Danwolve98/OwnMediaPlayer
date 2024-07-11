package com.example.ownmediaplayer

import android.content.Context
import android.net.Uri
import android.nfc.Tag
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.View
import com.danwolve.own_media_player.dialog.OwnVideoPlayerDialog
import com.danwolve.own_media_player.views.OwnMediaPlayer
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
            .setUrl("https://escolesvalenciapre.grupotecopy.es/sites/default/files/videos/2024-02/EIMP%20Corregido%20%282%29_0.mp4")
           /* .activeNotification(
                "OwnMediaPlayer",
                "CR7",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRdhHe79aHGHO5SfYZ01rniGOn7--_yPBXC4HIlynkunrmLLU3rli-La4uyaHQq76-ywBUL6RDQ_qzZ4FxW39LM4ERCN9balNn4FJwRUQ")*/
            .build()

        dialog.show(supportFragmentManager, TAG)
    }
}