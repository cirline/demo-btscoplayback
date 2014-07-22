package com.wizhong.btscoplayback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BTBroadcastReceiver extends BroadcastReceiver {
	static final String LOG_TAG = "BTBroadcastReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		
		//Log.e(LOG_TAG, "BT connect changed!");
		String action = intent.getAction();
		
		if(action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			Log.i(LOG_TAG, Intent.ACTION_BOOT_COMPLETED);
			Intent service = new Intent(context, BtService.class);
			context.startService(service);
	    }
	}
}
