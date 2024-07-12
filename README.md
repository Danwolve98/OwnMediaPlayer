# OwnMediaPlayer
![OwnMediaPlayer](https://raw.githubusercontent.com/Danwolve98/OwnMediaPlayer/main/own_media_player/src/main/res/drawable/own_media_player.png)

[![JitPackVersion](https://jitpack.io/v/Danwolve98/OwnMediaPlayer.svg)](https://jitpack.io/#Danwolve98/OwnMediaPlayer)

*OwnMediaPlayer* is a library designed to upload videos easily and simply. It is built with [*Media3*](https://developer.android.com/media/implement/playback-app) of Android.
The notification part has been used following the [*MediaControls*](https://developer.android.com/media/implement/surfaces/mobile) guide.

![horizontal](https://github.com/user-attachments/assets/d52e697a-2c64-4ffb-8c4e-f523e836d711)

![notification_api33](https://github.com/user-attachments/assets/0b6adf2a-fcf0-4a02-815b-7e7bd458c64c)

In the current version it supports:
|| OwnMediaPlayer | OwnVideoPlayerDialog |
|--|--|--|
| URL | ✅ | ✅ |
| Resource | ❌ | ❌|
| Youtube | ❌ | ❌ |

🗈 We are working to make it work in the future with many more formats and forms.

### StartConfig
This setting is required if you want it to work correctly with notifications

> ⚠️If you don't do it, it won't work properly.

In the AndroidManifest.xml of your activity, you have to add these 2 permissions:
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />  
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```
And add the media service for the notification
```xml
<service  
  android:name="com.danwolve.own_media_player.service.MediaService"  
  android:description="@string/servicio_para_mostrar_audio_saliente_museos"  
  android:exported="true"  
  android:foregroundServiceType="mediaPlayback">  
  <intent-filter>  
	  <action android:name="androidx.media3.session.MediaSessionService" />  
  </intent-filter>  
</service>
```
## OwnMediaPlayer
*OwnMediaPlayer* has been used at the moment mainly for the OwnMediaPlayerDialog part, so it is hardly complete and will be updated as the versions go by.

An example of **implementation** would be the following:
```xml
<com.danwolve.own_media_player.views.OwnMediaPlayer  
      android:id="@+id/own_media_player"  
      android:layout_width="match_parent"  
      android:layout_height="wrap_content"  
      app:hasNotification="true"  
      app:titleNotification="weapon"  
      app:authorNotification="Against the Current"  
      app:photoNotification="@drawable/aganist_the_current"  
      app:layout_constraintBottom_toBottomOf="parent"  
      app:layout_constraintTop_toTopOf="parent"/>
```
This part is used to customize the **notification**.
```xml 
app:hasNotification="true"  
app:titleNotification="weapon"  
app:authorNotification="Against the Current"  
app:photoNotification="@drawable/aganist_the_current" '''
```
The most basic implementation would be as follows, this will load the video automatically:
```kotlin
binding.ownMediaPlayer.setVideoUrl("your_url.mp4")
```
The result with the notification will be the following, depending on the API it will be displayed in one way or another, there is a great variety of notifications, these are some examples:

*API 26*

![noti1](https://github.com/user-attachments/assets/cc4aeea8-386e-4323-8b6b-7237d8ed9432)

*API 33*

![noti2](https://github.com/user-attachments/assets/b51aa2c4-81ca-4d04-bed8-0d833f63d750)

## OwnVideoPlayerDialog
*OwnVideoPlayerDialog* has been used at the moment mainly for the *OwnMediaPlayerDialog* part, so it is hardly complete and will be updated as the versions go by.
  
The main thing about this version currently, you can show a dialog with the video. You cannot display more than one dialog at a time.

An example of **implementation** would be the following:
``` kotlin
OwnVideoPlayerDialog.Builder  
  .setUrl("your_url.mp4")  
  .activeNotification(  
	  "weapon",  
      "Against the Current",  
      "your_image.png")  
  .build()  
  .show(supportFragmentManager, TAG)
  ```
  When activating the notification, no field is mandatory, it simply will not be displayed.
  If `activeNotification()` is not used, no notification will be shown.
  
## License
OwnMediaPlayer is available under the MIT license. Read the [LICENSE.txt](https://github.com/Danwolve98/OwnMediaPlayer/blob/main/LICENSE) file for more information.
  
