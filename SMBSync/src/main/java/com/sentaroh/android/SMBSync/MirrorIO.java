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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import com.sentaroh.android.Utilities.StringUtil;
import com.sentaroh.android.Utilities.MiscUtil;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.ThreadCtrl;

public class MirrorIO implements Runnable {
	
	private int mirrorIoBufferSize =65536*2; //64K x 2
	private byte[] mirrorIoBuffer;
	
	private ISvcCallback callBackStub=null;

	private boolean syncLocalToLocal=false;

	private String syncMasterProfType="";  	// R:remote to local, L:local to remote
	private String syncType="";	    		// M:mirror, C:copy, X:move, S:SYNC
	private String syncProfName="";
	private Pattern fileFilterInclude,fileFilterExclude;
	private Pattern dirFilterInclude,dirFilterExclude;
	private ArrayList<Pattern[]> dirIncludeFilterList=new ArrayList<Pattern[]>();
	private ArrayList<Pattern> dirExcludeFilterList=new ArrayList<Pattern>();
	
	private String mirrorIoRootDir="";
	
	private String syncRemoteAddr;
	private String syncRemotePort="";
	private String syncRemoteShare;
	private String syncRemoteDir;
	private String syncLocalDir;
	private String syncMasterLocalDir;
	private String syncTargetLocalDir;
	private String syncRemoteUserid;
	private String syncRemotePassword;
	private String remoteUrl; 
	private String localUrl;
	private String remoteMasterDir;
	private NtlmPasswordAuthentication ntlmPasswordAuth =null;;
	
//	private String SMBSync_External_Root_Dir;
	
	private ArrayList<MirrorIoParmList> syncList;

//	private final int SMBSYNC_ERROR_ASYNC_KILL = -10;

	private long totalTransferByte = 0, totalTransferTime=0;
	private int copyCount, deleteCount, ignoreCount, retryCount=0;
	private int totalCopyCnt=0, totalDeleteCnt=0, totalIgnoreCnt=0, 
				totalWarningMsgCnt=0, totalErrorMsgCnt=0, totalRetryCnt=0;
	
	private ThreadCtrl tcMirror=null;
	private ThreadCtrl tcConfirm=null;
	
	private boolean isExceptionOccured=false;
	
	private NotifyEvent notifyEvent;
	
	private String settingsIoBuffers=""; 
	
	private String settingsMediaStoreUseLastModTime="0";
	private boolean settingsMediaFiles,
		defaultSettingScanExternalStorage;

	private boolean syncMasterDirFileProcess=true, 
			syncProfileConfirmRequired=false, syncProfileUseJavaLastModified=true;
	private boolean syncProfileNotUseLastModifiedForRemote=false;
	
	private boolean syncProfileSyncEmptyDirectory=false,
			syncProfileSyncHiddenDirectory=false,
			syncProfileSyncHiddenFile=false,
			syncProfileSyncSubDir=true,
			syncProfileSyncUseRemoteSmallIoArea=false
			;
	
	private boolean isExceptionRetryRequired=false;
	private int syncProfileRetryCount=0;
	private int exceptionRetryCount=0;
	private int syncProfileRetryIntervalTime=30; 
	
	private ArrayList<String> mediaStoreImageList = new ArrayList<String>();
	private ArrayList<String> mediaStoreAudioList = new ArrayList<String>();
	private ArrayList<String> mediaStoreVideoList = new ArrayList<String>();
//	private ArrayList<String> mediaStoreFilesList = new ArrayList<String>();
	
	private boolean isMediaStoreChangeWarningIssued=false;
	private int timeDifferenceLimit=3;
	
	private int timeZone;

//	private ArrayList<String> mHistoryCopiedList,mHistoryDeletedList,mHistoryIgnoredList;
	
	private boolean isSyncParmError;
	
	private MediaScannerConnection mediaScanner ;
	
	private ArrayList<FileLastModifiedEntryItem> currentFileLastModifiedList=null;
	private ArrayList<FileLastModifiedEntryItem> newFileLastModifiedList=null;
	private String loadedLocalMountPoint="";
	private ArrayList<LocalFileLastModifiedListCacheItem>mLocalFileLastModifiedCache=
			new ArrayList<LocalFileLastModifiedListCacheItem>();
	
//	private ArrayList<SyncHistoryListItem> syncHistoryList=null;
	
	private GlobalParameters mGp=null;
	
	private SMBSyncUtil mUtil=null;

	private PrintWriter mSyncHistoryPrintWriter=null;
	private String mSyncHistroryResultFilepath=null;
	
//	private ArrayList<SyncHistoryListItem> mAddedSyncHistoryList=null;
	
	private SafCommonArea mSafCA=new SafCommonArea();
	
	public MirrorIO(GlobalParameters gwa, NotifyEvent ne, ThreadCtrl ac, ThreadCtrl tw,
			ISvcCallback cb) {
		mGp=gwa;
		notifyEvent=ne;
		tcConfirm=tw;
		syncList = mGp.mirrorIoParms;
		loadMsgString(mGp);
		tcMirror=ac; //new ThreadCtrl();
		
		callBackStub=cb;
		
		mUtil=new SMBSyncUtil(mGp.appContext, settingsMediaStoreUseLastModTime, gwa);
		mUtil.setLogIdentifier("MirrorIO");
		
		SafUtil.initWorkArea(mGp.appContext, mSafCA, mGp.debugLevel>0);
		
		if (mGp.debugLevel>=1 && mSafCA.rootDocumentFile!=null) {
			String uri_string=SafUtil.getSafExternalSdcardRootTreeUri(mSafCA);
			addDebugLogMsg(1,"I","","SafUri="+uri_string+", SafRoot="+mSafCA.rootDocumentFile.getName());
			for(String esd:mSafCA.external_sdcard_dir_list) {
				addDebugLogMsg(1,"I","","esd="+esd);
			}
		}

		
//		SMBSync_External_Root_Dir = LocalMountPoint.getExternalStorageDir();
		
		initIoBuffer();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mGp.appContext);
		settingsMediaStoreUseLastModTime=
			prefs.getString(mGp.appContext.getString(R.string.settings_media_store_last_mod_time),"0");
		String td=
			prefs.getString(mGp.appContext.getString(R.string.settings_file_diff_time_seconds), "3");
		timeDifferenceLimit=Integer.parseInt(td)*1000;

		buildMediaStoreDirList();
		
		TimeZone tz=TimeZone.getDefault();
		timeZone = tz.getRawOffset();
		
		settingsMediaFiles =
				prefs.getBoolean(mGp.appContext.getString(R.string.settings_media_scanner_non_media_files_scan),false);
		defaultSettingScanExternalStorage=
				prefs.getBoolean(mGp.appContext.getString(R.string.settings_media_scanner_scan_extstg),false);

		if (mGp.debugLevel>=1) {
			addDebugLogMsg(1,"I",
					"defautSettingsMediaStore=",settingsMediaStoreUseLastModTime);
			addDebugLogMsg(1,"I",
					"settings_file_diff_time_seconds="+timeDifferenceLimit);
			addDebugLogMsg(1,"I","time zone="+timeZone);
			
			addDebugLogMsg(1,"I",
					"defaultSettingMediaFiles="+settingsMediaFiles+
					", defaultSettingScanExternalStorage="+defaultSettingScanExternalStorage);
		}

		mediaScanner = new MediaScannerConnection(mGp.appContext,
				new MediaScannerConnectionClient() {
			@Override
			public void onMediaScannerConnected() {
				if (mGp.debugLevel>=1) 
					addDebugLogMsg(1,"I","MediaScanner connected.");
			};
			@Override
			public void onScanCompleted(final String fp, final Uri uri) {
				if (mGp.debugLevel>=2) 
					addDebugLogMsg(2,"I","MediaScanner scan completed. fn=",
							fp,", Uri="+uri);
//				checkMediaScannerReult(fp,uri);
			};
		});
		mediaScanner.connect();
		currentFileLastModifiedList=new ArrayList<FileLastModifiedEntryItem>();
		newFileLastModifiedList=new ArrayList<FileLastModifiedEntryItem>();
		
//		syncHistoryList=mUtil.loadHistoryList();
//		mAddedSyncHistoryList=new ArrayList<SyncHistoryListItem>();
		
		mUtil.initAppSpecificExternalDirectory(mGp.appContext);
	};

	@SuppressWarnings("unused")
	final private boolean checkMediaScannerReult(String fp, Uri uri) {
		File lf=new File(fp);
		long lf_lm=lf.lastModified()/1000;
		boolean result=false;
        ContentResolver resolver = mGp.appContext.getContentResolver();
        Cursor ci = resolver.query(uri,
        		new String[] {MediaStore.MediaColumns.DATA,
        						MediaStore.MediaColumns.DATE_MODIFIED},
        		MediaStore.MediaColumns.DATA+"=?",new String[]{fp},
        		MediaStore.MediaColumns.DATA);
        boolean checked=false;
        while( ci.moveToNext() ){
        	checked=true;
        	long lm=ci.getLong(ci.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED));
        	long ms_lm=0;
        	if (Build.VERSION.SDK_INT<=10) {//Android 3.0以下のバグ回避
	        	if (lm>=1000000000000L) ms_lm=lm/1000;
	        	else ms_lm=lm;
        	} else ms_lm=lm;
//        	Log.v("","lm="+lm+", lmx="+ms_lm);
        	if (ms_lm!=lf_lm) {
        		addLogMsg("W",fp,String
        			.format(msgs_mirror_prof_ms_different_file_last_mod,
        			StringUtil.convDateTimeTo_YearMonthDayHourMinSec(lf_lm*1000),
        			StringUtil.convDateTimeTo_YearMonthDayHourMinSec(ms_lm*1000)));
        	} else result=true;
        }
        if (!checked) {
        	addLogMsg("W",fp,msgs_mirror_prof_ms_read_error);
//        	Log.v("","count="+ci.getCount()+", isFirst="+ci.isFirst());
//        	Log.v("","uri="+uri); 
        }
        ci.close();
        return result;
	};

	final private boolean waitMediaScanner(boolean ds) {
    	boolean result=false;
    	try {
    		int limit=0;
    		while(true) {
    			if (mediaScanner.isConnected()==ds) {
    				result=true;
    				break;
    			}
    			synchronized(mediaScanner) {
    				mediaScanner.wait(50);
    			}
    			limit++;
    			if (limit>200) {
    				result=false;
    				if (mGp.debugLevel>=1) 
    					addDebugLogMsg(1,"E","MediaScannerConnection wait timeout occured.");
    				break;
    			}
    		}
		} catch (InterruptedException e) {
			addLogMsg("E","","MediaScannerConnection wait error:"+e.getMessage());
			addLogMsg("W","","InterruptedException occured");
			printStackTraceElement(e.getStackTrace());
			result=false;
		}
		return result;
    };

 // Default uncaught exception handler variable
    private UncaughtExceptionHandler defaultUEH;
    
 // handler listener
    private Thread.UncaughtExceptionHandler unCaughtExceptionHandler =
        new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
            	Thread.currentThread().setUncaughtExceptionHandler(defaultUEH);
            	ex.printStackTrace();
            	StackTraceElement[] st=ex.getStackTrace();
            	String st_msg="";
            	for (int i=0;i<st.length;i++) {
            		st_msg+="\n at "+st[i].getClassName()+"."+
            				st[i].getMethodName()+"("+st[i].getFileName()+
            				":"+st[i].getLineNumber()+")";
            	}
    			tcMirror.setThreadResultError();
    			String end_msg=msgs_mirror_task_result_error_ended+"\n"+
    					ex.toString()+st_msg;
        		tcMirror.setThreadMessage(end_msg);
        		addLogMsg("E","",end_msg);
        		NotificationUtil.showOngoingMsg(mGp,System.currentTimeMillis(),end_msg);
        		notifyThreadTerminate();
        		tcMirror.setDisabled();
                // re-throw critical exception further to the os (important)
//                defaultUEH.uncaughtException(thread, ex);
            }
    };
    
	@Override
	final public void run() {
		addLogMsg("I","",msgs_mirror_task_started);

		defaultUEH = Thread.currentThread().getUncaughtExceptionHandler();
        Thread.currentThread().setUncaughtExceptionHandler(unCaughtExceptionHandler);
        
		NotificationUtil.showOngoingMsg(mGp,System.currentTimeMillis(),msgs_mirror_task_started);
		waitMediaScanner(true);//wait for media scanner service connection
		totalCopyCnt=totalDeleteCnt=totalIgnoreCnt=totalWarningMsgCnt=totalRetryCnt=totalErrorMsgCnt=0;
		boolean error_occured_but_ignored=false;
		for (int i = 0; i < syncList.size(); i++) {
			totalTransferByte = totalTransferTime= 0;
			isExceptionOccured=isSyncParmError=false;
			isExceptionRetryRequired=false;
			copyCount=deleteCount=ignoreCount=retryCount=0;
//			mHistoryCopiedList=new ArrayList<String>();
//			mHistoryDeletedList=new ArrayList<String>();
//			mHistoryIgnoredList=new ArrayList<String>();
			isMediaStoreChangeWarningIssued=false;
			if (tcMirror.isEnabled()) { // async process was enabled
				syncProfName = syncList.get(i).getProfname();
				openSyncResultLog();
				addMsgToProgDlg(false,"I", System.currentTimeMillis(), "",msgs_mirror_prof_started);
				addLogMsg("I","",msgs_mirror_prof_started);
				initSyncParm(syncList.get(i));
				if (!isSyncParmError) {
					loadLocalFileLastModifiedList(syncList.get(i).getLocalMountPoint());
					setJcifsAuthParm();
					//sync 開始
					setSyncExecuting();
					if (isMountPointAvailable(localUrl)) {
						if (syncType.equals(SMBSYNC_SYNC_TYPE_MIRROR)) { // mirror
							doSyncMirror(syncList.get(i));
						} else if (syncType.equals(SMBSYNC_SYNC_TYPE_COPY)) { // copy
							doSyncCopy(syncList.get(i));
						} else if (syncType.equals(SMBSYNC_SYNC_TYPE_MOVE)) { // move
							doSyncMove(syncList.get(i));
						} else if (syncType.equals(SMBSYNC_SYNC_TYPE_SYNC)) { // sync
							// sync process
						} else {
							// invalid mirror type was specified
							addLogMsg("E","",
									" "+syncType+" "+msgs_mirror_prof_invalid_mirror_type);
							tcMirror.setThreadMessage(syncType+" "+msgs_mirror_prof_invalid_mirror_type);
							isSyncParmError=true;
						}
					} else {
						// invalid mirror type was specified
						addLogMsg("E",localUrl,msgs_mirror_prof_sync_local_mount_point_unavailable);
						tcMirror.setThreadMessage(msgs_mirror_prof_sync_local_mount_point_unavailable+","+localUrl);
						isSyncParmError=true;
					}
					saveLocalFileLastModifiedList(syncList.get(i).getLocalMountPoint());
				}
				//sync 終了
				//
				totalCopyCnt+=copyCount;
				totalDeleteCnt+=deleteCount;
				totalIgnoreCnt+=ignoreCount;
				totalRetryCnt+=retryCount;
				if (tcMirror.isEnabled()) {
					if (!isSyncParmError && !isExceptionOccured) {
						addLogMsg("I","",String.format(msgs_mirror_prof_no_of_copy,
										copyCount , deleteCount, ignoreCount));
						if (copyCount>0) {
							if (mGp.debugLevel>=1) 
								addDebugLogMsg(1,"I","TotalByte="+totalTransferByte+
										",Time="+totalTransferTime);
							addLogMsg("I","",
									msgs_mirror_prof_avg_rate+" " + 
									calTransferRate(totalTransferByte,totalTransferTime));
						}
						addLogMsg("I","",msgs_mirror_prof_success_end);
						addHistoryList(SyncHistoryListItem.SYNC_STATUS_SUCCESS,
								copyCount,deleteCount,ignoreCount, retryCount, "");
						mUtil.saveHistoryList(mGp.syncHistoryAdapter);
						closeSyncResultLog();
						ProfileUtility.saveProfileToFile(mGp, mGp.appContext, mUtil, false, "", "", mGp.profileAdapter, false);
					} else { 
						addLogMsg("E","",msgs_mirror_prof_was_failed);
						addHistoryList(SyncHistoryListItem.SYNC_STATUS_ERROR,
								copyCount,deleteCount,ignoreCount,  retryCount,
								tcMirror.getThreadMessage());
						mUtil.saveHistoryList(mGp.syncHistoryAdapter);
						ProfileUtility.saveProfileToFile(mGp, mGp.appContext, mUtil, false, "", "", mGp.profileAdapter, false);
						closeSyncResultLog();
						tcMirror.setExtraDataInt(1);//Indicate error occured
						if (!mGp.settingErrorOption) {
							break;
						} else {
							error_occured_but_ignored=true;
						}
					}
				} else {
					addLogMsg("W","",msgs_mirror_prof_was_cancelled);
					addHistoryList(SyncHistoryListItem.SYNC_STATUS_CANCEL,
							copyCount,deleteCount, ignoreCount, retryCount, "");
					mUtil.saveHistoryList(mGp.syncHistoryAdapter);
					ProfileUtility.saveProfileToFile(mGp, mGp.appContext, mUtil, false, "", "", mGp.profileAdapter, false);
					closeSyncResultLog();
					isSyncParmError=true;
					break;
				}
			}
		}
		syncProfName="";
		mediaScanner.disconnect();
		waitMediaScanner(false);//wait for media scanner disconnection

		addLogMsg("I","",msgs_mirror_task_ended);
		NotificationUtil.showOngoingMsg(mGp,System.currentTimeMillis(),msgs_mirror_task_ended);

		addLogMsg("I","",String.format(msgs_mirror_task_result_stats, 
				totalCopyCnt, totalDeleteCnt, totalIgnoreCnt,totalWarningMsgCnt,totalRetryCnt,totalErrorMsgCnt));

		String end_msg="";
		if (!tcMirror.isEnabled()) {
			tcMirror.setThreadResultCancelled();
			end_msg=msgs_mirror_task_result_cancel;
		} else if (isSyncParmError || isExceptionOccured || error_occured_but_ignored) {
			tcMirror.setThreadResultError();
			if (error_occured_but_ignored) {
				tcMirror.setThreadMessage(msgs_mirror_task_result_error_ignored);
				end_msg=msgs_mirror_task_result_error_ignored;
			} else {
				end_msg=msgs_mirror_task_result_error_ended;
			}
		} else {
			tcMirror.setThreadResultSuccess();
			end_msg=msgs_mirror_task_result_ok;
		}
		tcMirror.setThreadMessage(end_msg);
		addLogMsg("I","",end_msg);
		NotificationUtil.showOngoingMsg(mGp,System.currentTimeMillis(),end_msg);
		notifyThreadTerminate();
		tcMirror.setDisabled();
	};

	private boolean isMountPointAvailable(String fp) {
		boolean result=false;
		result=LocalMountPoint.isMountPointAvailable(mGp.appContext, fp);
		return result;
	};
	
	final private void openSyncResultLog() {
		mSyncHistroryResultFilepath=mUtil.createSyncResultFilePath(syncProfName);
		if (mSyncHistoryPrintWriter!=null) closeSyncResultLog();
		try {
			FileWriter fos=new FileWriter(mSyncHistroryResultFilepath);
			BufferedWriter bow=new BufferedWriter(fos,1024*256);
			mSyncHistoryPrintWriter=new PrintWriter(bow);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	};
	
	private void closeSyncResultLog() {
		if (mSyncHistoryPrintWriter!=null) {
			final PrintWriter pw=mSyncHistoryPrintWriter;
			Thread th=new Thread() {
				@Override
				public void run() {
					pw.flush();
					pw.close();
//					Log.v("","closed");
				}
			};
			th.start();
			mSyncHistoryPrintWriter=null;
//			Log.v("","close exit");
		}
	};
	
	final private void addHistoryList(int status, int copy_cnt, int del_cnt, int ignore_cnt,
			int retry_cnt, String error_msg) {
		String date_time=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
		String date=date_time.substring(0,10);
		String time=date_time.substring(11);
		final SyncHistoryListItem hli=new SyncHistoryListItem();
		hli.sync_date=date;
		hli.sync_time=time;
		hli.sync_prof=syncProfName;
		hli.sync_status=status;
		hli.sync_result_no_of_copied=copy_cnt;
		hli.sync_result_no_of_deleted=del_cnt;
		hli.sync_result_no_of_ignored=ignore_cnt;
		hli.sync_result_no_of_retry=retry_cnt;
		hli.sync_error_text=error_msg;
//		if (!mGp.currentLogFilePath.equals("")) hli.isLogFileAvailable=true;
//		hli.sync_log_file_path=mGp.currentLogFilePath;
		hli.sync_result_file_path=mSyncHistroryResultFilepath;
		
		ProfileListItem pfli=ProfileUtility.getProfile(syncProfName, mGp.profileAdapter);
		if (pfli!=null) {
			pfli.setLastSyncTime(date+" "+time);
			pfli.setLastSyncResult(status);
			pfli.setSyncRunning(false);
		}
		mGp.uiHandler.post(new Runnable(){
			@Override
			public void run() {
				mGp.profileAdapter.notifyDataSetChanged();
				mUtil.addHistoryList(mGp.syncHistoryAdapter, hli);
				mGp.syncHistoryAdapter.notifyDataSetChanged();
			}
		});
//		mAddedSyncHistoryList.add(hli);
	};
	
	private void setSyncExecuting() {
		String date_time=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
		String date=date_time.substring(0,10);
		String time=date_time.substring(11);
		
		ProfileListItem pfli=ProfileUtility.getProfile(syncProfName, mGp.profileAdapter);
		if (pfli!=null) {
			pfli.setLastSyncTime(date+" "+time);
			pfli.setSyncRunning(true);
		}
		mGp.uiHandler.post(new Runnable(){
			@Override
			public void run() {
				mGp.profileAdapter.notifyDataSetChanged();
				for(int i=0;i<mGp.profileAdapter.getCount();i++) {
					if (mGp.profileAdapter.getItem(i).getProfileName().equals(syncProfName)) {
						final int pos=i;
						if (mGp.currentTab.equals(SMBSYNC_TAB_NAME_PROF)) {
							mGp.newProfileListViewPos=-1;
							mGp.profileListView.post(new Runnable(){
								@Override
								public void run() {
									mGp.profileListView.setSelection(pos);
								}
							});
						} else {
							mGp.newProfileListViewPos=i;
						}
						break;
					}
				}
			}
		});
	};

	final private void initSyncParm(MirrorIoParmList mipl) {
		syncMasterProfType = mipl.getMasterType();
		String syncTargetProfType = mipl.getTargetType();
		syncType =mipl.getMirrorType();
//		syncRemoteAddr = mipl.getRemoteAddr();
		syncRemoteShare = mipl.getRemoteShare();
		syncRemoteDir = mipl.getRemoteDir();
		syncLocalDir = mipl.getLocalDir();
		syncRemoteUserid = mipl.getRemoteUserid();
		syncRemotePassword = mipl.getRemotePass();
		syncMasterDirFileProcess=mipl.isMasterDirFileProcessed();
		syncProfileConfirmRequired=mipl.isConfirmRequired();
//		Log.v("","addr="+mipl.getRemoteAddr()+", host="+mipl.getHostName());
				
//		if (mipl.getHostName().equals("")) syncRemoteAddr = mipl.getRemoteAddr();
//		else syncRemoteAddr = resolveHostName(mipl.getHostName());
		if (mipl.getHostName().equals("")) syncRemoteAddr = mipl.getRemoteAddr();
		else syncRemoteAddr = mipl.getHostName();

		syncProfileNotUseLastModifiedForRemote=mipl.isNotUseLastModifiedForRemote();
		
		syncProfileSyncEmptyDirectory=mipl.isSyncEmptyDirectory();
		syncProfileSyncHiddenDirectory=mipl.isSyncHiddenDirectory();
		syncProfileSyncHiddenFile=mipl.isSyncHiddenFile();
		syncProfileSyncSubDir=mipl.isSyncSubDirectory();
		syncProfileSyncUseRemoteSmallIoArea=mipl.isSyncUseRemoteSmallIoArea();

		syncRemotePort="";
		String sep="";
		if (!mipl.getRemotePort().equals("")) {
			sep=":";
			syncRemotePort=mipl.getRemotePort();
		}
		
		if (syncRemoteDir.equals("")) {
			remoteUrl= "smb://"+syncRemoteAddr+sep+syncRemotePort+"/"+syncRemoteShare+syncRemoteDir;
		}else {
			remoteUrl= "smb://"+syncRemoteAddr+sep+syncRemotePort+"/"+syncRemoteShare+"/"+syncRemoteDir;
		}
		remoteMasterDir=remoteUrl;
//		if (syncLocalDir.equals("")) {
//			localUrl=SMBSync_External_Root_Dir+syncLocalDir;
//		} else {
//			localUrl=SMBSync_External_Root_Dir+"/"+syncLocalDir;
//		}
		localUrl=syncLocalDir;
		
//		compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
		
		if (syncMasterProfType.equals("L") && syncTargetProfType.equals("L")) {
			syncLocalToLocal=true;
			localUrl=syncLocalDir=mipl.getTargetLocalMountPoint();
			
			if (mipl.getMasterLocalDir().equals("")) syncMasterLocalDir=mipl.getMasterLocalMountPoint();
			else syncMasterLocalDir=mipl.getMasterLocalMountPoint()+"/"+mipl.getMasterLocalDir();
			
			if (mipl.getTargetLocalDir().equals("")) syncTargetLocalDir=mipl.getTargetLocalMountPoint();
			else syncTargetLocalDir=mipl.getTargetLocalMountPoint()+"/"+mipl.getTargetLocalDir();
			
			syncProfileRetryCount=0;
		} else {
			syncLocalToLocal=false;
			if (mipl.getReryCount()>0) {
				syncProfileRetryCount=3;
			} else {
				syncProfileRetryCount=0;
			}
		}

// Check local directory access		
		if (syncMasterProfType.equals("L") && syncTargetProfType.equals("L")) {
			String mlmp=mipl.getMasterLocalMountPoint();
			if (!mipl.getMasterLocalDir().equals("")) mlmp+="/"+mipl.getMasterLocalDir();
			File m_lf=new File(mlmp);
			boolean ex=m_lf.exists();
			boolean cr=m_lf.canRead();
			if (!ex || (ex && !cr)) {
				addLogMsg("E",mlmp,msgs_mirror_master_local_mount_point_not_readable);
				tcMirror.setThreadMessage(msgs_mirror_master_local_mount_point_not_readable+" "+mlmp);
				isSyncParmError=true;
			}

			String tlmp=mipl.getTargetLocalMountPoint();
			if (!mipl.getTargetLocalDir().equals("")) {
				tlmp+="/"+mipl.getTargetLocalDir();
			}
			File t_lf=new File(tlmp);
			ex=t_lf.exists();
			if (!ex) {
				boolean c_dir=t_lf.mkdirs();
				addDebugLogMsg(1,"W","Directory was created "+tlmp+", result="+c_dir);
			}
			try {
				if (SafUtil.isSafExternalSdcardPath(mSafCA, tlmp+"/SMBSyncWk.tmp")) { 
//					boolean cw=t_lf.canWrite();
//					if ((ex && !cw)) {
//						addLogMsg("E",tlmp,msgs_mirror_target_local_mount_point_not_writable);
//						tcMirror.setThreadMessage(msgs_mirror_target_local_mount_point_not_writable+" "+tlmp);
//						isSyncParmError=true;
//					} 

					SafFile df=SafUtil.getSafDocumentFileByPath(mSafCA, tlmp+"/SMBSyncWk.tmp", false);
					File m_out=new File(mlmp+"/SMBSyncWk.tmp");
					if (m_out.exists() && m_out.lastModified()==df.lastModified()) {
						//Same physical dir
						if (mipl.getMasterLocalDir().equals(mipl.getTargetLocalDir())) {
							String msg=String.format(msgs_mirror_physcal_access_to_same_dir, mipl.getMasterLocalMountPoint(),
									mipl.getTargetLocalMountPoint());
							addLogMsg("E",mipl.getLocalMountPoint(),msg);
/*debug*/					addLogMsg("E",mipl.getLocalMountPoint(),"tlmp="+tlmp+", mlmp="+mlmp+", ex="+ex);
							printMpList();
							tcMirror.setThreadMessage(msg);
							isSyncParmError=true;
						}
					}
				} else {
					boolean cw=t_lf.canWrite();
					if ((ex && !cw)) {
						addLogMsg("E",tlmp,msgs_mirror_target_local_mount_point_not_writable);
						tcMirror.setThreadMessage(msgs_mirror_target_local_mount_point_not_writable+" "+tlmp);
						isSyncParmError=true;
					} 

					File t_out=new File(tlmp+"/SMBSyncWk.tmp");
					if (t_out.exists()) t_out.delete();
					if (t_out.createNewFile()) {
						File m_out=new File(mlmp+"/SMBSyncWk.tmp");
						if (m_out.lastModified()==t_out.lastModified()) {
							//Same physical dir
							if (mipl.getMasterLocalDir().equals(mipl.getTargetLocalDir())) {
								String msg=String.format(msgs_mirror_physcal_access_to_same_dir, mipl.getMasterLocalMountPoint(),
										mipl.getTargetLocalMountPoint());
								addLogMsg("E",mipl.getLocalMountPoint(),msg);
	/*debug*/					addLogMsg("E",mipl.getLocalMountPoint(),"tlmp="+tlmp+", mlmp="+mlmp+", ex="+ex+", cw="+cw);
								printMpList();
								tcMirror.setThreadMessage(msg);
								isSyncParmError=true;
							}
						}
						t_out.delete();
					} else {
						//Create error
						String msg=String.format(msgs_mirror_physcal_access_check_create_error,tlmp+"/SMBSyncWk.tmp");
						addLogMsg("E",mipl.getLocalMountPoint(),msg);
	/*debug*/			addLogMsg("E",mipl.getLocalMountPoint(),"tlmp="+tlmp+", mlmp="+mlmp+", ex="+ex+", cw="+cw);
						printMpList();
						tcMirror.setThreadMessage(msg);
						isSyncParmError=true;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				String msg=String.format(msgs_mirror_physcal_access_check_create_error,tlmp+"/SMBSyncWk.tmp");
				addLogMsg("E",mipl.getLocalMountPoint(),msg+"\n"+e.getMessage());
/*debug*/		addLogMsg("E",mipl.getLocalMountPoint(),"tlmp="+tlmp+", mlmp="+mlmp+", ex="+ex);
				printMpList();
				printStackTraceElement(e.getStackTrace());
				tcMirror.setThreadMessage(msg+"\n"+e.getMessage());
				isSyncParmError=true;
			}
		} else {
			if (syncMasterProfType.equals("L")) {
//				Log.v("","d1="+mipl.getLocalDir());
				String mlmp=mipl.getLocalMountPoint();
				if (!mipl.getLocalDir().equals("")) mlmp=mipl.getLocalDir();
				File lf=new File(mlmp);
				boolean ex=lf.exists();
				boolean cr=lf.canRead();
				if (!ex || (ex && !cr)) {
					addLogMsg("E",mipl.getLocalMountPoint(),msgs_mirror_master_local_mount_point_not_readable);
					tcMirror.setThreadMessage(msgs_mirror_master_local_mount_point_not_readable+" "+mlmp);
					isSyncParmError=true;
				}
			} else {
//				Log.v("","d2="+mipl.getLocalDir());
				String tlmp=mipl.getLocalMountPoint();
				if (!mipl.getLocalDir().equals("")) tlmp=mipl.getLocalDir();
				File lf=new File(tlmp);
				boolean ex=lf.exists();
				
				if (!ex) {
					boolean c_dir=lf.mkdirs();
					addDebugLogMsg(1,"W","Directory was created "+tlmp+", result="+c_dir);
				}
				
				if (SafUtil.isSafExternalSdcardPath(mSafCA, tlmp+"/SMBSyncWk.tmp")) { 
//					SafFile df=SafUtil.getSafDocumentFileByPath(mGp.appContext, mSafCA, tlmp+"/SMBSyncWk.tmp", false);
//					File m_out=new File(tlmp+"/SMBSyncWk.tmp");
//					if (m_out.exists() && m_out.lastModified()==df.lastModified()) {
//						//Same physical dir
//						if (mipl.getMasterLocalDir().equals(mipl.getTargetLocalDir())) {
//							String msg=String.format(msgs_mirror_physcal_access_to_same_dir, mipl.getMasterLocalMountPoint(),
//									mipl.getTargetLocalMountPoint());
//							addLogMsg("E",mipl.getLocalMountPoint(),msg);
///*debug*/					addLogMsg("E",mipl.getLocalMountPoint(),"tlmp="+tlmp+", mlmp="+tlmp+", ex="+ex);
//							printMpList();
//							tcMirror.setThreadMessage(msg);
//							isSyncParmError=true;
//						}
//					}
				} else {
					boolean cw=lf.canWrite();
					if ((ex && !cw)) {
						addLogMsg("E",mipl.getLocalMountPoint(),msgs_mirror_target_local_mount_point_not_writable);
						tcMirror.setThreadMessage(msgs_mirror_target_local_mount_point_not_writable+" "+tlmp);
						isSyncParmError=true;
					}
				}
			}
		}

// Check remote connection		
		mRemoteHostName=mRemoteHostAddress="";
		mRemoteHostPort=-1;
		mRemoteHostParmsAvailable=false;
		if (syncMasterProfType.equals("R") || syncTargetProfType.equals("R")) {
			if (mipl.getHostName().equals("")) {
				if (mipl.getRemotePort().length()>0) {//Check for report port specified
					mRemoteHostParmsAvailable=true;
					mRemoteHostAddress=mipl.getRemoteAddr();
					mRemoteHostPort=Integer.parseInt(mipl.getRemotePort());
					if (!SMBSyncUtil.isSmbHostAddressConnected(mipl.getRemoteAddr(),
							Integer.parseInt(mipl.getRemotePort()))) {
						String msg=String.format(mGp.appContext.getString(R.string.msgs_mirror_remote_addr_not_connected_with_port),
								mipl.getRemoteAddr(),mipl.getRemotePort());
						addLogMsg("E","",msg);
						tcMirror.setThreadMessage(msg);
						isSyncParmError=true;
					}
				} else {//Check for default report port
					mRemoteHostParmsAvailable=true;
					mRemoteHostAddress=mipl.getRemoteAddr();
					if (!SMBSyncUtil.isSmbHostAddressConnected(mipl.getRemoteAddr())) {
						String msg=String.format(mGp.appContext.getString(R.string.msgs_mirror_remote_addr_not_connected),
								mipl.getRemoteAddr());
						addLogMsg("E","",msg);
						tcMirror.setThreadMessage(msg);
						isSyncParmError=true;
					}
				}
			} else {
				mRemoteHostName=mipl.getHostName();
				mRemoteHostParmsAvailable=true;
				if (resolveHostName(mipl.getHostName())==null) {
					String msg=mGp.appContext.getString(R.string.msgs_mirror_remote_name_not_found)+
							mipl.getHostName();
					addLogMsg("E","",msg);
					tcMirror.setThreadMessage(msg);
					isSyncParmError=true;
				}
			}
		}

		if (mipl.isForceLastModifiedUseSmbsync()) syncProfileUseJavaLastModified=false;
		else {
			if (syncMasterProfType.equals("L") && syncTargetProfType.equals("L")) {
				if (mipl.getLocalDir().equals("")) syncProfileUseJavaLastModified=isSetLastModifiedFunctional(mipl.getLocalMountPoint());
				else syncProfileUseJavaLastModified=isSetLastModifiedFunctional(mipl.getLocalDir());
			} else {
				if (mipl.getLocalDir().equals("")) syncProfileUseJavaLastModified=isSetLastModifiedFunctional(mipl.getLocalMountPoint());
				else syncProfileUseJavaLastModified=isSetLastModifiedFunctional(mipl.getLocalDir());
			}
		}

		if (mGp.debugLevel>=1) {
			addDebugLogMsg(1,"I","Sync parameters: " +
				"syncLocalToLocal="+syncLocalToLocal+
				", syncMasterProfType="+syncMasterProfType+
				", syncTargetProfType="+syncTargetProfType+
				", syncType="+ syncType + ", syncProfName=" + syncProfName
				+ ", syncRemoteAddr=" + syncRemoteAddr
				+ ", syncRemotePort=" + syncRemotePort
				+ ", syncRemoteShare=" + syncRemoteShare
				+ ", syncRemoteDir=" + syncRemoteDir
				+ ", syncLocalDir=" + syncLocalDir
				+ ", localUrl=" + localUrl
				+ ", remoteUrl=" + remoteUrl
				+ ", syncMasterLocalDir=" + syncMasterLocalDir
				+ ", syncTargetLocalDir=" + syncTargetLocalDir
				+ ", syncMasterDirFileProcess="+syncMasterDirFileProcess
				+ ", syncProfileConfirmRequired="+syncProfileConfirmRequired
				+ ", syncProfileUseJavaLastModified="+syncProfileUseJavaLastModified
				+ ", syncProfileNotUseLastModifiedForRemote="+syncProfileNotUseLastModifiedForRemote
				+ ", syncProfileSyncSubDir="+syncProfileSyncSubDir
				+ ", syncProfileSyncEmptyDirectory="+syncProfileSyncEmptyDirectory
				+ ", syncProfileSyncHiddenDirectory="+syncProfileSyncHiddenDirectory
				+ ", syncProfileSyncHiddenFile="+syncProfileSyncHiddenFile
				+ ", syncProfileSyncUseRemoteSmallIoArea="+syncProfileSyncUseRemoteSmallIoArea
				+ ", fileFilter=" + mipl.getFileFilter()
				+ ", dirFilter=" + mipl.getDirFilter());
 			addDebugLogMsg(9,"I","syncRemoteUserid=" + syncRemoteUserid+", syncRemotePassword=" + syncRemotePassword);
		}
	};
	
	private void printMpList() {
		if (mGp.localMountPointList!=null) {
			for (int i=0;i<mGp.localMountPointList.size();i++) addLogMsg("E","","mp="+mGp.localMountPointList.get(i));
		}
	}
	
	private String mRemoteHostName="", mRemoteHostAddress="";
	private int mRemoteHostPort=-1;
	private boolean mRemoteHostParmsAvailable=false;
	
	@SuppressWarnings("unused")
	private boolean checkRemoteHostAvailable(boolean msg) {
		boolean result=false;
		if (mRemoteHostParmsAvailable) {
			String msg_txt="";
			String srv="";
			if (mRemoteHostPort!=-1) {
				result=SMBSyncUtil.isSmbHostAddressConnected(mRemoteHostAddress, mRemoteHostPort);
				srv=mRemoteHostAddress+"("+String.valueOf(mRemoteHostPort)+")";
			} else if (mRemoteHostName.equals("")) {
				result=SMBSyncUtil.isSmbHostAddressConnected(mRemoteHostAddress);
				srv=mRemoteHostAddress;
			} else {
				if (resolveHostName(mRemoteHostName)!=null) result=true;
				srv=mRemoteHostName;
			}
			if (msg) {
				if (result) msg_txt=mGp.appContext.getString(R.string.msgs_mirror_remote_server_can_be_connected);
				else msg_txt=mGp.appContext.getString(R.string.msgs_mirror_remote_server_the_connection_impossible);
				addLogMsg("I","",String.format(msg_txt, srv));
			}
		}
		return result;
	};
	
	private String resolveHostName(String hn) {
		String ipAddress=NetworkUtil.getSmbHostIpAddressFromName(hn);
		addDebugLogMsg(1,"I","resolveHostName Name="+hn+", IP addr="+ipAddress);
		return ipAddress;
	}
	
	final private void doSyncMirror(MirrorIoParmList mipl) {
		if (mGp.debugLevel>=1) 
			addDebugLogMsg(1,"I","doSyncMirror entered ",
				"errorStatus="+checkErrorStatus(),
				", isExceptionOccured="+isExceptionOccured);
		if (syncLocalToLocal) {
			// Mirror local -> Local
			mirrorIoRootDir=syncMasterLocalDir;
//			isSamePhyscalDirectory(syncMasterLocalDir,syncTargetLocalDir);
			if (!isExceptionOccured) {
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				mirrorCopyLocalToLocal(false, syncMasterLocalDir,
						syncTargetLocalDir, syncTargetLocalDir);
				if (!isExceptionOccured && !isSyncParmError) {
					mirrorDeleteLocalToLocalFile(syncMasterLocalDir, syncTargetLocalDir);
				}
			}
		} else {
			if (syncMasterProfType.equals(SMBSYNC_PROF_TYPE_REMOTE)) { 
				// Mirror remote -> local
				mirrorIoRootDir=remoteUrl;
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				exceptionRetryCount=0;
				while(true) {
					mirrorCopyRemoteToLocal(false, remoteUrl, localUrl);
					if (!checkRetry()) break;
				}
//				Log.v("","exec="+isExceptionOccured+", parm="+isSyncParmError);
				if (!isExceptionOccured && !isSyncParmError) {
					exceptionRetryCount=0;
					while(true) {
						mirrorDeleteLocalFile(remoteUrl, localUrl);
						if (!checkRetry()) break;
					}
				}
			} else if (syncMasterProfType.equals(SMBSYNC_PROF_TYPE_LOCAL)) { 
				// Mirror local -> remote
				mirrorIoRootDir=localUrl;
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				exceptionRetryCount=0;
				while(true) {
					mirrorCopyLocalToRemote(false, localUrl,remoteUrl);
					if (!checkRetry()) break;
				}
				if (!isExceptionOccured && !isSyncParmError) {
					exceptionRetryCount=0;
					while(true) {
						mirrorDeleteRemoteFile(localUrl, remoteUrl);
						if (!checkRetry()) break;
					}
				}
			} else { 
				addLogMsg("E","","invalid master profile type specified:"+syncMasterProfType);
				tcMirror.setThreadMessage("invalid master profile type specified:"+syncMasterProfType);
				isSyncParmError=true;
			}
		}
	};

	final private void doSyncCopy(MirrorIoParmList mipl) {
		if (mGp.debugLevel>=1) 
			addDebugLogMsg(1,"I","doSyncCopy entered ",
				"errorStatus="+checkErrorStatus(),
				", isExceptionOccured="+isExceptionOccured);
		if (syncLocalToLocal) {
			// Mirror local -> Local
			mirrorIoRootDir=syncMasterLocalDir;
//			isSamePhyscalDirectory(syncMasterLocalDir,syncTargetLocalDir);
			if (!isExceptionOccured) {
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				mirrorCopyLocalToLocal(false, syncMasterLocalDir,
						syncTargetLocalDir, syncTargetLocalDir);
			}
		} else {
			if (syncMasterProfType.equals(SMBSYNC_PROF_TYPE_REMOTE)) { 
				// Copy remote -> local
				mirrorIoRootDir=remoteUrl;
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				exceptionRetryCount=0;
				while(true) {
					mirrorCopyRemoteToLocal(false, remoteUrl, localUrl);
					if (!checkRetry()) break;
				}
			} else if (syncMasterProfType.equals(SMBSYNC_PROF_TYPE_LOCAL)) { 
				// copy local -> remote
				mirrorIoRootDir=localUrl;
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				exceptionRetryCount=0;
				while(true) {
					mirrorCopyLocalToRemote(false, localUrl,remoteUrl);
					if (!checkRetry()) break;
				}
			} else {
				addLogMsg("E","","invalid master profile type specified:"+syncMasterProfType);
				tcMirror.setThreadMessage("invalid master profile type specified:"+syncMasterProfType);
				isSyncParmError=true;
			}
		}
	};
	
	final private void doSyncMove(MirrorIoParmList mipl) {
		if (mGp.debugLevel>=1) 
			addDebugLogMsg(1,"I","doSyncMove entered ",
				"errorStatus="+checkErrorStatus(),
				", isExceptionOccured="+isExceptionOccured);
		ArrayList<String> moved_dir_list = new ArrayList<String>();

		if (syncLocalToLocal) {
			// Mirror local -> Local
			mirrorIoRootDir=syncMasterLocalDir;
//			isSamePhyscalDirectory(syncMasterLocalDir,syncTargetLocalDir);
			if (!isExceptionOccured) {
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				mirrorMoveLocalToLocal(false, syncMasterLocalDir,
						syncTargetLocalDir, syncTargetLocalDir, moved_dir_list);
				if (copyCount>=0 && !isExceptionOccured && !isSyncParmError) {
					Collections.sort(moved_dir_list);
					for (int j=moved_dir_list.size()-1;j>=0;j--) {
						if (localUrl.equals(moved_dir_list.get(j)))
							break;
						if (isLocalDirEmpty(moved_dir_list.get(j))) 
							deleteLocalItem(true,moved_dir_list.get(j));
					}
				}
			}
		} else {
			if (syncMasterProfType.equals(SMBSYNC_PROF_TYPE_REMOTE)) { 
				// Move remote -> local
				mirrorIoRootDir=remoteUrl;
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				exceptionRetryCount=0;
				while(true) {
					mirrorMoveRemoteToLocal(false, remoteUrl, localUrl,moved_dir_list);
					if (!checkRetry()) break;
				}
				if (copyCount>=0 && !isExceptionOccured && !isSyncParmError) {
					Collections.sort(moved_dir_list);
					for (int j=moved_dir_list.size()-1;j>=0;j--) {
						if ((remoteUrl+"/").equals(moved_dir_list.get(j))) break;
						boolean dir_empty=false;
						exceptionRetryCount=0;
						while(true) {
							dir_empty=isRemoteDirEmpty(moved_dir_list.get(j));
							if (!checkRetry()) break;
						}
						if (!isExceptionOccured) {
							if (dir_empty) {
								exceptionRetryCount=0;
								while(true) {
									deleteRemoteItem(true,moved_dir_list.get(j));
									if (!checkRetry()) break;
								}
							}
						}
					}
				} 
			} else if (syncMasterProfType.equals(SMBSYNC_PROF_TYPE_LOCAL)) { 
				// Move local -> remote
				mirrorIoRootDir=localUrl;
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				exceptionRetryCount=0;
				while(true) {
					mirrorMoveLocalToRemote(false, localUrl,remoteUrl, moved_dir_list);
					if (!checkRetry()) break;
				}
				if (copyCount>=0 && !isExceptionOccured && !isSyncParmError) {
					Collections.sort(moved_dir_list);
					for (int j=moved_dir_list.size()-1;j>=0;j--) {
						if (localUrl.equals(moved_dir_list.get(j)))
							break;
						if (isLocalDirEmpty(moved_dir_list.get(j))) {
							deleteLocalItem(true,moved_dir_list.get(j));
						}
					}
				}
			} else {
				addLogMsg("E","","invalid master profile type specified:"+syncMasterProfType);
				tcMirror.setThreadMessage("invalid master profile type specified:"+syncMasterProfType);
				isSyncParmError=true;
			}
		}
	};

//	final private void isSamePhyscalDirectory(String master_fp, String target_fp) {
//		File mf=new File(master_fp+"/smbsync_tmp_file"); 
//		File tf=new File(target_fp+"/smbsync_tmp_file");
//		boolean result=false;
//		try {
//			String mfcp=mf.getCanonicalPath();
//			String tfcp=tf.getCanonicalPath();
////			Log.v("","isSamePhyscalDirectory mfcp="+mfcp);
////			Log.v("","isSamePhyscalDirectory tfcp="+tfcp);
//			if (mfcp.equals(tfcp)) result=true;
//		} catch (IOException e) {
//			e.printStackTrace();
//			result=true;
//		}
//		if (result) addLogMsg("E","","isSamePhyscalDirectory Exception detected. "
//				+ "Tried to write in the same directory the same file.");
//		isExceptionOccured=result;
//	};
	
	final private void buildMediaStoreDirList() {
		String [] proj = new String[] {MediaStore.MediaColumns.DATA};
    	ContentResolver resolver = mGp.appContext.getContentResolver();
        String c_m_d="";
    	//build image
        Cursor ci = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI ,
        		proj ,null ,null ,"_data");
        if (ci!=null) {
            while( ci.moveToNext() ){
            	String data=ci.getString( ci.getColumnIndex(MediaStore.MediaColumns.DATA ));
            	if (data!=null) {
                	if (data.lastIndexOf("/")>=1) {
        	        	String t_dir=data.substring(0,data.lastIndexOf("/"));
        	        	if  (!c_m_d.equals(t_dir)) {
        	        		mediaStoreImageList.add(t_dir);
        	        		c_m_d=t_dir;
        	        	}
                	}
            	}
            }
            ci.close();
        }
    	//build audio
        Cursor ca = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI ,
        		proj ,null ,null ,"_data");
        c_m_d="";
        if (ca!=null) {
            while( ca.moveToNext() ){
            	String data=ca.getString( ca.getColumnIndex(MediaStore.MediaColumns.DATA ));
            	if (data!=null) {
                	if (data.lastIndexOf("/")>=1) {
        	        	String t_dir=data.substring(0,data.lastIndexOf("/"));
        	        	if  (!c_m_d.equals(t_dir)) {
        	        		mediaStoreAudioList.add(t_dir);
        	        		c_m_d=t_dir;
        	        	}
                	}
            	}
            }
            ca.close();
        }
    	//build video
        Cursor cv = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        		proj ,null ,null ,"_data");
        c_m_d="";
        if (cv!=null) {
            while( cv.moveToNext() ){
            	String data=cv.getString( cv.getColumnIndex(MediaStore.MediaColumns.DATA ));
            	if (data!=null) {
                	if (data.lastIndexOf("/")>=1) {
        	        	String t_dir=data.substring(0,data.lastIndexOf("/"));
        	        	if  (!c_m_d.equals(t_dir)) {
        	        		mediaStoreVideoList.add(t_dir);
        	        		c_m_d=t_dir;
        	        	}
                	}
            	}
            }
            cv.close();
        }

        //build Files(Document)
//        if(Build.VERSION.SDK_INT >= 11) {
//       	Cursor cd = resolver.query(MediaStore.Files.getContentUri("external"),
//        				proj ,null ,null ,"_data");
//	        while( cd.moveToNext() ){
//	        	String data=cd.getString( cd.getColumnIndex(MediaStore.MediaColumns.DATA));
//	        	if (data.lastIndexOf("/")>=1) {
//		        	String t_dir=data.substring(0,data.lastIndexOf("/"));
//		        	if  (!findMediaStoreDirList(t_dir,mediaStoreFilesList)) {
//		        		mediaStoreFilesList.add(t_dir);
//		        	}
//	        	}
//	        }
//	        cd.close();
//        }

		if (mGp.debugLevel>=1) { 
        	for (int i=0;i<mediaStoreImageList.size();i++) 
        		addDebugLogMsg(2,"I","mediaStoreImageList="+mediaStoreImageList.get(i));
        	for (int i=0;i<mediaStoreAudioList.size();i++) 
        		addDebugLogMsg(2,"I","mediaStoreAudioList="+mediaStoreAudioList.get(i));
        	for (int i=0;i<mediaStoreVideoList.size();i++) 
        		addDebugLogMsg(2,"I","mediaStoreVideoList="+mediaStoreVideoList.get(i));
//        	for (int i=0;i<mediaStoreFilesList.size();i++) 
//        		sendDebugLogMsg(1,"I","mediaStoreFilesList="+mediaStoreFilesList.get(i));
        }
	};
	
//	private boolean findMediaStoreDirList(String dir, ArrayList<String> al) {
//		boolean found=false;
//		if (mediaStoreImageList.size()!=0) {
//			if (Collections.binarySearch(al,dir)>=0) 
//				found=true;
//		}
//		return found;
//	};

	final private void scanMediaStoreLibrary(String fp) {
//		defaultSettingScanExternalStorage
		if (LocalMountPoint.isExternalMountPoint(mGp.appContext, fp) && !defaultSettingScanExternalStorage) {
			if (mGp.debugLevel>=1) 
				addDebugLogMsg(1,"I",
					"scanMediaStoreLibrary scan external storage disabled, " ,
					"MediaScanner not invoked. Path=",fp);
			return;
		}
		if (!mediaScanner.isConnected()) {
			if (mGp.debugLevel>=1) 
				addLogMsg("W",fp,
					"mediaScanner not connected, MediaScanner not invoked.");
			return;
		}
		String mt=isMediaFile(fp);
		if (mt==null) mt="";
		if (Build.VERSION.SDK_INT<11) { //Android 2.1/2.2/2.3
			if (!isNoMediaPath(fp)) {  
				if	(mt.startsWith("audio") || mt.startsWith("video") ||
						 mt.startsWith("image") ) { 
					File lf=new File(fp);
					if (mGp.debugLevel>=2) 
						addDebugLogMsg(2,"I",
							"scanMediaStoreLibrary MediaScanner invoked. fn=",fp,
							", lastModified="+lf.lastModified()+
							", date=",
							StringUtil.convDateTimeTo_YearMonthDayHourMinSec(lf.lastModified()));
					mediaScanner.scanFile(fp, mt);
				} else {
					if (mGp.debugLevel>=2) 
						addDebugLogMsg(2,"I",
							"scanMediaStoreLibrary Mime type was not audio/image/video, " ,
							"MediaScanner not invoked. mime type=",mt);
				}
			} else {
				if (mGp.debugLevel>=2) 
					addDebugLogMsg(2,"I",
							"scanMediaStoreLibrary hidden directory or .nomedia found, " ,
							"MediaScanner not invoked.");
			}
		} else {//Android 3.0以上
			if	(mt.startsWith("audio") || mt.startsWith("video") ||
					 mt.startsWith("image") ) {
				if (!isNoMediaPath(fp)) {
					File lf=new File(fp);
					if (mGp.debugLevel>=2) 
						addDebugLogMsg(2,"I",
							"scanMediaStoreLibrary MediaScanner invoked. fn=",fp,
							", lastModified="+lf.lastModified(),
							", date=",
							StringUtil.convDateTimeTo_YearMonthDayHourMinSec(lf.lastModified()));
					mediaScanner.scanFile(fp, mt);
				} else {
					if (mGp.debugLevel>=2) 
						addDebugLogMsg(2,"I",
							"scanMediaStoreLibrary hidden directory or .nomedia found, ",
							"MediaScanner not invoked.");
				}
			} else {
				if (settingsMediaFiles) {
					File lf=new File(fp);
					if (mGp.debugLevel>=2) 
						addDebugLogMsg(2,"I",
							"scanMediaStoreLibrary MediaScanner invoked. fn=",fp,
							", lastModified="+lf.lastModified(),
							", date=",StringUtil.convDateTimeTo_YearMonthDayHourMinSec(lf.lastModified()));
					mediaScanner.scanFile(fp, mt);
				} else {
					if (mGp.debugLevel>=2) 
						addDebugLogMsg(2,"I",
							"scanMediaStoreLibrary scan MediaFiles disabled, " ,
							"MediaScanner not invoked.");
				}
			}
		}
	};

	static final private String isMediaFile(String fp) {
		String mt=null;
		String fid="";
		if (fp.lastIndexOf(".")>0) {
			fid=fp.substring(fp.lastIndexOf(".")+1,fp.length());
			fid=fid.toLowerCase(Locale.getDefault());
		}
		mt=MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
		return mt;
	};
	
	static final private boolean isNoMediaPath(String path) {
        if (path == null) return false;

        if (path.indexOf("/.") >= 0) return true;

        int offset = 1;
        while (offset >= 0  && offset<path.lastIndexOf("/")) {
            int slashIndex = path.indexOf('/', offset);
            if (slashIndex > offset) {
                slashIndex++; // move past slash
                File file = new File(path.substring(0, slashIndex) + ".nomedia");
//              Log.v("","off="+offset+", si="+slashIndex+", p="+file.getPath());                
                if (file.exists()) {
                    return true;
                }
            }
            offset = slashIndex;
        }
        return false;
    }

//	private void scanMediaStoreLibrary(String fp) {
//		String fid="";
//		if (fp.lastIndexOf(".")>0) {
//			fid=fp.substring(fp.lastIndexOf(".")+1,fp.length());
//			fid=fid.toLowerCase();
//		}
//		String mt=MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
//		if (mt!=null) {
//			sendDebugLogMsg(1,"I",
//					"scanMediaStoreLibrary scan was invoked. mt="+mt+", fn="+fp);
//			mediaScanner.scanFile(fp, mt);
//		} else {
//			sendDebugLogMsg(1,"I","scanMediaStoreLibrary scan was not invoked. fn="+fp);
//		}
//	};
	
	//API8
//	private void scanMediaStoreLibrary(String fp) {
//		String[] scanfile = new String[] {fp};
//		sendDebugLogMsg(1,"I","scanMediaStoreLibrary scan invoked fn="+fp);
//		MediaScannerConnection.scanFile(globalWorkArea.appContext,scanfile, null,null);
//	};
	
	@SuppressLint("NewApi")
	final private int deleteMediaStoreItem(String fp) {
		int dc_image=0, dc_audio=0, dc_video=0, dc_files=0;
		if (isMediaStoreDir(fp)) {
	    	ContentResolver cr = mGp.appContext.getContentResolver();
	    	dc_image=cr.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
	          		MediaStore.Images.Media.DATA + "=?", new String[]{fp} );
	       	dc_audio=cr.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
	           		MediaStore.Audio.Media.DATA + "=?", new String[]{fp} );
	       	dc_video=cr.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
	           		MediaStore.Video.Media.DATA + "=?", new String[]{fp} );
	        if(Build.VERSION.SDK_INT >= 11) {
	        	dc_files=cr.delete(MediaStore.Files.getContentUri("external"), 
	          		MediaStore.Files.FileColumns.DATA + "=?", new String[]{fp} );
	        }
			if (mGp.debugLevel>=1) 
	       		addDebugLogMsg(1,"I","deleMediaStoreItem fn=",fp,
	       				", delete count image="+dc_image,
	       				", audio="+dc_audio,", video="+dc_video,", files="+dc_files);
		} else {
			if (mGp.debugLevel>=1) 
	       		addDebugLogMsg(1,"I","deleMediaStoreItem not MediaStore library. fn=",fp);
		}
		
		return dc_image+dc_audio+dc_video+dc_files;
	};
	
	static final private String calTransferRate(long tb, long tt) {
	    String tfs = null;
	    BigDecimal bd_tr;
	    
	    if (tb==0) return "0Bytes/sec";
	    
	    if (tb>(1024)) {//KB
		    BigDecimal dfs1 = new BigDecimal(tb*1.000);
		    BigDecimal dfs2 = new BigDecimal(1024*1.000);
		    BigDecimal dfs3 = new BigDecimal("0.000000");
		    dfs3=dfs1.divide(dfs2);
			BigDecimal dft1 = new BigDecimal(tt*1.000);
		    BigDecimal dft2 = new BigDecimal(1000.000);
		    BigDecimal dft3 = new BigDecimal("0.000000");
		    dft3=dft1.divide(dft2);
			bd_tr=dfs3.divide(dft3,2,BigDecimal.ROUND_HALF_UP);
			tfs=bd_tr+"KBytes/sec";
		} else {
		    BigDecimal dfs1 = new BigDecimal(tb*1.000);
		    BigDecimal dfs2 = new BigDecimal(1024*1.000);
		    BigDecimal dfs3 = new BigDecimal("0.000000");
		    dfs3=dfs1.divide(dfs2);
			BigDecimal dft1 = new BigDecimal(tt*1.000);
		    BigDecimal dft2 = new BigDecimal(1000.000);
		    BigDecimal dft3 = new BigDecimal("0.000000");
		    dft3=dft1.divide(dft2);
			bd_tr=dfs3.divide(dft3,2,BigDecimal.ROUND_HALF_UP);
			tfs=bd_tr+"Bytes/sec";
		}
		
		return tfs;
	}

	final private void initIoBuffer() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mGp.appContext);
		String cp=
				prefs.getString(mGp.appContext.getString(R.string.settings_smb_perform_class), "");
		
		if (cp.equals("0")) {//Minimum
			settingsIoBuffers="4";
		} else if (cp.equals("1")) {//Medium
			settingsIoBuffers="4";
		} else if (cp.equals("2")) {//Large
			settingsIoBuffers="8";
		} else {
			settingsIoBuffers=
					prefs.getString(mGp.appContext.getString(R.string.settings_io_buffers), "8");
		}
			
		mirrorIoBufferSize=Integer.parseInt(settingsIoBuffers)*65536;
		
		if (mGp.debugLevel>=1) 
			addDebugLogMsg(1,"I","Io buffer size="+mirrorIoBufferSize);
		mirrorIoBuffer = new byte[mirrorIoBufferSize];
	};
	
	final private void setJcifsAuthParm() { 

		String tuser=null,tpass=null;
		if (syncRemoteUserid.length()!=0) tuser=syncRemoteUserid;
		if (syncRemotePassword.length()!=0) tpass=syncRemotePassword;
		ntlmPasswordAuth = 
				new NtlmPasswordAuthentication(null, tuser, tpass);
//		UniAddress dc;
//		try {
//			dc = UniAddress.getByName("WIN-SRV-2008");
//			SmbSession.logon(dc, ntlmPasswordAuth);
//		} catch (UnknownHostException e) {
//			e.printStackTrace();
//		} catch (SmbException e) {
//			e.printStackTrace();
//		}
	};

	
	final private int checkErrorStatus() {
		int rc=0;
		if (!tcMirror.isEnabled()) rc=-10;
		if (isExceptionOccured) rc=-1;
//		if (glblParms.debugLevel>=3) 
//			addDebugLogMsg(3,"I","checkErrorStatus status="+rc);
		return rc;
	};
	
	final private int mirrorCopyLocalToRemote(boolean allcopy, String masterUrl, String targetUrl) {
		SmbFile hf = null;
		File lf;
		
		if (mGp.debugLevel>=2) 
			addDebugLogMsg(2,"I","mirrorCopyLocalToRemote master=", masterUrl,
				", target=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();

		String tmp_target="";
		
		try {
			lf = new File(masterUrl);
			if (lf.exists()) {
				if (lf.isDirectory()) { // Directory copy
					if (lf.canRead() && !isHiddenDirectory(lf) &&
							isDirToBeProcessed(masterUrl.replace(mirrorIoRootDir, ""))
							) {
						if (syncProfileSyncEmptyDirectory) {
							createRemoteDirByDir(targetUrl);
							if (checkErrorStatus()!=0) return checkErrorStatus();
						}
						File[] children = lf.listFiles();
						if (children!=null) {
							for (File element : children) {
								String tmp = element.getName();
								if (!tmp.equals(".android_secure")) {
									if (tmp.lastIndexOf("/")>0) tmp=tmp.substring(0,tmp.lastIndexOf("/"));
									String n_master=masterUrl + "/"+ tmp;
									String n_target=targetUrl + "/"+ tmp;
									exceptionRetryCount=0;
									while(true) {
										if (element.isFile() ||
												(element.isDirectory() && syncProfileSyncSubDir)) {
											mirrorCopyLocalToRemote(allcopy, n_master, n_target);
											if (!checkRetry()) break;
										} else {
											if (mGp.debugLevel>=2) 
												addDebugLogMsg(2,"I","sub directory ignored by option, dir="+n_master);
											break;
										}
									}
									if (checkErrorStatus()!=0) return checkErrorStatus();
								} else {
									addDebugLogMsg(1,"I","Android system directory is ignored, fp="+element.getPath());
								}
							}
						} else {
							addDebugLogMsg(1,"I","Directory was null, dir="+lf.getPath());
						}
					} else { 
						if (!lf.canRead()) addDebugLogMsg(1,"I","Directory ignored because can not read, fp="+masterUrl);
					}
				} else { // file copy
					if (isDirFilteredByFileName(masterUrl.replace(mirrorIoRootDir+"/", "")) &&
							!isHiddenFile(lf) &&
							isFileFiltered(masterUrl)) {
						createRemoteDirByFilePath(targetUrl,null,masterUrl);
//						copiedFileList.add(masterUrl);
						lf = new File(masterUrl);
						hf = new SmbFile(targetUrl,ntlmPasswordAuth);
//						String t_fp=masterUrl;//.replace(localUrl, "");
						if (isFileChangedForLocalToRemote(masterUrl,lf,hf,allcopy)) { 
							// copy was done
							if (confirmCopy(targetUrl)) {
								long file_byte=lf.length();
								String t_fn=lf.getName().replace("/","");

								if (mGp.settingRemoteFileCopyByRename) {
									tmp_target=makeTempFilePath(targetUrl);
								}
								copyFileLocalToRemote(lf,hf,file_byte,t_fn,masterUrl,tmp_target);
								if (checkErrorStatus()!=0) {
									return checkErrorStatus();
								}
								try {
									if (!syncProfileNotUseLastModifiedForRemote)
										hf.setLastModified(lf.lastModified());
								} catch(SmbException e) {
									addLogMsg("W",targetUrl,
											mGp.appContext.getString(R.string.msgs_mirror_prof_remote_file_set_last_modified_failed));
									addDebugLogMsg(1,"W",targetUrl,
											"Remote file setLastModified() failed, reason="+ e.getMessage());
								}
								copyCount++;
//								mHistoryCopiedList.add(targetUrl);
							} else {
								addLogMsg("W",targetUrl,msgs_mirror_confirm_copy_cancel);
							}
						} else {
//							updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
//									targetUrl,hf.getLastModified());
						}
						if (!tcMirror.isEnabled()) return -10;
						if (isExceptionOccured) return -1;
					}
				}
			} else {
				if (mGp.debugLevel>=1) 
					addDebugLogMsg(1,"E","Local file ", masterUrl,
							" was not copied, because file/dir not existed.");
				addLogMsg("E",masterUrl,msgs_mirror_prof_master_not_found );
				tcMirror.setThreadMessage(msgs_mirror_prof_master_not_found+","+masterUrl);
				isExceptionOccured=true;
				return -1;
				
			}
		} catch (MalformedURLException e) {
			addLogMsg("E","","mirrorCopyLocalToRemote MFUE From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return setErrorStatusRetry(e.getMessage());
		} catch (SmbException e) {
			addLogMsg("E","","mirrorCopyLocalToRemote SMBE From="+masterUrl+", To="+targetUrl);
			String[] e_msg=NetworkUtil.analyzeNtStatusCode(e, mGp.appContext, 
					targetUrl,ntlmPasswordAuth.getUsername());
			addLogMsg("E","",e_msg[0]);//e.toString());
			printStackTraceElement(e.getStackTrace());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return setErrorStatusRetry(e_msg[0]);
		} catch (UnknownHostException e) {
			addLogMsg("E","","mirrorCopyLocalToRemote UKHE From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return setErrorStatusRetry(e.getMessage());
		} catch (FileNotFoundException e) {
			addLogMsg("E","","mirrorCopyLocalToRemote FNFD From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return setErrorStatusRetry(e.getMessage());
		} catch (IOException e) {
			addLogMsg("E","","mirrorCopyLocalToRemote IOE From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return setErrorStatusRetry(e.getMessage());
		}
		return 0;
	};

	private int setErrorStatusRetry(String msg) {
		isExceptionRetryRequired=true;
		isExceptionOccured=true;
		tcMirror.setThreadMessage(msg);
		return -1;
	};

	private boolean checkRetry() {
		if (syncProfileRetryCount>0) {
			if (isExceptionRetryRequired) {
				isExceptionRetryRequired=false;
				exceptionRetryCount++;
				if (exceptionRetryCount>syncProfileRetryCount) {
					addLogMsg("I","",mGp.appContext.getString(R.string.msgs_mirror_retry_ignored_by_limit));
					return false;//Retry abort
				} else {
					retryCount++;
					addDebugLogMsg(1,"I","Retry wait entered");
					synchronized(tcMirror) {
						try {
							tcMirror.wait(syncProfileRetryIntervalTime*1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if (tcMirror.isEnabled()) {
						addLogMsg("I","",mGp.appContext.getString(R.string.msgs_mirror_retry_issued));
						isExceptionOccured=false;
						tcMirror.setThreadMessage("");
						return true;//Retry required
					} else {
						addDebugLogMsg(1,"I","Cancel detected while retry wait");
						return false;//Retry not required
					}
				}
			} else {
				if (exceptionRetryCount>0) {
					addDebugLogMsg(1,"I","Retry was successful");
					exceptionRetryCount=0;
				}
				return false;
			}
		} else {
			isExceptionRetryRequired=false;
			return false;//Retry not required
		}
	};
	
	private void deleteRemoteTempFile(String tmp_target) {
		SmbFile hf_tmp=null;
		try {
			if (!tmp_target.equals("")) {
				hf_tmp=new SmbFile(tmp_target, ntlmPasswordAuth);
				if (hf_tmp.exists()) hf_tmp.delete();
			}
		} catch (SmbException e1) {
		} catch (MalformedURLException e) {
		}
		hf_tmp=null;
	};

	private void deleteLocalTempFile(String tmp_target) {
		if (!tmp_target.equals("")) {
			File lf_tmp=new File(tmp_target);
			if (lf_tmp.exists()) lf_tmp.delete();
		}
	};

	private void printStackTraceElement(StackTraceElement[] ste) {
		for (int i=0;i<ste.length;i++) {
			addLogMsg("E","",ste[i].toString());	
		}
	}
	
	final private int mirrorDeleteRemoteFile(String masterUrl, String targetUrl) {
		SmbFile hf;
		File lf;

		if (mGp.debugLevel>=1) 
			addDebugLogMsg(2,"I","mirrorDeleteRemoteFile master=", masterUrl,
				", target=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();

		try {
			hf = new SmbFile(targetUrl,ntlmPasswordAuth);
			String t_dir="";
			if (hf.isDirectory()) t_dir=hf.getPath();
				else t_dir=hf.getParent();
			if (!isDirExcluded(t_dir.replace(remoteUrl, "")) && 
//					!isHiddenDirectory(hf, hf.isHidden()) &&
					isDirToBeProcessed(t_dir.replace(remoteUrl, ""))
					){ 
				if (hf.isDirectory()) { // Directory Delete
					lf = new File(masterUrl);
					if (lf.exists()) {
						hf = new SmbFile(targetUrl + "/",ntlmPasswordAuth);
						SmbFile[] children = hf.listFiles();
						for (SmbFile element : children) {
							String tmp = element.getName();
							if (tmp.lastIndexOf("/")>0) tmp=tmp.substring(0,tmp.lastIndexOf("/"));
							exceptionRetryCount=0;
							while(true) {
								if (element.isFile() ||
										(element.isDirectory() && syncProfileSyncSubDir)) {
									mirrorDeleteRemoteFile(masterUrl + "/"+tmp,targetUrl+"/"+tmp);
									if (!checkRetry()) break;
								} else {
									if (mGp.debugLevel>=2) 
										addDebugLogMsg(2,"I","sub directory ignored by option, dir="+masterUrl + "/"+ tmp);
									break;
								}
							}
							if (checkErrorStatus()!=0) return checkErrorStatus();
						}
					} else {
						// local Dir was not found,delete remote dir
						if (confirmDelete(targetUrl)) {
							deleteRemoteItem(true,targetUrl);
//							mHistoryDeletedList.add(targetUrl);
						} else {
							addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
						}
						if (checkErrorStatus()!=0) return checkErrorStatus();
					}
				} else { // file Delete
					if (!isHiddenFile(hf, hf.isHidden())) {
						lf = new File(masterUrl);
						if (mGp.debugLevel>=1) 
							addDebugLogMsg(3,"I","Local file exists="+lf.exists());
						if (!lf.exists()) {
							String m_dir=targetUrl.replace(remoteMasterDir+"/","");
							if (!(m_dir.indexOf("/")<0 && !syncMasterDirFileProcess)) { 
								if (confirmDelete(targetUrl)) {
									deleteRemoteItem(true,targetUrl);
//									mHistoryDeletedList.add(targetUrl);
								} else {
									addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
								}
							}
							if (checkErrorStatus()!=0) return checkErrorStatus();
						}
					}
				}
			}
		} catch (MalformedURLException e) {
			addLogMsg("E","","mirrorDeleteRemoteFile MFUE From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			return setErrorStatusRetry(e.getMessage());
		} catch (SmbException e) {
			addLogMsg("E","","mirrorDeleteRemoteFile SMBE From="+masterUrl+", To="+targetUrl);
			String[] e_msg=NetworkUtil.analyzeNtStatusCode(e, mGp.appContext, 
					targetUrl,ntlmPasswordAuth.getUsername());
			addLogMsg("E","",e_msg[0]);//e.toString());
			printStackTraceElement(e.getStackTrace());
			return setErrorStatusRetry(e_msg[0]);
		}
		return deleteCount;
	};

	final private int mirrorCopyLocalToLocal(boolean allcopy, String masterUrl,
			String targetUrl, String target_fp_base) {
		File tf;
		File mf;
		
		if (mGp.debugLevel>=2) 
			addDebugLogMsg(2,"I","mirrorCopyLocalToLocal master=", masterUrl,
				", target=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();
		String tmp_target="";
		try {
			mf = new File(masterUrl);
			if (mf.exists()) {
				if (mf.isDirectory()) { // Directory copy
					if (mf.canRead() && !isHiddenDirectory(mf) &&
							isDirToBeProcessed(masterUrl.replace(mirrorIoRootDir, ""))
							) {
						if (syncProfileSyncEmptyDirectory) {
							createLocalDirByDir(targetUrl);
							if (checkErrorStatus()!=0) return checkErrorStatus();
						}
						File[] children = mf.listFiles();
						if (children!=null) {
							for (File element : children) {
								String tmp = element.getName();
								if (!tmp.equals(".android_secure")) {
									if (tmp.lastIndexOf("/")>0) tmp=tmp.substring(0,tmp.lastIndexOf("/"));
									String n_master=(masterUrl + "/"+ tmp).replaceAll("//", "/");
									String n_target=(targetUrl + "/" + tmp).replaceAll("//", "/");
									if (!n_master.equals(target_fp_base)) {
										if (element.isFile() ||
												(element.isDirectory() && syncProfileSyncSubDir)) {
											mirrorCopyLocalToLocal(allcopy, n_master, n_target, target_fp_base);
											if (checkErrorStatus()!=0) return checkErrorStatus();
										} else {
											if (mGp.debugLevel>=2) 
												addDebugLogMsg(2,"I","sub directory ignored by option, dir="+
															n_master);
										}
									} else {
//										addLogMsg("W","",
										addDebugLogMsg(1,"W",
												String.format(msgs_mirror_same_directory_ignored,n_master));
									}
								} else {
									addDebugLogMsg(1,"I","Android system directory is ignored, fp="+element.getPath());
								}
							}
						} else {
							addDebugLogMsg(1,"I","Directory was null, dir="+mf.getPath());
						}
					} else {
						if (!mf.canRead()) addDebugLogMsg(1,"I","Directory ignored because can not read, fp="+masterUrl);
					} 
				} else { // file copy
					if (isDirFilteredByFileName(masterUrl.replace(mirrorIoRootDir, "")) &&
							!isHiddenFile(mf) &&
							isFileFiltered(masterUrl)) {
						if (createLocalDirByFilePath(targetUrl,null,masterUrl)) {
							mf = new File(masterUrl);
							tf = new File(targetUrl);
//							String t_fp=masterUrl;//.replace(localUrl, "");
							if (isFileChanged(targetUrl,tf,mf,allcopy)) {							
								// copy was done
								if (confirmCopy(targetUrl)) {
									long file_byte=mf.length();
									String t_fn=mf.getName().replace("/","");

									if (mGp.settingLocalFileCopyByRename) {
										tmp_target=makeTempFilePath(targetUrl);
//										tmp_target=makeLocalTempFilePath(targetUrl);
									}
									copyFileLocalToLocal(mf,tf,file_byte,t_fn,masterUrl, tmp_target);
									if (checkErrorStatus()!=0) return checkErrorStatus();
									tf.setLastModified(mf.lastModified());
									updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
											targetUrl,mf.lastModified());
									scanMediaStoreLibrary(targetUrl);
									copyCount++;
//									mHistoryCopiedList.add(targetUrl);
								} else {
									addLogMsg("W",targetUrl,msgs_mirror_confirm_copy_cancel);
								}
							}
						}
						if (!tcMirror.isEnabled()) return -10;
						if (isExceptionOccured) return -1;
					}
				}
			} else {
				if (mGp.debugLevel>=1) 
					addDebugLogMsg(1,"E","Local file ", masterUrl,
							" was not copied, because file/dir not existed.");
				addLogMsg("E",masterUrl,msgs_mirror_prof_master_not_found );
				tcMirror.setThreadMessage(msgs_mirror_prof_master_not_found+","+masterUrl);
				isExceptionOccured=true;
				return -1;
				
			}
//		} catch (MalformedURLException e) {
//			addLogMsg("E","","mirrorCopyLocalToRemote From="+masterUrl+", To="+targetUrl);
//			addLogMsg("E","",e.getMessage());//e.toString());
//			printStackTraceElement(e.getStackTrace());
//			isExceptionOccured=true;
//			tcMirror.setThreadMessage(e.getMessage());
//			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
//			return -1;
//		} catch (SmbException e) {
//			addLogMsg("E","","mirrorCopyLocalToRemote From="+masterUrl+", To="+targetUrl);
//			addLogMsg("E","",e.getMessage());//e.toString());
//			printStackTraceElement(e.getStackTrace());
//			isExceptionOccured=true;
//			tcMirror.setThreadMessage(e.getMessage());
//			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
//			return -1;
//		} catch (UnknownHostException e) {
//			addLogMsg("E","","mirrorCopyLocalToRemote From="+masterUrl+", To="+targetUrl);
//			addLogMsg("E","",e.getMessage());//e.toString());
//			printStackTraceElement(e.getStackTrace());
//			isExceptionOccured=true;
//			tcMirror.setThreadMessage(e.getMessage());
//			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
//			return -1;
		} catch (FileNotFoundException e) {
			addLogMsg("E","","mirrorCopyLocalToLocal From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (IOException e) {
			addLogMsg("E","","mirrorCopyLocalToLocal From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		}
		return 0;
	};

	final private int mirrorDeleteLocalToLocalFile(String masterUrl, String targetUrl) {
		File mf;
		File lf;

		if (mGp.debugLevel>=1) 
			addDebugLogMsg(2,"I","mirrorDeleteLocalToLocalFile master=", masterUrl,
				", target=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();

		mf = new File(targetUrl);
		String t_dir="";
		if (mf.isDirectory()) t_dir=mf.getPath();
			else t_dir=mf.getParent();
		if (!isDirExcluded(t_dir.replace(syncTargetLocalDir, "")) && 
				!isHiddenDirectory(mf) &&
//				isDirectoryToBeProcessed(t_dir.replace(syncTargetLocalDir+"/", ""))
				isDirToBeProcessed(t_dir.replace(syncTargetLocalDir, ""))
				){ 
			if (mf.isDirectory()) { // Directory Delete
				lf = new File(masterUrl);
				if (lf.exists()) {
					mf = new File(targetUrl + "/");
					File[] children = mf.listFiles();
					for (File element : children) {
						String tmp = element.getName();
						if (tmp.lastIndexOf("/")>0) tmp=tmp.substring(0,tmp.lastIndexOf("/"));
						if (element.isFile() ||
								(element.isDirectory() && syncProfileSyncSubDir)) {
							mirrorDeleteLocalToLocalFile(masterUrl + "/"+tmp,targetUrl+"/"+tmp);
						} else {
							if (mGp.debugLevel>=2) 
								addDebugLogMsg(2,"I","sub directory ignored by option, dir="+masterUrl+"/"+ tmp);
						}
						if (checkErrorStatus()!=0) return checkErrorStatus();
					}
				} else {
					// local Dir was not found,delete remote dir
					if (confirmDelete(targetUrl)) {
						deleteLocalItem(true,targetUrl);
//							mHistoryDeletedList.add(targetUrl);
					} else {
						addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
					}
					if (checkErrorStatus()!=0) return checkErrorStatus();
				}
			} else { // file Delete
				if (!isHiddenFile(mf)) {
					lf = new File(masterUrl);
					if (mGp.debugLevel>=1) 
						addDebugLogMsg(3,"I","Local file exists="+lf.exists());
					if (!lf.exists()) {
						String m_dir=targetUrl.replace(syncTargetLocalDir+"/","");
//						Log.v("","mdir="+m_dir);
						if (!(m_dir.indexOf("/")<0 && !syncMasterDirFileProcess)) { 
							if (confirmDelete(targetUrl)) {
								deleteLocalItem(true,targetUrl);
//									mHistoryDeletedList.add(targetUrl);
							} else {
								addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
							}
						}
						if (checkErrorStatus()!=0) return checkErrorStatus();
					}
				}
			}
		}
		return deleteCount;
	};

	private boolean isHiddenDirectory(File lf) {
		boolean result=false;
		String name=lf.getName();
		if (!syncProfileSyncHiddenDirectory) {
			if (name.startsWith(".")) result=true;
		}
		if (mGp.debugLevel>=2) 
			addDebugLogMsg(2,"I","isHiddenDirectory(Local) result="+result+", Name="+name);
		return result;
	};

	private boolean isHiddenDirectory(SmbFile hf, boolean isHidden) {
		boolean result=false;
		String name=hf.getName().replace("/","");
		if (!syncProfileSyncHiddenDirectory) {
			result=isHidden;
//			result=hf.isHidden();
		}
		if (mGp.debugLevel>=2) 
			addDebugLogMsg(2,"I","isHiddenDirectory(Remote) result="+result+", Name="+name);
		return result;
	};

	private boolean isHiddenFile(File lf) {
		boolean result=false;
		String name=lf.getName();
		if (!syncProfileSyncHiddenFile) {
			if (name.startsWith(".")) result=true;
		}
		if (mGp.debugLevel>=2) 
			addDebugLogMsg(2,"I","isHiddenFile(Local) result="+result+", Name="+name);
		return result;
	};

	private boolean isHiddenFile(SmbFile hf, boolean isHidden) {
		boolean result=false;
		String name=hf.getName().replace("/","");
		if (!syncProfileSyncHiddenFile) {
			result=isHidden;
//			result=hf.isHidden();
		}
		if (mGp.debugLevel>=2) 
			addDebugLogMsg(2,"I","isHiddenFile(Remote) result="+result+", Name="+name);
		return result;
	};

	final private int mirrorCopyRemoteToLocal(boolean allcopy, String masterUrl, String targetUrl) {
		SmbFile hf;
		File lf;

		if (mGp.debugLevel>=2) 
			addDebugLogMsg(2,"I","mirrorCopyRemoteToLocal from=", masterUrl, ", to=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();
		String tmp_target="";
		try {
			hf = new SmbFile(masterUrl,ntlmPasswordAuth);
			if (hf.exists()) {
				if (hf.isDirectory()) { // Directory copy
					if (hf.canRead() && !isHiddenDirectory(hf, hf.isHidden()) && 
							isDirToBeProcessed(masterUrl.replace(mirrorIoRootDir, ""))
							) {
						if (syncProfileSyncEmptyDirectory) {
							createLocalDirByDir(targetUrl);
							if (checkErrorStatus()!=0) return checkErrorStatus();
						}
						hf = new SmbFile(masterUrl + "/",ntlmPasswordAuth);
						try {
							SmbFile[] children = hf.listFiles();
							for (SmbFile element : children) {
								String tmp = element.getName();
								if (tmp.lastIndexOf("/")>0) tmp=tmp.substring(0,tmp.lastIndexOf("/"));
								String n_master=masterUrl+"/"+tmp;
								String n_target=targetUrl+"/"+tmp;
								exceptionRetryCount=0;
								while(true) {
									if (element.isFile() ||
											(element.isDirectory() && syncProfileSyncSubDir)) {
										mirrorCopyRemoteToLocal(allcopy, n_master, n_target);
										if (!checkRetry()) break;
									} else {
										if (mGp.debugLevel>=2) 
											addDebugLogMsg(2,"I","sub directory ignored by option, dir="+n_master);
										break;
									}
								}
								if (checkErrorStatus()!=0) return checkErrorStatus();
							}
						} catch (SmbException e) {
							addLogMsg("W","","SmbException occured during SmbFile#listFiles(), name="+masterUrl+
										", jcifs error="+e.getMessage());
						}
					} else {
						if (!hf.canRead()) addDebugLogMsg(1,"I","Directory ignored because can not read, fp="+masterUrl);
					}
				} else { // file copy
					if (isDirFilteredByFileName(masterUrl.replace(mirrorIoRootDir, "")) &&
							!isHiddenFile(hf, hf.isHidden()) &&
							isFileFiltered(masterUrl)) {
						if (createLocalDirByFilePath(targetUrl,null,masterUrl)) {
							lf = new File(targetUrl);
//							String t_fp=masterUrl;//.replace("smb://"+syncRemoteAddr, "");
							if (isFileChanged(masterUrl,lf,hf,allcopy)) {
								// copy 
								if (confirmCopy(targetUrl)) {
									long file_byte=hf.length();
									String t_fn =hf.getName().replace("/", "");
									if (mGp.settingLocalFileCopyByRename) {
										tmp_target=makeTempFilePath(targetUrl);
									}
									copyFileRemoteToLocal(hf,lf,file_byte,t_fn,masterUrl, tmp_target);
									updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
											targetUrl,hf.getLastModified());
									if (checkErrorStatus()!=0) return checkErrorStatus();
//									mHistoryCopiedList.add(targetUrl);
									if (syncProfileUseJavaLastModified) {
										if (!lf.setLastModified(hf.lastModified())) {
											addLogMsg("W",targetUrl,
												mGp.appContext.getString(R.string.msgs_mirror_prof_local_file_set_last_modified_failed));
										}
									}
//									if (isMediaStoreDir(lf.getParent()))
									scanMediaStoreLibrary(targetUrl);
									copyCount++;
								} else {
//									copiedFileList.remove(targetUrl);
									addLogMsg("W",targetUrl,msgs_mirror_confirm_delete_cancel);
								}
							} else {
								updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
										targetUrl,hf.getLastModified());
							}
						}
						if (checkErrorStatus()!=0) return checkErrorStatus();
					}
				}
			} else {
				if (mGp.debugLevel>=1) 
					addDebugLogMsg(1,"E","remote file ", masterUrl,
							" was not copied, because file/dir not found");
				addMsgToProgDlg(true,"E", 0, masterUrl,msgs_mirror_prof_master_not_found);
				tcMirror.setThreadMessage(msgs_mirror_prof_master_not_found+","+masterUrl);
				isExceptionOccured=true;
				return -1;
			}
		} catch (MalformedURLException e) {
			addLogMsg("E","","mirrorCopyRemoteToLocal MFUE From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return setErrorStatusRetry(e.getMessage());
		} catch (SmbException e) {
			addLogMsg("E","","mirrorCopyRemoteToLocal SMBE From="+masterUrl+", To="+targetUrl);
			String[] e_msg=NetworkUtil.analyzeNtStatusCode(e, mGp.appContext, 
					masterUrl,ntlmPasswordAuth.getUsername());
			addLogMsg("E","",e_msg[0]);//e.toString());
			printStackTraceElement(e.getStackTrace());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return setErrorStatusRetry(e_msg[0]);
		} catch (UnknownHostException e) {
			addLogMsg("E","","mirrorCopyRemoteToLocal UKHE From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return setErrorStatusRetry(e.getMessage());
		} catch (FileNotFoundException e) {
			addLogMsg("E","","mirrorCopyRemoteToLocal FNFD From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return setErrorStatusRetry(e.getMessage());
		} catch (IOException e) {
			addLogMsg("E","","mirrorCopyRemoteToLocal IOE From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return setErrorStatusRetry(e.getMessage());
		}
		return 0;
	};
	
	private int confirmCopyResult=0, confirmDeleteResult=0;
	final private boolean confirmDelete(String url) {
		boolean result=true;
		if (mGp.debugLevel>=2) 
			addDebugLogMsg(2,"I","confirmDelete entered url=",url);

		if (syncProfileConfirmRequired) {
			if (confirmDeleteResult!=SMBSYNC_CONFIRM_RESP_YESALL && 
					confirmDeleteResult!=SMBSYNC_CONFIRM_RESP_NOALL) {
				try {
					tcConfirm.initThreadCtrl();
					callBackStub.cbShowConfirm(url, SMBSYNC_CONFIRM_REQUEST_DELETE);
					synchronized(tcConfirm) {
						tcConfirm.wait();//Posted by SMBSyncService#aidlConfirmResponse()
					}
					confirmDeleteResult=tcConfirm.getExtraDataInt();
					if (confirmDeleteResult>0) result=true;
					else result=false;
				} catch (RemoteException e) {
					addLogMsg("E","","RemoteException occured");
					printStackTraceElement(e.getStackTrace());
				} catch (InterruptedException e) {
					addLogMsg("E","","InterruptedException occured");
					printStackTraceElement(e.getStackTrace());
				}
			} else {
				if (confirmDeleteResult==SMBSYNC_CONFIRM_RESP_YESALL) result=true;
				else result=false;
			}
		}
		if (mGp.debugLevel>=2) 
			addDebugLogMsg(2,"I","confirmDelete result="+result,
				", confirmResult="+confirmDeleteResult,", syncProfileConfirmRequired="+syncProfileConfirmRequired);

		return result;
	};

	final private boolean confirmCopy(String url) throws MalformedURLException, SmbException {
		boolean result=true;
		if (mGp.debugLevel>=2) 
			addDebugLogMsg(2,"I","confirmCopy entered url=",url);
		if (syncProfileConfirmRequired) {
			if (confirmCopyResult!=SMBSYNC_CONFIRM_RESP_YESALL && 
					confirmCopyResult!=SMBSYNC_CONFIRM_RESP_NOALL) {
				boolean file_exists=false;
				if (url.startsWith("smb://")) {
					SmbFile rf=new SmbFile(url+"/",ntlmPasswordAuth);
					file_exists=rf.exists();
				} else {
					File lf=new File(url);
					file_exists=lf.exists();
				}
				if (file_exists) {
					try {
						tcConfirm.initThreadCtrl();
						callBackStub.cbShowConfirm(url, SMBSYNC_CONFIRM_REQUEST_COPY);
						synchronized(tcConfirm) {
							tcConfirm.wait();//Posted by SMBSyncService#aidlConfirmResponse()
						}
						confirmCopyResult=tcConfirm.getExtraDataInt();
						if (confirmCopyResult>0) result=true;
						else result=false;
					} catch (RemoteException e) {
						addLogMsg("E","","RemoteException occured");
						printStackTraceElement(e.getStackTrace());
					} catch (InterruptedException e) {
						addLogMsg("E","","InterruptedException occured");
						printStackTraceElement(e.getStackTrace());
					}
				}
			} else {
				if (confirmCopyResult==SMBSYNC_CONFIRM_RESP_YESALL) result=true;
				else result=false;
			}
		}
		
		if (mGp.debugLevel>=2) 
			addDebugLogMsg(2,"I","confirmCopy result="+result
				+", confirmResult="+confirmCopyResult+", syncProfileConfirmRequired="+syncProfileConfirmRequired);

		return result;
	};
	
	final private int mirrorDeleteLocalFile(String masterUrl, String targetUrl) {
		SmbFile hf;
		File lf;

		if (mGp.debugLevel>=2) addDebugLogMsg(2,"I","mirrorDeleteLocalFile master=", masterUrl, ", target=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();

		try {
			lf = new File(targetUrl);
			String t_dir="";
			if (lf.isDirectory()) t_dir=lf.getPath();
				else t_dir=lf.getParent();
			if (!isDirExcluded(t_dir.replace(localUrl, "")+"/") && 
//					!isHiddenDirectory(lf) &&
//					isDirectoryToBeProcessed(t_dir.replace(localUrl, "")+"/")
					isDirToBeProcessed(t_dir.replace(localUrl, ""))
					){ 
				if (lf.isDirectory()) { // Directory Delete
					lf = new File(targetUrl + "/");
					hf = new SmbFile(masterUrl,ntlmPasswordAuth);
					if (hf.exists()) {
						File[] children = lf.listFiles();
						for (File element : children) {
							String tmp = element.getName();
							if (tmp.lastIndexOf("/")>0) 
								tmp=tmp.substring(0,tmp.lastIndexOf("/"));
							while(true) {
								if (element.isFile() ||
										(element.isDirectory() && syncProfileSyncSubDir)) {
									mirrorDeleteLocalFile(masterUrl + "/"+ tmp,targetUrl + "/" + tmp);
									if (!checkRetry()) break;
								} else {
									if (mGp.debugLevel>=2) 
										addDebugLogMsg(2,"I","sub directory ignored by option, dir="+masterUrl + "/"+ tmp);
									break;
								}
							}
							if (checkErrorStatus()!=0) return checkErrorStatus();
						}
					} else {
						// remote Dir was not found, delete local dir
						if (confirmDelete(targetUrl)) {
							deleteLocalItem(true,targetUrl); 
//							mHistoryDeletedList.add(targetUrl);
						} else {
							addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
						}
						if (checkErrorStatus()!=0) return checkErrorStatus();
					}
				} else { // file Delete
					if (!isHiddenFile(lf)) {
						hf = new SmbFile(masterUrl,ntlmPasswordAuth);
						if (mGp.debugLevel>=3) 
							addDebugLogMsg(3,"I","Remote file exists="+hf.exists());
						if (!hf.exists() ) {
							String m_dir=targetUrl.replace(syncLocalDir+"/","");
							if (!(m_dir.indexOf("/")<0 && !syncMasterDirFileProcess)) { 
								if (confirmDelete(targetUrl)) {
									deleteLocalItem(true,targetUrl);
//									mHistoryDeletedList.add(targetUrl);
								} else {
									addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
								}
							}
							if (checkErrorStatus()!=0) return checkErrorStatus();
						}
					}
				}
			}
		} catch (MalformedURLException e) {
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			printStackTraceElement(e.getStackTrace());
			return setErrorStatusRetry(e.getMessage());
		} catch (SmbException e) {
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			printStackTraceElement(e.getStackTrace());
			String[] e_msg=NetworkUtil.analyzeNtStatusCode(e, mGp.appContext, 
					masterUrl,ntlmPasswordAuth.getUsername());
			return setErrorStatusRetry(e_msg[0]);
		}
		return deleteCount;
	};

//	private boolean isRootDirFile(String fp) {
//		
//		String t_fp=fp.replace("/", "");
//		Log.v("","tu="+fp+". td="+t_fp);
//		if (t_fp.indexOf("/")>0) return false;
//		else return true;
//		
//	};
	
	final private int mirrorMoveRemoteToLocal(boolean allcopy, String masterUrl,
			String targetUrl, ArrayList<String> moved_dirs) {
		SmbFile hf;
		File lf;
		
		if (mGp.debugLevel>=2) 
			addDebugLogMsg(2,"I","mirrorMoveFileToLocal_Copy from=", masterUrl, ", to=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();
		String tmp_target="";
		try {
			hf = new SmbFile(masterUrl,ntlmPasswordAuth);
			if (hf.exists()) {
				if (hf.isDirectory()) { // Directory copy
					if (hf.canRead() && !isHiddenDirectory(hf, hf.isHidden()) &&
							isDirToBeProcessed(masterUrl.replace(mirrorIoRootDir, ""))
							) {
						if (syncProfileSyncEmptyDirectory) {
							createLocalDirByDir(targetUrl);
							if (checkErrorStatus()!=0) return checkErrorStatus();
						}
						hf = new SmbFile(masterUrl + "/",ntlmPasswordAuth);
						try {
							SmbFile[] children = hf.listFiles();
							for (SmbFile element : children) {
								String tmp = element.getName();
								if (tmp.lastIndexOf("/")>0) tmp=tmp.substring(0,tmp.lastIndexOf("/"));
								
								String n_master=masterUrl + "/"+ tmp;
								String n_target=targetUrl + "/"+ tmp;
								exceptionRetryCount=0;
								while(true) {
									if (element.isFile() ||
											(element.isDirectory() && syncProfileSyncSubDir)) {
										mirrorMoveRemoteToLocal(allcopy, n_master, n_target, moved_dirs);
										if (!checkRetry()) break;
									} else {
										if (mGp.debugLevel>=2) 
											addDebugLogMsg(2,"I","sub directory ignored by option, dir="+masterUrl + "/"+ tmp);
										break;
									}
								}
								if (checkErrorStatus()!=0) return checkErrorStatus();
							}
						} catch (SmbException e) {
							addLogMsg("W","","SmbException occured during SmbFile#listFiles(), name="+masterUrl+
									", jcifs error="+e.getMessage());
						}
					} else {
						if (!hf.canRead()) addDebugLogMsg(1,"I","Directory ignored because can not read, fp="+masterUrl);
					}
				} else { // file copy
					if (isDirFilteredByFileName(masterUrl.replace(mirrorIoRootDir+"/", "")) &&
							!isHiddenFile(hf, hf.isHidden()) &&
							isFileFiltered(masterUrl)) {
						if (createLocalDirByFilePath(targetUrl,moved_dirs,masterUrl)) {
							lf = new File(targetUrl);
							String t_fn=hf.getName().replace("/", "");
//							String t_fp=masterUrl;//.replace("smb://"+syncRemoteAddr, "");
							if (isFileChanged(masterUrl,lf,hf,allcopy)) {
								// copy
								if (confirmCopy(targetUrl)) {
									long file_byte=hf.length();
									if (mGp.settingLocalFileCopyByRename) {
										tmp_target=makeTempFilePath(targetUrl);
									}
									copyFileRemoteToLocal(hf,lf,file_byte,t_fn,masterUrl, tmp_target);
									updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
											targetUrl,hf.lastModified());
									if (checkErrorStatus()!=0) return checkErrorStatus();
//									mHistoryCopiedList.add(targetUrl);
									if (!lf.setLastModified(hf.lastModified())) {
										if (mGp.debugLevel>=1) 
											addDebugLogMsg(1,"E","setLastModified() was failed. File name=", targetUrl);
									}
//									if (isMediaStoreDir(lf.getParent()))
									scanMediaStoreLibrary(targetUrl);
									copyCount++;
									
									// delete master file
									if (confirmDelete(masterUrl)) {
										addMovedDirList(moved_dirs,hf.getParent());
										deleteRemoteItem(true,masterUrl);
//										mHistoryDeletedList.add(masterUrl);
										addMsgToProgDlg(false,"I", 0, t_fn,msgs_mirror_prof_file_deleted);
									} else {
										addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
									}
								} else {
									addLogMsg("W",targetUrl,msgs_mirror_confirm_copy_cancel);
								}
							} else {
								updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
										targetUrl,hf.lastModified());
								if (confirmDelete(masterUrl)) {
									// delete master file
									addMovedDirList(moved_dirs,hf.getParent());
									deleteRemoteItem(true,masterUrl);
//									mHistoryDeletedList.add(masterUrl);
									addMsgToProgDlg(false,"I", 0, t_fn,msgs_mirror_prof_file_deleted);
								} else {
									addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
								}
							}
						}
						if (checkErrorStatus()!=0) return checkErrorStatus();
					}
				}
			} else {
				if (mGp.debugLevel>=1) 
					addDebugLogMsg(1,"E","remote file ", masterUrl,
							" was not copied, because file/dir not found");
				addMsgToProgDlg(true,"E",0,masterUrl,msgs_mirror_prof_master_not_found);
				tcMirror.setThreadMessage(msgs_mirror_prof_master_not_found+","+masterUrl);
				isExceptionOccured=true;
				return -1;
				
			}
		} catch (MalformedURLException e) {
			addLogMsg("E","","mirrorMoveRemoteToLocal MFUE From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return setErrorStatusRetry(e.getMessage());
		} catch (SmbException e) {
			addLogMsg("E","","mirrorMoveRemoteToLocal SMBE From="+masterUrl+", To="+targetUrl);
			String[] e_msg=NetworkUtil.analyzeNtStatusCode(e, mGp.appContext, 
					masterUrl,ntlmPasswordAuth.getUsername());
			addLogMsg("E","",e_msg[0]);//e.toString());
			printStackTraceElement(e.getStackTrace());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return setErrorStatusRetry(e_msg[0]);
		} catch (UnknownHostException e) {
			addLogMsg("E","","mirrorMoveRemoteToLocal UKHE From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return setErrorStatusRetry(e.getMessage());
		} catch (FileNotFoundException e) {
			addLogMsg("E","","mirrorMoveRemoteToLocal FNFD From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return setErrorStatusRetry(e.getMessage());
		} catch (IOException e) {
			addLogMsg("E","","mirrorMoveRemoteToLocal IOE From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return setErrorStatusRetry(e.getMessage());
		}
		return 0;
	};

	private String makeTempFilePath(String  targetUrl) {
		String tmp_wu="";
		String last_sep="";
		if (targetUrl.endsWith("/")) {
			tmp_wu=targetUrl.substring(0,(targetUrl.length()-1));
			last_sep="/";
		} else tmp_wu=targetUrl;
//		String target_dir=tmp_wu.substring(0,tmp_wu.lastIndexOf("/"));
//		target_dir=target_dir.substring(0,target_dir.lastIndexOf("/"))+"/";
//		String target_fn=tmp_wu.replace(target_dir, "");
//		target_fn=target_fn.substring(0,(target_fn.length()-1));
//		String tmp_target=target_dir+"SMBSyncWk.tmp"+last_sep;
		String tmp_target=tmp_wu+".SMBSync.work"+last_sep;
//		Log.v("","tmp="+tmp_target+", to="+targetUrl);
		return tmp_target;
	};

//	private String makeLocalTempFilePath(String  targetUrl) {
//		String tmp_target="/sdcard/Android/data/com.sentaroh.android.SMBSync/files/SMBSync.work";
////		Log.v("","tmp="+tmp_target+", to="+targetUrl);
//		return tmp_target;
//	};

	final private int mirrorMoveLocalToRemote(boolean allcopy, String masterUrl,
			String targetUrl, ArrayList<String> moved_dirs) {
		SmbFile hf=null;
		File lf;
		
		if (mGp.debugLevel>=1) 
			addDebugLogMsg(2,"I","mirrorMoveLocalToRemote master=", masterUrl,
				", target=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();

		String tmp_target="";
		
		try {
			lf = new File(masterUrl);
			if (lf.exists()) {
				if (lf.isDirectory()) { // Directory copy
					if (lf.canRead() && !isHiddenDirectory(lf) &&
							isDirToBeProcessed(masterUrl.replace(mirrorIoRootDir, ""))
							) {
						if (syncProfileSyncEmptyDirectory) {
							createRemoteDirByDir(targetUrl);
							if (checkErrorStatus()!=0) return checkErrorStatus();
						}
						File[] children = lf.listFiles();
						if (children!=null) {
							for (File element : children) {
								String tmp = element.getName();
								
								if (!tmp.equals(".android_secure")) {
									if (tmp.lastIndexOf("/")>0) tmp=tmp.substring(0,tmp.lastIndexOf("/"));
									String n_master=masterUrl + "/"+ tmp;
									String n_target=targetUrl + "/" + tmp;
									exceptionRetryCount=0;
									while(true) {
										if (element.isFile() ||
												(element.isDirectory() && syncProfileSyncSubDir)) {
											mirrorMoveLocalToRemote(allcopy, n_master, n_target, moved_dirs);
											if (!checkRetry()) break;
										} else {
											if (mGp.debugLevel>=2) 
												addDebugLogMsg(2,"I","sub directory ignored by option, dir="+masterUrl + "/"+ tmp);
											break;
										}
									}
									if (checkErrorStatus()!=0) return checkErrorStatus();
								} else {
									addDebugLogMsg(1,"I","Android system directory is ignored, fp="+element.getPath());
								}

							}
						} else {
							addDebugLogMsg(1,"I","Directory was null, dir="+lf.getPath());
						}
					} else {
						if (!lf.canRead()) addDebugLogMsg(1,"I","Directory ignored because can not read, fp="+masterUrl);
					}
				} else { // file copy
//					Log.v("","master="+masterUrl+", mirrorIoRootDir="+mirrorIoRootDir);
					if (isDirFilteredByFileName(masterUrl.replace(mirrorIoRootDir+"/", "")) &&
							!isHiddenFile(lf) &&
							isFileFiltered(masterUrl)) {
						createRemoteDirByFilePath(targetUrl,moved_dirs,masterUrl);
						lf = new File(masterUrl);
						hf = new SmbFile(targetUrl,ntlmPasswordAuth);
						String t_fn=lf.getName().replace("/","");
//						String t_fp=masterUrl.replace(SMBSync_External_Root_Dir, "");
//						String t_fp=masterUrl;//.replace(localUrl, "");
						if (isFileChangedForLocalToRemote(masterUrl,lf,hf,allcopy)) {
							// copy done
							if (confirmCopy(targetUrl)) {
								long file_byte=lf.length();
								
								if (mGp.settingRemoteFileCopyByRename) {
									tmp_target=makeTempFilePath(targetUrl);
								} 
								copyFileLocalToRemote(lf,hf,file_byte,t_fn,masterUrl, tmp_target);
								if (checkErrorStatus()!=0) {
									return checkErrorStatus();
								}
								try {
									if (!syncProfileNotUseLastModifiedForRemote)
										hf.setLastModified(lf.lastModified());
								} catch(SmbException e) {
									addLogMsg("W",targetUrl,
											mGp.appContext.getString(R.string.msgs_mirror_prof_remote_file_set_last_modified_failed));
									addDebugLogMsg(1,"W",targetUrl,
											"Remote file setLastModified() failed, reason="+ e.getMessage());
								}
//								mHistoryCopiedList.add(targetUrl);
//								updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
//										masterUrl,hf.getLastModified());
								copyCount++;
								
								if (confirmDelete(masterUrl)) {
									// delete master file
									addMovedDirList(moved_dirs,lf.getParent());
									deleteLocalItem(true,masterUrl);
//									mHistoryDeletedList.add(masterUrl);
									addMsgToProgDlg(false,"I", 0, t_fn,msgs_mirror_prof_file_deleted);
								} else {
									addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
								}
							} else {
								addLogMsg("W",masterUrl,msgs_mirror_confirm_copy_cancel);								
							}
						} else {
							if (confirmDelete(masterUrl)) {
								// delete master file
								addMovedDirList(moved_dirs,lf.getParent());
								deleteLocalItem(true,masterUrl);
//								mHistoryDeletedList.add(masterUrl);
								addMsgToProgDlg(false,"I", 0, t_fn,msgs_mirror_prof_file_deleted);
							} else {
								addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
							}
						}
						if (checkErrorStatus()!=0) return checkErrorStatus();
					}
				}
			} else {
				if (mGp.debugLevel>=1) 
					addDebugLogMsg(1,"E","Local file ", masterUrl,
							" was not copied, because file/dir not existed.");
				addLogMsg("E",masterUrl,msgs_mirror_prof_master_not_found);
				tcMirror.setThreadMessage(msgs_mirror_prof_master_not_found+","+masterUrl);
				isExceptionOccured=true;
				return -1;
				
			}
		} catch (MalformedURLException e) {
			printStackTraceElement(e.getStackTrace());
			addLogMsg("E","","mirrorMoveLocalToRemote MFUE From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return setErrorStatusRetry(e.getMessage());
		} catch (SmbException e) {
			printStackTraceElement(e.getStackTrace());
			addLogMsg("E","","mirrorMoveLocalToRemote SMBE From="+masterUrl+", To="+targetUrl);
			String[] e_msg=NetworkUtil.analyzeNtStatusCode(e, mGp.appContext, 
					targetUrl,ntlmPasswordAuth.getUsername());
			addLogMsg("E","",e_msg[0]);//e.toString());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return setErrorStatusRetry(e_msg[0]);
		} catch (UnknownHostException e) {
			printStackTraceElement(e.getStackTrace());
			addLogMsg("E","","mirrorMoveLocalToRemote UKHE From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return setErrorStatusRetry(e.getMessage());
		} catch (FileNotFoundException e) {
			printStackTraceElement(e.getStackTrace());
			addLogMsg("E","","mirrorMoveLocalToRemote FNFD From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return setErrorStatusRetry(e.getMessage());
		} catch (IOException e) {
			printStackTraceElement(e.getStackTrace());
			addLogMsg("E","","mirrorMoveLocalToRemote IOE From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return setErrorStatusRetry(e.getMessage());
		}
		return copyCount;
	};

	final private int mirrorMoveLocalToLocal(boolean allcopy, String masterUrl,
			String targetUrl, String target_fp_base, ArrayList<String> moved_dirs) {
		File tf;
		File mf;
		
		if (mGp.debugLevel>=1) 
			addDebugLogMsg(2,"I","mirrorMoveLocalToLocal master=", masterUrl,
				", target=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();
		String tmp_target="";
		try {
			mf = new File(masterUrl);
			if (mf.exists()) {
				if (mf.isDirectory()) { // Directory copy
					if (mf.canRead() && !isHiddenDirectory(mf) &&
							isDirToBeProcessed(masterUrl.replace(mirrorIoRootDir, ""))
							) {
						if (syncProfileSyncEmptyDirectory) {
							createLocalDirByDir(targetUrl);
							if (checkErrorStatus()!=0) return checkErrorStatus();
						}
						File[] children = mf.listFiles();
						if (children!=null) {
							for (File element : children) {
								String tmp = element.getName();
								if (!tmp.equals(".android_secure")) {
									if (tmp.lastIndexOf("/")>0) tmp=tmp.substring(0,tmp.lastIndexOf("/"));
									String n_master=(masterUrl + "/"+ tmp).replaceAll("//", "/");
									String n_target=(targetUrl + "/" + tmp).replaceAll("//", "/");
									if (!n_master.equals(target_fp_base)) {
										if (element.isFile() ||
												(element.isDirectory() && syncProfileSyncSubDir)) {
											mirrorMoveLocalToLocal(allcopy, n_master, n_target, target_fp_base,
													moved_dirs);
										} else {
											if (mGp.debugLevel>=2) 
												addDebugLogMsg(2,"I","sub directory ignored by option, dir="+n_master);
										}
										if (checkErrorStatus()!=0) return checkErrorStatus();
									} else {
//										addLogMsg("W","",
										addDebugLogMsg(1,"W",
												String.format(msgs_mirror_same_directory_ignored,n_master));
									}
								} else {
									addDebugLogMsg(1,"I","Android system directory is ignored, fp="+element.getPath());
								}
							}
						} else {
							addDebugLogMsg(1,"I","Directory was null, dir="+mf.getPath());
						}
						
					} else {
						if (!mf.canRead()) addDebugLogMsg(1,"I","Directory ignored because can not read, fp="+masterUrl);
					}
				} else { // file copy
					if (isDirFilteredByFileName(masterUrl.replace(mirrorIoRootDir, "")) &&
							!isHiddenFile(mf) &&
							isFileFiltered(masterUrl)) {
						if (createLocalDirByFilePath(targetUrl,moved_dirs,masterUrl)) {
							mf = new File(masterUrl);
							tf = new File(targetUrl);
							String t_fn=mf.getName().replace("/","");
//							String t_fp=masterUrl.replace(SMBSync_External_Root_Dir, "");
//							String t_fp=masterUrl;//.replace(localUrl, "");
//							if (isFileChanged(masterUrl,mf,tf,allcopy)) {
							if (isFileChanged(targetUrl,tf,mf,allcopy)) {							
								// copy done
								if (confirmCopy(targetUrl)) {
									long file_byte=mf.length();
									if (mGp.settingLocalFileCopyByRename) {
										tmp_target=makeTempFilePath(targetUrl);
									}
									copyFileLocalToLocal(mf,tf,file_byte,t_fn,masterUrl,tmp_target);
									if (checkErrorStatus()!=0) return checkErrorStatus();
//									mHistoryCopiedList.add(targetUrl);
									tf.setLastModified(mf.lastModified());
									updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
											targetUrl,mf.lastModified());
									scanMediaStoreLibrary(targetUrl);
									copyCount++;
									
									if (confirmDelete(masterUrl)) {
										// delete master file
										addMovedDirList(moved_dirs,mf.getParent());
										deleteLocalItem(true,masterUrl);
//										mHistoryDeletedList.add(masterUrl);
										addMsgToProgDlg(false,"I", 0, t_fn,msgs_mirror_prof_file_deleted);
									} else {
										addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
									}
								} else {
									addLogMsg("W",masterUrl,msgs_mirror_confirm_copy_cancel);								
								}
							} else {
								if (confirmDelete(masterUrl)) {
									// delete master file
									addMovedDirList(moved_dirs,mf.getParent());
									deleteLocalItem(true,masterUrl);
//									mHistoryDeletedList.add(masterUrl);
									addMsgToProgDlg(false,"I", 0, t_fn,msgs_mirror_prof_file_deleted);
								} else {
									addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
								}
							}
						}
						if (checkErrorStatus()!=0) return checkErrorStatus();
					}
				}
			} else {
				if (mGp.debugLevel>=1) 
					addDebugLogMsg(1,"E","Local file ", masterUrl,
							" was not copied, because file/dir not existed.");
				addLogMsg("E",masterUrl,msgs_mirror_prof_master_not_found);
				tcMirror.setThreadMessage(msgs_mirror_prof_master_not_found+","+masterUrl);
				isExceptionOccured=true;
				return -1;
				
			}
//		} catch (MalformedURLException e) {
//			printStackTraceElement(e.getStackTrace());
//			addLogMsg("E","",e.getMessage());//e.toString());
//			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
//			isExceptionOccured=true;
//			tcMirror.setThreadMessage(e.getMessage());
//			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
//			return -1;
//		} catch (SmbException e) {
//			printStackTraceElement(e.getStackTrace());
//			addLogMsg("E","",e.getMessage());//e.toString());
//			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
//			isExceptionOccured=true;
//			tcMirror.setThreadMessage(e.getMessage());
//			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
//			return -1;
//		} catch (UnknownHostException e) {
//			printStackTraceElement(e.getStackTrace());
//			addLogMsg("E","",e.getMessage());//e.toString());
//			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
//			isExceptionOccured=true;
//			tcMirror.setThreadMessage(e.getMessage());
//			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
//			return -1;
		} catch (FileNotFoundException e) {
			printStackTraceElement(e.getStackTrace());
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (IOException e) {
			printStackTraceElement(e.getStackTrace());
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		}
		return copyCount;
	};

	final private void createRemoteDirByFilePath(String targetUrl,
			ArrayList<String> moved_dirs,String masterUrl) 
					throws MalformedURLException, SmbException {
		String target_dir="",master_dir="";
		if (targetUrl.lastIndexOf("/")<=0) return ;
		else target_dir=targetUrl.substring(0,targetUrl.lastIndexOf("/"));
		if (masterUrl.lastIndexOf("/")<=0) return ;
		else master_dir=masterUrl.substring(0,masterUrl.lastIndexOf("/"));

		SmbFile hf = new SmbFile(target_dir + "/",ntlmPasswordAuth);
		if (!hf.exists()) {
			createRemoteDirByDir(target_dir);
			if (moved_dirs!=null) addMovedDirList(moved_dirs,master_dir);
		}
	};

	final private void createRemoteDirByDir(String target_dir) 
					throws MalformedURLException, SmbException {
		SmbFile hf = new SmbFile(target_dir + "/",ntlmPasswordAuth);
		if (!hf.exists()) {
			hf.mkdirs();
			if (mGp.settingShowSyncDetailMessage)  
				addLogMsg("I",target_dir,msgs_mirror_prof_dir_create);
		}
	};

	final private boolean createLocalDirByFilePath(String targetUrl,
			ArrayList<String> moved_dirs,String masterUrl) {
		boolean result=false;
		String target_dir="",master_dir="";
		if (targetUrl.lastIndexOf("/")<=0) {
			addDebugLogMsg(1,"I","targetDir is root dir");
			return true;
		} else target_dir=targetUrl.substring(0,targetUrl.lastIndexOf("/"));
		if (masterUrl.lastIndexOf("/")<=0) {
			addDebugLogMsg(1,"I","masterDir is root dir");
			return true;
		} else master_dir=masterUrl.substring(0,masterUrl.lastIndexOf("/"));
		
		File lf = new File(target_dir);
		if (!lf.exists()) {
			result=createLocalDirByDir(target_dir);
			if (result) {
				if (moved_dirs!=null) addMovedDirList(moved_dirs,master_dir + "/");
			}
		} else {
//			addDebugLogMsg(1,"I","Local directory already created, dir="+target_dir);
			result=true;
		}
		
		return result;
	};

	final private boolean createLocalDirByDir(String target_dir) {
		boolean result=false;
		
		File lf = new File(target_dir);
		if (!lf.exists()) {
			if (SafUtil.isSafExternalSdcardPath(mSafCA, target_dir)) {
				SafUtil.getSafDocumentFileByPath(mSafCA, target_dir, true);
				result=true;
			} else {
				result=lf.mkdirs();
			}
			if (result) {
				if (mGp.settingShowSyncDetailMessage)  
					addLogMsg("I",target_dir,msgs_mirror_prof_dir_create);
			} else {
				addLogMsg("E",target_dir,msgs_mirror_prof_dir_create_failed+" write="+lf.canWrite());
				isExceptionOccured=true;
			}
		} else {
//			addDebugLogMsg(1,"I","Local directory already created, dir="+target_dir);
			result=true;
		}
		
		return result;
	};

	
	final private int copyFileLocalToRemote(File in_file, SmbFile out_file, 
			long file_byte, String t_fn, String t_fp, String tmp_target) throws IOException {
		
		
		long fileReadBytes = 0;
		long readBeginTime = System.currentTimeMillis();
		int bufferReadBytes=0;
		boolean out_file_exits=out_file.exists();
		SmbFile tmp_out=null;
		OutputStream out=null;
		if (!tmp_target.equals("")) {
			tmp_out=new SmbFile(tmp_target, ntlmPasswordAuth);
			out=tmp_out.getOutputStream();
		} else {
			out=out_file.getOutputStream();
		}
		FileInputStream in=new FileInputStream(in_file);
//		BufferedInputStream in=new BufferedInputStream(fis,4096*512);
//		BufferedOutputStream out=new BufferedOutputStream(out_str,4096*16*32);
		byte[] io_buffer;
		if (syncProfileSyncUseRemoteSmallIoArea) {
			io_buffer=new byte[1024*16];
		} else {
			io_buffer=mirrorIoBuffer;
		}
//		Log.v("","io size="+io_buffer.length);
		while ((bufferReadBytes = in.read(io_buffer)) > 0) {
			out.write(io_buffer, 0, bufferReadBytes);
			fileReadBytes += bufferReadBytes;
			if (file_byte>fileReadBytes) {
				addMsgToProgDlg(false,"I",0, t_fn,
					String.format(msgs_mirror_prof_file_copying,(fileReadBytes*100)/file_byte));
			}
			if (!tcMirror.isEnabled()) {
				in.close();
				out.flush();
				out.close();
				if (tmp_out!=null && tmp_out.exists()) tmp_out.delete();
				return -10;
			}
		}
		long readElapsedTime = System.currentTimeMillis() - readBeginTime;
		in.close();
		out.flush();
		out.close();
		
		if (tmp_out!=null) {
			if (out_file.exists()) out_file.delete();
			tmp_out.renameTo(out_file);
		}
		
		String tmsg="";
		if (out_file_exits) tmsg=msgs_mirror_prof_file_replaced;
		else tmsg=msgs_mirror_prof_file_copied;
		addMsgToProgDlg(false,"I",0,t_fn,tmsg);
		if (mGp.settingShowSyncDetailMessage) addLogMsg("I",t_fp,tmsg);

		totalTransferByte+=fileReadBytes;
		totalTransferTime+=readElapsedTime;
		if (mGp.debugLevel>=1) 
			addDebugLogMsg(1,"I",t_fp+" "+fileReadBytes + " bytes transfered in ",
				readElapsedTime+" mili seconds at "+calTransferRate(fileReadBytes,readElapsedTime));
		return 0;
	};

	final private int copyFileLocalToLocal(File in_file, File out_file,
			long file_byte, String t_fn, String t_fp, String tmp_target) throws IOException {
		int result=0;
		if (SafUtil.isSafExternalSdcardPath(mSafCA, out_file.getPath())) {
			result=copySafFileLocalToLocal(in_file, out_file,
					file_byte, t_fn, t_fp, tmp_target);
		} else {
			result=copyFileLocalToLocalByApi(in_file, out_file,
					file_byte, t_fn, t_fp, tmp_target);
		}
		return result;
	}

	final private int copySafFileLocalToLocal(File in_file, File out_file,
			long file_byte, String t_fn, String t_fp, String tmp_target) throws IOException {
		long fileReadBytes = 0;
		long readBeginTime = System.currentTimeMillis();
		int bufferReadBytes=0;
		boolean out_file_exits=out_file.exists();
		FileInputStream in=new FileInputStream(in_file);
		OutputStream out=null;
		SafFile t_df=null, o_df=null;

		if (!tmp_target.equals("")) {
			t_df=SafUtil.getSafDocumentFileByPath(mSafCA, tmp_target, false);
			o_df=SafUtil.getSafDocumentFileByPath(mSafCA, out_file.getPath(), false);
			if (mGp.debugLevel>=1) addDebugLogMsg(1,"I","","t_df="+t_df+", o_df="+o_df+", temp="+tmp_target+", o_path="+out_file.getPath());
			out=mGp.appContext.getContentResolver().openOutputStream(t_df.getUri());
		} else {
			o_df=SafUtil.getSafDocumentFileByPath(mSafCA, out_file.getPath(), false);
			if (mGp.debugLevel>=1) addDebugLogMsg(1,"I","","o_df="+o_df+", o_path="+out_file.getPath());
			out=mGp.appContext.getContentResolver().openOutputStream(o_df.getUri());
		}
		
//		BufferedInputStream in=new BufferedInputStream(fis,4096*512);
//		BufferedOutputStream out=new BufferedOutputStream(fos,4096*512);
		while ((bufferReadBytes = in.read(mirrorIoBuffer)) > 0) {
			out.write(mirrorIoBuffer, 0, bufferReadBytes);
			fileReadBytes += bufferReadBytes;
			if (file_byte>fileReadBytes) {
				addMsgToProgDlg(false,"I",0, t_fn,
					String.format(msgs_mirror_prof_file_copying,(fileReadBytes*100)/file_byte));
			}
			if (!tcMirror.isEnabled()) {
				in.close();
				out.flush();
				out.close();
				if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
				return -10;
			}
		}
		long readElapsedTime = System.currentTimeMillis() - readBeginTime;
		if (readElapsedTime==0) readElapsedTime=1;
		in.close();
		out.flush();
		out.close();
		
		if (t_df!=null) {
			if (o_df.exists()) o_df.delete();
			t_df.renameTo(t_fn);
		}
		String tmsg="";
		if (out_file_exits) tmsg=msgs_mirror_prof_file_replaced;
		else tmsg=msgs_mirror_prof_file_copied;
		addMsgToProgDlg(false,"I",0, t_fn,tmsg);
		if (mGp.settingShowSyncDetailMessage) addLogMsg("I",t_fp,tmsg);
		totalTransferByte+=fileReadBytes;
		totalTransferTime+=readElapsedTime;
		if (mGp.debugLevel>=1) 
			addDebugLogMsg(1,"I",t_fp+" "+fileReadBytes + " bytes transfered in ",
				readElapsedTime+" mili seconds at "+calTransferRate(fileReadBytes,readElapsedTime));
		return 0;
	};

	final private int copyFileLocalToLocalByApi(File in_file, File out_file,
			long file_byte, String t_fn, String t_fp, String tmp_target) throws IOException {
		long fileReadBytes = 0;
		long readBeginTime = System.currentTimeMillis();
		int bufferReadBytes=0;
		boolean out_file_exits=out_file.exists();
		FileInputStream in=new FileInputStream(in_file);
		FileOutputStream out=null;
		File tmp_out=null;
		if (!tmp_target.equals("")) {
			tmp_out=new File(tmp_target);	
			out=new FileOutputStream(tmp_out);
		} else {
			out=new FileOutputStream(out_file);
		}
		
//		BufferedInputStream in=new BufferedInputStream(fis,4096*512);
//		BufferedOutputStream out=new BufferedOutputStream(fos,4096*512);
		while ((bufferReadBytes = in.read(mirrorIoBuffer)) > 0) {
			out.write(mirrorIoBuffer, 0, bufferReadBytes);
			fileReadBytes += bufferReadBytes;
			if (file_byte>fileReadBytes) {
				addMsgToProgDlg(false,"I",0, t_fn,
					String.format(msgs_mirror_prof_file_copying,(fileReadBytes*100)/file_byte));
			}
			if (!tcMirror.isEnabled()) {
				in.close();
				out.flush();
				out.close();
				if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
				return -10;
			}
		}
		long readElapsedTime = System.currentTimeMillis() - readBeginTime;
		if (readElapsedTime==0) readElapsedTime=1;
		in.close();
		out.flush();
		out.close();
		
		if (tmp_out!=null) {
			tmp_out.setLastModified(in_file.lastModified());
			if (out_file.exists()) out_file.delete();
			tmp_out.renameTo(out_file);
		}

		String tmsg="";
		if (out_file_exits) tmsg=msgs_mirror_prof_file_replaced;
		else tmsg=msgs_mirror_prof_file_copied;
		addMsgToProgDlg(false,"I",0, t_fn,tmsg);
		if (mGp.settingShowSyncDetailMessage) addLogMsg("I",t_fp,tmsg);
		totalTransferByte+=fileReadBytes;
		totalTransferTime+=readElapsedTime;
		if (mGp.debugLevel>=1) 
			addDebugLogMsg(1,"I",t_fp+" "+fileReadBytes + " bytes transfered in ",
				readElapsedTime+" mili seconds at "+calTransferRate(fileReadBytes,readElapsedTime));
		return 0;
	};

	final private int copyFileRemoteToLocal(SmbFile in_file, File out_file, 
			long file_byte, String t_fn, String t_fp, String tmp_target) throws IOException {
		int result=0;
		if (SafUtil.isSafExternalSdcardPath(mSafCA, out_file.getPath())) {
			result=copySafFileRemoteToLocal(in_file, out_file, 
					file_byte, t_fn, t_fp, tmp_target);
		} else {
			result=copyFileRemoteToLocalByApi(in_file, out_file, 
					file_byte, t_fn, t_fp, tmp_target);
		}
		return result;
	};

	final private int copySafFileRemoteToLocal(SmbFile in_file, File out_file, 
			long file_byte, String t_fn, String t_fp, String tmp_target) throws IOException {
		long readBeginTime = System.currentTimeMillis();
		long fileReadBytes = 0;
		int bufferReadBytes=0;
		boolean out_file_exits=out_file.exists();
		SmbFileInputStream in=new SmbFileInputStream(in_file);
		OutputStream out=null;
		SafFile t_df=null, o_df=null;
		if (!tmp_target.equals("")) {
			t_df=SafUtil.getSafDocumentFileByPath(mSafCA, tmp_target, false);
			out=mGp.appContext.getContentResolver().openOutputStream(t_df.getUri());
		} else {
			o_df=SafUtil.getSafDocumentFileByPath(mSafCA, out_file.getPath(), false);
			out=mGp.appContext.getContentResolver().openOutputStream(o_df.getUri());
		}

//		BufferedInputStream in=new BufferedInputStream(fis,4096*512);
//		BufferedOutputStream out=new BufferedOutputStream(fos,4096*512);
		while ((bufferReadBytes = in.read(mirrorIoBuffer)) > 0) {
			out.write(mirrorIoBuffer, 0, bufferReadBytes);
			fileReadBytes += bufferReadBytes;
			if (file_byte>fileReadBytes) {
				addMsgToProgDlg(false,"I",0,t_fn,
					String.format(msgs_mirror_prof_file_copying,(fileReadBytes*100)/file_byte));
			}
			if (!tcMirror.isEnabled()) {
				in.close();
				out.flush();
				out.close();
				if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
				return -10;
			}
		}
		long readElapsedTime = System.currentTimeMillis() - readBeginTime;
		in.close();
		out.flush();
		out.close();
		
		if (t_df!=null) {
			if (out_file.exists()) o_df.delete();
			t_df.renameTo(t_fn);
		}

		String tmsg="";
		if (out_file_exits) tmsg=msgs_mirror_prof_file_replaced;
		else tmsg=msgs_mirror_prof_file_copied;
		addMsgToProgDlg(false,"I",0,t_fn,tmsg);
		if (mGp.settingShowSyncDetailMessage) addLogMsg("I",t_fp,tmsg);

		if (mGp.debugLevel>=1) 
			addDebugLogMsg(1,"I",t_fp+" "+fileReadBytes + " bytes transfered in "
					+ readElapsedTime + " mili seconds at "+ 
					calTransferRate(fileReadBytes,readElapsedTime));
		totalTransferByte+=fileReadBytes;
		totalTransferTime+=readElapsedTime;

		return 0;
	};

	final private int copyFileRemoteToLocalByApi(SmbFile in_file, File out_file, 
			long file_byte, String t_fn, String t_fp, String tmp_target) throws IOException {
		long readBeginTime = System.currentTimeMillis();
		long fileReadBytes = 0;
		int bufferReadBytes=0;
		boolean out_file_exits=out_file.exists();
		SmbFileInputStream in=new SmbFileInputStream(in_file);
		FileOutputStream out=null;
		File tmp_out=null;
		if (!tmp_target.equals("")) {
			tmp_out=new File(tmp_target);	
			out=new FileOutputStream(tmp_out);
		} else {
			out=new FileOutputStream(out_file);
		}
		
//		BufferedInputStream in=new BufferedInputStream(fis,4096*512);
//		BufferedOutputStream out=new BufferedOutputStream(fos,4096*512);
		while ((bufferReadBytes = in.read(mirrorIoBuffer)) > 0) {
			out.write(mirrorIoBuffer, 0, bufferReadBytes);
			fileReadBytes += bufferReadBytes;
			if (file_byte>fileReadBytes) {
				addMsgToProgDlg(false,"I",0,t_fn,
					String.format(msgs_mirror_prof_file_copying,(fileReadBytes*100)/file_byte));
			}
			if (!tcMirror.isEnabled()) {
				in.close();
				out.flush();
				out.close();
				if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
				return -10;
			}
		}
		long readElapsedTime = System.currentTimeMillis() - readBeginTime;
		in.close();
		out.flush();
		out.close();
		
		if (tmp_out!=null) {
			if (out_file.exists()) out_file.delete();
			tmp_out.renameTo(out_file);
		}

		String tmsg="";
		if (out_file_exits) tmsg=msgs_mirror_prof_file_replaced;
		else tmsg=msgs_mirror_prof_file_copied;
		addMsgToProgDlg(false,"I",0,t_fn,tmsg);
		if (mGp.settingShowSyncDetailMessage) addLogMsg("I",t_fp,tmsg);

		if (mGp.debugLevel>=1) 
			addDebugLogMsg(1,"I",t_fp+" "+fileReadBytes + " bytes transfered in "
					+ readElapsedTime + " mili seconds at "+ 
					calTransferRate(fileReadBytes,readElapsedTime));
		totalTransferByte+=fileReadBytes;
		totalTransferTime+=readElapsedTime;

		return 0;
	};

	static final private void addMovedDirList(ArrayList<String> moved_dirs,String path) {
		if (moved_dirs.size()!=0) {
			boolean found=false;
			for (int i=0;i<moved_dirs.size();i++) {
				if (moved_dirs.get(i).equals(path)) found=true;
			}
			if (!found) {
				moved_dirs.add(path);
			}
		} else {
			moved_dirs.add(path);
		}
	};
	
	final private void compileFilter(ArrayList<String> ff, ArrayList<String> df) {
		int flags = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE;
		String ffinc="", ffexc="",dfinc="",dfexc="";
		if (ff.size()!=0) {
			String prefix, filter, cni="", cne="";
			for (int j=0;j<ff.size();j++) {
				prefix=ff.get(j).substring(0,1);
				filter=ff.get(j).substring(1,ff.get(j).length());
				
				String pre_str="";
				if (!filter.startsWith("*")) pre_str="^";
				if (prefix.equals("I")) {
					ffinc = ffinc+cni+pre_str+MiscUtil.convertRegExp(filter);
					cni="|";
				} else {
					ffexc = ffexc+cne+pre_str+MiscUtil.convertRegExp(filter);
					cne="|";
				}
			}
		}
		dirIncludeFilterList.clear();
		dirExcludeFilterList.clear();
		if (df.size()!=0) {
			String prefix, filter, cni="", cne="";
			for (int j=0;j<df.size();j++) {
				prefix=df.get(j).substring(0,1);
//				filter=mirrorIoRootDir+
				filter=df.get(j).substring(1,df.get(j).length());
				createDirFilterList(prefix,filter);
				String pre_str="", suf_str="/";
				if (!filter.startsWith("*")) pre_str="^";
//				if (!filter.endsWith("*")) suf_str="$";
				if (prefix.equals("I")) {
					dfinc = dfinc+cni+pre_str+MiscUtil.convertRegExp(filter)+suf_str;
					cni="|";
				} else {
					dfexc = dfexc+cne+pre_str+MiscUtil.convertRegExp(filter)+suf_str;
					cne="|";
				}
			}
		}

		fileFilterInclude = fileFilterExclude = null;
		dirFilterInclude = dirFilterExclude = null;
		if (ffinc.length() != 0) fileFilterInclude = Pattern.compile("(" + ffinc + ")", flags);
		if (ffexc.length() != 0) fileFilterExclude = Pattern.compile("(" + ffexc + ")", flags);
		if (dfinc.length() != 0) dirFilterInclude = Pattern.compile("(" + dfinc + ")", flags);
		if (dfexc.length() != 0) dirFilterExclude = Pattern.compile("(" + dfexc + ")", flags);

	};
	
	final private void createDirFilterList(String prefix, String filter) {
		int flags = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE;
//		String[] filter_array=filter.split("/");
		String[] filter_array=null;
		if (filter.startsWith("/")) filter_array=filter.replaceFirst("/", "").split("/"); 
		else filter_array=filter.split("/");
		
		Pattern[] pattern_array=new Pattern[filter_array.length];
		
		for (int k=0;k<filter_array.length;k++)
			pattern_array[k] = 
				Pattern.compile("^"+MiscUtil.convertRegExp(filter_array[k])+"$", flags);
		
//		boolean[] pattern_notreg=new boolean[pattern_array.length];
//		
//		for (int k=0;k<pattern_array.length;k++){ 
//			if (pattern_array[k].toString().equals(filter_array[k])) 
//				 pattern_notreg[k]=true;  //non regular expression
//			else pattern_notreg[k]=false; //regular expression
//		}
		
		if (prefix.equals("I")) {
			dirIncludeFilterList.add(pattern_array);
		} else {
//			Log.v("","filter="+filter+", conv="+convertRegExp(filter)+
//					", comp="+Pattern.compile(convertRegExp(filter), flags));
			dirExcludeFilterList.add(
					Pattern.compile(MiscUtil.convertRegExp(filter), flags));
		}
	};
	
	final private boolean isFileChanged(String fp, File lf, SmbFile hf, boolean ac) 
			throws SmbException {
		long hf_time=0, hf_length=0;
		boolean hf_exists = hf.exists();
		
		if (hf_exists) {
			hf_time=hf.lastModified();
			hf_length=hf.length();
		}
		return isFileChangedDetailCompare(fp, lf, 
				hf_exists, hf_time, hf_length, ac);
	};

	final private boolean isFileChanged(String fp, File mf, File tf, boolean ac) 
			throws SmbException {
		long tf_time=0, tf_length=0;
		boolean tf_exists = tf.exists();
		
		if (tf_exists) {
			tf_time=tf.lastModified();
			tf_length=tf.length();
		}
		return isFileChangedDetailCompare(fp, mf, 
				tf_exists, tf_time, tf_length, ac);
	};

	final private boolean isFileChangedDetailCompare(String fp, File lf, 
			boolean hf_exists, long hf_time, long hf_length, 
			boolean ac) 
			throws SmbException {
		boolean diff=false;
		long lf_time=0, lf_length=0;
		boolean lf_exists = lf.exists();
		boolean exists_diff=false;
		
		if (lf_exists) {
			lf_time=lf.lastModified();
			lf_length=lf.length();
		}
		long time_diff=Math.abs((hf_time-lf_time));
		long length_diff=Math.abs((hf_length-lf_length));
		long time_diff_tz1=Math.abs(hf_time-(lf_time-timeZone));
//		long diff_tz_2=Math.abs(hf_time-(lf_time-(timeZone*2)));

		if (hf_exists!=lf_exists) exists_diff=true;
		if (exists_diff || length_diff>0 || ac) {
			if (!syncProfileUseJavaLastModified) {//Use lastModified
				if (lf_exists) {
					updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
							lf.getPath(), hf_time); 
				} else {
					boolean updated=
							updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
									lf.getPath(), hf_time);
					if (!updated)
						addLastModifiedItem(currentFileLastModifiedList,newFileLastModifiedList,
								lf.getPath(), lf_time,hf_time); 
				}
			}
			diff=true;
		} else {//Check lastModified()
			if (syncProfileUseJavaLastModified) {//Use lastModified
				if (time_diff>timeDifferenceLimit) { //LastModified was changed
					if (isMediaStoreDir(lf.getParent())) {//MediaStore Directory
						if (settingsMediaStoreUseLastModTime.equals("0")) diff=true;// 
						else {
							if (settingsMediaStoreUseLastModTime.equals("1")) {
							//Check lastModified and TimeZone
								if (time_diff_tz1>timeDifferenceLimit) {
									diff=true;
									if (mGp.debugLevel>=1) 
										addDebugLogMsg(1,"W",
												"TimeZone does not matched. different(ms) time="+
												time_diff);
								} else diff=false;
							} else {// Ignore lastModified time
								diff=false;
//								mHistoryIgnoredList.add(fp);
								ignoreCount++;
								if (!isMediaStoreChangeWarningIssued) {
									addLogMsg("W","",msgs_mirror_prof_file_bypass_media_store_change);
									isMediaStoreChangeWarningIssued=true;
								}
								if (mGp.debugLevel>=1) 
									addDebugLogMsg(1,"W",
										"Was ignored the difference between the last ",
										"update time of local and remote files.",
										" time_diff="+time_diff+", fn=",fp);
							}
						}
					} else {//not MediaStore file, lastModified was changed ->do copy
						diff=true;
					}
				} else diff=false;
			} else {//Use Filelist
				String lfp=lf.getPath();
				diff=isLocalFileLastModifiedWasDifferent(
						currentFileLastModifiedList,
						newFileLastModifiedList,
						lfp, lf_time,hf_time); 
//				Log.v("","lfp="+lfp+", lf_time="+lf_time+", hf_time="+hf_time);
			}
		}
		if (mGp.debugLevel>=3) { 
			addDebugLogMsg(3,"I","isFileChangedDetailCompare");
			if (hf_exists) addDebugLogMsg(3,"I","Remote file length="+hf_length+
						", last modified(ms)="+hf_time+
						", date="+StringUtil.convDateTimeTo_YearMonthDayHourMinSec((hf_time/1000)*1000));
			else addDebugLogMsg(3,"I","Remote file was not exists");
			if (lf_exists) addDebugLogMsg(3,"I","Local  file length="+lf_length+
					", last modified(ms)="+lf_time+
					", date="+StringUtil.convDateTimeTo_YearMonthDayHourMinSec((lf_time/1000)*1000));
			else addDebugLogMsg(3,"I","Local  file was not exists");
			addDebugLogMsg(3,"I","allcopy="+ac+",exists_diff="+exists_diff+
					",time_diff="+time_diff+", time_zone_diff="+time_diff_tz1+
					",length_diff="+length_diff+", diff="+diff);
		}
		return diff;
	};

	final private boolean isFileChangedForLocalToRemote(
			String fp, File lf, SmbFile hf, boolean ac) 
			throws SmbException {
		boolean diff=false;
		long hf_time=0, hf_length=0;
		boolean hf_exists = hf.exists();
		
		if (hf_exists) {
			hf_time=hf.lastModified();
			hf_length=hf.length();
		}
		long lf_time=0, lf_length=0;
		boolean lf_exists = lf.exists();
		boolean exists_diff=false;
		
		if (lf_exists) {
			lf_time=lf.lastModified();
			lf_length=lf.length();
		}
		long time_diff=Math.abs((hf_time-lf_time));
		long length_diff=Math.abs((hf_length-lf_length));
		long time_diff_tz1=Math.abs(hf_time-(lf_time-timeZone));
//		long diff_tz_2=Math.abs(hf_time-(lf_time-(timeZone*2)));

		if (hf_exists!=lf_exists) exists_diff=true;
		if (exists_diff || length_diff>0 || ac) {
			diff=true;
		} else {//Check lastModified()
			if (!syncProfileNotUseLastModifiedForRemote) {
				if (time_diff>timeDifferenceLimit) { //LastModified was changed
					diff=true;
				} else diff=false;
			}
		}
		if (mGp.debugLevel>=3) { 
			addDebugLogMsg(3,"I","isFileChangedForLocalToRemote");
			if (hf_exists) addDebugLogMsg(3,"I","Remote file length="+hf_length+
						", last modified(ms)="+hf_time+
						", date="+StringUtil.convDateTimeTo_YearMonthDayHourMinSec((hf_time/1000)*1000));
			else addDebugLogMsg(3,"I","Remote file was not exists");
			if (lf_exists) addDebugLogMsg(3,"I","Local  file length="+lf_length+
					", last modified(ms)="+lf_time+
					", date="+StringUtil.convDateTimeTo_YearMonthDayHourMinSec((lf_time/1000)*1000));
			else addDebugLogMsg(3,"I","Local  file was not exists");
			addDebugLogMsg(3,"I","allcopy="+ac+",exists_diff="+exists_diff+
					",time_diff="+time_diff+", time_zone_diff="+time_diff_tz1+
					",length_diff="+length_diff+", diff="+diff);
		}
		return diff;
	};

	final private boolean isLocalFileLastModifiedWasDifferent(
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list,
			String fp, long l_lm, long r_lm) {
		boolean result=LocalFileLastModified.isCurrentListWasDifferent(
				curr_last_modified_list, new_last_modified_list,
				fp,l_lm,r_lm,timeDifferenceLimit);
		if (mGp.debugLevel>=3) 
			addDebugLogMsg(3,"I","isLocalFileLastModifiedWasDifferent result="+result+", item="+fp);
		return result;
	};
	
	final private void deleteLocalFileLastModifiedEntry(
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list,
			String fp){
		LocalFileLastModified.deleteLastModifiedItem(
				curr_last_modified_list, new_last_modified_list, fp);
		if (mGp.debugLevel>=3) 
			addDebugLogMsg(3,"I","deleteLocalFileLastModifiedEntry entry="+fp);

	};
	
	final private boolean updateLocalFileLastModifiedList(
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list,
			String targetUrl, long r_lm) {
		return LocalFileLastModified.updateLastModifiedList(
				curr_last_modified_list, new_last_modified_list, targetUrl, r_lm);
	};
	
	final private void addLastModifiedItem(
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list,
			String targetUrl, long l_lm, long r_lm) {
		LocalFileLastModified.addLastModifiedItem(
				curr_last_modified_list, new_last_modified_list, targetUrl, l_lm, r_lm);
		if (mGp.debugLevel>=3) 
			addDebugLogMsg(3,"I","addLastModifiedItem entry="+targetUrl);
	};
	
	final private boolean isSetLastModifiedFunctional(String lmp) {
		boolean result=
				LocalFileLastModified.isSetLastModifiedFunctional(lmp);
		if (mGp.debugLevel>=1) 
			addDebugLogMsg(1,"I","isSetLastModifiedFunctional result="+result+", lmp="+lmp);
		return result;
	};

	final private void saveLocalFileLastModifiedList(String lmp) {
		if (syncProfileUseJavaLastModified) return;
		if (mGp.debugLevel>=1) 
			addDebugLogMsg(1,"I","saveLocalFileLastModifiedList mp="+lmp+", curr size="+
					currentFileLastModifiedList.size()+", add size="+newFileLastModifiedList.size());
		LocalFileLastModified.saveLastModifiedList(
				lmp, currentFileLastModifiedList, newFileLastModifiedList);
	};
	
	final private void saveLocalFileLastModifiedListCache(String lmp) {
		if (lmp.equals("")) return; 
		boolean found=false;
		int idx=0;
		for (int i=0;i<mLocalFileLastModifiedCache.size();i++) {
			if (mLocalFileLastModifiedCache.get(i).mount_point_name.equals(lmp)) {
				found=true;
				idx=i;
				break;
			}
		}
		LocalFileLastModifiedListCacheItem cli=null;
		if (found) {
			cli=mLocalFileLastModifiedCache.get(idx);
			cli.cureent_list.clear();
			cli.cureent_list.addAll(currentFileLastModifiedList);
			cli.new_list.clear();
			cli.new_list.addAll(newFileLastModifiedList);
			cli.mount_point_name=lmp;
		} else {
			cli=new LocalFileLastModifiedListCacheItem();
			cli.mount_point_name=lmp;
			cli.cureent_list.addAll(currentFileLastModifiedList);
			cli.new_list.addAll(newFileLastModifiedList);
			mLocalFileLastModifiedCache.add(cli);
		}
	};
	
	final private void loadLocalFileLastModifiedList(String lmp) {
		if (syncProfileUseJavaLastModified) return;
		if (!loadedLocalMountPoint.equals(lmp)) {
			saveLocalFileLastModifiedListCache(loadedLocalMountPoint);
			boolean hit=false;
			for (int i=0;i<mLocalFileLastModifiedCache.size();i++) {
				if (mLocalFileLastModifiedCache.get(i).mount_point_name.equals(lmp)) {
					hit=true;
					currentFileLastModifiedList.clear();
					currentFileLastModifiedList.addAll(mLocalFileLastModifiedCache.get(i).cureent_list);
					newFileLastModifiedList.clear();
					newFileLastModifiedList.addAll(mLocalFileLastModifiedCache.get(i).new_list);
					if (mGp.debugLevel>=1) 
						addDebugLogMsg(1,"I","loadLocalFileLastModifiedList cache hit. mp="+lmp);
					break;
				}
			}
			if (!hit) {
				LocalFileLastModified.loadLastModifiedList(
					lmp, currentFileLastModifiedList, newFileLastModifiedList);
				if (mGp.debugLevel>=1) 
					addDebugLogMsg(1,"I","loadLocalFileLastModifiedList list loaded. mp="+lmp);
			}
			loadedLocalMountPoint=lmp;
			if (mGp.debugLevel>=1) 
				addDebugLogMsg(1,"I","loadLocalFileLastModifiedList mp="+lmp+", list size current="+
						currentFileLastModifiedList.size()+", added="+newFileLastModifiedList.size());		
		} else {
			if (mGp.debugLevel>=1) 
				addDebugLogMsg(1,"I","loadLocalFileLastModifiedList already loaded. mp="+lmp);
		}
	};
	
	final private boolean isMediaStoreDir(String path) {
		boolean found=false;
		if(Build.VERSION.SDK_INT >= 11) {
			//android 3.1 以上は常にMediaScanを行う
			found=true;
		} else {
			if (mediaStoreImageList.size()!=0) {
				if (Collections.binarySearch(mediaStoreImageList, path)>=0) 
					found=true;
			}
			if (!found && mediaStoreAudioList.size()!=0) {
				if (Collections.binarySearch(mediaStoreAudioList, path)>=0) 
					found=true;
			}
			if (!found && mediaStoreVideoList.size()!=0) {
				if (Collections.binarySearch(mediaStoreVideoList, path)>=0) 
					found=true;
			}
		}
		if (mGp.debugLevel>=2) 
			addDebugLogMsg(2,"I","isMediaStoreDir="+found+",dir="+path);
		return found;
	};

	final private boolean isFileFiltered(String url) {
		boolean filtered = false;
		Matcher mt;
		
		if (!syncMasterDirFileProcess) {//「root直下のファイルは処理しないオプション」が有郊
			String tmp_d="";
			if (mirrorIoRootDir.endsWith("/")) tmp_d=url.replace(mirrorIoRootDir, "");
			else tmp_d=url.replace(mirrorIoRootDir+"/", "");
//			Log.v("","tmp_d="+tmp_d+", r="+mirrorIoRootDir);
			if (tmp_d.indexOf("/")<0) {
				//root直下なので処理しない
				if (mGp.debugLevel>=3) 
					addDebugLogMsg(3,"I","Filefilter not filtered, " +
							"because Master Dir not processed was effective");
				return false;
			}
		}
		
		String temp_fid = url.substring(url.lastIndexOf("/") + 1, url.length());
//		Log.v("","t="+temp_fid+", url="+url+", pattern="+fileFilterInclude);
		if (fileFilterInclude == null) {
			// nothing filter
			filtered = true;
		} else {
			mt = fileFilterInclude.matcher(temp_fid);
			if (mt.find()) filtered = true;
			if (mGp.debugLevel>=3) 
				addDebugLogMsg(3,"I","Filefilter Include result:"+filtered);
		}
		if (fileFilterExclude==null) {
			//nop
		} else {
			mt = fileFilterExclude.matcher(temp_fid);
			if (mt.find()) filtered=false;
			if (mGp.debugLevel>=3) 
				addDebugLogMsg(3,"I","Filefilter Exclude result:"+filtered);
		}
		if (mGp.debugLevel>=3) 
			addDebugLogMsg(3,"I","Filefilter result:"+filtered);
		return filtered;
	};

	final private boolean isDirFilteredByFileName(String f_dir_name) {
		
		String n_fp="";
		String t_dir=f_dir_name;
		String n_dir="";
		if (f_dir_name.startsWith("/")) t_dir=f_dir_name.substring(1); 
		if (t_dir.endsWith("/")) n_fp=t_dir.substring(0, t_dir.length());
		else n_fp=t_dir;
		
		if (n_fp.lastIndexOf("/")>0) n_dir=n_fp.substring(0,n_fp.lastIndexOf("/"));
//		Log.v("","by file f_dir="+f_dir_name+", n_dir="+n_dir+", t_dir="+t_dir);
		boolean result=isDirFilteredByDirName(n_dir);
//		if (!result) {
//			Thread.dumpStack();
//		}
		return result;
	};

	final private boolean isDirFilteredByDirName(String f_dir) {
		boolean filtered = false;
		Matcher mt;
		
		String t_dir=f_dir;
		String n_dir="";
		if (f_dir.startsWith("/")) t_dir=f_dir.substring(1); 
		if (!t_dir.endsWith("/")) n_dir=t_dir+"/";
		else n_dir=t_dir;
		
//		Log.v("","by dir f_dir="+f_dir+", n_dir="+n_dir+", t_dir="+t_dir);
		if (n_dir.equals("/")) {
			//not filtered
			filtered = true;
		} else {
			if (mGp.debugLevel>=2)  {
				addDebugLogMsg(2,"I","Dirfilter dir="+n_dir+", Include filter="+dirFilterInclude+
						", Exclude filter="+dirFilterExclude);
			}

			if (dirFilterInclude == null) {
				// nothing filter
				filtered = true;
			} else {
				mt = dirFilterInclude.matcher(n_dir);
				if (mt.find()) {
					filtered = true;
				}
				if (mGp.debugLevel>=2) 
					addDebugLogMsg(2,"I","Dirfilter Include result:"+filtered);
			}
			if (dirFilterExclude==null) {
				//nop
			} else {
				mt = dirFilterExclude.matcher(n_dir);
				if (mt.find()) {
					filtered=false;
				}
				if (mGp.debugLevel>=2) 
					addDebugLogMsg(2,"I","Dirfilter Exclude result:"+filtered);
			}
			if (mGp.debugLevel>=2) 
				addDebugLogMsg(2,"I","Dirfilter result:"+filtered);
		}
		return filtered;
	};

	final private boolean isDirExcluded(String fp) {
		boolean result=false;
		
		Matcher mt;
		
		if (dirFilterExclude==null) {
			//nop
		} else {
			mt = dirFilterExclude.matcher(fp);
			if (mt.find()) {
				result=true;
			}
		}
		if (mGp.debugLevel>=2) addDebugLogMsg(2,"I","isDirExcluded result:"+result);
		
		return result;
	}

	final private boolean isDirToBeProcessed(String dir) {
		boolean inc=false, exc=false, result=false;
		
		String filter_dir;
		if (dir.length()!=0) {
			if (dir.endsWith("/")) filter_dir=dir.substring(0,dir.length()-1);
			else filter_dir=dir;
			String[] dir_array=null;
			if (filter_dir.startsWith("/")) dir_array=filter_dir.replaceFirst("/", "").split("/");
			else dir_array=filter_dir.split("/");

			if (dirIncludeFilterList.size()==0) inc=true;
			else {
				for (int i=0;i<dirIncludeFilterList.size();i++) {
					Pattern[] pattern_array=dirIncludeFilterList.get(i);
					boolean found=true;
					for (int j=0;j<Math.min(dir_array.length,pattern_array.length);j++) {
						Matcher mt = pattern_array[j].matcher(dir_array[j]);
						if (dir_array[j].length()!=0) {
							found=mt.find();
							if (!found) {
								break;
							} 
						}
					}
					if (found) {
						inc=true;
						break;
					}
				}
			}
			if (dirExcludeFilterList.size()==0) exc=false;
			else {
				exc=false;
				for (int i=0;i<dirExcludeFilterList.size();i++) {
					Pattern filter_pattern=dirExcludeFilterList.get(i);
					Matcher mt = filter_pattern.matcher(filter_dir);
					if (mt.find()) {
						exc=true;
						break;
					}
					if (exc) break;
				}
			}
			
			if (exc) result=false;
				else if (inc) result=true;
					else result=false;
		} else {
			result=true;
			inc=exc=false;
		}
		if (mGp.debugLevel>=2) 
			addDebugLogMsg(2,"I","isDirectoryToBeProcessed"+
				" include="+inc+", exclude="+exc+", result="+result+", dir="+dir);
		return result;
	};

	final private void notifyThreadTerminate() {
		LogUtil.flushLogFile(mGp);
		notifyEvent.notifyToListener(true, null);
	};

	final private int deleteRemoteItem(boolean deldir, String url) {
		SmbFile sf;

		if (mGp.debugLevel>=1) 
			addDebugLogMsg(2,"I","deleteRemoteItem=" + url);

		try {
			sf = new SmbFile(url,ntlmPasswordAuth);
			if (deldir) { 
				deleteRemoteFile("", sf); //delete specified dir
			} else { 
				deleteCount=deleteRemoteFile(sf.getPath(), sf); //not delete specified dir
			}
			

		} catch (MalformedURLException e) {
			addLogMsg("E","","deleteRemoteFile MFUE url="+url);
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","url="+url);
			printStackTraceElement(e.getStackTrace());
			return setErrorStatusRetry(e.getMessage());
//			isExceptionOccured=true;
//			tcMirror.setThreadMessage(e.getMessage());
//			return -1;
		}
		return deleteCount;
	};

	final private int deleteRemoteFile(String rootpath, SmbFile hf) {
		try {
			if (hf.isDirectory()) {// ディレクトリの場合
				// ディレクトリにあるすべてのファイルを処理する
				String[] children = (new SmbFile(hf.getPath()+"/",ntlmPasswordAuth)).list();
				for (int i = 0; i < children.length; i++) {
					exceptionRetryCount=0;
					while(true) {
						deleteRemoteFile(rootpath, (new SmbFile(hf.getPath()+"/"+children[i],ntlmPasswordAuth)));
						if (!checkRetry()) break;
					}
					if (checkErrorStatus()!=0) return checkErrorStatus();
				}
			}
			// 削除
			if (rootpath.equals(hf.getPath())) {
				//root dirなので削除しない
			} else {
				String t_dir=hf.getPath();
				String t_fn=hf.getName().replaceAll("/", "");
				SmbFile hfd = new SmbFile(t_dir+"/",ntlmPasswordAuth);
				boolean td=hfd.isDirectory();
				hfd.delete();
//				String t_prf="smb://"+syncRemoteAddr;
				if (td) {
					addMsgToProgDlg(false,"I",0, t_fn,msgs_mirror_prof_dir_deleted);
					if (mGp.settingShowSyncDetailMessage) 
						addLogMsg("I",t_dir,msgs_mirror_prof_dir_deleted);
					if (mGp.debugLevel>=1) 
						addDebugLogMsg(1,"I",
							"Remote directory was deleted:"+hfd.getPath().substring(0,hfd.getPath().length()-1));
				}
				else{ 
					addMsgToProgDlg(false,"I",0, t_fn,msgs_mirror_prof_file_deleted);
					if (mGp.settingShowSyncDetailMessage) 
						addLogMsg("I",t_dir,msgs_mirror_prof_file_deleted);
					if (mGp.debugLevel>=1) 
						addDebugLogMsg(1,"I",
							"Remote file was deleted:"+hfd.getPath().substring(0,hfd.getPath().length()-1));
				}
				deleteCount++;
				if (checkErrorStatus()!=0) return checkErrorStatus();
			}
		} catch (MalformedURLException e) {
			addLogMsg("E","","deleteRemoteFile MFUE url="+hf.getPath());
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			return setErrorStatusRetry(e.getMessage());
		} catch (SmbException e) {
			addLogMsg("E","","deleteRemoteFile SMBE url="+hf.getPath());
			String[] e_msg=NetworkUtil.analyzeNtStatusCode(e, mGp.appContext, 
					hf.getPath(),ntlmPasswordAuth.getUsername());
			addLogMsg("E","",e_msg[0]);//e.toString());
			printStackTraceElement(e.getStackTrace());
			return setErrorStatusRetry(e_msg[0]);
		}
		return deleteCount;
	};

	final private int deleteLocalItem(boolean deldir, String url) {
		if (mGp.debugLevel>=1) addDebugLogMsg(2,"I","deleteLocalItem=" + url);
		File sf = new File(url);
		if (SafUtil.isSafExternalSdcardPath(mSafCA, url)) {
			int result=0;
			if (deldir) result=deleteSafLocalFile("", sf); // delete specified dir
			else result=deleteSafLocalFile(sf.getPath(), sf); //not delete specified dir
			return result;
		} else {
			try {
				if (deldir) deleteLocalFile("", sf); // delete specified dir
				else deleteLocalFile(sf.getPath(), sf); //not delete specified dir
			} catch (Exception e) {
				addLogMsg("E","","deleteLocalItem EXCEP url="+url);
				addLogMsg("E","",e.getMessage());//e.toString());
				addLogMsg("E","","url="+url);
				printStackTraceElement(e.getStackTrace());
				isExceptionOccured=true;
				tcMirror.setThreadMessage(e.getMessage());
				return -1;
			}
		}
		return 0;
	};

	final private int deleteSafLocalFile(String rootpath, File lf) {
		int result=-1;
		if (lf.isDirectory()) {// ディレクトリの場合
			String[] children = lf.list();// ディレクトリにあるすべてのファイルを処理する
			for (int i = 0; i < children.length; i++) {
				deleteSafLocalFile(rootpath,(new File(lf, children[i])));
				if (checkErrorStatus()!=0) return checkErrorStatus();
			}
		}
		// 削除
		String url=lf.getPath();
		if (rootpath.equals(url)) {
			//root dirなので削除しない
			result=0;
		} else {
			SafFile df=SafUtil.getSafDocumentFileByPath(mSafCA, url, lf.isDirectory());
			if (df!=null) {
				boolean td=lf.isDirectory();
				if (df.delete()) result=0;
				deleteLocalFileLastModifiedEntry(
						currentFileLastModifiedList,newFileLastModifiedList,lf.getPath());
				deleteCount++;
				if (td) {
					addMsgToProgDlg(false,"I",0,lf.getName(),msgs_mirror_prof_dir_deleted);
					if (mGp.settingShowSyncDetailMessage)  
						addLogMsg("I",lf.getPath().replace(localUrl,""),msgs_mirror_prof_dir_deleted);
//									lf.getPath().replace(SMBSync_External_Root_Dir,"")));
					if (mGp.debugLevel>=1) 
						addDebugLogMsg(1,"I",
							"Local directory was deleted:"+lf.getPath());

				} else {
//					Log.v("","Dir="+lf.getParent()+", path="+lf.getPath());
					deleteMediaStoreItem(lf.getPath());
					addMsgToProgDlg(false,"I",0,lf.getName(),msgs_mirror_prof_file_deleted);
					if (mGp.settingShowSyncDetailMessage)  
						addLogMsg("I",lf.getPath().replace(localUrl,""),msgs_mirror_prof_file_deleted);
//									lf.getPath().replace(SMBSync_External_Root_Dir,"")));
					if (mGp.debugLevel>=1) 
						addDebugLogMsg(1,"I",
							"Local file was deleted:"+lf.getPath());

				}
				if (checkErrorStatus()!=0) return checkErrorStatus();
				return 0;
			}
		}
		return result;
	};
	
	final private int deleteLocalFile(String rootpath, File lf) {
		if (lf.isDirectory()) {// ディレクトリの場合
			String[] children = lf.list();// ディレクトリにあるすべてのファイルを処理する
			for (int i = 0; i < children.length; i++) {
				deleteLocalFile(rootpath,(new File(lf, children[i])));
				if (checkErrorStatus()!=0) return checkErrorStatus();
			}
		}
		// 削除
		if (rootpath.equals(lf.getPath())) {
			//root dirなので削除しない
		} else {
			boolean td=lf.isDirectory();
			lf.delete();
			deleteLocalFileLastModifiedEntry(
					currentFileLastModifiedList,newFileLastModifiedList,lf.getPath());
			deleteCount++;
			if (td) {
				addMsgToProgDlg(false,"I",0,lf.getName(),msgs_mirror_prof_dir_deleted);
				if (mGp.settingShowSyncDetailMessage)  
					addLogMsg("I",lf.getPath().replace(localUrl,""),msgs_mirror_prof_dir_deleted);
//								lf.getPath().replace(SMBSync_External_Root_Dir,"")));
				if (mGp.debugLevel>=1) 
					addDebugLogMsg(1,"I",
						"Local directory was deleted:"+lf.getPath());

			} else {
//				Log.v("","Dir="+lf.getParent()+", path="+lf.getPath());
				deleteMediaStoreItem(lf.getPath());
				addMsgToProgDlg(false,"I",0,lf.getName(),msgs_mirror_prof_file_deleted);
				if (mGp.settingShowSyncDetailMessage)  
					addLogMsg("I",lf.getPath().replace(localUrl,""),msgs_mirror_prof_file_deleted);
//								lf.getPath().replace(SMBSync_External_Root_Dir,"")));
				if (mGp.debugLevel>=1) 
					addDebugLogMsg(1,"I",
						"Local file was deleted:"+lf.getPath());

			}
			if (checkErrorStatus()!=0) return checkErrorStatus();
			return 0;
		}
		
		return 0;
	};
	
	final private boolean isRemoteDirEmpty(String url) {
		SmbFile hf;
		boolean dirEmpty=false;

		try {
			hf = new SmbFile(url,ntlmPasswordAuth);
			String[] list=hf.list();
			if (list.length==0) dirEmpty=true; 
		} catch (MalformedURLException e) {
			addLogMsg("E","","isRemoteDirEmpty MFUE url="+url);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			setErrorStatusRetry(e.getMessage());
//			isExceptionOccured=true;
//			tcMirror.setThreadMessage(e.getMessage());
			return false;
		} catch (SmbException e) {
			addLogMsg("E","","isRemoteDirEmpty SMBE url="+url);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			setErrorStatusRetry(e.getMessage());
//			isExceptionOccured=true;
//			tcMirror.setThreadMessage(e.getMessage());
			return false;
		}
		if (mGp.debugLevel>=1) 
			addDebugLogMsg(1,"I","isRemoteDirEmpty=" + url+", empty="+dirEmpty);
		return dirEmpty;
	};

	final private boolean isLocalDirEmpty(String url) {
		File lf;
		boolean dirEmpty=false;

		try {
			lf = new File(url);
			String[] list=lf.list();
			if (list==null) dirEmpty=true; 
			else if (list!=null && list.length==0) dirEmpty=true;
		} catch (Exception e) {
			addLogMsg("E","","isLocalDirEmpty EXCEP url="+url);
			isExceptionOccured=true;
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			tcMirror.setThreadMessage(e.getMessage());
			return false;
		}
		if (mGp.debugLevel>=1) 
			addDebugLogMsg(1,"I","isLocalDirEmpty=" + url+", empty="+dirEmpty);
		return dirEmpty;
	};
	
	final private void addMsgToProgDlg(boolean log, String log_cat, long when, String fp, String log_msg) {
		mUtil.addMsgToProgDlg(log, log_cat, when, syncProfName, fp, log_msg);
	};

	final private void addDebugLogMsg(int lvl, String log_cat, String...log_msg) {
		mUtil.addDebugLogMsg(lvl, log_cat, syncProfName, log_msg);
	};
	
	private StringBuilder mSbAddLogMsg=new StringBuilder(256);
	final private void addLogMsg(String log_cat, String fp, String log_msg) {
		mUtil.addLogMsg(log_cat, syncProfName, fp, log_msg);
		if (log_cat.equals("W")) totalWarningMsgCnt++;
		else if (log_cat.equals("E")) totalErrorMsgCnt++;
		if (mSyncHistoryPrintWriter!=null) {
			mSbAddLogMsg.setLength(0);
			if (fp.equals("")) {
				mSbAddLogMsg
					.append(log_cat)
					.append(" ")
					.append(syncProfName)
					.append(" ")
					.append(log_msg);
			} else {
				mSbAddLogMsg
					.append(log_cat)
					.append(" ")
					.append(syncProfName)
					.append(" ")
					.append(fp)
					.append(" ")
					.append(log_msg);
			}
			mSyncHistoryPrintWriter.println(mSbAddLogMsg.toString());
		}
	};

	
	static private String	msgs_mirror_prof_file_deleted	;
	static private String	msgs_mirror_prof_started	;
	static private String	msgs_mirror_prof_invalid_mirror_type	;
	static private String	msgs_mirror_prof_was_failed	;
	static private String	msgs_mirror_prof_no_of_copy	;
	static private String	msgs_mirror_prof_avg_rate	;
	static private String	msgs_mirror_prof_success_end	;
	static private String	msgs_mirror_prof_was_cancelled	;
	static private String	msgs_mirror_prof_file_copied	;
	static private String	msgs_mirror_prof_file_replaced	;
	static private String	msgs_mirror_prof_master_not_found;
	static private String	msgs_mirror_prof_dir_deleted	;
	static private String	msgs_mirror_prof_dir_create	;
	static private String	msgs_mirror_prof_dir_create_failed;
	static private String  msgs_mirror_prof_file_copying;
	static private String  msgs_mirror_prof_file_bypass_media_store_change;
	static private String  msgs_mirror_task_result_stats;
	static private String  msgs_mirror_task_started;
	static private String  msgs_mirror_task_ended;
	static private String  msgs_mirror_prof_ms_different_file_last_mod;
	static private String  msgs_mirror_prof_ms_read_error;
	static private String  msgs_mirror_prof_sync_local_mount_point_unavailable;
	static private String msgs_mirror_confirm_copy_cancel;
	static private String msgs_mirror_confirm_delete_cancel;
	static private String msgs_mirror_task_result_error_ignored;
	static private String msgs_mirror_task_result_error_ended;
	static private String msgs_mirror_task_result_ok;
	static private String msgs_mirror_task_result_cancel;
	static private String msgs_mirror_master_local_mount_point_not_readable;
	static private String msgs_mirror_target_local_mount_point_not_writable;
	static private String msgs_mirror_physcal_access_to_same_dir;
	static private String msgs_mirror_physcal_access_check_create_error;
	static private String msgs_mirror_same_directory_ignored;
	
	static private void loadMsgString(GlobalParameters glblParms) {

		msgs_mirror_task_result_ok=glblParms.appContext.getString(R.string.msgs_mirror_task_result_ok);
		msgs_mirror_task_result_cancel=glblParms.appContext.getString(R.string.msgs_mirror_task_result_cancel);
		msgs_mirror_task_result_error_ignored=glblParms.appContext.getString(R.string.msgs_mirror_task_result_error_ignored);
		msgs_mirror_task_result_error_ended=glblParms.appContext.getString(R.string.msgs_mirror_task_result_error_ended);
		msgs_mirror_confirm_copy_cancel=glblParms.appContext.getString(R.string.msgs_mirror_confirm_copy_cancel);
		msgs_mirror_confirm_delete_cancel=glblParms.appContext.getString(R.string.msgs_mirror_confirm_delete_cancel);
		
		msgs_mirror_prof_sync_local_mount_point_unavailable=glblParms.appContext.getString(R.string.msgs_mirror_prof_sync_local_mount_point_unavailable);
        msgs_mirror_prof_ms_different_file_last_mod=glblParms.appContext.getString(R.string.msgs_mirror_prof_ms_different_file_last_mod);
        msgs_mirror_prof_ms_read_error=glblParms.appContext.getString(R.string.msgs_mirror_prof_ms_read_error);

		msgs_mirror_task_started=glblParms.appContext.getString(R.string.msgs_mirror_task_started);
		msgs_mirror_task_ended=glblParms.appContext.getString(R.string.msgs_mirror_task_ended);
		msgs_mirror_task_result_stats=glblParms.appContext.getString(R.string.msgs_mirror_task_result_stats);
		msgs_mirror_prof_file_bypass_media_store_change=glblParms.appContext.getString(R.string.msgs_mirror_prof_file_bypass_media_store_change);
		msgs_mirror_prof_file_deleted=glblParms.appContext.getString(R.string.msgs_mirror_prof_file_deleted);
		msgs_mirror_prof_started=glblParms.appContext.getString(R.string.msgs_mirror_prof_started);
		msgs_mirror_prof_invalid_mirror_type=glblParms.appContext.getString(R.string.msgs_mirror_prof_invalid_mirror_type);
		msgs_mirror_prof_was_failed=glblParms.appContext.getString(R.string.msgs_mirror_prof_was_failed);
		msgs_mirror_prof_no_of_copy=glblParms.appContext.getString(R.string.msgs_mirror_prof_no_of_copy);
		msgs_mirror_prof_avg_rate=glblParms.appContext.getString(R.string.msgs_mirror_prof_avg_rate);
		msgs_mirror_prof_success_end=glblParms.appContext.getString(R.string.msgs_mirror_prof_success_end);
		msgs_mirror_prof_was_cancelled=glblParms.appContext.getString(R.string.msgs_mirror_prof_was_cancelled);
		msgs_mirror_prof_file_copied=glblParms.appContext.getString(R.string.msgs_mirror_prof_file_copied);
		msgs_mirror_prof_file_replaced=glblParms.appContext.getString(R.string.msgs_mirror_prof_file_replaced);
		msgs_mirror_prof_master_not_found=glblParms.appContext.getString(R.string.msgs_mirror_prof_master_not_found);
		msgs_mirror_prof_dir_deleted=glblParms.appContext.getString(R.string.msgs_mirror_prof_dir_deleted);
		msgs_mirror_prof_dir_create=glblParms.appContext.getString(R.string.msgs_mirror_prof_dir_create);
		msgs_mirror_prof_dir_create_failed=glblParms.appContext.getString(R.string.msgs_mirror_prof_dir_create_failed);
		msgs_mirror_prof_file_copying=glblParms.appContext.getString(R.string.msgs_mirror_prof_file_copying);
		
		msgs_mirror_master_local_mount_point_not_readable=glblParms.appContext.getString(R.string.msgs_mirror_master_local_mount_point_not_readable);
		msgs_mirror_target_local_mount_point_not_writable=glblParms.appContext.getString(R.string.msgs_mirror_target_local_mount_point_not_writable);
		msgs_mirror_physcal_access_to_same_dir=glblParms.appContext.getString(R.string.msgs_mirror_physcal_access_to_same_dir);
		msgs_mirror_physcal_access_check_create_error=glblParms.appContext.getString(R.string.msgs_mirror_physcal_access_check_create_error);
		
		msgs_mirror_same_directory_ignored=glblParms.appContext.getString(R.string.msgs_mirror_same_directory_ignored);
		
		return ;
				
	}
}
class MirrorIoParmList {
	private String mp_profname=""; 
	private String mp_master_type="";
	private String mp_target_type="";
	private String mp_target_name="";
	private String mp_mirror_type="";
	private String mp_remote_addr="";
	private String mp_remote_host="";
	private String mp_remote_port="";
	private String mp_remote_share="";
	private String mp_remote_dir="";
	private String mp_local_dir="";
	private String mp_local_mount_point="";
	private String mp_master_local_dir="";
	private String mp_master_local_mount_point="";
	private String mp_target_local_dir="";
	private String mp_target_local_mount_point="";
	private String mp_remote_userid="";
	private String mp_remote_pass="";
	private ArrayList<String> mp_file_filter=new ArrayList<String>();
	private ArrayList<String> mp_dir_filter=new ArrayList<String>();
	private boolean mp_master_dir_proc =true;
	private boolean mp_confirm_required =true;
	private boolean mp_force_last_modified_use_smbsync =true;
	private boolean mp_not_use_last_modified_for_remote =false;
	private int mp_retry_count =0;
	private boolean mp_sync_empty_dir=false;
	private boolean mp_sync_hidden_dir=false;
	private boolean mp_sync_hidden_file=false;
	private boolean mp_sync_sub_dir=false;
	
	private boolean mp_sync_use_remote_small_io_area=false;
	
	private boolean mp_sync_target_dir_app_specific=false;
	
	private String mp_sync_zip_file_name="";
	private int mp_sync_zip_enc_method=0, mp_sync_zip_aes_key_length=256;

	
	public MirrorIoParmList (
			String profname,
			String master_type,
			String target_type,
			String target,
			String type,
			String addr,
			String host,
			String port,
			String share,
			String r_dir,
			String lmp,
			String l_dir, 
			String user,
			String pass,
			ArrayList<String> ff,
			ArrayList<String> df,
			boolean mdp,
			boolean conf,
			boolean ujlm,
			boolean nulm_remote,
			int rc,
			boolean sync_empty_dir, boolean sync_hidden_dir, boolean sync_hidden_file,
			boolean sync_sub_dir, boolean sync_use_remote_small_io_area) {

		mp_profname=profname;
		mp_master_type=master_type;
		mp_target_type=target_type;
		mp_target_name=target;
		mp_mirror_type=type;
		mp_remote_addr=addr;
		mp_remote_host=host;
		mp_remote_port=port;
		mp_remote_share=share;
		mp_remote_dir=r_dir;
		mp_local_mount_point=lmp;
		mp_local_dir=l_dir;
		mp_remote_userid=user;
		mp_remote_pass=pass;
		mp_file_filter=ff;
		mp_dir_filter=df;
		mp_master_dir_proc=mdp;
		mp_confirm_required=conf;
		mp_force_last_modified_use_smbsync=ujlm;
		mp_not_use_last_modified_for_remote=nulm_remote;
		mp_retry_count=rc;
		mp_sync_empty_dir=sync_empty_dir;
		mp_sync_hidden_dir=sync_hidden_dir;
		mp_sync_hidden_file=sync_hidden_file;
		mp_sync_sub_dir=sync_sub_dir;
		mp_sync_use_remote_small_io_area=sync_use_remote_small_io_area;
	}

	public String getProfname() { return mp_profname;}
	public String getMasterType() { return mp_master_type;}
	public String getTargetType() { return mp_target_type;}
	public String getTargetName() { return mp_target_name;}
	public String getMirrorType() { return mp_mirror_type;}
	public String getRemoteAddr() { return mp_remote_addr;}
	public String getHostName()   { return mp_remote_host;}
	public String getRemoteShare() { return mp_remote_share;}
	public String getRemotePort() { return mp_remote_port;}
	public String getRemoteDir() { return mp_remote_dir;}
	public String getLocalDir() { return mp_local_dir;}
	public String getLocalMountPoint() { return mp_local_mount_point;}
	public String getMasterLocalDir() { return mp_master_local_dir;}
	public String getMasterLocalMountPoint() { return mp_master_local_mount_point;}
	public String getTargetLocalDir() { return mp_target_local_dir;}
	public String getTargetLocalMountPoint() { return mp_target_local_mount_point;}
	public String getRemoteUserid() { return mp_remote_userid;}
	public String getRemotePass() { return mp_remote_pass;}
	public ArrayList<String> getFileFilter() { return mp_file_filter;}
	public ArrayList<String> getDirFilter() { return mp_dir_filter;}
	public boolean isMasterDirFileProcessed() {return mp_master_dir_proc;}
	public boolean isConfirmRequired() {return mp_confirm_required;}
	public boolean isForceLastModifiedUseSmbsync() {return mp_force_last_modified_use_smbsync;}
	public boolean isNotUseLastModifiedForRemote() {return mp_not_use_last_modified_for_remote;}
	public void setNotUseLastModifiedForRemote(boolean p) {mp_not_use_last_modified_for_remote=p;}
	
	public boolean isSyncTargetDirIsAppSpecific() {return mp_sync_target_dir_app_specific;}
	public void setSyncTargetDirIsAppSpecific(boolean p) {mp_sync_target_dir_app_specific=p;}
	
	public void setProfname(String p) { mp_profname=p;}
	public void setMasterType(String p) { mp_master_type=p;}
	public void setTargetType(String p) { mp_target_type=p;}
	public void setTargetName(String p) { mp_target_name=p;}
	public void setMirrorType(String p) { mp_mirror_type=p;}
	public void setRemoteAddr(String p) { mp_remote_addr=p;}
	public void setHostName(String p)   { mp_remote_host=p;}
	public void setRemoteShare(String p) { mp_remote_share=p;}
	public void setRemotePort(String p) { mp_remote_port=p;}
	public void setRemoteDir(String p) { mp_remote_dir=p;}
	public void setLocalDir(String p) { mp_local_dir=p;}
	public void setLocalMountPoint(String p) { mp_local_mount_point=p;}
	public void setMasterLocalDir(String p) { mp_master_local_dir=p;}
	public void setMasterLocalMountPoint(String p) { mp_master_local_mount_point=p;}
	public void setTargetLocalDir(String p) { mp_target_local_dir=p;}
	public void setTargetLocalMountPoint(String p) { mp_target_local_mount_point=p;}
	public void setRemoteUserid(String p) { mp_remote_userid=p;}
	public void setRemotePass(String p) { mp_remote_pass=p;}
	public void setFileFilter(ArrayList<String> p) { mp_file_filter=p;}
	public void setDirFilter(ArrayList<String> p) { mp_dir_filter=p;}
	public void setMasterDirFileProcessed(boolean p) {mp_master_dir_proc=p;}
	public void setConfirmRequired(boolean p) {mp_confirm_required=p;}
	public void setForceLastModifiedUseSmbsync(boolean p) {mp_force_last_modified_use_smbsync=p;}
	
	public int getReryCount() {return mp_retry_count;}
	public void setRetryCount(int p) {mp_retry_count=p;};

	public boolean isSyncEmptyDirectory() {return mp_sync_empty_dir;}
	public void setSyncEmptyDirectory(boolean p) {mp_sync_empty_dir=p;};

	public boolean isSyncHiddenDirectory() {return mp_sync_hidden_dir;}
	public void setSyncHiddenDirectory(boolean p) {mp_sync_hidden_dir=p;};

	public boolean isSyncHiddenFile() {return mp_sync_hidden_file;}
	public void setSyncHiddenFile(boolean p) {mp_sync_hidden_file=p;};

	public boolean isSyncSubDirectory() {return mp_sync_sub_dir;}
	public void setSyncSubDir(boolean p) {mp_sync_sub_dir=p;};

	
	public boolean isSyncUseRemoteSmallIoArea() {return mp_sync_use_remote_small_io_area;}
	public void setSyncUseRemoteSmallIoArea(boolean p) {mp_sync_use_remote_small_io_area=p;};
	
	public void setSyncZipFileName(String p) {mp_sync_zip_file_name=p;}
	public void setSyncZipEncMethod(int enc_method) {mp_sync_zip_enc_method=enc_method;}
	public void setSyncZipAesKeyLength(int key_length) {mp_sync_zip_aes_key_length=key_length;}
	public String getSyncZipFileName() {return mp_sync_zip_file_name;}
	public int getSyncZipEncMethod() {return mp_sync_zip_enc_method;}
	public int getSyncZipAesKeyLength() {return mp_sync_zip_aes_key_length;}

}

class LocalFileLastModifiedListCacheItem {
	String mount_point_name;
	ArrayList<FileLastModifiedEntryItem> cureent_list=
			new ArrayList<FileLastModifiedEntryItem>();
	ArrayList<FileLastModifiedEntryItem> new_list=
			new ArrayList<FileLastModifiedEntryItem>();
}
