package com.sentaroh.android.SMBSync;

/*
The MIT License (MIT)
Copyright (c) 2011-2013 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to 
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or 
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import static com.sentaroh.android.SMBSync.SchedulerConstants.*;
import static com.sentaroh.android.SMBSync.Constants.*;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.ThreadCtrl;

@SuppressLint("Wakelock")
public class SMBSyncService extends Service {
	private GlobalParameters mGp=null;
	
	private SMBSyncUtil mUtil=null;
	
	private ThreadCtrl tcMirror=null, tcConfirm=null;
	
	private WakeLock mPartialWakeLock=null;
	
	private ISvcCallback callBackStub=null;

	private WifiManager mWifiMgr=null;
	@SuppressWarnings("unused")
	private WifiLock mWifiLock=null;
	
	private Context mContext=null;
	
	private WifiReceiver mWifiReceiver=new WifiReceiver();
	
	@SuppressWarnings("unused")
	private Handler mUiHandler=null;
	
	@Override
	public void onCreate() {
		super.onCreate();
		mGp=(GlobalParameters) getApplication();
		mContext=getApplicationContext();
		if (mGp.appContext==null) mGp.appContext=this.getApplicationContext();
		NotificationUtil.initNotification(mGp);
		mUtil=new SMBSyncUtil(getApplicationContext(), "Service", mGp);
		
		mUtil.addDebugLogMsg(1,"I","onCreate entered");

		mUiHandler=new Handler();
		
		mWifiMgr=(WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
		mWifiLock=mWifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL, "SMBSync-Service");
		
		initWifiStatus();
		
		IntentFilter int_filter = new IntentFilter();
  		int_filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
  		int_filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
  		int_filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		int_filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		int_filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		int_filter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
		registerReceiver(mWifiReceiver, int_filter);

		mPartialWakeLock=((PowerManager)getSystemService(Context.POWER_SERVICE))
    			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
	    				| PowerManager.ACQUIRE_CAUSES_WAKEUP
//	   	    				| PowerManager.ON_AFTER_RELEASE
	    				, "SMBSync-Service-Partial");
//		PackageInfo packageInfo;
//		try {
//			packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
//			int flags = packageInfo.applicationInfo.flags;
//			mDebugEnabled = (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
//		} catch (NameNotFoundException e) {
//			e.printStackTrace();
//		}           
		tcMirror=new ThreadCtrl(); 
		tcConfirm=new ThreadCtrl();
	};
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		WakeLock wl=((PowerManager)getSystemService(Context.POWER_SERVICE))
    			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
	    				| PowerManager.ACQUIRE_CAUSES_WAKEUP
	    				, "SMBSync-Service-1");
		wl.acquire();
		String action="";
		if (intent!=null) if (intent.getAction()!=null) action=intent.getAction();
		if (action.equals(SCHEDULER_INTENT_TIMER_EXPIRED)) {
			mUtil.addDebugLogMsg(1,"I","onStartCommand entered, action="+action);
			startSyncActivity();
		} else if (action.equals(SCHEDULER_INTENT_WIFI_OFF)) {
			mUtil.addDebugLogMsg(1,"I","onStartCommand entered, action="+action);
			setWifiOff();
		} else {
			mUtil.addDebugLogMsg(1,"I","onStartCommand entered, action="+action);
		}
		wl.release();
		return START_NOT_STICKY;
	};
	
	private void setWifiOn() {
		if (!mWifiMgr.isWifiEnabled()) {
			mWifiMgr.setWifiEnabled(true);
			mUtil.addDebugLogMsg(1,"I", "setWifiEnabled(true) issued");
		} else {
			mUtil.addDebugLogMsg(1,"I", "setWifiEnabled(true) not issued, because Wifi is already enabled");
		}
	};

	private void setWifiOff() {
		if (mWifiMgr.isWifiEnabled()) {
			mWifiMgr.setWifiEnabled(false);
			mUtil.addDebugLogMsg(1,"I", "setWifiEnabled(false) issued");
		} else {
			mUtil.addDebugLogMsg(1,"I", "setWifiEnabled(false) not issued, because Wifi is already disabled");
		}
	};

	private void startSyncActivity() {
		mUtil.addDebugLogMsg(1,"I", "startSyncActivity entered");
		final Handler hndl=new Handler();
		final WakeLock wake_lock=((PowerManager)getSystemService(Context.POWER_SERVICE))
    			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
	    				| PowerManager.ACQUIRE_CAUSES_WAKEUP
	    				, "SMBSync-startSync-Partial");
		final WifiLock wifi_lock=mWifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL, "SMBSync-startSync");
		wake_lock.acquire();
		Thread th=new Thread() {
			@Override
			public void run() {
				SchedulerParms sp=new SchedulerParms();
				SchedulerUtil.loadScheduleData(sp, mGp.appContext);
				if (sp.syncWifiOnBeforeSyncStart && !mWifiMgr.isWifiEnabled()) {
					setWifiOn();
					wifi_lock.acquire();
					if (sp.syncStartDelayTimeAfterWifiOn==0) {
//						if (mGp.settingWifiOption.equals(SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_OFF)) {
//							mUtil.addDebugLogMsg(1,"I", "Sync not delayed because WiFi is not turn on");						
//						} else if (mGp.settingWifiOption.equals(SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_ON)) {
//							mUtil.addDebugLogMsg(1,"I", "Sync start wait until WiFi is turn on");
//							SystemClock.sleep(1000);
//							while(!mWifiMgr.isWifiEnabled()) {
//								SystemClock.sleep(1000);
//							}
//						} else if (mGp.settingWifiOption.equals(SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP)) {
//							mUtil.addDebugLogMsg(1,"I", "Sync start wait until WiFi access point is connected");
//							int wc=30;
//							while(wc>0) {
//								SystemClock.sleep(1000);
//								if (mWifiMgr.isWifiEnabled()) {
//									String ssid=mWifiMgr.getConnectionInfo().getSSID();
//									if (ssid!=null && !ssid.equals("0x") && !ssid.equals("") && 
//											!ssid.equals("<unknown ssid>")) {
//										break;
//									} else {
//										wc--;
//									}
//								}
//							}
//						} else if (mGp.settingWifiOption.equals(SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_SPEC_AP)) {
//						}
					} else {
						mUtil.addDebugLogMsg(1,"I", "Sync start delayed "+sp.syncStartDelayTimeAfterWifiOn+"Seconds");
						SystemClock.sleep(sp.syncStartDelayTimeAfterWifiOn*1000);
					}
				}

		    	Intent in=new Intent(mContext,SMBSyncMain.class);
				in.setAction(Intent.ACTION_MAIN);
				in.addCategory(Intent.CATEGORY_LAUNCHER);
				in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

		    	in.putExtra(SMBSYNC_SCHEDULER_ID,"SMBSync Scheduler");
		    	String[] prof=sp.syncProfile.split(",");
		    	in.putExtra(SMBSYNC_EXTRA_PARM_SYNC_PROFILE, prof);
		    	in.putExtra(SMBSYNC_EXTRA_PARM_AUTO_START, true);
		    	in.putExtra(SMBSYNC_EXTRA_PARM_AUTO_TERM, sp.syncOptionAutoterm);
		    	in.putExtra(SMBSYNC_EXTRA_PARM_BACKGROUND_EXECUTION, sp.syncOptionBgExec);
//		    	mGp.appContext.startActivity(in);
		    	mContext.startActivity(in);
		    	
		    	hndl.postDelayed(new Runnable(){
					@Override
					public void run() {
						mUtil.addDebugLogMsg(1,"I", "startSyncActivity Wakelock and Wifilock released");
						if (wake_lock.isHeld()) wake_lock.release();
						if (wifi_lock.isHeld()) wifi_lock.release();
					}
		    	}, 1000);
			}
		};
		th.start();
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		mUtil.addDebugLogMsg(1,"I","onBind entered,action="+arg0.getAction());
//		if (arg0.getAction().equals("MessageConnection")) 
			return mSvcClientStub;
//		else return svcInterface;
	};
	
	@Override
	public boolean onUnbind(Intent intent) {
		mUtil.addDebugLogMsg(1,"I","onUnbind entered");
		return super.onUnbind(intent);
	};
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mUtil.addDebugLogMsg(1,"I","onDestroy entered");
		unregisterReceiver(mWifiReceiver);
		LogUtil.closeLogFile();
		Handler hndl=new Handler();
		hndl.postDelayed(new Runnable(){
			@Override
			public void run() {
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		}, 100);
//		glblParms.logWriter.close();
	};
    
    final private ISvcClient.Stub mSvcClientStub = new ISvcClient.Stub() {
		@Override
		public void setCallBack(ISvcCallback callback)
				throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlSetCallBack entered");
			callBackStub=callback;
		}

		@Override
		public void removeCallBack(ISvcCallback callback)
				throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlRemoveCallBack entered");
			callBackStub=null;
		}

		@Override
		public void aidlStartThread() throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlStartThread entered");
			startThread();
		}

		@Override
		public void aidlCancelThread() throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlCancelThread entered");
			synchronized(tcMirror) {
				tcMirror.setDisabled();
				tcMirror.notify();
			}
		}

		@Override
		public void aidlStartForeground() throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlStartForeground entered");
//			NotificationUtil.setNotificationEnabled(glblParms);
			startForeground(R.string.app_name,mGp.notification);
		}

		@Override
		public void aidlStopForeground(boolean clear_notification)
				throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlStopForeground entered, clear="+clear_notification);
//			if (clear_notification) NotificationUtil.setNotificationDisabled(glblParms);
			stopForeground(clear_notification);
		}

		@Override
		public void aidlConfirmResponse(int confirmed)
				throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlConfirmResponse entered, confirmed="+confirmed);
			synchronized(tcConfirm) {
				tcConfirm.setExtraDataInt(confirmed);
				tcConfirm.notify();
			}
		}

		@Override
		public void aidlStopService() throws RemoteException {
			stopSelf();
		}

		@Override
		public void aidlShowNotificationMsg(String prof, String fp, String msg)
				throws RemoteException {
			NotificationUtil.showOngoingMsg(mGp, System.currentTimeMillis(), prof, fp, msg);
		}
		
//		@Override
//		public void aidlSetNotificationIcon(int icon_res)
//				throws RemoteException {
////			Log.v("","icon="+icon_res);
////			Thread.currentThread().dumpStack();
//			NotificationUtil.setNotificationIcon(mGp, icon_res);
//		}
		
		@Override
		public void aidlAcqWakeLock() throws RemoteException {
			if (mPartialWakeLock.isHeld()) {
				mUtil.addDebugLogMsg(1, "I", "aidlAcqWakeLock WakeLock not acquired, already held.");
			} else {
				mPartialWakeLock.acquire();
				mUtil.addDebugLogMsg(1, "I", "aidlAcqWakeLock WakeLock acquired.");
			}
		}
		@Override
		public void aidlRelWakeLock() throws RemoteException {
			if (!mPartialWakeLock.isHeld()) {
				mUtil.addDebugLogMsg(1, "I", "aidlRelWakeLock WakeLock not released, not held.");
			} else {
				mPartialWakeLock.release();
				mUtil.addDebugLogMsg(1, "I", "aidlRelWakeLock WakeLock released.");
			}
		}
		
    };

	private void startThread() {
//		final Handler hndl=new Handler();
		if (Build.VERSION.SDK_INT<=10) {
			NotificationUtil.setNotificationIcon(mGp, R.drawable.ic_48_smbsync_run);
		} else {
			NotificationUtil.setNotificationIcon(mGp, R.drawable.ic_48_smbsync_run_anim);
		}
		tcConfirm.initThreadCtrl();
		tcMirror.initThreadCtrl();
		tcMirror.setEnabled();//enableAsyncTask();
		tcConfirm.setEnabled();//enableAsyncTask();
		NotifyEvent ntfy = new NotifyEvent(this);
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c, Object[] o) {
				NotificationUtil.setNotificationIcon(mGp, R.drawable.ic_48_smbsync_wait);
				String result_code="", result_msg="";
				if (tcMirror.isThreadResultSuccess()) {
					result_code=SMBSYNC_MIRROR_THREAD_RESULT_SUCCESS;
					result_msg=tcMirror.getThreadMessage();
				} else if (tcMirror.isThreadResultCancelled()) {
					result_code=SMBSYNC_MIRROR_THREAD_RESULT_CANCELLED;
					result_msg=tcMirror.getThreadMessage();
				} else if (tcMirror.isThreadResultError()) {
					result_code=SMBSYNC_MIRROR_THREAD_RESULT_ERROR;
					result_msg=tcMirror.getThreadMessage();
				}
				try {
					callBackStub.cbThreadEnded(result_code, result_msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {} 
		});
		
		Thread tm = new Thread(new MirrorIO(mGp, ntfy, tcMirror, tcConfirm, callBackStub)); 
		tm.setPriority(Thread.MIN_PRIORITY);
		tm.start();
	};
	
	private void initWifiStatus() {
		mGp.wifiIsActive=mWifiMgr.isWifiEnabled();
		if (mGp.wifiIsActive) {
			if (mWifiMgr.getConnectionInfo().getSSID()!=null) mGp.wifiSsid=mWifiMgr.getConnectionInfo().getSSID();
			else mGp.wifiSsid="";
		}
		mUtil.addDebugLogMsg(1,"I","Wi-Fi Status, Active="+mGp.wifiIsActive+", SSID="+mGp.wifiSsid);
	};
	
    final private class WifiReceiver  extends BroadcastReceiver {
		@Override
		final public void onReceive(Context c, Intent in) {
			String tssid=mWifiMgr.getConnectionInfo().getSSID();
			String wssid="";
			String ss=mWifiMgr.getConnectionInfo().getSupplicantState().toString();
			if (tssid==null || tssid.equals("<unknown ssid>")) wssid="";
			else wssid=tssid.replaceAll("\"", "");
			if (wssid.equals("0x")) wssid="";
			
			boolean new_wifi_enabled=mWifiMgr.isWifiEnabled();
			if (!new_wifi_enabled && mGp.wifiIsActive ) {
				mUtil.addDebugLogMsg(1,"I","WIFI receiver, WIFI Off");
				mGp.wifiSsid="";
				mGp.wifiIsActive=false;
				try {
					if (callBackStub!=null) callBackStub.cbWifiStatusChanged("On", "");
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			} else {
				if (ss.equals("COMPLETED") ||ss.equals("ASSOCIATING") || ss.equals("ASSOCIATED")) {
					if (mGp.wifiSsid.equals("") && !wssid.equals("")) {
						mUtil.addDebugLogMsg(1,"I","WIFI receiver, Connected WIFI Access point ssid="+wssid);
						mGp.wifiSsid=wssid;
						mGp.wifiIsActive=true;
						try {
							if (callBackStub!=null) callBackStub.cbWifiStatusChanged("Connected", mGp.wifiSsid);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				} else if (ss.equals("INACTIVE") ||
						ss.equals("DISCONNECTED") ||
						ss.equals("UNINITIALIZED") ||
						ss.equals("INTERFACE_DISABLED") ||
						ss.equals("SCANNING")) {
					if (mGp.wifiIsActive) {
						if (!mGp.wifiSsid.equals("")) {
							mUtil.addDebugLogMsg(1,"I","WIFI receiver, Disconnected WIFI Access point ssid="+mGp.wifiSsid);
							mGp.wifiSsid="";
							mGp.wifiIsActive=true;
							try {
								if (callBackStub!=null) callBackStub.cbWifiStatusChanged("Disconnected", "");
							} catch (RemoteException e) {
								e.printStackTrace();
							}
						}
					} else {
						if (new_wifi_enabled) {
							mUtil.addDebugLogMsg(1,"I","WIFI receiver, WIFI On");
							mGp.wifiSsid="";
							mGp.wifiIsActive=true;
							try {
								if (callBackStub!=null) callBackStub.cbWifiStatusChanged("On", "");
							} catch (RemoteException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
    };

	
}