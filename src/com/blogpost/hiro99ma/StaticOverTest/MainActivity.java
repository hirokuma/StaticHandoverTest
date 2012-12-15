package com.blogpost.hiro99ma.StaticOverTest;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	
	public void onHandover(View v) {
		EditText ssid_edit = (EditText)findViewById(R.id.editSsid);
		EditText key_edit = (EditText)findViewById(R.id.editKey);

		String ssid = ssid_edit.getText().toString();
		String key = key_edit.getText().toString();
		
		boolean ret = wifiConfigWep(ssid, key);
		if(ret) {
			Toast.makeText(this, "OK", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "fail...", Toast.LENGTH_SHORT).show();
		}
	}
	
	
	private boolean wifiConfigWep(String ssid, String key) {
		boolean ret = true;
		int id = -1;
		
		if((ssid == null) || (ssid.length() == 0)) {
			return false;
		}
		if((key == null) || (key.length() != 13)) {
			return false;
		}
		
		WifiManager wifiMgr = (WifiManager)getSystemService(WIFI_SERVICE);
		WifiConfiguration conf = new WifiConfiguration();
		
		conf.SSID = "\"" + ssid + "\"";
		conf.hiddenSSID = true;
		conf.status = WifiConfiguration.Status.DISABLED;
		conf.priority = 40;
		conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
		conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
	//	conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
		conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
		conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
		conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
		//conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
		conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
		//conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
	
		conf.wepKeys[0] = "\"" + key + "\"";
		conf.wepTxKeyIndex = 0;
		
		if(ret) {
			boolean res1 = wifiMgr.setWifiEnabled(true);
			if(!res1) {
				Log.d("xx", "oops1");
				ret = false;
			}
		}
		if(ret) {
			id = wifiMgr.addNetwork(conf);
			if(id == -1) {
				Log.d("xxx", "network id : " + id);
				ret = false;
			}
		}
		if(ret) {
			boolean res3 = wifiMgr.saveConfiguration();
			if(!res3) {
				Log.d("xx", "oops3");
				ret = false;
			}
		}
		if(ret) {
			wifiMgr.updateNetwork(conf);
			boolean res4 = wifiMgr.enableNetwork(id, true);
			if(!res4) {
				Log.d("xx", "oops4");
				ret = false;
			}
		}

		return ret;
	}
}
