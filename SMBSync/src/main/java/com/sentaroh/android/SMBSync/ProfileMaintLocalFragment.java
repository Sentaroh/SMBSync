package com.sentaroh.android.SMBSync;

import static com.sentaroh.android.SMBSync.Constants.*;

import com.sentaroh.android.Utilities.LocalMountPoint;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.SafUtil;
import com.sentaroh.android.Utilities.SafCommonArea;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class ProfileMaintLocalFragment extends DialogFragment{
	private final static boolean DEBUG_ENABLE=false;
	private final static String SUB_APPLICATION_TAG="LocalProfile ";

	private Dialog mDialog=null;
	private boolean mTerminateRequired=true;
	private Context mContext=null;
	private ProfileMaintLocalFragment mFragment=null;
	private GlobalParameters mGp=null;
	private ProfileUtility mProfUtil=null;
	private CommonDialog mCommonDlg=null;

	private SafCommonArea mSafCA=new SafCommonArea();
	
	public static ProfileMaintLocalFragment newInstance() {
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"newInstance");
		ProfileMaintLocalFragment frag = new ProfileMaintLocalFragment();
        Bundle bundle = new Bundle();
        frag.setArguments(bundle);
        return frag;
    };
    
	public ProfileMaintLocalFragment() {
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
    	SafUtil.initWorkArea(mContext, mSafCA, mGp.debugLevel>0);
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
			final Button btnCancel = (Button) mDialog.findViewById(R.id.edit_profile_local_btn_cancel);
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

        public int lmp_pos=-1;
        public String lcl_dir="";
    };

    private SavedViewContents saveViewContents() {
    	SavedViewContents sv=new SavedViewContents();
		
		final EditText editname = (EditText)mDialog.findViewById(R.id.edit_profile_local_dlg_profile_name);
		final CheckedTextView ctv_active = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_local_dlg_ctv_active);

		final EditText editdir = (EditText) mDialog.findViewById(R.id.edit_profile_local_dlg_dir);
		final Spinner spinner=(Spinner) mDialog.findViewById(R.id.edit_profile_local_dlg_lmp_btn);

        sv.prof_name_et=editname.getText();
        sv.prof_name_et_spos=editname.getSelectionStart();
        sv.prof_name_et_epos=editname.getSelectionEnd();
        sv.cb_active=ctv_active.isChecked();

        sv.lmp_pos=spinner.getSelectedItemPosition();
        sv.lcl_dir=editdir.getText().toString();

//        final Spinner spinnerDateTimeType = (Spinner) mDialog.findViewById(R.id.edit_profile_time_date_time_type);
//        
//        sv.day_of_the_week=getDayOfTheWeekString(mGp,mDialog); 
        return sv;
    };

    private void restoreViewContents(final SavedViewContents sv) {
		final EditText editname = (EditText)mDialog.findViewById(R.id.edit_profile_local_dlg_profile_name);
		final CheckedTextView ctv_active = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_local_dlg_ctv_active);

		final EditText editdir = (EditText) mDialog.findViewById(R.id.edit_profile_local_dlg_dir);
		final Spinner spinner=(Spinner) mDialog.findViewById(R.id.edit_profile_local_dlg_lmp_btn);

        spinner.setEnabled(false);

    	Handler hndl1=new Handler();
    	hndl1.postDelayed(new Runnable(){
			@Override
			public void run() {
		        editname.setText(sv.prof_name_et);
//		        editname.setSelection(sv.prof_name_et_spos);
//		        editname.getSelectionEnd();
		        ctv_active.setChecked(sv.cb_active);

		        sv.lmp_pos=spinner.getSelectedItemPosition();
		        sv.lcl_dir=editdir.getText().toString();

		    	Handler hndl2=new Handler();
		    	hndl2.postDelayed(new Runnable(){
					@Override
					public void run() {
				        spinner.setEnabled(true);
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
	private NotifyEvent mNotifyComplete=null;
    public void showDialog(FragmentManager fm, Fragment frag,
    		final String op_type,
			final ProfileListItem pli,
			int pin,
			ProfileUtility pm,
			SMBSyncUtil ut,
			CommonDialog cd,
			NotifyEvent ntfy) {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,SUB_APPLICATION_TAG+"showDialog");
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
		mDialog.setContentView(R.layout.edit_profile_local);

		LinearLayout ll_dlg_view=(LinearLayout) mDialog.findViewById(R.id.edit_profile_local_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		final LinearLayout title_view=(LinearLayout) mDialog.findViewById(R.id.edit_profile_local_dlg_title_view);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		final TextView dlg_title=(TextView) mDialog.findViewById(R.id.edit_profile_local_dlg_title);
		dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);

		if (!copy) dlg_title.setText(mContext.getString(R.string.msgs_add_local_profile));
		else dlg_title.setText(mContext.getString(R.string.msgs_copy_local_profile));
		
		final TextView dlg_msg=(TextView) mDialog.findViewById(R.id.edit_profile_local_dlg_msg);
		final CheckedTextView ctv_active = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_local_dlg_ctv_active);
		final EditText editdir = (EditText) mDialog.findViewById(R.id.edit_profile_local_dlg_dir);
		editdir.setText(pfli.getDirectoryName());
		final EditText editname = (EditText) mDialog.findViewById(R.id.edit_profile_local_dlg_profile_name);
		editname.setText(pfli.getProfileName());
		final TextView dlg_title_sub=(TextView) mDialog.findViewById(R.id.edit_profile_local_dlg_title_sub);
		if (pfli.getProfileName().equals("")) dlg_title_sub.setVisibility(TextView.GONE);

		final Spinner spinner=(Spinner) mDialog.findViewById(R.id.edit_profile_local_dlg_lmp_btn);
		SMBSyncUtil.setSpinnerBackground(mContext, spinner, mGp.themeIsLight);
		spinner.setVisibility(Spinner.VISIBLE);

		ProfileUtility.setLocalMountPointSpinner(mGp, mContext, spinner, pfli.getLocalMountPoint());
		
		CommonDialog.setDlgBoxSizeLimit(mDialog,true);
		
		spinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				checkAppSpecificDir(spinner.getSelectedItem().toString(), editdir.getText().toString());
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
			
		});
		
		ctv_active.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctv_active.toggle();
			}
		});

//		if (pfli.getActive().equals("A")) ctv_active.setChecked(true);
//		else ctv_active.setChecked(false);
		ctv_active.setChecked(true);
		
		// GET_btn1ボタンの指定
		Button btnGet1 = (Button) mDialog.findViewById(R.id.edit_profile_local_dlg_list_dir_btn);
		if (!mGp.externalStorageIsMounted) btnGet1.setEnabled(false);
		btnGet1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				String url=(String)spinner.getSelectedItem();
				String p_dir=editdir.getText().toString();

				NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						editdir.setText((String)arg1[0]);
					}

					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {
						dlg_msg.setText("");
					}
					
				});
				mProfUtil.selectLocalDirDlg(url,"",p_dir,ntfy);
			}
		});
		// CANCELボタンの指定
		final Button btn_cancel = (Button) mDialog.findViewById(R.id.edit_profile_local_btn_cancel);
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mDialog.dismiss();
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
		
		final Button btn_ok = (Button) mDialog.findViewById(R.id.edit_profile_local_btn_ok);
		btn_ok.setEnabled(false);
		dlg_msg.setText(mContext.getString(R.string.msgs_audit_msgs_profilename2));
		
		editname.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (!mProfUtil.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_LOCAL, s.toString())) {
					String audit_msg="";
					if (mProfUtil.hasInvalidChar(s.toString(),new String[]{"\t"})) {
						audit_msg=String.format(mContext.getString(R.string.msgs_audit_msgs_profilename1),
								"\t");
						btn_ok.setEnabled(false);
					} else if (s.toString().length()==0) {
						audit_msg=mContext.getString(R.string.msgs_audit_msgs_profilename2);
						editname.requestFocus();
						btn_ok.setEnabled(false);
					} else {
						btn_ok.setEnabled(true);
					}
					dlg_msg.setText(audit_msg);
				} else {
					dlg_msg.setText(mContext.getString(R.string.msgs_duplicate_profile));
					btn_ok.setEnabled(false);
				}
			}
		});
		editdir.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				String audit_msg="";
				if (mProfUtil.hasInvalidChar(s.toString(),new String[]{"\t"})) {
					audit_msg=String.format(mContext.getString(R.string.msgs_audit_msgs_local_dir),mProfUtil.getInvalidCharMsg());
				}
				dlg_msg.setText(audit_msg);
				checkAppSpecificDir(spinner.getSelectedItem().toString(), editdir.getText().toString());
			}
		});

		// OKボタンの指定
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String prof_name, prof_dir, prof_act, prof_lmp;
				boolean audit_error = false;
				String audit_msg="";
//				prof_lmp=mGp.externalRootDirectory;
				prof_lmp=(String)spinner.getSelectedItem();
				prof_dir = editdir.getText().toString();
				prof_name = editname.getText().toString();
				if (mProfUtil.hasInvalidChar(prof_dir,new String[]{"\t"})) {
					audit_error=true;
					prof_dir=mProfUtil.removeInvalidChar(prof_dir);
					audit_msg=String.format(
							mContext.getString(R.string.msgs_audit_msgs_local_dir),mProfUtil.getInvalidCharMsg());
					editdir.setText(prof_dir);
				}
				if (!audit_error) {
					if (mProfUtil.hasInvalidChar(prof_name,new String[]{"\t"})) {
						audit_error=true;
						prof_dir=mProfUtil.removeInvalidChar(prof_name);
						audit_msg=String.format(
								mContext.getString(R.string.msgs_audit_msgs_profilename1),mProfUtil.getInvalidCharMsg());
						editname.setText(prof_name);
					} else if (prof_name.length()==0) {
						audit_error=true;
						audit_msg=mContext.getString(R.string.msgs_audit_msgs_profilename2);
						editname.requestFocus();
					}
				}
				if (ctv_active.isChecked()) prof_act = SMBSYNC_PROF_ACTIVE;
					else prof_act = SMBSYNC_PROF_INACTIVE;
				
				if (audit_error) {
					((TextView) mDialog.findViewById(R.id.edit_profile_local_dlg_msg))
						.setText(audit_msg);
					return;
				} else {
					if (!mProfUtil.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_LOCAL, prof_name)) {
						mDialog.dismiss();
//						int pos=glblParms.profileListView.getFirstVisiblePosition();
//						int posTop=glblParms.profileListView.getChildAt(0).getTop();
						if (mGp.profileAdapter.getItem(0).getProfileType().equals(""))
							mGp.profileAdapter.remove(0);
						ProfileUtility.updateLocalProfileAdapter(mGp, true,prof_name,prof_act,
								prof_lmp, prof_dir,
								"",0,0,
								false,0);
						mGp.profileAdapter.sort();
						ProfileUtility.saveProfileToFile(mGp, mContext, mUtil, false,"","",mGp.profileAdapter,false);
						if (SafUtil.isSafExternalSdcardPath(mSafCA, prof_lmp)) checkSafExternalSdcardTreeUri();
//						AdapterProfileList tfl= createProfileList(false,"");
//						replaceglblParms.profileAdapterContent(tfl);
//						glblParms.profileListView.setSelectionFromTop(pos,posTop);
						ProfileUtility.setAllProfileToUnchecked(true, mGp.profileAdapter);
						if (mNotifyComplete!=null) mNotifyComplete.notifyToListener(true, null);
					} else {
						((TextView) mDialog.findViewById(R.id.edit_profile_local_dlg_msg))
							.setText(mContext.getString(R.string.msgs_duplicate_profile));
						return;
					}
//					glblParms.profileListView.setSelectionFromTop(currentViewPosX,currentViewPosY);
				}
			}
		}); 

    };
    
	private final int REQUEST_CODE_STORAGE_ACCESS=40;
	@SuppressLint("InlinedApi")
	private void checkSafExternalSdcardTreeUri() {
		if (Build.VERSION.SDK_INT>=21) {
			if (mProfUtil.isExternalSdcardUsedByOutput()) {
				if (SafUtil.hasSafExternalSdcard(mSafCA) && !SafUtil.isValidSafExternalSdcardRootTreeUri(mSafCA)) {
					final Activity actv=getActivity();
	        		NotifyEvent ntfy=new NotifyEvent(mContext);
	        		ntfy.setListener(new NotifyEventListener(){
						@Override
						public void positiveResponse(Context c, Object[] o) {
							Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
						    actv.startActivityForResult(intent, REQUEST_CODE_STORAGE_ACCESS);
						}
						@Override
						public void negativeResponse(Context c, Object[] o) {
							ProfileListItem pli=mProfUtil.getExternalSdcardUsedSyncProfile();
							String msg=String.format(mContext.getString(R.string.msgs_main_external_sdcard_select_required_cancel_msg),pli.getProfileName());
			        		mCommonDlg.showCommonDialog(false, "W", 
	        				mContext.getString(R.string.msgs_main_external_sdcard_select_required_title), 
	        				msg, 
	        				null);
						}
	        		});
//	        		mCommonDlg.showCommonDialog(false, "W", 
//	        				mContext.getString(R.string.msgs_main_external_sdcard_select_required_title), 
//	        				mContext.getString(R.string.msgs_main_external_sdcard_select_required_select_msg), 
//	        				ntfy);
	        		mProfUtil.showSelectSdcardMsg(ntfy,
	        				mContext.getString(R.string.msgs_main_external_sdcard_select_required_select_msg));
				} 
			}
		}
	};

    private void checkAppSpecificDir(String lmp, String dir) {
    	if (LocalMountPoint.isAppSpecificDirectory(mContext, lmp, dir)) {
    		mCommonDlg.showCommonDialog(false, "W", 
    				mContext.getString(R.string.msgs_local_mount_point_app_specific_dir_used_title), 
    				mContext.getString(R.string.msgs_local_mount_point_app_specific_dir_used_msg), null);
    	}
    };
    
	private void editProfile(final ProfileListItem pfli) {
		mDialog.setContentView(R.layout.edit_profile_local);
		
		LinearLayout ll_dlg_view=(LinearLayout) mDialog.findViewById(R.id.edit_profile_local_dlg_view);
		ll_dlg_view.setBackgroundColor(mGp.themeColorList.dialog_msg_background_color);

		final LinearLayout title_view=(LinearLayout) mDialog.findViewById(R.id.edit_profile_local_dlg_title_view);
		title_view.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
		final TextView dlg_title=(TextView) mDialog.findViewById(R.id.edit_profile_local_dlg_title);
		dlg_title.setTextColor(mGp.themeColorList.text_color_dialog_title);
		dlg_title.setText(mContext.getString(R.string.msgs_edit_local_profile));
		final TextView dlg_title_sub=(TextView) mDialog.findViewById(R.id.edit_profile_local_dlg_title_sub);
		dlg_title_sub.setTextColor(mGp.themeColorList.text_color_dialog_title);
		dlg_title_sub.setText(" ("+pfli.getProfileName()+")");
		final TextView dlg_msg=(TextView) mDialog.findViewById(R.id.edit_profile_local_dlg_msg);
		final EditText editdir = (EditText) mDialog.findViewById(R.id.edit_profile_local_dlg_dir);
		editdir.setText(pfli.getDirectoryName());
		final EditText editname = (EditText) mDialog.findViewById(R.id.edit_profile_local_dlg_profile_name);
		editname.setText(pfli.getProfileName());
		editname.setTextColor(Color.LTGRAY);
		editname.setEnabled(false);
		editname.setVisibility(EditText.GONE);
		final CheckedTextView ctv_active = (CheckedTextView) mDialog.findViewById(R.id.edit_profile_local_dlg_ctv_active);
		ctv_active.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctv_active.toggle();
			}
		});
		ctv_active.setChecked(pfli.isProfileActive());
		
		final Spinner spinner=(Spinner) mDialog.findViewById(R.id.edit_profile_local_dlg_lmp_btn);
		spinner.setVisibility(Spinner.VISIBLE);
		SMBSyncUtil.setSpinnerBackground(mContext, spinner, mGp.themeIsLight);
		ProfileUtility.setLocalMountPointSpinner(mGp, mContext, spinner, pfli.getLocalMountPoint());

		spinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				checkAppSpecificDir(spinner.getSelectedItem().toString(), editdir.getText().toString());
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
			
		});

		CommonDialog.setDlgBoxSizeLimit(mDialog,true);
		
		// GET_btn1ボタンの指定
		Button btnGet1 = (Button) mDialog.findViewById(R.id.edit_profile_local_dlg_list_dir_btn);
		if (!mGp.externalStorageIsMounted) btnGet1.setEnabled(false);
		btnGet1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
//				String url=SMBSync_External_Root_Dir;
				String url=(String)spinner.getSelectedItem();
				String p_dir=editdir.getText().toString();
				NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						editdir.setText((String)arg1[0]);
					}

					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {
						dlg_msg.setText("");
					}
					
				});
				mProfUtil.selectLocalDirDlg(url,"",p_dir,ntfy);
			}
		});

		editdir.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				String audit_msg="";
				if (mProfUtil.hasInvalidChar(s.toString(),new String[]{"\t"})) {
					audit_msg=String.format(mContext.getString(R.string.msgs_audit_msgs_local_dir),mProfUtil.getInvalidCharMsg());
				}
				dlg_msg.setText(audit_msg);
				checkAppSpecificDir(spinner.getSelectedItem().toString(), editdir.getText().toString());
			}
		});

		// CANCELボタンの指定
		final Button btn_cancel = (Button) mDialog.findViewById(R.id.edit_profile_local_btn_cancel);
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mDialog.dismiss();
				if (mNotifyComplete!=null) mNotifyComplete.notifyToListener(false, null);
//				glblParms.profileListView.setSelectionFromTop(currentViewPosX, currentViewPosY);
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
		Button btn_ok = (Button) mDialog.findViewById(R.id.edit_profile_local_btn_ok);
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String prof_name, prof_dir, prof_act, prof_lmp;
				boolean audit_error = false;
				String audit_msg="";
				prof_lmp=(String)spinner.getSelectedItem();
				prof_dir = editdir.getText().toString();
				prof_name = editname.getText().toString();

				if (mProfUtil.hasInvalidChar(prof_dir,new String[]{"\t"})) {
					audit_error=true;
					prof_dir=mProfUtil.removeInvalidChar(prof_dir);
					audit_msg=String.format(mContext.getString(R.string.msgs_audit_msgs_local_dir),mProfUtil.getInvalidCharMsg());
					editdir.setText(prof_dir);
				}
				if (!audit_error) {
					if (mProfUtil.hasInvalidChar(prof_name,new String[]{"\t"})) {
						audit_error=true;
						prof_name=mProfUtil.removeInvalidChar(prof_name);
						audit_msg=String.format(
								mContext.getString(R.string.msgs_audit_msgs_profilename1),mProfUtil.getInvalidCharMsg());
						editname.setText(prof_name);
					}  else if (prof_name.length()==0) {
						audit_error=true;
						audit_msg=mContext.getString(R.string.msgs_audit_msgs_sync1);
					}
				}
				if (ctv_active.isChecked()) prof_act = SMBSYNC_PROF_ACTIVE;
					else prof_act = SMBSYNC_PROF_INACTIVE;

				if (audit_error) {
					((TextView) mDialog.findViewById(R.id.edit_profile_local_dlg_msg))
						.setText(audit_msg);
					return;
				} else {
					ProfileListItem item = mGp.profileAdapter.getItem(mProfileItemPos);
					boolean lmp_changed=false;
					if (!LocalFileLastModified.isSetLastModifiedFunctional(prof_lmp)) {
						if (!item.getLocalMountPoint().equals(prof_lmp)) lmp_changed=true;
					}
					if (prof_name.equals(item.getProfileName()) ||
							(!prof_name.equals(item.getProfileName()) &&
							 !mProfUtil.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,
									 SMBSYNC_PROF_TYPE_LOCAL,prof_name))) {
						final String t_prof_name=prof_name;
						final String t_prof_act=prof_act;
						final String t_prof_lmp=prof_lmp;
						final String t_prof_dir=prof_dir;
						NotifyEvent ntfy=new NotifyEvent(null);
						ntfy.setListener(new NotifyEventListener(){
							@Override
							public void positiveResponse(Context c,Object[] o) {
								mDialog.dismiss();
								ProfileUtility.updateLocalProfileAdapter(mGp, false,t_prof_name,t_prof_act,
										t_prof_lmp, t_prof_dir,
										"",0,0,
										false, mProfileItemPos);
								ProfileUtility.resolveSyncProfileRelation(mGp);
								ProfileUtility.saveProfileToFile(mGp, mContext, mUtil, false,"","",mGp.profileAdapter,false);
								mGp.profileAdapter.notifyDataSetChanged();
								if (mNotifyComplete!=null) mNotifyComplete.notifyToListener(true, null);
//								AdapterProfileList tfl= createProfileList(false,"");
//								replaceglblParms.profileAdapterContent(tfl);
								if (SafUtil.isSafExternalSdcardPath(mSafCA, t_prof_lmp)) 
									checkSafExternalSdcardTreeUri();
							}
							@Override
							public void negativeResponse(Context c, Object[] o) {}
						});
						if (lmp_changed) {
							mCommonDlg.showCommonDialog(true, "W", 
									mContext.getString(R.string.msgs_local_profile_dlg_local_mount_changed_confirm_title),
									mContext.getString(R.string.msgs_local_profile_dlg_local_mount_changed_confirm_msg),
									ntfy);
						} else {
							ntfy.notifyToListener(true, null);
						}
					} else {
						((TextView) mDialog.findViewById(R.id.edit_profile_local_dlg_msg))
						.setText(mContext.getString(R.string.msgs_duplicate_profile));
					}
				}
			}
		});

	};

}
