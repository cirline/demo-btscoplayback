package com.wizhong.btscoplayback;

import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;
import android.widget.Toast;

public class BTBroadcastReceiver extends BroadcastReceiver {
	static final String LOG_TAG = "BTBroadcastReceiver";
	static final boolean DEBUG = true;
	static Context mContext = null;
	private int mBluetoothHeadsetState = 0;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		Log.e(LOG_TAG, "BT connect changed!");
		
		if(intent.getAction().equals(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)) {
			handleScoChange(context, intent);
		} else if(intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
			mBluetoothHeadsetState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
                    BluetoothHeadset.STATE_DISCONNECTED);
			Log.d(LOG_TAG, "==> new state: " + mBluetoothHeadsetState);
			updateBluetoothIndication(true);  // Also update any visible UI if necessary
		}
	}
	
	public void updateBluetoothIndication(boolean it) {
		if (mBluetoothHeadsetState == BluetoothProfile.STATE_CONNECTED) {
			Log.i(LOG_TAG, "BluetoothProfile.STATE_CONNECTED");
			connect();
		} else if (mBluetoothHeadsetState == BluetoothProfile.STATE_DISCONNECTED) {
			Log.i(LOG_TAG, "BluetoothProfile.STATE_DISCONNECTED");
			disconnect();
		}
	}
	
	public void handleScoChange(Context context, Intent intent) {
		// if(DEBUG)
		// Log.e(TAG, " mSCOHeadsetAudioState--->onReceive");

		int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE,
				-1);

		if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
			Log.i(LOG_TAG, "BT SCO Music is now enabled. Play song in Media Player");
		} else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
			Log.i(LOG_TAG, "BT SCO Music is now disabled");
		}
	}

	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	private AudioManager mAudioManager = null;

	private void disconnect() {
		init();
		
        if(DEBUG)
            Log.e(LOG_TAG, "BTSCOApp Checkbox Unchecked ");
         mAudioManager.setBluetoothScoOn(false);
         mAudioManager.stopBluetoothSco();
	}
	
	private void init() {
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
	
	private void connect() {
		init();
		
		// 匹配名字
		if (DEBUG)
			Log.e(LOG_TAG, "BTSCOApp: Checkbox Checked ");
		if (mAudioManager != null) {
			mAudioManager.setBluetoothScoOn(true);
			mAudioManager.startBluetoothSco();
			// OMAP4 has dedicated support for MM playback on BT SCO
			// so just establish SCO connection and play music in media player
			// OMAP4 ABE takes care of 44.1 to 8k conversion.

			// For other platform or omap3, the user
			// needs to play mono 8k sample using aplay on shell
		}
		
		//MainActivity.updateRecordingState();
		
	}

}
