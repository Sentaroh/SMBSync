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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckedTextView;
import android.widget.Spinner;

import com.sentaroh.android.Utilities.StringUtil;

@SuppressLint("SimpleDateFormat")
public class SMBSyncUtil {
	
	private GlobalParameters mGp=null;
	private Context mContext=null;
	
	public SMBSyncUtil (Context c, String lid, GlobalParameters gp) {
		mContext=c;
		mGp=gp;
		setLogIdentifier(lid);
	};
	
	public static void setSpinnerBackground(Context c, Spinner spinner, boolean theme_is_light) {
		Log.v("","light="+theme_is_light);
//		if (theme_is_light) spinner.setBackgroundDrawable(c.getResources().getDrawable(R.drawable.spinner_color_background));
//		else spinner.setBackgroundDrawable(c.getResources().getDrawable(R.drawable.spinner_color_background_light));
		if (theme_is_light) spinner.setBackgroundColor(0xffdddddd);
		else spinner.setBackgroundColor(0xff555555);
	};

	public boolean setActivityIsForeground(boolean d) {
		mGp.activityIsForeground=d;
		return d;
	};

	public boolean isDebuggable() {
        PackageManager manager = mContext.getPackageManager();
        ApplicationInfo appInfo = null;
        try {
            appInfo = manager.getApplicationInfo(mContext.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            return false;
        }
        if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE)
            return true;
        return false;
    };
	
	public boolean isActivityForeground() {return mGp.activityIsForeground;};
	
	public boolean isRemoteDisable() {
		boolean ret=false;
		boolean ws=isWifiActive();
		if (mGp.settingWifiOption.equals(SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_OFF)) {
			ret=false;
		} else {
			if (ws) ret=false;
			else ret=true;
		}
		
		addDebugLogMsg(2,"I","isRemoteDisable settingWifiOption="+mGp.settingWifiOption+
				", WifiConnected="+ws+", result="+ret);
		
		return ret;
	};
	
//	@SuppressLint("NewApi")
	public void initAppSpecificExternalDirectory(Context c) {
//		if (Build.VERSION.SDK_INT>=19) {
//			c.getExternalFilesDirs(null);
//		} else {
//		}
		ContextCompat.getExternalFilesDirs(c, null);
	};
	
	public boolean isWifiActive() { 
		boolean ret=false;
		WifiManager mWifi = 
				(WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
//		if (mWifi.getConnectionInfo().getSupplicantState()==
//				SupplicantState.COMPLETED)
		String ssid="";
		if (mWifi.isWifiEnabled()) {
			if (mGp.settingWifiOption.equals(SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_ON)) {
				ret=true;
			} else if (mGp.settingWifiOption.equals(SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP)) {
				ssid=mWifi.getConnectionInfo().getSSID();
				if (ssid!=null && 
						!ssid.equals("0x") &&
						!ssid.equals("<unknown ssid>") &&
						!ssid.equals("")) ret=true;
//				Log.v("","ssid="+ssid);
			} else if (mGp.settingWifiOption.equals(SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_SPEC_AP)) {
				ret=true;
			}
		}
		addDebugLogMsg(2,"I","isWifiActive WifiEnabled="+mWifi.isWifiEnabled()+
				", settingWifiOption="+mGp.settingWifiOption+
				", SSID="+ssid+", result="+ret);
		return ret;
	};
	
	public static boolean isSmbHostAddressConnected(String addr) {
		boolean result=false;
		if (NetworkUtil.isIpAddressAndPortConnected(addr,139,3500) || 
				NetworkUtil.isIpAddressAndPortConnected(addr,445,3500)) result=true;
		return result;
	};
	
	public static boolean isSmbHostAddressConnected(String addr, int port) {
		boolean result=false;
		result=NetworkUtil.isIpAddressAndPortConnected(addr,port,3500);
//		Log.v("","addr="+addr+", port="+port+", result="+result);
		return result;
	};

    public static String getLocalIpAddress() {
        String result="192.168.0.1";
        boolean exit=false;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
//	            	Log.v("","ip="+inetAddress.getHostAddress()+
//	            			", name="+intf.getName()+", v4="+(inetAddress instanceof Inet4Address ? "Yes":"No"));
                    if (inetAddress.isSiteLocalAddress() &&  (inetAddress instanceof Inet4Address)) {
                        if (intf.getName().equals("wlan0")) {
                            exit=true;
                            result=inetAddress.getHostAddress();
                            break;
                        }
                    }
                }
                if (exit) break;
            }
        } catch (SocketException ex) {
            Log.e(APPLICATION_TAG, ex.toString());
        }
        return result;
    };

//	public static String getIfIpAddress() {
//		String result="";
//	    try {
//	        for (Enumeration<NetworkInterface> en =
//	        		NetworkInterface.getNetworkInterfaces();
//	        		en.hasMoreElements();) {
//	            NetworkInterface intf = en.nextElement();
//	            for (Enumeration<InetAddress> enumIpAddr =
//	            		intf.getInetAddresses();
//	            		enumIpAddr.hasMoreElements();) {
//	            	InetAddress inetAddress = enumIpAddr.nextElement();
////	            	Log.v("","ip="+inetAddress.getHostAddress());
//	            	if (!inetAddress.isLoopbackAddress() &&
//	            			(inetAddress.getHostAddress().startsWith("0") ||
//	            					inetAddress.getHostAddress().startsWith("1") ||
//	            					inetAddress.getHostAddress().startsWith("2"))) {
//	                    result=inetAddress.getHostAddress();
//	                    break;
//	            	}
//	            }
//	        }
//	    } catch (SocketException ex) {
//	        Log.e(APPLICATION_TAG, ex.toString());
//	        result="192.168.0.1";
//	    }
////		Log.v("","getIfIpAddress result="+result);
//	    if (result.equals("")) result="192.168.0.1";
//	    return result;
//	};

	private StringBuilder mSbForaddMsgToProgDlg = new StringBuilder(256);
	final public void addMsgToProgDlg(boolean log, String log_cat, long when, String syncProfName, 
			String fp, String log_msg) {
		String msgflag="";
		if (log) msgflag="1"; 	// flag=1 both, arg2=0 dialog only, arg2=2 msgview only
		else msgflag="0";
		String dt=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
		sendMsgToActivity(log_cat,msgflag, when, syncProfName,dt.substring(0,10),
				dt.substring(11),mLogIdWithSep,"M",fp,log_msg);
		if (!msgflag.equals("0")) 
			writeLogMsgToFile(log_cat,syncProfName, fp,log_msg);
		if (mGp.debugLevel>0) { 
			if (mGp.debugLevel>0 && log) 
				Log.v(APPLICATION_TAG,
					buildLogCatString(mSbForaddMsgToProgDlg,
							log_cat,mLogIdWithSep,syncProfName,fp,log_msg));
		}
	};

	private StringBuilder mSbForWriteLog = new StringBuilder(256);
	final private String writeLogMsgToFile(String cat, String prof, 
			String fp, String msg) {
		String result=null;
		result=formatLogMsg(cat,prof,fp,msg);
		writeLog(mGp,result);
		return result;
	};

	final public String formatLogMsg(String cat, String prof, 
			String fp, String msg) {
		String dt=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
		mSbForWriteLog.setLength(0);
		mSbForWriteLog.append(cat).append(" ")
			.append(dt.substring(0,10)).append(" ")
			.append(dt.substring(11)).append(" ")
			.append(mLogIdWithSep);
		if (!prof.equals("")) {
			mSbForWriteLog.append(prof).append(" ");
		}
		if (!fp.equals("")) {
			mSbForWriteLog.append(fp).append(" ");
		}
		mSbForWriteLog.append(msg);
		return mSbForWriteLog.toString();
	};
	
	private StringBuilder mSbForsendMsgToActivity=new StringBuilder(256);
	final public void sendMsgToActivity(final String log_cat, final String msgflag, final long when, 
			final String sync_prof, final String date, final String time, final String tag, 
			final String debug_flag, final String fp, final String msg_text) {
		mGp.uiHandler.post(new Runnable(){
			@Override
			public void run() {
				if (msgflag.equals("0")) {
					mGp.progressSpinSyncprof.setText(sync_prof);
					mGp.progressSpinFilePath.setText(fp);
					mGp.progressSpinStatus.setText(msg_text);
					NotificationUtil.showOngoingMsg(mGp,when,sync_prof,fp,msg_text);
				} else { //
					if (msgflag.equals("1")) {
						mGp.progressSpinSyncprof.setText(sync_prof);
						mGp.progressSpinFilePath.setText(fp);
						mGp.progressSpinStatus.setText(msg_text);
						NotificationUtil.showOngoingMsg(mGp,when,sync_prof,fp,msg_text);
					}  
					if (debug_flag.equals("M") || 
							(debug_flag.equals("D")&&mGp.settingDebugMsgDisplay)) {
						mSbForsendMsgToActivity.setLength(0);
						if (!sync_prof.equals("")) mSbForsendMsgToActivity.append(sync_prof).append(" ");
						if (!fp.equals("")) mSbForsendMsgToActivity.append(fp).append(" ");
						mSbForsendMsgToActivity.append(msg_text);
						addMsgToMsglistAdapter(mGp,
								new MsgListItem(log_cat,date,time,tag,
										mSbForsendMsgToActivity.toString()));
					}
				}
			}
		});
	};

	private StringBuilder mSbForAddLogMsg=new StringBuilder(256);
	final public String addLogMsg(String log_cat, String sync_prof, String fp, String log_msg) {
		String dt=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
		// flag=1 both, arg2=0 dialog only, arg2=2 msgview only
		sendMsgToActivity(log_cat,"2",0, sync_prof,dt.substring(0,10), dt.substring(11),
				mLogIdWithSep,"M",fp,log_msg);
		String wmsg=writeLogMsgToFile("M "+log_cat, sync_prof, fp, log_msg);
		if (mGp.debugLevel>0)
			Log.v(APPLICATION_TAG,
					buildLogCatString(mSbForAddLogMsg, log_cat,mLogIdWithSep,sync_prof,fp,log_msg));
		return wmsg;
	};

	final public void addLogMsg(String cat, String logmsg) {
		String dt=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
		addMsgToMsglistAdapter(mGp,
			  		 new MsgListItem(cat,dt.substring(0,10), dt.substring(11), mLogId,logmsg));
		writeLog(mGp, "M "+cat+" "+dt.substring(0,10)+" "+dt.substring(11)+" "+
				mLogIdWithSep+logmsg);
		if (mGp.debugLevel>0) Log.v(APPLICATION_TAG,cat+" "+mLogIdWithSep+logmsg);
	};
	
	final static public void addMsgToMsglistAdapter(
			final GlobalParameters gp, MsgListItem mli) {
		if (gp.msgListAdapter!=null) {
			if (gp.msgListAdapter.getCount()>5000) { 
				for (int i=0;i<1000;i++) gp.msgListAdapter.remove(0);
			}
			gp.msgListAdapter.add(mli);
			if (!gp.freezeMessageViewScroll && gp.activityIsForeground) {
				if (gp.msgListView!=null)
					gp.msgListView.setSelection(gp.msgListView.getCount()-1);
			}
//			gp.msgListAdapter.notifyDataSetChanged();
		}
	};

	final public void addMsgToMsglistAdapter(MsgListItem mli) {
		addMsgToMsglistAdapter(mGp,mli);
	};

	private String mLogIdWithSep="Util       ", mLogId="";
	final public void setLogIdentifier(String lid) {
		mLogIdWithSep=(lid+"                        ").substring(0,13);
		mLogId=lid;
	};
	
	final static private String buildLogCatString(
			StringBuilder sb,
			String cat, String lid, String prof, String fp, String msg) {
		sb.setLength(0);
		if (!cat.equals("")) {
			sb.append(cat).append(" ");
		}
		sb.append(lid);
		if (!prof.equals("")) {
			sb.append(prof).append(" ");
		}
		if (!fp.equals("")) {
			sb.append(fp).append(" ");
		}
		sb.append(msg);
		return sb.toString();
	};

	private StringBuilder mSbForaddDebugLogMsg1=new StringBuilder(256);
	final public void addDebugLogMsg(
			int lvl, String log_cat, String syncProfName, String...log_msg) {
		if (mGp.debugLevel>=lvl) {
			mSbForaddDebugLogMsg1.setLength(0);
			for (int i=0;i<log_msg.length;i++) mSbForaddDebugLogMsg1.append(log_msg[i]);
			if (mGp.settingDebugMsgDisplay) {
//				// flag=1 both, arg2=0 dialog only, arg2=2 msg view only
				String dt=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
				sendMsgToActivity(log_cat,"2",0, syncProfName,dt.substring(0,10),
						dt.substring(11),mLogIdWithSep,"D","", 
						mSbForaddDebugLogMsg1.toString());
			}			
			writeLogMsgToFile("D "+log_cat,syncProfName, "", mSbForaddDebugLogMsg1.toString());
			
			Log.v(APPLICATION_TAG,
				buildLogCatString(mSbForaddDebugLogMsg1,log_cat,mLogIdWithSep,syncProfName,"",
						mSbForaddDebugLogMsg1.toString()));
		}
	};

	private StringBuilder mSbForaddDebugLogMsg2=new StringBuilder(256);
	final public void addDebugLogMsg(
			int lvl, String cat, String logmsg) {
		if (mGp.debugLevel>=lvl ) {
			if (mGp.settingDebugMsgDisplay) {
				String dt=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
				addMsgToMsglistAdapter(mGp,
				    		 new MsgListItem(cat,dt.substring(0,10),
				    				 dt.substring(11),mLogIdWithSep,logmsg));
			}
			String dt=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
			mSbForaddDebugLogMsg2.setLength(0);
			mSbForaddDebugLogMsg2.append("D ")
				.append(cat)
				.append(" ")
				.append(dt.substring(0,10))
				.append(" ")
				.append(dt.substring(11))
				.append(" ")
				.append(mLogIdWithSep)
				.append(logmsg);
			writeLog(mGp,mSbForaddDebugLogMsg2.toString());
			Log.v(APPLICATION_TAG,cat+" "+mLogIdWithSep+logmsg);
		}
	};

	static public void writeLog(GlobalParameters gp, String msg) {
		LogUtil.writeLog(msg);
	};
	
	static public void setCheckedTextView(final CheckedTextView ctv) {
		ctv.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctv.toggle();
			}
		});
	};

    
	@SuppressLint("SdCardPath")
	public ArrayList<SyncHistoryListItem> loadHistoryList() {
//		Log.v("","load hist started");
		ArrayList<SyncHistoryListItem> hl=new ArrayList<SyncHistoryListItem>();
		try {
			String dir=mGp.externalRootDirectory+"/SMBSync";
			File lf=new File(dir+"/history.txt");
			if (lf.exists()) {
				FileReader fw=new FileReader(lf);
				BufferedReader br=new BufferedReader(fw,4096*16);
				String line="";
				String[] l_array=null;
			    while ((line = br.readLine()) != null) {
			    	l_array=line.split("\u0001");
//			    	Log.v("","line="+line);
//			    	Log.v("","em="+l_array[7]);
			    	if (l_array!=null && l_array.length>=11 && !l_array[2].equals("")) {
			    		SyncHistoryListItem hli=new SyncHistoryListItem();
			    		try {
				    		hli.sync_date=l_array[0];
				    		hli.sync_time=l_array[1];
				    		hli.sync_prof=l_array[2];
				    		hli.sync_status=Integer.valueOf(l_array[3]);
				    		hli.sync_result_no_of_copied=Integer.valueOf(l_array[4]);
				    		hli.sync_result_no_of_deleted=Integer.valueOf(l_array[5]);
				    		hli.sync_result_no_of_ignored=Integer.valueOf(l_array[6]);
				    		hli.sync_error_text=l_array[7].replaceAll("\u0002", "\n");
				    		if (!l_array[8].equals(" ")) hli.sync_result_no_of_retry=Integer.valueOf(l_array[8]);
//				    		hli.sync_deleted_file=string2Array(l_array[9]);
//				    		hli.sync_ignored_file=string2Array(l_array[10]);
				    		if (l_array.length>=12) {
				    			hli.sync_log_file_path=l_array[11];
					    		if (!hli.sync_log_file_path.equals("")) {
									File tf=new File(hli.sync_log_file_path);
									if (tf.exists()) hli.isLogFileAvailable=true;
					    		}
					    		if (l_array.length>=13) {
					    			hli.sync_result_file_path=l_array[12];
					    		}
				    		}
				    		hl.add(hli);
			    		} catch(Exception e) {
			    			addLogMsg("W","History list can not loaded");
			    			e.printStackTrace();
			    		}
			    	} 
				}
				br.close();
				if (hl.size()>1) {
					Collections.sort(hl,new Comparator<SyncHistoryListItem>(){
						@Override
						public int compare(SyncHistoryListItem lhs, SyncHistoryListItem rhs) {
							if (rhs.sync_date.equals(lhs.sync_date)) {
								if (rhs.sync_time.equals(lhs.sync_time)) {
									return lhs.sync_prof.compareToIgnoreCase(rhs.sync_prof);
								} else return rhs.sync_time.compareTo(lhs.sync_time) ;
							} else return rhs.sync_date.compareTo(lhs.sync_date) ;
						}
					});
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (hl.size()==0) {
			SyncHistoryListItem hli=new SyncHistoryListItem();
			hli.sync_prof="";
			hl.add(hli);
		}
//		Log.v("","load hist ended");
		return hl;
	};
	
	public void housekeepHistoryList() {
//		final Handler hndl=new Handler();
		Thread th=new Thread() {
			@Override
			public void run() {
				String dir=mGp.externalRootDirectory+"/SMBSync/result_log";
				File lf=new File(dir);
				if (lf.exists()) {
					File[] fl=lf.listFiles();
					if (fl!=null && fl.length>0) {
						for(int i=0;i<fl.length;i++) {
							String fp=fl[i].getPath();
							boolean found=false;
							for (int j=0;j<mGp.syncHistoryAdapter.getCount();j++) {
								if (mGp.syncHistoryAdapter.getItem(j).sync_result_file_path.equals(fp)) {
									found=true;
									break;
								}
							}
							if (!found) {
								File tlf=new File(fp);
								tlf.delete();
								addDebugLogMsg(1,"I","Sync history result log was delete because file name not registerd to SyncHistoryList, fp="+fp);							}
						}
					}
				}
			}
		};
		th.start();
	};

	final public String createSyncResultFilePath(String syncProfName) {
		String dir=mGp.externalRootDirectory+"/SMBSync/result_log";
		File tlf=new File(dir);
		if (!tlf.exists()) tlf.mkdirs();
		String dt=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
		String fn="result_"+syncProfName+"_"+dt+".txt";
		String fp=dir+"/"+fn.replaceAll("/", "-").replaceAll(":", "").replaceAll(" ","_");
		return fp;
	};
	
	@SuppressLint("SdCardPath")
	final public void saveHistoryList(final AdapterSyncHistory ha) {
//		Log.v("","save hist started");
		ArrayList<SyncHistoryListItem> hl=ha.getSyncHistoryList();
		try {
			String dir=mGp.externalRootDirectory+"/SMBSync";
			File lf=new File(dir);
			lf.mkdirs();
			lf=new File(dir+"/history.txt");
			FileWriter fw=new FileWriter(lf);
			BufferedWriter bw=new BufferedWriter(fw,4096*16);
			int max=500;
			StringBuilder sb_buf=new StringBuilder(1024*2);
			SyncHistoryListItem shli=null;
//			String cpy_str, del_str, ign_str;
			final ArrayList<SyncHistoryListItem>del_list=new ArrayList<SyncHistoryListItem>();
			for (int i=0;i<hl.size();i++) {
//				Log.v("","i="+i+", n="+hl.get(i).sync_prof);
				if (!hl.get(i).sync_prof.equals("")) {
					shli=hl.get(i);
					if (i<max) {
//						cpy_str=array2String(sb_buf,shli.sync_copied_file);
//						del_str=array2String(sb_buf,shli.sync_deleted_file);
//						ign_str=array2String(sb_buf,shli.sync_ignored_file);
						String lfp="";
						if (shli.isLogFileAvailable) lfp=shli.sync_log_file_path;
						sb_buf.setLength(0);
						sb_buf.append(shli.sync_date).append("\u0001")
							.append(shli.sync_time).append("\u0001")
							.append(shli.sync_prof).append("\u0001")
							.append(shli.sync_status).append("\u0001")
							.append(shli.sync_result_no_of_copied).append("\u0001")
							.append(shli.sync_result_no_of_deleted).append("\u0001")
							.append(shli.sync_result_no_of_ignored).append("\u0001")
							.append(shli.sync_error_text.replaceAll("\n", "\u0002")).append("\u0001")
							.append(shli.sync_result_no_of_retry).append("\u0001") //retry count
							.append(" ").append("\u0001") //Dummy
							.append(" ").append("\u0001") //Dummy 
							.append(lfp).append("\u0001")
							.append(shli.sync_result_file_path)
							.append("\n");
								
						bw.append(sb_buf.toString());
					} else {
						del_list.add(shli);
						if (!shli.sync_result_file_path.equals("")) {
							File tlf=new File(shli.sync_result_file_path);
							if (tlf.exists()) tlf.delete();
						}
					}
				}
			}
			
			synchronized(ha) {
				mGp.uiHandler.post(new Runnable(){
					@Override
					public void run() {
						for (int i=0;i<del_list.size();i++) ha.remove(del_list.get(i));
						ha.notifyDataSetChanged();
					}
				});
			}
			
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		Log.v("","save hist ended");
	};
	
//	final static private String array2String(StringBuilder sb_buf,String[] sa) {
//		sb_buf.setLength(0);
//		if (sa!=null) {
//			sb_buf
//				.append(Integer.toString(sa.length))
//				.append("\u0002");
//			for (int i=0;i<sa.length;i++) {
//				sb_buf.append("\u0003")
//					.append(sa[i])
//					.append("\u0002");
//			}
//		} else {
//			sb_buf.append(Integer.toString(0));
//		}
//		return sb_buf.toString();
//	};
//
//	final static private String[] string2Array(String str) {
//		String[]t_array=str.split("\u0002");
//		String[] result=null;
//		if (!t_array[0].equals("0")) {
//			result=new String[Integer.parseInt(t_array[0])];
//			for (int i=0;i<result.length;i++) {
//				result[i]=t_array[i+1].replace("\u0003", "");
//			}
//		} 
//		return result;
//	};
	
	public void addHistoryList(AdapterSyncHistory ha, SyncHistoryListItem item) {
		synchronized(ha) {
			if (ha.getCount()==1) {
				if (ha.getItem(0).sync_prof.equals("")) ha.remove(0);
			}
//			Log.v("","add");
			ha.insert(item,0);
//			Log.v("","Notify");
			ha.notifyDataSetChanged();
		}
	};
//	public void removeHistoryList(ArrayList<SyncHistoryListItem> hl, int pos) {
//		String result_fp=hl.get(pos).sync_result_file_path;
//		if (!result_fp.equals("")) {
//			File lf=new File(result_fp);
//			lf.delete();
//		}
//		hl.remove(pos);
//	};
//	public void removeHistoryList(ArrayList<SyncHistoryListItem> hl, SyncHistoryListItem item) {
//		String result_fp=item.sync_result_file_path;
//		if (!result_fp.equals("")) {
//			File lf=new File(result_fp);
//			lf.delete();
//		}
//		hl.remove(item);
//	};
	
}
