package com.wizhong.btscoplayback;  

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;
  
public class MainActivity extends Activity {  
    /** Called when the activity is first created. */  
    Button btnRecord, btnStop, btnExit;  
    SeekBar skbVolume;//调节音量  
    boolean isRecording = false;//是否录放的标记  
    static final int frequency = 8000;//44100;  
    static final int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;  
    static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;  
    int recBufSize,playBufSize;  
    AudioRecord audioRecord;  
    AudioTrack audioTrack;  
    @Override  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
        setContentView(R.layout.activity_main);  
        setTitle("蓝牙录放回还"); 
        recBufSize = AudioRecord.getMinBufferSize(frequency,  
        		 AudioFormat.CHANNEL_IN_MONO, audioEncoding);  
  
        playBufSize=AudioTrack.getMinBufferSize(frequency,  
        		 AudioFormat.CHANNEL_OUT_MONO, audioEncoding);  
        // -----------------------------------------  
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,  
        		 AudioFormat.CHANNEL_IN_MONO, audioEncoding, recBufSize*10);  
  
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency,  
        		AudioFormat.CHANNEL_OUT_MONO, audioEncoding,  
                playBufSize, AudioTrack.MODE_STREAM);  
        //------------------------------------------  
        btnRecord = (Button) this.findViewById(R.id.btnRecord);  
        btnRecord.setOnClickListener(new ClickEvent());  
        btnStop = (Button) this.findViewById(R.id.btnStop);  
        btnStop.setOnClickListener(new ClickEvent());  
        btnExit = (Button) this.findViewById(R.id.btnExit);  
        btnExit.setOnClickListener(new ClickEvent());  
        skbVolume=(SeekBar)this.findViewById(R.id.skbVolume);  
        skbVolume.setMax(100);//音量调节的极限  
        skbVolume.setProgress(70);//设置seekbar的位置值  
        audioTrack.setStereoVolume(0.7f, 0.7f);//设置当前音量大小  
        skbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {  
              
            @Override  
            public void onStopTrackingTouch(SeekBar seekBar) {  
                float vol=(float)(seekBar.getProgress())/(float)(seekBar.getMax());  
                audioTrack.setStereoVolume(vol, vol);//设置音量  
            }  
              
            @Override  
            public void onStartTrackingTouch(SeekBar seekBar) {  
                // TODO Auto-generated method stub  
            }  
              
            @Override  
            public void onProgressChanged(SeekBar seekBar, int progress,  
                    boolean fromUser) {  
                // TODO Auto-generated method stub  
            }  
        });  
    }  
    
    @Override  
    protected void onDestroy() {  
        super.onDestroy();  
        android.os.Process.killProcess(android.os.Process.myPid());  
    }  
  
    class ClickEvent implements View.OnClickListener {  
  
        @Override  
        public void onClick(View v) {  
            if (v == btnRecord) {  
                isRecording = true;  
                new RecordPlayThread().start();// 开一条线程边录边放  
            } else if (v == btnStop) {  
                isRecording = false;  
            } else if (v == btnExit) {  
                isRecording = false;  
                MainActivity.this.finish();  
            }  
        }  
    }  
  
    class RecordPlayThread extends Thread {  
        public void run() {  
            try {  
                byte[] buffer = new byte[recBufSize];  
                audioRecord.startRecording();//开始录制  
                audioTrack.play();//开始播放  
                  
                while (isRecording) {  
                    //从MIC保存数据到缓冲区  
                    int bufferReadResult = audioRecord.read(buffer, 0,  
                            recBufSize);  
  
                    byte[] tmpBuf = new byte[bufferReadResult];  
                    System.arraycopy(buffer, 0, tmpBuf, 0, bufferReadResult);  
                    //写入数据即播放  
                    audioTrack.write(tmpBuf, 0, tmpBuf.length);  
                }  
                audioTrack.stop();  
                audioRecord.stop();  
            } catch (Throwable t) {  
                Toast.makeText(MainActivity.this, t.getMessage(), 1000);  
            }  
        }  
    };  
}  
