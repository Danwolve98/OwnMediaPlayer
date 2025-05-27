package com.danwolve.own_media_player.exception

sealed class VideoException(error:String) : Exception(error) {
    data object URLNeeded : VideoException("You need to specify a url in the Builder class")
}