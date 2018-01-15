package com.example.hitesh.audiorecoder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;

import com.recorder.customaudiorecorder.CustomAudioRecorder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Hitesh Khatri
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener,CustomAudioRecorder.MaxFileSizeReachedListener {
    //Instance for custom audio recorder.
    private CustomAudioRecorder audioRecorder;
    private Chronometer chronometer;
    private  long timeWhenStopped = 0;
    private final static int BEFORE_START_RECORDING=0;
    private final static int START_RECORDING=1;
    private final static int STOP_RECORDING=2;
    private final static int PAUSE_RECORDING=3;
    private final static String TAG="MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_start_recording).setOnClickListener(this);
        findViewById(R.id.btn_pause_recording).setOnClickListener(this);
        findViewById(R.id.btn_stop_recording).setOnClickListener(this);
        findViewById(R.id.btn_play_audio).setOnClickListener(this);
        chronometer= findViewById(R.id.tv_record_timer);
        initCustomAudioRecorder();
        invalidateViews(BEFORE_START_RECORDING);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_start_recording:
                if (checkPermissions()){
                    recordingStart();
                }
                break;

            case R.id.btn_pause_recording:
                timeWhenStopped = chronometer.getBase() - SystemClock.elapsedRealtime();
                chronometer.stop();
                CustomAudioRecorder.setRecorderState(CustomAudioRecorder.AUDIO_RECORDER_PAUSE);
                invalidateViews(PAUSE_RECORDING);
                break;

            case R.id.btn_stop_recording:
                CustomAudioRecorder.setRecorderState(CustomAudioRecorder.AUDIO_RECORDER_STOP);
                break;
            case R.id.btn_play_audio:
                startPlaying();
                break;

        }
    }

    //method to start media player
    private void startPlaying() {
        final MediaPlayer player = new MediaPlayer();
        try {
            //set the file source
            player.setDataSource(getAudioPath());
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                player.start();
            }
        });
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                player.stop();
                player.release();
            }
        });
    }
    /**
     * Method to check all permissions are granted or not
     * @return : if any required permission is not granted then return false.
     */
    private boolean checkPermissions() {
        int result;
        String[] permissions = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
        };
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ActivityCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    100);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        boolean isPermissionDenied=false;

        if (requestCode == 100) {
            for (int i:grantResults){
                if (i==PackageManager.PERMISSION_DENIED){
                    isPermissionDenied=true;
                    break;
                }
            }

            if (!isPermissionDenied){
                recordingStart();
            }else {
                finishAffinity();
            }

        }
    }

    //init custom audio recorder
    private void initCustomAudioRecorder(){
        audioRecorder=new CustomAudioRecorder(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioRecorder.setThreadMaxFileSizeInBytes(2000000);
        audioRecorder.setMaxFileSizeReachedListener(this);

    }

    /**
     * Method which starts the recording.
     */
    private void recordingStart(){
        if (CustomAudioRecorder.getRecorderState()==CustomAudioRecorder.AUDIO_RECORDER_STOP) {
            audioRecorder.setAudioPath(getAudioPath());
            audioRecorder.startRecorder(new CustomAudioRecorder.AudioRecorderEventListener() {
                @Override
                public void onStop() {
                    chronometer.stop();
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    invalidateViews(STOP_RECORDING);
                }

                @Override
                public void onError(Exception e) {
                    chronometer.stop();
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    invalidateViews(BEFORE_START_RECORDING);
                }
            });
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();
            invalidateViews(START_RECORDING);

        }else {
            chronometer.setBase(SystemClock.elapsedRealtime()+timeWhenStopped);
            chronometer.start();
            CustomAudioRecorder.setRecorderState(CustomAudioRecorder.AUDIO_RECORDER_RECORDING);
            invalidateViews(START_RECORDING);
        }

    }

    /**
     * Method to get path of mp3 file
     * @return : path of the auido file.
     */
    public static String getAudioPath() {
        return Environment.getExternalStorageDirectory() + "/Sample.mp3";
    }

    /**
     * Method which nvalidates the view according to the recording states.
     * @param id : id of the current view.
     */
    private void invalidateViews(int id){
        switch (id){
            case BEFORE_START_RECORDING:
                findViewById(R.id.btn_start_recording).setVisibility(View.VISIBLE);
                findViewById(R.id.btn_pause_recording).setVisibility(View.GONE);
                findViewById(R.id.btn_stop_recording).setVisibility(View.GONE);
                findViewById(R.id.btn_play_audio).setVisibility(View.VISIBLE);
                findViewById(R.id.btn_play_audio).setEnabled(false);
                findViewById(R.id.btn_play_audio).setAlpha(0.5f);
                break;
            case STOP_RECORDING:
                findViewById(R.id.btn_start_recording).setVisibility(View.VISIBLE);
                findViewById(R.id.btn_pause_recording).setVisibility(View.GONE);
                findViewById(R.id.btn_stop_recording).setVisibility(View.GONE);
                findViewById(R.id.btn_play_audio).setVisibility(View.VISIBLE);
                findViewById(R.id.btn_play_audio).setEnabled(true);
                findViewById(R.id.btn_play_audio).setAlpha(1.0f);
                break;
            case START_RECORDING:
                findViewById(R.id.btn_start_recording).setVisibility(View.GONE);
                findViewById(R.id.btn_pause_recording).setVisibility(View.VISIBLE);
                findViewById(R.id.btn_stop_recording).setVisibility(View.VISIBLE);
                findViewById(R.id.btn_play_audio).setVisibility(View.VISIBLE);
                findViewById(R.id.btn_play_audio).setEnabled(false);
                findViewById(R.id.btn_play_audio).setAlpha(0.5f);
                break;
            case PAUSE_RECORDING:
                findViewById(R.id.btn_start_recording).setVisibility(View.VISIBLE);
                findViewById(R.id.btn_pause_recording).setVisibility(View.GONE);
                findViewById(R.id.btn_stop_recording).setVisibility(View.VISIBLE);
                findViewById(R.id.btn_play_audio).setVisibility(View.VISIBLE);
                findViewById(R.id.btn_play_audio).setEnabled(false);
                findViewById(R.id.btn_play_audio).setAlpha(0.5f);
                break;
        }
    }

    @Override
    public void onFileSizeReached() {
        CustomAudioRecorder.setRecorderState(CustomAudioRecorder.AUDIO_RECORDER_STOP);
        Log.w(TAG,"Maximum file size has been reached.");
    }
}
