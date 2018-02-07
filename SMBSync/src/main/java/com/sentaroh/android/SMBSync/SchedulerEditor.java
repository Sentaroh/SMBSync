package com.sentaroh.android.SMBSync;

import static com.sentaroh.android.SMBSync.Constants.*;
import static com.sentaroh.android.SMBSync.SchedulerConstants.*;

import java.util.ArrayList;
import java.util.Collections;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.sentaroh.android.SMBSync.SchedulerAdapterSyncList.ViewHolder;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenu;
import com.sentaroh.android.Utilities.StringUtil;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.Widget.CustomSpinnerAdapter;

@SuppressWarnings("unused")
public class SchedulerEditor {
	private CommonDialog commonDlg=null;
	
	private GlobalParameters mGp=null;

	private Context mContext=null;
	private AppCompatActivity mActivity=null;
	
	private SMBSyncUtil util=null;
	
	private SchedulerParms mSched=null;
	
	SchedulerEditor (SMBSyncUtil mu, AppCompatActivity a, Context c,  
			CommonDialog cd, CustomContextMenu ccm, GlobalParameters gp) {
		mContext=c;
		mActivity=a;
		mGp=gp;
		util=mu;
		commonDlg=cd;
		mSched=new SchedulerParms();
	};
	
	public void initDialog() {
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.scheduler_main_dlg);
		
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.scheduler_main_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

    	LinearLayout title_view=(LinearLayout)dialog.findViewById(R.id.scheduler_main_dlg_title_view);
    	title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
    	TextView dlg_title=(TextView)dialog.findViewById(R.id.scheduler_main_dlg_title);
    	dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);

		final Button btn_ok = (Button) dialog.findViewById(R.id.scheduler_main_dlg_ok);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.scheduler_main_dlg_cancel);
		final Button btn_edit = (Button) dialog.findViewById(R.id.scheduler_main_dlg_edit_sync_prof);
		final TextView tv_msg=(TextView)dialog.findViewById(R.id.scheduler_main_dlg_msg);
		
		final CheckedTextView ctv_sched_enabled=(CheckedTextView)dialog.findViewById(R.id.scheduler_main_dlg_ctv_enabled);
		ctv_sched_enabled.setTextColor(mGp.themeColorList.text_color_primary);
		SMBSyncUtil.setCheckedTextView(ctv_sched_enabled);
		final Spinner sp_sched_type=(Spinner)dialog.findViewById(R.id.scheduler_main_dlg_date_time_type);
		SMBSyncUtil.setSpinnerBackground(mContext, sp_sched_type, mGp.themeIsLight);
		final Spinner sp_sched_hours=(Spinner)dialog.findViewById(R.id.scheduler_main_dlg_exec_hours);
		SMBSyncUtil.setSpinnerBackground(mContext, sp_sched_hours, mGp.themeIsLight);
		final Spinner sp_sched_minutes=(Spinner)dialog.findViewById(R.id.scheduler_main_dlg_exec_minutes);
		SMBSyncUtil.setSpinnerBackground(mContext, sp_sched_minutes, mGp.themeIsLight);
//		final CheckBox cb_sched_sun=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_sunday);
//		final CheckBox cb_sched_mon=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_monday);
//		final CheckBox cb_sched_tue=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_tuesday);
//		final CheckBox cb_sched_wed=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_wedsday);
//		final CheckBox cb_sched_thu=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_thursday);
//		final CheckBox cb_sched_fri=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_friday);
//		final CheckBox cb_sched_sat=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_satday);
		final CheckedTextView ctv_auto_term=(CheckedTextView)dialog.findViewById(R.id.scheduler_main_dlg_ctv_autoterm);
		ctv_auto_term.setTextColor(mGp.themeColorList.text_color_primary);
		SMBSyncUtil.setCheckedTextView(ctv_auto_term);
		final CheckedTextView ctv_bg_exec=(CheckedTextView)dialog.findViewById(R.id.scheduler_main_dlg_ctv_bgexec);
		ctv_bg_exec.setTextColor(mGp.themeColorList.text_color_primary);
		SMBSyncUtil.setCheckedTextView(ctv_bg_exec);
		final TextView tv_sync_prof=(TextView)dialog.findViewById(R.id.scheduler_main_dlg_sync_prof_list);
//		final LinearLayout ll_sched_dw=(LinearLayout)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week);
//		final LinearLayout ll_sched_hm=(LinearLayout)dialog.findViewById(R.id.scheduler_main_dlg_ll_exec_hm);
//		final LinearLayout ll_sched_hours=(LinearLayout)dialog.findViewById(R.id.scheduler_main_dlg_ll_exec_hour);
//		final LinearLayout ll_sched_minutes=(LinearLayout)dialog.findViewById(R.id.scheduler_main_dlg_ll_exec_minute);
		final CheckedTextView ctv_sync_all_prof=(CheckedTextView)dialog.findViewById(R.id.scheduler_main_dlg_ctv_sync_all_sync_profile);
		ctv_sync_all_prof.setTextColor(mGp.themeColorList.text_color_primary);
		SMBSyncUtil.setCheckedTextView(ctv_sync_all_prof);
		
		final CheckedTextView ctv_wifi_on=(CheckedTextView)dialog.findViewById(R.id.scheduler_main_dlg_ctv_wifi_on);
		ctv_wifi_on.setTextColor(mGp.themeColorList.text_color_primary);
		SMBSyncUtil.setCheckedTextView(ctv_wifi_on);
//		final LinearLayout ll_wifi_on_delay_time_viewx=(LinearLayout)dialog.findViewById(R.id.scheduler_main_dlg_wifi_on_delay_time_view);
		final TextView tv_wifi_on_delay_time=(TextView)dialog.findViewById(R.id.scheduler_main_dlg_wifi_on_delay_time_text);
		final RadioGroup rg_wifi_on_delay_time=(RadioGroup)dialog.findViewById(R.id.scheduler_main_dlg_wifi_on_delay_time_rg);
		final RadioButton rb_wifi_on_delay_1=(RadioButton)dialog.findViewById(R.id.scheduler_main_dlg_wifi_on_delay_time_rg_1);
		final RadioButton rb_wifi_on_delay_2=(RadioButton)dialog.findViewById(R.id.scheduler_main_dlg_wifi_on_delay_time_rg_2);
		final RadioButton rb_wifi_on_delay_3=(RadioButton)dialog.findViewById(R.id.scheduler_main_dlg_wifi_on_delay_time_rg_3);
		final CheckedTextView ctv_wifi_off=(CheckedTextView)dialog.findViewById(R.id.scheduler_main_dlg_ctv_wifi_off);
		ctv_wifi_off.setTextColor(mGp.themeColorList.text_color_primary);
		SMBSyncUtil.setCheckedTextView(ctv_wifi_off);
		
		loadScheduleData();
		
		CommonDialog.setDlgBoxSizeLimit(dialog, true);
//		CommonDialog.setDlgBoxSizeHeightMax(dialog);
		
		setScheduleTypeSpinner(dialog, mSched.scheduleType);
		setScheduleHoursSpinner(dialog, mSched.scheduleHours);
		setScheduleMinutesSpinner(dialog, mSched.scheduleType, mSched.scheduleMinutes);
		setDayOfTheWeekCb(dialog, mSched.scheduleDayOfTheWeek);
		
		setViewVisibility(dialog);
		
		ctv_sched_enabled.setChecked(mSched.scheduleEnabled);
		ctv_auto_term.setChecked(mSched.syncOptionAutoterm);
		ctv_bg_exec.setChecked(mSched.syncOptionBgExec);

		if (mSched.syncProfile.equals("")) {
			ctv_sync_all_prof.setChecked(true);
			btn_edit.setVisibility(Button.GONE);//.setEnabled(false);
			tv_sync_prof.setVisibility(TextView.GONE);//.setEnabled(false);
		} else {
			ctv_sync_all_prof.setChecked(false);
			btn_edit.setVisibility(Button.VISIBLE);//.setEnabled(true);
			tv_sync_prof.setVisibility(TextView.VISIBLE);//.setEnabled(true);
		}
		tv_sync_prof.setText(mSched.syncProfile);
		
		sp_sched_type.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				String sched_type=getScheduleTypeFromPosition(position);
				setScheduleMinutesSpinner(dialog, sched_type, mSched.scheduleMinutes);
				setViewVisibility(dialog);
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		ctv_wifi_on.setChecked(mSched.syncWifiOnBeforeSyncStart);
		
		if (mSched.syncWifiOnBeforeSyncStart) {
			tv_wifi_on_delay_time.setEnabled(true);//.setVisibility(RadioGroup.VISIBLE);
			rb_wifi_on_delay_1.setEnabled(true);
			rb_wifi_on_delay_2.setEnabled(true);
			rb_wifi_on_delay_3.setEnabled(true);
			rg_wifi_on_delay_time.setEnabled(true);//.setVisibility(RadioGroup.VISIBLE);
			ctv_wifi_off.setEnabled(true);//setVisibility(CheckBox.VISIBLE);
		} else {
			tv_wifi_on_delay_time.setEnabled(false);//setVisibility(RadioGroup.GONE);
			rb_wifi_on_delay_1.setEnabled(false);
			rb_wifi_on_delay_2.setEnabled(false);
			rb_wifi_on_delay_3.setEnabled(false);
			rg_wifi_on_delay_time.setEnabled(false);//.setVisibility(RadioGroup.VISIBLE);
			ctv_wifi_off.setEnabled(false);//setVisibility(CheckBox.GONE);
		}
		
		if (mSched.syncStartDelayTimeAfterWifiOn==5) {
			rg_wifi_on_delay_time.check(R.id.scheduler_main_dlg_wifi_on_delay_time_rg_1);
		} else if (mSched.syncStartDelayTimeAfterWifiOn==10) {
			rg_wifi_on_delay_time.check(R.id.scheduler_main_dlg_wifi_on_delay_time_rg_2);
		} else if (mSched.syncStartDelayTimeAfterWifiOn==30) {
			rg_wifi_on_delay_time.check(R.id.scheduler_main_dlg_wifi_on_delay_time_rg_3);
		}
		
		ctv_wifi_off.setChecked(mSched.syncWifiOffAfterSyncEnd);
		
		ctv_wifi_on.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctv_wifi_on.toggle();
				boolean isChecked=ctv_wifi_on.isChecked();
				if (isChecked) {
					tv_wifi_on_delay_time.setEnabled(true);//setVisibility(RadioGroup.VISIBLE);
					rb_wifi_on_delay_1.setEnabled(true);
					rb_wifi_on_delay_2.setEnabled(true);
					rb_wifi_on_delay_3.setEnabled(true);
					rg_wifi_on_delay_time.setEnabled(true);//.setVisibility(RadioGroup.VISIBLE);
					ctv_wifi_off.setEnabled(true);//setVisibility(CheckBox.VISIBLE);
				} else {
					tv_wifi_on_delay_time.setEnabled(false);//setVisibility(RadioGroup.GONE);
					rb_wifi_on_delay_1.setEnabled(false);
					rb_wifi_on_delay_2.setEnabled(false);
					rb_wifi_on_delay_3.setEnabled(false);
					rg_wifi_on_delay_time.setEnabled(false);//.setVisibility(RadioGroup.VISIBLE);
					ctv_wifi_off.setEnabled(false);//setVisibility(CheckBox.GONE);
					ctv_wifi_off.setChecked(false);
				}
			}
		});

		ctv_sync_all_prof.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctv_sync_all_prof.toggle();
				boolean isChecked=ctv_sync_all_prof.isChecked();
				if (isChecked) {
					btn_edit.setVisibility(Button.GONE);//.setEnabled(false);
					tv_sync_prof.setVisibility(TextView.GONE);//.setEnabled(false);
					btn_ok.setEnabled(true);
					tv_msg.setText("");
				} else {
					btn_edit.setVisibility(Button.VISIBLE);//.setEnabled(true);
					tv_sync_prof.setVisibility(TextView.VISIBLE);//.setEnabled(true);
					if (tv_sync_prof.getText().equals("")) {
						btn_ok.setEnabled(false);
						tv_msg.setText(mContext.getString(R.string.msgs_scheduler_edit_sync_prof_list_not_specified));
					} else {
						btn_ok.setEnabled(true);
						tv_msg.setText("");
					}
				}
			}
		});

		btn_edit.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						String prof_list=(String)o[0];
						tv_sync_prof.setText(prof_list);
						if (prof_list.equals("")) {
							btn_ok.setEnabled(false);
							tv_msg.setText(mContext.getString(R.string.msgs_scheduler_edit_sync_prof_list_not_specified));
						} else {
							btn_ok.setEnabled(true);
							tv_msg.setText("");
						}
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
				});
				editSyncProfileList(tv_sync_prof.getText().toString(), ntfy);
			}
		});
		
		btn_ok.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (sp_sched_type.getSelectedItemPosition()==2 && 
						buildDayOfWeekString(dialog).equals("0000000")) {
					tv_msg.setText(mContext.getString(R.string.msgs_scheduler_main_dw_not_selected));
					return;
				}
				
				dialog.dismiss();
		    	mSched.scheduleDayOfTheWeek=buildDayOfWeekString(dialog);
				mSched.scheduleEnabled=ctv_sched_enabled.isChecked();
		    	
		    	if (sp_sched_type.getSelectedItemPosition()==0) mSched.scheduleType=SCHEDULER_SCHEDULE_TYPE_EVERY_HOURS;
		    	else if (sp_sched_type.getSelectedItemPosition()==1) mSched.scheduleType=SCHEDULER_SCHEDULE_TYPE_EVERY_DAY;
		    	else if (sp_sched_type.getSelectedItemPosition()==2) mSched.scheduleType=SCHEDULER_SCHEDULE_TYPE_DAY_OF_THE_WEEK;
		    	else if (sp_sched_type.getSelectedItemPosition()==3) mSched.scheduleType=SCHEDULER_SCHEDULE_TYPE_INTERVAL;
		    	
		    	mSched.scheduleHours=sp_sched_hours.getSelectedItem().toString();
		    	
		    	mSched.scheduleMinutes=sp_sched_minutes.getSelectedItem().toString();
		    	
		    	mSched.scheduleLastExecTime=System.currentTimeMillis();

		    	if (ctv_sync_all_prof.isChecked()) mSched.syncProfile="";
		    	else mSched.syncProfile=tv_sync_prof.getText().toString();
		    	
		    	mSched.syncOptionAutoterm=ctv_auto_term.isChecked();
		    	
		    	mSched.syncOptionBgExec=ctv_bg_exec.isChecked();
		    	
		    	mSched.syncWifiOnBeforeSyncStart=ctv_wifi_on.isChecked();
		    	
		    	if (rg_wifi_on_delay_time.getCheckedRadioButtonId()==R.id.scheduler_main_dlg_wifi_on_delay_time_rg_1) {
		    		mSched.syncStartDelayTimeAfterWifiOn=5;
		    	} else if (rg_wifi_on_delay_time.getCheckedRadioButtonId()==R.id.scheduler_main_dlg_wifi_on_delay_time_rg_2) {
		    		mSched.syncStartDelayTimeAfterWifiOn=10;
		    	} else if (rg_wifi_on_delay_time.getCheckedRadioButtonId()==R.id.scheduler_main_dlg_wifi_on_delay_time_rg_3) {
		    		mSched.syncStartDelayTimeAfterWifiOn=30;
		    	} else {
		    		mSched.syncStartDelayTimeAfterWifiOn=5;
		    	}
		
		    	if (ctv_wifi_on.isChecked()) mSched.syncWifiOffAfterSyncEnd=ctv_wifi_off.isChecked();
		    	else mSched.syncWifiOffAfterSyncEnd=false;

		    	SchedulerUtil.saveScheduleData(mSched, mContext);
		    	
		    	SchedulerUtil.sendTimerRequest(mContext, SCHEDULER_INTENT_SET_TIMER);
		    	
		    	SchedulerUtil.setSchedulerInfo(mGp, mContext, mSched);
			}
		});

		btn_cancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		dialog.setOnCancelListener(new OnCancelListener(){
			@Override
			public void onCancel(DialogInterface dialog) {
				btn_cancel.performClick();
			}
		});

		dialog.show();
	};

	private void editSyncProfileList(final String prof_list, final NotifyEvent p_ntfy) {
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mActivity, mGp.applicationTheme);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.scheduler_edit_synclist_dlg);
		
		LinearLayout ll_dlg_view=(LinearLayout) dialog.findViewById(R.id.scheduler_edit_synclist_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

    	LinearLayout title_view=(LinearLayout)dialog.findViewById(R.id.scheduler_edit_synclist_dlg_title_view);
    	title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
    	TextView dlg_title=(TextView)dialog.findViewById(R.id.scheduler_edit_synclist_dlg_title);
    	dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);

		final CheckedTextView ctv_show_all_prof = (CheckedTextView) dialog.findViewById(R.id.scheduler_edit_synclist_dlg_show_active_prof);
		final Button btn_ok = (Button) dialog.findViewById(R.id.scheduler_edit_synclist_dlg_ok);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.scheduler_edit_synclist_dlg_cancel);
		
		final ListView lv_sync_list=(ListView)dialog.findViewById(R.id.scheduler_edit_synclist_dlg_sync_prof_list);
	
		final SchedulerAdapterSyncList adapter=
				new SchedulerAdapterSyncList(mActivity,android.R.layout.simple_list_item_checked);
		
		btn_ok.setEnabled(setSyncProfListView(true, prof_list, lv_sync_list, adapter));
		
		lv_sync_list.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				boolean sel=false;
				for (int i=0;i<lv_sync_list.getCount();i++) {
					if (lv_sync_list.isItemChecked(i)) {
						sel=true;
						break;
					}
				}
				if (sel) btn_ok.setEnabled(true);
				else btn_ok.setEnabled(false);
			}
		});
		
		ctv_show_all_prof.setChecked(true);
		ctv_show_all_prof.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				((CheckedTextView)v).toggle();
				String n_prof_list="", sep="";
				for (int i=0;i<lv_sync_list.getCount();i++) {
					if (lv_sync_list.isItemChecked(i)) {
						n_prof_list=n_prof_list+sep+adapter.getItem(i).substring(1);
						sep=",";
					}
				}
				btn_ok.setEnabled(setSyncProfListView(((CheckedTextView)v).isChecked(),
						n_prof_list, lv_sync_list, adapter));
			}
		});
		
		btn_ok.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				dialog.dismiss();
//				SparseBooleanArray sba=lv_sync_list.getCheckedItemPositions();
				String n_prof_list="", sep="";
				for (int i=0;i<lv_sync_list.getCount();i++) {
					if (lv_sync_list.isItemChecked(i)) {
						n_prof_list=n_prof_list+sep+adapter.getItem(i).substring(1);
						sep=",";
					}
				}
				p_ntfy.notifyToListener(true, new Object[]{n_prof_list});
			}
		});

		btn_cancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				p_ntfy.notifyToListener(false, null);
			}
		});
		dialog.show();
	};

	private boolean setSyncProfListView(boolean active,
			String prof_list, ListView lv, SchedulerAdapterSyncList adapter) {
		adapter.clear();

		for (int i=0;i<mGp.profileAdapter.getCount();i++) {
			ProfileListItem pfli=mGp.profileAdapter.getItem(i);
			if (pfli.isProfileActive() || !active) {
				if (pfli.getProfileType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
					String act=pfli.getProfileActive();
					adapter.add(act+pfli.getProfileName());
				}
			}
		};

		String[] pfa=null;
		pfa=prof_list.split(",");
		if (!prof_list.equals("")) {
			for (int i=0;i<pfa.length;i++) {
				setSelectedSyncList(pfa[i],lv,adapter);
			}
		};

		lv.setAdapter(adapter);
		lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		lv.setSelected(true);

		if (!prof_list.equals("")) {
			for (int k=0;k<pfa.length;k++) {
				for(int i=0;i<adapter.getCount();i++) {
					String prof_name=adapter.getItem(i).substring(1);
					if (prof_name.equals(pfa[k])) {
						lv.setItemChecked(i, true);
						break;
					}
				}
			}
		};
		
		boolean selected=false;
		for (int i=0;i<adapter.getCount();i++) {
			if (lv.isItemChecked(i)) {
				selected=true;
				break;
			}
		};
		adapter.notifyDataSetChanged();
		return selected;
	};
	
	private void setSelectedSyncList(String sel, ListView lv, SchedulerAdapterSyncList adapter) {
		boolean found=false;
		for(int i=0;i<adapter.getCount();i++) {
			String prof_name=adapter.getItem(i).substring(1);
			if (prof_name.equals(sel)) {
				found=true;
//				lv.setItemChecked(i, true);
				break;
			}
		}
		if (!found && ProfileUtility.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT, 
						SMBSYNC_PROF_TYPE_SYNC, sel, mGp.profileAdapter.getArrayList())) {
			for(int i=0;i<adapter.getCount();i++) {
				String prof_name=adapter.getItem(i).substring(1);
				if (prof_name.compareToIgnoreCase(sel)>0) {
					adapter.insert(SMBSYNC_PROF_INACTIVE+sel,i+1);
					adapter.notifyDataSetChanged();
//					lv.setItemChecked(i+1, true);
					break;
				}
			}
		}
	};
	
	private void setViewVisibility(Dialog dialog) {
		final Spinner sp_sched_type=(Spinner)dialog.findViewById(R.id.scheduler_main_dlg_date_time_type);
//		final Spinner sp_sched_hours=(Spinner)dialog.findViewById(R.id.scheduler_main_dlg_exec_hours);
//		final Spinner sp_sched_minutes=(Spinner)dialog.findViewById(R.id.scheduler_main_dlg_exec_minutes);
		final LinearLayout ll_sched_dw=(LinearLayout)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week);
		final LinearLayout ll_sched_hm=(LinearLayout)dialog.findViewById(R.id.scheduler_main_dlg_ll_exec_hm);
		final LinearLayout ll_sched_hours=(LinearLayout)dialog.findViewById(R.id.scheduler_main_dlg_ll_exec_hour);
		final LinearLayout ll_sched_minutes=(LinearLayout)dialog.findViewById(R.id.scheduler_main_dlg_ll_exec_minute);
		
		if (sp_sched_type.getSelectedItemPosition()<=0) {
			ll_sched_dw.setVisibility(LinearLayout.GONE);
			ll_sched_hm.setVisibility(LinearLayout.VISIBLE);
			ll_sched_hours.setVisibility(LinearLayout.GONE);
			ll_sched_minutes.setVisibility(LinearLayout.VISIBLE);
		} else if (sp_sched_type.getSelectedItemPosition()==1) {
			ll_sched_dw.setVisibility(LinearLayout.GONE);
			ll_sched_hm.setVisibility(LinearLayout.VISIBLE);
			ll_sched_hours.setVisibility(LinearLayout.VISIBLE);
			ll_sched_minutes.setVisibility(LinearLayout.VISIBLE);
		} else if (sp_sched_type.getSelectedItemPosition()==2) {
			ll_sched_dw.setVisibility(LinearLayout.VISIBLE);
			ll_sched_hm.setVisibility(LinearLayout.VISIBLE);
			ll_sched_hours.setVisibility(LinearLayout.VISIBLE);
			ll_sched_minutes.setVisibility(LinearLayout.VISIBLE);
		} else if (sp_sched_type.getSelectedItemPosition()==3) {
			ll_sched_dw.setVisibility(LinearLayout.GONE);
			ll_sched_hm.setVisibility(LinearLayout.VISIBLE);
			ll_sched_hours.setVisibility(LinearLayout.GONE);
			ll_sched_minutes.setVisibility(LinearLayout.VISIBLE);
		}
	};
	
	private String buildDayOfWeekString(Dialog dialog) {
		String sun="0", mon="0", tue="0", wed="0", thu="0", fri="0", sat="0";
		final CheckBox cb_sched_sun=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_sunday);
		final CheckBox cb_sched_mon=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_monday);
		final CheckBox cb_sched_tue=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_tuesday);
		final CheckBox cb_sched_wed=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_wedsday);
		final CheckBox cb_sched_thu=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_thursday);
		final CheckBox cb_sched_fri=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_friday);
		final CheckBox cb_sched_sat=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_satday);
		if (cb_sched_sun.isChecked()) sun="1";
		if (cb_sched_mon.isChecked()) mon="1";
		if (cb_sched_tue.isChecked()) tue="1";
		if (cb_sched_wed.isChecked()) wed="1";
		if (cb_sched_thu.isChecked()) thu="1";
		if (cb_sched_fri.isChecked()) fri="1";
		if (cb_sched_sat.isChecked()) sat="1";
		return sun+mon+tue+wed+thu+fri+sat;
	}
	
	private void setDayOfTheWeekCb(Dialog dialog, String dw) {
		final CheckBox cb_sched_sun=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_sunday);
		final CheckBox cb_sched_mon=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_monday);
		final CheckBox cb_sched_tue=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_tuesday);
		final CheckBox cb_sched_wed=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_wedsday);
		final CheckBox cb_sched_thu=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_thursday);
		final CheckBox cb_sched_fri=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_friday);
		final CheckBox cb_sched_sat=(CheckBox)dialog.findViewById(R.id.scheduler_main_dlg_day_of_the_week_satday);
		if (dw.substring(0,1).equals("1")) cb_sched_sun.setChecked(true);
		else cb_sched_sun.setChecked(false);	
		if (dw.substring(1,2).equals("1")) cb_sched_mon.setChecked(true);
		else cb_sched_mon.setChecked(false);	
		if (dw.substring(2,3).equals("1")) cb_sched_tue.setChecked(true);
		else cb_sched_tue.setChecked(false);	
		if (dw.substring(3,4).equals("1")) cb_sched_wed.setChecked(true);
		else cb_sched_wed.setChecked(false);	
		if (dw.substring(4,5).equals("1")) cb_sched_thu.setChecked(true);
		else cb_sched_thu.setChecked(false);	
		if (dw.substring(5,6).equals("1")) cb_sched_fri.setChecked(true);
		else cb_sched_fri.setChecked(false);	
		if (dw.substring(6,7).equals("1")) cb_sched_sat.setChecked(true);
		else cb_sched_sat.setChecked(false);	
	};
	
	private void setScheduleTypeSpinner(Dialog dialog, String type) {
		final Spinner sp_sched_type=(Spinner)dialog.findViewById(R.id.scheduler_main_dlg_date_time_type);
		
		final CustomSpinnerAdapter adapter=new CustomSpinnerAdapter(mActivity, R.layout.custom_simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp_sched_type.setPrompt(mContext.getString(R.string.msgs_scheduler_main_spinner_sched_type_prompt));
		sp_sched_type.setAdapter(adapter);
		adapter.add(mContext.getString(R.string.msgs_scheduler_main_spinner_sched_type_every_hour));
		adapter.add(mContext.getString(R.string.msgs_scheduler_main_spinner_sched_type_every_day));
		adapter.add(mContext.getString(R.string.msgs_scheduler_main_spinner_sched_type_day_of_week));
		adapter.add(mContext.getString(R.string.msgs_scheduler_main_spinner_sched_type_interval));
		
		if (!type.equals("")) {
			int sel=-1;
			if (type.equals(SCHEDULER_SCHEDULE_TYPE_EVERY_HOURS)) sel=0;
			else if (type.equals(SCHEDULER_SCHEDULE_TYPE_EVERY_DAY)) sel=1;
			else if (type.equals(SCHEDULER_SCHEDULE_TYPE_DAY_OF_THE_WEEK)) sel=2;
			else if (type.equals(SCHEDULER_SCHEDULE_TYPE_INTERVAL)) sel=3;
			sp_sched_type.setSelection(sel);
		}
		adapter.notifyDataSetChanged();
	};
	
	private String getScheduleTypeFromSpinner(Spinner spinner) {
		return getScheduleTypeFromPosition(spinner.getSelectedItemPosition());
	};
	
	private String getScheduleTypeFromPosition(int position) {
		String sched_type="";
		if (position==0) sched_type=SCHEDULER_SCHEDULE_TYPE_EVERY_HOURS;
		else if (position==1) sched_type=SCHEDULER_SCHEDULE_TYPE_EVERY_DAY;
		else if (position==2) sched_type=SCHEDULER_SCHEDULE_TYPE_DAY_OF_THE_WEEK;
		else if (position==3) sched_type=SCHEDULER_SCHEDULE_TYPE_INTERVAL;
		return sched_type;
	};

	private void setScheduleHoursSpinner(Dialog dialog, String hh) {
		final Spinner sp_sched_hours=(Spinner)dialog.findViewById(R.id.scheduler_main_dlg_exec_hours);
		final CustomSpinnerAdapter adapter=new CustomSpinnerAdapter(mActivity, R.layout.custom_simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp_sched_hours.setPrompt(mContext.getString(R.string.msgs_scheduler_main_spinner_sched_hours_prompt));
		sp_sched_hours.setAdapter(adapter);
		
		int sel=-1, s_hh=Integer.parseInt(hh);
		for (int i=0;i<24;i++) {
			if (i>=10) adapter.add(""+i);
			else adapter.add("0"+i);
			if (s_hh==i) sel=i;
		}
		sp_sched_hours.setSelection(sel);
		adapter.notifyDataSetChanged();
	};

	private void setScheduleMinutesSpinner(Dialog dialog, String sched_type, String mm) {
		final Spinner sp_sched_minutes=(Spinner)dialog.findViewById(R.id.scheduler_main_dlg_exec_minutes);
		final CustomSpinnerAdapter adapter=new CustomSpinnerAdapter(mActivity, R.layout.custom_simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp_sched_minutes.setPrompt(mContext.getString(R.string.msgs_scheduler_main_spinner_sched_hours_prompt));
		sp_sched_minutes.setAdapter(adapter);
		
		int sel=-1, s_mm=Integer.parseInt(mm);
		if (sched_type.equals(SCHEDULER_SCHEDULE_TYPE_INTERVAL)) {
			for (int i=5;i<245;i+=5) {
				if (i>=10) adapter.add(""+i);
				else adapter.add("0"+i);
				if (s_mm==i) sel=adapter.getCount()-1;
			}
		} else {
			for (int i=0;i<60;i++) {
				if (i>=10) adapter.add(""+i);
				else adapter.add("0"+i);
				if (s_mm==i) sel=i;
			}
		}
		sp_sched_minutes.setSelection(sel);
		adapter.notifyDataSetChanged();
	};

    private void loadScheduleData() {
    	SchedulerUtil.loadScheduleData(mSched, mContext);

    	util.addDebugLogMsg(1,"I", "loadScheduleData type="+mSched.scheduleType+
    			", hours="+mSched.scheduleHours+
    			", minutes="+mSched.scheduleMinutes+
    			", dw="+mSched.scheduleDayOfTheWeek+
    			", sync_prof="+mSched.syncProfile+
    			", auto_start="+mSched.syncOptionAutostart+
    			", auto_term="+mSched.syncOptionAutoterm+
				", bg_exec="+mSched.syncOptionBgExec+
				", Wifi On="+mSched.syncWifiOnBeforeSyncStart+
				", Wifi Off="+mSched.syncWifiOffAfterSyncEnd+
				", Wifi On dlayed="+mSched.syncStartDelayTimeAfterWifiOn
    			);
    };

}

class SchedulerAdapterSyncList extends ArrayAdapter<String>{
	private int layout_id=0;
	private Context context=null;
	private int text_color=0;
	public SchedulerAdapterSyncList(Context c, int textViewResourceId) {
		super(c, textViewResourceId);
		layout_id=textViewResourceId;
		context=c;
	}
	
	@Override
	public View getView(final int position, View convertView, final ViewGroup parent) {
		final ViewHolder holder;
		final String o = getItem(position);
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(layout_id, null);
            holder=new ViewHolder();
            holder.tv_name=(TextView)v.findViewById(android.R.id.text1);
            text_color=holder.tv_name.getCurrentTextColor();
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        if (o != null) {
        	holder.tv_name.setText(o.substring(1));
        	if (o.substring(0, 1).equals(SMBSYNC_PROF_ACTIVE)) {
        		holder.tv_name.setTextColor(text_color);
        	} else {
        		holder.tv_name.setTextColor(Color.DKGRAY);
        	}
        }
        return v;
	
	}
	class ViewHolder {
		TextView tv_name;
	};
}

