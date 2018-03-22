package com.sentaroh.android.SMBSync;

import static com.sentaroh.android.SMBSync.Constants.*;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_GROUP_DEFAULT;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_INACTIVE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_REMOTE;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.ThreadCtrl;
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
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ProfileMaintRemoteFragment extends DialogFragment{
	private final static boolean DEBUG_ENABLE=false;
	private final static String SUB_APPLICATION_TAG="RemoteProfile ";

	private Dialog mDialog=null;
	private boolean mTerminateRequired=true;
	private Context mContext=null;
	private ProfileMaintRemoteFragment mFragment=null;
	private GlobalParameters mGp=null;
	private ProfileUtility mProfUtil=null;

	public static ProfileMaintRemoteFragment newInstance() {
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"newInstance");
		ProfileMaintRemoteFragment frag = new ProfileMaintRemoteFragment();
        Bundle bundle = new Bundle();
        frag.setArguments(bundle);
        return frag;
    };
    
	public ProfileMaintRemoteFragment() {
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
			final Button btnCancel = (Button) mDialog.findViewById(R.id.edit_profile_remote_btn_cancel);
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

        public boolean remote_use_user_pass, remote_use_port;
        public String remote_user, remote_pass, remote_share, remote_dir, remote_host,
        		remote_port;
    };

    private SavedViewContents saveViewContents() {
    	SavedViewContents sv=new SavedViewContents();
		
		final EditText editname = (EditText)mDialog.findViewById(R.id.edit_profile_remote_dlg_profile_name);
		final CheckedTextView cb_active = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_ctv_active);
		
		final EditText edituser = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_remote_user);
		final EditText editpass = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_remote_pass);
		final EditText editshare = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_share_name);
		final EditText editdir = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_dir);

		final EditText edithost = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_remote_server);
		final CheckedTextView cb_use_user_pass = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_ctv_use_user_pass);
		
		final CheckedTextView cb_use_port_number = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_ctv_use_remote_port_number);
		final EditText editport = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_remote_port);


        sv.prof_name_et=editname.getText();
        sv.prof_name_et_spos=editname.getSelectionStart();
        sv.prof_name_et_epos=editname.getSelectionEnd();
        sv.cb_active=cb_active.isChecked();
        
        sv.remote_use_user_pass=cb_use_user_pass.isChecked();
        sv.remote_use_port=cb_use_port_number.isChecked();
        sv.remote_dir=editdir.getText().toString();
        sv.remote_pass=editpass.getText().toString();
        sv.remote_port=editport.getText().toString();
        sv.remote_share=editshare.getText().toString();
        sv.remote_host=edithost.getText().toString();
        sv.remote_user=edituser.getText().toString();
        
        
//        final Spinner spinnerDateTimeType = (Spinner) mDialog.findViewById(R.id.edit_profile_time_date_time_type);
//        
//        sv.day_of_the_week=getDayOfTheWeekString(mGp,mDialog); 
        return sv;
    };

    private void restoreViewContents(final SavedViewContents sv) {
		final EditText editname = (EditText)mDialog.findViewById(R.id.edit_profile_remote_dlg_profile_name);
		final CheckedTextView cb_active = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_ctv_active);

		final EditText edituser = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_remote_user);
		final EditText editpass = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_remote_pass);
		final EditText editshare = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_share_name);
		final EditText editdir = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_dir);

		final EditText edithost = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_remote_server);
		final CheckedTextView cb_use_user_pass = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_ctv_use_user_pass);
		
		final CheckedTextView cb_use_port_number = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_ctv_use_remote_port_number);
		final EditText editport = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_remote_port);

    	Handler hndl1=new Handler();
    	hndl1.postDelayed(new Runnable(){
			@Override
			public void run() {
		        editname.setText(sv.prof_name_et);
//		        editname.setSelection(sv.prof_name_et_spos);
//		        editname.getSelectionEnd();
		        cb_active.setChecked(sv.cb_active);
		        
		        cb_use_user_pass.setChecked(sv.remote_use_user_pass);
		        cb_use_port_number.setChecked(sv.remote_use_port);
		        editdir.setText(sv.remote_dir);
		        editpass.setText(sv.remote_pass);
		        editport.setText(sv.remote_port);
		        editshare.setText(sv.remote_share);
		        edithost.setText(sv.remote_host);
		        edituser.setText(sv.remote_user);
				
		    	Handler hndl2=new Handler();
		    	hndl2.postDelayed(new Runnable(){
					@Override
					public void run() {
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
	private int mProfileItemPos=-1;
	private SMBSyncUtil mUtil=null;
	private CommonDialog mCommonDlg=null;
	private NotifyEvent mNotifyComplete=null;
    public void showDialog(FragmentManager fm, Fragment frag,
    		final String op_type,
			final ProfileListItem pli,
			int pin,
			ProfileUtility pm,
			SMBSyncUtil ut,
			CommonDialog cd,
			NotifyEvent ntfy) {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"showDialog");
    	mTerminateRequired=false;
    	mOpType=op_type;
    	mCurrentProfileListItem=pli;
    	mProfUtil=pm;
    	mProfileItemPos=pin;
    	mUtil=ut;
    	mCommonDlg=cd;
    	mNotifyComplete=ntfy;
	    FragmentTransaction ft = fm.beginTransaction();
	    ft.add(frag,null);
	    ft.commitAllowingStateLoss();
//	    show(fm,APPLICATION_TAG);
    };

    final private void addProfile(boolean copy, final ProfileListItem pfli) {
		mDialog.setContentView(R.layout.edit_profile_remote);

		LinearLayout ll_dlg_view=(LinearLayout) mDialog.findViewById(R.id.edit_profile_remote_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		final LinearLayout title_view=(LinearLayout) mDialog.findViewById(R.id.edit_profile_remote_dlg_title_view);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		final TextView dlg_title=(TextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_title);
		dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);

		if (!copy) dlg_title.setText(mContext.getString(R.string.msgs_add_remote_profile));
		else dlg_title.setText(mContext.getString(R.string.msgs_copy_remote_profile));
		
		final TextView dlg_msg=(TextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_msg);

		final CheckedTextView ctv_active = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_ctv_active);
		SMBSyncUtil.setCheckedTextView(ctv_active);
		final EditText edituser = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_remote_user);
		final EditText editpass = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_remote_pass);
		final EditText editshare = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_share_name);
		final EditText editdir = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_dir);
		final EditText editname = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_profile_name);

		final EditText edithost = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_remote_server);
		final CheckedTextView ctv_use_user_pass = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_ctv_use_user_pass);
		
		final Button btnAddr = (Button) mDialog.findViewById(R.id.edit_profile_remote_dlg_search_remote_host);
		final Button btnListShare = (Button) mDialog.findViewById(R.id.edit_profile_remote_dlg_list_share_btn);
		final Button btnListDir = (Button) mDialog.findViewById(R.id.edit_profile_remote_dlg_list_directory_btn);

		final CheckedTextView ctv_use_port_number = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_ctv_use_remote_port_number);
		ctv_active.setChecked(true);
		
		final TextView dlg_title_sub=(TextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_title_sub);
		if (pfli.getProfileName().equals("")) dlg_title_sub.setVisibility(TextView.GONE);

		edithost.setVisibility(EditText.VISIBLE);
		if (pfli.getRemoteUserID().equals("") && pfli.getRemotePassword().equals("")) {
			ctv_use_user_pass.setChecked(false);
			edituser.setEnabled(false);
			editpass.setEnabled(false);
		} else {
			ctv_use_user_pass.setChecked(true);
			edituser.setEnabled(true);
			editpass.setEnabled(true);
		}

		
		final EditText editport = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_remote_port);
		if (!pfli.getRemotePort().equals("")) {
			ctv_use_port_number.setChecked(true);
			editport.setText(pfli.getRemotePort());
		} else {
			editport.setText("");
			editport.setEnabled(false);
		}
		
		CommonDialog.setDlgBoxSizeLimit(mDialog,true);
		
		editname.setText(pfli.getProfileName());
		edituser.setText(pfli.getRemoteUserID());
		if (!pfli.getRemoteAddr().equals("")) edithost.setText(pfli.getRemoteAddr()); 
		else edithost.setText(pfli.getRemoteHostname());
		editpass.setText(pfli.getRemotePassword());
		editshare.setText(pfli.getRemoteShareName());
		editdir.setText(pfli.getDirectoryName());
		
		// IpAddressScanボタンの指定
		if (mUtil.isRemoteDisable()) btnAddr.setEnabled(false);
		btnAddr.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mProfUtil.ipAddressScanButtonDlg(mDialog);
			}
		});
		
		if (mUtil.isRemoteDisable()) btnListShare.setEnabled(false);
		btnListShare.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mProfUtil.invokeSelectRemoteShareDlg(mDialog);
			}
		});
		
		if (mUtil.isRemoteDisable()) btnListDir.setEnabled(false);
		btnListDir.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mProfUtil.selectRemoteDirectory(mDialog);
			}
		});
		
		final Button btnLogon = (Button) mDialog.findViewById(R.id.edit_profile_remote_dlg_logon_btn);
		btnLogon.setEnabled(false);
		btnLogon.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String user=null, pass=null;
				if (ctv_use_user_pass.isChecked()) {
					if (edituser.getText().length()>0) user=edituser.getText().toString();
					if (editpass.getText().length()>0) pass=editpass.getText().toString();
				}
				String port="";
				if (ctv_use_port_number.isChecked()) port=editport.getText().toString();
				if (NetworkUtil.isValidIpAddress(edithost.getText().toString())) {
					String t_addr=edithost.getText().toString();
					String s_addr=t_addr;
					if (t_addr.indexOf(":")>=0) s_addr=t_addr.substring(0,t_addr.indexOf(":")) ;
					logonToRemoteDlg("",s_addr, port, user,pass,null);
				} else {
					logonToRemoteDlg(edithost.getText().toString(), "", port, user,pass,null);
				}
			}
		});

		// CANCELボタンの指定
		final Button btn_cancel = (Button) mDialog.findViewById(R.id.edit_profile_remote_btn_cancel);
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

		final Button btn_ok = (Button) mDialog.findViewById(R.id.edit_profile_remote_btn_ok);
		btn_ok.setEnabled(false);
		dlg_msg.setText(mContext.getString(R.string.msgs_audit_msgs_profilename2));
		
		setRemoteProfileCommonListener(mDialog);
		
		editname.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				boolean enable_ok=true;
				if (!mProfUtil.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_REMOTE, s.toString())) {
					String audit_msg="";
					if (mProfUtil.hasInvalidChar(s.toString(),new String[]{"\t"})) {
						audit_msg=String.format(
								mContext.getString(R.string.msgs_audit_msgs_profilename1),mProfUtil.getInvalidCharMsg());
						enable_ok=false;
					} else if (s.toString().length()==0) {
						audit_msg=mContext.getString(R.string.msgs_audit_msgs_profilename2);
						editname.requestFocus();
						enable_ok=false;
					} else {
						enable_ok=true;
					}
					dlg_msg.setText(audit_msg);
				} else {
					dlg_msg.setText(mContext.getString(R.string.msgs_duplicate_profile));
					enable_ok=false;
				}
				if (enable_ok) setRemoteProfileOkBtnEnabled(mDialog);
				else btn_ok.setEnabled(false);
			}
		});
		
		// OKボタンの指定
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String prof_name, prof_user, prof_pass, prof_share, prof_dir, prof_act;
				String prof_addr="", prof_host="";
				
				if (NetworkUtil.isValidIpAddress(edithost.getText().toString())) prof_addr = edithost.getText().toString();
				else prof_host = edithost.getText().toString();
				
				prof_user = edituser.getText().toString();
				prof_pass = editpass.getText().toString();
				prof_share = editshare.getText().toString();
				prof_dir = editdir.getText().toString();
				prof_name = editname.getText().toString();

				if (!ctv_use_user_pass.isChecked()) {
					prof_user="";
					prof_pass="";
				}
				
				if (ctv_active.isChecked()) prof_act = SMBSYNC_PROF_ACTIVE;
					else prof_act = SMBSYNC_PROF_INACTIVE;
				if (!mProfUtil.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_REMOTE, prof_name)) {
					mFragment.dismiss();
					String remote_port="";
					if (ctv_use_port_number.isChecked()) remote_port=editport.getText().toString();
					if (mGp.profileAdapter.getItem(0).getProfileType().equals(""))
						mGp.profileAdapter.remove(0);
					ProfileUtility.updateRemoteProfileAdapter(mGp, true, prof_name, prof_act,prof_dir,
							prof_user,prof_pass,prof_share,prof_addr,prof_host,
							remote_port, 
							"",0,0,
							false,0);
					mGp.profileAdapter.sort();
					ProfileUtility.setAllProfileToUnchecked(true, mGp.profileAdapter);
//					mGp.profileAdapter.notifyDataSetChanged();
					ProfileUtility.saveProfileToFile(mGp, mContext, mUtil, false,"","",mGp.profileAdapter,false);
					if (mNotifyComplete!=null) mNotifyComplete.notifyToListener(true, null);
				} else {
					((TextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_msg))
					.setText(mContext.getString(R.string.msgs_duplicate_profile));
				}
			}
		});

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
				mUtil.addDebugLogMsg(1,"W","Logon is cancelled.");
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
				mUtil.addDebugLogMsg(1,"I","Test logon started, host="+host+", addr="+addr+
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
								mCommonDlg.showCommonDialog(false, "E", 
										mContext.getString(R.string.msgs_remote_profile_dlg_logon_error)
										, err_msg, null);
								if (p_ntfy!=null) p_ntfy.notifyToListener(false, null);
							} else {
								mCommonDlg.showCommonDialog(false, "I",  
									mContext.getString(R.string.msgs_remote_profile_dlg_logon_success), "", null);
								if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
							}
						} else {
							mCommonDlg.showCommonDialog(false, "I",  
									mContext.getString(R.string.msgs_remote_profile_dlg_logon_cancel), "", null);
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
						mUtil.addDebugLogMsg(1,"I","Test logon failed, remote server not connected");
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
						mUtil.addDebugLogMsg(1,"I","Test logon failed, remote server not connected");
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
//		SmbFile[] lf=null;
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
			mUtil.addDebugLogMsg(1,"I","Test logon completed, host="+host+
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
			mUtil.addDebugLogMsg(1,"I","Test logon failed."+"\n"+err_msg);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Thread.currentThread().setUncaughtExceptionHandler(defaultUEH);
		ntfy.notifyToListener(true, new Object[] {err_msg});
	};
	
	private void setRemoteProfileCommonListener(final Dialog dialog) {
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.edit_profile_remote_dlg_msg);

		final EditText edithost = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_server);

		final EditText edituser = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_user);
		final EditText editpass = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_pass);
		final EditText editshare = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_share_name);
		final EditText editdir = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_dir);

		final CheckedTextView ctv_use_user_pass = (CheckedTextView) dialog.findViewById(R.id.edit_profile_remote_dlg_ctv_use_user_pass);

		final CheckedTextView ctv_use_port_number = (CheckedTextView) dialog.findViewById(R.id.edit_profile_remote_dlg_ctv_use_remote_port_number);
		final EditText editport = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_port);

		
//		final Button btn_logon = (Button) dialog.findViewById(R.id.remote_profile_logon);
//		final Button btn_ok = (Button) dialog.findViewById(R.id.remote_profile_ok);
		final LinearLayout ll_port = (LinearLayout) dialog.findViewById(R.id.edit_profile_remote_dlg_port_option_view);
		if (mGp.settingShowRemotePortOption) {
			ll_port.setVisibility(LinearLayout.VISIBLE);
		} else {
			ll_port.setVisibility(LinearLayout.GONE);
		}
		ctv_use_user_pass.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctv_use_user_pass.toggle();
				boolean isChecked=ctv_use_user_pass.isChecked();
				if (isChecked) {
					edituser.setEnabled(true);
					editpass.setEnabled(true);
				} else {
					edituser.setEnabled(false);
					editpass.setEnabled(false);
				}
				setRemoteProfileOkBtnEnabled(dialog);
			}
		});

		edithost.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (mProfUtil.hasInvalidChar(s.toString(),new String[]{"\t"})) {
					String new_val=mProfUtil.removeInvalidChar(s.toString());
					dlg_msg.setText(String.format(
							mContext.getString(R.string.msgs_audit_msgs_address1),mProfUtil.getInvalidCharMsg()));
					edithost.setText(new_val);
					edithost.requestFocus();
				} else if (s.length()==0) {
					dlg_msg.setText(mContext.getString(R.string.msgs_audit_msgs_address2));
					edithost.requestFocus();
				}
				setRemoteProfileOkBtnEnabled(dialog);
			}
		});
		edituser.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (mProfUtil.hasInvalidChar(s.toString(),new String[]{"\t"})) {
					String new_val=mProfUtil.removeInvalidChar(s.toString());
					dlg_msg.setText(String.format(
							mContext.getString(R.string.msgs_audit_msgs_username1),mProfUtil.getInvalidCharMsg()));
					edituser.setText(new_val);
					edituser.requestFocus();
				} else if (s.length()==0) {
					if (edituser.getText().length()==0 && editpass.getText().length()==0) {
						dlg_msg.setText(String.format(
								mContext.getString(R.string.msgs_audit_msgs_user_or_pass_missing),mProfUtil.getInvalidCharMsg()));
						edituser.requestFocus();
					}
				}
				setRemoteProfileOkBtnEnabled(dialog);
			}
		});
		editpass.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (mProfUtil.hasInvalidChar(s.toString(),new String[]{"\t"})) {
					String new_val=mProfUtil.removeInvalidChar(s.toString());
					dlg_msg.setText(String.format(
							mContext.getString(R.string.msgs_audit_msgs_password1),mProfUtil.getInvalidCharMsg()));
					editpass.setText(new_val);
					editpass.requestFocus();
				} else if (s.length()==0) {
					if (edituser.getText().length()==0 && editpass.getText().length()==0) {
						dlg_msg.setText(String.format(
								mContext.getString(R.string.msgs_audit_msgs_user_or_pass_missing),mProfUtil.getInvalidCharMsg()));
						edituser.requestFocus();
					}
				}
				setRemoteProfileOkBtnEnabled(dialog);
			}
		});
		editshare.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (mProfUtil.hasInvalidChar(s.toString(),new String[]{"\t"})) {
					String new_val=mProfUtil.removeInvalidChar(s.toString());
					dlg_msg.setText(String.format(
							mContext.getString(R.string.msgs_audit_msgs_share1),mProfUtil.getInvalidCharMsg()));
					editshare.setText(new_val);
					editshare.requestFocus();
				} else if (s.toString().length()==0) {
					dlg_msg.setText(mContext.getString(R.string.msgs_audit_msgs_share2));
					editshare.requestFocus();
				}
				setRemoteProfileOkBtnEnabled(dialog);
			}
		});
		editdir.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (mProfUtil.hasInvalidChar(s.toString(),new String[]{"\t"})) {
					String new_val=mProfUtil.removeInvalidChar(s.toString());
					dlg_msg.setText(String.format(
							mContext.getString(R.string.msgs_audit_msgs_dir1),mProfUtil.getInvalidCharMsg()));
					editdir.setText(new_val);
				}
			}
		});
		
		ctv_use_port_number.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctv_use_port_number.toggle();
				boolean isChecked=ctv_use_port_number.isChecked();
				if (isChecked) editport.setEnabled(true);
				else editport.setEnabled(false);
				setRemoteProfileOkBtnEnabled(dialog);
			}
		});

		editport.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				setRemoteProfileOkBtnEnabled(dialog);
			}
		});
	};
	
	private void setRemoteProfileOkBtnEnabled(Dialog dialog) {
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.edit_profile_remote_dlg_msg);

		final EditText edithost = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_server);

		final EditText edituser = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_user);
		final EditText editpass = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_pass);
		final EditText editshare = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_share_name);

		final CheckedTextView ctv_use_user_pass = (CheckedTextView) dialog.findViewById(R.id.edit_profile_remote_dlg_ctv_use_user_pass);

		final CheckedTextView ctv_use_port_number = (CheckedTextView) dialog.findViewById(R.id.edit_profile_remote_dlg_ctv_use_remote_port_number);
		final EditText editport = (EditText) dialog.findViewById(R.id.edit_profile_remote_dlg_remote_port);
		
		final Button btn_logon = (Button) dialog.findViewById(R.id.edit_profile_remote_dlg_logon_btn);
		final Button btn_ok = (Button) dialog.findViewById(R.id.edit_profile_remote_btn_ok);
		btn_ok.setEnabled(false);
		btn_logon.setEnabled(false);
		if (edithost.getText().length()>0) {
			dlg_msg.setText("");
		} else {
			dlg_msg.setText(mContext.getString(R.string.msgs_audit_hostname_not_spec));
			return;
		}

		if (ctv_use_user_pass.isChecked()) {
			if (edituser.getText().length()>0 || editpass.getText().length()>0) {
				dlg_msg.setText("");
			} else {
				dlg_msg.setText(mContext.getString(R.string.msgs_audit_msgs_user_or_pass_missing));
				return;
			}
		}
		
		if (ctv_use_port_number.isChecked()) {
			if (editport.getText().length()>0) {
				dlg_msg.setText("");
			} else {
				dlg_msg.setText(mContext.getString(R.string.msgs_audit_hostport_not_spec));
				btn_logon.setEnabled(false);
				return;
			}
		}
		if (ctv_use_user_pass.isChecked() && !mUtil.isRemoteDisable()) btn_logon.setEnabled(true);
		else btn_logon.setEnabled(false);

		if (editshare.getText().length()==0) {
			dlg_msg.setText(mContext.getString(R.string.msgs_audit_share_not_spec));
			return;
		}
		btn_ok.setEnabled(true);	
	};

	public void editProfile(final ProfileListItem pfli) {
		mDialog.setContentView(R.layout.edit_profile_remote);

		LinearLayout ll_dlg_view=(LinearLayout) mDialog.findViewById(R.id.edit_profile_remote_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		final CheckedTextView ctv_active = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_ctv_active);
		SMBSyncUtil.setCheckedTextView(ctv_active);
		ctv_active.setChecked(pfli.isProfileActive());
		
		final LinearLayout title_view=(LinearLayout) mDialog.findViewById(R.id.edit_profile_remote_dlg_title_view);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		final TextView dlg_title=(TextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_title);
		dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		
		dlg_title.setText(mContext.getString(R.string.msgs_edit_remote_profile));
		final TextView dlg_title_sub=(TextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_title_sub);
		dlg_title_sub.setTextColor(mGp.themeColorList.text_color_dialog_title);
		dlg_title_sub.setText(" ("+pfli.getProfileName()+")");

		
//		final TextView dlg_msg=(TextView) mDialog.findViewById(R.id.remote_profile_dlg_msg);

		final EditText edithost = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_remote_server);
		
		final EditText edituser = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_remote_user);
		if (pfli.getRemoteUserID().length()!=0) edituser.setText(pfli.getRemoteUserID());
		final EditText editpass = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_remote_pass);
		if (pfli.getRemotePassword().length()!=0) editpass.setText(pfli.getRemotePassword());
		final EditText editshare = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_share_name);
		if (pfli.getRemoteShareName().length()!=0) editshare.setText(pfli.getRemoteShareName());
		final EditText editdir = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_dir);
		if (pfli.getDirectoryName().length()!=0) editdir.setText(pfli.getDirectoryName());
		final EditText editname = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_profile_name);
		editname.setText(pfli.getProfileName());
		editname.setTextColor(Color.LTGRAY);
		editname.setEnabled(false);
		editname.setVisibility(EditText.GONE);

		final CheckedTextView ctv_use_user_pass = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_ctv_use_user_pass);
		
		if (pfli.getRemoteAddr().length()!=0) edithost.setText(pfli.getRemoteAddr());
		else edithost.setText(pfli.getRemoteHostname());
		
		if (pfli.getRemoteUserID().equals("") && pfli.getRemotePassword().equals("")) {
			ctv_use_user_pass.setChecked(false);
			edituser.setEnabled(false);
			editpass.setEnabled(false);
		} else {
			ctv_use_user_pass.setChecked(true);
			edituser.setEnabled(true);
			editpass.setEnabled(true);
		}
		
		final CheckedTextView ctv_use_port_number = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_ctv_use_remote_port_number);
		final EditText editport = (EditText) mDialog.findViewById(R.id.edit_profile_remote_dlg_remote_port);
		if (!pfli.getRemotePort().equals("")) {
			ctv_use_port_number.setChecked(true);
			editport.setText(pfli.getRemotePort());
		} else {
			editport.setText("");
			editport.setEnabled(false);
		}
		
		final Button btnLogon = (Button) mDialog.findViewById(R.id.edit_profile_remote_dlg_logon_btn);
		btnLogon.setEnabled(ctv_use_user_pass.isChecked());
		btnLogon.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String user=null, pass=null;
				if (ctv_use_user_pass.isChecked()) {
					if (edituser.getText().length()>0) user=edituser.getText().toString();
					if (editpass.getText().length()>0) pass=editpass.getText().toString();
				}
				String port="";
				if (ctv_use_port_number.isChecked()) port=editport.getText().toString();
				if (NetworkUtil.isValidIpAddress(edithost.getText().toString())) {
					String t_addr=edithost.getText().toString();
					String s_addr=t_addr;
					if (t_addr.indexOf(":")>=0) s_addr=t_addr.substring(0,t_addr.indexOf(":")) ;
					logonToRemoteDlg("",s_addr, port, user,pass,null);
				} else {
					logonToRemoteDlg(edithost.getText().toString(), "", port, user,pass,null);
				}
			}
		});

		CommonDialog.setDlgBoxSizeLimit(mDialog,true);

		final Button btn_ok = (Button) mDialog.findViewById(R.id.edit_profile_remote_btn_ok);
		setRemoteProfileCommonListener(mDialog);
		
		// addressボタンの指定
		Button btnAddr = (Button) mDialog.findViewById(R.id.edit_profile_remote_dlg_search_remote_host);
		if (mUtil.isRemoteDisable()) btnAddr.setEnabled(false);
		btnAddr.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mProfUtil.ipAddressScanButtonDlg(mDialog);
			}
		});
		
		// RemoteShareボタンの指定
		Button btnGet1 = (Button) mDialog.findViewById(R.id.edit_profile_remote_dlg_list_share_btn);
		if (mUtil.isRemoteDisable()) btnGet1.setEnabled(false);
		btnGet1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mProfUtil.invokeSelectRemoteShareDlg(mDialog);
			}
		});
		// RemoteDirectoryボタンの指定
		final Button btnGet2 = (Button) mDialog.findViewById(R.id.edit_profile_remote_dlg_list_directory_btn);
		if (mUtil.isRemoteDisable()) btnGet2.setEnabled(false);
		btnGet2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mProfUtil.selectRemoteDirectory(mDialog);
			}
		});

		// CANCELボタンの指定
		final Button btn_cancel = (Button) mDialog.findViewById(R.id.edit_profile_remote_btn_cancel);
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mFragment.dismiss();
				if (mNotifyComplete!=null) mNotifyComplete.notifyToListener(false, null);
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
				String prof_name, remote_user, remote_pass, remote_share, 
				prof_dir, prof_act = null;
				String remote_host="",remote_addr="";

				
				remote_user = edituser.getText().toString();
				remote_pass = editpass.getText().toString();
				remote_share = editshare.getText().toString();
				prof_dir = editdir.getText().toString();
				prof_name = editname.getText().toString();
				
				if (!ctv_use_user_pass.isChecked()) {
					remote_user="";
					remote_pass="";
				}
				
				if (ctv_active.isChecked()) prof_act = SMBSYNC_PROF_ACTIVE;
					else prof_act = SMBSYNC_PROF_INACTIVE;
//				String e_msg=auditRemoteProfileField(dialog);
//				if (e_msg.length()!=0) {
//					((TextView) mDialog.findViewById(R.id.remote_profile_dlg_msg))
//					.setText(e_msg);
//					return;
//				} else {
					ProfileListItem item = mGp.profileAdapter.getItem(mProfileItemPos);
					if (prof_name.equals(item.getProfileName())||
							(!prof_name.equals(item.getProfileName()) &&
							 !mProfUtil.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,
									 SMBSYNC_PROF_TYPE_REMOTE, prof_name))) {
						mFragment.dismiss();
						if (NetworkUtil.isValidIpAddress(edithost.getText().toString())) {
							remote_addr=edithost.getText().toString();
						} else {
							remote_host=edithost.getText().toString();
						}

						String remote_port="";
						if (ctv_use_port_number.isChecked()) 
							remote_port=editport.getText().toString();
//						int pos=glblParms.profileListView.getFirstVisiblePosition();
//						int posTop=glblParms.profileListView.getChildAt(0).getTop();
						ProfileUtility.updateRemoteProfileAdapter(mGp, false,prof_name,prof_act,prof_dir,
								remote_user, remote_pass,remote_share,
								remote_addr,remote_host,remote_port,
								"",0,0,
								false,mProfileItemPos);
						ProfileUtility.resolveSyncProfileRelation(mGp);
						ProfileUtility.saveProfileToFile(mGp, mContext, mUtil, false,"","",mGp.profileAdapter,false);
						mGp.profileAdapter.notifyDataSetChanged();
//						AdapterProfileList tfl= createProfileList(false,"");
//						replaceglblParms.profileAdapterContent(tfl);
//						glblParms.profileListView.setSelectionFromTop(pos,posTop);
						if (mNotifyComplete!=null) mNotifyComplete.notifyToListener(true, null);
					} else {
						((TextView) mDialog.findViewById(R.id.edit_profile_remote_dlg_msg))
						.setText(mContext.getString(R.string.msgs_duplicate_profile));
					}
//				}
			}
		});

	};

}
