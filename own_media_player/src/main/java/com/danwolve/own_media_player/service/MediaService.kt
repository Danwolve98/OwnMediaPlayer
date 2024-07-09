package com.danwolve.own_media_player.service

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.ConnectionResult.AcceptedResultBuilder
import androidx.media3.session.MediaSessionService

class MediaService : MediaSessionService(){

    private lateinit var exoPlayer : ExoPlayer
    private var mediaSession : MediaSession? = null

    /*private val notificationIntent by lazy {
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, activity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }*/

    private val callbackMediaSession by lazy {
        object : MediaSession.Callback{
            @OptIn(UnstableApi::class)
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): ConnectionResult {
                AcceptedResultBuilder(session)
                    .setAvailablePlayerCommands(
                        ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                            .remove(Player.COMMAND_SEEK_TO_NEXT)
                            .remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                            .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                            .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                            .build()
                    )
                    .setAvailableSessionCommands(
                        ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().build()
                    )
                return super.onConnect(session, controller)
            }
            /*override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                return super.onCustomCommand(session, controller, customCommand, args)
            }*/
        }
    }

    //ONCREATE
    override fun onCreate() {
        super.onCreate()
        initializeExoPlayer()
        initializeMediaSession()
    }
    private fun initializeExoPlayer(){
        exoPlayer = ExoPlayer.Builder(this@MediaService).build()
    }
    private fun initializeMediaSession(){
        mediaSession = MediaSession.Builder(this@MediaService,exoPlayer)
            //.setSessionActivity(notificationIntent)
            .setCallback(callbackMediaSession)
            .build()
    }

    //TASK REMOVED
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player!!
        if (player.playWhenReady)
            player.pause()
        stopSelf()
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession


    //ONDESTROY
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession == null
        }
        super.onDestroy()
    }

}