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
import static com.sentaroh.android.SMBSync.SchedulerConstants.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.sentaroh.android.Utilities.Base64Compat;
import com.sentaroh.android.Utilities.EncryptUtil;
import com.sentaroh.android.Utilities.EncryptUtil.CipherParms;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.SafUtil;
import com.sentaroh.android.Utilities.SafCommonArea;
import com.sentaroh.android.Utilities.ThreadCtrl;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenu;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.Dialog.DialogBackKeyListener;
import com.sentaroh.android.Utilities.TreeFilelist.TreeFilelistAdapter;
import com.sentaroh.android.Utilities.TreeFilelist.TreeFilelistItem;
import com.sentaroh.android.Utilities.Widget.CustomSpinnerAdapter;

public class ProfileUtility {

//	private CustomContextMenu ccMenu=null;
	private String smbUser,smbPass;
	
	private Context mContext;
	
	private SMBSyncUtil util;
	
	private ArrayList<PreferenceParmListIItem> 
		importedSettingParmList=new ArrayList<PreferenceParmListIItem>();
	
	private CommonDialog commonDlg=null;
	private GlobalParameters mGp=null;
	private FragmentManager mFragMgr=null;

	private SafCommonArea mSafCA=new SafCommonArea();

	ProfileUtility (SMBSyncUtil mu, Context c,  
			CommonDialog cd, CustomContextMenu ccm, GlobalParameters gp, FragmentManager fm) {
		mContext=c;
		mGp=gp;
		util=mu;
		loadMsgString();
		commonDlg=cd;
//		ccMenu=ccm;
		mFragMgr=fm;
    	SafUtil.initWorkArea(mContext, mSafCA, gp.debugLevel>0);
	};

	public void importProfileDlg(final String lurl, final String ldir, 
			String file_name, final NotifyEvent p_ntfy) {
		
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
    			final String fpath=(String)o[0];
    			
    			NotifyEvent ntfy_pswd=new NotifyEvent(mContext);
    			ntfy_pswd.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						mGp.profilePassword=(String)o[0];
		    			final AdapterProfileList tfl = createProfileList(true,fpath);
		    			if (tfl==null) {
			    			commonDlg.showCommonDialog(false,"E",
			    					String.format(msgs_import_prof_fail,fpath),"",null);
		    			} else {
			    			ProfileListItem pli=tfl.getItem(0);
		    				if (tfl.getCount()==1 && pli.getProfileType().equals("")) {
		    	    			commonDlg.showCommonDialog(false,"E",
		    	    					String.format(msgs_import_prof_fail_no_valid_item,fpath),"",null);
		    				} else {
		    					selectImportProfileItem(tfl, p_ntfy);
		    				}
		    			}
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
    			});
    			if (isProfileWasEncrypted(fpath)) {
    				promptPasswordForImport(fpath,ntfy_pswd);
    			} else ntfy_pswd.notifyToListener(true, new Object[]{""});
			}
			@Override
			public void negativeResponse(Context c,Object[] o) {
			}
		});
		commonDlg.fileOnlySelectWithoutCreate(
				lurl,ldir,file_name,msgs_select_import_file,ntfy);
	};
	
	static private boolean isProfileWasEncrypted(String fpath) {
		boolean result=false;
		File lf=new File(fpath);
		if (lf.exists() && lf.canRead()) {
			try {
				BufferedReader br;
				br = new BufferedReader(new FileReader(fpath),8192);
				String pl = br.readLine();
				if (pl!=null) {
					if (pl.startsWith(SMBSYNC_PROF_VER1) || pl.startsWith(SMBSYNC_PROF_VER2)) {
						//NOtencrypted
					} else if (pl.startsWith(SMBSYNC_PROF_VER3)) {
						if (pl.startsWith(SMBSYNC_PROF_VER3+SMBSYNC_PROF_ENC)) result=true;
					} else if (pl.startsWith(SMBSYNC_PROF_VER4)) {
						if (pl.startsWith(SMBSYNC_PROF_VER4+SMBSYNC_PROF_ENC)) result=true;
					} else if (pl.startsWith(SMBSYNC_PROF_VER5)) {
						if (pl.startsWith(SMBSYNC_PROF_VER5+SMBSYNC_PROF_ENC)) result=true;
					} else if (pl.startsWith(SMBSYNC_PROF_VER6)) {
						if (pl.startsWith(SMBSYNC_PROF_VER6+SMBSYNC_PROF_ENC)) result=true;
					} else if (pl.startsWith(SMBSYNC_PROF_VER7)) {
						if (pl.startsWith(SMBSYNC_PROF_VER7+SMBSYNC_PROF_ENC)) result=true;
					} else if (pl.startsWith(SMBSYNC_PROF_VER8)) {
						if (pl.startsWith(SMBSYNC_PROF_VER8+SMBSYNC_PROF_ENC)) result=true;
					}
				}
				br.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	};
	
	public void promptPasswordForImport(final String fpath,  
			final NotifyEvent ntfy_pswd) {
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.password_input_dlg);
		
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.password_input_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.password_input_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.password_input_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		
		final TextView dlg_msg = (TextView) dialog.findViewById(R.id.password_input_msg);
		final CheckedTextView ctv_protect = (CheckedTextView) dialog.findViewById(R.id.password_input_ctv_protect);
		final Button btn_ok = (Button) dialog.findViewById(R.id.password_input_ok_btn);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.password_input_cancel_btn);
		final EditText et_password=(EditText) dialog.findViewById(R.id.password_input_password);
		final EditText et_confirm=(EditText) dialog.findViewById(R.id.password_input_password_confirm);
		et_confirm.setVisibility(EditText.GONE);
		btn_ok.setText(mContext.getString(R.string.msgs_export_import_pswd_btn_ok));
		ctv_protect.setVisibility(CheckedTextView.GONE);
		
		dlg_msg.setText(mContext.getString(R.string.msgs_export_import_pswd_password_required));
		
		CommonDialog.setDlgBoxSizeCompact(dialog);
		
		btn_ok.setEnabled(false);
		et_password.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
				if (arg0.length()>0) btn_ok.setEnabled(true);
				else btn_ok.setEnabled(false);
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {}
		});

		//OK button
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String passwd=et_password.getText().toString();
				BufferedReader br;
				String pl;
				boolean pswd_invalid=true;
				try {
					br = new BufferedReader(new FileReader(fpath),8192);
					pl = br.readLine();
					if (pl!=null) {
						String enc_str="";
						if (pl.startsWith(SMBSYNC_PROF_VER3+SMBSYNC_PROF_ENC)) {
							enc_str=pl.replace(SMBSYNC_PROF_VER3+SMBSYNC_PROF_ENC, "");
						} else if (pl.startsWith(SMBSYNC_PROF_VER4+SMBSYNC_PROF_ENC)) {
							enc_str=pl.replace(SMBSYNC_PROF_VER4+SMBSYNC_PROF_ENC, "");
						} else if (pl.startsWith(SMBSYNC_PROF_VER5+SMBSYNC_PROF_ENC)) {
							enc_str=pl.replace(SMBSYNC_PROF_VER5+SMBSYNC_PROF_ENC, "");
						} else if (pl.startsWith(SMBSYNC_PROF_VER6+SMBSYNC_PROF_ENC)) {
							enc_str=pl.replace(SMBSYNC_PROF_VER6+SMBSYNC_PROF_ENC, "");
						} else if (pl.startsWith(SMBSYNC_PROF_VER7+SMBSYNC_PROF_ENC)) {
							enc_str=pl.replace(SMBSYNC_PROF_VER7+SMBSYNC_PROF_ENC, "");
						} else if (pl.startsWith(SMBSYNC_PROF_VER8+SMBSYNC_PROF_ENC)) {
							enc_str=pl.replace(SMBSYNC_PROF_VER8+SMBSYNC_PROF_ENC, "");
						}
						if (!enc_str.equals("")) {
							CipherParms cp=EncryptUtil.initDecryptEnv(
									mGp.profileKeyPrefix+passwd);
							byte[] enc_array=Base64Compat.decode(enc_str, Base64Compat.NO_WRAP);
							String dec_str=EncryptUtil.decrypt(enc_array, cp);
							if (!SMBSYNC_PROF_ENC.equals(dec_str)) {
								dlg_msg.setText(mContext.getString(R.string.msgs_export_import_pswd_invalid_password));
							} else {
								pswd_invalid=false;
							}
						}
					}
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (!pswd_invalid) {
					dialog.dismiss();
					ntfy_pswd.notifyToListener(true, new Object[] {passwd});
				}
			}
		});
		// CANCELボタンの指定
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				ntfy_pswd.notifyToListener(false, null);
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
//		dialog.setCancelable(false);
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
		dialog.show();

	};

	public void promptPasswordForExport(final String fpath,  
			final NotifyEvent ntfy_pswd) {
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.password_input_dlg);
		
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.password_input_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.password_input_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.password_input_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);

		final TextView dlg_msg = (TextView) dialog.findViewById(R.id.password_input_msg);
		final CheckedTextView ctv_protect = (CheckedTextView) dialog.findViewById(R.id.password_input_ctv_protect);
		final Button btn_ok = (Button) dialog.findViewById(R.id.password_input_ok_btn);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.password_input_cancel_btn);
		final EditText et_password=(EditText) dialog.findViewById(R.id.password_input_password);
		final EditText et_confirm=(EditText) dialog.findViewById(R.id.password_input_password_confirm);
		
		dlg_msg.setText(mContext.getString(R.string.msgs_export_import_pswd_specify_password));
		
		CommonDialog.setDlgBoxSizeCompact(dialog);

		ctv_protect.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctv_protect.toggle();
				boolean isChecked=ctv_protect.isChecked();
				setPasswordFieldVisibility(isChecked, et_password,
						et_confirm, btn_ok, dlg_msg);
			}
		});

		ctv_protect.setChecked(mGp.settingExportedProfileEncryptRequired);
		setPasswordFieldVisibility(mGp.settingExportedProfileEncryptRequired,
				et_password, et_confirm, btn_ok, dlg_msg);

		et_password.setEnabled(true);
		et_confirm.setEnabled(false);
		et_password.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
				btn_ok.setEnabled(false);
				setPasswordPromptOkButton(et_password, et_confirm, 
						btn_ok, dlg_msg);
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {}
		});

		et_confirm.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
				btn_ok.setEnabled(false);
				setPasswordPromptOkButton(et_password, et_confirm, 
						btn_ok, dlg_msg);
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {}
		});

		//OK button
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String passwd=et_password.getText().toString();
				if ((ctv_protect.isChecked() && !mGp.settingExportedProfileEncryptRequired) ||
						(!ctv_protect.isChecked() && mGp.settingExportedProfileEncryptRequired)) {
					mGp.settingExportedProfileEncryptRequired=ctv_protect.isChecked();
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
					prefs.edit().putBoolean(mContext.getString(R.string.settings_exported_profile_encryption), 
							ctv_protect.isChecked()).commit();
				}
				if (!ctv_protect.isChecked()) {
					dialog.dismiss();
					ntfy_pswd.notifyToListener(true, new Object[] {""});
				} else {
					if (!passwd.equals(et_confirm.getText().toString())) {
						//Unmatch
						dlg_msg.setText(mContext.getString(R.string.msgs_export_import_pswd_unmatched_confirm_pswd));
					} else {
						dialog.dismiss();
						ntfy_pswd.notifyToListener(true, new Object[] {passwd});
					}
				}
			}
		});
		// CANCELボタンの指定
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				ntfy_pswd.notifyToListener(false, null);
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
//		dialog.setCancelable(false);
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
		dialog.show();

	};

	private void setPasswordFieldVisibility(boolean isChecked, EditText et_password,
			EditText et_confirm, Button btn_ok, TextView dlg_msg) {
		if (isChecked) {
			et_password.setVisibility(EditText.VISIBLE);
			et_confirm.setVisibility(EditText.VISIBLE);
			setPasswordPromptOkButton(et_password, et_confirm, 
					btn_ok, dlg_msg);
		} else {
			dlg_msg.setText("");
			et_password.setVisibility(EditText.GONE);
			et_confirm.setVisibility(EditText.GONE);
			btn_ok.setEnabled(true);
		}
	};
	
	private void setPasswordPromptOkButton(EditText et_passwd, EditText et_confirm, 
			Button btn_ok, TextView dlg_msg) {
		String password=et_passwd.getText().toString();
		String confirm=et_confirm.getText().toString();
		if (password.length()>0 && et_confirm.getText().length()==0) {
			dlg_msg.setText(mContext.getString(R.string.msgs_export_import_pswd_unmatched_confirm_pswd));
			et_confirm.setEnabled(true);
		} else if (password.length()>0 && et_confirm.getText().length()>0) {
			et_confirm.setEnabled(true);
			if (!password.equals(confirm)) {
				btn_ok.setEnabled(false);
				dlg_msg.setText(mContext.getString(R.string.msgs_export_import_pswd_unmatched_confirm_pswd));
			} else {
				btn_ok.setEnabled(true);
				dlg_msg.setText("");
			}
		} else if (password.length()==0 && confirm.length()==0) {
			btn_ok.setEnabled(false);
			dlg_msg.setText(mContext.getString(R.string.msgs_export_import_pswd_specify_password));
			et_passwd.setEnabled(true);
			et_confirm.setEnabled(false);
		} else if (password.length()==0 && confirm.length()>0) {
			dlg_msg.setText(mContext.getString(R.string.msgs_export_import_pswd_unmatched_confirm_pswd));
		}

	};
	
	private void selectImportProfileItem(final AdapterProfileList tfl, final NotifyEvent p_ntfy) {
		final Dialog dialog=new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.export_import_profile_dlg);
		dialog.setCanceledOnTouchOutside(false);

		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.export_import_profile_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		ArrayList<ExportImportProfileListItem> eipl=new ArrayList<ExportImportProfileListItem>();

		for (int i=0;i<tfl.getCount();i++) {
			ProfileListItem pl=tfl.getItem(i);
			ExportImportProfileListItem eipli=new ExportImportProfileListItem();
			eipli.isChecked=true;
			eipli.item_type=pl.getProfileType();
			eipli.item_name=pl.getProfileName();
			eipl.add(eipli);
		}
//		Collections.sort(eipl, new Comparator<ExportImportProfileListItem>(){
//			@Override
//			public int compare(ExportImportProfileListItem arg0,
//					ExportImportProfileListItem arg1) {
//				if (arg0.item_name.equals(arg1.item_name)) return arg0.item_type.compareToIgnoreCase(arg1.item_type);
//				return arg0.item_name.compareToIgnoreCase(arg1.item_name);
//			}
//		});
//		ExportImportProfileListItem eipli=new ExportImportProfileListItem();
//		eipli.isChecked=true;
//		eipli.item_type="*";
//		eipli.item_name=mContext.getString(R.string.msgs_export_import_profile_setting_parms);
//		eipl.add(eipli);
		final AdapterExportImportProfileList imp_list_adapt=
				new AdapterExportImportProfileList(mContext, R.layout.export_import_profile_list_item, eipl);
		
		ListView lv=(ListView)dialog.findViewById(R.id.export_import_profile_listview);
		lv.setAdapter(imp_list_adapt);
		
		CommonDialog.setDlgBoxSizeLimit(dialog,true);
		
		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.export_import_profile_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.export_import_profile_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		title.setText(mContext.getString(R.string.msgs_export_import_profile_title));
//		TextView tv_msgx=(TextView)dialog.findViewById(R.id.export_import_profile_msg);
//		tv_msgx.setVisibility(LinearLayout.GONE);
		LinearLayout ll_filelist=(LinearLayout)dialog.findViewById(R.id.export_import_profile_file_list);
		ll_filelist.setVisibility(LinearLayout.GONE);
		final Button ok_btn=(Button)dialog.findViewById(R.id.export_import_profile_dlg_btn_ok);
		Button cancel_btn=(Button)dialog.findViewById(R.id.export_import_profile_dlg_btn_cancel);
		
		final Button rb_select_all=(Button)dialog.findViewById(R.id.export_import_profile_list_select_all);
		final Button rb_unselect_all=(Button)dialog.findViewById(R.id.export_import_profile_list_unselect_all);
		final CheckedTextView ctv_reset_profile=(CheckedTextView)dialog.findViewById(R.id.export_import_profile_list_ctv_reset_profile);
		final CheckedTextView ctv_import_settings=(CheckedTextView)dialog.findViewById(R.id.export_import_profile_list_ctv_import_settings);
		final CheckedTextView ctv_import_schedule=(CheckedTextView)dialog.findViewById(R.id.export_import_profile_list_ctv_import_schedule);
		
		ctv_reset_profile.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				((CheckedTextView)v).toggle();
				setImportOkBtnEnabled(ctv_reset_profile,ctv_import_settings,ctv_import_schedule,imp_list_adapt,ok_btn);
			}
		});
		
		ctv_import_settings.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				((CheckedTextView)v).toggle();
				setImportOkBtnEnabled(ctv_reset_profile,ctv_import_settings,ctv_import_schedule,imp_list_adapt,ok_btn);
			}
		});

		ctv_import_schedule.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				((CheckedTextView)v).toggle();
				setImportOkBtnEnabled(ctv_reset_profile,ctv_import_settings,ctv_import_schedule,imp_list_adapt,ok_btn);
			}
		});

		ctv_import_settings.setChecked(true);
		ctv_import_schedule.setChecked(true);
		
		lv.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
//				  imp_list_adapt.getItem(pos).isChecked=!imp_list_adapt.getItem(pos).isChecked;
//				  imp_list_adapt.notifyDataSetChanged();
//				  if (imp_list_adapt.isItemSelected()) {
//					  ok_btn.setEnabled(true);
//				  } else {
//					  ok_btn.setEnabled(false);
//				  }
				  setImportOkBtnEnabled(ctv_reset_profile,ctv_import_settings,ctv_import_schedule,imp_list_adapt,ok_btn);
			}
		});
		
//		lv.setOnItemLongClickListener(new OnItemLongClickListener(){
//			@Override
//			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
//					int pos, long arg3) {
//				ccMenu.addMenuItem(
//						mContext.getString(R.string.msgs_export_import_profile_select_all),R.drawable.blank)
//			  	.setOnClickListener(new CustomContextMenuOnClickListener() {
//				  @Override
//				  final public void onClick(CharSequence menuTitle) {
//					  for (int i=0;i<imp_list_adapt.getCount();i++)
//						  imp_list_adapt.getItem(i).isChecked=true;
//					  imp_list_adapt.notifyDataSetChanged();
//					  ok_btn.setEnabled(true);
//				  	}
//			  	});
//				ccMenu.addMenuItem(
//						mContext.getString(R.string.msgs_export_import_profile_unselect_all),R.drawable.blank)
//			  	.setOnClickListener(new CustomContextMenuOnClickListener() {
//				  @Override
//				  final public void onClick(CharSequence menuTitle) {
//					  for (int i=0;i<imp_list_adapt.getCount();i++)
//						  imp_list_adapt.getItem(i).isChecked=false;
//					  imp_list_adapt.notifyDataSetChanged();
//					  ok_btn.setEnabled(false);
//				  	}
//			  	});
//				ccMenu.createMenu();
//				return false;
//			}
//		});
		
		rb_select_all.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				for (int i=0;i<imp_list_adapt.getCount();i++)
					  imp_list_adapt.getItem(i).isChecked=true;
				ctv_import_settings.setChecked(true);
				ctv_import_schedule.setChecked(true);
				imp_list_adapt.notifyDataSetChanged();
				ok_btn.setEnabled(true);
			}
		});
		rb_unselect_all.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				for (int i=0;i<imp_list_adapt.getCount();i++)
					  imp_list_adapt.getItem(i).isChecked=false;
				ctv_import_settings.setChecked(false);
				ctv_import_schedule.setChecked(false);
				imp_list_adapt.notifyDataSetChanged();
				ok_btn.setEnabled(false);
			}
		});
		
		NotifyEvent ntfy_ctv_listener=new NotifyEvent(mContext);
		ntfy_ctv_listener.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
//				  if (imp_list_adapt.isItemSelected()) {
//					  ok_btn.setEnabled(true);
//				  } else {
//					  if (ctv_import_settings.isChecked()) ok_btn.setEnabled(true);
//					  else ok_btn.setEnabled(false);
//				  }
				setImportOkBtnEnabled(ctv_reset_profile,ctv_import_settings,ctv_import_schedule,imp_list_adapt,ok_btn);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		imp_list_adapt.setCheckButtonListener(ntfy_ctv_listener);
		
		
		ok_btn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (ctv_reset_profile.isChecked()) mGp.profileAdapter.clear();
				else {
					if (mGp.profileAdapter.getCount()==1 && mGp.profileAdapter.getItem(0).getProfileType().equals("")) {
						mGp.profileAdapter.clear();
					}
				}
				importSelectedProfileItem(imp_list_adapt,tfl,
						ctv_import_settings.isChecked(),
						ctv_import_schedule.isChecked(),
						p_ntfy);
				dialog.dismiss();
			}
		});
		cancel_btn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				dialog.dismiss();
			}
		});
		
		dialog.show();
		
	};
	
	private void setImportOkBtnEnabled(
			final CheckedTextView ctv_reset_profile,
			final CheckedTextView ctv_import_settings,
			final CheckedTextView ctv_import_schedule,
			final AdapterExportImportProfileList imp_list_adapt,
			final Button ok_btn) {
		if (ctv_import_settings.isChecked() || ctv_import_schedule.isChecked() || imp_list_adapt.isItemSelected()) ok_btn.setEnabled(true);
		else ok_btn.setEnabled(false);
	};
	
	private void importSelectedProfileItem(
			final AdapterExportImportProfileList imp_list_adapt,
			final AdapterProfileList tfl, 
			final boolean import_settings,
			final boolean import_schedule,
			final NotifyEvent p_ntfy) {
		String repl_list="";
		for (int i=0;i<imp_list_adapt.getCount();i++) {
			ExportImportProfileListItem eipli=imp_list_adapt.getItem(i);
			if (eipli.isChecked &&
					getProfile(eipli.item_name, mGp.profileAdapter)!=null) {
				repl_list+=eipli.item_name+"\n";
			}
		}
		
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				String imp_list="";
				for (int i=0;i<tfl.getCount();i++) {
					ProfileListItem ipfli=tfl.getItem(i);
					ExportImportProfileListItem eipli=imp_list_adapt.getItem(i);
					if (eipli.isChecked ) {
						imp_list+=ipfli.getProfileName()+"\n";
//						Log.v("","name1="+ipfli.getName()+
//								", result="+getProfile(ipfli.getName(), mGp.profileAdapter));
						ProfileListItem mpfli=getProfile(ipfli.getProfileName(), mGp.profileAdapter);
						if (mpfli!=null) {
							mGp.profileAdapter.remove(mpfli);
							mGp.profileAdapter.add(ipfli);
						} else {
							mGp.profileAdapter.add(ipfli);
						}
					}
				}
				resolveSyncProfileRelation(mGp);
//				ExportImportProfileListItem eipli=imp_list_adapt.getItem(imp_list_adapt.getCount()-1);
				restoreImportedSystemOption();
				if (import_settings) {
					restoreImportedSettingParms();
					imp_list+=mContext.getString(R.string.msgs_export_import_profile_setting_parms)+"\n";
				}
				if (import_schedule) {
					restoreImportedScheduleParms();
					imp_list+=mContext.getString(R.string.msgs_export_import_profile_schedule_parms)+"\n";
				}
				if (imp_list.length()>0) imp_list+=" ";
				mGp.profileAdapter.sort();
				mGp.profileListView.setSelection(0);
				saveProfileToFile(mGp, mContext, util, false,"","",mGp.profileAdapter,false);
				commonDlg.showCommonDialog(false,"I",
						mContext.getString(R.string.msgs_export_import_profile_import_success),
						imp_list,null); 
				if (import_settings || import_schedule) {
					boolean[] parm=new boolean[] {import_settings, import_schedule};
					p_ntfy.notifyToListener(true, new Object[] {parm});
				}
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		if (!repl_list.equals("")) {
			//Confirm
			commonDlg.showCommonDialog(true,"W",
					mContext.getString(R.string.msgs_export_import_profile_confirm_override),
					repl_list,ntfy); 
		} else {
			ntfy.notifyToListener(true, null);
		}
		
	};

	private void restoreImportedSystemOption() {
		final ArrayList<PreferenceParmListIItem> spl=importedSettingParmList;
		
		if (spl.size()==0) {
			util.addDebugLogMsg(2,"I","Import setting parms can not be not found.");
			return;
		}
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		final Editor pe= prefs.edit();
		
		if (spl.size()>=0) {
			for (int i=0;i<spl.size();i++) {
				if (spl.get(i).parms_key.startsWith("system_rest")) {
					restorePreferenceParms(pe,spl.get(i));
				}
			}
			pe.commit();
//			applySettingParms();
		}
	};

	private void restorePreferenceParms(Editor pe, PreferenceParmListIItem pa) {
		if (pa.parms_type.equals(SMBSYNC_SETTINGS_TYPE_STRING)) {
			pe.putString(pa.parms_key,pa.parms_value);
			util.addDebugLogMsg(2,"I","Restored parms="+pa.parms_key+"="+pa.parms_value);
		} else if (pa.parms_type.equals(SMBSYNC_SETTINGS_TYPE_BOOLEAN)) {
			boolean b_val = false;
			if (pa.parms_value.equals("false")) b_val = false;
			else b_val = true;
			pe.putBoolean(pa.parms_key,b_val);
			util.addDebugLogMsg(2,"I","Restored parms="+pa.parms_key+"="+pa.parms_value);
		} else if (pa.parms_type.equals(SMBSYNC_SETTINGS_TYPE_INT)) {
			int i_val = 0;
			i_val = Integer.parseInt(pa.parms_value);;
			pe.putInt(pa.parms_key,i_val);
			util.addDebugLogMsg(2,"I","Restored parms="+pa.parms_key+"="+pa.parms_value);
		} else if (pa.parms_type.equals(SMBSYNC_SETTINGS_TYPE_LONG)) {
			long i_val = 0;
			i_val = Long.parseLong(pa.parms_value);;
			pe.putLong(pa.parms_key,i_val);
			util.addDebugLogMsg(2,"I","Restored parms="+pa.parms_key+"="+pa.parms_value);
		}
	};
	
	private void restoreImportedScheduleParms() {
		final ArrayList<PreferenceParmListIItem> spl=importedSettingParmList;
		
		if (spl.size()==0) {
			util.addDebugLogMsg(2,"I","Import setting parms can not be not found.");
			return;
		}
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		final Editor pe= prefs.edit();
		
		if (spl.size()>=0) {
			for (int i=0;i<spl.size();i++) {
				if (spl.get(i).parms_key.startsWith("schedule")) {
					restorePreferenceParms(pe,spl.get(i));
				}
			}
			pe.commit();
//			applySettingParms();
		}
	};

	private void restoreImportedSettingParms() {
		final ArrayList<PreferenceParmListIItem> spl=importedSettingParmList;
		
		if (spl.size()==0) {
			util.addDebugLogMsg(2,"I","Import setting parms can not be not found.");
			return;
		}
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		final Editor pe= prefs.edit();
		
		if (spl.size()>=0) {
			for (int i=0;i<spl.size();i++) {
				if (spl.get(i).parms_key.startsWith("settings")) {
					restorePreferenceParms(pe,spl.get(i));
				}
			}
			pe.commit();
//			applySettingParms();
		}
	};

	public void exportProfileDlg(final String lurl, final String ldir, final String ifn) {

		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
    			final String fpath=(String)o[0];
    			NotifyEvent ntfy_pswd=new NotifyEvent(mContext);
    			ntfy_pswd.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						mGp.profilePassword=(String)o[0];
						boolean encrypt_required=false;
						if (!mGp.profilePassword.equals("")) encrypt_required=true; 
		    			String fd=fpath.substring(0,fpath.lastIndexOf("/"));
		    			String fn=fpath.replace(fd+"/","");
		    			exportProfileToFile(fd,fn,encrypt_required);
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
    			});
    			promptPasswordForExport(fpath,ntfy_pswd);
			}

			@Override
			public void negativeResponse(Context c,Object[] o) {}
		});
		commonDlg.fileOnlySelectWithCreate(lurl,ldir,ifn,msgs_select_export_file,ntfy);
	};	
			
	public void exportProfileToFile(final String profile_dir, 
			final String profile_filename, final boolean encrypt_required) {
		
		File lf = new File(profile_dir+"/"+profile_filename);
		if (lf.exists()) {
			NotifyEvent ntfy=new NotifyEvent(mContext);
			ntfy.setListener(new NotifyEventListener() {
				@Override
				public void positiveResponse(Context c,Object[] o) {
	    			String fp =profile_dir+"/"+profile_filename;
	    			String fd =profile_dir;
	    			
					if (saveProfileToFile(mGp, mContext, util, true,fd,fp,mGp.profileAdapter,encrypt_required)) {
						commonDlg.showCommonDialog(false,"I",msgs_export_prof_success,
								"File="+fp, null);
						util.addDebugLogMsg(1,"I","Profile was exported. fn="+fp);						
					} else {
						commonDlg.showCommonDialog(false,"E",msgs_export_prof_fail,
								"File="+fp, null);
					}
				}
	
				@Override
				public void negativeResponse(Context c,Object[] o) {}
			});
			commonDlg.showCommonDialog(true,"W",msgs_export_prof_title,
					profile_dir+"/"+profile_filename+" "+msgs_override,ntfy);
		} else {
			String fp =profile_dir+"/"+profile_filename;
			String fd =profile_dir;
			if (saveProfileToFile(mGp, mContext, util, true,fd,fp,mGp.profileAdapter,encrypt_required)) {
				commonDlg.showCommonDialog(false,"I",msgs_export_prof_success,
						"File="+fp, null);
				util.addDebugLogMsg(1,"I","Profile was exported. fn="+fp);						
			} else {
				commonDlg.showCommonDialog(false,"E",msgs_export_prof_fail,
						"File="+fp, null);
			}
		}
	};
	
	static public void setAllProfileToUnchecked(boolean hideCheckBox, AdapterProfileList pa) {
		pa.setAllItemChecked(false);
		if (hideCheckBox) pa.setShowCheckBox(false);
		pa.notifyDataSetChanged();
	};
	
	static public void setProfileToChecked(boolean chk, AdapterProfileList pa, 
			 int no) {
		ProfileListItem item=pa.getItem(no);
		item.setChecked(chk);
	};
	
	public void setProfileToActive(GlobalParameters gp) {
		ProfileListItem item ;

//		int pos=gp.profileListView.getFirstVisiblePosition();
//		int posTop=gp.profileListView.getChildAt(0).getTop();
		for (int i=0;i<gp.profileAdapter.getCount();i++) {
			item = gp.profileAdapter.getItem(i);
			if (item.isChecked()) {
				if (!item.getProfileType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
					item.setProfileActive(SMBSYNC_PROF_ACTIVE);
//					item.setChecked(false);
				}
			} 
		}
		for (int i=0;i<gp.profileAdapter.getCount();i++) {
			item = gp.profileAdapter.getItem(i);
			if (item.isChecked()) {
				if (!item.getProfileType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
					item.setProfileActive(SMBSYNC_PROF_ACTIVE);
//					item.setChecked(false);
				} else {
					if (isSyncProfileDisabled(gp, item)) {
						//Not activated
						commonDlg.showCommonDialog(false, "W", 
								mContext.getString(R.string.msgs_prof_active_not_activated), "", null);
					} else {
						item.setProfileActive(SMBSYNC_PROF_ACTIVE);
//						item.setChecked(false);
					}
				}
			} 
		}

//		resolveSyncProfileRelation();

		saveProfileToFile(mGp, mContext, util, false,"","",gp.profileAdapter,false);
		mGp.profileAdapter.notifyDataSetChanged();
		gp.profileAdapter.setNotifyOnChange(true);
//		gp.profileListView.setSelectionFromTop(pos,posTop);			
	};
	
	public void setProfileToInactive() {
		ProfileListItem item ;

		int pos=mGp.profileListView.getFirstVisiblePosition();
		int posTop=mGp.profileListView.getChildAt(0).getTop();
		for (int i=0;i<mGp.profileAdapter.getCount();i++) {
			item = mGp.profileAdapter.getItem(i);
			if (item.isChecked()) {
				item.setProfileActive(SMBSYNC_PROF_INACTIVE);
//				item.setChecked(false);
			}		
		}
		
		resolveSyncProfileRelation(mGp);
		
		saveProfileToFile(mGp, mContext, util, false,"","",mGp.profileAdapter,false);
		mGp.profileAdapter.notifyDataSetChanged();
		mGp.profileAdapter.setNotifyOnChange(true);
		mGp.profileListView.setSelectionFromTop(pos,posTop);
	};
	
	public void deleteProfile(final NotifyEvent p_ntfy) {
		final int[] dpnum = new int[mGp.profileAdapter.getCount()];
		String dpmsg="";
		
		for (int i=0;i<mGp.profileAdapter.getCount();i++) {
			if (mGp.profileAdapter.getItem(i).isChecked()) {
				dpmsg=dpmsg+mGp.profileAdapter.getItem(i).getProfileName()+"\n";
				dpnum[i]=i;
			} else dpnum[i]=-1;
		}

		NotifyEvent ntfy=new NotifyEvent(mContext);
		// set commonDlg.showCommonDialog response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				ArrayList<ProfileListItem> dpItemList = new ArrayList<ProfileListItem>();
				int pos=mGp.profileListView.getFirstVisiblePosition();
				for (int i=0;i<dpnum.length;i++) {
					if (dpnum[i]!=-1)
						dpItemList.add(mGp.profileAdapter.getItem(dpnum[i]));
				}
				for (int i=0;i<dpItemList.size();i++) mGp.profileAdapter.remove(dpItemList.get(i));
				
				resolveSyncProfileRelation(mGp);
				
				saveProfileToFile(mGp, mContext, util, false,"","",mGp.profileAdapter,false);
				
				if (mGp.profileAdapter.getCount()<=0) { 
					mGp.profileAdapter.add(new ProfileListItem("","",
							mContext.getString(R.string.msgs_no_profile_entry),
							"","",null,"",0,0,false));
				}
//				glblParms.profileListView.setAdapter(glblParms.profileAdapter);
				mGp.profileAdapter.setNotifyOnChange(true);
				mGp.profileListView.setSelection(pos);
				
				if (mGp.profileAdapter.isEmptyAdapter()) {
					mGp.profileAdapter.setShowCheckBox(false);
				}
				
				ProfileUtility.setAllProfileToUnchecked(true, mGp.profileAdapter);
				
				p_ntfy.notifyToListener(true, null);
			}

			@Override
			public void negativeResponse(Context c,Object[] o) {
				p_ntfy.notifyToListener(false, null);
			}
		});
		commonDlg.showCommonDialog(true,"W",msgs_delete_following_profile,dpmsg,ntfy);
	};
	
	static public void resolveSyncProfileRelation(GlobalParameters gp) {
		for (int i=0;i<gp.profileAdapter.getCount();i++) {
			ProfileListItem item=gp.profileAdapter.getItem(i);
			if (item.getProfileType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
				if (isSyncProfileDisabled(gp, item)) {
//					glblParms.profileAdapter.remove(glblParms.profileAdapter.getItem(i));
//					gp.profileAdapter.replace(item,i);
				}

			}
		}
		gp.profileAdapter.notifyDataSetChanged();
	};

	static private boolean isSyncProfileDisabled(GlobalParameters gp, ProfileListItem item) {
		boolean result=false;
		if (getProfileType(item.getMasterName(),gp.profileAdapter).equals("")) {
			item.setMasterType("");
			item.setMasterName("");
			item.setProfileActive(SMBSYNC_PROF_INACTIVE);
			result=true;
		} else {
			if (!isProfileActive(gp, SMBSYNC_PROF_GROUP_DEFAULT,
					item.getMasterType(), item.getMasterName())) {
				item.setProfileActive(SMBSYNC_PROF_INACTIVE);
				result=true;
			}
		}
		if (getProfileType(item.getTargetName(),gp.profileAdapter).equals("")) {
			item.setTargetType("");
			item.setTargetName("");
			item.setProfileActive(SMBSYNC_PROF_INACTIVE);
			result=true;
		} else {
			if (!isProfileActive(gp, SMBSYNC_PROF_GROUP_DEFAULT,
					item.getTargetType(), item.getTargetName())) {
				item.setProfileActive(SMBSYNC_PROF_INACTIVE);
				result=true;
			}
		}
		return result;
	};
	
	public void showSelectSdcardMsg(final NotifyEvent ntfy, String msg) {
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    dialog.setContentView(R.layout.show_select_sdcard_dlg);

		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.show_select_sdcard_dlg_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.show_select_sdcard_dlg_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		
		final TextView dlg_msg=(TextView)dialog.findViewById(R.id.show_select_sdcard_dlg_msg);
		dlg_msg.setText(msg);;
		
		final ImageView func_view=(ImageView)dialog.findViewById(R.id.show_select_sdcard_dlg_image);
		
		
		try {
		    InputStream is = 
		    	mContext.getResources().getAssets().open(mContext.getString(R.string.msgs_main_external_sdcard_select_required_select_msg_file));
		    Bitmap bm = BitmapFactory.decodeStream(is);
		    func_view.setImageBitmap(bm);
		} catch (IOException e) {
		    /* 例外処理 */
		}
		
		final Button btnOk = (Button) dialog.findViewById(R.id.show_select_sdcard_dlg_btn_ok);
		final Button btnCancel = (Button) dialog.findViewById(R.id.show_select_sdcard_dlg_btn_cancel);

		CommonDialog.setDlgBoxSizeLimit(dialog,true);

		// OKボタンの指定
		btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				ntfy.notifyToListener(true, null);
			}
		});
		// Cancelボタンの指定
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				ntfy.notifyToListener(false, null);
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnCancel.performClick();
			}
		});

		dialog.show();

	}

	public boolean isExternalSdcardUsedByOutput() {
		boolean result=false;
		for(ProfileListItem pli:mGp.profileAdapter.getArrayList()) {
//			Log.v("","name="+pli.getProfileName()+", type="+pli.getProfileType()+", act="+pli.isProfileActive());
			if (pli.isProfileActive() && pli.getProfileType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
				ProfileListItem target=ProfileUtility.getProfile(pli.getTargetName(),mGp.profileAdapter);
//				Log.v("","name="+pli.getProfileName()+", target="+target);
				if (target!=null) {
					if (target.getProfileType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
						if (SafUtil.isSafExternalSdcardPath(mSafCA, target.getLocalMountPoint())) {
							result=true;
							break;
						}
					}
				}
			}
		}
		return result;
	};

	public ProfileListItem getExternalSdcardUsedSyncProfile() {
		ProfileListItem result=null;
		for(ProfileListItem pli:mGp.profileAdapter.getArrayList()) {
//			Log.v("","name="+pli.getProfileName()+", type="+pli.getProfileType()+", act="+pli.isProfileActive());
			if (pli.isProfileActive() && pli.getProfileType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
				ProfileListItem target=ProfileUtility.getProfile(pli.getTargetName(),mGp.profileAdapter);
//				Log.v("","name="+pli.getProfileName()+", target="+target);
				if (target!=null) {
					if (target.getProfileType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
						if (SafUtil.isSafExternalSdcardPath(mSafCA, target.getLocalMountPoint())) {
							result=pli;
							break;
						}
					}
				}
			}
		}
		return result;
	};

	public void logonToRemoteDlg(final String host, final String addr, final String port, 
			final String user, final String pass, final NotifyEvent p_ntfy) {
		final ThreadCtrl tc=new ThreadCtrl();
		tc.setEnabled();
		tc.setThreadResultSuccess();
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.progress_spin_dlg);
		
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.progress_spin_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.progress_spin_dlg_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.progress_spin_dlg_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		title.setText(R.string.msgs_progress_spin_dlg_test_logon);
		
		final Button btn_cancel = (Button) dialog.findViewById(R.id.progress_spin_dlg_btn_cancel);
		btn_cancel.setText(R.string.msgs_progress_spin_dlg_test_logon_cancel);
		
//		(dialog.context.findViewById(R.id.progress_spin_dlg)).setVisibility(TextView.GONE);
//		(dialog.context.findViewById(R.id.progress_spin_dlg)).setEnabled(false);
		
		CommonDialog.setDlgBoxSizeCompact(dialog);
		// CANCELボタンの指定
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				tc.setDisabled();//disableAsyncTask();
				btn_cancel.setText(mContext.getString(R.string.msgs_progress_dlg_canceling));
				btn_cancel.setEnabled(false);
				util.addDebugLogMsg(1,"W","Logon is cancelled.");
			}
		});
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
//		dialog.show(); showDelayedProgDlgで表示

		Thread th=new Thread() {
			@Override
			public void run() {
				util.addDebugLogMsg(1,"I","Test logon started, host="+host+", addr="+addr+
						", port="+port+", user="+user);
				NtlmPasswordAuthentication auth=new NtlmPasswordAuthentication(null, user, pass);
				
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						dialog.dismiss();
						String err_msg=(String)o[0];
						if (tc.isEnabled()) {
							if (err_msg!=null) {
								commonDlg.showCommonDialog(false, "E", 
										mContext.getString(R.string.msgs_remote_profile_dlg_logon_error)
										, err_msg, null);
								if (p_ntfy!=null) p_ntfy.notifyToListener(false, null);
							} else {
								commonDlg.showCommonDialog(false, "I", "", 
									mContext.getString(R.string.msgs_remote_profile_dlg_logon_success), null);
								if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
							}
						} else {
							commonDlg.showCommonDialog(false, "I", "", 
									mContext.getString(R.string.msgs_remote_profile_dlg_logon_cancel), null);
								if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
						}
					}

					@Override
					public void negativeResponse(Context c, Object[] o) {}					
				});
				
				if (host.equals("")) {
					boolean reachable=false;
					if (port.equals("")) {
						if (NetworkUtil.isIpAddressAndPortConnected(addr,139,3500) ||
								NetworkUtil.isIpAddressAndPortConnected(addr,445,3500)) {
							reachable=true;
						}
					} else {
						reachable=NetworkUtil.isIpAddressAndPortConnected(addr,
								Integer.parseInt(port),3500);
					}
					if (reachable) {
						testAuth(auth,addr,port,ntfy);
					} else {
						util.addDebugLogMsg(1,"I","Test logon failed, remote server not connected");
						String unreachble_msg="";
						if (port.equals("")) {
							unreachble_msg=String.format(mContext.getString(R.string.msgs_mirror_remote_addr_not_connected)
									,addr);
						} else {
							unreachble_msg=String.format(mContext.getString(R.string.msgs_mirror_remote_addr_not_connected_with_port)
									,addr,port);
						}
						ntfy.notifyToListener(true, new Object[]{unreachble_msg});
					}
				} else {
					if (NetworkUtil.getSmbHostIpAddressFromName(host)!=null) testAuth(auth,host,port,ntfy);
					else {
						util.addDebugLogMsg(1,"I","Test logon failed, remote server not connected");
						String unreachble_msg="";
						unreachble_msg=mContext.getString(R.string.msgs_mirror_remote_name_not_found)+host;
						ntfy.notifyToListener(true, new Object[]{unreachble_msg});
					}
				}
			}
		};
		th.start();
		dialog.show();
	};
	
	@SuppressWarnings("unused")
	private void testAuth(final NtlmPasswordAuthentication auth, 
			final String host, String port, final NotifyEvent ntfy) {
		final UncaughtExceptionHandler defaultUEH = 
				Thread.currentThread().getUncaughtExceptionHandler();
        Thread.currentThread().setUncaughtExceptionHandler( new Thread.UncaughtExceptionHandler() {
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
    			String end_msg=ex.toString()+st_msg;
    			ntfy.notifyToListener(true, new Object[] {end_msg});
                // re-throw critical exception further to the os (important)
//                defaultUEH.uncaughtException(thread, ex);
            }
        });

		String err_msg=null;
		SmbFile sf=null;
		SmbFile[] lf=null;
		String url="";
		if (port.equals("")) {
			url="smb://"+host+"/IPC$/";
		} else {
			url="smb://"+host+":"+port+"/IPC$/";
		}
//		Log.v("","url="+url);
		try {
			sf=new SmbFile(url,auth);
			sf.connect();
//			sf.getSecurity();
//			lf=sf.listFiles();
//			if (lf!=null) {
//				for (int i=0;i<lf.length;i++) {
//					Log.v("","name="+lf[i].getName()+", share="+lf[i].getShare());
//				}
//			}
//			String aa=null;
//			aa.length();
			util.addDebugLogMsg(1,"I","Test logon completed, host="+host+
					", port="+port+", user="+auth.getUsername());
		} catch(SmbException e) {
//			if (e.getNtStatus()==NtStatus.NT_STATUS_LOGON_FAILURE ||
//					e.getNtStatus()==NtStatus.NT_STATUS_ACCOUNT_RESTRICTION ||
//					e.getNtStatus()==NtStatus.NT_STATUS_INVALID_LOGON_HOURS ||
//					e.getNtStatus()==NtStatus.NT_STATUS_INVALID_WORKSTATION ||
//					e.getNtStatus()==NtStatus.NT_STATUS_PASSWORD_EXPIRED ||
//					e.getNtStatus()==NtStatus.NT_STATUS_ACCOUNT_DISABLED) {
//			}
			String[] e_msg=NetworkUtil.analyzeNtStatusCode(e, mContext, url, auth.getUsername());
			err_msg=e_msg[0];
			util.addDebugLogMsg(1,"I","Test logon failed."+"\n"+err_msg);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Thread.currentThread().setUncaughtExceptionHandler(defaultUEH);
		ntfy.notifyToListener(true, new Object[] {err_msg});
	};
	
	public void setSyncOptionSpinner(Spinner spinnerSyncOption, String prof_syncopt) {
		setSyncOptionSpinner(mContext, spinnerSyncOption, prof_syncopt); 
	};

	static public void setSyncOptionSpinner(Context c, Spinner spinnerSyncOption, String prof_syncopt) {
//		final Spinner spinnerSyncOption=(Spinner)dialog.findViewById(R.id.sync_profile_sync_option);
		final CustomSpinnerAdapter adapterSyncOption=
				new CustomSpinnerAdapter(c, R.layout.custom_simple_spinner_item);
		adapterSyncOption.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerSyncOption.setPrompt(c.getString(R.string.msgs_sync_profile_dlg_syncopt_prompt));
		spinnerSyncOption.setAdapter(adapterSyncOption);
		adapterSyncOption.add(c.getString(R.string.msgs_sync_profile_dlg_mirror));
		adapterSyncOption.add(c.getString(R.string.msgs_sync_profile_dlg_copy));
		adapterSyncOption.add(c.getString(R.string.msgs_sync_profile_dlg_move));
		
		if (prof_syncopt.equals(SMBSYNC_SYNC_TYPE_MIRROR)) spinnerSyncOption.setSelection(0);
		else if (prof_syncopt.equals(SMBSYNC_SYNC_TYPE_COPY)) spinnerSyncOption.setSelection(1);
		else if (prof_syncopt.equals(SMBSYNC_SYNC_TYPE_MOVE)) spinnerSyncOption.setSelection(2);
		
		adapterSyncOption.notifyDataSetChanged();
	};

	static public void setSyncMasterProfileSpinner(GlobalParameters gp, Context c, Spinner spinner_master, String prof_master) {
		final AdapterProfileSelectionSpinner adapter_spinner=
				new AdapterProfileSelectionSpinner(c, R.layout.custom_simple_spinner_item);
		adapter_spinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_master.setPrompt(msgs_select_master_profile);
		spinner_master.setAdapter(adapter_spinner);
		int pos=0,cnt=-1;
		
		for (ProfileListItem pli:gp.profileAdapter.getArrayList()) {
			if (pli.getProfileType().equals(SMBSYNC_PROF_TYPE_REMOTE) || 
					pli.getProfileType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
				cnt++;
//				Log.v("","master added="+pli.getName());
				adapter_spinner.add(pli.getProfileType()+" "+pli.getProfileName());
				if (prof_master.equals(pli.getProfileName())) pos=cnt;
			}
		}
//		Log.v("","set master master="+prof_master+", pos="+pos);
		spinner_master.setSelection(pos);
		adapter_spinner.notifyDataSetChanged();
	};

	static public void setSyncTargetProfileSpinner(GlobalParameters gp, Context c, Spinner spinner_target, String prof_master, String prof_target) {
		final AdapterProfileSelectionSpinner adapter_spinner=new AdapterProfileSelectionSpinner(c, R.layout.custom_simple_spinner_item);
		adapter_spinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_target.setPrompt(msgs_select_target_profile);
		spinner_target.setAdapter(adapter_spinner);

		if (!prof_master.equals("")) {
			ProfileListItem m_pli=getProfile(gp, c, SMBSYNC_PROF_GROUP_DEFAULT, prof_master);
			String mst_type="";
			if (m_pli!=null) mst_type=m_pli.getProfileType();
			
			int pos=0, cnt=-1;
			
			for (ProfileListItem pli:gp.profileAdapter.getArrayList()) {
				if (mst_type.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
					if (pli.getProfileType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
						cnt++;
						adapter_spinner.add(pli.getProfileType()+" "+pli.getProfileName());
						if (prof_target.equals(pli.getProfileName())) pos=cnt;
					}
				} else if (mst_type.equals(SMBSYNC_PROF_TYPE_LOCAL)) {
					if (pli.getProfileType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
						cnt++;
						adapter_spinner.add(pli.getProfileType()+" "+pli.getProfileName());
						if (prof_target.equals(pli.getProfileName())) pos=cnt;
					} else if (pli.getProfileType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
						if (!prof_master.equals(pli.getProfileName())) {
							String m_path=m_pli.getLocalMountPoint()+"/"+m_pli.getDirectoryName();
							String t_path=pli.getLocalMountPoint()+"/"+pli.getDirectoryName();
							if (!m_path.equals(t_path)) {
								cnt++;
								adapter_spinner.add(pli.getProfileType()+" "+pli.getProfileName());
								if (prof_target.equals(pli.getProfileName())) pos=cnt;
							}
						}
					}
				}
			}
			spinner_target.setSelection(pos);
		} else {
			if (!prof_target.equals("")) {
				ProfileListItem t_pli=getProfile(gp, c, SMBSYNC_PROF_GROUP_DEFAULT, prof_target);
				String tgt_type="";
				if (t_pli!=null) tgt_type=t_pli.getProfileType();
				int pos=0, cnt=-1;
				
				for (ProfileListItem pli:gp.profileAdapter.getArrayList()) {
					if (tgt_type.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
						if (pli.getProfileType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
							cnt++;
							adapter_spinner.add(pli.getProfileType()+" "+pli.getProfileName());
							if (prof_target.equals(pli.getProfileName())) pos=cnt;
						}
					} else if (tgt_type.equals(SMBSYNC_PROF_TYPE_LOCAL)) {
						if (pli.getProfileType().equals(SMBSYNC_PROF_TYPE_REMOTE) || 
								pli.getProfileType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
							if (!prof_master.equals(pli.getProfileName())) {
								cnt++;
								adapter_spinner.add(pli.getProfileType()+" "+pli.getProfileName());
								if (prof_target.equals(pli.getProfileName())) pos=cnt;
							}
						}
					}
				}
				spinner_target.setSelection(pos);
			} else {
				int pos=0, cnt=-1;
				for (ProfileListItem pli:gp.profileAdapter.getArrayList()) {
					if (pli.getProfileType().equals(SMBSYNC_PROF_TYPE_REMOTE) || 
							pli.getProfileType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
						if (!prof_master.equals(pli.getProfileName())) {
							cnt++;
							adapter_spinner.add(pli.getProfileType()+" "+pli.getProfileName());
							if (prof_target.equals(pli.getProfileName())) pos=cnt;
						}
					}
				}
				spinner_target.setSelection(pos);
			}
		}
		adapter_spinner.notifyDataSetChanged();
//		Log.v("","master="+prof_master+", target="+prof_target);
//		Log.v("","set sp_t="+spinner_target.getSelectedItem());
	};

	public void copyProfile(ProfileListItem pli, NotifyEvent p_ntfy) {
		if (pli.getProfileType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
			ProfileMaintLocalFragment pmlp=ProfileMaintLocalFragment.newInstance();
			pmlp.showDialog(mFragMgr, pmlp, "COPY",pli, 0, this, util, commonDlg,p_ntfy);
		} else if (pli.getProfileType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
			ProfileMaintRemoteFragment pmrp=ProfileMaintRemoteFragment.newInstance();
			pmrp.showDialog(mFragMgr, pmrp, "COPY",pli, 0, this, util, commonDlg,p_ntfy);
		} else if (pli.getProfileType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
			ArrayList<String>ff=new ArrayList<String>();
			ff.addAll(pli.getFileFilter());
			ArrayList<String>df=new ArrayList<String>();
			df.addAll(pli.getDirFilter());
			ProfileListItem npfli=new ProfileListItem(pli.getProfileGroup(), pli.getProfileType(), 
					pli.getProfileName(), pli.getProfileActive(),
					pli.getSyncType(), pli.getMasterType(), pli.getMasterName(),
					pli.getTargetType(), pli.getTargetName(),
					ff, df, pli.isMasterDirFileProcess(), pli.isConfirmRequired(), 
					pli.isForceLastModifiedUseSmbsync(), pli.isNotUseLastModifiedForRemote(), 
					pli.getRetryCount(), pli.isSyncEmptyDirectory(), 
					pli.isSyncSubDirectory(), pli.isSyncHiddenFile(), pli.isSyncSubDirectory(), pli.isSyncUseRemoteSmallIoArea(),
					pli.getSyncZipFileName(),pli.getSyncZipEncMethod(), pli.getSyncZipAesKeyLength(),
					pli.getLastSyncTime(), pli.getLastSyncResult(),
					false);
			ProfileMaintSyncFragment pmsp=ProfileMaintSyncFragment.newInstance();
			pmsp.showDialog(mFragMgr, pmsp, "COPY",npfli, this, util, commonDlg,p_ntfy);
		}
	};

	public void renameProfile(final ProfileListItem pli, final NotifyEvent p_ntfy) {
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.single_item_input_dlg);
		
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.single_item_input_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.single_item_input_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.single_item_input_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);

//		final TextView dlg_msg = (TextView) dialog.findViewById(R.id.single_item_input_msg);
		final TextView dlg_cmp = (TextView) dialog.findViewById(R.id.single_item_input_name);
		final Button btn_ok = (Button) dialog.findViewById(R.id.single_item_input_ok_btn);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.single_item_input_cancel_btn);
		final EditText etInput=(EditText) dialog.findViewById(R.id.single_item_input_dir);
		
		title.setText(mContext.getString(R.string.msgs_rename_profile));
		
		dlg_cmp.setVisibility(TextView.GONE);
		CommonDialog.setDlgBoxSizeCompact(dialog);
		etInput.setText(pli.getProfileName());
		btn_ok.setEnabled(false);
		etInput.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
				if (!arg0.equals(pli.getProfileName())) btn_ok.setEnabled(true);
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {}
		});
		
		//OK button
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				String new_name=etInput.getText().toString();
//				int pos=glblParms.profileListView.getFirstVisiblePosition();
//				int posTop=glblParms.profileListView.getChildAt(0).getTop();
				if (!pli.getProfileType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
					for (int i=0;i<mGp.profileAdapter.getCount();i++) {
						ProfileListItem tpli=mGp.profileAdapter.getItem(i);
						boolean update_required=false;
						if (tpli.getProfileType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
							if (tpli.getMasterName().equals(pli.getProfileName())) {
								tpli.setMasterName(new_name);
								update_required=true;
							}
							if (tpli.getTargetName().equals(pli.getProfileName())) {
								tpli.setTargetName(new_name);
								update_required=true;
							}
							if (update_required) mGp.profileAdapter.replace(tpli, i);
						}
					}
				}
				mGp.profileAdapter.remove(pli);
				pli.setProfileName(new_name);
				mGp.profileAdapter.add(pli);

				resolveSyncProfileRelation(mGp);

				mGp.profileAdapter.sort();
				mGp.profileAdapter.notifyDataSetChanged();

				saveProfileToFile(mGp, mContext, util, false,"","",mGp.profileAdapter,false);
				
				ProfileUtility.setAllProfileToUnchecked(true, mGp.profileAdapter);
				
				p_ntfy.notifyToListener(true, null);
			}
		});
		// CANCELボタンの指定
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
//		dialog.setCancelable(false);
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
		dialog.show();

	};

	public void ipAddressScanButtonDlg(Dialog dialog) {
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.edit_profile_remote_dlg_msg);
		final EditText edithost = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_server);
		final CheckedTextView ctv_use_port_number = (CheckedTextView) dialog.findViewById(R.id.edit_profile_remote_dlg_ctv_use_remote_port_number);
		final EditText editport = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_port);
		NotifyEvent ntfy=new NotifyEvent(mContext);
		//Listen setRemoteShare response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context arg0, Object[] arg1) {
				edithost.setText((String)arg1[1]);
			}

			@Override
			public void negativeResponse(Context arg0, Object[] arg1) {
				dlg_msg.setText("");
			}
			
		});
		String port_num="";
		if (ctv_use_port_number.isChecked()) port_num=editport.getText().toString();
		scanRemoteNetworkDlg(ntfy,port_num,false);
	};

	public void invokeSelectRemoteShareDlg(Dialog dialog) {
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.edit_profile_remote_dlg_msg);

		final EditText edituser = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_user);
		final EditText editpass = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_pass);
		final EditText editshare = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_share_name);
		final EditText edithost = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_server);
		final CheckedTextView ctv_use_userpass = (CheckedTextView) dialog.findViewById(R.id.edit_profile_remote_dlg_ctv_use_user_pass);
		final EditText editport = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_port);
		final CheckedTextView ctv_use_port_number = (CheckedTextView) dialog.findViewById(R.id.edit_profile_remote_dlg_ctv_use_remote_port_number);
		String remote_addr, remote_user="", remote_pass="",remote_host;
		
		if (ctv_use_userpass.isChecked()) {
			remote_user = edituser.getText().toString();
			remote_pass = editpass.getText().toString();
		}

		if (edithost.getText().toString().length()<1) { 
			dlg_msg.setText(mContext.getString(R.string.msgs_audit_hostname_not_spec));
			return;
		}
		if (hasInvalidChar(remote_pass,new String[]{"\t"})) {
			remote_pass=removeInvalidChar(remote_pass);
			dlg_msg.setText(String.format(msgs_audit_msgs_password1,detectedInvalidCharMsg));
			editpass.setText(remote_pass);
			editpass.requestFocus();
			return;
		}
		if (hasInvalidChar(remote_user,new String[]{"\t"})) {
			remote_user=removeInvalidChar(remote_user);
			dlg_msg.setText(String.format(msgs_audit_msgs_username1,detectedInvalidCharMsg));
			edituser.setText(remote_user);
			edituser.requestFocus();
			return;
		}

		setSmbUserPass(remote_user,remote_pass);
		String t_url="";
		if (NetworkUtil.isValidIpAddress(edithost.getText().toString()))  {
			remote_addr = edithost.getText().toString();
			t_url=remote_addr;
		}  else {
			remote_host = edithost.getText().toString();
			t_url=remote_host;
		}
		String h_port="";
		if (ctv_use_port_number.isChecked()) {
			if (editport.getText().length()>0) h_port=":"+editport.getText().toString();
			else {
				dlg_msg.setText(mContext.getString(R.string.msgs_audit_hostport_not_spec));
				return;
			}
		}
		String remurl="smb://"+t_url+h_port+"/";
		NotifyEvent ntfy=new NotifyEvent(mContext);
		//Listen setRemoteShare response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context arg0, Object[] arg1) {
				editshare.setText((String)arg1[0]);
			}

			@Override
			public void negativeResponse(Context arg0, Object[] arg1) {
				if (arg1!=null) dlg_msg.setText((String)arg1[0]);
				else dlg_msg.setText("");
			}
			
		});
		selectRemoteShareDlg(remurl,"", ntfy);
	};

	public void setSmbUserPass(String user, String pass) {
		smbUser=user;
		smbPass=pass;
	};
	
	public void selectRemoteDirectory(Dialog dialog) {
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.edit_profile_remote_dlg_msg);

		final EditText edithost = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_server);
		final EditText edituser = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_user);
		final EditText editpass = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_pass);
		final EditText editshare = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_share_name);
		final EditText editdir = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_dir);
		final CheckedTextView ctv_use_userpass = (CheckedTextView) dialog.findViewById(R.id.edit_profile_remote_dlg_ctv_use_user_pass);
		final EditText editport = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_port);
		final CheckedTextView ctv_use_port_number = (CheckedTextView) dialog.findViewById(R.id.edit_profile_remote_dlg_ctv_use_remote_port_number);
		String remote_addr, remote_user="", remote_pass="",remote_share,remote_host;
		if (ctv_use_userpass.isChecked()) {
			remote_user = edituser.getText().toString();
			remote_pass = editpass.getText().toString();
		}

		remote_share = editshare.getText().toString();

		if (edithost.getText().toString().length()<1) {
			dlg_msg.setText(mContext.getString(R.string.msgs_audit_hostname_not_spec));
			return;
		}
		if (remote_share.length()<1) {
			dlg_msg.setText(msgs_audit_share_not_spec);
			return;
		}
		if (hasInvalidChar(remote_pass,new String[]{"\t"})) {
			remote_pass=removeInvalidChar(remote_pass);
			dlg_msg.setText(String.format(msgs_audit_msgs_password1,detectedInvalidCharMsg));
			editpass.setText(remote_pass);
			editpass.requestFocus();
			return;
		}
		if (hasInvalidChar(remote_user,new String[]{"\t"})) {
			remote_user=removeInvalidChar(remote_user);
			dlg_msg.setText(String.format(msgs_audit_msgs_username1,detectedInvalidCharMsg));
			edituser.setText(remote_user);
			edituser.requestFocus();
			return;
		}
		String p_dir = editdir.getText().toString();
		
		setSmbUserPass(remote_user,remote_pass);
		String t_url="";
		if (NetworkUtil.isValidIpAddress(edithost.getText().toString()))  {
			remote_addr = edithost.getText().toString();
			t_url=remote_addr;
		}  else {
			remote_host = edithost.getText().toString();
			t_url=remote_host;
		}
		String h_port="";
		if (ctv_use_port_number.isChecked()) {
			if (editport.getText().length()>0) h_port=":"+editport.getText().toString();
			else {
				dlg_msg.setText(mContext.getString(R.string.msgs_audit_hostport_not_spec));
				return;
			}
		}
		String remurl="smb://"+t_url+h_port+"/"+remote_share+"/";
		NotifyEvent ntfy=new NotifyEvent(mContext);
		//Listen setRemoteShare response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context arg0, Object[] arg1) {
				editdir.setText((String)arg1[0]);
			}

			@Override
			public void negativeResponse(Context arg0, Object[] arg1) {
				if (arg1!=null) dlg_msg.setText((String)arg1[0]);
				else dlg_msg.setText("");
			}
			
		});
		setRemoteDir(remurl, "",p_dir, ntfy);
	};
	
//	private void setMasterProfileEditButtonListener(Dialog dialog, final String prof_name) {
//		final ImageButton ib_edit_master=(ImageButton) dialog.findViewById(R.id.sync_profile_edit_master);
//		final ProfileListItem pli=getProfile(mGp, mContext, SMBSYNC_PROF_GROUP_DEFAULT, prof_name);
//		if (pli==null) ib_edit_master.setEnabled(false);//setVisibility(ImageButton.GONE);
//		else {
//			ib_edit_master.setEnabled(true);//.setVisibility(ImageButton.VISIBLE);
//			
//		}
//
//	};
//
//	private void setTargetProfileEditButtonListener(Dialog dialog, final String prof_name) {
//		final ImageButton ib_edit_target=(ImageButton) dialog.findViewById(R.id.sync_profile_edit_target);
//		final ProfileListItem pli=getProfile(mGp, mContext, SMBSYNC_PROF_GROUP_DEFAULT, prof_name);
//		if (pli==null) ib_edit_target.setEnabled(false);//.setVisibility(ImageButton.GONE);
//		else {
//			ib_edit_target.setEnabled(true);//.setVisibility(ImageButton.VISIBLE);
//		}
//
//	};

	static public ProfileListItem getProfile(GlobalParameters gp, Context c, String group, String name) {
		ProfileListItem pli=null;
		for (int i=0;i<gp.profileAdapter.getCount();i++) {
			if (gp.profileAdapter.getItem(i).getProfileGroup().equals(SMBSYNC_PROF_GROUP_DEFAULT) &&
					gp.profileAdapter.getItem(i).getProfileName().equals(name)) {
				pli=gp.profileAdapter.getItem(i);
			}
		}
		return pli;
		
	};
	
	public int getActiveSyncProfileCount(AdapterProfileList pa) {
		int result=0;
		for (int i=0;i<pa.getCount();i++) {
			if (pa.getItem(i).isProfileActive() && 
					pa.getItem(i).getProfileType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
				result++;
			}
		}
		return result;
	}
	
//	private void invokeEditFileFilterDlg(Dialog dialog,
//			final ArrayList<String> n_file_filter) {
//		final TextView dlg_file_filter=(TextView) dialog.findViewById(R.id.sync_profile_file_filter);
//
//		NotifyEvent ntfy=new NotifyEvent(mContext);
//		//Listen setRemoteShare response 
//		ntfy.setListener(new NotifyEventListener() {
//			@Override
//			public void positiveResponse(Context arg0, Object[] arg1) {
//				String f_fl="";
//				if (n_file_filter!=null) {
//					String cn="";
//					for (int i=0;i<n_file_filter.size();i++) {
//						f_fl+=cn+n_file_filter.get(i).substring(1,n_file_filter.get(i).length());
//						cn=",";
//					}
//				}
//				if (f_fl.length()==0) f_fl=mContext.getString(R.string.msgs_filter_list_dlg_not_specified);
//				dlg_file_filter.setText(f_fl);
//			}
//
//			@Override
//			public void negativeResponse(Context arg0, Object[] arg1) {}
//			
//		});
//		editFileFilterDlg(n_file_filter,ntfy);
//
//	};
	
	public void invokeEditDirFilterDlg(Dialog dialog,
			final ArrayList<String> n_dir_filter) {
//		final CheckedTextView cbmpd = (CheckedTextView)dialog.findViewById(R.id.sync_profile_master_dir_cb);
		final TextView dlg_dir_filter=(TextView) dialog.findViewById(R.id.sync_profile_dir_filter);
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.edit_profile_sync_msg);

		NotifyEvent ntfy=new NotifyEvent(mContext);
		//Listen setRemoteShare response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context arg0, Object[] arg1) {
				String d_fl="";
				if (n_dir_filter!=null) {
					String cn="";
					for (int i=0;i<n_dir_filter.size();i++) {
						d_fl+=cn+n_dir_filter.get(i).substring(1,n_dir_filter.get(i).length());
						cn=",";
					}
				}
				if (d_fl.length()==0)  d_fl=mContext.getString(R.string.msgs_filter_list_dlg_not_specified);
				dlg_dir_filter.setText(d_fl);
//				if (n_dir_filter.size()!=0) cbmpd.setVisibility(CheckedTextView.VISIBLE);//.setEnabled(true);
//				else cbmpd.setVisibility(CheckedTextView.GONE);//.setEnabled(false);
			}
			@Override
			public void negativeResponse(Context arg0, Object[] arg1) {}
		});

		Spinner spinner_master=(Spinner)dialog.findViewById(R.id.edit_profile_sync_dlg_master_spinner);
//		Spinner spinner_target=(Spinner)dialog.findViewById(R.id.sync_profile_target_spinner);
//		String m_prof_type=spinner_master.getSelectedItem().toString().substring(0,1);
		String m_prof_name=spinner_master.getSelectedItem().toString().substring(2);
		if (getProfileType(m_prof_name,mGp.profileAdapter)
				.equals("")) {
			if (m_prof_name.length()==0) {
				dlg_msg.setText(msgs_audit_msgs_master2);
			} else {
				dlg_msg.setText(String.format(
					mContext.getString(R.string.msgs_filter_list_dlg_master_prof_not_found), m_prof_name));
			}
			return;
		}
		editDirFilterDlg(mGp.profileAdapter,m_prof_name,n_dir_filter,ntfy);
	};
	
	@SuppressLint("SdCardPath")
	public static void setLocalMountPointSpinner(GlobalParameters gp,
			Context c, Spinner spinner, String prof_lmp) {
//		Local mount pointの設定
		AdapterLocalMountPointSpinner adapter = 
        		new AdapterLocalMountPointSpinner(c,
//        				android.R.layout.simple_spinner_item);
        				R.layout.custom_simple_spinner_item, gp.localMountPointList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setPrompt(c.getString(R.string.msgs_local_profile_dlg_local_mount_point));
        spinner.setAdapter(adapter);

//        ArrayList<String>ml=LocalMountPoint.getLocalMountPointList(c);
//        if (ml.size()==0) ml.add("/sdcard");
//
//        if (!prof_lmp.equals("")) {
//            boolean add_lmp=false;
//            for(int i=0;i<ml.size();i++) {
//            	if (ml.get(i).equals(prof_lmp)) {
//            		add_lmp=true;
//            		break;
//            	}
//            }
//            if (!add_lmp) {
//            	ml.add(prof_lmp);
//            }
//        }
//    	Collections.sort(ml);
        
        for (int i=0;i<gp.localMountPointList.size();i++) {
			adapter.add(gp.localMountPointList.get(i));
			if (gp.localMountPointList.get(i).equals(prof_lmp)) spinner.setSelection(i);
		}
        adapter.notifyDataSetChanged();
	};
	
	public void setSyncDialogIcon(String prof, ImageView iv) {
		if (prof==null || prof.length()==0) {
			iv.setImageResource(R.drawable.blank);
		} else {
			if (getProfileType(prof,mGp.profileAdapter).equals("R")) {
				iv.setImageResource(R.drawable.ic_32_server);
			} else {
				iv.setImageResource(R.drawable.ic_32_mobile);
			}
		}
	};
	
	public void editFileFilterDlg(final ArrayList<String>file_filter, final NotifyEvent p_ntfy) {
		ArrayList<FilterListItem> filterList=new ArrayList<FilterListItem>() ;
		final AdapterFilterList filterAdapter;
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.filter_list_dlg);
		
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.filter_select_edit_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
		
		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.filter_select_edit_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.filter_select_edit_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);

		Button dirbtn=(Button) dialog.findViewById(R.id.filter_select_edit_dir_btn);
		dirbtn.setVisibility(Button.GONE);

		filterAdapter = new AdapterFilterList(mContext, R.layout.filter_list_item_view, filterList);
		ListView lv=(ListView) dialog.findViewById(R.id.filter_select_edit_listview);
		
		for (int i=0; i<file_filter.size();i++) {
			String inc=file_filter.get(i).substring(0,1);
			String filter=file_filter.get(i).substring(1,file_filter.get(i).length());
			boolean b_inc=false;
			if (inc.equals(SMBSYNC_PROF_FILTER_INCLUDE)) b_inc=true;
			filterAdapter.add(new FilterListItem(filter,b_inc) );
		}
		if (filterAdapter.getCount()==0) filterAdapter.add(
				new FilterListItem(mContext.getString(R.string.msgs_filter_list_no_filter),false) );
		lv.setAdapter(filterAdapter);
//		filterAdapter.getFileFilter().filter("D");
//		lv.setTextFilterEnabled(false);
//		lv.setDivider(new ColorDrawable(Color.WHITE));
		title.setText(mContext.getString(R.string.msgs_filter_list_dlg_file_filter));
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.filter_select_edit_msg);
		
		CommonDialog.setDlgBoxSizeLimit(dialog,true);
//		CommonDialog.setDlgBoxSizeCompact(dialog);

		final EditText et_filter=(EditText)dialog.findViewById(R.id.filter_select_edit_new_filter);
		final Button addBtn = (Button) dialog.findViewById(R.id.filter_select_edit_add_btn);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.filter_select_edit_cancel_btn);
		final Button btn_ok = (Button) dialog.findViewById(R.id.filter_select_edit_ok_btn);
		
        lv.setOnItemClickListener(new OnItemClickListener(){
        	public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
        		FilterListItem fli = filterAdapter.getItem(idx);
        		if (fli.getFilter().startsWith("---") || fli.isDeleted()) return;
                // リストアイテムを選択したときの処理
        		editDirFilter(idx,filterAdapter,fli,fli.getFilter());
            }
        });	 

		// Addボタンの指定
		addBtn.setEnabled(false);
		et_filter.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (s.length()!=0) {
					if (isFilterExists(s.toString().trim(),filterAdapter)) {
						String mtxt=mContext.getString(R.string.msgs_filter_list_duplicate_filter_specified);
						dlg_msg.setText(String.format(mtxt, s.toString().trim()));
						addBtn.setEnabled(false);
						btn_ok.setEnabled(true);
					} else {
						dlg_msg.setText("");
						addBtn.setEnabled(true);
						btn_ok.setEnabled(false);
					}
				} else {
					addBtn.setEnabled(false);
					btn_ok.setEnabled(true);
				}
//				et_filter.setText(s);
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before,int count) {}
		});
		addBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dlg_msg.setText("");
				String newfilter=et_filter.getText().toString().trim();
				et_filter.setText("");
				if (filterAdapter.getItem(0).getFilter().startsWith("---"))
						filterAdapter.remove(filterAdapter.getItem(0));
				filterAdapter.add(new FilterListItem(newfilter,true) );
				filterAdapter.setNotifyOnChange(true);
				filterAdapter.sort(new Comparator<FilterListItem>() {
					@Override
					public int compare(FilterListItem lhs,
							FilterListItem rhs) {
						return lhs.getFilter().compareToIgnoreCase(rhs.getFilter());
					};
				});
				btn_ok.setEnabled(true);
			}
		});

		// CANCELボタンの指定
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
//				glblParms.profileListView.setSelectionFromTop(currentViewPosX,currentViewPosY);
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
		// OKボタンの指定
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				file_filter.clear();
				if (filterAdapter.getCount()>0) {
					for (int i=0;i<filterAdapter.getCount();i++) {
						if (!filterAdapter.getItem(i).isDeleted() &&
								!filterAdapter.getItem(i).getFilter().startsWith("---")) {
							String inc=SMBSYNC_PROF_FILTER_EXCLUDE;
							if (filterAdapter.getItem(i).getInc()) inc=SMBSYNC_PROF_FILTER_INCLUDE;
							file_filter.add(inc+filterAdapter.getItem(i).getFilter());
						}
							
					}
				}
				p_ntfy.notifyToListener(true, null);
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
		dialog.show();
		
	};
	
	public void editDirFilterDlg(final AdapterProfileList prof_dapter,
			final String prof_master, final ArrayList<String>dir_filter, 
			final NotifyEvent p_ntfy) {
		ArrayList<FilterListItem> filterList=new ArrayList<FilterListItem>() ;
		final AdapterFilterList filterAdapter;
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.filter_list_dlg);
		
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.filter_select_edit_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);
		
		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.filter_select_edit_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.filter_select_edit_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);

		filterAdapter = new AdapterFilterList(mContext,
				R.layout.filter_list_item_view,filterList);
		final ListView lv=
				(ListView) dialog.findViewById(R.id.filter_select_edit_listview);
		
		for (int i=0; i<dir_filter.size();i++) {
			String inc=dir_filter.get(i).substring(0,1);
			String filter=dir_filter.get(i).substring(1,dir_filter.get(i).length());
			boolean b_inc=false;
			if (inc.equals(SMBSYNC_PROF_FILTER_INCLUDE)) b_inc=true;
			filterAdapter.add(new FilterListItem(filter,b_inc) );
		}
		if (filterAdapter.getCount()==0) filterAdapter.add(
				new FilterListItem(mContext.getString(R.string.msgs_filter_list_no_filter),false) );
		lv.setAdapter(filterAdapter);
        lv.setScrollingCacheEnabled(false);
        lv.setScrollbarFadingEnabled(false);
		
		title.setText(mContext.getString(R.string.msgs_filter_list_dlg_dir_filter));
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.filter_select_edit_msg);
		final Button dirbtn = (Button) dialog.findViewById(R.id.filter_select_edit_dir_btn);
		
		CommonDialog.setDlgBoxSizeLimit(dialog,true);

		final EditText et_filter=(EditText)dialog.findViewById(R.id.filter_select_edit_new_filter);
		final Button addbtn = (Button) dialog.findViewById(R.id.filter_select_edit_add_btn);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.filter_select_edit_cancel_btn);
		final Button btn_ok = (Button) dialog.findViewById(R.id.filter_select_edit_ok_btn);

        lv.setOnItemClickListener(new OnItemClickListener(){
        	public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
        		FilterListItem fli = filterAdapter.getItem(idx);
        		if (fli.getFilter().startsWith("---") || fli.isDeleted()) return;
                // リストアイテムを選択したときの処理
        		editDirFilter(idx,filterAdapter,fli,fli.getFilter());
            }
        });	 

		// Addボタンの指定
		et_filter.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (s.length()!=0) {
					if (isFilterExists(s.toString().trim(),filterAdapter)) {
						String mtxt=mContext.getString(R.string.msgs_filter_list_duplicate_filter_specified);
						dlg_msg.setText(String.format(mtxt, s.toString().trim()));
						addbtn.setEnabled(false);
						dirbtn.setEnabled(true);
						btn_ok.setEnabled(true);
					} else {
						dlg_msg.setText("");
						addbtn.setEnabled(true);
						dirbtn.setEnabled(false);
						btn_ok.setEnabled(false);
					}
				} else {
					addbtn.setEnabled(false);
					dirbtn.setEnabled(true);
					btn_ok.setEnabled(true);
				}
//				et_filter.setText(s);
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before,int count) {}
		});
		addbtn.setEnabled(false);
		addbtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dlg_msg.setText("");
				String newfilter=et_filter.getText().toString();
				if (isFilterExists(newfilter,filterAdapter)) {
					String mtxt=mContext.getString(R.string.msgs_filter_list_duplicate_filter_specified);
					dlg_msg.setText(String.format(mtxt, newfilter));
					return;
				}
				dlg_msg.setText("");
				et_filter.setText("");
				if (filterAdapter.getItem(0).getFilter().startsWith("---"))
					filterAdapter.remove(filterAdapter.getItem(0));
				filterAdapter.add(new FilterListItem(newfilter,true) );
				filterAdapter.setNotifyOnChange(true);
				filterAdapter.sort(new Comparator<FilterListItem>() {
					@Override
					public int compare(FilterListItem lhs,
							FilterListItem rhs) {
						return lhs.getFilter().compareToIgnoreCase(rhs.getFilter());
					};
				});
				dirbtn.setEnabled(true);
				btn_ok.setEnabled(true);
			}
		});
		// Directoryボタンの指定
		if (getProfileType(prof_master,prof_dapter).equals("L")) {
			if (!mGp.externalStorageIsMounted) dirbtn.setEnabled(false);
		} else if (getProfileType(prof_master,prof_dapter).equals("R")) {
			if (util.isRemoteDisable()) dirbtn.setEnabled(false);
		} else dirbtn.setEnabled(false);
		dirbtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						dlg_msg.setText("");
					}
					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {
						if (arg1!=null) dlg_msg.setText((String)arg1[0]);
						else dlg_msg.setText("");
					}
				});
				listDirFilter(prof_dapter, prof_master,dir_filter,filterAdapter,ntfy);
			}
		});

		// CANCELボタンの指定
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
//				glblParms.profileListView.setSelectionFromTop(currentViewPosX,currentViewPosY);
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
		// OKボタンの指定
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				dir_filter.clear();
				if (filterAdapter.getCount()>0) {
					for (int i=0;i<filterAdapter.getCount();i++) {
						if (!filterAdapter.getItem(i).isDeleted() &&
								!filterAdapter.getItem(i).getFilter().startsWith("---")) {
							String inc=SMBSYNC_PROF_FILTER_EXCLUDE;
							if (filterAdapter.getItem(i).getInc()) inc=SMBSYNC_PROF_FILTER_INCLUDE;
							dir_filter.add(inc+filterAdapter.getItem(i).getFilter());
						}
							
					}
				}
				p_ntfy.notifyToListener(true, null);
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
		dialog.show();
		
	};

	private void editDirFilter(final int edit_idx, final AdapterFilterList fa, 
			final FilterListItem fli, final String filter) {
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.filter_edit_dlg);
		
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.filter_edit_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.filter_edit_dlg_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.filter_edit_dlg_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);

		CommonDialog.setDlgBoxSizeCompact(dialog);
		final EditText et_filter=(EditText)dialog.findViewById(R.id.filter_edit_dlg_filter);
		et_filter.setText(filter);
		// CANCELボタンの指定
		final Button btn_cancel = (Button) dialog.findViewById(R.id.filter_edit_dlg_cancel_btn);
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
//				glblParms.profileListView.setSelectionFromTop(currentViewPosX,currentViewPosY);
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
		// OKボタンの指定
		Button btn_ok = (Button) dialog.findViewById(R.id.filter_edit_dlg_ok_btn);
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				TextView dlg_msg =(TextView)dialog.findViewById(R.id.filter_edit_dlg_msg);
				
				String newfilter=et_filter.getText().toString();
				if (!filter.equals(newfilter)) {
					if (isFilterExists(newfilter,fa)) {
						String mtxt=mContext.getString(R.string.msgs_filter_list_duplicate_filter_specified);
						dlg_msg.setText(String.format(mtxt,newfilter));
						return;
					}
				}
				dialog.dismiss();

				fa.remove(fli);
				fa.insert(fli, edit_idx);
				fli.setFilter(newfilter);
				
				et_filter.setText("");
				
				fa.setNotifyOnChange(true);
				fa.sort(new Comparator<FilterListItem>() {
					@Override
					public int compare(FilterListItem lhs,
							FilterListItem rhs) {
						return lhs.getFilter().compareToIgnoreCase(rhs.getFilter());
					};
				});
//				p_ntfy.notifyToListener(true, null);
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
		dialog.show();
		
	};

	private boolean isFilterExists(String nf, AdapterFilterList fa) {
		if (fa.getCount()==0) return false;
		for (int i=0;i<fa.getCount();i++) {
			if (!fa.getItem(i).isDeleted())
				if (fa.getItem(i).getFilter().equals(nf)) return true;
		}
		return false;
	};
	
	private void listDirFilter(AdapterProfileList t_prof,
			String prof_master, final ArrayList<String>dir_filter, 
			AdapterFilterList fla, final NotifyEvent p_ntfy) {
		if (getProfileType(prof_master,t_prof).equals("L")) {
			listDirFilterLocal(t_prof, prof_master, dir_filter, fla, p_ntfy);
		} else {
			listDirFilterRemote(t_prof, prof_master, dir_filter, fla, p_ntfy);
		}
	};

	private void listDirFilterLocal(AdapterProfileList t_prof,
			String prof_master, final ArrayList<String>dir_filter, 
			final AdapterFilterList fla, final NotifyEvent p_ntfy) {
		ProfileListItem t_i=null;
		
		for (int i=0;i<t_prof.getCount();i++) 
			if (t_prof.getItem(i).getProfileName().equals(prof_master)) 
				t_i=t_prof.getItem(i);
		if (t_i==null) p_ntfy.notifyToListener(false, new Object[]{ 
			String.format(mContext.getString(
				R.string.msgs_filter_list_dlg_master_prof_not_found),prof_master)});
		
		final ProfileListItem item=t_i;
		final String cdir=item.getDirectoryName();
		
    	//カスタムダイアログの生成
        final Dialog dialog=new Dialog(mContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
    	dialog.setContentView(R.layout.item_select_list_dlg);
    	
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.item_select_list_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.item_select_list_dlg_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.item_select_list_dlg_title);
		final TextView subtitle = (TextView) dialog.findViewById(R.id.item_select_list_dlg_subtitle);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		subtitle.setTextColor(mGp.themeColorList.text_color_dialog_title);
		
		title.setText(mContext.getString(R.string.msgs_filter_list_dlg_add_dir_filter));
		subtitle.setText(msgs_current_dir+" "+item.getLocalMountPoint()+"/"+cdir);
        final TextView dlg_msg=(TextView)dialog.findViewById(R.id.item_select_list_dlg_msg);
	    final Button btn_ok=(Button)dialog.findViewById(R.id.item_select_list_dlg_ok_btn);

	    final LinearLayout ll_context=(LinearLayout)dialog.findViewById(R.id.context_view_file_select);
        ll_context.setVisibility(LinearLayout.VISIBLE);
        final ImageButton ib_select_all=(ImageButton)ll_context.findViewById(R.id.context_button_select_all);
        final ImageButton ib_unselect_all=(ImageButton)ll_context.findViewById(R.id.context_button_unselect_all);

        dlg_msg.setVisibility(TextView.VISIBLE);

//        if (rows.size()<=2) 
//        	((TextView)dialog.findViewById(R.id.item_select_list_dlg_spacer))
//        	.setVisibility(TextView.VISIBLE);

        CommonDialog.setDlgBoxSizeLimit(dialog, true);
		
        final ListView lv = (ListView) dialog.findViewById(android.R.id.list);
        final TreeFilelistAdapter tfa= 
        		new TreeFilelistAdapter(mContext, false,false);
        lv.setAdapter(tfa);
        ArrayList<TreeFilelistItem> tfl=createLocalFilelist(true,item.getLocalMountPoint(),"/"+cdir);
        if (tfl.size()<1) tfl.add(new TreeFilelistItem(msgs_dir_empty));
        tfa.setDataList(tfl);
        lv.setScrollingCacheEnabled(false);
        lv.setScrollbarFadingEnabled(false);
        
        NotifyEvent ntfy_expand_close=new NotifyEvent(mContext);
        ntfy_expand_close.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				int idx=(Integer)o[0];
	    		final int pos=tfa.getItem(idx);
	    		final TreeFilelistItem tfi=tfa.getDataItem(pos);
				if (tfi.getName().startsWith("---")) return;
				expandHideLocalDirTree(true,item.getLocalMountPoint(), pos,tfi,tfa);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
        });
        tfa.setExpandCloseListener(ntfy_expand_close);
        lv.setOnItemClickListener(new OnItemClickListener(){
        	public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
	    		final int pos=tfa.getItem(idx);
	    		final TreeFilelistItem tfi=tfa.getDataItem(pos);
				if (tfi.getName().startsWith("---")) return;
				expandHideLocalDirTree(true,item.getLocalMountPoint(), pos,tfi,tfa);
//				tfa.setDataItemIsSelected(pos);
			}
        });
        
		lv.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					final int position, long arg3) {
				return true;
			}
		});

	    ib_select_all.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				for(int i=0;i<tfa.getDataItemCount();i++) {
					TreeFilelistItem tfli=tfa.getDataItem(i);
					if (!tfli.isHideListItem()) tfa.setDataItemIsSelected(i);
				}
				tfa.notifyDataSetChanged();
				btn_ok.setEnabled(true);
			}
	    });

	    ib_unselect_all.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				for(int i=0;i<tfa.getDataItemCount();i++) {
					tfa.setDataItemIsUnselected(i);
				}
				tfa.notifyDataSetChanged();
				btn_ok.setEnabled(false);
			}
	    });

	    //OKボタンの指定
	    btn_ok.setEnabled(false);
        NotifyEvent ntfy=new NotifyEvent(mContext);
		//Listen setRemoteShare response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context arg0, Object[] arg1) {
				btn_ok.setEnabled(true);
			}
			@Override
			public void negativeResponse(Context arg0, Object[] arg1) {
				boolean checked=false;
				for (int i=0;i<tfa.getDataItemCount();i++) {
					if (tfa.getDataItem(i).isChecked()) {
						checked=true;
						break;
					}
				}
				if (checked) btn_ok.setEnabled(true);
				else btn_ok.setEnabled(false);
			}
		});
		tfa.setCbCheckListener(ntfy);
		
	    btn_ok.setText(mContext.getString(R.string.msgs_filter_list_dlg_add));
	    btn_ok.setVisibility(Button.VISIBLE);
	    btn_ok.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	        	if (!addDirFilter(true,tfa,fla,"/"+cdir+"/",dlg_msg)) return;
	        	addDirFilter(false,tfa,fla,"/"+cdir+"/",dlg_msg);
	            dialog.dismiss();
	            p_ntfy.notifyToListener(true,null );
	        }
	    });

        //CANCELボタンの指定
        final Button btn_cancel=(Button)dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
        btn_cancel.setText(mContext.getString(R.string.msgs_filter_list_dlg_close));
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                p_ntfy.notifyToListener(true, null);
            }
        });
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
//        dialog.setOnKeyListener(new DialogOnKeyListener(context));
//        dialog.setCancelable(false);
        dialog.show();
		
		return ;
 
	};
	
	private void listDirFilterRemote(AdapterProfileList t_prof,
			String prof_master, final ArrayList<String>dir_filter, 
			final AdapterFilterList fla, final NotifyEvent p_ntfy) {
		ProfileListItem item=null;
		
		for (int i=0;i<t_prof.getCount();i++) 
			if (t_prof.getItem(i).getProfileName().equals(prof_master)) 
				item=t_prof.getItem(i);
		if (item==null) p_ntfy.notifyToListener(false, new Object[]{
			String.format(mContext.getString(
					R.string.msgs_filter_list_dlg_master_prof_not_found),prof_master)});

		setSmbUserPass(item.getRemoteUserID(),item.getRemotePassword());
		String t_remurl="";
		if (item.getRemoteHostname().equals("")) t_remurl=item.getRemoteAddr();
		else t_remurl=item.getRemoteHostname();
		String h_port="";
		if (!item.getRemotePassword().equals("")) h_port=":"+item.getRemotePort();
		final String remurl="smb://"+t_remurl+h_port+"/"+item.getRemoteShareName();
		final String remdir="/"+item.getDirectoryName()+"/";

		NotifyEvent ntfy=new NotifyEvent(mContext);
		// set thread response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				final ArrayList<TreeFilelistItem> rows = new ArrayList<TreeFilelistItem>();
				@SuppressWarnings("unchecked")
				ArrayList<TreeFilelistItem> rfl=(ArrayList<TreeFilelistItem>)o[0];
				
				for (int i=0;i<rfl.size();i++){
					if (rfl.get(i).isDir() && rfl.get(i).canRead()) rows.add(rfl.get(i));
				}
				Collections.sort(rows);
				if (rows.size()<1) rows.add(new TreeFilelistItem(msgs_dir_empty));
				//カスタムダイアログの生成
				final Dialog dialog=new Dialog(mContext);
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				dialog.setCanceledOnTouchOutside(false);
				dialog.setContentView(R.layout.item_select_list_dlg);
				
				LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.item_select_list_dlg_view);
				ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

				final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.item_select_list_dlg_title_view);
				final TextView title = (TextView) dialog.findViewById(R.id.item_select_list_dlg_title);
				final TextView subtitle = (TextView) dialog.findViewById(R.id.item_select_list_dlg_subtitle);
				title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
				title.setTextColor(mGp.themeColorList.text_color_dialog_title);
				subtitle.setTextColor(mGp.themeColorList.text_color_dialog_title);

				title.setText(mContext.getString(R.string.msgs_filter_list_dlg_add_dir_filter));
				subtitle.setText(msgs_current_dir+" "+remurl+remdir);
		        final TextView dlg_msg=(TextView)dialog.findViewById(R.id.item_select_list_dlg_msg);
		        final LinearLayout ll_context=(LinearLayout)dialog.findViewById(R.id.context_view_file_select);
		        ll_context.setVisibility(LinearLayout.VISIBLE);
		        final ImageButton ib_select_all=(ImageButton)ll_context.findViewById(R.id.context_button_select_all);
		        final ImageButton ib_unselect_all=(ImageButton)ll_context.findViewById(R.id.context_button_unselect_all);
			    final Button btn_ok=(Button)dialog.findViewById(R.id.item_select_list_dlg_ok_btn);
		        dlg_msg.setVisibility(TextView.VISIBLE);
				
//		        if (rows.size()<=2) 
//		        	((TextView)dialog.findViewById(R.id.item_select_list_dlg_spacer))
//		        	.setVisibility(TextView.VISIBLE);
				
				CommonDialog.setDlgBoxSizeLimit(dialog, true);
				
				final ListView lv = (ListView) dialog.findViewById(android.R.id.list);
			    final TreeFilelistAdapter tfa=new TreeFilelistAdapter(mContext,false,false);
				tfa.setDataList(rows);
			    lv.setAdapter(tfa);
			    lv.setScrollingCacheEnabled(false);
			    lv.setScrollbarFadingEnabled(false);
//			    lv.setFastScrollEnabled(true);
		        NotifyEvent ntfy_expand_close=new NotifyEvent(mContext);
		        ntfy_expand_close.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						int idx=(Integer)o[0];
			    		final int pos=tfa.getItem(idx);
			    		final TreeFilelistItem tfi=tfa.getDataItem(pos);
						if (tfi.getName().startsWith("---")) return;
						expandHideRemoteDirTree(remurl, pos,tfi,tfa);
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
		        });
		        tfa.setExpandCloseListener(ntfy_expand_close);
		        lv.setOnItemClickListener(new OnItemClickListener(){
		        	public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
			    		final int pos=tfa.getItem(idx);
			    		final TreeFilelistItem tfi=tfa.getDataItem(pos);
						if (tfi.getName().startsWith("---")) return;
//						tfa.setDataItemIsSelected(pos);
						expandHideRemoteDirTree(remurl, pos,tfi,tfa);
					}
		        });
		        
				lv.setOnItemLongClickListener(new OnItemLongClickListener(){
					@Override
					public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
							final int position, long arg3) {
						return true;
					}
				});

			    ib_select_all.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						for(int i=0;i<tfa.getDataItemCount();i++) {
							TreeFilelistItem tfli=tfa.getDataItem(i);
							if (!tfli.isHideListItem()) tfa.setDataItemIsSelected(i);
						}
						tfa.notifyDataSetChanged();
						btn_ok.setEnabled(true);
					}
			    });

			    ib_unselect_all.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						for(int i=0;i<tfa.getDataItemCount();i++) {
							tfa.setDataItemIsUnselected(i);
						}
						tfa.notifyDataSetChanged();
						btn_ok.setEnabled(false);
					}
			    });

				//OKボタンの指定
			    btn_ok.setEnabled(false);
		        NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						btn_ok.setEnabled(true);
					}
					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {
						if (tfa.isDataItemIsSelected()) btn_ok.setEnabled(true);
						else btn_ok.setEnabled(false);
					}
				});
				tfa.setCbCheckListener(ntfy);

			    btn_ok.setText(mContext.getString(R.string.msgs_filter_list_dlg_add));
			    btn_ok.setVisibility(Button.VISIBLE);
			    btn_ok.setOnClickListener(new View.OnClickListener() {
			        public void onClick(View v) {
			        	if (!addDirFilter(true,tfa,fla,remdir,dlg_msg)) return;
			        	addDirFilter(false,tfa,fla,remdir,dlg_msg);
			            dialog.dismiss();
			            p_ntfy.notifyToListener(true,null );
			        }
			    });
				//CANCELボタンの指定
				final Button btn_cancel=(Button)dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
				btn_cancel.setText(mContext.getString(R.string.msgs_filter_list_dlg_close));
				btn_cancel.setOnClickListener(new View.OnClickListener() {
				    public void onClick(View v) {
				        dialog.dismiss();
				        p_ntfy.notifyToListener(true, null);
				    }
				});
				// Cancelリスナーの指定
				dialog.setOnCancelListener(new Dialog.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
						btn_cancel.performClick();
					}
				});
//				dialog.setOnKeyListener(new DialogOnKeyListener(context));
//				dialog.setCancelable(false);
				dialog.show();
			}

			@Override
			public void negativeResponse(Context c,Object[] o) {
				p_ntfy.notifyToListener(false, o);
			}
		});
		createRemoteFileList(remurl,remdir,ntfy,true);
	};
	
	private boolean addDirFilter(boolean check_only, TreeFilelistAdapter tfa, 
			AdapterFilterList fla, String cdir, TextView dlg_msg) {
        String sel="", add_msg="";
        //check duplicate entry
        for (int i=0;i<tfa.getCount();i++) {
        	if (tfa.getDataItem(i).isChecked()) {
        		if (tfa.getDataItem(i).getPath().length()==1) sel=tfa.getDataItem(i).getName();
        		else sel=tfa.getDataItem(i).getPath()+tfa.getDataItem(i).getName();
        		sel=sel.replaceFirst(cdir, "");
        		if (isFilterExists(sel,fla)) {
        			String mtxt=mContext.getString(R.string.msgs_filter_list_duplicate_filter_specified);
					dlg_msg.setText(String.format(mtxt,sel));
					return false;
				}
        		if (!check_only) {
    				fla.add(new FilterListItem(sel,true) );
    				if (add_msg.length()==0) add_msg=sel;
    				else add_msg=add_msg+","+sel;
        		}
        	}
        }
        if (!check_only) {
			fla.setNotifyOnChange(true);
			fla.sort(new Comparator<FilterListItem>() {
				@Override
				public int compare(FilterListItem lhs,
						FilterListItem rhs) {
					return lhs.getFilter().compareToIgnoreCase(rhs.getFilter());
				};
			});
			dlg_msg.setText(String.format(mContext.getString(R.string.msgs_filter_list_dlg_filter_added), 
					add_msg));
        }        
		return true;
	};
	
	
	
//	private String auditRemoteProfileField(Dialog dialog) {
//		String prof_name, prof_addr, prof_user, prof_pass, prof_share, prof_dir, prof_host;
//		boolean audit_error=false;
//		String audit_msg="";
//		final EditText editaddr = (EditText) dialog.findViewById(R.id.remote_profile_addr);
//		final EditText edituser = (EditText) dialog.findViewById(R.id.remote_profile_user);
//		final EditText editpass = (EditText) dialog.findViewById(R.id.remote_profile_pass);
//		final EditText editshare = (EditText) dialog.findViewById(R.id.remote_profile_share);
//		final EditText editdir = (EditText) dialog.findViewById(R.id.remote_profile_dir);
//		final EditText editname = (EditText) dialog.findViewById(R.id.remote_profile_name);
//		final EditText edithost = (EditText) dialog.findViewById(R.id.remote_profile_hostname);
//		final CheckedTextView cb_use_hostname = (CheckedTextView) dialog.findViewById(R.id.remote_profile_use_computer_name);
//		final CheckedTextView cb_use_user_pass = (CheckedTextView) dialog.findViewById(R.id.remote_profile_use_user_pass);
//		
//		prof_addr = editaddr.getText().toString();
//		prof_host = edithost.getText().toString();
//		prof_user = edituser.getText().toString();
//		prof_pass = editpass.getText().toString();
//		prof_share = editshare.getText().toString();
//		prof_dir = editdir.getText().toString();
//		prof_name = editname.getText().toString();
//		if (hasInvalidChar(prof_name,new String[]{"\t"})) {
//			audit_error=true;
//			prof_name=removeInvalidChar(prof_name);
//			audit_msg=String.format(msgs_audit_msgs_profilename1,detectedInvalidCharMsg);
//			editname.setText(prof_name);
//			editdir.requestFocus();
//		} else if (prof_name.length()==0) {
//			audit_error=true;
//			audit_msg=msgs_audit_msgs_profilename2;
//			editname.requestFocus();
//		}
//		if (!audit_error) {
//			if (hasInvalidChar(prof_addr,new String[]{"\t"})) {
//				audit_error=true;
//				prof_addr=removeInvalidChar(prof_addr);
//				audit_msg=String.format(msgs_audit_msgs_address1,detectedInvalidCharMsg);
//				editaddr.setText(prof_addr);
//				editaddr.requestFocus();
//			} else {
//				if (!cb_use_hostname.isChecked() && prof_addr.length()==0) {
//					audit_error=true;
//					audit_msg=msgs_audit_msgs_address2;
//					editaddr.requestFocus();
//				} else {
//					if (cb_use_hostname.isChecked() && prof_host.length()==0) {
//						audit_error=true;
//						audit_msg=msgs_audit_msgs_address2;
//						editaddr.requestFocus();
//					} else {
//						audit_error=false;
//					}
//				}
//			}
//		}
//		if (!audit_error) {
//			if (cb_use_user_pass.isChecked()) {
//				if (hasInvalidChar(prof_user,new String[]{"\t"})) {
//					audit_error=true;
//					prof_user=removeInvalidChar(prof_user);
//					audit_msg=String.format(msgs_audit_msgs_username1,detectedInvalidCharMsg);
//					edituser.setText(prof_user);
//					edituser.requestFocus();
//				} else {
//					if (prof_user.equals("") && prof_pass.equals("")) {
//						audit_error=true;
//						audit_msg=String.format(msgs_audit_msgs_user_or_pass_missing,detectedInvalidCharMsg);
//						edituser.requestFocus();
//					} else {
//						audit_error=false;
//					}
//				}
//			}
//		}
//		if (!audit_error) {
//			if (cb_use_user_pass.isChecked()) {
//				if (hasInvalidChar(prof_pass,new String[]{"\t"})) {
//					audit_error=true;
//					prof_pass=removeInvalidChar(prof_pass);
//					audit_msg=String.format(msgs_audit_msgs_password1,detectedInvalidCharMsg);
//					editpass.setText(prof_pass);
//					editpass.requestFocus();
//				} else {
//					if (prof_user.equals("") && prof_pass.equals("")) {
//						audit_error=true;
//						audit_msg=String.format(msgs_audit_msgs_user_or_pass_missing,detectedInvalidCharMsg);
//						editpass.requestFocus();
//					} else {
//						audit_error=false;
//					}
//				}
//			}
//		}
//		if (!audit_error) {					
//			if (hasInvalidChar(prof_share,new String[]{"\t"})) {
//				audit_error=true;
//				prof_share=removeInvalidChar(prof_share);
//				audit_msg=String.format(msgs_audit_msgs_share1,detectedInvalidCharMsg);
//				editshare.setText(prof_share);
//				editshare.requestFocus();
//			} else if (prof_share.length()==0) {
//				audit_error=true;
//				audit_msg=msgs_audit_msgs_share2;
//				editshare.requestFocus();
//			}
//		}
//		if (!audit_error) {
//			if (hasInvalidChar(prof_dir,new String[]{"\t"})) {
//				audit_error=true;
//				prof_dir=removeInvalidChar(prof_dir);
//				audit_msg=String.format(msgs_audit_msgs_dir1,detectedInvalidCharMsg);
//				editdir.setText(prof_dir);
//				editdir.requestFocus();
//			}
//		}
//		return audit_msg;
//	};

	private String detectedInvalidChar="",detectedInvalidCharMsg="";
	public boolean hasInvalidChar(String in_text,String[] invchar) {
		for (int i=0;i<invchar.length;i++) {
			if (in_text.indexOf(invchar[i])>=0) {
				if (invchar[i].equals("\t")) {
					detectedInvalidCharMsg="TAB";
					detectedInvalidChar="\t";
				} else {
					detectedInvalidCharMsg=detectedInvalidChar=invchar[i];
				}
				return true;
			}
			
		}
		return false ;
	};
	
	public String getInvalidCharMsg() {
		return detectedInvalidCharMsg;
	};

	public String removeInvalidChar(String in){
		if (detectedInvalidChar==null || detectedInvalidChar.length()==0) return in;
		String out="";
		for (int i=0;i<in.length();i++) {
			if (in.substring(i,i+1).equals(detectedInvalidChar)) {
				//ignore
			} else {
				out=out+in.substring(i,i+1);
			}
		}
		return out;
	}
	public boolean isProfileExists(String prof_grp,String prof_type, 
			String prof_name) {
		return isProfileExists(prof_grp, prof_type, prof_name, mGp.profileAdapter.getArrayList());
	};

	static public boolean isProfileExists(String prof_grp,String prof_type, 
			String prof_name, ArrayList<ProfileListItem>pfl) {
		boolean dup = false;

		for (int i = 0; i <= pfl.size() - 1; i++) {
			ProfileListItem item = pfl.get(i);
			String prof_chk=item.getProfileGroup()+item.getProfileName();
			if (prof_chk.equals(prof_grp+prof_name)) {
				dup = true;
				break;
			}
		}
		return dup;
	};

	static public boolean isProfileActive(GlobalParameters gp,
			String prof_grp,String prof_type, 
			String prof_name) {
		boolean active = false;

		for (int i = 0; i <= gp.profileAdapter.getCount() - 1; i++) {
			String item_key=gp.profileAdapter.getItem(i).getProfileGroup()+
					gp.profileAdapter.getItem(i).getProfileType()+
					gp.profileAdapter.getItem(i).getProfileName();
			if (item_key.equals(prof_grp+prof_type+prof_name)) {
				active=gp.profileAdapter.getItem(i).isProfileActive();
			}
		}
		return active;
	};

	static public boolean isAnyProfileSelected(AdapterProfileList pa, String prof_grp) {
		boolean result = false;

		for (int i = 0; i<pa.getCount(); i++) {
			if (pa.getItem(i).getProfileGroup().equals(prof_grp) && pa.getItem(i).isChecked()) {
				result=true;
				break;
			}
		}
		return result;
	};

	static public int getAnyProfileSelectedItemCount(AdapterProfileList pa, String prof_grp) {
		int result = 0;

		for (int i = 0; i<pa.getCount(); i++) {
			if (pa.getItem(i).getProfileGroup().equals(prof_grp) && pa.getItem(i).isChecked()) {
				result++;
			}
		}
		return result;
	};

	static public int getSyncProfileSelectedItemCount(AdapterProfileList pa, String prof_grp) {
		int result = 0;

		for (int i = 0; i<pa.getCount(); i++) {
			if (pa.getItem(i).getProfileGroup().equals(prof_grp) &&
					pa.getItem(i).getProfileType().equals(SMBSYNC_PROF_TYPE_SYNC) &&
					pa.getItem(i).isChecked()) {
				result++;
			}
		}
		return result;
	};

	public void scanRemoteNetworkDlg(final NotifyEvent p_ntfy, 
			String port_number, boolean scan_start) {
		//カスタムダイアログの生成
	    final Dialog dialog=new Dialog(mContext);
	    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    dialog.setCanceledOnTouchOutside(false);
	    dialog.setContentView(R.layout.scan_remote_ntwk_dlg);
	    
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.scan_remote_ntwk_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.scan_remote_ntwk_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.scan_remote_ntwk_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);

	    final Button btn_scan=(Button)dialog.findViewById(R.id.scan_remote_ntwk_btn_ok);
	    final Button btn_cancel=(Button)dialog.findViewById(R.id.scan_remote_ntwk_btn_cancel);
	    final TextView tvmsg = (TextView) dialog.findViewById(R.id.scan_remote_ntwk_msg);
	    final TextView tv_result = (TextView) dialog.findViewById(R.id.scan_remote_ntwk_scan_result_title);
	    tvmsg.setText(mContext.getString(R.string.msgs_scan_ip_address_press_scan_btn));
	    tv_result.setVisibility(TextView.GONE);
	    
		final String from=SMBSyncUtil.getLocalIpAddress();
		String subnet=from.substring(0,from.lastIndexOf("."));
		String subnet_o1, subnet_o2,subnet_o3;
		subnet_o1=subnet.substring(0,subnet.indexOf("."));
		subnet_o2=subnet.substring(subnet.indexOf(".")+1,subnet.lastIndexOf("."));
		subnet_o3=subnet.substring(subnet.lastIndexOf(".")+1,subnet.length());
		final EditText baEt1 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o1);
		final EditText baEt2 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o2);
		final EditText baEt3 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o3);
		final EditText baEt4 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o4);
		final EditText eaEt4 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_end_address_o4);
		baEt1.setText(subnet_o1);
		baEt2.setText(subnet_o2);
		baEt3.setText(subnet_o3);
		baEt4.setText("1");
		baEt4.setSelection(1);
		eaEt4.setText("254");
		baEt4.requestFocus();
		
		final CheckedTextView ctv_use_port_number = (CheckedTextView) dialog.findViewById(R.id.scan_remote_ntwk_ctv_use_port);
		final EditText et_port_number = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_port_number);

		final LinearLayout ll_port=(LinearLayout) dialog.findViewById(R.id.scan_remote_ntwk_port_option);
		if (mGp.settingShowRemotePortOption) {
			ll_port.setVisibility(LinearLayout.VISIBLE);
		} else {
			ll_port.setVisibility(LinearLayout.GONE);
		}
		
		
	    CommonDialog.setDlgBoxSizeLimit(dialog, true);
	    
	    if (port_number.equals("")) {
		    et_port_number.setEnabled(false);
		    ctv_use_port_number.setChecked(false);
	    } else {
		    et_port_number.setEnabled(true);
		    et_port_number.setText(port_number);
		    ctv_use_port_number.setChecked(true);
	    }
	    ctv_use_port_number.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctv_use_port_number.toggle();
				boolean isChecked=ctv_use_port_number.isChecked();
				et_port_number.setEnabled(isChecked);
			}
		});
	    
	    final NotifyEvent ntfy_lv_click=new NotifyEvent(mContext);
	    ntfy_lv_click.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
	            dialog.dismiss();
				p_ntfy.notifyToListener(true,o);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
	    });
	    
		final ArrayList<ScanAddressResultListItem> ipAddressList = new ArrayList<ScanAddressResultListItem>();
//		ScanAddressResultListItem li=new ScanAddressResultListItem();
//		li.server_name=mContext.getString(R.string.msgs_ip_address_no_address);
//		ipAddressList.add(li);
	    final ListView lv = (ListView) dialog.findViewById(R.id.scan_remote_ntwk_scan_result_list);
	    final AdapterScanAddressResultList adap=new AdapterScanAddressResultList
		    	(mContext, R.layout.scan_address_result_list_item, ipAddressList, ntfy_lv_click);
	    lv.setAdapter(adap);
	    lv.setScrollingCacheEnabled(false);
	    lv.setScrollbarFadingEnabled(false);
	    
	    //SCANボタンの指定
	    btn_scan.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	            ipAddressList.clear();
	            NotifyEvent ntfy=new NotifyEvent(mContext);
	    		ntfy.setListener(new NotifyEventListener() {
	    			@Override
	    			public void positiveResponse(Context c,Object[] o) {
	    				if (ipAddressList.size()<1) {
	    					tvmsg.setText(mContext.getString(R.string.msgs_scan_ip_address_not_detected));
	    					tv_result.setVisibility(TextView.GONE);
	    				} else {
	    					tvmsg.setText(mContext.getString(R.string.msgs_scan_ip_address_select_detected_host));
	    					tv_result.setVisibility(TextView.VISIBLE);
	    				}
//	    				adap.clear();
//	    				for (int i=0;i<ipAddressList.size();i++) 
//	    					adap.add(ipAddressList.get(i));
	    			}
	    			@Override
	    			public void negativeResponse(Context c,Object[] o) {}

	    		});
				if (auditScanAddressRangeValue(dialog)) {
					tv_result.setVisibility(TextView.GONE);
					String ba1=baEt1.getText().toString();
					String ba2=baEt2.getText().toString();
					String ba3=baEt3.getText().toString();
					String ba4=baEt4.getText().toString();
					String ea4=eaEt4.getText().toString();
					String subnet=ba1+"."+ba2+"."+ba3;
					int begin_addr = Integer.parseInt(ba4);
					int end_addr = Integer.parseInt(ea4);
					scanRemoteNetwork(dialog,lv,adap,ipAddressList,
							subnet, begin_addr, end_addr, ntfy);
				} else {
					//error
				}
	        }
	    });

	    //CANCELボタンの指定
	    btn_cancel.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	            dialog.dismiss();
	            p_ntfy.notifyToListener(false, null);
	        }
	    });
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
	    dialog.show();

	    if (scan_start) btn_scan.performClick();
	};

	private int mScanCompleteCount=0, mScanAddrCount=0;
	private ArrayList<String> mScanRequestedAddrList=new ArrayList<String>();
	private String mLockScanCompleteCount="";
	private void scanRemoteNetwork(
			final Dialog dialog,
			final ListView lv_ipaddr,
			final AdapterScanAddressResultList adap,
			final ArrayList<ScanAddressResultListItem> ipAddressList,
			final String subnet, final int begin_addr, final int end_addr,
			final NotifyEvent p_ntfy) {
		final Handler handler=new Handler();
		final ThreadCtrl tc=new ThreadCtrl();
		final LinearLayout ll_addr=(LinearLayout) dialog.findViewById(R.id.scan_remote_ntwk_scan_address);
		final LinearLayout ll_prog=(LinearLayout) dialog.findViewById(R.id.scan_remote_ntwk_progress);
		final TextView tvmsg=(TextView) dialog.findViewById(R.id.scan_remote_ntwk_progress_msg);
		final Button btn_scan = (Button) dialog.findViewById(R.id.scan_remote_ntwk_btn_ok);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.scan_remote_ntwk_btn_cancel);
		final Button scan_cancel = (Button) dialog.findViewById(R.id.scan_remote_ntwk_progress_cancel);
		
		final CheckedTextView ctv_use_port_number = (CheckedTextView) dialog.findViewById(R.id.scan_remote_ntwk_ctv_use_port);
		final EditText et_port_number = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_port_number);

		tvmsg.setText("");
		scan_cancel.setText(R.string.msgs_progress_spin_dlg_addr_cancel);
		ll_addr.setVisibility(LinearLayout.GONE);
		ll_prog.setVisibility(LinearLayout.VISIBLE);
		btn_scan.setEnabled(false);
		btn_cancel.setEnabled(false);
		adap.setButtonEnabled(false);
		scan_cancel.setEnabled(true);
	    dialog.setOnKeyListener(new DialogBackKeyListener(mContext));
	    dialog.setCancelable(false);
		// CANCELボタンの指定
		scan_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				scan_cancel.setText(mContext.getString(R.string.msgs_progress_dlg_canceling));
				scan_cancel.setEnabled(false);
				util.addDebugLogMsg(1,"W","IP Address list creation was cancelled");
				tc.setDisabled();
			}
		});
		if (util.isActivityForeground()) dialog.show();
		
		mScanRequestedAddrList.clear();
		
		util.addDebugLogMsg(1,"I","Scan IP address ransge is "+subnet+ "."+begin_addr+" - "+end_addr);
		
		final String scan_prog=mContext.getString(R.string.msgs_ip_address_scan_progress);
		String p_txt=String.format(scan_prog,0);
		tvmsg.setText(p_txt);

       	new Thread(new Runnable() {
			@Override
			public void run() {//non UI thread
				mScanCompleteCount=0;
				mScanAddrCount=end_addr-begin_addr+1;
				int scan_thread=60;
				String scan_port="";
				if (ctv_use_port_number.isChecked()) scan_port=et_port_number.getText().toString();
				for (int i=begin_addr; i<=end_addr;i+=scan_thread) {
					if (!tc.isEnabled()) break;
					boolean scan_end=false;
					for (int j=i;j<(i+scan_thread);j++) {
						if (j<=end_addr) {
							startRemoteNetworkScanThread(handler, tc, dialog, p_ntfy,
									lv_ipaddr, adap, tvmsg, subnet+"."+j,ipAddressList, scan_port);
						} else {
							scan_end=true;
						}
					}
					if (!scan_end) {
						for (int wc=0;wc<210;wc++) {
							if (!tc.isEnabled()) break;
							SystemClock.sleep(30);
						}
					}
				}
				if (!tc.isEnabled()) {
					for(int i=0;i<1000;i++) {
						SystemClock.sleep(100);
						synchronized(mScanRequestedAddrList) {
							if (mScanRequestedAddrList.size()==0) break;
						}
					}

					handler.post(new Runnable() {// UI thread
						@Override
						public void run() {
							closeScanRemoteNetworkProgressDlg(dialog, p_ntfy, lv_ipaddr, adap, tvmsg);
						}
					});
				} else {
					for(int i=0;i<1000;i++) {
						SystemClock.sleep(100);
						synchronized(mScanRequestedAddrList) {
							if (mScanRequestedAddrList.size()==0) break;
						}
					}
					handler.post(new Runnable() {// UI thread
						@Override
						public void run() {
							synchronized(mLockScanCompleteCount) {
								lv_ipaddr.setSelection(lv_ipaddr.getCount());
								adap.notifyDataSetChanged();
								closeScanRemoteNetworkProgressDlg(dialog, p_ntfy, lv_ipaddr, adap, tvmsg);
							}
						}
					});
				}
			}
		})
       	.start();
	};

	private void closeScanRemoteNetworkProgressDlg(
			final Dialog dialog,
			final NotifyEvent p_ntfy,
			final ListView lv_ipaddr,
			final AdapterScanAddressResultList adap,
			final TextView tvmsg) {
		final LinearLayout ll_addr=(LinearLayout) dialog.findViewById(R.id.scan_remote_ntwk_scan_address);
		final LinearLayout ll_prog=(LinearLayout) dialog.findViewById(R.id.scan_remote_ntwk_progress);
		final Button btn_scan = (Button) dialog.findViewById(R.id.scan_remote_ntwk_btn_ok);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.scan_remote_ntwk_btn_cancel);
		ll_addr.setVisibility(LinearLayout.VISIBLE);
		ll_prog.setVisibility(LinearLayout.GONE);
		btn_scan.setEnabled(true);
		btn_cancel.setEnabled(true);
		adap.setButtonEnabled(true);
	    dialog.setOnKeyListener(null);
	    dialog.setCancelable(true);
		if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
		
	};
	
	private void startRemoteNetworkScanThread(final Handler handler,
			final ThreadCtrl tc,
			final Dialog dialog,
			final NotifyEvent p_ntfy,
			final ListView lv_ipaddr,
			final AdapterScanAddressResultList adap,
			final TextView tvmsg,
			final String addr,
			final ArrayList<ScanAddressResultListItem> ipAddressList,
			final String scan_port) {
		final String scan_prog=mContext.getString(R.string.msgs_ip_address_scan_progress);
		Thread th=new Thread(new Runnable() {
			@Override
			public void run() {
				synchronized(mScanRequestedAddrList) {
					mScanRequestedAddrList.add(addr);
				}
				if (isIpAddrSmbHost(addr,scan_port)) {
					final String srv_name=getSmbHostName(addr);
					handler.post(new Runnable() {// UI thread
						@Override
						public void run() {
							synchronized(mScanRequestedAddrList) {
//								Log.v("","addr="+addr+", contained="+mScanRequestedAddrList.contains(addr));
								mScanRequestedAddrList.remove(addr);
								ScanAddressResultListItem li=new ScanAddressResultListItem();
								li.server_address=addr;
								li.server_name=srv_name;
								ipAddressList.add(li);
								Collections.sort(ipAddressList, new Comparator<ScanAddressResultListItem>(){
									@Override
									public int compare(ScanAddressResultListItem lhs,
											ScanAddressResultListItem rhs) {
										String lhs_o0="", lhs_o1="", lhs_o2="", lhs_o3="";
										String[] lhs_o=lhs.server_address.split("\\.");
//										Log.v("","lhs_addr="+lhs.server_address+", l="+lhs_o.length);
										if (lhs_o[0].length()==3) lhs_o0=lhs_o[0];
										else if (lhs_o[0].length()==2) lhs_o0="0"+lhs_o[0];
										else if (lhs_o[0].length()==1) lhs_o0="00"+lhs_o[0];
										if (lhs_o[1].length()==3) lhs_o1=lhs_o[1];
										else if (lhs_o[1].length()==2) lhs_o1="0"+lhs_o[1];
										else if (lhs_o[1].length()==1) lhs_o1="00"+lhs_o[1];
										if (lhs_o[2].length()==3) lhs_o2=lhs_o[1];
										else if (lhs_o[2].length()==2) lhs_o2="0"+lhs_o[2];
										else if (lhs_o[2].length()==1) lhs_o2="00"+lhs_o[2];
										if (lhs_o[3].length()==3) lhs_o3=lhs_o[3];
										else if (lhs_o[3].length()==2) lhs_o3="0"+lhs_o[3];
										else if (lhs_o[3].length()==1) lhs_o3="00"+lhs_o[3];

										String rhs_o0="", rhs_o1="", rhs_o2="", rhs_o3="";
										String[] rhs_o=rhs.server_address.split("\\.");
										if (rhs_o[0].length()==3) rhs_o0=lhs_o[0];
										else if (rhs_o[0].length()==2) rhs_o0="0"+rhs_o[0];
										else if (rhs_o[0].length()==1) rhs_o0="00"+rhs_o[0];
										if (rhs_o[1].length()==3) rhs_o1=rhs_o[1];
										else if (rhs_o[1].length()==2) rhs_o1="0"+rhs_o[1];
										else if (rhs_o[1].length()==1) rhs_o1="00"+rhs_o[1];
										if (rhs_o[2].length()==3) rhs_o2=rhs_o[1];
										else if (rhs_o[2].length()==2) rhs_o2="0"+rhs_o[2];
										else if (rhs_o[2].length()==1) rhs_o2="00"+rhs_o[2];
										if (rhs_o[3].length()==3) rhs_o3=rhs_o[3];
										else if (rhs_o[3].length()==2) rhs_o3="0"+rhs_o[3];
										else if (rhs_o[3].length()==1) rhs_o3="00"+rhs_o[3];

										String lhs_addr=lhs_o0+"."+lhs_o1+"."+lhs_o2+"."+lhs_o3;
										String rhs_addr=rhs_o0+"."+rhs_o1+"."+rhs_o2+"."+rhs_o3;
										
//										Log.v("","lhs="+lhs_addr+", rhs="+rhs_addr);
										
										return lhs_addr.compareTo(rhs_addr);
									}
								});
								adap.notifyDataSetChanged();
							}
							synchronized(mLockScanCompleteCount) {
								mScanCompleteCount++;
							}
						}
					});
				} else {
					synchronized(mScanRequestedAddrList) {
//						Log.v("","addr="+addr+", contained="+mScanRequestedAddrList.contains(addr));
						mScanRequestedAddrList.remove(addr);
					}
					synchronized(mLockScanCompleteCount) {
						mScanCompleteCount++;
					}
				}
				handler.post(new Runnable() {// UI thread
					@Override
					public void run() {
						synchronized(mLockScanCompleteCount) {
							lv_ipaddr.setSelection(lv_ipaddr.getCount());
							adap.notifyDataSetChanged();
							String p_txt=String.format(scan_prog, 
									(mScanCompleteCount*100)/mScanAddrCount);
							tvmsg.setText(p_txt);
						}
					}
				});
			}
       	});
       	th.start();
	};
	
	private boolean isIpAddrSmbHost(String address, String scan_port) {
		boolean smbhost=false;
//		boolean reachable=NetworkUtil.ping(address);
//		if (reachable) {
//		}
		if (scan_port.equals("")) {
			if (!NetworkUtil.isIpAddressAndPortConnected(address,139,3000)) {
				smbhost=NetworkUtil.isIpAddressAndPortConnected(address,445,3000);
			} else smbhost=true;
		} else {
			smbhost=NetworkUtil.isIpAddressAndPortConnected(address,
					Integer.parseInt(scan_port),3000);
		}
		util.addDebugLogMsg(2,"I","isIpAddrSmbHost Address="+address+
				", port="+scan_port+", smbhost="+smbhost);
		return smbhost;
	};


//	@SuppressWarnings("unused")
//	private boolean isNbtAddressActive(String address) {
//		boolean result=NetworkUtil.isNbtAddressActive(address);
//    	util.addDebugLogMsg(1,"I","isSmbHost Address="+address+", result="+result);
//		return result;
//	};
	
	private String getSmbHostName(String address) {
		String srv_name=NetworkUtil.getSmbHostNameFromAddress(address);
       	util.addDebugLogMsg(1,"I","getSmbHostName Address="+address+", name="+srv_name);
    	return srv_name;
 	};

	private boolean auditScanAddressRangeValue(Dialog dialog) {
		boolean result=false;
		final EditText baEt1 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o1);
		final EditText baEt2 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o2);
		final EditText baEt3 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o3);
		final EditText baEt4 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o4);
		final EditText eaEt4 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_end_address_o4);
		final TextView tvmsg = (TextView) dialog.findViewById(R.id.scan_remote_ntwk_msg);

		String ba1=baEt1.getText().toString();
		String ba2=baEt2.getText().toString();
		String ba3=baEt3.getText().toString();
		String ba4=baEt4.getText().toString();
		String ea4=eaEt4.getText().toString();
		
    	tvmsg.setText("");
		if (ba1.equals("")) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
			baEt1.requestFocus();
			return false;
		} else if (ba2.equals("")) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
			baEt2.requestFocus();
			return false;
		} else if (ba3.equals("")) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
			baEt3.requestFocus();
			return false;
		} else if (ba4.equals("")) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
			baEt4.requestFocus();
			return false;
		} else if (ea4.equals("")) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_end_notspecified));
			eaEt4.requestFocus();
			return false;
		}
		int iba1 = Integer.parseInt(ba1);
		if (iba1>255) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_addr_range_error));
			baEt1.requestFocus();
			return false;
		}
		int iba2 = Integer.parseInt(ba2);
		if (iba2>255) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_addr_range_error));
			baEt2.requestFocus();
			return false;
		}
		int iba3 = Integer.parseInt(ba3);
		if (iba3>255) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_addr_range_error));
			baEt3.requestFocus();
			return false;
		}
		int iba4 = Integer.parseInt(ba4);
		int iea4 = Integer.parseInt(ea4);
		if (iba4>0 && iba4<255) {
			if (iea4>0 && iea4<255) {
				if (iba4<=iea4) {
					result=true;
				} else {
					baEt4.requestFocus();
					tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_addr_gt_end_addr));
				}
			} else {
				eaEt4.requestFocus();
				tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_end_range_error));
			}
		} else {
			baEt4.requestFocus();
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_range_error));
		}

		if (iba1==192&&iba2==168) {
			//class c private
		} else {
			if (iba1==10) {
				//class a private
			} else {
				if (iba1==172 && (iba2>=16&&iba2<=31)) {
					//class b private
				} else {
					//not private
					result=false;
					tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_not_private));
				}
			}
		}
		
		return result;
	};
	
//	private void setSyncMaterOrTagetProfile(
//			boolean mp, String base_prof_name, final NotifyEvent p_ntfy) {
//		final ArrayList<String> rows = new ArrayList<String>();
//		String prof_type=getProfileType(base_prof_name,glblParms.profileAdapter);
//		ProfileListItem base_pli=getProfile(base_prof_name, glblParms.profileAdapter);
//		for (int i = 0; i < glblParms.profileAdapter.getCount(); i++) {
//			ProfileListItem item = glblParms.profileAdapter.getItem(i);
//			if (!mp) {
//				if (!item.getType().equals(SMBSYNC_PROF_TYPE_SYNC) && 
//					item.getActive().equals(SMBSYNC_PROF_ACTIVE)) {
//					if (prof_type.equals(SMBSYNC_PROF_TYPE_LOCAL)) {
//						if (item.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
//							if (!base_pli.getLocalMountPoint().equals(item.getLocalMountPoint()) || 
//									!base_pli.getDir().equals(item.getDir())) {
//								rows.add(item.getType()+" "+item.getName());
//							}
//						} else {
//							rows.add(item.getType()+" "+item.getName());
//						}
//					} else {
//						if (item.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
//							rows.add(item.getType()+" "+item.getName());
//						}
//					}
//				}
//			} else {
//				if (!item.getType().equals(SMBSYNC_PROF_TYPE_SYNC) && 
//						item.getActive().equals(SMBSYNC_PROF_ACTIVE)) {
//						rows.add(item.getType()+" "+item.getName());
//				}
//			}
//		}
//		if (rows.size()<1) rows.add(msgs_no_profile);
//    	//カスタムダイアログの生成
//        final Dialog dialog=new Dialog(mContext);
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//    	dialog.setContentView(R.layout.item_select_list_dlg);
//        ((TextView)dialog.findViewById(R.id.item_select_list_dlg_title))
//        	.setText(msgs_select_profile);
//        ((TextView)dialog.findViewById(R.id.item_select_list_dlg_subtitle))
//    	.setVisibility(TextView.GONE);
//        
//        CommonDialog.setDlgBoxSizeLimit(dialog, false);
//        
//        ListView lv = (ListView) dialog.findViewById(android.R.id.list);
//        lv.setAdapter(new AdapterSelectSyncProfileList(mContext, R.layout.sync_profile_list_item_view, rows));
//        lv.setScrollingCacheEnabled(false);
//        lv.setScrollbarFadingEnabled(false);
//        
//        lv.setOnItemClickListener(new OnItemClickListener(){
//        	public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
//        		if (rows.get(idx).startsWith("---")) return;
//	        	// リストアイテムを選択したときの処理
//                dialog.dismiss();
//				p_ntfy.notifyToListener(true, 
//						new Object[]{rows.get(idx).substring(2,rows.get(idx).length())});
//            }
//        });	 
//        //CANCELボタンの指定
//        final Button btn_cancel=(Button)dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
//        btn_cancel.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                dialog.dismiss();
//                p_ntfy.notifyToListener(false, null);
//            }
//        });
//		// Cancelリスナーの指定
//		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
//			@Override
//			public void onCancel(DialogInterface arg0) {
//				btn_cancel.performClick();
//			}
//		});
//        dialog.show();
//		
//	};

	private ArrayList<TreeFilelistItem>  createLocalFilelist(boolean dironly, 
			String url, String dir) {
		
		ArrayList<TreeFilelistItem> tfl = new ArrayList<TreeFilelistItem>(); ;
		String tdir,fp;
		
		if (dir.equals("")) fp=tdir="/";
		else {
			tdir=dir;
			fp=dir+"/";
		}
		File lf = new File(url+tdir);
		final File[]  ff = lf.listFiles();
		TreeFilelistItem tfi=null;
		if (ff!=null) {
			for (int i=0;i<ff.length;i++){
//				Log.v("","name="+ff[i].getName()+", d="+ff[i].isDirectory()+", r="+ff[i].canRead());
				if (ff[i].canRead()) {
					int dirct=0;
					if (ff[i].isDirectory()) {
						File tlf=new File(url+tdir+"/"+ff[i].getName());
						File[] lfl=tlf.listFiles();
						if (lfl!=null) {
							for (int j=0;j<lfl.length;j++) {
								if (dironly) {
									if (lfl[j].isDirectory()) dirct++;
								} else dirct++;
							}
						}
					}
					tfi=new TreeFilelistItem(ff[i].getName(),
							""+", ", ff[i].isDirectory(), 0,0,false,
							ff[i].canRead(),ff[i].canWrite(),
							ff[i].isHidden(),fp,0);
					tfi.setSubDirItemCount(dirct);

					if (dironly) {
						if (ff[i].isDirectory()) tfl.add(tfi);
					} else tfl.add(tfi);
				}
			}
			Collections.sort(tfl);
		}
		return tfl;
	};
	
	
	public void selectLocalDirDlg(final String url,final String dir,
			String p_dir,final NotifyEvent p_ntfy) {
		
    	//カスタムダイアログの生成
        final Dialog dialog=new Dialog(mContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
    	dialog.setContentView(R.layout.item_select_list_dlg);
    	
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.item_select_list_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.item_select_list_dlg_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.item_select_list_dlg_title);
		final TextView subtitle = (TextView) dialog.findViewById(R.id.item_select_list_dlg_subtitle);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);
//		subtitle.setTextColor(mGp.themeColorList.text_color_dialog_title);
		subtitle.setTextColor(mGp.themeColorList.text_color_primary);//.text_color_dialog_title);

        title.setText(msgs_select_local_dir);
        subtitle.setText(msgs_current_dir+url+dir);
        
	    final Button btn_ok=(Button)dialog.findViewById(R.id.item_select_list_dlg_ok_btn);

//        if (rows.size()<=2) 
//        	((TextView)dialog.findViewById(R.id.item_select_list_dlg_spacer))
//        	.setVisibility(TextView.VISIBLE);

        CommonDialog.setDlgBoxSizeLimit(dialog, true);
		
        ListView lv = (ListView) dialog.findViewById(android.R.id.list);
        final TreeFilelistAdapter tfa=new TreeFilelistAdapter(mContext,true,false);
        lv.setAdapter(tfa);
        ArrayList<TreeFilelistItem> tfl =createLocalFilelist(true,url,dir);
        if (tfl.size()<1) tfl.add(new TreeFilelistItem(msgs_dir_empty));
        tfa.setDataList(tfl);
        lv.setScrollingCacheEnabled(false);
        lv.setScrollbarFadingEnabled(false);

        if (p_dir.length()!=0)
        	for (int i=0;i<tfa.getDataItemCount();i++) {
        		if (tfa.getDataItem(i).getName().equals(p_dir)) 
        			lv.setSelection(i);
        	}
        
        NotifyEvent ntfy_expand_close=new NotifyEvent(mContext);
        ntfy_expand_close.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				int idx=(Integer)o[0];
	    		final int pos=tfa.getItem(idx);
	    		final TreeFilelistItem tfi=tfa.getDataItem(pos);
				if (tfi.getName().startsWith("---")) return;
				expandHideLocalDirTree(true,url, pos,tfi,tfa);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
        });
        tfa.setExpandCloseListener(ntfy_expand_close);
        lv.setOnItemClickListener(new OnItemClickListener(){
        	public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
	    		final int pos=tfa.getItem(idx);
	    		final TreeFilelistItem tfi=tfa.getDataItem(pos);
				if (tfi.getName().startsWith("---")) return;
//				tfa.setDataItemIsSelected(pos);
//				if (tfa.isDataItemIsSelected()) btn_ok.setEnabled(true);
//				else btn_ok.setEnabled(false);
				expandHideLocalDirTree(true,url, pos,tfi,tfa);
			}
        });
        
		lv.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					final int position, long arg3) {
				return true;
			}
		});

		NotifyEvent ctv_ntfy=new NotifyEvent(mContext);
		ctv_ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				if (o!=null) {
					int pos=(Integer)o[0];
					if (tfa.getDataItem(pos).isChecked()) btn_ok.setEnabled(true);
				}
			}
			@Override
			public void negativeResponse(Context c,Object[] o) {
				btn_ok.setEnabled(false);
				for (int i=0;i<tfa.getDataItemCount();i++) {
					if (tfa.getDataItem(i).isChecked()) {
						btn_ok.setEnabled(true);
						break;
					}
				}
			}
		});
		tfa.setCbCheckListener(ctv_ntfy);

	    //OKボタンの指定
		btn_ok.setEnabled(false);
	    btn_ok.setVisibility(Button.VISIBLE);
	    btn_ok.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	            String sel="";
	            for (int i=0;i<tfa.getCount();i++) {
	            	if (tfa.getDataItem(i).isChecked()  && 
	            			!tfa.getDataItem(i).getName().equals(msgs_dir_empty)) {
	            		if (tfa.getDataItem(i).getPath().length()==1) 
		            		sel=tfa.getDataItem(i).getName();
	            		else sel=tfa.getDataItem(i).getPath()
	            				.substring(1,tfa.getDataItem(i).getPath().length())+
	            				tfa.getDataItem(i).getName();
	            		break;
	            	}
	            }
	            if (sel.equals("")) {
	            	
	            }
	            dialog.dismiss();
	            p_ntfy.notifyToListener(true,new Object[]{sel} );
	        }
	    });

        //CANCELボタンの指定
        final Button btn_cancel=(Button)dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                p_ntfy.notifyToListener(false, null);
            }
        });
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
//        dialog.setOnKeyListener(new DialogOnKeyListener(context));
//        dialog.setCancelable(false);
        dialog.show();
		
		return ;
	};

	private void createRemoteFileList(String remurl,String remdir, 
			final NotifyEvent p_event, boolean readSubDirCnt) {
		final ArrayList<TreeFilelistItem> remoteFileList =
								new ArrayList<TreeFilelistItem>();
		final ThreadCtrl tc=new ThreadCtrl();
		tc.setEnabled();
		tc.setThreadResultSuccess();
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.progress_spin_dlg);
		
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.progress_spin_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.progress_spin_dlg_title_view);
		final TextView title = (TextView) dialog.findViewById(R.id.progress_spin_dlg_title);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		title.setTextColor(mGp.themeColorList.text_color_dialog_title);

		title.setText(R.string.msgs_progress_spin_dlg_filelist_getting);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.progress_spin_dlg_btn_cancel);
		btn_cancel.setText(R.string.msgs_progress_spin_dlg_filelist_cancel);
		
//		(dialog.context.findViewById(R.id.progress_spin_dlg)).setVisibility(TextView.GONE);
//		(dialog.context.findViewById(R.id.progress_spin_dlg)).setEnabled(false);
		
		CommonDialog.setDlgBoxSizeCompact(dialog);
		// CANCELボタンの指定
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				tc.setDisabled();//disableAsyncTask();
				btn_cancel.setText(mContext.getString(R.string.msgs_progress_dlg_canceling));
				btn_cancel.setEnabled(false);
				util.addDebugLogMsg(1,"W","Sharelist is cancelled.");
			}
		});
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
//		dialog.show(); showDelayedProgDlgで表示

		final Handler hndl=new Handler();
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				hndl.post(new Runnable(){
					@Override
					public void run() {
						dialog.dismiss();
						String err;
						util.addDebugLogMsg(1,"I","FileListThread result="+tc.getThreadResult()+","+
								"msg="+tc.getThreadMessage()+", enable="+
									tc.isEnabled());
						if (tc.isThreadResultSuccess()) {
							p_event.notifyToListener(true, new Object[]{remoteFileList});
						} else {
							if (tc.isThreadResultCancelled()) err=msgs_filelist_cancel;
							else err=msgs_filelist_error+"\n"+tc.getThreadMessage();
							p_event.notifyToListener(false, new Object[]{err});
						}
					}
				});
			}
			@Override
			public void negativeResponse(Context c,Object[] o) {}
		});
		
		Thread tf = new Thread(new ReadRemoteFilelist(mContext, tc, remurl, remdir, remoteFileList,
				smbUser,smbPass, ntfy, true, readSubDirCnt, mGp));
		tf.start();
		
//		showDelayedProgDlg(200,dialog, tc);
		dialog.show();
	};
	
//	private void showDelayedProgDlg(final int wt, final Dialog dialog, final ThreadCtrl tc) {
//    	final Handler handler=new Handler();
//
//       	new Thread(new Runnable() {
//			@Override
//			public void run() {//Non UI thread
//				try { 
//					Thread.sleep(wt);
//				} catch (InterruptedException e) 
//					{e.printStackTrace();}
//				
//				handler.post(new Runnable() {
//					@Override
//					public void run() {// UI thread
//						if (tc.isEnabled()) if (dialog!=null) dialog.show();
//					}
//				});
//			}
//		})
//       	.start();
//	}
	
	public void selectRemoteShareDlg(final String remurl, String remdir,
			final NotifyEvent p_ntfy) { 
		
		NotifyEvent ntfy=new NotifyEvent(mContext);
		// set thread response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				final ArrayList<String> rows = new ArrayList<String>();
				@SuppressWarnings("unchecked")
				ArrayList<TreeFilelistItem> rfl=(ArrayList<TreeFilelistItem>)o[0];
				
				for (int i=0;i<rfl.size();i++){
					if (rfl.get(i).isDir() && rfl.get(i).canRead() && 
							!rfl.get(i).getName().endsWith("$"))
//							!rfl.get(i).getName().startsWith("IPC$"))
						rows.add(rfl.get(i).getName().replaceAll("/", ""));
				}
				boolean wk_list_empty=false; 
				if (rows.size()<1) {
					wk_list_empty=true;
					rows.add(msgs_dir_empty);
				}
				final boolean list_empty=wk_list_empty;
				Collections.sort(rows, String.CASE_INSENSITIVE_ORDER);
				//カスタムダイアログの生成
				final Dialog dialog=new Dialog(mContext);
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				dialog.setCanceledOnTouchOutside(false);
				dialog.setContentView(R.layout.item_select_list_dlg);
				
				LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.item_select_list_dlg_view);
				ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

				final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.item_select_list_dlg_title_view);
				final TextView title = (TextView) dialog.findViewById(R.id.item_select_list_dlg_title);
				final TextView subtitle = (TextView) dialog.findViewById(R.id.item_select_list_dlg_subtitle);
				title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
				title.setTextColor(mGp.themeColorList.text_color_dialog_title);
				subtitle.setTextColor(mGp.themeColorList.text_color_dialog_title);

				title.setText(msgs_select_remote_share);
				subtitle.setVisibility(TextView.GONE);
				
//		        if (rows.size()<=2) 
//		        	((TextView)dialog.findViewById(R.id.item_select_list_dlg_spacer))
//		        	.setVisibility(TextView.VISIBLE);
				final Button btn_cancel=(Button)dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
				final Button btn_ok=(Button)dialog.findViewById(R.id.item_select_list_dlg_ok_btn);
				btn_ok.setEnabled(false);
				
				CommonDialog.setDlgBoxSizeLimit(dialog, false);
				
				final ListView lv = (ListView) dialog.findViewById(android.R.id.list);
				if (!list_empty) {
					lv.setAdapter(new ArrayAdapter<String>(mContext,
							R.layout.custom_simple_list_item_checked,rows));
					//  android.R.layout.simple_list_item_checked,rows));
					lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
				} else {
					lv.setAdapter(new ArrayAdapter<String>(mContext,
							R.layout.simple_list_item_1o, rows));
				}
				lv.setScrollingCacheEnabled(false);
				lv.setScrollbarFadingEnabled(false);
				
				lv.setOnItemClickListener(new OnItemClickListener(){
					public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
						if (rows.get(idx).startsWith("---")) return;
						if (!list_empty) btn_ok.setEnabled(true);
					}
				});	 
				//CANCELボタンの指定
				btn_cancel.setOnClickListener(new View.OnClickListener() {
				    public void onClick(View v) {
				        dialog.dismiss();
			    		p_ntfy.notifyToListener(false, null);
				    }
				});
				//OKボタンの指定
				btn_ok.setVisibility(Button.VISIBLE);
				btn_ok.setOnClickListener(new View.OnClickListener() {
				    public void onClick(View v) {
				        dialog.dismiss();
		                SparseBooleanArray checked = lv.getCheckedItemPositions();
		                for(int i=0; i<=rows.size();i++){
		                    if(checked.get(i) == true){
					    		p_ntfy.notifyToListener(true, new Object[]{rows.get(i)});
					    		break;
		                    }
		                }
				    }
				});
				// Cancelリスナーの指定
				dialog.setOnCancelListener(new Dialog.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
						btn_cancel.performClick();
					}
				});
//				dialog.setOnKeyListener(new DialogOnKeyListener(context));
//				dialog.setCancelable(false);
				dialog.show();
			}

			@Override
			public void negativeResponse(Context c,Object[] o) {
				p_ntfy.notifyToListener(false, o);
			}
		});
		createRemoteFileList(remurl,remdir, ntfy,false);

	};
        
	public void setRemoteDir(final String remurl, 
			final String curdir, final String p_dir, final NotifyEvent p_ntfy) { 
		final ArrayList<TreeFilelistItem> rows = new ArrayList<TreeFilelistItem>();
		
		NotifyEvent ntfy=new NotifyEvent(mContext);
		// set thread response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				@SuppressWarnings("unchecked")
				ArrayList<TreeFilelistItem> rfl = (ArrayList<TreeFilelistItem>)o[0];
				for (int i=0;i<rfl.size();i++){
					if (rfl.get(i).isDir() && rfl.get(i).canRead()) rows.add(rfl.get(i));
				}
				Collections.sort(rows);
				if (rows.size()<1) rows.add(new TreeFilelistItem(msgs_dir_empty));
				//カスタムダイアログの生成
			    final Dialog dialog=new Dialog(mContext);
			    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			    dialog.setCanceledOnTouchOutside(false);
				dialog.setContentView(R.layout.item_select_list_dlg);
				
				LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.item_select_list_dlg_view);
				ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

				final LinearLayout title_view = (LinearLayout) dialog.findViewById(R.id.item_select_list_dlg_title_view);
				final TextView title = (TextView) dialog.findViewById(R.id.item_select_list_dlg_title);
				final TextView subtitle = (TextView) dialog.findViewById(R.id.item_select_list_dlg_subtitle);
				title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
				title.setTextColor(mGp.themeColorList.text_color_dialog_title);
//				subtitle.setTextColor(mGp.themeColorList.text_color_dialog_title);
				subtitle.setTextColor(mGp.themeColorList.text_color_primary);//.text_color_dialog_title);

			    title.setText(msgs_select_remote_dir);
			    subtitle.setText(msgs_current_dir+"/"+remurl);
//			    if (rows.size()<1) {
//			    	TextView dlg_msg=(TextView)dialog.findViewById(R.id.item_select_list_dlg_msg);
//			    	dlg_msg.setText(msgs_dir_empty);
//			    	dlg_msg.setVisibility(TextView.VISIBLE);
//			    }
			    final Button btn_ok=(Button)dialog.findViewById(R.id.item_select_list_dlg_ok_btn);
//		        if (rows.size()<=2) 
//		        	((TextView)dialog.findViewById(R.id.item_select_list_dlg_spacer))
//		        	.setVisibility(TextView.VISIBLE);
			    
			    CommonDialog.setDlgBoxSizeLimit(dialog, true);
				
			    final ListView lv = (ListView) dialog.findViewById(android.R.id.list);
			    final TreeFilelistAdapter tfa=new TreeFilelistAdapter(mContext,true,false);
//				tfa.setNotifyOnChange(true);
				tfa.setDataList(rows);
			    lv.setAdapter(tfa);
			    lv.setScrollingCacheEnabled(false);
			    lv.setScrollbarFadingEnabled(false);
			    
		        if (p_dir.length()!=0)
		        	for (int i=0;i<tfa.getDataItemCount();i++) {
		        		if (tfa.getDataItem(i).getName().equals(p_dir)) 
		        			lv.setSelection(i);
		        	}
		        NotifyEvent ntfy_expand_close=new NotifyEvent(mContext);
		        ntfy_expand_close.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						int idx=(Integer)o[0];
			    		final int pos=tfa.getItem(idx);
			    		final TreeFilelistItem tfi=tfa.getDataItem(pos);
						if (tfi.getName().startsWith("---")) return;
						expandHideRemoteDirTree(remurl, pos,tfi,tfa);
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
		        });
		        tfa.setExpandCloseListener(ntfy_expand_close);
		        lv.setOnItemClickListener(new OnItemClickListener(){
		        	public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
			    		final int pos=tfa.getItem(idx);
			    		final TreeFilelistItem tfi=tfa.getDataItem(pos);
						if (tfi.getName().startsWith("---")) return;
//						tfa.setDataItemIsSelected(pos);
//						if (tfa.isDataItemIsSelected()) btn_ok.setEnabled(true);
//						else btn_ok.setEnabled(false);
						expandHideRemoteDirTree(remurl, pos,tfi,tfa);
					}
		        });
		        
				lv.setOnItemLongClickListener(new OnItemLongClickListener(){
					@Override
					public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
							final int position, long arg3) {
						return true;
					}
				});

				NotifyEvent ctv_ntfy=new NotifyEvent(mContext);
				// set file list thread response listener 
				ctv_ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context c,Object[] o) {
						if (o!=null) {
							int pos=(Integer)o[0];
							if (tfa.getDataItem(pos).isChecked()) btn_ok.setEnabled(true);
						}
					}
					@Override
					public void negativeResponse(Context c,Object[] o) {
						btn_ok.setEnabled(false);
						for (int i=0;i<tfa.getDataItemCount();i++) {
							if (tfa.getDataItem(i).isChecked()) {
								btn_ok.setEnabled(true);
								break;
							}
						}
					}
				});
				tfa.setCbCheckListener(ctv_ntfy);

			    //OKボタンの指定
				btn_ok.setEnabled(false);
			    btn_ok.setVisibility(Button.VISIBLE);
			    btn_ok.setOnClickListener(new View.OnClickListener() {
			        public void onClick(View v) {
			            String sel="";
			            for (int i=0;i<tfa.getCount();i++) {
			            	if (tfa.getDataItem(i).isChecked() && 
			            			!tfa.getDataItem(i).getName().equals(msgs_dir_empty)) {
			            		if (tfa.getDataItem(i).getPath().length()==1) 
				            		sel=tfa.getDataItem(i).getName();
			            		else sel=tfa.getDataItem(i).getPath()
			            				.substring(1,tfa.getDataItem(i).getPath().length())+
			            				tfa.getDataItem(i).getName();
			            		break;
			            	}
			            }
			            dialog.dismiss();
			            p_ntfy.notifyToListener(true,new Object[]{sel} );
			        }
			    });
			    //CANCELボタンの指定
			    final Button btn_cancel=(Button)dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
			    btn_cancel.setOnClickListener(new View.OnClickListener() {
			        public void onClick(View v) {
			            dialog.dismiss();
			            p_ntfy.notifyToListener(false, null);
			        }
			    });
				// Cancelリスナーの指定
				dialog.setOnCancelListener(new Dialog.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
						btn_cancel.performClick();
					}
				});

//			    dialog.setOnKeyListener(new DialogOnKeyListener(context));
//			    dialog.setCancelable(false);
			    dialog.show();
			}

			@Override
			public void negativeResponse(Context c,Object[] o) {
				p_ntfy.notifyToListener(false, o);
			}
		});
		createRemoteFileList(remurl,curdir,ntfy,true);
        return ;
	};

	private void expandHideRemoteDirTree(String remurl, final int pos, 
			final TreeFilelistItem tfi, final TreeFilelistAdapter tfa) {
		if (tfi.getSubDirItemCount()==0) return;
		if(tfi.isChildListExpanded()) {
			tfa.hideChildItem(tfi,pos);
		} else {
			if (tfi.isSubDirLoaded()) 
				tfa.reshowChildItem(tfi,pos);
			else {
				if (tfi.isSubDirLoaded()) 
					tfa.reshowChildItem(tfi,pos);
				else {
					NotifyEvent ne=new NotifyEvent(mContext);
					ne.setListener(new NotifyEventListener() {
						@SuppressWarnings("unchecked")
						@Override
						public void positiveResponse(Context c,Object[] o) {
							tfa.addChildItem(tfi,(ArrayList<TreeFilelistItem>)o[0],pos);
						}
						@Override
						public void negativeResponse(Context c,Object[] o) {}
					});
					createRemoteFileList(remurl,tfi.getPath()+tfi.getName()+"/",ne,true);
				}
			}
		}
	};
	private void expandHideLocalDirTree(boolean dironly,String lclurl, final int pos, 
			final TreeFilelistItem tfi, final TreeFilelistAdapter tfa) {
		if (tfi.getSubDirItemCount()==0) return;
		if(tfi.isChildListExpanded()) {
			tfa.hideChildItem(tfi,pos);
		} else {
			if (tfi.isSubDirLoaded()) 
				tfa.reshowChildItem(tfi,pos);
			else {
				if (tfi.isSubDirLoaded()) tfa.reshowChildItem(tfi,pos);
				else {
					ArrayList<TreeFilelistItem> ntfl =
							createLocalFilelist(dironly,lclurl,tfi.getPath()+tfi.getName());
					tfa.addChildItem(tfi,ntfl,pos);
				}
			}
		}
	};
	
	public AdapterProfileList createProfileList(boolean sdcard, String fp) {
		AdapterProfileList pfl=null;
		
		ArrayList<ProfileListItem> sync = new ArrayList<ProfileListItem>();
		ArrayList<ProfileListItem> rem = new ArrayList<ProfileListItem>();
		ArrayList<ProfileListItem> lcl = new ArrayList<ProfileListItem>();
		
		importedSettingParmList.clear();

		if (sdcard) {
			File sf = new File(fp);
			if (sf.exists()) {
				CipherParms cp=null;
				boolean prof_encrypted=isProfileWasEncrypted(fp);
				if (prof_encrypted) {
					cp=EncryptUtil.initDecryptEnv(
							mGp.profileKeyPrefix+mGp.profilePassword);
				}
				try {
					BufferedReader br;
					br = new BufferedReader(new FileReader(fp),8192);
					String pl;
					while ((pl = br.readLine()) != null) {
						if (pl.startsWith(SMBSYNC_PROF_VER1) || pl.startsWith(SMBSYNC_PROF_VER2)) {
							addProfileList(pl, sync, rem, lcl,importedSettingParmList);
						} else if (pl.startsWith(SMBSYNC_PROF_VER3) || 
								pl.startsWith(SMBSYNC_PROF_VER4) ||
								pl.startsWith(SMBSYNC_PROF_VER5) ||
								pl.startsWith(SMBSYNC_PROF_VER6) ||
								pl.startsWith(SMBSYNC_PROF_VER7) ||
								pl.startsWith(SMBSYNC_PROF_VER8)
								) {
							String prof_pre="";
							if (pl.startsWith(SMBSYNC_PROF_VER3)) prof_pre=SMBSYNC_PROF_VER3;
							else if (pl.startsWith(SMBSYNC_PROF_VER4)) prof_pre=SMBSYNC_PROF_VER4;
							else if (pl.startsWith(SMBSYNC_PROF_VER5)) prof_pre=SMBSYNC_PROF_VER5;
							else if (pl.startsWith(SMBSYNC_PROF_VER6)) prof_pre=SMBSYNC_PROF_VER6;
							else if (pl.startsWith(SMBSYNC_PROF_VER7)) prof_pre=SMBSYNC_PROF_VER7;
							else if (pl.startsWith(SMBSYNC_PROF_VER8)) prof_pre=SMBSYNC_PROF_VER8;
							if (!pl.startsWith(prof_pre+SMBSYNC_PROF_ENC) &&
									!pl.startsWith(prof_pre+SMBSYNC_PROF_DEC)) {
								if (prof_encrypted) {
									String enc_str=pl.replace(prof_pre, "");
									byte[] enc_array=Base64Compat.decode(enc_str, Base64Compat.NO_WRAP);
									String dec_str=EncryptUtil.decrypt(enc_array, cp);
									addProfileList(prof_pre+dec_str, sync, rem, lcl,importedSettingParmList);
								} else {
									addProfileList(pl, sync, rem, lcl,importedSettingParmList);
								}
							}
						}
					}
					br.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					util.addLogMsg("E",String.format(msgs_create_profile_error,fp));
					util.addLogMsg("E",e.toString());
					return null;
				} catch (IOException e) {
					e.printStackTrace();
					util.addLogMsg("E",String.format(msgs_create_profile_error,fp));
					util.addLogMsg("E",e.toString());
					return null;
				}
			} else {
				util.addLogMsg("E",String.format(msgs_create_profile_not_found,fp));
				return null;
			}

		} else {
			BufferedReader br;
			String pf = SMBSYNC_PROFILE_FILE_NAME_V0; 
			try {
				File lf1= new File(mGp.internalRootDirectory+"/"+
						SMBSYNC_PROFILE_FILE_NAME_V1);
				File lf2= new File(mGp.internalRootDirectory+"/"+
						SMBSYNC_PROFILE_FILE_NAME_V2);
				File lf3= new File(mGp.internalRootDirectory+"/"+
						SMBSYNC_PROFILE_FILE_NAME_V3);
				File lf4= new File(mGp.internalRootDirectory+"/"+
						SMBSYNC_PROFILE_FILE_NAME_V4);
				File lf5= new File(mGp.internalRootDirectory+"/"+
						SMBSYNC_PROFILE_FILE_NAME_V5);
				File lf6= new File(mGp.internalRootDirectory+"/"+
						SMBSYNC_PROFILE_FILE_NAME_V6);
				File lf7= new File(mGp.internalRootDirectory+"/"+
						SMBSYNC_PROFILE_FILE_NAME_V7);
				File lf8= new File(mGp.internalRootDirectory+"/"+
						SMBSYNC_PROFILE_FILE_NAME_V8);
				if (lf8.exists()) pf=SMBSYNC_PROFILE_FILE_NAME_V8;
				else if (lf7.exists()) pf=SMBSYNC_PROFILE_FILE_NAME_V7;
				else if (lf6.exists()) pf=SMBSYNC_PROFILE_FILE_NAME_V6;
				else if (lf5.exists()) pf=SMBSYNC_PROFILE_FILE_NAME_V5;
				else if (lf4.exists()) pf=SMBSYNC_PROFILE_FILE_NAME_V4;
				else if (lf3.exists()) pf=SMBSYNC_PROFILE_FILE_NAME_V3;
				else if (lf2.exists()) pf=SMBSYNC_PROFILE_FILE_NAME_V2; 
				else if (lf1.exists()) pf=SMBSYNC_PROFILE_FILE_NAME_V1;
				else pf=SMBSYNC_PROFILE_FILE_NAME_V0;
				
				File lf= new File(mGp.internalRootDirectory+"/"+pf);
				
				if (lf.exists()) {
					br = new BufferedReader(
							new FileReader(mGp.internalRootDirectory+"/"+pf),8192); 
	//				InputStream in = context.openFileInput(SMBSYNC_PROFILE_FILE_NAME);
	//				BufferedReader br = new BufferedReader(new InputStreamReader(
	//						in, "UTF-8"));
					String pl;
					while ((pl = br.readLine()) != null) {
						addProfileList(pl, sync, rem, lcl, importedSettingParmList);
					}
					br.close();
				} else {
					util.addDebugLogMsg(1, "W", 
							"profile not found, empty profile list created. fn="+
									mGp.internalRootDirectory+"/"+pf);
				}
			} catch (IOException e) {
				e.printStackTrace();
				util.addLogMsg("E",String.format(msgs_create_profile_error,pf));
				util.addLogMsg("E",e.toString());
				return null;
			}
		}

		Collections.sort(sync);
		Collections.sort(rem);
		Collections.sort(lcl);
		sync.addAll(rem);
		sync.addAll(lcl); 

		for (int i=0;i<sync.size();i++) {
			ProfileListItem item = sync.get(i);
			if (item.getMasterType().equals("")) {
				item.setMasterType(getProfileType(item.getMasterName(), sync));
				item.setTargetType(getProfileType(item.getTargetName(), sync));
//				pfl.replace(item, i);
			}
		}
		pfl = new AdapterProfileList(mContext, R.layout.profile_list_item_view, sync);

		if (pfl.getCount() == 0) {
			if (BUILD_FOR_AMAZON) {
				//アマゾン用はサンプルプロファイルを作成しない
				pfl.add(new ProfileListItem("","",
						mContext.getString(R.string.msgs_no_profile_entry),
						"","",null,"",0,0,false));
			} else {
				if (mGp.sampleProfileCreateRequired) {
					createSampleProfile(pfl);
					saveProfileToFile(mGp, mContext, util, false,"","",pfl,false);
					mGp.sampleProfileCreateRequired=false;
				} else {
					pfl.add(new ProfileListItem("","",
							mContext.getString(R.string.msgs_no_profile_entry),
							"","",null,"",0,0,false));
				}
			}
		}
		return pfl;
	};

	private void createSampleProfile(AdapterProfileList pfl) {
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_SYNC,"S-DOWNLOAD-MY-PICTURE", SMBSYNC_PROF_ACTIVE,
				SMBSYNC_SYNC_TYPE_MIRROR,"R","R-SAMP-DOWNLOAD",
				"L","L-SAMP-DOWNLOAD",new ArrayList<String>(), 
				new ArrayList<String>(), true, true,false,false,"0",false,true,true,true,false,"",0,0,"",0,false));
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_LOCAL,"L-SAMP-DOWNLOAD", SMBSYNC_PROF_ACTIVE, 
				mGp.externalRootDirectory,"Pictures", "",0,0,false));
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_REMOTE,"R-SAMP-DOWNLOAD", SMBSYNC_PROF_ACTIVE, 
				"TESTUSER","PSWD","192.168.0.2","","","SHARE", "Android/Pictures",
				"",0,0,
				false));
		ArrayList<String> ff1=new ArrayList<String>();
		ArrayList<String> df1=new ArrayList<String>();
		ArrayList<String> ff2=new ArrayList<String>();
		ArrayList<String> df2=new ArrayList<String>();
		df1.add("E.thumbnails");
		ff1.add("I*.jpg");
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_SYNC,"S-BACKUP-MY-PICTURE", SMBSYNC_PROF_ACTIVE,
				SMBSYNC_SYNC_TYPE_MIRROR,"L","L-SAMP-UPLOAD",
				"R","R-SAMP-UPLOAD",ff1,df1, true, true,false,false,"0",false,true,true,true,false,
				"",0,0,"",0,false));
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_LOCAL,"L-SAMP-UPLOAD", SMBSYNC_PROF_ACTIVE, 
				mGp.externalRootDirectory,"DCIM", 
				"",0,0,false));
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_REMOTE,"R-SAMP-UPLOAD", SMBSYNC_PROF_ACTIVE, 
				"TESTUSER","PSWD","192.168.0.2","","","SHARE", "Android/DCIM", 
				"",0,0,
				false));
		
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_SYNC,"S-BACKUP-TO-USB-MEMORY", SMBSYNC_PROF_ACTIVE,
				SMBSYNC_SYNC_TYPE_MIRROR,"L","L-SAMP-LOCAL",
				"L","L-SAMP-USBDISK",ff2,df2, true, true,false,false,"0",false,true,true,true,false,
				"",0,0,"",0,false));
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_LOCAL,"L-SAMP-LOCAL", SMBSYNC_PROF_ACTIVE, 
				mGp.externalRootDirectory,"DCIM", 
				"",0,0,false));
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_LOCAL,"L-SAMP-USBDISK", SMBSYNC_PROF_ACTIVE, 
				"/mnt/usbdisk","usb", 
				"",0,0,false));
		pfl.sort();
	};
	
	private void addProfileList(String pl, ArrayList<ProfileListItem> sync,
			ArrayList<ProfileListItem> rem, ArrayList<ProfileListItem> lcl,
			ArrayList<PreferenceParmListIItem> ispl) {
		String profVer="";
		if (pl.length()>7) profVer=pl.substring(0, 6);
		if (profVer.equals(SMBSYNC_PROF_VER1)) {
			if (pl.length()>10){
				addProfileListVer1(pl.substring(7,pl.length()),sync,rem,lcl);
				addImportSettingsParm(pl,ispl);
			}
		} else if (profVer.equals(SMBSYNC_PROF_VER2)) {
				if (pl.length()>10){
					addProfileListVer2(pl.substring(7,pl.length()),sync,rem,lcl);
					addImportSettingsParm(pl,ispl);
				}
		} else if (profVer.equals(SMBSYNC_PROF_VER3)) {
			if (pl.length()>10){
				addProfileListVer3(pl.substring(6,pl.length()),sync,rem,lcl);
				addImportSettingsParm(pl,ispl);
			}
		} else if (profVer.equals(SMBSYNC_PROF_VER4)) {
			if (pl.length()>10){
				addProfileListVer4(pl.substring(6,pl.length()),sync,rem,lcl);
				addImportSettingsParm(pl,ispl);
			}
		} else if (profVer.equals(SMBSYNC_PROF_VER5)) {
			if (pl.length()>10){
				addProfileListVer5(pl.substring(6,pl.length()),sync,rem,lcl);
				addImportSettingsParm(pl,ispl);
			}
		} else if (profVer.equals(SMBSYNC_PROF_VER6)) {
			if (pl.length()>10){
				addProfileListVer6(pl.substring(6,pl.length()),sync,rem,lcl);
				addImportSettingsParm(pl,ispl);
			}
		} else if (profVer.equals(SMBSYNC_PROF_VER7)) {
			if (pl.length()>10){
				addProfileListVer7(pl.substring(6,pl.length()),sync,rem,lcl);
				addImportSettingsParm(pl,ispl);
			}
		} else if (profVer.equals(SMBSYNC_PROF_VER8)) {
			if (pl.length()>10){
				addProfileListVer8(pl.substring(6,pl.length()),sync,rem,lcl);
				addImportSettingsParm(pl,ispl);
			}
		} else addProfileListVer0(pl, sync, rem, lcl);
	};
	
	private void addProfileListVer0(String pl, ArrayList<ProfileListItem> sync,
			ArrayList<ProfileListItem> rem, ArrayList<ProfileListItem> lcl) {

		String prof_group=SMBSYNC_PROF_GROUP_DEFAULT;
		
		String[] tmp_pl=pl.split(",");// {"type","name","active",options...};
		String[] parm= new String[30];
		for (int i=0;i<30;i++) parm[i]="";
		for (int i=0;i<tmp_pl.length;i++) {
			if (tmp_pl[i]==null) parm[i]="";
			else {
				if (tmp_pl[i]==null) parm[i]="";
				else parm[i]=tmp_pl[i];
			}
		}
		if (parm[0].equals(SMBSYNC_PROF_TYPE_REMOTE)) {//Remote
			rem.add(createRemoteProfilelistItem(mGp,
					prof_group,// group
					parm[1],//Name
					parm[2],//Active
					parm[7],//directory
					parm[5],//user
					parm[6],//pass
					parm[4],//share
					parm[3],//address
					"",		//hostname
					"",//port
					"",0,0,
					false));
		} else {
			if (parm[0].equals(SMBSYNC_PROF_TYPE_LOCAL)) {//Local
				lcl.add(createLocalProfilelistItem(mGp,
						prof_group,// group
						parm[1],//Name
						parm[2],//Active
						parm[3],//Directory
						mGp.externalRootDirectory,
						"",0,0,
						false));
			} else if (parm[0].equals(SMBSYNC_PROF_TYPE_SYNC)) {//Sync
				ArrayList<String> ff=new ArrayList<String>();
				ArrayList<String> df=new ArrayList<String>();
				if (parm[6].length()!=0) ff.add("IF"+parm[6]);
				if (parm[7].length()!=0) ff.add("IF"+parm[7]);
				if (parm[8].length()!=0) ff.add("IF"+parm[8]);
				sync.add(createSyncProfilelistItem(mGp,
						prof_group,// group
						parm[1],//Name
						parm[ 2],//Active
						parm[ 5],//Sync type
						"",//Master type
						parm[ 3],//Master name
						"",//Target type
						parm[ 4],//Target name
						ff,//File Filter
						df,//Dir Filter
						true,
						false,
						true,
						false,
						"0",false,true,true,true,false,
						"",0,0,
						"",0,
						false));
			}
		}
	};

	private void addProfileListVer1(String pl, ArrayList<ProfileListItem> sync,
			ArrayList<ProfileListItem> rem, ArrayList<ProfileListItem> lcl) {
		if (pl.startsWith(SMBSYNC_PROF_VER1+","+"SETTINGS")) return; //ignore settings entry
		//Extract ArrayList<String> field
		String list1="",list2="", npl="";
		if (pl.indexOf("[")>=0) {
			// found first List
			list1=pl.substring(pl.indexOf("[")+1, pl.indexOf("]"));
			npl=pl.replace("["+list1+"]\t", "");
			if (npl.indexOf("[")>=0) {
				// found second List
				list2=npl.substring(npl.indexOf("[")+1, npl.indexOf("]"));
				npl=npl.replace("["+list2+"]\t", "");
			}
		} else npl=pl;
//		String prof_group = npl.substring(0,11).trim();
//		String tmp_ps=npl.substring(12,npl.length());

		String[] tmp_pl=npl.split("\t");// {"type","name","active",options...};
		String[] parm= new String[30];
		for (int i=0;i<30;i++) parm[i]="";
		for (int i=0;i<tmp_pl.length;i++) {
			if (tmp_pl[i]==null) parm[i]="";
			else {
				if (tmp_pl[i]==null) parm[i]="";
				else parm[i]=convertToSpecChar(tmp_pl[i]);
			}
		}
		if (parm[1].equals(SMBSYNC_PROF_TYPE_REMOTE)) {//Remote
			rem.add(createRemoteProfilelistItem(mGp,
					parm[0],//group
					parm[2],//Name
					parm[3],//Active
					parm[8],//directory
					parm[4],//user
					parm[5],//pass
					parm[7],//share
					parm[6],//address
					"",		//hostname
					"",//port
					"",0,0,
					false));

		} else {
			if (parm[1].equals(SMBSYNC_PROF_TYPE_LOCAL)) {//Local
				if (parm[5].equals("")) parm[5]=mGp.externalRootDirectory;
				lcl.add(createLocalProfilelistItem(mGp,
						parm[0],//group
						parm[2],//Name
						parm[3],//Active
						parm[4],//Directory
						parm[5],//Local mount point
						"",0,0,
						false));
			} else if (parm[1].equals(SMBSYNC_PROF_TYPE_SYNC)) {//Sync
				ArrayList<String> ff=new ArrayList<String>();
				ArrayList<String> df=new ArrayList<String>();
				if (list1.length()!=0) {
					String[] fp=list1.split("\t");
					for (int i=0;i<fp.length;i++) ff.add(convertToSpecChar(fp[i]));					
				} else ff.clear();
				if (list2.length()!=0) {
					String[] dp=list2.split("\t");
					for (int i=0;i<dp.length;i++) df.add(convertToSpecChar(dp[i]));
				} else df.clear();
				boolean mpd=true, conf=false, ujlm=false;
				if (parm[9].equals("0")) mpd=false;
				if (parm[10].equals("1")) conf=true;
				if (parm[11].equals("1")) ujlm=true;
				sync.add(createSyncProfilelistItem(mGp,
						parm[0],//group
						parm[ 2],//Name
						parm[ 3],//Active
						parm[ 4],//Sync type
						parm[ 5],//Master type
						parm[ 6],//Master name
						parm[ 7],//Target type
						parm[ 8],//Target name
						ff,//File Filter
						df,//Dir Filter
						mpd,
						conf,
						ujlm,
						false,
						"0",false,true,true,true,false,
						"",0,0,
						"",0,
						false));
			}
		}
	};

	private void addProfileListVer2(String pl, ArrayList<ProfileListItem> sync,
			ArrayList<ProfileListItem> rem, ArrayList<ProfileListItem> lcl) {
		if (pl.startsWith(SMBSYNC_PROF_VER2+","+"SETTINGS")) return; //ignore settings entry
		//Extract ArrayList<String> field
		String list1="",list2="", npl="";
		if (pl.indexOf("[")>=0) {
			// found first List
			list1=pl.substring(pl.indexOf("[")+1, pl.indexOf("]"));
			npl=pl.replace("["+list1+"]\t", "");
			if (npl.indexOf("[")>=0) {
				// found second List
				list2=npl.substring(npl.indexOf("[")+1, npl.indexOf("]"));
				npl=npl.replace("["+list2+"]\t", "");
			}
		} else npl=pl;
//		String prof_group = npl.substring(0,11).trim();
//		String tmp_ps=npl.substring(12,npl.length());

		String[] tmp_pl=npl.split("\t");// {"type","name","active",options...};
		String[] parm= new String[30];
		for (int i=0;i<30;i++) parm[i]="";
		for (int i=0;i<tmp_pl.length;i++) {
			if (tmp_pl[i]==null) parm[i]="";
			else {
				if (tmp_pl[i]==null) parm[i]="";
				else parm[i]=convertToSpecChar(tmp_pl[i]);
			}
		}
		if (parm[1].equals(SMBSYNC_PROF_TYPE_REMOTE)) {//Remote
			rem.add(createRemoteProfilelistItem(mGp,
					parm[0],//group
					parm[2],//Name
					parm[3],//Active
					parm[8],//directory
					parm[4],//user
					parm[5],//pass
					parm[7],//share
					parm[6],//address
					parm[9],//hostname
					"",//port
					"",0,0,
					false));

		} else {
			if (parm[1].equals(SMBSYNC_PROF_TYPE_LOCAL)) {//Local
				if (parm[5].equals("")) parm[5]=mGp.externalRootDirectory;
				lcl.add(createLocalProfilelistItem(mGp,
						parm[0],//group
						parm[2],//Name
						parm[3],//Active
						parm[4],//Directory
						parm[5],//Local mount point
						"",0,0,
						false));
			} else if (parm[1].equals(SMBSYNC_PROF_TYPE_SYNC)) {//Sync
				ArrayList<String> ff=new ArrayList<String>();
				ArrayList<String> df=new ArrayList<String>();
				if (list1.length()!=0) {
					String[] fp=list1.split("\t");
					for (int i=0;i<fp.length;i++) ff.add(convertToSpecChar(fp[i]));					
				} else ff.clear();
				if (list2.length()!=0) {
					String[] dp=list2.split("\t");
					for (int i=0;i<dp.length;i++) df.add(convertToSpecChar(dp[i]));
				} else df.clear();
				boolean mpd=true, conf=false, ujlm=false;
				if (parm[9].equals("0")) mpd=false;
				if (parm[10].equals("1")) conf=true;
				if (parm[11].equals("1")) ujlm=true;
				sync.add(createSyncProfilelistItem(mGp,
						parm[0],//group
						parm[ 2],//Name
						parm[ 3],//Active
						parm[ 4],//Sync type
						parm[ 5],//Master type
						parm[ 6],//Master name
						parm[ 7],//Target type
						parm[ 8],//Target name
						ff,//File Filter
						df,//Dir Filter
						mpd,
						conf,
						ujlm,
						false,
						"0",false,true,true,true,false,
						"",0,0,
						"",0,
						false));
			}
		}
	};

	private void addProfileListVer3(String pl, ArrayList<ProfileListItem> sync,
			ArrayList<ProfileListItem> rem, ArrayList<ProfileListItem> lcl) {
		//Extract ArrayList<String> field
		String list1="",list2="", npl="";
		if (pl.indexOf("[")>=0) {
			// found first List
			list1=pl.substring(pl.indexOf("[")+1, pl.indexOf("]"));
			npl=pl.replace("["+list1+"]\t", "");
			if (npl.indexOf("[")>=0) {
				// found second List
				list2=npl.substring(npl.indexOf("[")+1, npl.indexOf("]"));
				npl=npl.replace("["+list2+"]\t", "");
			}
		} else npl=pl;
//		String prof_group = npl.substring(0,11).trim();
//		String tmp_ps=npl.substring(12,npl.length());

		String[] tmp_pl=npl.split("\t");// {"type","name","active",options...};
		String[] parm= new String[30];
		for (int i=0;i<30;i++) parm[i]="";
		for (int i=0;i<tmp_pl.length;i++) {
			if (tmp_pl[i]==null) parm[i]="";
			else {
				if (tmp_pl[i]==null) parm[i]="";
				else parm[i]=convertToSpecChar(tmp_pl[i].trim());
			}
		}
		if (parm[1].equals("SETTINGS")) return; //ignore settings entry
		
		if (parm[1].equals(SMBSYNC_PROF_TYPE_REMOTE)) {//Remote
			String h_addr="", h_name="";
			if (parm[6].length()>0) {
				if (parm[6].substring(0,1).compareTo("0")>=0 && parm[6].substring(0,1).compareTo("9")<=0) {
					h_addr=parm[6];
				} else {
					h_name=parm[6];
				}
			} else {
				h_addr="";
				h_name=parm[9];
			}
//			Log.v("","h_addr="+h_addr+", h_name="+h_name);
			rem.add(createRemoteProfilelistItem(mGp,
					parm[0],//group
					parm[2],//Name
					parm[3],//Active
					parm[8],//directory
					parm[4],//user
					parm[5],//pass
					parm[7],//share
					h_addr,//address
					h_name,//hostname
					"",//port
					"",0,0,
					false));

		} else {
			if (parm[1].equals(SMBSYNC_PROF_TYPE_LOCAL)) {//Local
				if (parm[5].equals("")) parm[5]=mGp.externalRootDirectory;
				lcl.add(createLocalProfilelistItem(mGp,
						parm[0],//group
						parm[2],//Name
						parm[3],//Active
						parm[4],//Directory
						parm[5],//Local mount point
						"",0,0,
						false));
			} else if (parm[1].equals(SMBSYNC_PROF_TYPE_SYNC)) {//Sync
				ArrayList<String> ff=new ArrayList<String>();
				ArrayList<String> df=new ArrayList<String>();
				if (list1.length()!=0) {
					String[] fp=list1.split("\t");
					for (int i=0;i<fp.length;i++) ff.add(convertToSpecChar(fp[i]));					
				} else ff.clear();
				if (list2.length()!=0) {
					String[] dp=list2.split("\t");
					for (int i=0;i<dp.length;i++) df.add(convertToSpecChar(dp[i]));
				} else df.clear();
				boolean mpd=true, conf=false, ujlm=false;
				if (parm[9].equals("0")) mpd=false;
				if (parm[10].equals("1")) conf=true;
				if (parm[11].equals("1")) ujlm=true;
				sync.add(createSyncProfilelistItem(mGp,
						parm[0],//group
						parm[ 2],//Name
						parm[ 3],//Active
						parm[ 4],//Sync type
						parm[ 5],//Master type
						parm[ 6],//Master name
						parm[ 7],//Target type
						parm[ 8],//Target name
						ff,//File Filter
						df,//Dir Filter
						mpd,
						conf,
						ujlm,
						false,
						"0",false,true,true,true,false,
						"",0,0,
						"",0,
						false));
			}
		}
	};

	private void addProfileListVer4(String pl, ArrayList<ProfileListItem> sync,
			ArrayList<ProfileListItem> rem, ArrayList<ProfileListItem> lcl) {
		//Extract ArrayList<String> field
		String list1="",list2="", npl="";
		if (pl.indexOf("[")>=0) {
			// found first List
			list1=pl.substring(pl.indexOf("[")+1, pl.indexOf("]"));
			npl=pl.replace("["+list1+"]\t", "");
			if (npl.indexOf("[")>=0) {
				// found second List
				list2=npl.substring(npl.indexOf("[")+1, npl.indexOf("]"));
				npl=npl.replace("["+list2+"]\t", "");
			}
		} else npl=pl;
//		String prof_group = npl.substring(0,11).trim();
//		String tmp_ps=npl.substring(12,npl.length());

		String[] tmp_pl=npl.split("\t");// {"type","name","active",options...};
		String[] parm= new String[30];
		for (int i=0;i<30;i++) parm[i]="";
		for (int i=0;i<tmp_pl.length;i++) {
			if (tmp_pl[i]==null) parm[i]="";
			else {
				if (tmp_pl[i]==null) parm[i]="";
				else parm[i]=convertToSpecChar(tmp_pl[i].trim());
			}
		}
		if (parm[1].equals("SETTINGS")) return; //ignore settings entry
		
		if (parm[1].equals(SMBSYNC_PROF_TYPE_REMOTE)) {//Remote
			String h_addr="", h_name="";
			if (parm[6].length()>0) {
				if (parm[6].substring(0,1).compareTo("0")>=0 && parm[6].substring(0,1).compareTo("9")<=0) {
					h_addr=parm[6];
				} else {
					h_name=parm[6];
				}
			} else {
				h_addr="";
				h_name=parm[9];
			}
//			Log.v("","h_addr="+h_addr+", h_name="+h_name);
			rem.add(createRemoteProfilelistItem(mGp,
					parm[0],//group
					parm[2],//Name
					parm[3],//Active
					parm[8],//directory
					parm[4],//user
					parm[5],//pass
					parm[7],//share
					h_addr,//address
					h_name,//hostname
					"",//port
					"",0,0,
					false));

		} else {
			if (parm[1].equals(SMBSYNC_PROF_TYPE_LOCAL)) {//Local
				if (parm[5].equals("")) parm[5]=mGp.externalRootDirectory;
				lcl.add(createLocalProfilelistItem(mGp,
						parm[0],//group
						parm[2],//Name
						parm[3],//Active
						parm[4],//Directory
						parm[5],//Local mount point
						"",0,0,
						false));
			} else if (parm[1].equals(SMBSYNC_PROF_TYPE_SYNC)) {//Sync
				ArrayList<String> ff=new ArrayList<String>();
				ArrayList<String> df=new ArrayList<String>();
				if (list1.length()!=0) {
					String[] fp=list1.split("\t");
					for (int i=0;i<fp.length;i++) ff.add(convertToSpecChar(fp[i]));					
				} else ff.clear();
				if (list2.length()!=0) {
					String[] dp=list2.split("\t");
					for (int i=0;i<dp.length;i++) df.add(convertToSpecChar(dp[i]));
				} else df.clear();
				boolean mpd=true, conf=false, ujlm=false, nulm_remote=false;
				if (parm[9].equals("0")) mpd=false;
				if (parm[10].equals("1")) conf=true;
				if (parm[11].equals("1")) ujlm=true;
				if (parm[12].equals("1")) nulm_remote=true;
				sync.add(createSyncProfilelistItem(mGp,
						parm[0],//group
						parm[ 2],//Name
						parm[ 3],//Active
						parm[ 4],//Sync type
						parm[ 5],//Master type
						parm[ 6],//Master name
						parm[ 7],//Target type
						parm[ 8],//Target name
						ff,//File Filter
						df,//Dir Filter
						mpd,
						conf,
						ujlm,
						nulm_remote,
						"0",false,true,true,true,false,
						"",0,0,
						"",0,
						false));
			}
		}
	};

	private void addProfileListVer5(String pl, ArrayList<ProfileListItem> sync,
			ArrayList<ProfileListItem> rem, ArrayList<ProfileListItem> lcl) {
		//Extract ArrayList<String> field
		String list1="",list2="", npl="";
		if (pl.indexOf("[")>=0) {
			// found first List
			list1=pl.substring(pl.indexOf("[")+1, pl.indexOf("]"));
			npl=pl.replace("["+list1+"]\t", "");
			if (npl.indexOf("[")>=0) {
				// found second List
				list2=npl.substring(npl.indexOf("[")+1, npl.indexOf("]"));
				npl=npl.replace("["+list2+"]\t", "");
			}
		} else npl=pl;
//		Log.v("","pl="+pl);
//		String prof_group = npl.substring(0,11).trim();
//		String tmp_ps=npl.substring(12,npl.length());

		String[] tmp_pl=npl.split("\t");// {"type","name","active",options...};
		String[] parm= new String[30];
		for (int i=0;i<30;i++) parm[i]="";
		for (int i=0;i<tmp_pl.length;i++) {
			if (tmp_pl[i]==null) parm[i]="";
			else {
				if (tmp_pl[i]==null) parm[i]="";
				else parm[i]=convertToSpecChar(tmp_pl[i].trim());
			}
//			Log.v("","i="+i+", "+parm[i]);
		}
		if (parm[1].equals("SETTINGS")) return; //ignore settings entry
		
		if (parm[1].equals(SMBSYNC_PROF_TYPE_REMOTE)) {//Remote
			String h_addr="", h_name="";
			if (parm[6].length()>0) {
				if (parm[6].substring(0,1).compareTo("0")>=0 && parm[6].substring(0,1).compareTo("9")<=0) {
					h_addr=parm[6];
				} else {
					h_name=parm[6];
				}
			} else {
				h_addr="";
				h_name=parm[9];
			}
//			Log.v("","h_addr="+h_addr+", h_name="+h_name);
			rem.add(createRemoteProfilelistItem(mGp,
					parm[0],//group
					parm[2],//Name
					parm[3],//Active
					parm[8],//directory
					parm[4],//user
					parm[5],//pass
					parm[7],//share
					h_addr,//address
					h_name,//hostname
					parm[10],//port
					"",0,0,
					false));

		} else {
			if (parm[1].equals(SMBSYNC_PROF_TYPE_LOCAL)) {//Local
				if (parm[5].equals("")) parm[5]=mGp.externalRootDirectory;
				lcl.add(createLocalProfilelistItem(mGp,
						parm[0],//group
						parm[2],//Name
						parm[3],//Active
						parm[4],//Directory
						parm[5],//Local mount point
						"",0,0,
						false));
			} else if (parm[1].equals(SMBSYNC_PROF_TYPE_SYNC)) {//Sync
				ArrayList<String> ff=new ArrayList<String>();
				ArrayList<String> df=new ArrayList<String>();
				if (list1.length()!=0) {
					String[] fp=list1.split("\t");
					for (int i=0;i<fp.length;i++) ff.add(convertToSpecChar(fp[i]));					
				} else ff.clear();
				if (list2.length()!=0) {
					String[] dp=list2.split("\t");
					for (int i=0;i<dp.length;i++) df.add(convertToSpecChar(dp[i]));
				} else df.clear();
				boolean mpd=true, conf=false, ujlm=false, nulm_remote=false;
				if (parm[9].equals("0")) mpd=false;
				if (parm[10].equals("1")) conf=true;
				if (parm[11].equals("1")) ujlm=true;
				if (parm[12].equals("1")) nulm_remote=true;
				sync.add(createSyncProfilelistItem(mGp,
						parm[0],//group
						parm[ 2],//Name
						parm[ 3],//Active
						parm[ 4],//Sync type
						parm[ 5],//Master type
						parm[ 6],//Master name
						parm[ 7],//Target type
						parm[ 8],//Target name
						ff,//File Filter
						df,//Dir Filter
						mpd,
						conf,
						ujlm,
						nulm_remote,
						"0",false,true,true,true,false,
						"",0,0,
						"",0,
						false));
			}
		}
	};

	private void addProfileListVer6(String pl, ArrayList<ProfileListItem> sync,
			ArrayList<ProfileListItem> rem, ArrayList<ProfileListItem> lcl) {
		//Extract ArrayList<String> field
		String list1="",list2="", npl="";
		if (pl.indexOf("[")>=0) {
			// found first List
			list1=pl.substring(pl.indexOf("[")+1, pl.indexOf("]"));
			npl=pl.replace("["+list1+"]\t", "");
			if (npl.indexOf("[")>=0) {
				// found second List
				list2=npl.substring(npl.indexOf("[")+1, npl.indexOf("]"));
				npl=npl.replace("["+list2+"]\t", "");
			}
		} else npl=pl;
//		Log.v("","pl="+pl);
//		String prof_group = npl.substring(0,11).trim();
//		String tmp_ps=npl.substring(12,npl.length());

		String[] tmp_pl=npl.split("\t");// {"type","name","active",options...};
		String[] parm= new String[30];
		for (int i=0;i<30;i++) parm[i]="";
		for (int i=0;i<tmp_pl.length;i++) {
			if (tmp_pl[i]==null) parm[i]="";
			else {
				if (tmp_pl[i]==null) parm[i]="";
				else parm[i]=convertToSpecChar(tmp_pl[i].trim());
			}
//			Log.v("","i="+i+", "+parm[i]);
		}
		if (parm[1].equals("SETTINGS")) return; //ignore settings entry
		
		if (parm[1].equals(SMBSYNC_PROF_TYPE_REMOTE)) {//Remote
			String h_addr="", h_name="";
			if (parm[6].length()>0) {
				if (parm[6].substring(0,1).compareTo("0")>=0 && parm[6].substring(0,1).compareTo("9")<=0) {
					h_addr=parm[6];
				} else {
					h_name=parm[6];
				}
			} else {
				h_addr="";
				h_name=parm[9];
			}
//			Log.v("","h_addr="+h_addr+", h_name="+h_name);
			rem.add(createRemoteProfilelistItem(mGp,
					parm[0],//group
					parm[2],//Name
					parm[3],//Active
					parm[8],//directory
					parm[4],//user
					parm[5],//pass
					parm[7],//share
					h_addr,//address
					h_name,//hostname
					parm[10],//port
					"",0,0,
					false));

		} else {
			if (parm[1].equals(SMBSYNC_PROF_TYPE_LOCAL)) {//Local
				if (parm[5].equals("")) parm[5]=mGp.externalRootDirectory;
				lcl.add(createLocalProfilelistItem(mGp,
						parm[0],//group
						parm[2],//Name
						parm[3],//Active
						parm[4],//Directory
						parm[5],//Local mount point
						"",0,0,
						false));
			} else if (parm[1].equals(SMBSYNC_PROF_TYPE_SYNC)) {//Sync
				ArrayList<String> ff=new ArrayList<String>();
				ArrayList<String> df=new ArrayList<String>();
				if (list1.length()!=0) {
					String[] fp=list1.split("\t");
					for (int i=0;i<fp.length;i++) ff.add(convertToSpecChar(fp[i]));					
				} else ff.clear();
				if (list2.length()!=0) {
					String[] dp=list2.split("\t");
					for (int i=0;i<dp.length;i++) df.add(convertToSpecChar(dp[i]));
				} else df.clear();
				boolean mpd=true, conf=false, ujlm=false, nulm_remote=false;
				if (parm[9].equals("0")) mpd=false;
				if (parm[10].equals("1")) conf=true;
				if (parm[11].equals("1")) ujlm=true;
				if (parm[12].equals("1")) nulm_remote=true;
				boolean sync_empty_dir=false;
				if (parm[14].equals("1")) sync_empty_dir=true;
				boolean sync_hidden_dir=false;
				if (parm[15].equals("1")) sync_hidden_dir=true;
				boolean sync_hidden_file=false;
				if (parm[16].equals("1")) sync_hidden_file=true;
				boolean sync_sub_dir=false;
				if (parm[17].equals("1")) sync_sub_dir=true;
//				Log.v("","17="+parm[17]);
				sync.add(createSyncProfilelistItem(mGp, 
						parm[0],//group
						parm[ 2],//Name
						parm[ 3],//Active
						parm[ 4],//Sync type
						parm[ 5],//Master type
						parm[ 6],//Master name
						parm[ 7],//Target type
						parm[ 8],//Target name
						ff,//File Filter
						df,//Dir Filter
						mpd,
						conf,
						ujlm,
						nulm_remote,
						parm[13],//Retry count
						sync_empty_dir,
						sync_hidden_dir, sync_hidden_file, sync_sub_dir,false,
						"",0,0,
						"",0,
						false));
			}
		}
	};

	private void addProfileListVer7(String pl, ArrayList<ProfileListItem> sync,
			ArrayList<ProfileListItem> rem, ArrayList<ProfileListItem> lcl) {
		//Extract ArrayList<String> field
		String list1="",list2="", npl="";
		if (pl.indexOf("[")>=0) {
			// found first List
			list1=pl.substring(pl.indexOf("[")+1, pl.indexOf("]"));
			npl=pl.replace("["+list1+"]\t", "");
			if (npl.indexOf("[")>=0) {
				// found second List
				list2=npl.substring(npl.indexOf("[")+1, npl.indexOf("]"));
				npl=npl.replace("["+list2+"]\t", "");
			}
		} else npl=pl;
//		Log.v("","pl="+pl);
//		String prof_group = npl.substring(0,11).trim();
//		String tmp_ps=npl.substring(12,npl.length());

		String[] tmp_pl=npl.split("\t");// {"type","name","active",options...};
		String[] parm= new String[30];
		for (int i=0;i<30;i++) parm[i]="";
		for (int i=0;i<tmp_pl.length;i++) {
			if (tmp_pl[i]==null) parm[i]="";
			else {
				if (tmp_pl[i]==null) parm[i]="";
				else parm[i]=convertToSpecChar(tmp_pl[i].trim());
			}
//			Log.v("","i="+i+", "+parm[i]);
		}
		if (parm[1].equals("SETTINGS")) return; //ignore settings entry
		
		if (parm[1].equals(SMBSYNC_PROF_TYPE_REMOTE)) {//Remote
			String h_addr="", h_name="";
			if (parm[6].length()>0) {
				if (parm[6].substring(0,1).compareTo("0")>=0 && parm[6].substring(0,1).compareTo("9")<=0) {
					h_addr=parm[6];
				} else {
					h_name=parm[6];
				}
			} else {
				h_addr="";
				h_name=parm[9];
			}
//			Log.v("","h_addr="+h_addr+", h_name="+h_name);
			rem.add(createRemoteProfilelistItem(mGp,
					parm[0],//group
					parm[2],//Name
					parm[3],//Active
					parm[8],//directory
					parm[4],//user
					parm[5],//pass
					parm[7],//share
					h_addr,//address
					h_name,//hostname
					parm[10],//port
					parm[11],//Zip file name
					Integer.parseInt(parm[12]),//Zip enc method
					Integer.parseInt(parm[13]),//Zip enc key length
					false));

		} else {
			if (parm[1].equals(SMBSYNC_PROF_TYPE_LOCAL)) {//Local
				if (parm[5].equals("")) parm[5]=mGp.externalRootDirectory;
				lcl.add(createLocalProfilelistItem(mGp,
						parm[0],//group
						parm[2],//Name
						parm[3],//Active
						parm[4],//Directory
						parm[5],//Local mount point
						parm[6],//Zip file name
						Integer.parseInt(parm[7]),//Zip enc method
						Integer.parseInt(parm[8]),//Zip enc key length
						false));
			} else if (parm[1].equals(SMBSYNC_PROF_TYPE_SYNC)) {//Sync
				ArrayList<String> ff=new ArrayList<String>();
				ArrayList<String> df=new ArrayList<String>();
				if (list1.length()!=0) {
					String[] fp=list1.split("\t");
					for (int i=0;i<fp.length;i++) ff.add(convertToSpecChar(fp[i]));					
				} else ff.clear();
				if (list2.length()!=0) {
					String[] dp=list2.split("\t");
					for (int i=0;i<dp.length;i++) df.add(convertToSpecChar(dp[i]));
				} else df.clear();
				boolean mpd=true, conf=false, ujlm=false, nulm_remote=false;
				if (parm[9].equals("0")) mpd=false;
				if (parm[10].equals("1")) conf=true;
				if (parm[11].equals("1")) ujlm=true;
				if (parm[12].equals("1")) nulm_remote=true;
				boolean sync_empty_dir=false;
				if (parm[14].equals("1")) sync_empty_dir=true;
				boolean sync_hidden_dir=false;
				if (parm[15].equals("1")) sync_hidden_dir=true;
				boolean sync_hidden_file=false;
				if (parm[16].equals("1")) sync_hidden_file=true;
				boolean sync_sub_dir=false;
				if (parm[17].equals("1")) sync_sub_dir=true;
//				Log.v("","17="+parm[17]);
				sync.add(createSyncProfilelistItem(mGp, 
						parm[0],//group
						parm[ 2],//Name
						parm[ 3],//Active
						parm[ 4],//Sync type
						parm[ 5],//Master type
						parm[ 6],//Master name
						parm[ 7],//Target type
						parm[ 8],//Target name
						ff,//File Filter
						df,//Dir Filter
						mpd,
						conf,
						ujlm,
						nulm_remote,
						parm[13],//Retry count
						sync_empty_dir,
						sync_hidden_dir, sync_hidden_file, sync_sub_dir, false,
						parm[18],//Zip file name
						Integer.parseInt(parm[19]),//Zip enc method
						Integer.parseInt(parm[20]),//Zip enc key length
						parm[21],//Last sync time
						Integer.parseInt(parm[22]),//Last sync result
						false));
			}
		}
	};

	private void addProfileListVer8(String pl, ArrayList<ProfileListItem> sync,
			ArrayList<ProfileListItem> rem, ArrayList<ProfileListItem> lcl) {
		//Extract ArrayList<String> field
		String list1="",list2="", npl="";
		if (pl.indexOf("[")>=0) {
			// found first List
			list1=pl.substring(pl.indexOf("[")+1, pl.indexOf("]"));
			npl=pl.replace("["+list1+"]\t", "");
			if (npl.indexOf("[")>=0) {
				// found second List
				list2=npl.substring(npl.indexOf("[")+1, npl.indexOf("]"));
				npl=npl.replace("["+list2+"]\t", "");
			}
		} else npl=pl;
//		Log.v("","pl="+pl);
//		String prof_group = npl.substring(0,11).trim();
//		String tmp_ps=npl.substring(12,npl.length());

		String[] tmp_pl=npl.split("\t");// {"type","name","active",options...};
		String[] parm= new String[30];
		for (int i=0;i<30;i++) parm[i]="";
		for (int i=0;i<tmp_pl.length;i++) {
			if (tmp_pl[i]==null) parm[i]="";
			else {
				if (tmp_pl[i]==null) parm[i]="";
				else parm[i]=convertToSpecChar(tmp_pl[i].trim());
			}
//			Log.v("","i="+i+", "+parm[i]);
		}
		if (parm[1].equals("SETTINGS")) return; //ignore settings entry
		
		if (parm[1].equals(SMBSYNC_PROF_TYPE_REMOTE)) {//Remote
			String h_addr="", h_name="";
			if (parm[6].length()>0) {
				if (parm[6].substring(0,1).compareTo("0")>=0 && parm[6].substring(0,1).compareTo("9")<=0) {
					h_addr=parm[6];
				} else {
					h_name=parm[6];
				}
			} else {
				h_addr="";
				h_name=parm[9];
			}
//			Log.v("","h_addr="+h_addr+", h_name="+h_name);
			rem.add(createRemoteProfilelistItem(mGp,
					parm[0],//group
					parm[2],//Name
					parm[3],//Active
					parm[8],//directory
					parm[4],//user
					parm[5],//pass
					parm[7],//share
					h_addr,//address
					h_name,//hostname
					parm[10],//port
					parm[11],//Zip file name
					Integer.parseInt(parm[12]),//Zip enc method
					Integer.parseInt(parm[13]),//Zip enc key length
					false));

		} else {
			if (parm[1].equals(SMBSYNC_PROF_TYPE_LOCAL)) {//Local
				if (parm[5].equals("")) parm[5]=mGp.externalRootDirectory;
				lcl.add(createLocalProfilelistItem(mGp,
						parm[0],//group
						parm[2],//Name
						parm[3],//Active
						parm[4],//Directory
						parm[5],//Local mount point
						parm[6],//Zip file name
						Integer.parseInt(parm[7]),//Zip enc method
						Integer.parseInt(parm[8]),//Zip enc key length
						false));
			} else if (parm[1].equals(SMBSYNC_PROF_TYPE_SYNC)) {//Sync
				ArrayList<String> ff=new ArrayList<String>();
				ArrayList<String> df=new ArrayList<String>();
				if (list1.length()!=0) {
					String[] fp=list1.split("\t");
					for (int i=0;i<fp.length;i++) ff.add(convertToSpecChar(fp[i]));					
				} else ff.clear();
				if (list2.length()!=0) {
					String[] dp=list2.split("\t");
					for (int i=0;i<dp.length;i++) df.add(convertToSpecChar(dp[i]));
				} else df.clear();
				boolean mpd=true, conf=false, ujlm=false, nulm_remote=false;
				if (parm[9].equals("0")) mpd=false;
				if (parm[10].equals("1")) conf=true;
				if (parm[11].equals("1")) ujlm=true;
				if (parm[12].equals("1")) nulm_remote=true;
				boolean sync_empty_dir=false;
				if (parm[14].equals("1")) sync_empty_dir=true;
				boolean sync_hidden_dir=false;
				if (parm[15].equals("1")) sync_hidden_dir=true;
				boolean sync_hidden_file=false;
				if (parm[16].equals("1")) sync_hidden_file=true;
				boolean sync_sub_dir=false;
				if (parm[17].equals("1")) sync_sub_dir=true;
				
				boolean sync_use_remote_small_io_area=false;
				if (parm[18].equals("1")) sync_use_remote_small_io_area=true;

//				Log.v("","17="+parm[17]);
				sync.add(createSyncProfilelistItem(mGp, 
						parm[0],//group
						parm[ 2],//Name
						parm[ 3],//Active
						parm[ 4],//Sync type
						parm[ 5],//Master type
						parm[ 6],//Master name
						parm[ 7],//Target type
						parm[ 8],//Target name
						ff,//File Filter
						df,//Dir Filter
						mpd,
						conf,
						ujlm,
						nulm_remote,
						parm[13],//Retry count
						sync_empty_dir,
						sync_hidden_dir, sync_hidden_file, sync_sub_dir, sync_use_remote_small_io_area,
						parm[19],//Zip file name
						Integer.parseInt(parm[20]),//Zip enc method
						Integer.parseInt(parm[21]),//Zip enc key length
						parm[22],//Last sync time
						Integer.parseInt(parm[23]),//Last sync result
						false));
			}
		}
	};

	private String convertToSpecChar(String in) {
		if (in==null || in.length()==0) return "";
		boolean cont=true;
		String out=in;
		while (cont) {
			if (out.indexOf("\u0001")>=0) out=out.replace("\u0001","[") ;
			else cont=false;
		}

		cont=true;
		while (cont) {
			if (out.indexOf("\u0002")>=0) out=out.replace("\u0002","]") ;
			else cont=false;
		}
		
		return out;
	};
	
	private static String convertToCodeChar(String in) {
		if (in==null || in.length()==0) return "";
		boolean cont=true;
		String out=in;
		while (cont) {
			if (out.indexOf("[")>=0) out=out.replace("[","\u0001") ;
			else cont=false;
		}

		cont=true;
		while (cont) {
			if (out.indexOf("]")>=0) out=out.replace("]","\u0002") ;
			else cont=false;
		}
		return out;
	};

	static public void updateSyncProfileAdapter(GlobalParameters gp, String prof_name, 
			String prof_act,String prof_syncopt, String prof_master_typ,String prof_master,
			String prof_target_typ,String prof_target, 
			ArrayList<String> file_filter, ArrayList<String> dir_filter,
			boolean prof_mpd, boolean prof_conf, boolean prof_ujlm, boolean nulm_remote,
			String retry_count, boolean sync_empty_dir, boolean sync_hidden_file, 
			boolean sync_hidden_dir, boolean sync_sub_dir, boolean sync_use_remote_small_io_area,
			String zip_file_name, int zip_enc_method, int zip_enc_key_length,
			String last_sync_time, int last_sync_result,
			boolean isChk, int pos) {
		String prof_group=SMBSYNC_PROF_GROUP_DEFAULT;
		boolean isExists=isProfileExists(prof_group, SMBSYNC_PROF_TYPE_SYNC, prof_name,
				gp.profileAdapter.getArrayList());
		if (!isExists) {
			ProfileListItem pfli=createSyncProfilelistItem(gp, prof_group,prof_name,prof_act,
					prof_syncopt,prof_master_typ,prof_master,prof_target_typ,prof_target,
					file_filter, dir_filter,prof_mpd,prof_conf,prof_ujlm,nulm_remote,
					retry_count, sync_empty_dir, sync_hidden_file, sync_hidden_dir, 
					sync_sub_dir,sync_use_remote_small_io_area,
					zip_file_name, zip_enc_method, zip_enc_key_length,
					last_sync_time, last_sync_result,
					isChk);
			gp.profileAdapter.add(pfli);
		} else {
//			glblParms.profileAdapter.remove(glblParms.profileAdapter.getItem(pos));
			ProfileListItem pfli=createSyncProfilelistItem(gp, prof_group,prof_name,prof_act,
					prof_syncopt,prof_master_typ,prof_master,prof_target_typ,prof_target,
					file_filter, dir_filter, prof_mpd,prof_conf,prof_ujlm,nulm_remote,
					retry_count, sync_empty_dir, sync_hidden_file, sync_hidden_dir,
					sync_sub_dir,sync_use_remote_small_io_area,
					zip_file_name, zip_enc_method, zip_enc_key_length,
					last_sync_time, last_sync_result,
					isChk);
			gp.profileAdapter.replace(pfli,pos);
		}
	};

	static public ProfileListItem createSyncProfilelistItem(GlobalParameters gp, 
			String prof_group,String prof_name, 
			String prof_act,String prof_syncopt, String prof_master_typ,String prof_master,
			String prof_target_typ,String prof_target, 
			ArrayList<String> ff, ArrayList<String> df,boolean prof_mpd, 
			boolean prof_conf, boolean prof_ujlm, boolean nulm_remote, 
			String retry_count, boolean sync_empty_dir, boolean sync_hidden_file, 
			boolean sync_hidden_dir, boolean sync_sub_dir, boolean sync_use_remote_small_io_area,
			String zip_file_name, int zip_enc_method, int zip_enc_key_length,
			String last_sync_time, int last_sync_result,
			boolean isChk) {
		return new ProfileListItem(prof_group,SMBSYNC_PROF_TYPE_SYNC,prof_name,prof_act,
				prof_syncopt,
				prof_master_typ,
				prof_master,
				prof_target_typ,
				prof_target,
				ff,
				df,
				prof_mpd,
				prof_conf,
				prof_ujlm,
				nulm_remote,
				retry_count,
				sync_empty_dir,
				sync_hidden_file,
				sync_hidden_dir,
				sync_sub_dir,
				sync_use_remote_small_io_area,
				zip_file_name, zip_enc_method, zip_enc_key_length,
				last_sync_time, last_sync_result,
				isChk);
	};

	static public void updateRemoteProfileAdapter(GlobalParameters gp, boolean isAdd, String prof_name, 
			String prof_act,String prof_dir, String prof_user, String prof_pass, 
			String prof_share, String prof_addr, String prof_host, String prof_port,
			String zip_file_name, int zip_enc_method, int zip_enc_key_length,
			boolean isChk,int pos) {
		String prof_group=SMBSYNC_PROF_GROUP_DEFAULT;
		if (isAdd) {
			gp.profileAdapter.add(createRemoteProfilelistItem(gp,prof_group, prof_name,prof_act,
					prof_dir,prof_user,prof_pass,prof_share,prof_addr,prof_host,prof_port,
					zip_file_name, zip_enc_method, zip_enc_key_length,
					isChk));
		} else {
//			glblParms.profileAdapter.remove(glblParms.profileAdapter.getItem(pos));
			gp.profileAdapter.replace(createRemoteProfilelistItem(gp,prof_group, prof_name,prof_act,
					prof_dir,prof_user,prof_pass,prof_share,prof_addr,prof_host,prof_port,
					zip_file_name, zip_enc_method, zip_enc_key_length,
					isChk),pos);
		}

	};
	static private ProfileListItem createRemoteProfilelistItem(GlobalParameters gp, String prof_group, String prof_name, 
			String prof_act,String prof_dir, String prof_user, String prof_pass, 
			String prof_share, String prof_addr, String prof_host, String prof_port,
			String zip_file_name, int zip_enc_method, int zip_enc_key_length,
			boolean isChk) {
		return new ProfileListItem(prof_group,SMBSYNC_PROF_TYPE_REMOTE,prof_name,prof_act,
				prof_user,
				prof_pass,
				prof_addr,
				prof_host,
				prof_port,
				prof_share,
				prof_dir,
				zip_file_name, zip_enc_method, zip_enc_key_length,
				isChk);
	};

	static public void updateLocalProfileAdapter(GlobalParameters gp, boolean isAdd, String prof_name, 
			String prof_act, String prof_lmp, String prof_dir,
			String zip_file_name, int zip_enc_method, int zip_enc_key_length,
			boolean isChk,int pos) {
		String prof_group=SMBSYNC_PROF_GROUP_DEFAULT;
		if (isAdd) {
			gp.profileAdapter.add(createLocalProfilelistItem(gp,prof_group,prof_name,
					prof_act, prof_dir, prof_lmp,
					zip_file_name, zip_enc_method, zip_enc_key_length,
					isChk));
		} else {
			gp.profileAdapter.replace(
					createLocalProfilelistItem(gp,prof_group,prof_name,prof_act,
							prof_dir, prof_lmp,
							zip_file_name, zip_enc_method, zip_enc_key_length,
							isChk),pos);
		}
	};

	static private ProfileListItem createLocalProfilelistItem(GlobalParameters gp, String prof_group,
			String prof_name, String prof_act, String prof_dir, 
			String prof_lmp,
			String zip_file_name, int zip_enc_method, int zip_enc_key_length,
			boolean isChk) {
		return new ProfileListItem(prof_group,SMBSYNC_PROF_TYPE_LOCAL,
				prof_name,prof_act, prof_lmp, prof_dir,
				zip_file_name, zip_enc_method, zip_enc_key_length,
				isChk);
	};

	static public int getProfilePos(String pfn, AdapterProfileList pa) {
		int pos=-1;
		for(int i=0;i<pa.getCount();i++) {
			if (pa.getItem(i).getProfileName().equals(pfn)) {
				pos=i;
				break;
			}
		}
		return pos;
	};
	
	public static String getProfileType(String pfn, AdapterProfileList pa) {
		return getProfileType(pfn,pa.getArrayList());
	}; 

	public static String getProfileType(String pfn, ArrayList<ProfileListItem> pfl) {
		for (int i=0;i<pfl.size();i++)
			if (pfl.get(i).getProfileName().equals(pfn)) 
				return pfl.get(i).getProfileType(); 
		return "";
	};

	public static ProfileListItem getProfile(String pfn, AdapterProfileList pa) {
		return getProfile(pfn, pa.getArrayList());
	};

	public static ProfileListItem getProfile(String pfn, ArrayList<ProfileListItem> pfl) {
		for (int i=0;i<pfl.size();i++)
			if (pfl.get(i).getProfileName().equals(pfn)) 
				return pfl.get(i); 
		return null;
	};

	public static boolean saveProfileToFile(GlobalParameters mGp, Context c, SMBSyncUtil util,
			boolean sdcard, String fd, String fp,
			AdapterProfileList pfl, boolean encrypt_required) {
		boolean result=true;
		String ofp="";
		PrintWriter pw;
		BufferedWriter bw=null;
		try {
			CipherParms cp=null;
			if (sdcard) {
				if (encrypt_required) {
					cp=EncryptUtil.initEncryptEnv(mGp.profileKeyPrefix+mGp.profilePassword);
				}
				File lf=new File(fd);
				if (!lf.exists()) lf.mkdir();
				bw = new BufferedWriter(new FileWriter(fp),8192);
				pw = new PrintWriter(bw);
				ofp=fp;
				if (encrypt_required) {
					byte[] enc_array=EncryptUtil.encrypt(SMBSYNC_PROF_ENC, cp); 
					String enc_str = 
							Base64Compat.encodeToString(enc_array, Base64Compat.NO_WRAP);
//					MiscUtil.hexString("", enc_array, 0, enc_array.length);
					pw.println(CURRENT_SMBSYNC_PROFILE_VERSION+SMBSYNC_PROF_ENC+enc_str);
				}
				else pw.println(CURRENT_SMBSYNC_PROFILE_VERSION+SMBSYNC_PROF_DEC);
			} else {
//				OutputStream out = context.openFileOutput(SMBSYNC_PROFILE_FILE_NAME,
//						Context.MODE_PRIVATE);
//				pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
//				ofp=SMBSYNC_PROFILE_FILE_NAME;
				ofp=mGp.internalRootDirectory+"/"+CURRENT_SMBSYNC_PROFILE_FILE_NAME;
				File lf=new File(mGp.internalRootDirectory);
				if (!lf.exists()) lf.mkdir();
				bw =new BufferedWriter(new FileWriter(ofp),8192);
				pw = new PrintWriter(bw);
			}

			if (pfl.getCount() > 0) {
				String pl;
				for (int i = 0; i < pfl.getCount(); i++) {
					ProfileListItem item = pfl.getItem(i);
					String pl_name=convertToCodeChar(item.getProfileName());
					String pl_active=item.getProfileActive();
					String pl_lmp=convertToCodeChar(item.getLocalMountPoint());
					String pl_dir=convertToCodeChar(item.getDirectoryName());
					String pl_user=convertToCodeChar(item.getRemoteUserID());
					String pl_pass=convertToCodeChar(item.getRemotePassword());
					String pl_addr=convertToCodeChar(item.getRemoteAddr());
					String pl_host=convertToCodeChar(item.getRemoteHostname());
					String pl_share=convertToCodeChar(item.getRemoteShareName());
					String pl_port=item.getRemotePort();
					String pl_synctype=item.getSyncType();
					String pl_mastertype=item.getMasterType();
					String pl_targettype=item.getTargetType();
					String pl_mastername=convertToCodeChar(item.getMasterName());
					String pl_targetname=convertToCodeChar(item.getTargetName());
					String pl_retry_count=item.getRetryCount();
					if (!item.getProfileType().equals("")) {
						pl = "";
						if (item.getProfileType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
							pl =item.getProfileGroup()+"\t"+
									SMBSYNC_PROF_TYPE_LOCAL+ "\t" + pl_name + "\t"
									+ pl_active + "\t" +pl_dir+"\t"+
									pl_lmp+"\t"+
									item.getLocalZipFileName()+"\t"+
									item.getLocalZipEncMethod()+"\t"+
									item.getLocalZipAesKeyLength()+"\t"
									;
						} else if (item.getProfileType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
							pl =item.getProfileGroup()+"\t"+
									SMBSYNC_PROF_TYPE_REMOTE+ "\t" + pl_name + "\t"
									+ pl_active + "\t" +
									pl_user+"\t" +
									pl_pass+"\t" +
									pl_addr+ "\t" +
									pl_share+"\t" +
									pl_dir+"\t" +
									pl_host+ "\t"+
									pl_port+ "\t"+
									item.getRemoteZipFileName()+"\t"+
									item.getRemoteZipEncMethod()+"\t"+
									item.getRemoteZipAesKeyLength()+"\t"
									;
						} else if (item.getProfileType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
							String fl="", dl="";
							for (int j=0;j<item.getFileFilter().size();j++) {
								if (fl.length()!=0) fl+="\t"; 
								if (!item.getFileFilter().get(j).equals("")) 
									fl+=item.getFileFilter().get(j); 
							}
							fl=convertToCodeChar(fl);
							fl="["+fl+"]";
							for (int j=0;j<item.getDirFilter().size();j++) {
								if (dl.length()!=0) dl+="\t";
								if (!item.getDirFilter().get(j).equals("")) 
									dl+=item.getDirFilter().get(j); 
							}
							dl=convertToCodeChar(dl);
							dl="["+dl+"]";

							String mpd="1";
							if (!item.isMasterDirFileProcess()) mpd="0";
							String conf="1";
							if (!item.isConfirmRequired()) conf="0";
							String ujlm="1";
							if (!item.isForceLastModifiedUseSmbsync()) ujlm="0";
							String nulm_remote="0";
							if (item.isNotUseLastModifiedForRemote()) nulm_remote="1";
							String empty_dir="0";
							if (item.isSyncEmptyDirectory()) empty_dir="1";
							String sync_hidden_dir="0";
							if (item.isSyncHiddenDirectory()) sync_hidden_dir="1";
							String sync_hidden_file="0";
							if (item.isSyncHiddenFile()) sync_hidden_file="1";
							String sync_sub_dir="0";
							if (item.isSyncSubDirectory()) sync_sub_dir="1";
							String sync_use_remote_small_io_area="0";
							if (item.isSyncUseRemoteSmallIoArea()) sync_use_remote_small_io_area="1";
//							Log.v("","io="+sync_use_remote_small_io_area);
							pl =item.getProfileGroup()+"\t"+
									SMBSYNC_PROF_TYPE_SYNC+ "\t" + pl_name + "\t"+
									pl_active + "\t" +
									pl_synctype+"\t" +
									pl_mastertype+"\t" +
									pl_mastername+"\t" +
									pl_targettype+"\t" +
									pl_targetname+"\t" +
									fl+"\t" +
									dl+"\t"+
									mpd+"\t"+
									conf+"\t"+
									ujlm+"\t"+
									nulm_remote+"\t"+
									pl_retry_count+"\t"+
									empty_dir+"\t"+
									sync_hidden_dir+"\t"+
									sync_hidden_file+"\t"+
									sync_sub_dir+"\t"+
									sync_use_remote_small_io_area+"\t"+
									item.getSyncZipFileName()+"\t"+
									item.getSyncZipEncMethod()+"\t"+
									item.getSyncZipAesKeyLength()+"\t"+
									item.getLastSyncTime()+"\t"+
									item.getLastSyncResult()
									;
						}
						util.addDebugLogMsg(9,"I","saveProfileToFile=" + pl);
						if (sdcard) {
							if (encrypt_required) {
								String enc = 
										Base64Compat.encodeToString(
											EncryptUtil.encrypt(pl, cp), 
											Base64Compat.NO_WRAP);
								pw.println(CURRENT_SMBSYNC_PROFILE_VERSION+enc);
							} else {
								pw.println(CURRENT_SMBSYNC_PROFILE_VERSION+pl);
							}
						} else {
							pw.println(CURRENT_SMBSYNC_PROFILE_VERSION+pl);
						}
					}
				}
			}
			saveSettingsParmsToFile(c,pw,encrypt_required,cp);
			pw.close();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			util.addLogMsg("E",String.format(msgs_save_to_profile_error,ofp));
			util.addLogMsg("E",e.toString());
			result=false;
		}
		
		return result;
	};
	
	static private void addImportSettingsParm(String pl, ArrayList<PreferenceParmListIItem>ispl) {
		String tmp_ps=pl.substring(7,pl.length());
		String[] tmp_pl=tmp_ps.split("\t");// {"type","name","active",options...};
		if (tmp_pl!=null && tmp_pl.length>=5 && tmp_pl[1]!=null && tmp_pl[1].equals(SMBSYNC_PROF_TYPE_SETTINGS)) {
//			String[] val = new String[]{parm[2],parm[3],parm[4]};
			PreferenceParmListIItem ppli=new PreferenceParmListIItem();
			if (tmp_pl[2]!=null) ppli.parms_key=tmp_pl[2];
			if (tmp_pl[3]!=null) ppli.parms_type=tmp_pl[3];
			if (tmp_pl[4]!=null) ppli.parms_value=tmp_pl[4];
			if (!ppli.parms_key.equals("") && !ppli.parms_type.equals("")) {
//				Log.v("","key="+tmp_pl[2]+", value="+tmp_pl[4]+", type="+tmp_pl[3]);
				ispl.add(ppli);
			}
		}
	};

	private static void saveSettingsParmsToFileString(Context c, String group, PrintWriter pw, String dflt,
			boolean encrypt_required, final CipherParms cp, String key) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		String k_type, k_val;

		k_val=prefs.getString(key, dflt);
		k_type=SMBSYNC_SETTINGS_TYPE_STRING;
		String k_str=group+"\t"+
				SMBSYNC_PROF_TYPE_SETTINGS+"\t"+key+"\t"+k_type+"\t"+k_val;
		if (encrypt_required) {
			byte[] out=EncryptUtil.encrypt(k_str,cp);
			String enc = Base64Compat.encodeToString(
						out, 
						Base64Compat.NO_WRAP);
			pw.println(CURRENT_SMBSYNC_PROFILE_VERSION+enc);
		} else {
			pw.println(CURRENT_SMBSYNC_PROFILE_VERSION+k_str);
		}
	};
	
	@SuppressWarnings("unused")
	static private void saveSettingsParmsToFileInt(Context c, String group, PrintWriter pw, int dflt,
			boolean encrypt_required, final CipherParms cp, String key) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		String k_type;
		int k_val;

		k_val=prefs.getInt(key, dflt);
		k_type=SMBSYNC_SETTINGS_TYPE_INT;
		String k_str=group+"\t"+
				SMBSYNC_PROF_TYPE_SETTINGS+"\t"+key+"\t"+k_type+"\t"+k_val;
		if (encrypt_required) {
			String enc = Base64Compat.encodeToString(
						EncryptUtil.encrypt(k_str,cp), 
						Base64Compat.NO_WRAP);
			pw.println(CURRENT_SMBSYNC_PROFILE_VERSION+enc);
		} else {
			pw.println(CURRENT_SMBSYNC_PROFILE_VERSION+k_str);
		}
	};
	static private void saveSettingsParmsToFileLong(Context c, String group, PrintWriter pw, long dflt,
			boolean encrypt_required, final CipherParms cp, String key) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		String k_type;
		long k_val;

		k_val=prefs.getLong(key, dflt);
		k_type=SMBSYNC_SETTINGS_TYPE_LONG;
		String k_str=group+"\t"+
				SMBSYNC_PROF_TYPE_SETTINGS+"\t"+key+"\t"+k_type+"\t"+k_val;
		if (encrypt_required) {
			String enc = Base64Compat.encodeToString(
						EncryptUtil.encrypt(k_str,cp), 
						Base64Compat.NO_WRAP);
			pw.println(CURRENT_SMBSYNC_PROFILE_VERSION+enc);
		} else {
			pw.println(CURRENT_SMBSYNC_PROFILE_VERSION+k_str);
		}
	};
	static private void saveSettingsParmsToFileBoolean(Context c, String group, PrintWriter pw, boolean dflt,
			boolean encrypt_required, final CipherParms cp, String key) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		String k_type;
		boolean k_val;

		k_val=prefs.getBoolean(key, dflt);
		k_type=SMBSYNC_SETTINGS_TYPE_BOOLEAN;
		String k_str=group+"\t"+
				SMBSYNC_PROF_TYPE_SETTINGS+"\t"+key+"\t"+k_type+"\t"+k_val;
		if (encrypt_required) {
			String enc = Base64Compat.encodeToString(
						EncryptUtil.encrypt(k_str, cp), 
						Base64Compat.NO_WRAP);
			pw.println(CURRENT_SMBSYNC_PROFILE_VERSION+enc);
		} else {
			pw.println(CURRENT_SMBSYNC_PROFILE_VERSION+k_str);
		}
	};
	
	private static void saveSettingsParmsToFile(Context c, 
			PrintWriter pw, boolean encrypt_required,
			final CipherParms cp) {
		String group="Default";// 12Bytes
		saveSettingsParmsToFileString(c, group, pw, "0",    encrypt_required,cp,c.getString(R.string.settings_network_wifi_option));
		saveSettingsParmsToFileBoolean(c, group, pw, false, encrypt_required,cp,c.getString(R.string.settings_auto_start));
		saveSettingsParmsToFileBoolean(c, group, pw, false, encrypt_required,cp,c.getString(R.string.settings_auto_term));
		saveSettingsParmsToFileBoolean(c, group, pw, false, encrypt_required,cp,c.getString(R.string.settings_backgroound_execution));
		saveSettingsParmsToFileString(c, group, pw, "0",    encrypt_required,cp,c.getString(R.string.settings_background_termination_notification));
		saveSettingsParmsToFileBoolean(c, group, pw, false, encrypt_required,cp,c.getString(R.string.settings_error_option));
		saveSettingsParmsToFileString(c, group, pw, GlobalParameters.KEEP_SCREEN_ON_WHEN_SCREEN_UNLOCKED, 
				encrypt_required,cp,c.getString(R.string.settings_keep_screen_on));
		saveSettingsParmsToFileBoolean(c, group, pw, false, encrypt_required,cp,c.getString(R.string.settings_wifi_lock));
		saveSettingsParmsToFileBoolean(c, group, pw, false, encrypt_required,cp,c.getString(R.string.settings_remote_file_copy_by_rename));
		saveSettingsParmsToFileBoolean(c, group, pw, false, encrypt_required,cp,c.getString(R.string.settings_local_file_copy_by_rename));

		saveSettingsParmsToFileBoolean(c, group, pw, false, encrypt_required,cp,c.getString(R.string.settings_debug_msg_diplay));
		saveSettingsParmsToFileString(c, group, pw, "0",    encrypt_required,cp,c.getString(R.string.settings_log_option));
		saveSettingsParmsToFileString(c, group, pw, "0",    encrypt_required,cp,c.getString(R.string.settings_log_level));
		saveSettingsParmsToFileString(c, group, pw, "",     encrypt_required,cp,c.getString(R.string.settings_log_dir));
		saveSettingsParmsToFileString(c, group, pw, "10",   encrypt_required,cp,c.getString(R.string.settings_log_generation));
		saveSettingsParmsToFileBoolean(c, group, pw, false, encrypt_required,cp,c.getString(R.string.settings_suppress_warning_mixed_mp));
		saveSettingsParmsToFileString(c, group, pw, "0", encrypt_required,cp,c.getString(R.string.settings_playback_ringtone_when_sync_ended));
		saveSettingsParmsToFileString(c, group, pw, "0", encrypt_required,cp,c.getString(R.string.settings_vibrate_when_sync_ended));
		saveSettingsParmsToFileBoolean(c, group, pw, false, encrypt_required,cp,c.getString(R.string.settings_show_sync_on_action_bar));
		saveSettingsParmsToFileBoolean(c, group, pw, false, encrypt_required,cp,c.getString(R.string.settings_use_light_theme));

		saveSettingsParmsToFileString(c, group, pw, "0",    encrypt_required,cp,c.getString(R.string.settings_media_store_last_mod_time));
		saveSettingsParmsToFileString(c, group, pw, "3",    encrypt_required,cp,c.getString(R.string.settings_file_diff_time_seconds));
		saveSettingsParmsToFileBoolean(c, group, pw, false, encrypt_required,cp,c.getString(R.string.settings_media_scanner_non_media_files_scan));
		saveSettingsParmsToFileBoolean(c, group, pw, false, encrypt_required,cp,c.getString(R.string.settings_media_scanner_scan_extstg));
		
		saveSettingsParmsToFileBoolean(c, group, pw, false, encrypt_required,cp,c.getString(R.string.settings_exit_clean));
		saveSettingsParmsToFileBoolean(c, group, pw, false, encrypt_required,cp,c.getString(R.string.settings_exported_profile_encryption));
		
		saveSettingsParmsToFileString(c, group, pw, "0",    encrypt_required,cp,c.getString(R.string.settings_smb_lm_compatibility));
		saveSettingsParmsToFileBoolean(c, group, pw, false, encrypt_required,cp,c.getString(R.string.settings_smb_use_extended_security));
		
		saveSettingsParmsToFileString(c, group, pw, "0",    encrypt_required,cp,c.getString(R.string.settings_smb_perform_class));
		
		saveSettingsParmsToFileString(c, group, pw, "0",    encrypt_required,cp,c.getString(R.string.settings_smb_log_level));
		saveSettingsParmsToFileString(c, group, pw, "",     encrypt_required,cp,c.getString(R.string.settings_smb_rcv_buf_size));
		saveSettingsParmsToFileString(c, group, pw, "",     encrypt_required,cp,c.getString(R.string.settings_smb_snd_buf_size));
		saveSettingsParmsToFileString(c, group, pw, "",     encrypt_required,cp,c.getString(R.string.settings_smb_listSize));
		saveSettingsParmsToFileString(c, group, pw, "",     encrypt_required,cp,c.getString(R.string.settings_smb_maxBuffers));
		saveSettingsParmsToFileString(c, group, pw, "",     encrypt_required,cp,c.getString(R.string.settings_io_buffers));
		saveSettingsParmsToFileString(c, group, pw, "false",encrypt_required,cp,c.getString(R.string.settings_smb_tcp_nodelay));
		
    	saveSettingsParmsToFileBoolean(c, group, pw, false,   encrypt_required,cp,SCHEDULER_SCHEDULE_ENABLED_KEY);
    	
    	saveSettingsParmsToFileString(c, group, pw, SCHEDULER_SCHEDULE_TYPE_EVERY_DAY,
    													   encrypt_required,cp,SCHEDULER_SCHEDULE_TYPE_KEY);
    	saveSettingsParmsToFileString(c, group, pw, "00",     encrypt_required,cp,SCHEDULER_SCHEDULE_HOURS_KEY);
    	saveSettingsParmsToFileString(c, group, pw, "00",     encrypt_required,cp,SCHEDULER_SCHEDULE_MINUTES_KEY);
    	saveSettingsParmsToFileString(c, group, pw, "0000000",encrypt_required,cp,SCHEDULER_SCHEDULE_DAY_OF_THE_WEEK_KEY);
    	saveSettingsParmsToFileLong(c, group, pw, -1, encrypt_required,cp,SCHEDULER_SCHEDULE_LAST_EXEC_TIME_KEY);
    	saveSettingsParmsToFileString(c, group, pw, "",       encrypt_required,cp,SCHEDULER_SYNC_PROFILE_KEY);
    	saveSettingsParmsToFileBoolean(c, group, pw, false,   encrypt_required,cp,SCHEDULER_SYNC_OPTION_AUTOSTART_KEY);
    	saveSettingsParmsToFileBoolean(c, group, pw, false,   encrypt_required,cp,SCHEDULER_SYNC_OPTION_AUTOTERM_KEY);
    	saveSettingsParmsToFileBoolean(c, group, pw, false,   encrypt_required,cp,SCHEDULER_SYNC_OPTION_BGEXEC_KEY);
    	saveSettingsParmsToFileBoolean(c, group, pw, false,   encrypt_required,cp,SCHEDULER_SYNC_WIFI_ON_BEFORE_SYNC_START_KEY);
    	saveSettingsParmsToFileBoolean(c, group, pw, false,   encrypt_required,cp,SCHEDULER_SYNC_WIFI_OFF_AFTER_SYNC_END_KEY);
    	saveSettingsParmsToFileString(c, group, pw, SCHEDULER_SYNC_DELAYED_TIME_FOR_WIFI_ON_DEFAULT_VALUE,
    													   	encrypt_required,cp,SCHEDULER_SYNC_DELAYED_TIME_FOR_WIFI_ON_KEY);
    	
		saveSettingsParmsToFileString(c, group, pw, SMBSYNC_PROFILE_CONFIRM_COPY_DELETE_REQUIRED, 
														   	encrypt_required,cp,SMBSYNC_PROFILE_CONFIRM_COPY_DELETE);
		saveSettingsParmsToFileString(c, group, pw, SMBSYNC_PROFILE_2_CONFIRM_COPY_DELETE_REQUIRED, 
				   											encrypt_required,cp,SMBSYNC_PROFILE_2_CONFIRM_COPY_DELETE);
		
		saveSettingsParmsToFileString(c, group, pw, "", encrypt_required,cp,SMBSYNC_USER_LOCAL_MOUNT_POINT_LIST_KEY);
	};

	
	private static String msgs_dir_empty	;
//	private static String msgs_no_profile	;
	private static String msgs_override	;
	private static String msgs_current_dir	;
	private static String msgs_delete_following_profile	;
	private static String msgs_export_prof_title	;
	private static String msgs_export_prof_success;
	private static String msgs_export_prof_fail;
	private static String msgs_import_prof_fail	;
	private static String msgs_import_prof_fail_no_valid_item;
	private static String msgs_audit_msgs_master2	;
	private static String msgs_audit_msgs_password1	;
	private static String msgs_select_export_file	;
	private static String msgs_select_import_file	;
	private static String msgs_select_local_dir	;
	private static String msgs_select_master_profile;
	private static String msgs_select_target_profile;
	private static String msgs_select_remote_share;
	private static String msgs_select_remote_dir;
	private static String msgs_audit_msgs_username1	;

   	private static String msgs_filelist_error;
   	private static String msgs_filelist_cancel;
   	
    private static String msgs_create_profile_not_found;
    private static String msgs_create_profile_error;
    private static String msgs_save_to_profile_error;
    
    private static String msgs_audit_share_not_spec;
    
//    private static String msgs_dlg_hardkey_back_button;
    
	public void loadMsgString() {
		
		msgs_audit_share_not_spec=mContext.getString(R.string.msgs_audit_share_not_spec);
		
		msgs_create_profile_not_found=mContext.getString(R.string.msgs_create_profile_not_found);
	    msgs_create_profile_error=mContext.getString(R.string.msgs_create_profile_error);
	    msgs_save_to_profile_error=mContext.getString(R.string.msgs_save_to_profile_error);
		
		msgs_filelist_cancel=	mContext.getString(R.string.msgs_filelist_cancel);
		msgs_filelist_error=	mContext.getString(R.string.msgs_filelist_error);
		
		msgs_dir_empty					=	mContext.getString(R.string.msgs_dir_empty	);
//		msgs_no_profile					=	mContext.getString(R.string.msgs_no_profile	);
		msgs_override					=	mContext.getString(R.string.msgs_override	);
		msgs_current_dir				=	mContext.getString(R.string.msgs_current_dir	);
		msgs_delete_following_profile	=	mContext.getString(R.string.msgs_delete_following_profile	);
		msgs_export_prof_title			=	mContext.getString(R.string.msgs_export_prof_title	);
		msgs_export_prof_success		=	mContext.getString(R.string.msgs_export_prof_success);
		msgs_export_prof_fail		=	mContext.getString(R.string.msgs_export_prof_fail);
		msgs_import_prof_fail		=	mContext.getString(R.string.msgs_import_prof_fail	);
		msgs_import_prof_fail_no_valid_item=mContext.getString(R.string.msgs_import_prof_fail_no_valid_item);
		msgs_audit_msgs_master2			=	mContext.getString(R.string.msgs_audit_msgs_master2	);
		msgs_audit_msgs_password1		=	mContext.getString(R.string.msgs_audit_msgs_password1	);
		msgs_select_export_file			=	mContext.getString(R.string.msgs_select_export_file	);
		msgs_select_import_file			=	mContext.getString(R.string.msgs_select_import_file	);
		msgs_select_local_dir			=	mContext.getString(R.string.msgs_select_local_dir	);
		msgs_select_master_profile		=	mContext.getString(R.string.msgs_select_master_profile	);
		msgs_select_target_profile		=	mContext.getString(R.string.msgs_select_target_profile	);
		msgs_select_remote_share		=	mContext.getString(R.string.msgs_select_remote_share);
		msgs_select_remote_dir		=	mContext.getString(R.string.msgs_select_remote_dir);
		msgs_audit_msgs_username1		=	mContext.getString(R.string.msgs_audit_msgs_username1	);
	};

	public class FilterAdapterSort implements Comparator<String>{
        @Override
        public int compare(String s1, String s2){
            return s1.compareTo(s2);
        }
    }

}

class PreferenceParmListIItem {
	public String parms_key="";
	public String parms_type="";
	public String parms_value="";
}
