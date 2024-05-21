package com.example.ownmediaplayer

import android.content.Context
import android.net.Uri
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        OwnVideoPlayerDialog().apply {
            setUrl("https://escolesvalenciapre.grupotecopy.es/sites/default/files/videos/2024-02/EIMP%20Corregido%20%282%29_0.mp4")
        }.show(supportFragmentManager,"TAG")
/*        binding.ownMediaPlayer.setVideoUrl("https://escolesvalenciades.grupotecopy.es/sites/default/files/videos/2024-02/Big_Buck_Bunny_1080_10.mp4")*/
    }
}