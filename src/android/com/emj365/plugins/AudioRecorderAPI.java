package com.emj365.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.CountDownTimer;
import android.os.Environment;
import android.content.Context;
import java.util.UUID;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;



public class AudioRecorderAPI extends CordovaPlugin {

  private static final int RECORDER_SAMPLERATE = 8000;
  private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
  private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
  private int BufferElements2Rec = 1024;
  private int BytesPerElement = 2;
  private AudioRecord recorder = null;

  private String outputFile;
  private CountDownTimer countDowntimer;
  private boolean isRecording = false;

  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Context context = cordova.getActivity().getApplicationContext();
    Integer seconds;
    if (args.length() >= 1) {
      seconds = args.getInt(0);
    } else {
      seconds = 7;
    }
    if (action.equals("record")) {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
            RECORDER_SAMPLERATE, RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);
      try {
         recorder.startRecording();
         isRecording= true;
         writeAudioDataToFile();
      } catch (final Exception e) {
        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
            callbackContext.error(e.getMessage());
          }
        });
        return false;
      }

      countDowntimer = new CountDownTimer(seconds * 1000, 1000) {
        public void onTick(long millisUntilFinished) {}
        public void onFinish() {
          stopRecord(callbackContext);
        }
      };
      countDowntimer.start();
      return true;
    }

    if (action.equals("stop")) {
      countDowntimer.cancel();
      stopRecord(callbackContext);
      return true;
    }

    return false;
  }

  private void stopRecord(final CallbackContext callbackContext) {
      if (recorder!=null) {
        recorder.stop();
        recorder.release();
        recorder = null;
    }
    isRecording = false;
    
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        callbackContext.success(outputFile);
      }
    });
  }
  
  private byte[] short2byte(short[] sData) {
    int shortArrsize = sData.length;
    byte[] bytes = new byte[shortArrsize * 2];

    for (int i = 0; i < shortArrsize; i++) {
        bytes[i * 2] = (byte) (sData[i] & 0x00FF);
        bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
        sData[i] = 0;
    }
    return bytes;

}
  
  
private void writeAudioDataToFile() {
    outputFile = context.getFilesDir().getAbsoluteFile() + "/"
        + UUID.randomUUID().toString() + ".pcm";
    short sData[] = new short[BufferElements2Rec];

    FileOutputStream os = null;
    try {
        os = new FileOutputStream(outputFile);
    } catch (FileNotFoundException e) {
        e.printStackTrace();
    }

    while (isRecording) {
        // gets the voice output from microphone to byte format

        recorder.read(sData, 0, BufferElements2Rec);
        System.out.println("Short wirting to file" + sData.toString());
        try {
            // // writes the data to file from buffer
            // // stores the voice buffer

            byte bData[] = short2byte(sData);

            os.write(bData, 0, BufferElements2Rec * BytesPerElement);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    try {
        os.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
}
  
  
  
}
