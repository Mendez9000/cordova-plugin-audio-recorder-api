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
import android.media.MediaRecorder;
import mp3.Main;



public class AudioRecorderAPI extends CordovaPlugin {
  private static final int RECORDER_BPP = 16;
  private static final int RECORDER_SAMPLERATE = 8000;
  private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_CONFIGURATION_STEREO;
  private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
  private int BufferElements2Rec = 1024;
  private int BytesPerElement = 2;
  private AudioRecord recorder = null;

  private CountDownTimer countDowntimer;
  private boolean isRecording = false;
  private String outputFile=null;
  private String outputFileTmp=null;
  private String error= "";
  private int bufferSize = 0;

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
       bufferSize = AudioRecord.getMinBufferSize
              (RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING)*3;

        outputFileTmp = context.getFilesDir().getAbsoluteFile() + "/" + UUID.randomUUID().toString() + ".pcm";
        outputFile = context.getFilesDir().getAbsoluteFile() + "/" + UUID.randomUUID().toString() + ".wav";
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
            RECORDER_SAMPLERATE, RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);
      try {
         recorder.startRecording();
         isRecording= true;
         
         
        cordova.getThreadPool().execute(new Runnable() {
          public void run() {
           writeAudioDataToFile();
          }
        });
         
         
         
        
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
    
    copyWaveFile(outputFileTmp, outputFile);
    
    cordova.getThreadPool().execute(new Runnable() {
      public void run() {
        error+= "..PASO-10";
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
      error+= "..PASO-0";
    short sData[] = new short[BufferElements2Rec];

    FileOutputStream os = null;
    try {
       error+= "..PASO2";
        os = new FileOutputStream(outputFileTmp);
    } catch (FileNotFoundException e) {
       error+= "..ERROR0";
        e.printStackTrace();
    }
     error+= "..PASO3";

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
          error+= "..ERROR1";
            e.printStackTrace();
        }
    }

    try {
        os.close();
         error+= "..PASO4";
    } catch (IOException e) {
        error+= "..ERROR2";
        e.printStackTrace();
    }
  }
  
  private void copyWaveFile(String inFilename,String outFilename){
         FileInputStream in = null;
         FileOutputStream out = null;
         long totalAudioLen = 0;
         long totalDataLen = totalAudioLen + 36;
         long longSampleRate = RECORDER_SAMPLERATE;
         int channels = 2;
         long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

         byte[] data = new byte[bufferSize];

         try {
             in = new FileInputStream(inFilename);
             out = new FileOutputStream(outFilename);
             totalAudioLen = in.getChannel().size();
             totalDataLen = totalAudioLen + 36;

            

             WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                          longSampleRate, channels, byteRate);

             while(in.read(data) != -1) {
                 out.write(data);
             }

             in.close();
             out.close();
         } catch (FileNotFoundException e) {
             e.printStackTrace();
         } catch (IOException e) {
             e.printStackTrace();
         }
     }
  
   private void WriteWaveFileHeader(
         FileOutputStream out, long totalAudioLen,
         long totalDataLen, long longSampleRate, int channels,
         long byteRate) throws IOException
     {
         byte[] header = new byte[44];

         header[0] = 'R';  // RIFF/WAVE header
         header[1] = 'I';
         header[2] = 'F';
         header[3] = 'F';
         header[4] = (byte) (totalDataLen & 0xff);
         header[5] = (byte) ((totalDataLen >> 8) & 0xff);
         header[6] = (byte) ((totalDataLen >> 16) & 0xff);
         header[7] = (byte) ((totalDataLen >> 24) & 0xff);
         header[8] = 'W';
         header[9] = 'A';
         header[10] = 'V';
         header[11] = 'E';
         header[12] = 'f';  // 'fmt ' chunk
         header[13] = 'm';
         header[14] = 't';
         header[15] = ' ';
         header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
         header[17] = 0;
         header[18] = 0;
         header[19] = 0;
         header[20] = 1;  // format = 1
         header[21] = 0;
         header[22] = (byte) channels;
         header[23] = 0;
         header[24] = (byte) (longSampleRate & 0xff);
         header[25] = (byte) ((longSampleRate >> 8) & 0xff);
         header[26] = (byte) ((longSampleRate >> 16) & 0xff);
         header[27] = (byte) ((longSampleRate >> 24) & 0xff);
         header[28] = (byte) (byteRate & 0xff);
         header[29] = (byte) ((byteRate >> 8) & 0xff);
         header[30] = (byte) ((byteRate >> 16) & 0xff);
         header[31] = (byte) ((byteRate >> 24) & 0xff);
         header[32] = (byte) (2 * 16 / 8);  // block align
         header[33] = 0;
         header[34] = RECORDER_BPP;  // bits per sample
         header[35] = 0;
         header[36] = 'd';
         header[37] = 'a';
         header[38] = 't';
         header[39] = 'a';
         header[40] = (byte) (totalAudioLen & 0xff);
         header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
         header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
         header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

         out.write(header, 0, 44);
     }
  
  
}
