package com.recorder.customaudiorecorder;

import android.media.AudioRecord;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A class for making custom audio recorder with play and pause functionality.
 * @author Agile Axis
 */

public class  CustomAudioRecorder {

    private static AudioRecord audioRecorder;//instance for audio recorder.
    private static DataOutputStream dataOutputStream; //instance for writing data out stream.
    private static int audioSource;//instance for audio source
    private static int sampleRateInHz;
    private static int channelConfig;
    private static int audioFormat;//audio format of the file.
    private static int bufferSizeInBytes;
    private static int recorderState=2;// default state for the recorder state stop
    private static long threadMaxFileSizeInBytes;
    private static boolean isFileAlreadyWritten;

    //Constant for recording status;
    public final static int AUDIO_RECORDER_PAUSE=1;
    public final static int AUDIO_RECORDER_STOP=2;
    public final static int AUDIO_RECORDER_RECORDING=3;

    private final static String TAG="CustomAudioRecorder";

    private static AudioRecorderEventListener eventListener;
    private static MaxFileSizeReachedListener maxFileSizeReachedListener;

    /**
     * Call back which identifies two clauses first when the recording has been stopped and the
     * second one when there is an error occurs during the recording.
     */
    public interface AudioRecorderEventListener {
        /**
         * called when recording has been stopped.
         */
        void onStop();

        /**
         * Called on any error instance while recording the audio.
         * @param e : returns the exception.
         */
        void onError(Exception e);
    }

    /**
     * Callback to limit the audio recording to a size.
     */
    public interface MaxFileSizeReachedListener{
        /**
         * Call on maximum file size reach.
         */
        void onFileSizeReached();
    }

    /**
     *
     * Constructor fot initialising the {#CustomAudioRecorder}.
     * @param audioSource2 : audio source of the camera.
     * @param sampleRateInHz2 : IT is expressed in Hertz. 41000HZ sampling rate supports for
     *                       all devices.
     * @param channelConfig2 : Describe the channel for the configuaration.
     *                      {@link android.media.AudioFormat#CHANNEL_IN_MONO}
     *                      or {@link android.media.AudioFormat#CHANNEL_IN_STEREO}.
     *                      However {@link android.media.AudioFormat#CHANNEL_IN_MONO}
     *                      is works in all devices.
     * @param audioFormat2 : Audio Formate for the file.
     * {@link android.media.AudioFormat#ENCODING_PCM_16BIT} works for all device.
     */
    public CustomAudioRecorder(int audioSource2,
                               int sampleRateInHz2,
                               int channelConfig2,
                               int audioFormat2) {
        audioSource = audioSource2;
        sampleRateInHz = sampleRateInHz2;
        channelConfig = channelConfig2;
        audioFormat = audioFormat2;
        bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig,
                audioFormat);
    }

    /**
     * Setter for {@link MaxFileSizeReachedListener}.
     * @param maxFileSizeReachedListener2: Calling instance for {@link MaxFileSizeReachedListener}.
     */
    public void setMaxFileSizeReachedListener(MaxFileSizeReachedListener maxFileSizeReachedListener2) {
        maxFileSizeReachedListener = maxFileSizeReachedListener2;
    }

    /**
     * Getter for the recorder current state.
     * @return : {@link #recorderState}.
     */
    public static int getRecorderState() {
        return recorderState;
    }

    /**
     * Setter for the recorder state.
     * @param recorderState2 : calling instance for the {@link #recorderState}.
     */
    public static void setRecorderState(int recorderState2) {
        recorderState = recorderState2;
    }

    /**
     * Setter for {@link #threadMaxFileSizeInBytes}.
     * @param threadMaxFileSizeInBytes2 : calling instance for maximum file size.
     */
    public void setThreadMaxFileSizeInBytes(long threadMaxFileSizeInBytes2) {
        threadMaxFileSizeInBytes = threadMaxFileSizeInBytes2;
    }

    /**
     * Method set audio path for the file.
     * @param filePath : file path.
     */
    public void setAudioPath(String filePath){
        try {
            dataOutputStream=new DataOutputStream(new
                    BufferedOutputStream(new FileOutputStream(new File(filePath))));
        } catch (FileNotFoundException e) {
            Log.d(TAG,"File not exist.");
            eventListener.onError(e);
            releaseAudioRecorder();
        }
    }

    /**
     * Method which prepare the audio der.
     * @return : returns the state of audio recorder.
     */
    private static int prepareVideoRecorder(){
        audioRecorder = new AudioRecord(audioSource,
                sampleRateInHz,
                channelConfig,
                audioFormat,
                bufferSizeInBytes*2);
        isFileAlreadyWritten =false;
        return audioRecorder.getState();
    }

    /**
     * Method for proceed audio recording. this method encode the pcm_16B_bit file to MP# by using
     * Android Lame library. anw write the dat in a file. if uer paused the recording then no data
     * will be written.
     * @return : true if recording has been stopped successfully.
     */
    private static boolean proceedRecording() {
        audioRecorder.startRecording();
        setRecorderState(AUDIO_RECORDER_RECORDING);
        //Init Android Lame for mp3 encoding.
        AndroidLame androidLame = new AndroidLame();
        short[] Data = new short[bufferSizeInBytes*2*5];
        byte[] mp3buffer = new byte[(int) (7200 + Data.length * 2 * 1.25)];
        long currentFileSizeInBytes=0;
        //loop runs till recorder is recording or paused.
        while(getRecorderState()==AUDIO_RECORDER_RECORDING
                ||getRecorderState()==AUDIO_RECORDER_PAUSE) {
            //when recorder paused force thread to go in sleep state for 100 milliseconds.
            if (getRecorderState()==AUDIO_RECORDER_PAUSE){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Thread has been interrupted");
                    releaseAudioRecorder();
                    eventListener.onError(e);
                }
            }else {
                final int length = audioRecorder.read(Data, 0, bufferSizeInBytes);
                int bytesEncoded = 0;
                if (length > 0) {
                    int i=0;
                    for (short a:Data){
                        a=(short) Math.min((int)(a * 2.00), (int)Short.MAX_VALUE);
                        Data[i]=a;
                        i++;
                    }
                    bytesEncoded = androidLame.encode(Data, Data, length, mp3buffer);

                    if (bytesEncoded > 0) {
                        try {
                            dataOutputStream.write(mp3buffer, 0, bytesEncoded);
                        } catch (IOException e) {
                            Log.d(TAG, "There may be something wrong with the file so " +
                                    "recorder has been stopped.");
                            releaseAudioRecorder();
                            eventListener.onError(e);
                        }
                    }
                }
                currentFileSizeInBytes=currentFileSizeInBytes+bufferSizeInBytes;
                //If the next input clip goes over, just stop the thread now.
                if (0!=threadMaxFileSizeInBytes&&currentFileSizeInBytes+bytesEncoded>
                        threadMaxFileSizeInBytes){
                    releaseAudioRecorder();
                    Log.d(TAG,"Max file size has been reached. Stopping recording thread.");
                    new Thread(new MaxSizeReachedRunnable()).run();
                }
            }
        }
        if (!isFileAlreadyWritten) {
            int outputMp3buf = androidLame.flush(mp3buffer);
            if (outputMp3buf > 0) {
                try {
                    dataOutputStream.write(mp3buffer, 0, outputMp3buf);
                    dataOutputStream.close();
                    isFileAlreadyWritten =true;
                } catch (IOException e) {
                    Log.d(TAG,
                            "There may be something wrong with the file so recorder has been stopped.");
                    releaseAudioRecorder();
                    eventListener.onError(e);
                }
            }

            releaseAudioRecorder();
        }
        return true;
    }

    /**
     * Method which release the media recorder.
     */
    private static void releaseAudioRecorder(){
        if (null!=audioRecorder){
            audioRecorder.stop();
            audioRecorder.release();
            audioRecorder=null;
            setRecorderState(AUDIO_RECORDER_STOP);
        }
    }

    /**
     * Method to start audio recorder.
     * @param audioRecorderEventListener : for listening state of audio recording.
     */

    public void startRecorder(AudioRecorderEventListener audioRecorderEventListener){
        eventListener=audioRecorderEventListener;
        StartAsyncRecorder startAsyncRecorder =new StartAsyncRecorder();
        startAsyncRecorder.execute();
    }

    /**
     * Async task which prepare the audio recorder. and further start the recording in bakground
     * thread. when recording stopped it notifies the user.
     */
    private static class StartAsyncRecorder extends AsyncTask<Void,Void,Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            return prepareVideoRecorder() ==
                    AudioRecord.STATE_INITIALIZED && proceedRecording();

        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean) {
                //stop event listener.
                eventListener.onStop();
            }
        }

    }

    /**
     * Runnable that fires when the max file size has been reached approximately.
     */
    private static class MaxSizeReachedRunnable implements Runnable{

        @Override
        public void run() {
            if (maxFileSizeReachedListener !=null){
                maxFileSizeReachedListener.onFileSizeReached();
            }
        }
    }
}
