package com.sentaroh.android.SMBSync;

import static com.sentaroh.android.SMBSync.Constants.*;

import java.util.ArrayList;

import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class ProfileMaintSyncFragment extends DialogFragment{
	private final static boolean DEBUG_ENABLE=false;
	private final static String SUB_APPLICATION_TAG="SyncProfile ";

	private Dialog mDialog=null;
	private boolean mTerminateRequired=true;
	private Context mContext=null;
	private ProfileMaintSyncFragment mFragment=null;
	private GlobalParameters mGp=null;
	private ProfileUtility mProfUtil=null;

	public static ProfileMaintSyncFragment newInstance() {
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"newInstance");
		ProfileMaintSyncFragment frag = new ProfileMaintSyncFragment();
        Bundle bundle = new Bundle();
        frag.setArguments(bundle);
        return frag;
    };
    
	public ProfileMaintSyncFragment() {
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"Constructor(Default)");
	};

	@Override
	public void onSaveInstanceState(Bundle outState) {  
		super.onSaveInstanceState(outState);
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"onSaveInstanceState");
		if(outState.isEmpty()){
	        outState.putBoolean("WORKAROUND_FOR_BUG_19917_KEY", true);
	    }
	};  
	
	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
	    // Ignore orientation change to keep activity from restarting
	    super.onConfigurationChanged(newConfig);
	    if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"onConfigurationChanged");

	    reInitViewWidget();
	};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"onCreateView");
    	View view=super.onCreateView(inflater, container, savedInstanceState);
    	CommonDialog.setDlgBoxSizeLimit(mDialog,true);
    	return view;
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"onCreate");
        mContext=this.getActivity();
    	mFragment=this;
    	mGp=(GlobalParameters)getActivity().getApplication();
        if (mTerminateRequired) {
        	this.dismiss();
        }
    };

	@Override
	final public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"onActivityCreated");
	};
	
	@Override
	final public void onAttach(Activity activity) {
	    super.onAttach(activity);
	    if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"onAttach");
	};
	
	@Override
	final public void onDetach() {
	    super.onDetach();
	    if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"onDetach");
	};
	
	@Override
	final public void onStart() {
    	CommonDialog.setDlgBoxSizeLimit(mDialog,true);
	    super.onStart();
	    if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"onStart");
	    if (mTerminateRequired) mDialog.cancel(); 
	};
	
	@Override
	final public void onStop() {
	    super.onStop();
	    if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"onStop");
	};

	@Override
	public void onDestroyView() {
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"onDestroyView");
	    if (getDialog() != null && getRetainInstance())
	        getDialog().setDismissMessage(null);
	    super.onDestroyView();
	};
	
	@Override
	public void onCancel(DialogInterface di) {
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"onCancel");
		if (!mTerminateRequired) {
			final Button btnCancel = (Button) mDialog.findViewById(R.id.edit_profile_sync_btn_cancel);
			btnCancel.performClick();
		}
		mFragment.dismiss();
		super.onCancel(di);
	};
	
	@Override
	public void onDismiss(DialogInterface di) {
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"onDismiss");
		super.onDismiss(di);
	}

	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"onCreateDialog");

//    	mContext=getActivity().getApplicationContext();
    	mDialog=new Dialog(getActivity(), mGp.applicationTheme);
		mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mDialog.setCanceledOnTouchOutside(false);
		mDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		if (!mTerminateRequired) {
			initViewWidget();
		}
        return mDialog;
    };

    class SavedViewContents {
        CharSequence prof_name_et;
        int prof_name_et_spos;
        int prof_name_et_epos;
        boolean cb_active;

        public int sync_opt=-1;
        public boolean sync_mpd, sync_conf, sync_last_mod_java, sync_last_mod_remote,
        	sync_retry, sync_empty_dir, sync_hidden_dir, sync_hidden_file, sync_sub_dir;
        public boolean sync_UseRemoteSmallIoArea;
        public int sync_master_pos=-1, sync_target_pos=-1;
    };

    private SavedViewContents saveViewContents() {
    	SavedViewContents sv=new SavedViewContents();
		
		final EditText editname = (EditText)mDialog.findViewById(R.id.edit_profile_sync_dlg_profile_name);
		final CheckedTextView ctv_active = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_sync_dlg_ctv_active);
		final Spinner spinnerSyncOption=(Spinner)mDialog.findViewById(R.id.edit_profile_sync_dlg_sync_option);
		final CheckedTextView ctvmpd = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_master_root_dir_file);
		final CheckedTextView ctvConf = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_confirm);
		final CheckedTextView ctvLastMod = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_last_modified);
		final CheckedTextView ctvNotUseLastModRem = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_not_use_last_modified_remote_file_for_diff);
		final CheckedTextView ctvRetry = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_retry_if_error_occured);
		final CheckedTextView ctvSyncEmptyDir = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_empty_directory);
		final CheckedTextView ctvSyncHiddenDir = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_hidden_directory);
		final CheckedTextView ctvSyncHiddenFile = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_hidden_file);
		final CheckedTextView ctvSyncSubDir = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_sub_dir);
		final CheckedTextView ctvSyncUseRemoteSmallIoArea = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_use_remote_small_io_area);
		final Spinner spinner_master=(Spinner)mDialog.findViewById(R.id.edit_profile_sync_dlg_master_spinner);
		final Spinner spinner_target=(Spinner)mDialog.findViewById(R.id.edit_profile_sync_dlg_target_spinner);

//		final ScrollView svx=(ScrollView)mDialog.findViewById(R.id.sync_profile_dlg_scroll_view);
//		Log.v("","x="+svx.getScrollX()+", y="+svx.getScrollY());
		
        sv.prof_name_et=editname.getText();
        sv.prof_name_et_spos=editname.getSelectionStart();
        sv.prof_name_et_epos=editname.getSelectionEnd();
        sv.cb_active=ctv_active.isChecked();
        
        sv.sync_opt=spinnerSyncOption.getSelectedItemPosition();
        sv.sync_mpd=ctvmpd.isChecked();
        sv.sync_conf=ctvConf.isChecked();
        sv.sync_last_mod_java=ctvLastMod.isChecked();
        sv.sync_last_mod_remote=ctvNotUseLastModRem.isChecked();
        sv.sync_retry=ctvRetry.isChecked();
        sv.sync_empty_dir=ctvSyncEmptyDir.isChecked();
        sv.sync_hidden_dir=ctvSyncHiddenDir.isChecked();
        sv.sync_hidden_file=ctvSyncHiddenFile.isChecked();
        sv.sync_sub_dir=ctvSyncSubDir.isChecked();
        sv.sync_UseRemoteSmallIoArea = ctvSyncUseRemoteSmallIoArea.isChecked();
        sv.sync_master_pos=spinner_master.getSelectedItemPosition();
        sv.sync_target_pos=spinner_target.getSelectedItemPosition();

//        final Spinner spinnerDateTimeType = (Spinner) mDialog.findViewById(R.id.edit_profile_time_date_time_type);
//        
//        sv.day_of_the_week=getDayOfTheWeekString(mGp,mDialog); 
        return sv;
    };

    private void restoreViewContents(final SavedViewContents sv) {
		final EditText editname = (EditText)mDialog.findViewById(R.id.edit_profile_sync_dlg_profile_name);
		final CheckedTextView ctv_active = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_sync_dlg_ctv_active);
		final Spinner spinnerSyncOption=(Spinner)mDialog.findViewById(R.id.edit_profile_sync_dlg_sync_option);
		final CheckedTextView ctvmpd = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_master_root_dir_file);
		final CheckedTextView ctvConf = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_confirm);
		final CheckedTextView ctvLastMod = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_last_modified);
		final CheckedTextView ctvNotUseLastModRem = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_not_use_last_modified_remote_file_for_diff);
		final CheckedTextView ctvRetry = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_retry_if_error_occured);
		final CheckedTextView ctvSyncEmptyDir = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_empty_directory);
		final CheckedTextView ctvSyncHiddenDir = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_hidden_directory);
		final CheckedTextView ctvSyncHiddenFile = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_hidden_file);
		final CheckedTextView ctvSyncSubDir = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_sub_dir);
		final CheckedTextView ctvSyncUseRemoteSmallIoArea = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_use_remote_small_io_area);
		final Spinner spinner_master=(Spinner)mDialog.findViewById(R.id.edit_profile_sync_dlg_master_spinner);
		final Spinner spinner_target=(Spinner)mDialog.findViewById(R.id.edit_profile_sync_dlg_target_spinner);

    	Handler hndl1=new Handler();
    	hndl1.postDelayed(new Runnable(){
			@Override
			public void run() {
		        editname.setText(sv.prof_name_et);
//		        editname.setSelection(sv.prof_name_et_spos);
//		        editname.getSelectionEnd();
		        ctv_active.setChecked(sv.cb_active);
		        
		        spinnerSyncOption.setEnabled(false);
		        spinnerSyncOption.setSelection(sv.sync_opt);
		        ctvmpd.setChecked(sv.sync_mpd);
		        ctvConf.setChecked(sv.sync_conf);
		        ctvLastMod.setChecked(sv.sync_last_mod_java);
		        ctvNotUseLastModRem.setChecked(sv.sync_last_mod_remote);
		        ctvRetry.setChecked(sv.sync_retry);
		        ctvSyncEmptyDir.setChecked(sv.sync_empty_dir);
		        ctvSyncHiddenDir.setChecked(sv.sync_hidden_dir);
		        ctvSyncHiddenFile.setChecked(sv.sync_hidden_file);
		        ctvSyncSubDir.setChecked(sv.sync_sub_dir);
		        ctvSyncUseRemoteSmallIoArea.setChecked(sv.sync_UseRemoteSmallIoArea);
		        
		        spinner_master.setEnabled(false);
		        spinner_master.setSelection(sv.sync_master_pos);
		        spinner_target.setEnabled(false);
		        spinner_target.setSelection(sv.sync_target_pos);
				
		    	Handler hndl2=new Handler();
		    	hndl2.postDelayed(new Runnable(){
					@Override
					public void run() {
						spinnerSyncOption.setEnabled(true);
				        spinner_master.setEnabled(true);
				        spinner_target.setEnabled(true);
					}
		    	},50);
			}
    	},50);
    };
    
    public void reInitViewWidget() {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"reInitViewWidget");
    	if (!mTerminateRequired) {
    		Handler hndl=new Handler();
    		hndl.post(new Runnable(){
				@Override
				public void run() {
			    	SavedViewContents sv=null;
			    	if (!mOpType.equals("BROWSE")) sv=saveViewContents();
			    	initViewWidget();
			    	if (!mOpType.equals("BROWSE")) restoreViewContents(sv);
			    	CommonDialog.setDlgBoxSizeLimit(mDialog,true);
				}
    		});
    	}
    };
    
    public void initViewWidget() {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"initViewWidget");
		
		if (mOpType.equals("EDIT")) editProfile(mCurrentProfileListItem);
		else if (mOpType.equals("ADD")) addProfile(false, mCurrentProfileListItem);
		else if (mOpType.equals("COPY")) addProfile(true, mCurrentProfileListItem);
		
    };

	private String mOpType="";
	private ProfileListItem mCurrentProfileListItem;
	private SMBSyncUtil mUtil=null;
	private CommonDialog mCommonDlg=null;
	private FragmentManager mFragmentMgr=null;
	private NotifyEvent mNotifyComplete=null;
    public void showDialog(FragmentManager fm, Fragment frag,
    		final String op_type,
			final ProfileListItem pli,
			ProfileUtility pm,
			SMBSyncUtil ut,
			CommonDialog cd,
			NotifyEvent ntfy) {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"showDialog");
    	mTerminateRequired=false;
    	mFragmentMgr=fm;
    	mOpType=op_type;
    	mCurrentProfileListItem=pli;
    	mProfUtil=pm;
    	mUtil=ut;
    	mCommonDlg=cd;
    	mNotifyComplete=ntfy;
	    FragmentTransaction ft = fm.beginTransaction();
	    ft.add(frag,null);
	    ft.commitAllowingStateLoss();
//	    show(fm,APPLICATION_TAG);
    };

    final private void addProfile(boolean copy, final ProfileListItem pfli) {
		mDialog.setContentView(R.layout.edit_profile_sync);

		LinearLayout ll_dlg_view=(LinearLayout) mDialog.findViewById(R.id.edit_profile_sync_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		final LinearLayout title_view=(LinearLayout) mDialog.findViewById(R.id.edit_profile_sync_title_view);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		final TextView dlg_title=(TextView) mDialog.findViewById(R.id.edit_profile_sync_title);
		dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);

		if (!copy) dlg_title.setText(mContext.getString(R.string.msgs_add_sync_profile));
		else dlg_title.setText(mContext.getString(R.string.msgs_copy_sync_profile));
//		dlg_title.setText(mContext.getString(R.string.msgs_copy_sync_profile));
		final TextView dlg_msg=(TextView) mDialog.findViewById(R.id.edit_profile_sync_msg);
		final TextView dlg_file_filter=(TextView) mDialog.findViewById(R.id.sync_profile_file_filter);
		final TextView dlg_dir_filter=(TextView) mDialog.findViewById(R.id.sync_profile_dir_filter);
		final EditText editname = (EditText)mDialog.findViewById(R.id.edit_profile_sync_dlg_profile_name);
		editname.setText(pfli.getProfileName());
		
		final TextView dlg_title_sub=(TextView) mDialog.findViewById(R.id.edit_profile_sync_title_sub);
		if (pfli.getProfileName().equals("")) dlg_title_sub.setVisibility(TextView.GONE);

		String f_fl="", d_fl="";
		if (pfli.getFileFilter()!=null) {
			String cn="";
			for (int i=0;i<pfli.getFileFilter().size();i++) {
				f_fl+=cn+pfli.getFileFilter().get(i).substring(1,pfli.getFileFilter().get(i).length());
				cn=",";
			}
		} 
		if (f_fl.length()==0) f_fl=mContext.getString(R.string.msgs_filter_list_dlg_not_specified);
		if (pfli.getDirFilter()!=null) {
			String cn="";
			for (int i=0;i<pfli.getDirFilter().size();i++) {
				d_fl+=cn+pfli.getDirFilter().get(i).substring(1,pfli.getDirFilter().get(i).length());
				cn=",";
			}
		}
		if (d_fl.length()==0)  d_fl=mContext.getString(R.string.msgs_filter_list_dlg_not_specified);
		dlg_file_filter.setText(f_fl);
		dlg_dir_filter.setText(d_fl);

		final Spinner spinnerSyncOption=(Spinner)mDialog.findViewById(R.id.edit_profile_sync_dlg_sync_option);
		SMBSyncUtil.setSpinnerBackground(mContext, spinnerSyncOption, mGp.themeIsLight);
		ProfileUtility.setSyncOptionSpinner(mContext, spinnerSyncOption, pfli.getSyncType()); 

		final CheckedTextView ctvmpd = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_master_root_dir_file);
		final CheckedTextView ctvConf = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_confirm);
		SMBSyncUtil.setCheckedTextView(ctvConf);
		ctvConf.setChecked(pfli.isConfirmRequired());
		final CheckedTextView ctvLastMod = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_last_modified);
		SMBSyncUtil.setCheckedTextView(ctvLastMod);
		ctvLastMod.setChecked(pfli.isForceLastModifiedUseSmbsync());
		final CheckedTextView ctvNotUseLastModRem = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_not_use_last_modified_remote_file_for_diff);
		SMBSyncUtil.setCheckedTextView(ctvNotUseLastModRem);
		ctvNotUseLastModRem.setChecked(pfli.isNotUseLastModifiedForRemote());
		
		final CheckedTextView ctvRetry = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_retry_if_error_occured);
		SMBSyncUtil.setCheckedTextView(ctvRetry);
		final CheckedTextView ctvSyncEmptyDir = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_empty_directory);
		SMBSyncUtil.setCheckedTextView(ctvSyncEmptyDir);
		final CheckedTextView ctvSyncHiddenDir = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_hidden_directory);
		SMBSyncUtil.setCheckedTextView(ctvSyncHiddenDir);
		final CheckedTextView ctvSyncHiddenFile = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_hidden_file);
		SMBSyncUtil.setCheckedTextView(ctvSyncHiddenFile);
		final CheckedTextView ctvSyncSubDir = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_sub_dir);
		SMBSyncUtil.setCheckedTextView(ctvSyncSubDir);
		
		final CheckedTextView ctvSyncUseRemoteSmallIoArea = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_use_remote_small_io_area);
		SMBSyncUtil.setCheckedTextView(ctvSyncUseRemoteSmallIoArea);

		if (pfli.getRetryCount()==null || pfli.getRetryCount().equals("0")) ctvRetry.setChecked(false);
		else ctvRetry.setChecked(true);
		
		ctvSyncEmptyDir.setChecked(pfli.isSyncEmptyDirectory());
		ctvSyncHiddenDir.setChecked(pfli.isSyncHiddenDirectory());
		ctvSyncHiddenFile.setChecked(pfli.isSyncHiddenDirectory());

		if (pfli.isMasterDirFileProcess()) {
			ctvmpd.setChecked(true);
			ctvSyncSubDir.setChecked(pfli.isSyncSubDirectory());
		} else {
			ctvmpd.setChecked(false);
			ctvSyncSubDir.setChecked(true);
		}

		ctvmpd.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctvmpd.toggle();
				boolean isChecked=ctvmpd.isChecked();
				if (!isChecked) {
					ctvSyncSubDir.setEnabled(false);
					ctvSyncSubDir.setChecked(true);
				} else {
					ctvSyncSubDir.setEnabled(true);
				}
			}
		});
		
		ctvSyncUseRemoteSmallIoArea.setChecked(pfli.isSyncUseRemoteSmallIoArea());
		ctvSyncUseRemoteSmallIoArea.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctvmpd.toggle();
				boolean isChecked=!ctvSyncUseRemoteSmallIoArea.isChecked();
				ctvSyncUseRemoteSmallIoArea.setChecked(isChecked);
			}
		});
		
		
		CommonDialog.setDlgBoxSizeLimit(mDialog,true);

		final CheckedTextView ctv_active = (CheckedTextView)mDialog.findViewById(R.id.edit_profile_sync_dlg_ctv_active);
		SMBSyncUtil.setCheckedTextView(ctv_active);
		if (pfli.isProfileActive()) ctv_active.setChecked(true);
			else ctv_active.setChecked(false);

		
		final Spinner spinner_master=(Spinner)mDialog.findViewById(R.id.edit_profile_sync_dlg_master_spinner);
		SMBSyncUtil.setSpinnerBackground(mContext, spinner_master, mGp.themeIsLight);
		final Spinner spinner_target=(Spinner)mDialog.findViewById(R.id.edit_profile_sync_dlg_target_spinner);
		SMBSyncUtil.setSpinnerBackground(mContext, spinner_target, mGp.themeIsLight);
		ProfileUtility.setSyncMasterProfileSpinner(mGp, getActivity(), spinner_master,pfli.getMasterName());
		if (spinner_master.getCount()>0) {
			if (pfli.getTargetName().equals("")) ProfileUtility.setSyncTargetProfileSpinner(mGp, mContext, spinner_target,spinner_master.getSelectedItem().toString().substring(2),"");
			else ProfileUtility.setSyncTargetProfileSpinner(mGp, mContext, spinner_target,pfli.getMasterName(),pfli.getTargetName());
		} else {
			ProfileUtility.setSyncTargetProfileSpinner(mGp, mContext, spinner_target,"","");
		}
		
		final Button swap_master_target = (Button)mDialog.findViewById(R.id.edit_profile_sync_dlg_change_master_and_target_btn);
		if (spinner_master.getCount()>0 && spinner_target.getCount()>0) swap_master_target.setEnabled(true);
		else swap_master_target.setEnabled(false);
		swap_master_target.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				String mst=spinner_target.getSelectedItem().toString().substring(2);
				String tgt=spinner_master.getSelectedItem().toString().substring(2);;
				ProfileUtility.setSyncMasterProfileSpinner(mGp, mContext, spinner_master,mst);
				ProfileUtility.setSyncTargetProfileSpinner(mGp, mContext, spinner_target,mst,tgt);
				setMasterProfileEditButtonListener(mDialog, mst);
				setTargetProfileEditButtonListener(mDialog, tgt);
			}
		});

//		Log.v("","add main sp_m="+spinner_master.getSelectedItem()+", sp_t="+spinner_target.getSelectedItem());
		
		final ImageButton ib_edit_master = (ImageButton)mDialog.findViewById(R.id.edit_profile_sync_dlg_edit_master_btn);
		final ImageButton ib_edit_target = (ImageButton)mDialog.findViewById(R.id.edit_profile_sync_dlg_edit_target_btn);
		
		spinner_master.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				String c_mst="", c_tgt="";
				if (spinner_target.getSelectedItem()!=null) 
					c_tgt=spinner_target.getSelectedItem().toString().substring(2);
				if (spinner_master.getSelectedItem()!=null) 
					c_mst=spinner_master.getSelectedItem().toString().substring(2);
				ProfileUtility.setSyncTargetProfileSpinner(mGp, mContext, spinner_target,c_mst,c_tgt);
//				Log.v("","c_mst="+c_mst+", c_tgt="+c_tgt);
				setMasterProfileEditButtonListener(mDialog, c_mst);
				setTargetProfileEditButtonListener(mDialog, c_tgt);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		ib_edit_master.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (spinner_master.getCount()>0) {
					String m_name=spinner_master.getSelectedItem().toString().substring(2);
					ProfileListItem m_pli=ProfileUtility.getProfile(mGp, mContext, SMBSYNC_PROF_GROUP_DEFAULT,m_name);
					if (m_pli!=null) {
						if (m_pli.getProfileType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
							  ProfileMaintLocalFragment pmlp=ProfileMaintLocalFragment.newInstance();
							  pmlp.showDialog(mFragmentMgr, pmlp, "EDIT",m_pli, 0, mProfUtil, mUtil, mCommonDlg,null);

						} else if (m_pli.getProfileType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
							  ProfileMaintRemoteFragment pmrp=ProfileMaintRemoteFragment.newInstance();
							  pmrp.showDialog(mFragmentMgr, pmrp, "EDIT",m_pli, 0, mProfUtil, mUtil, mCommonDlg,null);
						}
					}
				}
			}
		});

		ib_edit_target.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (spinner_target.getCount()>0) {
					String t_name=spinner_target.getSelectedItem().toString().substring(2);;
					ProfileListItem m_pli=ProfileUtility.getProfile(mGp, mContext, SMBSYNC_PROF_GROUP_DEFAULT, t_name);
					if (m_pli!=null) {
						if (m_pli.getProfileType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
							  ProfileMaintRemoteFragment pmrp=ProfileMaintRemoteFragment.newInstance();
							  pmrp.showDialog(mFragmentMgr, pmrp, "EDIT",m_pli, 0, mProfUtil, mUtil, mCommonDlg,null);
						} else if (m_pli.getProfileType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
							  ProfileMaintLocalFragment pmlp=ProfileMaintLocalFragment.newInstance();
							  pmlp.showDialog(mFragmentMgr, pmlp, "EDIT",m_pli, 0, mProfUtil, mUtil, mCommonDlg,null);
						}
					}
				}
			}
		});

		final Button btn_ok = (Button) mDialog.findViewById(R.id.edit_profile_sync_btn_ok);
		btn_ok.setEnabled(false);
		dlg_msg.setText(mContext.getString(R.string.msgs_audit_msgs_profilename2));
		editname.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (!ProfileUtility.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_SYNC, s.toString(), mGp.profileAdapter.getArrayList())) {
					String e_msg=auditSyncProfileField(mDialog);
					if (e_msg.length()!=0) {
						dlg_msg.setText(e_msg);
						btn_ok.setEnabled(false);
					} else {
						dlg_msg.setText("");
						btn_ok.setEnabled(true);
					} 
				} else {
					dlg_msg.setText(mContext.getString(R.string.msgs_duplicate_profile));
					btn_ok.setEnabled(false);
				}
			}
		});
		
		// file filterボタンの指定
		Button file_filter_btn = (Button) mDialog.findViewById(R.id.sync_profile_file_filter_btn);
		file_filter_btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				invokeEditFileFilterDlg(mDialog, pfli.getFileFilter());
			}
		});
		// directory filterボタンの指定
		Button dir_filter_btn = (Button) mDialog.findViewById(R.id.sync_profile_dir_filter_btn);
		if (spinner_master.getCount()>0) dir_filter_btn.setEnabled(true);
		else dir_filter_btn.setEnabled(false);
		dir_filter_btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mProfUtil.invokeEditDirFilterDlg(mDialog, pfli.getDirFilter());
			}
		});
		
		// CANCELボタンの指定
		final Button btn_cancel = (Button) mDialog.findViewById(R.id.edit_profile_sync_btn_cancel);
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mFragment.dismiss();
				if (mNotifyComplete!=null) mNotifyComplete.notifyToListener(false, null);
//				glblParms.profileListView.setSelectionFromTop(currentViewPosX,currentViewPosY);
			}
		});
		// Cancelリスナーの指定
		mDialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});

		// OKボタンの指定
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (saveSyncProfile(mDialog, pfli.getFileFilter(), pfli.getDirFilter(), pfli, 0)) {
					mFragment.dismiss();
					ProfileUtility.setAllProfileToUnchecked(true, mGp.profileAdapter);
					if (mNotifyComplete!=null) mNotifyComplete.notifyToListener(true, null);
				}
			}
		});
    };
    
	private boolean saveSyncProfile(Dialog dialog,
			final ArrayList<String> prof_file_filter, 
			final ArrayList<String> prof_dir_filter,
			final ProfileListItem prev_pfli,
			int prof_pos) {
		
//		final TextView dlg_title=(TextView) dialog.findViewById(R.id.sync_profile_dlg_title);
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.edit_profile_sync_msg);
//		final TextView dlg_file_filter=(TextView) dialog.findViewById(R.id.sync_profile_file_filter);
//		final TextView dlg_dir_filter=(TextView) dialog.findViewById(R.id.sync_profile_dir_filter);
		final EditText editname = (EditText)dialog.findViewById(R.id.edit_profile_sync_dlg_profile_name);
		final Spinner spinnerSyncOption=(Spinner)dialog.findViewById(R.id.edit_profile_sync_dlg_sync_option);
		final CheckedTextView ctvmpd = (CheckedTextView)dialog.findViewById(R.id.sync_profile_ctv_sync_master_root_dir_file);
		final CheckedTextView ctvConf = (CheckedTextView)dialog.findViewById(R.id.sync_profile_ctv_confirm);
		final CheckedTextView ctvLastMod = (CheckedTextView)dialog.findViewById(R.id.sync_profile_ctv_last_modified);
		final CheckedTextView ctvNotUseLastModRem = (CheckedTextView)dialog.findViewById(R.id.sync_profile_ctv_not_use_last_modified_remote_file_for_diff);
		final CheckedTextView ctvRetry = (CheckedTextView)dialog.findViewById(R.id.sync_profile_ctv_retry_if_error_occured);
		final CheckedTextView ctvSyncEmptyDir = (CheckedTextView)dialog.findViewById(R.id.sync_profile_ctv_sync_empty_directory);
		final CheckedTextView ctvSyncHiddenDir = (CheckedTextView)dialog.findViewById(R.id.sync_profile_ctv_sync_hidden_directory);
		final CheckedTextView ctvSyncHiddenFile = (CheckedTextView)dialog.findViewById(R.id.sync_profile_ctv_sync_hidden_file);
		final CheckedTextView ctvSyncSubDir = (CheckedTextView)dialog.findViewById(R.id.sync_profile_ctv_sync_sub_dir);
		final CheckedTextView ctvSyncUseRemoteSmallIoArea = (CheckedTextView)dialog.findViewById(R.id.sync_profile_ctv_sync_use_remote_small_io_area);
		final CheckedTextView ctv_active = (CheckedTextView)dialog.findViewById(R.id.edit_profile_sync_dlg_ctv_active);
		final Spinner spinner_master=(Spinner)dialog.findViewById(R.id.edit_profile_sync_dlg_master_spinner);
		final Spinner spinner_target=(Spinner)dialog.findViewById(R.id.edit_profile_sync_dlg_target_spinner);
//		final Button swap_master_target = (Button)dialog.findViewById(R.id.sync_profile_edit_change_master_and_target);
//		final ImageButton ib_edit_master = (ImageButton)dialog.findViewById(R.id.sync_profile_edit_master);
//		final ImageButton ib_edit_target = (ImageButton)dialog.findViewById(R.id.sync_profile_edit_target);
//		final Button btn_ok = (Button) dialog.findViewById(R.id.sync_profile_ok);
//		Button file_filter_btn = (Button) dialog.findViewById(R.id.sync_profile_file_filter_btn);
//		Button dir_filter_btn = (Button) dialog.findViewById(R.id.sync_profile_dir_filter_btn);
//		final Button btn_cancel = (Button) dialog.findViewById(R.id.sync_profile_cancel);
		
		String prof_name, prof_act, prof_master, prof_target, prof_syncopt = null;
		boolean profile_saved = true;
		String audit_msg="";
		
		prof_master = spinner_master.getSelectedItem().toString().substring(2);
		prof_target = spinner_target.getSelectedItem().toString().substring(2);
		prof_name = editname.getText().toString();

		String e_msg=auditSyncProfileField(dialog);
		if (e_msg.length()!=0) {
			audit_msg=e_msg;
			profile_saved=false;
		} else if (e_msg.length()==0 && prof_master.equals(prof_target)) {
				profile_saved=false;
				audit_msg=mContext.getString(R.string.msgs_audit_msgs_master_target);
		} 

		if (ctv_active.isChecked()) prof_act = SMBSYNC_PROF_ACTIVE;
			else prof_act = SMBSYNC_PROF_INACTIVE;

		int syncopt=spinnerSyncOption.getSelectedItemPosition();
		if (syncopt==0) prof_syncopt = SMBSYNC_SYNC_TYPE_MIRROR;
		else if (syncopt==1) prof_syncopt = SMBSYNC_SYNC_TYPE_COPY;
		else if (syncopt==2) prof_syncopt = SMBSYNC_SYNC_TYPE_MOVE;
		else prof_syncopt = SMBSYNC_SYNC_TYPE_COPY;

		Boolean prof_mpd=true;
//		if (prof_dir_filter.size()>0) prof_mpd=cbmpd.isChecked();
		prof_mpd=ctvmpd.isChecked();

		if (!profile_saved) {
			dlg_msg.setText(audit_msg);
		} else {
			if (mGp.profileAdapter.getItem(0).getProfileType().equals(""))
				mGp.profileAdapter.remove(0);
			String m_typ=ProfileUtility.getProfileType(prof_master,mGp.profileAdapter);
			String t_typ=ProfileUtility.getProfileType(prof_target,mGp.profileAdapter);
			String retry_count="0";
			if (ctvRetry.isChecked()) retry_count=SMBSYNC_PROFILE_RETRY_COUNT;

			ProfileUtility.updateSyncProfileAdapter(mGp, prof_name, prof_act,
					prof_syncopt, m_typ,prof_master, t_typ,prof_target,
					prof_file_filter,prof_dir_filter,prof_mpd,
					ctvConf.isChecked(),ctvLastMod.isChecked(),
					ctvNotUseLastModRem.isChecked(),retry_count,
					ctvSyncEmptyDir.isChecked(),
					ctvSyncHiddenDir.isChecked(),ctvSyncHiddenFile.isChecked(),ctvSyncSubDir.isChecked(),ctvSyncUseRemoteSmallIoArea.isChecked(),
					prev_pfli.getSyncZipFileName(), prev_pfli.getSyncZipEncMethod(), prev_pfli.getSyncZipAesKeyLength(),
					prev_pfli.getLastSyncTime(), prev_pfli.getLastSyncResult(),
					false,prof_pos);
			mGp.profileAdapter.sort();
			mGp.profileAdapter.notifyDataSetChanged();
			ProfileUtility.saveProfileToFile(mGp, mContext, mUtil, false,"","",mGp.profileAdapter,false);
		}
		return profile_saved;
	};

	private void setMasterProfileEditButtonListener(Dialog dialog, final String prof_name) {
		final ImageButton ib_edit_master=(ImageButton) dialog.findViewById(R.id.edit_profile_sync_dlg_edit_master_btn);
		final ProfileListItem pli=ProfileUtility.getProfile(mGp, mContext, SMBSYNC_PROF_GROUP_DEFAULT, prof_name);
		if (pli==null) ib_edit_master.setEnabled(false);//setVisibility(ImageButton.GONE);
		else {
			ib_edit_master.setEnabled(true);//.setVisibility(ImageButton.VISIBLE);
			
		}

	};

	private void setTargetProfileEditButtonListener(Dialog dialog, final String prof_name) {
		final ImageButton ib_edit_target=(ImageButton) dialog.findViewById(R.id.edit_profile_sync_dlg_edit_target_btn);
		final ProfileListItem pli=ProfileUtility.getProfile(mGp, mContext, SMBSYNC_PROF_GROUP_DEFAULT, prof_name);
		if (pli==null) ib_edit_target.setEnabled(false);//.setVisibility(ImageButton.GONE);
		else {
			ib_edit_target.setEnabled(true);//.setVisibility(ImageButton.VISIBLE);
		}

	};

	private void invokeEditFileFilterDlg(Dialog dialog,
			final ArrayList<String> n_file_filter) {
		final TextView dlg_file_filter=(TextView) dialog.findViewById(R.id.sync_profile_file_filter);

		NotifyEvent ntfy=new NotifyEvent(mContext);
		//Listen setRemoteShare response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context arg0, Object[] arg1) {
				String f_fl="";
				if (n_file_filter!=null) {
					String cn="";
					for (int i=0;i<n_file_filter.size();i++) {
						f_fl+=cn+n_file_filter.get(i).substring(1,n_file_filter.get(i).length());
						cn=",";
					}
				}
				if (f_fl.length()==0) f_fl=mContext.getString(R.string.msgs_filter_list_dlg_not_specified);
				dlg_file_filter.setText(f_fl);
			}

			@Override
			public void negativeResponse(Context arg0, Object[] arg1) {}
			
		});
		mProfUtil.editFileFilterDlg(n_file_filter,ntfy);

	};
	

	public void editProfile(final ProfileListItem pfli) {
		String f_fl="", d_fl="";
		if (pfli.getFileFilter()!=null) {
			String cn="";
			for (int i=0;i<pfli.getFileFilter().size();i++) {
				f_fl+=cn+pfli.getFileFilter().get(i).substring(1,pfli.getFileFilter().get(i).length());
				cn=",";
			}
		} 
		if (f_fl.length()==0) f_fl=mContext.getString(R.string.msgs_filter_list_dlg_not_specified);
		if (pfli.getDirFilter()!=null) {
			String cn="";
			for (int i=0;i<pfli.getDirFilter().size();i++) {
				d_fl+=cn+pfli.getDirFilter().get(i).substring(1,pfli.getDirFilter().get(i).length());
				cn=",";
			}
		}
		if (d_fl.length()==0)  d_fl=mContext.getString(R.string.msgs_filter_list_dlg_not_specified);
	
		final ArrayList<String> n_file_filter= new ArrayList<String>();
		n_file_filter.addAll(pfli.getFileFilter());
		final ArrayList<String> n_dir_filter=new ArrayList<String>(); 
		n_dir_filter.addAll(pfli.getDirFilter());
	
		// カスタムダイアログの生成
		mDialog.setContentView(R.layout.edit_profile_sync);
		
		LinearLayout ll_dlg_view=(LinearLayout) mDialog.findViewById(R.id.edit_profile_sync_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		final LinearLayout title_view=(LinearLayout) mDialog.findViewById(R.id.edit_profile_sync_title_view);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		final TextView dlg_title=(TextView) mDialog.findViewById(R.id.edit_profile_sync_title);
		dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);

		dlg_title.setText(mContext.getString(R.string.msgs_edit_sync_profile));
		final TextView dlg_title_sub=(TextView) mDialog.findViewById(R.id.edit_profile_sync_title_sub);
		dlg_title_sub.setTextColor(mGp.themeColorList.text_color_dialog_title);
		dlg_title_sub.setText(" ("+pfli.getProfileName()+")");

		
//		final TextView dlg_msg=(TextView) mDialog.findViewById(R.id.sync_profile_dlg_msg);
//		dlg_msg.setText(dialog_msg);
		final TextView dlg_file_filter=(TextView) mDialog.findViewById(R.id.sync_profile_file_filter);
		dlg_file_filter.setText(f_fl);
		final TextView dlg_dir_filter=(TextView) mDialog.findViewById(R.id.sync_profile_dir_filter);
		dlg_dir_filter.setText(d_fl);
	
		final EditText editname = (EditText) mDialog.findViewById(R.id.edit_profile_sync_dlg_profile_name);
		editname.setText(pfli.getProfileName());
		editname.setTextColor(Color.LTGRAY);
		editname.setEnabled(false);
		editname.setVisibility(EditText.GONE);
		
		final CheckedTextView ctvmpd = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_master_root_dir_file);
		final CheckedTextView ctv_active = (CheckedTextView)mDialog.findViewById(R.id.edit_profile_sync_dlg_ctv_active);
		SMBSyncUtil.setCheckedTextView(ctv_active);
		ctv_active.setChecked(pfli.isProfileActive());
		final CheckedTextView ctvConf = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_confirm);
		SMBSyncUtil.setCheckedTextView(ctvConf);
		final CheckedTextView ctvLastMod = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_last_modified);
		SMBSyncUtil.setCheckedTextView(ctvLastMod);
		final CheckedTextView ctvRetry = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_retry_if_error_occured);
		SMBSyncUtil.setCheckedTextView(ctvRetry);
		final CheckedTextView ctvSyncEmptyDir = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_empty_directory);
		SMBSyncUtil.setCheckedTextView(ctvSyncEmptyDir);
		final CheckedTextView ctvSyncHiddenDir = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_hidden_directory);
		SMBSyncUtil.setCheckedTextView(ctvSyncHiddenDir);
		final CheckedTextView ctvSyncHiddenFile = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_hidden_file);
		SMBSyncUtil.setCheckedTextView(ctvSyncHiddenFile);
		final CheckedTextView ctvSyncSubDir = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_sub_dir);
		SMBSyncUtil.setCheckedTextView(ctvSyncSubDir);
		
		final CheckedTextView ctvSyncUseRemoteSmallIoArea = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_sync_use_remote_small_io_area);
		SMBSyncUtil.setCheckedTextView(ctvSyncUseRemoteSmallIoArea);
		
		if (pfli.getRetryCount().equals("0")) ctvRetry.setChecked(false);
		else ctvRetry.setChecked(true);
		
		ctvSyncEmptyDir.setChecked(pfli.isSyncEmptyDirectory());
		ctvSyncHiddenDir.setChecked(pfli.isSyncHiddenDirectory());
		ctvSyncHiddenFile.setChecked(pfli.isSyncHiddenFile());
		
		ctvConf.setChecked(pfli.isConfirmRequired());
		ctvLastMod.setChecked(pfli.isForceLastModifiedUseSmbsync());
		
		final ImageButton ib_edit_master=(ImageButton) mDialog.findViewById(R.id.edit_profile_sync_dlg_edit_master_btn);
		final ImageButton ib_edit_target=(ImageButton) mDialog.findViewById(R.id.edit_profile_sync_dlg_edit_target_btn);
	
		final Spinner spinnerSyncOption=(Spinner)mDialog.findViewById(R.id.edit_profile_sync_dlg_sync_option);
		SMBSyncUtil.setSpinnerBackground(mContext, spinnerSyncOption, mGp.themeIsLight);
		mProfUtil.setSyncOptionSpinner(spinnerSyncOption, pfli.getSyncType()); 
	
		CommonDialog.setDlgBoxSizeLimit(mDialog,true);
	
	//	if (prof_dir_filter.size()!=0) {
	//		cbmpd.setVisibility(CheckBox.VISIBLE);//.setEnabled(true);
	//		cbmpd.setChecked(true);
	//	} else cbmpd.setVisibility(CheckBox.GONE);//.setEnabled(false);
	
		final CheckedTextView ctvNotUseLastModRem = (CheckedTextView)mDialog.findViewById(R.id.sync_profile_ctv_not_use_last_modified_remote_file_for_diff);
		SMBSyncUtil.setCheckedTextView(ctvNotUseLastModRem);
		ctvNotUseLastModRem.setChecked(pfli.isNotUseLastModifiedForRemote());
	
	//	if (cbmpd.isChecked()) cbmpd.setText(mContext.getString(R.string.msgs_sync_profile_master_dir_cb_enable));
	//	else cbmpd.setText(mContext.getString(R.string.msgs_sync_profile_master_dir_cb_disable));
	//	cbmpd.setOnCheckedChangeListener(new OnCheckedChangeListener(){
	//		@Override
	//		public void onCheckedChanged(CompoundButton buttonView,
	//				boolean isChecked) {
	//			if (isChecked) cbmpd.setText(mContext.getString(R.string.msgs_sync_profile_master_dir_cb_enable));
	//			else cbmpd.setText(mContext.getString(R.string.msgs_sync_profile_master_dir_cb_disable));
	//		}
	//	});
	
		final Spinner spinner_master=(Spinner)mDialog.findViewById(R.id.edit_profile_sync_dlg_master_spinner);
		SMBSyncUtil.setSpinnerBackground(mContext, spinner_master, mGp.themeIsLight);
		final Spinner spinner_target=(Spinner)mDialog.findViewById(R.id.edit_profile_sync_dlg_target_spinner);
		SMBSyncUtil.setSpinnerBackground(mContext, spinner_target, mGp.themeIsLight);
		ProfileUtility.setSyncMasterProfileSpinner(mGp, mContext, spinner_master,pfli.getMasterName());
		ProfileUtility.setSyncTargetProfileSpinner(mGp, mContext, spinner_target,pfli.getMasterName(),pfli.getTargetName());
		
		spinner_master.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				String c_mst="", c_tgt="";
				if (spinner_target.getSelectedItem()!=null) 
					c_tgt=spinner_target.getSelectedItem().toString().substring(2);
				if (spinner_master.getSelectedItem()!=null) 
					c_mst=spinner_master.getSelectedItem().toString().substring(2);
				ProfileUtility.setSyncTargetProfileSpinner(mGp, mContext, spinner_target,c_mst,c_tgt);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
	
	//	spinner_target.setOnItemSelectedListener(new OnItemSelectedListener(){
	//		@Override
	//		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
	//			String c_mst="", c_tgt="";
	//			if (spinner_target.getSelectedItem()!=null) 
	//				c_tgt=spinner_target.getSelectedItem().toString().substring(2);
	//			if (spinner_master.getSelectedItem()!=null) 
	//				c_mst=spinner_master.getSelectedItem().toString().substring(2);
	//		}
	//		@Override
	//		public void onNothingSelected(AdapterView<?> arg0) {}
	//	});
	
		setMasterProfileEditButtonListener(mDialog, pfli.getMasterName());
		setTargetProfileEditButtonListener(mDialog, pfli.getTargetName());
		
		ib_edit_master.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				String m_name=spinner_master.getSelectedItem().toString().substring(2);
				ProfileListItem m_pli=ProfileUtility.getProfile(mGp, mContext, SMBSYNC_PROF_GROUP_DEFAULT,m_name);
				if (m_pli!=null) {
					if (m_pli.getProfileType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
						  int pos=ProfileUtility.getProfilePos(m_name,mGp.profileAdapter);
						  ProfileMaintRemoteFragment pmp=ProfileMaintRemoteFragment.newInstance();
						  pmp.showDialog(mFragmentMgr, pmp, "EDIT", m_pli, 
								  pos, mProfUtil, mUtil, mCommonDlg,null);
					} else if (m_pli.getProfileType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
						  int pos=ProfileUtility.getProfilePos(m_name,mGp.profileAdapter);
						  ProfileMaintLocalFragment pmp=ProfileMaintLocalFragment.newInstance();
						  pmp.showDialog(mFragmentMgr, pmp, "EDIT", m_pli, 
								  pos, mProfUtil, mUtil, mCommonDlg,null);
					}
				}
			}
			
		});
	
		final Button swap_master_target = (Button)mDialog.findViewById(R.id.edit_profile_sync_dlg_change_master_and_target_btn);
		if (spinner_master.getCount()>0 && spinner_target.getCount()>0) swap_master_target.setEnabled(true);
		else swap_master_target.setEnabled(false);
		swap_master_target .setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				String mst=spinner_target.getSelectedItem().toString().substring(2);
				String tgt=spinner_master.getSelectedItem().toString().substring(2);;
				ProfileUtility.setSyncMasterProfileSpinner(mGp, mContext, spinner_master,mst);
				ProfileUtility.setSyncTargetProfileSpinner(mGp, mContext, spinner_target,mst,tgt);
				setMasterProfileEditButtonListener(mDialog, mst);
				setTargetProfileEditButtonListener(mDialog, tgt);
			}
		});
	
		ib_edit_target.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				String t_name=spinner_target.getSelectedItem().toString().substring(2);;
				ProfileListItem m_pli=
						ProfileUtility.getProfile(mGp, mContext, SMBSYNC_PROF_GROUP_DEFAULT, t_name);
				if (m_pli!=null) {
					if (m_pli.getProfileType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
						  int pos=ProfileUtility.getProfilePos(t_name,mGp.profileAdapter);
						  ProfileMaintRemoteFragment pmp=ProfileMaintRemoteFragment.newInstance();
						  pmp.showDialog(mFragmentMgr, pmp, "EDIT", m_pli, 
								  pos, mProfUtil, mUtil, mCommonDlg,null);
					} else if (m_pli.getProfileType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
						  int pos=ProfileUtility.getProfilePos(t_name,mGp.profileAdapter);
						  ProfileMaintLocalFragment pmp=ProfileMaintLocalFragment.newInstance();
						  pmp.showDialog(mFragmentMgr, pmp, "EDIT", m_pli, 
								  pos, mProfUtil, mUtil, mCommonDlg,null);
					}
				}
			}
		});
	
		
		// file filterボタンの指定
		Button file_filter_btn = (Button) mDialog.findViewById(R.id.sync_profile_file_filter_btn);
		file_filter_btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				invokeEditFileFilterDlg(mDialog, n_file_filter);
			}
		});
		// directory filterボタンの指定
		Button dir_filter_btn = (Button) mDialog.findViewById(R.id.sync_profile_dir_filter_btn);
		if (spinner_master.getCount()>0) dir_filter_btn.setEnabled(true);
		else dir_filter_btn.setEnabled(false);
		dir_filter_btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mProfUtil.invokeEditDirFilterDlg(mDialog, n_dir_filter);
			}
		});
		// Master Dir processボタンの指定
	//	if (prof_dir_filter.size()!=0) cbmpd.setVisibility(CheckBoCheckedTextViewE);//.setEnabled(true);
	//		else cbmpd.setVisibility(CheckBox.GONE);//.setEnabled(false);
		if (pfli.isMasterDirFileProcess()) {
			ctvmpd.setChecked(true);
			ctvSyncSubDir.setChecked(pfli.isSyncSubDirectory());
		} else {
			ctvmpd.setChecked(false);
			ctvSyncSubDir.setChecked(true);
			ctvSyncSubDir.setEnabled(false);
		}
	
		ctvmpd.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctvmpd.toggle();
				boolean isChecked=ctvmpd.isChecked();
				if (!isChecked) {
					ctvSyncSubDir.setEnabled(false);
					ctvSyncSubDir.setChecked(true);
				} else {
					ctvSyncSubDir.setEnabled(true);
				}
			}
		});

		ctvSyncUseRemoteSmallIoArea.setChecked(pfli.isSyncUseRemoteSmallIoArea());
		ctvSyncUseRemoteSmallIoArea.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				boolean isChecked=!ctvSyncUseRemoteSmallIoArea.isChecked();
				ctvSyncUseRemoteSmallIoArea.setChecked(isChecked);
			}
		});

		// CANCELボタンの指定
		final Button btn_cancel = (Button) mDialog.findViewById(R.id.edit_profile_sync_btn_cancel);
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mFragment.dismiss();
				if (mNotifyComplete!=null) mNotifyComplete.notifyToListener(false, null);
	//			glblParms.profileListView.setSelectionFromTop(currentViewPosX,currentViewPosY);
			}
		});
		// Cancelリスナーの指定
		mDialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
		// OKボタンの指定
		Button btn_ok = (Button) mDialog.findViewById(R.id.edit_profile_sync_btn_ok);
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				int prof_num=ProfileUtility.getProfilePos(pfli.getProfileName(), mGp.profileAdapter);
				if (saveSyncProfile(mDialog, n_file_filter, n_dir_filter, pfli, prof_num)) {
					mFragment.dismiss();
					if (mNotifyComplete!=null) mNotifyComplete.notifyToListener(true, null);
				}
			}
		});
	};

	private String auditSyncProfileField(Dialog dialog) {
		String prof_name, prof_master="", prof_target="";
		boolean audit_error=false;
		String audit_msg="";
		Spinner spinner_master=(Spinner)dialog.findViewById(R.id.edit_profile_sync_dlg_master_spinner);
		Spinner spinner_target=(Spinner)dialog.findViewById(R.id.edit_profile_sync_dlg_target_spinner);
		EditText editname = (EditText) dialog.findViewById(R.id.edit_profile_sync_dlg_profile_name);
		CheckedTextView ctv_active = (CheckedTextView)dialog.findViewById(R.id.edit_profile_sync_dlg_ctv_active);
		
		if (spinner_master.getCount()>0) 
			prof_master = spinner_master.getSelectedItem().toString().substring(2);
		if (spinner_target.getCount()>0) 
			prof_target = spinner_target.getSelectedItem().toString().substring(2);
		prof_name = editname.getText().toString();

		if (mProfUtil.hasInvalidChar(prof_name,new String[]{"\t"})) {
			audit_error=true;
			prof_name=mProfUtil.removeInvalidChar(prof_name);
			audit_msg=String.format(
					mContext.getString(R.string.msgs_audit_msgs_profilename1),mProfUtil.getInvalidCharMsg());
			editname.setText(prof_name);
			editname.requestFocus();
		}  else if (prof_name.length()==0) {
			audit_error=true;
			audit_msg=mContext.getString(R.string.msgs_audit_msgs_profilename2);
			editname.requestFocus();
		}

		if (!audit_error) {
			if (mProfUtil.hasInvalidChar(prof_master,new String[]{"\t"})) {
				audit_error=true;
				prof_master=mProfUtil.removeInvalidChar(prof_master);
				audit_msg=String.format(mContext.getString(R.string.msgs_audit_msgs_master1),mProfUtil.getInvalidCharMsg());
			} else if (prof_master.length()==0) {
				audit_error=true;
				audit_msg=mContext.getString(R.string.msgs_audit_msgs_master2);
			} else if (!mProfUtil.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,
					 SMBSYNC_PROF_TYPE_SYNC, prof_master)) {
				audit_msg= String.format(
						mContext.getString(R.string.msgs_master_profile_not_found), 
						prof_master);
				audit_error=true;
			} else if (ctv_active.isChecked() && !ProfileUtility.isProfileActive(mGp, SMBSYNC_PROF_GROUP_DEFAULT,
					ProfileUtility.getProfileType(prof_master,mGp.profileAdapter), prof_master)) {
				audit_msg= 
						mContext.getString(R.string.msgs_prof_active_not_activated);
				audit_error=true;
			}
		}
		
		if (!audit_error) {
			if (mProfUtil.hasInvalidChar(prof_target,new String[]{"\t"})) {
				audit_error=true;
				prof_target=mProfUtil.removeInvalidChar(prof_target);
				audit_msg=String.format(
						mContext.getString(R.string.msgs_audit_msgs_target1),mProfUtil.getInvalidCharMsg());
			} else if (prof_target.length()==0) {
				audit_error=true;
				audit_msg=mContext.getString(R.string.msgs_audit_msgs_target2);
			} else if (!mProfUtil.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,
					 SMBSYNC_PROF_TYPE_SYNC, prof_target)) {
				audit_msg= String.format(
						mContext.getString(R.string.msgs_target_profile_not_found), 
						prof_target);
				audit_error=true;
			} else if (ctv_active.isChecked() && !ProfileUtility.isProfileActive(mGp, SMBSYNC_PROF_GROUP_DEFAULT,
					ProfileUtility.getProfileType(prof_target,mGp.profileAdapter), prof_target)) {
				audit_msg= 
						mContext.getString(R.string.msgs_prof_active_not_activated);
				audit_error=true;
			}

		}
		if (!audit_error) {
			String m_typ=ProfileUtility.getProfileType(prof_master,mGp.profileAdapter);
			String t_typ=ProfileUtility.getProfileType(prof_target,mGp.profileAdapter);
			if (m_typ.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
				if (t_typ.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
					audit_error=true;
					audit_msg=mContext.getString(R.string.msgs_invalid_profile_combination);
				}
			}
		}

		return audit_msg;
	}

}
