package com.sentaroh.android.SMBSync;

import java.util.ArrayList;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.ThemeColorList;
import com.sentaroh.android.Utilities.ThemeUtil;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.Dialog.FileSelectDialogFragment;
import com.sentaroh.android.Utilities.Dialog.MessageDialogFragment;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.ContextButton.ContextButtonUtil;

public class MountPointManagementFragment extends DialogFragment{
	private final static boolean DEBUG_ENABLE=false;
	private final static String APPLICATION_TAG="MountPointManagementFragment";

	private Dialog mDialog=null;
	private MountPointManagementFragment mFragment=null;
	private boolean terminateRequired=true;
	private GlobalParameters mGp=null;
	
	private Context mContext=null;

	private ArrayList<MountPointEditListItem> mMountPointList=null;
	private AdapterMountPointEditList mMountPointListAdapter=null;
	
	public static MountPointManagementFragment newInstance() {
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"newInstance");
		MountPointManagementFragment frag = new MountPointManagementFragment();
        Bundle bundle = new Bundle();
        frag.setArguments(bundle);
        return frag;
    }

	public MountPointManagementFragment() {
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"Constructor(Default)");
	}; 
	
	@Override
	public void onAttach(Activity activity) {
	    super.onAttach(activity);
	    if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"onAttach");
	};

	@Override
	public void onSaveInstanceState(Bundle outState) {  
		super.onSaveInstanceState(outState);
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"onSaveInstanceState");
		if(outState.isEmpty()){
	        outState.putBoolean("WORKAROUND_FOR_BUG_19917_KEY", true);
	    }
    	saveViewContents();
	};  
	
	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
	    // Ignore orientation change to keep activity from restarting
	    super.onConfigurationChanged(newConfig);
	    if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"onConfigurationChanged");

	    reInitViewWidget();
	};

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
	    super.onActivityCreated(savedInstanceState);
	    if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"onActivityCreated");
	};
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"onCreateView");
    	View view=super.onCreateView(inflater, container, savedInstanceState);
    	return view;
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"onCreate");
        
//        Bundle bd=this.getArguments();
    	mFragment=this;
    	mContext=getActivity().getApplicationContext();
    	
    	if (!terminateRequired) 
    		mMountPointList=loadMountPointList();
    };
    
    private ArrayList<MountPointEditListItem> loadMountPointList() {
    	ArrayList<MountPointEditListItem> lmp=new ArrayList<MountPointEditListItem>();
    	ArrayList<String>smpl=mGp.getSystemLocalMountPointList(mContext);
    	for(int i=0;i<smpl.size();i++) {
    		MountPointEditListItem lmpli=new MountPointEditListItem();
    		lmpli.mount_point=smpl.get(i);
    		lmpli.isAvailable=true;
    		lmpli.isSystemDefined=true;
    		lmp.add(lmpli);
    	}
    	ArrayList<String>ampl=mGp.getUserLocalMountPointList(mContext);
    	if (ampl.size()>0) {
        	for(int i=0;i<ampl.size();i++) {
        		MountPointEditListItem lmpli=new MountPointEditListItem();
        		lmpli.mount_point=ampl.get(i);
        		lmp.add(lmpli);
        	}
//    	} else {
//    		LocalMountPointEditListItem lmpli=new LocalMountPointEditListItem();
//    		lmp.add(lmpli);
    	}
    	AdapterMountPointEditList.sort(lmp);
    	return lmp;
    };
    
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"onCreateDialog");
    	
    	mDialog=new Dialog(getActivity(), mGp.applicationTheme);
		mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		mDialog.setCanceledOnTouchOutside(false);

		if (!terminateRequired) initViewWidget();

        return mDialog;
    };
    
	@Override
	public void onStart() {
    	CommonDialog.setDlgBoxSizeLimit(mDialog,true);
	    super.onStart();
	    if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"onStart");
	    if (terminateRequired) mDialog.cancel();
	    else {
	    	mDialog.setOnKeyListener(new OnKeyListener(){
    	        @Override
	    	    public boolean onKey ( DialogInterface dialog , int keyCode , KeyEvent event ){
	    	        // disable search button action
	    	        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction()==KeyEvent.ACTION_DOWN){
	    	        	if (mMountPointListAdapter.isShowCheckBox()) {
		    	        	for(int i=0;i<mMountPointListAdapter.getCount();i++) {
		    	        		mMountPointListAdapter.getItem(i).isChecked=false;
		    	        	}
		    	        	mMountPointListAdapter.setShowCheckBox(false);
		    	        	mMountPointListAdapter.notifyDataSetChanged();
		    	        	setContextButtonNormalMode(mMountPointListAdapter);
		    	        	return true;
	    	        	}
	    	        }
	    	        return false;
	    	    }
	    	});
	    }

	};
	
	@Override
	public void onCancel(DialogInterface di) {
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"onCancel");
		mFragment.dismiss();
		super.onCancel(di);
	};
	
	@Override
	public void onDismiss(DialogInterface di) {
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"onDismiss");
		super.onDismiss(di);
	};

	@Override
	public void onStop() {
	    super.onStop();
	    if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"onStop");
	};
	
	@Override
	public void onDestroyView() {
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"onDestroyView");
	    if (getDialog() != null && getRetainInstance())
	        getDialog().setDismissMessage(null);
	    super.onDestroyView();
	};
	
	@Override
	public void onDetach() {
	    super.onDetach();
	    if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"onDetach");
	};


    private void reInitViewWidget() {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"reInitViewWidget");
    	if (!terminateRequired) {
        	saveViewContents();
        	initViewWidget();
        	restoreViewContents();
        	if (mMountPointListAdapter.isAnyItemSelected()) {
        		setContextButtonSelecteMode(mMountPointListAdapter);
        	} else {
        		setContextButtonNormalMode(mMountPointListAdapter);
        	}
    	}
    };
    
    private void saveViewContents() {
    	
    };
    
    private void restoreViewContents() {
    	
    };

	private TextView mDlgTitle=null;
	private TextView mDlgMsg=null;
	private ImageButton mDlgBtnDone=null;
	private ListView mDlgListView=null;
	private Button mDlgBtnAdd=null;
	private Button mDlgBtnOk=null;
	private Button mDlgBtnCancel=null;
	private EditText mDlgEtPath=null;

    private void initViewWidget() {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"initViewWidget");
    	if (mGp==null) return;
    	
    	mDialog.setContentView(R.layout.mount_point_edit_dlg);
    	
    	ThemeColorList tcl=ThemeUtil.getThemeColorList(getActivity());
		LinearLayout ll_dlg_view=(LinearLayout) mDialog.findViewById(R.id.mount_point_edit_dlg_view);
		ll_dlg_view.setBackgroundColor(tcl.dialog_msg_background_color);

    	LinearLayout mDialogTitleView=(LinearLayout)mDialog.findViewById(R.id.mount_point_edit_dlg_title_view);
    	mDialogTitleView.setBackgroundColor(mGp.themeColorList.dialog_title_background_color);
    	mDlgTitle=(TextView)mDialog.findViewById(R.id.mount_point_edit_dlg_title);
    	mDlgTitle.setTextColor(mGp.themeColorList.text_color_dialog_title);
    	mDlgMsg=(TextView)mDialog.findViewById(R.id.mount_point_edit_dlg_msg);
    	mDlgBtnDone=(ImageButton)mDialog.findViewById(R.id.mount_point_edit_dlg_btn_done);
    	mDlgListView=(ListView)mDialog.findViewById(R.id.mount_point_edit_dlg_listview);
    	mDlgBtnAdd=(Button)mDialog.findViewById(R.id.mount_point_edit_dlg_add_path);
    	mDlgBtnOk=(Button)mDialog.findViewById(R.id.mount_point_edit_dlg_ok_btn);
    	mDlgBtnCancel=(Button)mDialog.findViewById(R.id.mount_point_edit_dlg_cancel_btn);
    	mDlgEtPath=(EditText)mDialog.findViewById(R.id.mount_point_edit_dlg_new_path);

    	mDlgBtnDone.setImageResource(R.drawable.abc_ic_ab_back_mtrl_am_alpha);

    	mDlgBtnDone.setVisibility(ImageButton.GONE);
    	
    	
    	NotifyEvent ntfy_cb_listener=new NotifyEvent(mContext);
    	ntfy_cb_listener.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				if (mMountPointListAdapter.isShowCheckBox()) {
//					if (mLogFileManagementAdapter.isAnyItemSelected()) {
//		        		setContextButtonSelecteMode(mLogFileManagementAdapter);
//		        	} else {
//		        		setContextButtonNormalMode(mLogFileManagementAdapter);
//		        	}
		        	setContextButtonSelecteMode(mMountPointListAdapter);
				}
			};

			@Override
			public void negativeResponse(Context c, Object[] o) {}
    	});
    	
    	mMountPointListAdapter= 
    				new AdapterMountPointEditList(getActivity(), 
    						R.layout.mount_point_edit_list_item, mMountPointList, ntfy_cb_listener);
    	mDlgListView.setAdapter(mMountPointListAdapter);
    	mDlgListView.setClickable(true);
    	mDlgListView.setFocusable(true);
    	mDlgListView.setFastScrollEnabled(true);
    	mDlgListView.setFocusableInTouchMode(true);
    	
    	mDlgMsg.setText(isCheckUserDefinedMountPointExists());
    	
    	setContextButtonListener();
    	setContextButtonNormalMode(mMountPointListAdapter);
    	
    	mDlgListView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
				if (mMountPointListAdapter.getItem(0).mount_point==null || 
						mMountPointListAdapter.getItem(pos).isSystemDefined) return;
//				if (mGlblParms.settingAltUiEnabled) {
//				} else mLogFileManagementAdapter.getItem(pos).isChecked=!mLogFileManagementAdapter.getItem(pos).isChecked;
				if (mMountPointListAdapter.isShowCheckBox()) {
					mMountPointListAdapter.getItem(pos).isChecked=
							!mMountPointListAdapter.getItem(pos).isChecked;
					mMountPointListAdapter.notifyDataSetChanged();
//		        	if (mLogFileManagementAdapter.isAnyItemSelected()) {
//		        		setContextButtonSelecteMode(mLogFileManagementAdapter);
//		        	} else {
//		        		setContextButtonNormalMode(mLogFileManagementAdapter);
//		        	}
	        		setContextButtonSelecteMode(mMountPointListAdapter);
				}
			}
    	});
    	
    	mDlgListView.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
				if (mMountPointListAdapter.isEmptyAdapter() || 
						mMountPointListAdapter.getItem(pos).isSystemDefined) return true;
				if (!mMountPointListAdapter.getItem(pos).isChecked) {
					if (mMountPointListAdapter.isAnyItemSelected()) {
						int down_sel_pos=-1, up_sel_pos=-1;
						int tot_cnt=mMountPointListAdapter.getCount();
						if (pos+1<=tot_cnt) {
							for(int i=pos+1;i<tot_cnt;i++) {
								if (mMountPointListAdapter.getItem(i).isChecked) {
									up_sel_pos=i;
									break;
								}
							}
						}
						if (pos>0) {
							for(int i=pos;i>=0;i--) {
								if (mMountPointListAdapter.getItem(i).isChecked) {
									down_sel_pos=i;
									break;
								}
							}
						}
//						Log.v("","up="+up_sel_pos+", down="+down_sel_pos);
						if (up_sel_pos!=-1 && down_sel_pos==-1) {
							for (int i=pos;i<up_sel_pos;i++) 
								mMountPointListAdapter.getItem(i).isChecked=true;
						} else if (up_sel_pos!=-1 && down_sel_pos!=-1) {
							for (int i=down_sel_pos+1;i<up_sel_pos;i++) 
								mMountPointListAdapter.getItem(i).isChecked=true;
						} else if (up_sel_pos==-1 && down_sel_pos!=-1) {
							for (int i=down_sel_pos+1;i<=pos;i++) 
								mMountPointListAdapter.getItem(i).isChecked=true;
						}
						mMountPointListAdapter.notifyDataSetChanged();
					} else {
						mMountPointListAdapter.setShowCheckBox(true);
						mMountPointListAdapter.getItem(pos).isChecked=true;
						mMountPointListAdapter.notifyDataSetChanged();
					}
					setContextButtonSelecteMode(mMountPointListAdapter);
				}
				return true;
			}
    	});
    	
    	mDlgBtnAdd.setEnabled(false);
    	mDlgEtPath.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before,int count) {
			}
			@Override
			public void afterTextChanged(Editable s) {
				String n_path=s.toString();
				checkMountPointPathValue(n_path);
			}
    		
    	});
    	mDlgBtnAdd.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (mMountPointListAdapter.getItem(0).mount_point==null) {
					mMountPointListAdapter.remove(mMountPointListAdapter.getItem(0));
				}
				MountPointEditListItem lmpli=new MountPointEditListItem();
				lmpli.mount_point=mDlgEtPath.getText().toString();
				mDlgEtPath.setText("");
				mDlgBtnAdd.setEnabled(false);
				mMountPointListAdapter.add(lmpli);
				mMountPointListAdapter.sort();
				mMountPointListAdapter.notifyDataSetChanged();
				mDlgMsg.setText(isCheckUserDefinedMountPointExists());
				setOkBtnEnabled(mMountPointListAdapter);
				setContextButtonNormalMode(mMountPointListAdapter);
			}
    	});
    	mDlgBtnOk.setEnabled(false);
    	mDlgBtnOk.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				mFragment.dismiss();
				ArrayList<String>ampl=new ArrayList<String>();
				for(int i=0;i<mMountPointListAdapter.getCount();i++) {
					if (!mMountPointListAdapter.getItem(i).isSystemDefined &&
						mMountPointListAdapter.getItem(i).mount_point!=null)
						ampl.add(mMountPointListAdapter.getItem(i).mount_point);
				}
				mGp.saveUserLocalMountPointList(mContext, ampl);
				mGp.buildLocalMountPointList(mContext);
			}
    	});
    	
    	mDlgBtnCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				mFragment.dismiss();
			}
    	});
    	
//    	CommonDialog.setDlgBoxSizeLimit(mDialog, true);
    };

    private String isCheckUserDefinedMountPointExists() {
    	String result="";
    	boolean sys_def=false, user_def=false;
    	for(int i=0;i<mMountPointList.size();i++) {
    		if (!mMountPointList.get(i).isSystemDefined) user_def=true;
    		else sys_def=true;
    	}
    	if (!sys_def && !user_def) {
    		//empty
    	} else if (sys_def && !user_def) {
    		//non user def
    		result=mContext.getString(R.string.msgs_edit_mp_no_lmp_entry);
    	} else if (!sys_def && user_def) {
    		//user def only
    	} else if (sys_def && user_def) {
    		//sys and user def
    	}
    	return result;
    };

    private void checkMountPointPathValue(String n_path) {
		if (n_path.equals("")) mDlgBtnAdd.setEnabled(false);
		else {
			if (!n_path.startsWith("/")) {
				mDlgMsg.setText(mContext.getString(R.string.msgs_edit_mp_msg_must_start_slash));
				mDlgBtnAdd.setEnabled(false);
			} else {
				boolean found=false;
				ArrayList<String>smpl=mGp.getSystemLocalMountPointList(mContext);
				for(int i=0;i<smpl.size();i++) {
					if (n_path.equals(smpl.get(i))) {
						mDlgBtnAdd.setEnabled(false);
						found=true;
						mDlgMsg.setText(mContext.getString(R.string.msgs_edit_mp_msg_dup_system_mpl));
						break;
					}
				}
				if (!found) {
					for(int i=0;i<mMountPointListAdapter.getCount();i++) {
						if (n_path.equals(mMountPointListAdapter.getItem(i).mount_point)) {
							mDlgBtnAdd.setEnabled(false);
							found=true;
							mDlgMsg.setText(mContext.getString(R.string.msgs_edit_mp_msg_dup_add_mpl));
							break;
						}
					}
				}
				if (!found) {
					mDlgMsg.setText("");
					mDlgBtnAdd.setEnabled(true);
				}
			}
		}

    }
    
	private void setContextButtonListener() {
		LinearLayout ll_prof=(LinearLayout) mDialog.findViewById(R.id.mount_point_edit_dlg_context_view);
		ImageButton ib_search_mp=(ImageButton)ll_prof.findViewById(R.id.context_button_search_directory);
        ImageButton ib_delete=(ImageButton)ll_prof.findViewById(R.id.context_button_delete);
        ImageButton ib_select_all=(ImageButton)ll_prof.findViewById(R.id.context_button_select_all);
        ImageButton ib_unselect_all=(ImageButton)ll_prof.findViewById(R.id.context_button_unselect_all);

//    	mDlgBtnDone.setVisibility(ImageButton.VISIBLE);
//
//    	mDlgBtnDone.setOnClickListener(new OnClickListener(){
//			@Override
//			public void onClick(View v) {
//				mDlgBtnDone.setVisibility(ImageButton.GONE);
//				mMountPointListAdapter.setAllItemChecked(false);
//				mMountPointListAdapter.setShowCheckBox(false);
//				mMountPointListAdapter.notifyDataSetChanged();
//				setContextButtonNormalMode(mMountPointListAdapter);
//			}
//    	});

        ib_search_mp.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						String n_path=(String)o[0];
						if (n_path.endsWith("/")) mDlgEtPath.setText(n_path.substring(0,n_path.length()-1));
						else mDlgEtPath.setText(n_path);
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
					
				});
				FileSelectDialogFragment fsdf=
						FileSelectDialogFragment.newInstance(false,false, false, false, true, true, "/", "", 
							"", mContext.getString(R.string.msgs_edit_mp_select_dir_for_lmp));
				fsdf.showDialog(getFragmentManager(), fsdf, ntfy);
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, ib_search_mp, 
        		mContext.getString(R.string.msgs_edit_mp_button_title_find_directory));
        
        ib_delete.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				confirmDeleteLocalMountPoint(mMountPointListAdapter);
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, ib_delete, 
        		mContext.getString(R.string.msgs_edit_mp_button_title_delete));
        
        ib_select_all.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				mMountPointListAdapter.setAllItemChecked(true);
				mMountPointListAdapter.setShowCheckBox(true);
				setContextButtonSelecteMode(mMountPointListAdapter);
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, ib_select_all, 
        		mContext.getString(R.string.msgs_edit_mp_button_title_select_all));

        ib_unselect_all.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				mMountPointListAdapter.setAllItemChecked(false);
//				mLogFileManagementAdapter.setShowCheckBox(false);
//				setContextButtonNormalMode(mLogFileManagementAdapter);
				setContextButtonSelecteMode(mMountPointListAdapter);
			}
        });
        ContextButtonUtil.setButtonLabelListener(mContext, ib_unselect_all, 
        		mContext.getString(R.string.msgs_edit_mp_button_title_unselect_all));

	};

	private void setContextButtonSelecteMode(AdapterMountPointEditList mpl_adapter) {
		mDlgMsg.setText("");
    	int u_cnt=0;
    	for(int i=0;i<mpl_adapter.getCount();i++) if (!mpl_adapter.getItem(i).isSystemDefined) u_cnt++;
    	String sel=""+mpl_adapter.getItemSelectedCount()+"/"+u_cnt;
    	mDlgTitle.setText(sel);

    	mDlgBtnOk.setVisibility(Button.GONE);
    	mDlgBtnCancel.setVisibility(Button.GONE);
    	
        mDlgEtPath.setVisibility(EditText.GONE);
        mDlgBtnAdd.setVisibility(Button.GONE);
        
    	mDlgBtnDone.setVisibility(ImageButton.VISIBLE);

		LinearLayout ll_prof=(LinearLayout) mDialog.findViewById(R.id.mount_point_edit_dlg_context_view);
		LinearLayout ll_search_mp=(LinearLayout)ll_prof.findViewById(R.id.context_button_search_directory_view);
		LinearLayout ll_delete=(LinearLayout)ll_prof.findViewById(R.id.context_button_delete_view);
		LinearLayout ll_select_all=(LinearLayout)ll_prof.findViewById(R.id.context_button_select_all_view);
		LinearLayout ll_unselect_all=(LinearLayout)ll_prof.findViewById(R.id.context_button_unselect_all_view);

		ll_search_mp.setVisibility(LinearLayout.GONE);
		
        ll_select_all.setVisibility(LinearLayout.VISIBLE);
        if (mpl_adapter.isAnyItemSelected()) ll_unselect_all.setVisibility(LinearLayout.VISIBLE);
        else ll_unselect_all.setVisibility(LinearLayout.GONE);
        
        if (mpl_adapter.isAnyItemSelected()) ll_delete.setVisibility(LinearLayout.VISIBLE);
        else ll_delete.setVisibility(LinearLayout.GONE);

	};

	private void setContextButtonNormalMode(AdapterMountPointEditList mpl_adapter) {
    	mDlgTitle.setText(mContext.getString(R.string.msgs_edit_mp_dialog_title));
    	int u_cnt=0;
    	for(int i=0;i<mpl_adapter.getCount();i++) if (!mpl_adapter.getItem(i).isSystemDefined) u_cnt++;

        mDlgEtPath.setVisibility(EditText.VISIBLE);
        mDlgBtnAdd.setVisibility(Button.VISIBLE);
        checkMountPointPathValue(mDlgEtPath.getText().toString());

    	mDlgBtnOk.setVisibility(Button.VISIBLE);
    	mDlgBtnCancel.setVisibility(Button.VISIBLE);

    	mDlgBtnDone.setVisibility(ImageButton.GONE);

		LinearLayout ll_prof=(LinearLayout) mDialog.findViewById(R.id.mount_point_edit_dlg_context_view);
		LinearLayout ll_search_mp=(LinearLayout)ll_prof.findViewById(R.id.context_button_search_directory_view);

		LinearLayout ll_delete=(LinearLayout)ll_prof.findViewById(R.id.context_button_delete_view);
		LinearLayout ll_select_all=(LinearLayout)ll_prof.findViewById(R.id.context_button_select_all_view);
		LinearLayout ll_unselect_all=(LinearLayout)ll_prof.findViewById(R.id.context_button_unselect_all_view);

		ll_search_mp.setVisibility(LinearLayout.VISIBLE);
		ll_delete.setVisibility(LinearLayout.GONE);
        
    	if (u_cnt==0) {
            ll_select_all.setVisibility(LinearLayout.GONE);
            ll_unselect_all.setVisibility(LinearLayout.GONE);
    	} else {
            ll_select_all.setVisibility(LinearLayout.VISIBLE);
            ll_unselect_all.setVisibility(LinearLayout.GONE);
    	}
	};

    private void confirmDeleteLocalMountPoint(final AdapterMountPointEditList lmp_adapter) {
    	String delete_list="",sep="";
    	final ArrayList<MountPointEditListItem> mpl=new ArrayList<MountPointEditListItem>();
    	for (int i=0;i<lmp_adapter.getCount();i++) {
    		MountPointEditListItem item=lmp_adapter.getItem(i);
    		if (item.isChecked) {
    			delete_list+=sep+item.mount_point;
    			sep="\n";
    			mpl.add(item);
    		}
    	}

    	NotifyEvent ntfy=new NotifyEvent(null);
    	ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				for (int i=0;i<mpl.size();i++) {
					lmp_adapter.remove(mpl.get(i));	
				}
				lmp_adapter.setAllItemChecked(false);
				lmp_adapter.setShowCheckBox(false);
		    	if (lmp_adapter.getCount()==0) {
		    		MountPointEditListItem lmpli=new MountPointEditListItem();
		    		lmp_adapter.add(lmpli);
		    	}
		    	mDlgMsg.setText(isCheckUserDefinedMountPointExists());
				lmp_adapter.notifyDataSetChanged();
				setContextButtonNormalMode(lmp_adapter);
				setOkBtnEnabled(lmp_adapter);
			}

			@Override
			public void negativeResponse(Context c, Object[] o) {}
    	});
        MessageDialogFragment cdf =MessageDialogFragment.newInstance(true, "W",
        		mContext.getString(R.string.msgs_edit_mp_confirm_delete_lmp),
        		delete_list);
        cdf.showDialog(mFragment.getFragmentManager(),cdf,ntfy);
    };

    private void setOkBtnEnabled(final AdapterMountPointEditList lmp_adapter) {
		String prev_lmp="", new_lmp="";
		ArrayList<String>ampl=mGp.getUserLocalMountPointList(mContext);
		for(int i=0;i<ampl.size();i++) prev_lmp+=ampl.get(i);
		for(int i=0;i<lmp_adapter.getCount();i++) {
			if (!lmp_adapter.getItem(i).isSystemDefined) new_lmp+=lmp_adapter.getItem(i).mount_point;
		}
//		Log.v("","prev="+prev_lmp+", new="+new_lmp);
		if (!prev_lmp.equals(new_lmp)) mDlgBtnOk.setEnabled(true);
		else mDlgBtnOk.setEnabled(false);

    };
    
    public void showDialog(FragmentManager fm, Fragment frag, GlobalParameters gp) {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"showDialog");
    	terminateRequired=false;
    	mGp=gp;
	    FragmentTransaction ft = fm.beginTransaction();
	    ft.add(frag,null);
	    ft.commitAllowingStateLoss();
//    	show(fm, APPLICATION_TAG);
    };


}

