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

import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V1;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V2;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V3;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_LOCAL_FILE_LAST_MODIFIED_WAS_FORCE_LASTEST;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_LOCAL;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_SYNC;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.ThemeColorList;
import com.sentaroh.android.Utilities.ThemeUtil;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.Dialog.DialogBackKeyListener;


public class LocalFileLastModified {

	private Context mContext;
	private AdapterProfileList profileAdapter;
	private SMBSyncUtil util;
	private CommonDialog commonDlg;
	
	private GlobalParameters mGp=null;
	
	LocalFileLastModified(GlobalParameters gp, Context c, AdapterProfileList pa, SMBSyncUtil ut, CommonDialog cd) {
		mGp=gp;
		mContext=c;
		profileAdapter=pa;
		util=ut;
		commonDlg=cd;
	};
	
	
	public void maintLastModListDlg() {
		ArrayList<LocalFileLastModifiedMaintListItem> maint_list=
				new ArrayList<LocalFileLastModifiedMaintListItem>();
		boolean holder_data_available=false;
		createLastModifiedMaintList(mContext, profileAdapter,maint_list);
		if (maint_list.size()==0) {
			maint_list.add(new LocalFileLastModifiedMaintListItem(
					mContext.getString(R.string.msgs_local_file_modified_maint_no_entry),"",false));
		} else holder_data_available=true;
					
		// common カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);//, mGp.applicationTheme);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.maint_last_mod_list_dlg);
		
    	ThemeColorList tcl=ThemeUtil.getThemeColorList(mContext);
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.maint_last_mod_list_dlg_view);
		ll_dlg_view.setBackgroundColor(tcl.dialog_msg_background_color);
		
		LinearLayout title_view=(LinearLayout)dialog.findViewById(R.id.maint_last_mod_list_dlg_title_view);
		TextView title=(TextView)dialog.findViewById(R.id.maint_last_mod_list_dlg_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		
		TextView dlg_msg=(TextView)dialog.findViewById(R.id.maint_last_mod_list_dlg_msg);
		ListView dlg_lv=(ListView)dialog.findViewById(R.id.maint_last_mod_list_dlg_listview);
		
		dlg_msg.setText("");
		
		final Button btnClose = (Button) dialog.findViewById(R.id.maint_last_mod_list_dlg_close_btn);
		final Button btnInit = (Button) dialog.findViewById(R.id.maint_last_mod_list_dlg_init_btn);
		final Button btnDelete = (Button) dialog.findViewById(R.id.maint_last_mod_list_dlg_delete_btn);
		final Button btnReset = (Button) dialog.findViewById(R.id.maint_last_mod_list_dlg_reset_btn);
		final Button btnCleanup = (Button) dialog.findViewById(R.id.maint_last_mod_list_dlg_cleanup_btn);
		btnDelete.setEnabled(false);
		btnInit.setEnabled(false);
		btnReset.setEnabled(false);
		
		CommonDialog.setDlgBoxSizeLimit(dialog,false);
		
		final AdapterLocalFileLastModifiedMaintList lflmAdapter=new AdapterLocalFileLastModifiedMaintList(
				mContext, R.layout.maint_last_modified_list_dlg_listview_item,
				maint_list); 
		dlg_lv.setAdapter(lflmAdapter);
		
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
//				int pos=(Integer)o[0];
//				boolean isChecked=(Boolean)o[1];
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		lflmAdapter.setNotifyCheckBoxListener(ntfy);
		
		dlg_lv.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
				boolean p_chk=lflmAdapter.getItem(pos).isChecked();
	         	if (!lflmAdapter.getItem(pos).getLocalMountPoint().equals(
	         			mContext.getString(R.string.msgs_local_file_modified_maint_no_entry))) {
					for (int i=0;i<lflmAdapter.getCount();i++) lflmAdapter.getItem(i).setChecked(false);
					lflmAdapter.getItem(pos).setChecked(!p_chk);
//					Log.v("","checked="+lflmAdapter.getItem(pos).isChecked()+", pchk="+p_chk);
					if (lflmAdapter.getItem(pos).isChecked()) {
						if (lflmAdapter.getItem(pos).getStatus().equals(mContext.getString(R.string.msgs_local_file_modified_maint_status_init))) {
							btnDelete.setEnabled(true);
							btnInit.setEnabled(false);
							btnReset.setEnabled(true);
						} else if (lflmAdapter.getItem(pos).getStatus().equals(mContext.getString(R.string.msgs_local_file_modified_maint_status_reset))) {
							btnDelete.setEnabled(true);
							btnInit.setEnabled(true);
							btnReset.setEnabled(false);
						} else if (lflmAdapter.getItem(pos).getStatus().equals(mContext.getString(R.string.msgs_local_file_modified_maint_status_valid))) {
							btnDelete.setEnabled(true);
							btnInit.setEnabled(true);
							btnReset.setEnabled(true);
						} else if (lflmAdapter.getItem(pos).getStatus().equals(mContext.getString(R.string.msgs_local_file_modified_maint_status_corrupted))) {
							btnDelete.setEnabled(true);
							btnInit.setEnabled(true);
							btnReset.setEnabled(true);
						} else if (lflmAdapter.getItem(pos).getStatus().equals(mContext.getString(R.string.msgs_local_file_modified_maint_status_fileonly))) {
							btnDelete.setEnabled(true);
							btnInit.setEnabled(false);
							btnReset.setEnabled(false);
						} else if (lflmAdapter.getItem(pos).getStatus().equals(mContext.getString(R.string.msgs_local_file_modified_maint_status_not_created))) {
							btnDelete.setEnabled(false);
							btnInit.setEnabled(true);
							btnReset.setEnabled(true);
						}
					} else {
						btnDelete.setEnabled(false);
						btnInit.setEnabled(false);
						btnReset.setEnabled(false);
					}
					lflmAdapter.notifyDataSetChanged();
	         	}
			}
		});
		dialog.setOnKeyListener(new DialogBackKeyListener(mContext));
		// Delete ボタンの指定
		btnDelete.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String d_mp="";
				LocalFileLastModifiedMaintListItem tli=null;
				for (int i=0;i<lflmAdapter.getCount();i++) {
					if (lflmAdapter.getItem(i).isChecked()) {
						tli=lflmAdapter.getItem(i);
						d_mp=tli.getLocalMountPoint();
					}
				}
				final LocalFileLastModifiedMaintListItem lflmmli=tli; 
				final String d_lmp=d_mp;
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						deleteListFile(d_lmp);
						lflmmli.setStatus(mContext.getString(R.string.msgs_local_file_modified_maint_status_not_created));
						lflmmli.setChecked(false);
						btnDelete.setEnabled(false);
						btnInit.setEnabled(false);
						btnReset.setEnabled(false);
						lflmAdapter.notifyDataSetChanged();
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {}
				});
				commonDlg.showCommonDialog(true, "W", 
						mContext.getString(R.string.msgs_local_file_modified_maint_delete_msg), 
						getFillePathFromLmpName(d_lmp), ntfy);
			}
		});
		// Init ボタンの指定
		btnInit.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						for (int i=0;i<lflmAdapter.getCount();i++)
							if (lflmAdapter.getItem(i).isChecked()) {
								ArrayList<FileLastModifiedEntryItem> curr_list=
										new ArrayList<FileLastModifiedEntryItem>();
								ArrayList<FileLastModifiedEntryItem> new_list=
										new ArrayList<FileLastModifiedEntryItem>();
								saveLastModifiedList(lflmAdapter.getItem(i).getLocalMountPoint(),
										curr_list, new_list);
								lflmAdapter.getItem(i).setStatus(mContext.getString(R.string.msgs_local_file_modified_maint_status_init));
								
								lflmAdapter.getItem(i).setChecked(false);
								btnDelete.setEnabled(false);
								btnInit.setEnabled(false);
								btnReset.setEnabled(false);

								lflmAdapter.notifyDataSetChanged();
							}
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {}
				});
				commonDlg.showCommonDialog(true, "W", 
						mContext.getString(R.string.msgs_local_file_modified_maint_init_btn), 
						mContext.getString(R.string.msgs_local_file_modified_maint_init_msg), 
						ntfy);
			}
		});
		// Reset ボタンの指定
		btnReset.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						for (int i=0;i<lflmAdapter.getCount();i++)
							if (lflmAdapter.getItem(i).isChecked()) {
								ArrayList<FileLastModifiedEntryItem> curr_list=
										new ArrayList<FileLastModifiedEntryItem>();
								ArrayList<FileLastModifiedEntryItem> new_list=
										new ArrayList<FileLastModifiedEntryItem>();
								new_list.add(new FileLastModifiedEntryItem(
										SMBSYNC_LOCAL_FILE_LAST_MODIFIED_WAS_FORCE_LASTEST,0,0,false));
								saveLastModifiedList(lflmAdapter.getItem(i).getLocalMountPoint(),
										curr_list, new_list);
								lflmAdapter.getItem(i).setStatus(mContext.getString(R.string.msgs_local_file_modified_maint_status_reset));
								
								lflmAdapter.getItem(i).setChecked(false);
								btnDelete.setEnabled(false);
								btnInit.setEnabled(false);
								btnReset.setEnabled(false);

								lflmAdapter.notifyDataSetChanged();
							}
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {}
				});
				commonDlg.showCommonDialog(true, "W", 
						mContext.getString(R.string.msgs_local_file_modified_maint_reset_btn), 
						mContext.getString(R.string.msgs_local_file_modified_maint_reset_msg), ntfy);
			}
		});
		// Cleanup ボタンの指定
		if (holder_data_available &&
			isLastModifiedFileV1Exists()) btnCleanup.setVisibility(Button.VISIBLE);
		else btnCleanup.setVisibility(Button.GONE);
		btnCleanup.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						File lf=new File(LocalMountPoint.getExternalStorageDir()+"/SMBSync/");
						File[] fl=lf.listFiles();
						if (fl!=null) {
							for (int i=0;i<fl.length;i++) {
//								Log.v("","del="+fl[i].getName());
								if (fl[i].isFile() && fl[i].getName().startsWith(SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V1)) {
									File dlf=new File(fl[i].getPath());
									dlf.delete();
									util.addDebugLogMsg(1, "I", 
											"maintLastModList Old holder file was deleted. file="+fl[i].getPath());
								}
							}
							btnCleanup.setVisibility(Button.GONE);
						}
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {}
				});
				commonDlg.showCommonDialog(true, "W", 
						mContext.getString(R.string.msgs_local_file_modified_maint_cleanup_btn), 
						mContext.getString(R.string.msgs_local_file_modified_maint_cleanup_msg), ntfy);
			}
		});
		// close ボタンの指定
		btnClose.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		dialog.setCancelable(false);
		dialog.show();
		
	};
	
	final static public void createLastModifiedMaintList(
			Context context, AdapterProfileList profile_adapter,
			ArrayList<LocalFileLastModifiedMaintListItem> maint_list) {
		final ArrayList<FileLastModifiedEntryItem> curr_list=new ArrayList<FileLastModifiedEntryItem>();
		final ArrayList<FileLastModifiedEntryItem> new_list=new ArrayList<FileLastModifiedEntryItem>();
		ArrayList<String> mpl=new ArrayList<String>();
		
		for (int i=0;i<profile_adapter.getCount();i++) {
			ProfileListItem syncprof_item =profile_adapter.getItem(i);
			if (syncprof_item.getProfileType().equals(SMBSYNC_PROF_TYPE_SYNC) &&
					syncprof_item.getTargetType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
				ProfileListItem lclprof_item=
						ProfileUtility.getProfile(syncprof_item.getTargetName(), profile_adapter);
				if (lclprof_item!=null) {
					String lmp=lclprof_item.getLocalMountPoint();
//					if (!lclprof_item.getDirectoryName().equals("")) lmp+="/"+lclprof_item.getDirectoryName();
//					Log.v("","lmp="+lmp);
					if (!lmp.equals("/")&&!lmp.equals("")) {
						if (!isSetLastModifiedFunctional(lmp) ||
								syncprof_item.isForceLastModifiedUseSmbsync()) {
							boolean found=false;
							for (int j=0;j<mpl.size();j++) {
								if (lmp.equals(mpl.get(j))) {
									found=true;
									break;
								}
							}
							if (!found) {
								mpl.add(lmp);
//								Log.v("","add prof lmp="+lmp);
							}
						}
					}
				}
			}
		}
		
		File lf=new File(LocalMountPoint.getExternalStorageDir()+"/SMBSync/lflm/");
		File[] fl=lf.listFiles();
		if (fl!=null) {
			for (int i=0;i<fl.length;i++) {
				if (fl[i].isFile() &&
					fl[i].getName().startsWith(SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V3)) {
					String lmp=fl[i].getName().replace(SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V3, "").replaceAll("_", "/");
					boolean found=false;
					for (int j=0;j<mpl.size();j++) {
						if (mpl.get(j).equals(lmp)) {
							found=true;
							break;
						}
					}
					if (!found) {
						mpl.add(lmp+"\t"+"F");
//						Log.v("","add file lmp="+lmp);
					}
//				} else if (fl[i].isFile() &&
//						fl[i].getName().startsWith(SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V2)) {
//						String lmp=fl[i].getName().replace(SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V2, "").replaceAll("_", "/");
//						boolean found=false;
//						for (int j=0;j<mpl.size();j++) {
//							if (mpl.get(j).equals(lmp)) {
//								found=true;
//								break;
//							}
//						}
//						if (!found) {
//							mpl.add(lmp+"\t"+"F");
////							Log.v("","add file lmp="+lmp);
//						}
					}
			}
		}
		
		Collections.sort(mpl);
		
		for (int i=0;i<mpl.size();i++) {
			String[] lmp=mpl.get(i).split("\t");
			String st="";
			if (lmp.length!=1) {
				st=context.getString(R.string.msgs_local_file_modified_maint_status_fileonly);
				maint_list.add(new LocalFileLastModifiedMaintListItem(lmp[0],st,false));
//				Log.v("","add maint file lmp="+lmp[0]);
			} else {
				boolean corrupted=loadLastModifiedList(lmp[0], curr_list,new_list);
				if (lmp.length!=1) {
				} else {
					if (corrupted) {
						st=context.getString(R.string.msgs_local_file_modified_maint_status_corrupted);
					} else {
						if (curr_list.size()==1 && 
								curr_list.get(0).getFullFilePath().equals(SMBSYNC_LOCAL_FILE_LAST_MODIFIED_WAS_FORCE_LASTEST)) {
							st=context.getString(R.string.msgs_local_file_modified_maint_status_reset);
						} else if (curr_list.size()==0) {
							File t_lf=new File(getFillePathFromLmpName(lmp[0]));
							Log.v("","lmp="+t_lf.getPath()+", e="+t_lf.exists()); 
							if (t_lf.exists()) {
								st=context.getString(R.string.msgs_local_file_modified_maint_status_init);
							} else {
								st=context.getString(R.string.msgs_local_file_modified_maint_status_not_created);
							}
						} else {
							st=context.getString(R.string.msgs_local_file_modified_maint_status_valid);
						}
					}
				}
				maint_list.add(new LocalFileLastModifiedMaintListItem(lmp[0],st,false));
//				Log.v("","add maint prof lmp="+lmp[0]);
			}
		}
	};
	
	final public static void deleteListFile(String lmp) {
		File lf=new File(getFillePathFromLmpName(lmp));
		lf.delete();
	};
	
	final public static String getFillePathFromLmpName(String lmp) {
		return LocalMountPoint.getExternalStorageDir()+"/SMBSync/lflm/"+
				SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V3+lmp.replaceAll("/", "_");
	}
	
	final public static boolean isLastModifiedWasUsed(AdapterProfileList profile_adapter) {
		boolean usable=false;
		for (int i=0;i<profile_adapter.getCount();i++) {
			ProfileListItem syncprof_item =profile_adapter.getItem(i);
			if (syncprof_item.getProfileType().equals(SMBSYNC_PROF_TYPE_SYNC) &&
					syncprof_item.getTargetType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
				ProfileListItem lclprof_item=
						ProfileUtility.getProfile(syncprof_item.getTargetName(), profile_adapter);
				if (lclprof_item!=null) {
					String lmp=lclprof_item.getLocalMountPoint();
					if (!lclprof_item.getDirectoryName().equals("")) lmp+="/"+lclprof_item.getDirectoryName();
//					Log.v("","lmp="+lmp);
					if (!lmp.equals("/")&&!lmp.equals("")) {
						if (!isSetLastModifiedFunctional(lmp) ||
								syncprof_item.isForceLastModifiedUseSmbsync()) {
							usable=true;
							break;
						}
					}
				}
			}
		}
		return usable;
	};

	final public static boolean isCurrentListWasDifferent(
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list,
			String fp, long l_lm, long r_lm, int timeDifferenceLimit) {
		boolean result=false;
		if (curr_last_modified_list.size()!=0) {
			if (curr_last_modified_list.get(0).getFullFilePath().equals(SMBSYNC_LOCAL_FILE_LAST_MODIFIED_WAS_FORCE_LASTEST)) {
				result=false;
				addLastModifiedItem(
						curr_last_modified_list,new_last_modified_list,fp,l_lm,r_lm);
			} else {
				int idx=Collections.binarySearch(curr_last_modified_list, 
						new FileLastModifiedEntryItem(fp,0,0,false), 
					new Comparator<FileLastModifiedEntryItem>(){
					@Override
					public int compare(FileLastModifiedEntryItem ci,
							FileLastModifiedEntryItem ni) {
						return ci.getFullFilePath().compareToIgnoreCase(ni.getFullFilePath());
					}
				});
//				Log.v("","idx="+idx+", fp="+fp);
				if (idx>=0) {
					long diff_lcl=Math.abs(curr_last_modified_list.get(idx).getLocalFileLastModified()-l_lm);
					long diff_rmt=Math.abs(curr_last_modified_list.get(idx).getRemoteFileLastModified()-r_lm);
					if (diff_lcl>timeDifferenceLimit || diff_rmt>timeDifferenceLimit) {
//						Log.v("","list l_lm="+curr_last_modified_list.get(idx).getLocalFileLastModified()+", r_lm="+curr_last_modified_list.get(idx).getRemoteFileLastModified());
//						Log.v("","file l_lm="+l_lm+", r_lm="+r_lm);
						result=true;
					}
					curr_last_modified_list.get(idx).setReferenced(true);
				} else {
//					Log.v("","added file l_lm="+l_lm+", r_lm="+r_lm);
					result=isAddedListWasDifferent(
							curr_last_modified_list,new_last_modified_list,
							fp,l_lm,r_lm,timeDifferenceLimit);
				}
			}
		} else {
//			Log.v("","added fp="+fp);
			result=isAddedListWasDifferent(
					curr_last_modified_list,new_last_modified_list,
					fp,l_lm,r_lm,timeDifferenceLimit);
		}
		return result;
	};
	
	final public static boolean isAddedListWasDifferent(
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list,
			String fp, long l_lm, long r_lm, int timeDifferenceLimit) {
		boolean result=true, found=false;
		if (new_last_modified_list.size()==0) result=true;
		else for (FileLastModifiedEntryItem fli : new_last_modified_list) {
			if (fli.getFullFilePath().equals(fp)) {
				found=true;
				long diff_lcl=Math.abs(fli.getLocalFileLastModified()-l_lm);
				long diff_rmt=Math.abs(fli.getRemoteFileLastModified()-r_lm);
				if (diff_lcl<=1 || diff_rmt<=1) result=false;
				else result=true;
				break;
			}
		}
		if (!found) addLastModifiedItem(
				curr_last_modified_list,new_last_modified_list,fp,l_lm,r_lm);
//		Log.v("","isAddedListWasDifferent="+result+", added fp="+fp);
		return result;
	};
		
	final public static void addLastModifiedItem(
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list,
			String fp, long l_lm, long r_lm){
//		Thread.dumpStack();
		FileLastModifiedEntryItem fli= new FileLastModifiedEntryItem
				(fp,l_lm,r_lm,true);
		new_last_modified_list.add(fli);
	};
	
	final public static void deleteLastModifiedItem(
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list,
			String fp){
		int idx=Collections.binarySearch(curr_last_modified_list, 
				new FileLastModifiedEntryItem(fp,0,0,false), 
			new Comparator<FileLastModifiedEntryItem>(){
			@Override
			public int compare(FileLastModifiedEntryItem ci,
					FileLastModifiedEntryItem ni) {
				return ci.getFullFilePath().compareToIgnoreCase(ni.getFullFilePath());
			}
		});
		if (idx>=0) curr_last_modified_list.remove(idx);
		else for (FileLastModifiedEntryItem fli : new_last_modified_list) {
			if (fli.getFullFilePath().equals(fp)) {
				new_last_modified_list.remove(fli);
				break;
			}
		}  
	};
	
	final public static boolean updateLastModifiedList(
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list,
			String targetUrl, long r_lm) {
		boolean result=false;
		File lf=new File(targetUrl);
		int idx=Collections.binarySearch(curr_last_modified_list, 
				new FileLastModifiedEntryItem(targetUrl,0,0,false), 
			new Comparator<FileLastModifiedEntryItem>(){
			@Override
			public int compare(FileLastModifiedEntryItem ci,
					FileLastModifiedEntryItem ni) {
				return ci.getFullFilePath().compareToIgnoreCase(ni.getFullFilePath());
			}
		});
		if (idx>=0) {
			curr_last_modified_list.get(idx).setLocalFileLastModified(lf.lastModified());
			curr_last_modified_list.get(idx).setRemoteFileLastModified(r_lm);
			curr_last_modified_list.get(idx).setReferenced(true);
			result=true;
		} else for (FileLastModifiedEntryItem fli : new_last_modified_list) {
			if (fli.getFullFilePath().equals(targetUrl)) {
				fli.setLocalFileLastModified(lf.lastModified());
				fli.setRemoteFileLastModified(r_lm);
				result=true;
				break;
			}
		}
		return result;
		
	};
	
	final public static boolean isSetLastModifiedFunctional(String lmp) {
		boolean result=false;
		File lf=new File(lmp+"/"+"SMBSyncLastModifiedTest.temp");
		File dir=new File(lmp+"/");
		try {
			if (dir.canWrite()) {
				if (lf.exists()) lf.delete();
				lf.createNewFile();
				result=lf.setLastModified(0);
				lf.delete();
			}
		} catch (IOException e) {
//			e.printStackTrace();
		}
//		Log.v("","result="+result+", lmp="+lmp);
		return result;
	};

	
	final public static void saveLastModifiedList(String lmp,
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list
			) {
//		long b_time=System.currentTimeMillis();
		ArrayList<FileLastModifiedEntryItem> save_last_modified_list=new ArrayList<FileLastModifiedEntryItem>();
		save_last_modified_list.addAll(curr_last_modified_list);
		if (save_last_modified_list.size()==1 && new_last_modified_list.size()!=0 &&
				save_last_modified_list.get(0).getFullFilePath()
				.equals(SMBSYNC_LOCAL_FILE_LAST_MODIFIED_WAS_FORCE_LASTEST))
			save_last_modified_list.remove(0);
//		Log.v("","size="+new_last_modified_list.size()+", name="+lmp);
		if (new_last_modified_list.size()!=0) {
			save_last_modified_list.addAll(new_last_modified_list);
			Collections.sort(save_last_modified_list, 
				new Comparator<FileLastModifiedEntryItem>(){
				@Override
				public int compare(FileLastModifiedEntryItem ci,
						FileLastModifiedEntryItem ni) {
					if (!ci.getFullFilePath().equals(ni.getFullFilePath()))
						return ci.getFullFilePath().compareToIgnoreCase(ni.getFullFilePath());
					else {
						String c_lt=Long.toString(ci.getLocalFileLastModified());
						String n_lt=Long.toString(ni.getLocalFileLastModified());
						return n_lt.compareTo(c_lt);
					}
				}
				});
		}
		try {
//			long b_time=System.currentTimeMillis();
			File lf_tmp=new File(LocalMountPoint.getExternalStorageDir()+"/SMBSync/lflm/");
			if (!lf_tmp.exists()) lf_tmp.mkdirs();
			String fn=SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V3+lmp.replace("/","_");
			lf_tmp=new File(LocalMountPoint.getExternalStorageDir()+"/SMBSync/lflm/"+fn+".tmp");
			lf_tmp.delete();
			
			File lf_save=new File(LocalMountPoint.getExternalStorageDir()+"/SMBSync/lflm/"+fn);
			
			FileOutputStream fos=new FileOutputStream(lf_tmp,false);
			BufferedOutputStream bos=new BufferedOutputStream(fos,1024*512);
			ZipOutputStream zos=new ZipOutputStream(bos);
			ZipEntry ze = new ZipEntry("list");
			zos.putNextEntry(ze);
			OutputStreamWriter osw=new OutputStreamWriter(zos,"UTF-8");
			BufferedWriter bw=new BufferedWriter(osw,1024*1024);
			
			StringBuffer pl=new StringBuffer(512);
			String last_fp="";
	    	String new_fp="";
//		    for (int i=0;i<save_last_modified_list.size();i++) {
//	    		LocalFileLastModifiedEntryItem lfme=save_last_modified_list.get(i);
		    for (FileLastModifiedEntryItem lfme : save_last_modified_list) {
		    	new_fp=lfme.getFullFilePath();
		    	if (!last_fp.equals(new_fp)) {
		    		boolean f_exists=true;
		    		if (!lfme.isReferenced()) {
			    		last_fp=new_fp;
			    		File slf=new File(last_fp);
			    		f_exists=slf.exists();
		    		}
		    		if (f_exists || new_fp.equals(SMBSYNC_LOCAL_FILE_LAST_MODIFIED_WAS_FORCE_LASTEST)) {
				    	pl.append(new_fp)
				    		.append("\t")
				    		.append(String.valueOf(lfme.getLocalFileLastModified()))
				    		.append("\t")
				    		.append(String.valueOf(lfme.getRemoteFileLastModified()))
				    		.append("\n");
				    	bw.append(pl);
				    	pl.setLength(0);
		    		} else {
//		    			Log.v(APPLICATION_TAG,"LocalMountPoint deleted entry, path="+new_fp);
		    		}
		    	} else {
//		    		Log.v(APPLICATION_TAG,"LocalMountPoint duplicate entry, path="+new_fp);
		    	}
		    }
//		    bw.flush();
		    bw.close();
		    lf_save.delete();
		    lf_tmp.renameTo(lf_save);
//		    Log.v("","save elapsed time="+(System.currentTimeMillis()-b_time));
		} catch (Exception e) {
			e.printStackTrace();
		}
	};

	final public static boolean isLastModifiedFileV2Exists(String lmp) {
		boolean exists=false;
		
		File lf=new File(LocalMountPoint.getExternalStorageDir()+"/SMBSync/lflm/"+
				SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V2+lmp.replace("/","_"));
		exists=lf.exists();
		
		return exists;
	};

	final public static boolean isLastModifiedFileV3Exists(String lmp) {
		boolean exists=false;
		
		File lf=new File(LocalMountPoint.getExternalStorageDir()+"/SMBSync/lflm/"+
				SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V3+lmp.replace("/","_"));
		exists=lf.exists();
		
		return exists;
	};

//	final public static boolean isLastModifiedFileV2Exists() {
//		boolean exists=false;
//		File lf=new File(LocalMountPoint.getExternalStorageDir()+"/SMBSync/lflm/");
//		File[] fl=lf.listFiles();
//		if (fl!=null) {
//			for (int i=0;i<fl.length;i++) {
//				if (fl[i].isFile() && fl[i].getName().startsWith(SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V2)) {
//					exists=true;
//					break;
//				}
//			}
//		}
//		return exists;
//	};

	final public static boolean isLastModifiedFileV1Exists() {
		boolean exists=false;
		File lf=new File(LocalMountPoint.getExternalStorageDir()+"/SMBSync/");
		File[] fl=lf.listFiles();
		if (fl!=null) {
			for (int i=0;i<fl.length;i++) {
				if (fl[i].isFile() && fl[i].getName().startsWith(SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V1)) {
					exists=true;
					break;
				}
			}
		}
		return exists;
	};

	final public static boolean loadLastModifiedList(String lmp,
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list
			) {
		boolean list_was_corrupted=false;
		curr_last_modified_list.clear();
		new_last_modified_list.clear();
		
		if (isLastModifiedFileV3Exists(lmp)) {//V3 found
			list_was_corrupted=
					loadLastModifiedListV3(lmp,curr_last_modified_list,
							new_last_modified_list);
		} else if (isLastModifiedFileV2Exists(lmp)) {//V2 found
				list_was_corrupted=
						loadLastModifiedListV2(lmp,curr_last_modified_list,
								new_last_modified_list);
			
		} else {//Assumed v1
			File lf=new File(LocalMountPoint.getExternalStorageDir()+"/SMBSync/");
			File[] fl=lf.listFiles();
			if (fl!=null) {
				for (int i=0;i<fl.length;i++) {
					if (fl[i].getName().startsWith(SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V1)) {
						String prof_sync=fl[i].getName().replace(SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V1, "");
						loadLastModifiedListV1(prof_sync,lmp,
								curr_last_modified_list,new_last_modified_list);
					}
				}
			}
		}
		
		return list_was_corrupted;
	};

	final public static boolean loadLastModifiedListV3(String lmp,
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list
			) {
		curr_last_modified_list.clear();
		new_last_modified_list.clear();
		boolean list_was_corrupted=false;
		try {
//			long b_time=System.currentTimeMillis();
			File lf=new File(LocalMountPoint.getExternalStorageDir()+"/SMBSync/lflm/"+
					SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V3+lmp.replace("/","_"));
			FileInputStream fis=new FileInputStream(lf);
//			BufferedInputStream bis=new BufferedInputStream(fis,1024*128*4);
			ZipInputStream zis=new ZipInputStream(fis);
			zis.getNextEntry();
			InputStreamReader isr=new InputStreamReader(zis,"UTF-8");
			BufferedReader br=new BufferedReader(isr,1024*1024);
		    String line=null;
		    String[] l_array=null;
		    String last_fp="";
		    while ((line = br.readLine()) != null) {
		    	l_array=line.split("\t");
		    	if (l_array!=null && l_array.length==3) {
//			    	Log.v("","line="+l_array[0]);
		    		if (!last_fp.equals(l_array[0])) {
		    			curr_last_modified_list.add(new FileLastModifiedEntryItem(
		    				l_array[0], Long.valueOf(l_array[1]),Long.valueOf(l_array[2]),false));
		    			last_fp=l_array[0];
		    		} else {
		    			list_was_corrupted=true;
		    		}
		    	} 
			}
		    br.close();
//		    Log.v("","load elapsed time="+(System.currentTimeMillis()-b_time));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list_was_corrupted;
	};

	final public static boolean loadLastModifiedListV2(String lmp,
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list
			) {
		curr_last_modified_list.clear();
		new_last_modified_list.clear();
		boolean list_was_corrupted=false;
		try {
			File lf=new File(LocalMountPoint.getExternalStorageDir()+"/SMBSync/lflm/"+
					SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V2+lmp.replace("/","_"));
		    FileReader fr = new FileReader(lf);
			BufferedReader br=new BufferedReader(fr,4096*128);
		    String line=null;
		    String[] l_array=null;
		    String last_fp="";
		    while ((line = br.readLine()) != null) {
		    	l_array=line.split("\t");
		    	if (l_array!=null && l_array.length==3) {
		    		if (!last_fp.equals(l_array[0])) {
		    			curr_last_modified_list.add(new FileLastModifiedEntryItem(
		    				l_array[0], Long.valueOf(l_array[1]),Long.valueOf(l_array[2]),false));
		    			last_fp=l_array[0];
		    		} else {
		    			list_was_corrupted=true;
		    		}
		    	} 
			}
		    br.close();
		    fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list_was_corrupted;
	};
	
	final public static boolean loadLastModifiedListV1(String prof_name, String lmp,
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list
			) {
		boolean list_was_corrupted=false;
		try {
			File lf=new File(LocalMountPoint.getExternalStorageDir()+"/SMBSync/"+
					SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V1+prof_name);
		    FileReader fr = new FileReader(lf);
			BufferedReader br=new BufferedReader(fr,4096*128);
		    String line=null;
		    String[] l_array=null;
		    String last_fp="";
		    while ((line = br.readLine()) != null) {
		    	l_array=line.split("\t");
		    	if (l_array!=null && l_array.length==3) {
		    		if (l_array[0].startsWith(lmp)) {
			    		if (!last_fp.equals(l_array[0])) {
			    			curr_last_modified_list.add(new FileLastModifiedEntryItem(
			    				l_array[0], Long.valueOf(l_array[1]),Long.valueOf(l_array[2]),false));
			    			last_fp=l_array[0];
			    		} else {
//			    			list_was_corrupted=true;
			    		}
		    		}
		    	} 
			}
		    br.close();
		    fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list_was_corrupted;
	};
	
}

class FileLastModifiedEntryItem implements Externalizable{
	private static final long serialVersionUID = 1L;
	private String full_file_path="";
	private long local_last_modified_time=0;
	private long remote_last_modified_time=0;
	private boolean referenced=false;
	
	FileLastModifiedEntryItem(String fp, long l_lm, long r_lm, boolean ref) {
		full_file_path=fp;
		local_last_modified_time=l_lm;
		remote_last_modified_time=r_lm;
		referenced=ref;
	}
	
	public boolean isReferenced() {return referenced;}
	public void setReferenced(boolean p) {referenced=p;}
	public String getFullFilePath() {return full_file_path;}
	public long getLocalFileLastModified() { return local_last_modified_time;}
	public long getRemoteFileLastModified() { return remote_last_modified_time;}

	public void setFullFilePath(String p) {full_file_path=p;}
	public void setLocalFileLastModified(long p) { local_last_modified_time=p;}
	public void setRemoteFileLastModified(long p) { remote_last_modified_time=p;}

	@Override
	public void readExternal(ObjectInput input) throws IOException,
			ClassNotFoundException {
		full_file_path=input.readUTF();
		local_last_modified_time=input.readLong();
		remote_last_modified_time=input.readLong();
	};

	@Override
	public void writeExternal(ObjectOutput output) throws IOException {
		output.writeUTF(full_file_path);
		output.writeLong(local_last_modified_time);
		output.writeLong(remote_last_modified_time);
	};
}
