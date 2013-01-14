package com.blogpost.hiro99ma.StaticOverTest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NdefRecord;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Parcelable;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private final byte[] kWPS = new byte[] {
					0x61, 0x70, 0x70, 0x6c, 0x69, 0x63, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x2f,
					//a   p     p     l     i     c     a     t     i     o     n     /
					0x76, 0x6e, 0x64, 0x2e, 0x77, 0x66, 0x61, 0x2e, 0x77, 0x73, 0x63,
					//v   n     d     .     w     f     a     .     w     s     c
    };
    private String mSsid = null;
    private String mKey = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//ソフトキーボードは邪魔
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		//NFC
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0,
                        new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
        if (mAdapter != null) {
        	IntentFilter[] intentFilter = new IntentFilter[] {
        		new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
        	};
        	String[][] techList = new String[][] {
        		{
        			android.nfc.tech.NfcF.class.getName()
        		}
        	};
        	mAdapter.enableForegroundDispatch(this, mPendingIntent, intentFilter, techList);
        }
	}

    @Override
    public void onPause() {
        super.onPause();
        if (mAdapter != null) {
        	mAdapter.disableForegroundDispatch(this);
        }
    }


    @Override
    public void onNewIntent(Intent intent) {
    	boolean ret = true;

    	super.onNewIntent(intent);
    	
    	String action = intent.getAction();
    	if(!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) && !NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
    		return;
    	}
    	
    	Parcelable[] pac = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
    	if((pac == null) || (pac.length == 0)) {
    		//Nexus7だとFeliCa LiteでNDEFが113-127byteだと解析に失敗する
			Toast.makeText(this, "fail 1...", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	NdefRecord[] recs = ((NdefMessage)pac[0]).getRecords();
    	if((recs == null) || (recs.length == 0)) {
			Toast.makeText(this, "fail 2...", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	for (NdefRecord rec : recs) {
    		ret = parseRecord(rec);
    		if(!ret) {
    			break;
    		}
    	}
    	
    	if(ret) {
    		wifiConfigWpa2Psk(mSsid, mKey);
    		Toast.makeText(this, "OK", Toast.LENGTH_SHORT).show();
    	} else {
			Toast.makeText(this, "fail 3...", Toast.LENGTH_SHORT).show();
    	}
    }

    private boolean parseRecord(NdefRecord rec) {
    	final short tnf = rec.getTnf();
    	final byte[] type = rec.getType();
    	boolean ret = true;
    	
    	if(tnf == NdefRecord.TNF_WELL_KNOWN) {
    		if(Arrays.equals(type, NdefRecord.RTD_HANDOVER_SELECT)) {
    			//Hs
    			ret = parseHs(rec);
    		} else if(Arrays.equals(type, NdefRecord.RTD_ALTERNATIVE_CARRIER)) {
    			//ac
    			ret = parseAc(rec);
    		}
    	} else if(tnf == NdefRecord.TNF_MIME_MEDIA) {
    		if(Arrays.equals(type, kWPS)) {
    			//application/vnd.wfa.wsc
    			parseWps(rec);
    		}
    	}
    	
    	return ret;
    }
    
    private boolean parseHs(NdefRecord rec) {
    	boolean ret = true;
    	byte[] payload = rec.getPayload();
    	if(payload == null) {
    		return false;
    	}
    	
    	byte version = payload[0];
    	if (version >= 0x12) {
    		//OK
    	} else {
    		ret = false;
    	}
    	
    	if(ret) {
    		if ((payload[1] == (byte)0xd1) &&
    						(payload[2] == 0x02) &&
    						(payload[3] == 0x04) &&
    						(payload[4] == (byte)'a') &&
    						(payload[5] == (byte)'c')) {
    			//OK
    		} else {
    			ret = false;
    		}
    	}
    	if(ret) {
    		//ac payload
    		if ((payload[6] == 0x01) &&						//PowerState
    						(payload[7] == 0x01) &&			//Carrier Data Ref Len
    						(payload[8] == (byte)'0') &&	//Carrier Data Ref
    						(payload[9] == 0x00)) {			//Aux Data Reference Count
    			//OK
    		} else {
    			ret = false;
    		}
    	}
    	
    	return ret;
    }
    
    private boolean parseAc(NdefRecord rec) {
    	return true;
    }
    
    private int getBE(byte upper, byte lower) {
    	int u = (((int)upper) & 0xff) << 8;
    	int l = lower & 0xff;
    	return u | l;
    }
    
    private boolean parseWps(NdefRecord rec) {
    	boolean ret = true;
    	
    	byte[] id = rec.getId();
    	if(id[0] != (byte)'0') {
    		return false;
    	}
    	
    	byte[] payload = rec.getPayload();
    	if(payload == null) {
    		return false;
    	}
    	
    	int plen = payload.length;
    	int pos = 0;
    	while (plen > pos) {
    		int type = getBE(payload[pos], payload[pos+1]);
    		int len = getBE(payload[pos+2], payload[pos+3]);
    		switch(type) {
    		case 0x104a:
    			//Version
    			pos += 4;
    			if ((len == 1) && (payload[pos] >= 0x10)) {
    				//OK
    				pos++;
    			} else {
    				ret = false;
    			}
    			break;
    			
    		case 0x100e:
    			//Credential
    			pos += 4;
    			{
    				int p = 0;
    				while(len > p) {
    					int t = getBE(payload[pos+p], payload[pos+p+1]);
    					int l = getBE(payload[pos+p+2], payload[pos+p+3]);
    					switch(t) {
    					case 0x1026:
    						//Network Index
    						p += 4;
    						if((l == 0x01) && (payload[pos+p] == 0x01)) {
    							//OK
    							p += l;
    						} else {
    							ret = false;
    						}
    						break;
    						
    					case 0x1045:
    						//SSID
    						p += 4;
    						try {
								mSsid = new String(payload, pos+p, l, "US-ASCII");
								p += l;
							} catch (UnsupportedEncodingException e) {
								ret = false;
							}
    						break;
    						
    					case 0x1003:
    						//Auth Type
    						p += 4;
    						if (l == 0x02) {
    							int auth = getBE(payload[pos+p], payload[pos+p+1]);
    							if (auth == 0x0020) {
    								//今回はWPA2/PSKのみOK
    								p += l;
    							} else {
    								ret = false;
    							}
    						} else {
    							ret = false;
    						}
    						break;
    						
    					case 0x100f:
    						//Enc Type
    						p += 4;
    						if (l == 0x02) {
    							int enc = getBE(payload[pos+p], payload[pos+p+1]);
    							if (enc == 0x0008) {
    								//今回はAESのみOK
    								p += l;
    							} else {
    								ret = false;
    							}
    						} else {
    							ret = false;
    						}
    						break;
    						
    					case 0x1027:
    						//Network Key
    						p += 4;
    						try {
								mKey = new String(payload, pos+p, l, "US-ASCII");
								p += l;
							} catch (UnsupportedEncodingException e) {
								ret = false;
							}
    						break;
    						
    					case 0x1020:
    						//MAC Addr
    						//今回は使わない
    						p += 4 + l;
    						break;
    						
    					default:
    						p += 4 + l;
    						break;
    					}
    					
    					if(!ret) {
    						break;
    					}
    				}
    				pos += p;
    			}
    			break;
    			
    		default:
    			pos += 4 + len;
    			break;
    		}
    		
    		if(!ret) {
    			break;
    		}
    	}

    	if(!ret) {
			mSsid = null;
			mKey = null;
    	}
    	return ret;
    }
    
	public void onHandover(View v) {
		EditText ssid_edit = (EditText)findViewById(R.id.editSsid);
		EditText key_edit = (EditText)findViewById(R.id.editKey);

		String ssid = ssid_edit.getText().toString();
		String key = key_edit.getText().toString();

		//boolean ret = wifiConfigWep(ssid, key);
		boolean ret = wifiConfigWpa2Psk(ssid, key);
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


	private boolean wifiConfigWpa2Psk(String ssid, String key) {
		boolean ret = true;
		int id = -1;

		if((ssid == null) || (ssid.length() == 0)) {
			return false;
		}
		if((key == null) || (key.length() == 0)) {
			return false;
		}

		WifiManager wifiMgr = (WifiManager)getSystemService(WIFI_SERVICE);
		WifiConfiguration conf = new WifiConfiguration();

		conf.SSID = "\"" + ssid + "\"";
		conf.hiddenSSID = true;
		conf.status = WifiConfiguration.Status.DISABLED;
		conf.priority = 40;
		conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
		conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
		conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

		conf.preSharedKey = "\"" + key + "\"";

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
