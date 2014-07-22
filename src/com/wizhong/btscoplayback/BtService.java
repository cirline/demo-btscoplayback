package com.wizhong.btscoplayback;

import java.util.Set;

import com.wizhong.btscoplayback.MainActivity.RecordPlayThread;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class BtService extends Service {
	private static String LOG_TAG = "BtService";
	private int mBluetoothHeadsetState;
	private Context mContext = null;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	private AudioManager mAudioManager = null;
	
	// audio 
    boolean isRecording = false;//是否录放的标记  
    static final int frequency = 8000;//44100;  
    static final int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;  
    static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;  
    int recBufSize,playBufSize;  
    AudioRecord audioRecord;  
    AudioTrack audioTrack;  
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(LOG_TAG, "onCreate");
		
		// second time
		IntentFilter btActionFilter = new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
		btActionFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
		registerReceiver(BtServiceActionReceiver, btActionFilter);
		
		// fisrt time
		checkFirstTime();
		
		mContext = this;
		initBT();
		audioRecordInit();
	}
	
	private void checkFirstTime() {
		
	}
	
	
	public void startRecoder() {
		Log.i(LOG_TAG, "startRecoder");
		if (isRecording)
			return;

		// start
		isRecording = true;
		new RecordPlayThread().start();// 开一条线程边录边放
	}

	public void stopRecoder() {
		Log.i(LOG_TAG, "stopRecoder");
		if (!isRecording)
			return;

		// stop
		isRecording = false;
	}
	
	private void audioRecordInit() {
		recBufSize = AudioRecord.getMinBufferSize(frequency,
				AudioFormat.CHANNEL_IN_MONO, audioEncoding);

		playBufSize = AudioTrack.getMinBufferSize(frequency,
				AudioFormat.CHANNEL_OUT_MONO, audioEncoding);
		// -----------------------------------------
		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,
				AudioFormat.CHANNEL_IN_MONO, audioEncoding, recBufSize * 10);

		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency,
				AudioFormat.CHANNEL_OUT_MONO, audioEncoding, playBufSize,
				AudioTrack.MODE_STREAM);
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
                //Toast.makeText(MainActivity.this, t.getMessage(), 1000);  
            }  
        }  
    }; 
	
	private BroadcastReceiver BtServiceActionReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)) {
				handleScoChange(context, intent);
			} else if(action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
				mBluetoothHeadsetState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
	                    BluetoothHeadset.STATE_DISCONNECTED);
				Log.d(LOG_TAG, "==> new state: " + mBluetoothHeadsetState);
				updateBluetoothIndication(true);  // Also update any visible UI if necessary
			}
		}
	};
	/**
	 * 经过测试这个个广播不准确，不好自己开好。
	 * @param context
	 * @param intent
	 */
	public void handleScoChange(Context context, Intent intent) {
		int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
		if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
			Log.i(LOG_TAG, "蓝牙录放回还测试已经开启！");
			Toast.makeText(mContext, "蓝牙录放回还测试已经开启！", Toast.LENGTH_SHORT).show();
		} else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
			Log.i(LOG_TAG, "蓝牙录放回还测试已经停止！");
			Toast.makeText(mContext, "蓝牙录放回还测试已经停止！", Toast.LENGTH_SHORT).show();
		}
	}
	
	public void updateBluetoothIndication(boolean it) {
		if (mBluetoothHeadsetState == BluetoothProfile.STATE_CONNECTED) {
			Log.i(LOG_TAG, "BluetoothProfile.STATE_CONNECTED");
			//先开录音，再改音频通道，否则会在更改音频通道后立即改回来的！！
			// 1.开启录放回还
			startRecoder();
			// 2.将音频通道改为BT SCO
			connect();
		} else if (mBluetoothHeadsetState == BluetoothProfile.STATE_DISCONNECTED) {
			Log.i(LOG_TAG, "BluetoothProfile.STATE_DISCONNECTED");
			// 1.关闭BT SCO
			disconnect();
			// 2.停止录放回还
			stopRecoder();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	private void disconnect() {
        Log.i(LOG_TAG, "BTSCOApp Checkbox Unchecked ");
        mAudioManager.setBluetoothScoOn(false);
        mAudioManager.stopBluetoothSco();
	}
	

	private void connect() {
		// 匹配名字
		Log.i(LOG_TAG, "BTSCOApp: Checkbox Checked ");
		if (mAudioManager != null) {
			mAudioManager.setBluetoothScoOn(true);
			mAudioManager.startBluetoothSco();
			// OMAP4 has dedicated support for MM playback on BT SCO
			// so just establish SCO connection and play music in media player
			// OMAP4 ABE takes care of 44.1 to 8k conversion.

			// For other platform or omap3, the user
			// needs to play mono 8k sample using aplay on shell
		} else {
			Log.e(LOG_TAG, "mAudioManger == null");
		}
		
	}
	private void initBT() {
		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(mContext, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			return;
		}

		// Check whether BT is enabled
		if (!mBluetoothAdapter.isEnabled()) {
			Toast.makeText(mContext, "Bluetooth is not enabled", Toast.LENGTH_LONG)
					.show();
			return;
		}

		// get the Audio Service context
		mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		if (mAudioManager == null) {
			Log.e(LOG_TAG, "mAudiomanager is null");
			return;
		}
		// Android 2.2 onwards supports BT SCO for non-voice call use case
		// Check the Android version whether it supports or not.
		if (!mAudioManager.isBluetoothScoAvailableOffCall()) {
			Toast.makeText(mContext,
					"Platform does not support use of SCO for off call",
					Toast.LENGTH_LONG).show();
			return;
		}

		// Check list of bonded devices
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
				.getBondedDevices();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
			// Loop through paired devices
			for (BluetoothDevice device : pairedDevices) {
				Log.e(LOG_TAG, "BT Device :" + device.getName() + " , BD_ADDR:"
						+ device.getAddress());
			}
			// To do:
			// Need to check from the paired devices which supports BT HF
			// profile
			// and take action based on that.
		} else {
			Toast.makeText(mContext,
					"No Paired Headset, Pair and connect to phone audio",
					Toast.LENGTH_LONG).show();
			return;
		}
	}
}
