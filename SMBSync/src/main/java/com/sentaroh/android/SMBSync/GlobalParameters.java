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

import static com.sentaroh.android.SMBSync.Constants.*;

import java.util.ArrayList;
import java.util.Collections;

import com.sentaroh.android.SMBSync.SMBSyncMain.ViewSaveArea;
import com.sentaroh.android.Utilities.LocalMountPoint;
import com.sentaroh.android.Utilities.ThemeColorList;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class GlobalParameters extends Application{
	public Context appContext=null;
	
	public int logLineCount=0;
//	public String currentLogFilePath="";
	
	public boolean activityIsForeground=true;
	
	public boolean enableMainUi=true;
	
	public String currentTab=SMBSYNC_TAB_NAME_PROF;
	
	public boolean mirrorThreadActive=false;
	
	public boolean supressAutoStart=false;
	
	public boolean themeIsLight=true;
	public int applicationTheme=-1;
	public ThemeColorList themeColorList=null;
	
	public Handler uiHandler=null;
	
	public TextView scheduleInfoView=null;
	public String scheduleInfoText="";
	
	public AdapterProfileList profileAdapter=null;
	public ListView profileListView=null;
	public int newProfileListViewPos=-1;

	public AdapterMessageList msgListAdapter=null;
	public ListView msgListView=null;
	
	public String externalRootDirectory="/mnt/sdcard";
	public boolean externalStorageIsMounted=false;
	public String internalRootDirectory="";
	
	public String profilePassword="";
	public final String profileKeyPrefix="*SMBSync*";
	
	public NotificationManager notificationManager=null;
	public int notificationIcon=R.drawable.ic_48_smbsync_wait;
	public Notification notification=null;
	public Builder notificationBuilder=null;
	public BigTextStyle notificationBigTextStyle=null;
	public Intent notificationIntent=null;
	public PendingIntent notificationPendingIntent=null;
	public String notificationLastShowedMessage=null, notificationLastShowedTitle="";
	public String notificationAppName="";
//	public boolean notiifcationEnabled=false;
	public long notificationNextShowedTime=0;
	
//	public ISvcCallback callBackStub=null;
	
	public ArrayList<MirrorIoParmList> mirrorIoParms=null;
	
	public ListView syncHistoryListView=null;
	public AdapterSyncHistory syncHistoryAdapter=null;
//	public ArrayList<SyncHistoryListItem> syncHistoryList=null;
//	public ArrayList<SyncHistoryListItem> addedSyncHistoryList=null;
	
	//Message view
	public boolean freezeMessageViewScroll=false;
	
	public boolean sampleProfileCreateRequired=false;
	
	//Parameters from Settings menu
	public int debugLevel=0;
	public boolean settingAutoStart=false,
			settingAutoTerm=false, settingErrorOption=false,
			settingDebugMsgDisplay=false,
			settingMediaFiles,
			settingScanExternalStorage,
			settingExitClean,
			settingBackgroundExecution=false;
	public String settingScreenOnOption=KEEP_SCREEN_ON_WHEN_SCREEN_UNLOCKED;
	public static final String KEEP_SCREEN_ON_DISABLED="0";
	public static final String KEEP_SCREEN_ON_ALWAYS="1";
	public static final String KEEP_SCREEN_ON_WHEN_SCREEN_UNLOCKED="2";
	public boolean settingWifiLockRequired=true;
	public String settingWifiOption=SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP;
	public boolean settingShowSyncButtonOnMenuItem=false;
	public boolean settingShowSyncDetailMessage=true;
	public boolean settingShowRemotePortOption=true;
	public String settingLogOption="0";
	public String settingLogMsgDir="/SMBSync/";
	
	public String settingBgTermNotifyMsg="0";
	public static final String BG_TERM_NOTIFY_MSG_ALWAYS = "0";
	public static final String BG_TERM_NOTIFY_MSG_ERROR = "1";
	public static final String BG_TERM_NOTIFY_MSG_NO = "2";

	public String settingLogMsgFilename="";
	public int settiingLogGeneration=1;
	public boolean settingExportedProfileEncryptRequired=true;
//	public boolean settingInternalProfileEncryptRequired=true;
	
	public static final String VIBRATE_WHEN_SYNC_ENDED_NONE="0";
	public static final String VIBRATE_WHEN_SYNC_ENDED_ALWAYS="1";
	public static final String VIBRATE_WHEN_SYNC_ENDED_SUCCESS="2";
	public static final String VIBRATE_WHEN_SYNC_ENDED_ERROR="3";
	public String settingVibrateWhenSyncEnded=VIBRATE_WHEN_SYNC_ENDED_ALWAYS;
	
	public static final String PB_RINGTONE_WHEN_SYNC_ENDED_NONE="0";
	public static final String PB_RINGTONE_WHEN_SYNC_ENDED_ALWAYS="1";
	public static final String PB_RINGTONE_WHEN_SYNC_ENDED_SUCCESS="2";
	public static final String PB_RINGTONE_WHEN_SYNC_ENDED_ERROR="3";
	public String settingRingtoneWhenSyncEnded=PB_RINGTONE_WHEN_SYNC_ENDED_ALWAYS;
	
	public boolean settingRemoteFileCopyByRename=false;
	public boolean settingLocalFileCopyByRename=true;
	
    public LinearLayout confirmView=null;
    public TextView confirmMsg=null;
    public Button confirmCancel=null;
    public OnClickListener confirmCancelListener=null;
    public Button confirmYes=null;
    public OnClickListener confirmYesListener=null;
    public Button confirmNo=null;
    public OnClickListener confirmNoListener=null;
    public Button confirmYesAll=null;
    public OnClickListener confirmYesAllListener=null;
    public Button confirmNoAll=null;
    public OnClickListener confirmNoAllListener=null;

    public LinearLayout progressBarView=null;
    public TextView progressBarMsg=null;
    public ProgressBar progressBarPb=null;
    public Button progressBarCancel=null;
    public OnClickListener progressBarCancelListener=null;
    public Button progressBarImmed=null;
    public OnClickListener progressBarImmedListener=null;

    public LinearLayout progressSpinView=null;
    public TextView progressSpinSyncprof=null;
    public TextView progressSpinFilePath=null;
    public TextView progressSpinStatus=null;
    public Button progressSpinCancel=null;
    public OnClickListener progressSpinCancelListener=null;

	public boolean wifiIsActive=false;
	public String wifiSsid="";
	
	public boolean onLowMemory=false;
	
	public WakeLock mDimScreenWakelock=null;
	public WifiManager.WifiLock mWifiLock=null;

	public boolean isApplicationIsRestartRequested=false;
	public int changedConfiguration=0;
	public ViewSaveArea mainViewSaveArea=null;
	public boolean confirmDialogShowed=false;
	public String confirmDialogFilePath="", confirmDialogMethod="";
	
	public GlobalParameters() {};
	
	@Override
	public void  onCreate() {
		super.onCreate();
		Log.v("SMBSyncGP","onCreate entered");
		onLowMemory=false;
		externalRootDirectory=LocalMountPoint.getExternalStorageDir();
		internalRootDirectory=getFilesDir().toString();

		loadSettingsParm(this);
	};
	
	@Override
	public void onLowMemory() {
//		Log.v("SMBSyncGP","onLowMemory entered");
		onLowMemory=true;
	};
	
//	private int mTextColorForeground=0;
//	private int mTextColorBackground=0;
//	public void initTextColor(Context c) {
//    	TypedValue outValue = new TypedValue();
//    	c.getTheme().resolveAttribute(android.R.attr.textColorPrimary, outValue, true);
//    	mTextColorForeground=c.getResources().getColor(outValue.resourceId);
//    	c.getTheme().resolveAttribute(android.R.attr.colorBackground, outValue, true);
//    	mTextColorBackground=c.getResources().getColor(outValue.resourceId);
//    	Log.v("","f="+String.format("0x%08x", mTextColorForeground));
//    	Log.v("","b="+String.format("0x%08x", mTextColorBackground));
//	};

	public void initSettingsParms(Context c) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		
		if (prefs.getString(c.getString(R.string.settings_log_dir), "-1").equals("-1")) {
			Editor pe=prefs.edit();
			
			sampleProfileCreateRequired=true;
			
			pe.putString(c.getString(R.string.settings_log_dir), externalRootDirectory+"/SMBSync/");
			pe.putString(c.getString(R.string.settings_network_wifi_option), SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP);
			pe.putString(c.getString(R.string.settings_file_diff_time_seconds), "3");
			pe.putString(c.getString(R.string.settings_media_store_last_mod_time), "0");

			pe.putBoolean(c.getString(R.string.settings_media_scanner_non_media_files_scan), true);
			pe.putBoolean(c.getString(R.string.settings_media_scanner_scan_extstg), true);

			pe.putBoolean(c.getString(R.string.settings_exit_clean), true);
			
			pe.putString(c.getString(R.string.settings_smb_lm_compatibility),"0");
			pe.putBoolean(c.getString(R.string.settings_smb_use_extended_security),false);
			pe.putString(c.getString(R.string.settings_smb_log_level),"0");
			pe.putString(c.getString(R.string.settings_smb_rcv_buf_size),"66576");
			pe.putString(c.getString(R.string.settings_smb_snd_buf_size),"66576");
			pe.putString(c.getString(R.string.settings_smb_listSize),"65535");
			pe.putString(c.getString(R.string.settings_smb_maxBuffers),"100");
			pe.putString(c.getString(R.string.settings_smb_tcp_nodelay),"true");
			pe.putString(c.getString(R.string.settings_io_buffers),"8");
			
			pe.putString(SMBSYNC_PROFILE_CONFIRM_COPY_DELETE, SMBSYNC_PROFILE_CONFIRM_COPY_DELETE_NOT_REQUIRED);
			
			pe.commit();
		}

		if (prefs.getString(c.getString(R.string.settings_keep_screen_on), "").equals("")) {
			prefs.edit().putString(c.getString(R.string.settings_keep_screen_on), GlobalParameters.KEEP_SCREEN_ON_WHEN_SCREEN_UNLOCKED)
				.commit();
		}
		
		if (prefs.getString(c.getString(R.string.settings_smb_perform_class), "-1").equals("-1")) {
			prefs.edit().putString(c.getString(R.string.settings_smb_perform_class), 
					"0").commit();
		}

		if (prefs.getString(c.getString(R.string.settings_network_wifi_option), "-1").equals("-1")) {
			if (prefs.getBoolean("settings_wifi_only", false)) {
				prefs.edit().putString(c.getString(R.string.settings_network_wifi_option), 
						SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP).commit();
			} else {
				prefs.edit().putString(c.getString(R.string.settings_network_wifi_option), 
						SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_OFF).commit();
			}
		}

	};
	
	public ArrayList<String> localMountPointList=new ArrayList<String>();
	public void buildLocalMountPointList(Context c) {
		ArrayList<String> smpl=getSystemLocalMountPointList(c);
		ArrayList<String> ampl=getUserLocalMountPointList(c);
		for(int i=0;i<ampl.size();i++) {
			if (ampl.get(i)!=null) {
				if (!smpl.contains(ampl.get(i))) smpl.add(ampl.get(i));
			}
		}
		Collections.sort(smpl);
		localMountPointList=smpl;
	};
	public ArrayList<String> getUserLocalMountPointList(Context c) {
		ArrayList<String> almpl=new ArrayList<String>();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		String lmpl=prefs.getString(SMBSYNC_USER_LOCAL_MOUNT_POINT_LIST_KEY, "");
		String[] a_lmp=null;
		if (lmpl!=null && !lmpl.equals("")) {
			a_lmp=lmpl.split("\t");
			for(int i=0;i<a_lmp.length;i++) {
				if (a_lmp[i]!=null) {
					almpl.add(a_lmp[i]);
				}
			}
		}
		return almpl;
	};

	@SuppressLint("SdCardPath")
	public ArrayList<String> getSystemLocalMountPointList(Context c) {
		ArrayList<String> tmpl=LocalMountPoint.getLocalMountPointList(c);
		ArrayList<String> mpl=new ArrayList<String>();
		if (tmpl.size()==0) tmpl.add("/sdcard");
		else {
			mpl=tmpl;
		}
    	Collections.sort(mpl);
    	
    	return mpl;
	}
	
	public void saveUserLocalMountPointList(Context c, ArrayList<String> almpl) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		String s_lmpl="";
		for(int i=0;i<almpl.size();i++) {
			s_lmpl+=almpl.get(i)+"\t";
		}
		prefs.edit().putString(SMBSYNC_USER_LOCAL_MOUNT_POINT_LIST_KEY, s_lmpl).commit();
	};

	public void clearUserLocalMountPointList(Context c) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		prefs.edit().remove(SMBSYNC_USER_LOCAL_MOUNT_POINT_LIST_KEY).commit();
	};

	public void loadSettingsParm(Context c) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

		debugLevel=
			Integer.parseInt(prefs.getString(c.getString(R.string.settings_log_level), "0")); 
//			Integer.parseInt(prefs.getString(getString(R.string.settings_log_level), "1"));//for debug

		settiingLogGeneration=Integer.valueOf(
				prefs.getString(c.getString(R.string.settings_log_generation), "10"));
		
		settingLogMsgDir=
				prefs.getString(c.getString(R.string.settings_log_dir),
						externalRootDirectory+"/SMBSync/");
		
		settingAutoStart=
				prefs.getBoolean(c.getString(R.string.settings_auto_start), false);
		settingDebugMsgDisplay=
				prefs.getBoolean(c.getString(R.string.settings_debug_msg_diplay), false);
		
		settingLogOption=
				prefs.getString(c.getString(R.string.settings_log_option), "0"); 
//				prefs.getString(getString(R.string.settings_log_option), "1"); //for debug

		settingAutoTerm=
				prefs.getBoolean(c.getString(R.string.settings_auto_term), false);
		settingWifiOption=
				prefs.getString(c.getString(R.string.settings_network_wifi_option), 
						SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP);
		settingErrorOption=
				prefs.getBoolean(c.getString(R.string.settings_error_option), false);
		settingBackgroundExecution=
				prefs.getBoolean(c.getString(R.string.settings_backgroound_execution), false);
		settingBgTermNotifyMsg=
				prefs.getString(c.getString(R.string.settings_background_termination_notification), "0");

		settingVibrateWhenSyncEnded=
				prefs.getString(c.getString(R.string.settings_vibrate_when_sync_ended), "0");
		settingRingtoneWhenSyncEnded=
				prefs.getString(c.getString(R.string.settings_playback_ringtone_when_sync_ended), "0");

		settingShowSyncButtonOnMenuItem=
				prefs.getBoolean(c.getString(R.string.settings_show_sync_on_action_bar), false);
		
		settingScreenOnOption=
				prefs.getString(c.getString(R.string.settings_keep_screen_on), GlobalParameters.KEEP_SCREEN_ON_WHEN_SCREEN_UNLOCKED);

		settingWifiLockRequired=
				prefs.getBoolean(c.getString(R.string.settings_wifi_lock), false);

		settingRemoteFileCopyByRename=
				prefs.getBoolean(c.getString(R.string.settings_remote_file_copy_by_rename), false);
		settingLocalFileCopyByRename=
				prefs.getBoolean(c.getString(R.string.settings_local_file_copy_by_rename), false);
		
		settingMediaFiles=
				prefs.getBoolean(c.getString(R.string.settings_media_scanner_non_media_files_scan), true);
		settingScanExternalStorage=
				prefs.getBoolean(c.getString(R.string.settings_media_scanner_scan_extstg), true);
		
		settingExitClean=
				prefs.getBoolean(c.getString(R.string.settings_exit_clean), true);
		settingExportedProfileEncryptRequired=
				prefs.getBoolean(c.getString(R.string.settings_exported_profile_encryption), true);
		
		if (!settingAutoStart) settingAutoTerm=false;
		
//		if (Build.VERSION.SDK_INT>=11) themeIsLight=prefs.getBoolean(c.getString(R.string.settings_use_light_theme), false);
//		else themeIsLight=false;
		themeIsLight=prefs.getBoolean(c.getString(R.string.settings_use_light_theme), false);
		if (themeIsLight) {
			applicationTheme=R.style.MainLight;
//			dialogViewBackGroundColor=Color.argb(255, 50, 50, 50);//.BLACK;
		} else {
			applicationTheme=R.style.Main;
//			dialogViewBackGroundColor=Color.argb(255, 50, 50, 50);//.BLACK;
		}
//		if (Build.VERSION.SDK_INT>=21) dialogViewBackGroundColor=0xff333333;
		
	};

}


