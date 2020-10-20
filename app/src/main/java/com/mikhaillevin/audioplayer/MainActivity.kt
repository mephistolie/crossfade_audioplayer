package com.mikhaillevin.audioplayer

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import java.util.*

class MainActivity : AppCompatActivity() {

    private var firstAudio : Uri = Uri.EMPTY
    private var secondAudio : Uri = Uri.EMPTY

    var firstMediaPlayer = MediaPlayer()
    var secondMediaPlayer = MediaPlayer()

    var firstVolume = 1F
    var secondVolume = 0F
    private var crossfade : Long = 2000

    private var firstTimer = Timer(true)
    private var secondTimer = Timer(true)
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val firstAudioPicker : Button = findViewById(R.id.first_audio)
        val secondAudioPicker : Button = findViewById(R.id.second_audio)
        val play : Button = findViewById(R.id.play)
        val slider = findViewById<Slider>(R.id.crossfade)

        firstAudioPicker.setOnClickListener {
            chooseFile(1)
        }
        secondAudioPicker.setOnClickListener {
            chooseFile(2)
        }

        play.setOnClickListener {
            if (!firstMediaPlayer.isPlaying && !secondMediaPlayer.isPlaying) {
                if (firstAudio != Uri.EMPTY && secondAudio != Uri.EMPTY) {
                    crossfade = slider.value.toLong()*1000
                    val firstLength = getLength(firstAudio)
                    val secondLength = getLength(secondAudio)

                    if (firstLength > 2 * crossfade && secondLength > 2*crossfade) {

                        firstMediaPlayer = createMediaPlayer(firstAudio)
                        secondMediaPlayer = createMediaPlayer(secondAudio)

                        isPlaying = true
                        firstVolume = 1F
                        secondVolume = 0F
                        slider.isEnabled = false
                        play.text = getText(R.string.stop)
                        firstAudioPicker.isClickable = false
                        secondAudioPicker.isClickable = false

                        startPlaying(firstLength, secondLength, crossfade)

                    } else {
                        Toast.makeText(this, R.string.too_short, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, R.string.select_audio, Toast.LENGTH_SHORT).show()
                }
            } else {
                reset()
                firstAudioPicker.isClickable = true
                secondAudioPicker.isClickable = true
                slider.isEnabled = true
                play.text = getText(R.string.play)
            }
        }
    }

    private fun chooseFile(audio: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "audio/*"
        startActivityForResult(intent, audio)
    }

    private fun getLength(file : Uri) : Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(applicationContext, file)
        return Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!).toLong()
    }

    private fun startPlaying(firstLength : Long, secondLength : Long, crossfade : Long) {
        firstMediaPlayer.start()
        val firstTimerTask: TimerTask = object : TimerTask() {
            override fun run() {
                crossfadeBegin(secondMediaPlayer)
            }
        }
        val secondTimerTask: TimerTask = object : TimerTask() {
            override fun run() {
                crossfadeBegin(firstMediaPlayer)
            }
        }
        firstTimer.schedule(firstTimerTask, firstLength - crossfade, firstLength + secondLength - 2 * crossfade)
        secondTimer.schedule(secondTimerTask, firstLength + secondLength - 2 * crossfade, firstLength + secondLength - 2 * crossfade)
    }

    private fun createMediaPlayer(audio : Uri) : MediaPlayer {
        return MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(applicationContext, audio)
            prepare()
        }
    }

    private fun crossfadeBegin(to : MediaPlayer) {
        val deltaVolume = 1 / (crossfade.toFloat()/100)
        if (to == firstMediaPlayer) {
            firstMediaPlayer.start()
        } else {
            secondMediaPlayer.start()
        }
        val timer = Timer(true)
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                val volume = if (to == firstMediaPlayer) firstVolume else secondVolume
                if (volume >= 1F) {
                    timer.cancel()
                    timer.purge()
                }
                crossfadeToAudio(deltaVolume, to)
            }
        }
        timer.schedule(timerTask, 100, 100)
    }

    fun crossfadeToAudio (deltaVolume: Float, to : MediaPlayer) {
        firstMediaPlayer.setVolume(firstVolume, firstVolume)
        secondMediaPlayer.setVolume(secondVolume, secondVolume)
        if (to == firstMediaPlayer) {
            firstVolume += deltaVolume
            secondVolume -= deltaVolume
        } else {
            firstVolume -= deltaVolume
            secondVolume += deltaVolume
        }
    }

    private fun reset() {
        isPlaying = false
        firstMediaPlayer.stop()
        secondMediaPlayer.stop()
        firstTimer.cancel()
        firstTimer.purge()
        secondTimer.cancel()
        secondTimer.purge()
        firstTimer = Timer(true)
        secondTimer = Timer(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == RESULT_OK) {
            when(requestCode) {
                1 -> firstAudio = data!!.data!!
                2 -> secondAudio = data!!.data!!
            }
            Toast.makeText(this, R.string.success, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show()
        }
    }
}