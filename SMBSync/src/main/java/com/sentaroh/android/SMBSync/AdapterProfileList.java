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
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_REMOTE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_SYNC;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_COPY;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_MIRROR;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_MOVE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_SYNC;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.ThemeColorList;
import com.sentaroh.android.Utilities.ThemeUtil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AdapterProfileList extends ArrayAdapter<ProfileListItem> {

		private Context mContext;
		private int id;
		private ArrayList<ProfileListItem>items;
		@SuppressWarnings("unused")
		private String tv_active_active,tv_active_inact, tv_no_sync, tv_status_running;
		private String tv_status_success, tv_status_error, tv_status_cancel;
		
		private ThemeColorList mThemeColorList;
		
		public AdapterProfileList(Context c, int textViewResourceId,
				ArrayList<ProfileListItem> objects) {
			super(c, textViewResourceId, objects);
			mContext = c;
			id = textViewResourceId;
			items = objects;
			
            tv_active_active=mContext.getString(R.string.msgs_sync_list_array_activ_activ);
            tv_active_inact=mContext.getString(R.string.msgs_sync_list_array_activ_inact);
            tv_no_sync=mContext.getString(R.string.msgs_sync_list_array_no_last_sync_time);
            tv_status_running=mContext.getString(R.string.msgs_sync_history_status_running);
            tv_status_success=mContext.getString(R.string.msgs_sync_history_status_success);
            tv_status_error=mContext.getString(R.string.msgs_sync_history_status_error);
            tv_status_cancel=mContext.getString(R.string.msgs_sync_history_status_cancel);
            
            mThemeColorList=ThemeUtil.getThemeColorList(c);
		}
		public ProfileListItem getItem(int i) {
			return items.get(i);
		}
		public  void remove(int i) {
			items.remove(i);
			notifyDataSetChanged();
		}
		public  void replace(ProfileListItem pli, int i) {
			items.set(i,pli);
			notifyDataSetChanged();
		}
		
		private NotifyEvent mNotifyCheckBoxEvent=null;
		public void setNotifyCheckBoxEventHandler(NotifyEvent ntfy) {mNotifyCheckBoxEvent=ntfy;}
		
		private boolean isShowCheckBox=false;
		public void setShowCheckBox(boolean p) {isShowCheckBox=p;}
		public boolean isShowCheckBox() {return isShowCheckBox;}
		
		public void setAllItemChecked(boolean p) {
			if (items!=null) {
				for (int i=0;i<items.size();i++) items.get(i).setChecked(p);
			}
		};
		
		public boolean isEmptyAdapter() {
			boolean result=false;
			if (items!=null) {
				if (items.size()==0 || items.get(0).getProfileType().equals("")) result=true;
			} else {
				result=true;
			}
			return result;
		};

		public ArrayList<ProfileListItem> getArrayList() {return items;}
		
		public void setArrayList(ArrayList<ProfileListItem> p) {
			items.clear();
			if (p!=null) {
				for(int i=0;i<p.size();i++) items.add(p.get(i));
			}
			notifyDataSetChanged();
		}

		public void sort() {
			Collections.sort(items, new Comparator<ProfileListItem>(){
				@Override
				public int compare(ProfileListItem litem, ProfileListItem ritem) {
					String l_t,l_n,l_g;
					String r_t,r_n,r_g;
					
					l_g=litem.getProfileGroup();
					if (litem.getProfileType().equals(SMBSYNC_PROF_TYPE_SYNC)) l_t="0";
					else if (litem.getProfileType().equals(SMBSYNC_PROF_TYPE_REMOTE)) l_t="1";
					else l_t="2";
					l_n=litem.getProfileName();
					
					r_g=ritem.getProfileGroup();
					if (ritem.getProfileType().equals(SMBSYNC_PROF_TYPE_SYNC)) r_t="0";
					else if (ritem.getProfileType().equals(SMBSYNC_PROF_TYPE_REMOTE)) r_t="1";
					else r_t="2";
					r_n=ritem.getProfileName();
					
					if (!l_g.equalsIgnoreCase(r_g)) return l_g.compareToIgnoreCase(r_g);
					else if (!l_t.equalsIgnoreCase(r_t)) return l_t.compareToIgnoreCase(r_t);
					else if (!l_n.equalsIgnoreCase(r_n)) return l_n.compareToIgnoreCase(r_n);
					return 0;
				}
			});
		};
		
//		@Override
//		public boolean isEnabled(int idx) {
//			 return getItem(idx).getActive().equals("A");
//		}

		private Drawable ll_default=null;
		
		@SuppressWarnings("deprecation")
		@Override
	    final public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(id, null);
                holder=new ViewHolder();
                holder.iv_row_icon= (ImageView) v.findViewById(R.id.profile_list_icon);
                holder.tv_row_name= (TextView) v.findViewById(R.id.profile_list_name);
                holder.tv_row_active= (TextView) v.findViewById(R.id.profile_list_active);
                holder.cbv_row_cb1=(CheckBox) v.findViewById(R.id.profile_list_checkbox1);
                
                holder.tv_row_master= (TextView) v.findViewById(R.id.profile_list_master_name);
                holder.tv_row_target= (TextView) v.findViewById(R.id.profile_list_target_name);
                holder.tv_row_synctype= (TextView) v.findViewById(R.id.profile_list_synctype);
                holder.iv_row_sync_dir_image= (ImageView) v.findViewById(R.id.profile_list_sync_direction_image);
                holder.iv_row_image_master= (ImageView) v.findViewById(R.id.profile_list_image_master);
                holder.iv_row_image_target= (ImageView) v.findViewById(R.id.profile_list_image_target);
                holder.tv_mtype_mirror=mContext.getString(R.string.msgs_sync_list_array_mtype_mirr);
                holder.tv_mtype_copy=mContext.getString(R.string.msgs_sync_list_array_mtype_copy);
                holder.tv_mtype_move=mContext.getString(R.string.msgs_sync_list_array_mtype_move);
                
                holder.ll_sync=(LinearLayout) v.findViewById(R.id.profile_list_sync_layout);
                holder.ll_entry=(LinearLayout) v.findViewById(R.id.profile_list_entry_layout);
                holder.ll_view=(LinearLayout) v.findViewById(R.id.profile_list_view);
                if (ll_default!=null) ll_default=holder.ll_view.getBackground();
                
                holder.tv_last_sync_time=(TextView) v.findViewById(R.id.profile_list_last_sync_time);
                holder.tv_last_sync_result=(TextView) v.findViewById(R.id.profile_list_last_sync_result);
                holder.ll_last_sync=(LinearLayout) v.findViewById(R.id.profile_list_last_sync_time_view);
                
                holder.tv_dir_name=(TextView) v.findViewById(R.id.profile_list_dir_name);
                
                v.setTag(holder);
            } else {
            	holder= (ViewHolder)v.getTag();
            }
            final ProfileListItem o = getItem(position);
            if (o != null) {
            	holder.ll_view.setBackgroundDrawable(ll_default);
            	
            	if (o.getProfileType().equals(SMBSYNC_PROF_TYPE_SYNC)) 
            		holder.iv_row_icon.setImageResource(R.drawable.ic_32_sync);
            	else if (o.getProfileType().equals(SMBSYNC_PROF_TYPE_REMOTE)) 
            		holder.iv_row_icon.setImageResource(R.drawable.ic_32_server);
            	else if (o.getProfileType().equals(SMBSYNC_PROF_TYPE_LOCAL)) 
            		holder.iv_row_icon.setImageResource(R.drawable.ic_32_mobile);
            		
            	String act="";
            	if (o.isProfileActive()) act=tv_active_active;
            	else act=tv_active_inact;
            	holder.tv_row_active.setText(act);
            	holder.tv_row_name.setText(o.getProfileName());
                
                if (!getItem(position).getProfileActive().equals("A")) {
//              	   holder.tv_row_name.setEnabled(false);
//              	   holder.tv_row_active.setEnabled(false);
              	   holder.tv_row_name.setTextColor(mThemeColorList.text_color_disabled);
              	   holder.tv_row_active.setTextColor(mThemeColorList.text_color_disabled);
                } else {
//               	   holder.tv_row_name.setEnabled(true);
//               	   holder.tv_row_active.setEnabled(true);
               	   holder.tv_row_name.setTextColor(mThemeColorList.text_color_primary);
               	   holder.tv_row_active.setTextColor(mThemeColorList.text_color_primary);
                }
               
                if (o.getProfileType().equals("S")) {//Sync profile
                	holder.tv_dir_name.setVisibility(LinearLayout.GONE);
                	holder.ll_sync.setVisibility(LinearLayout.VISIBLE);
                	holder.ll_last_sync.setVisibility(LinearLayout.VISIBLE);
                	holder.iv_row_icon.setVisibility(LinearLayout.VISIBLE);
                    holder.tv_row_active.setVisibility(LinearLayout.VISIBLE);
                    holder.cbv_row_cb1.setVisibility(LinearLayout.VISIBLE);
                	
                	String synctp="";
                    
                    if (o.getSyncType().equals(SMBSYNC_SYNC_TYPE_SYNC)) synctp="SYNC";
                    else if (o.getSyncType().equals(SMBSYNC_SYNC_TYPE_MIRROR)) synctp=holder.tv_mtype_mirror;
                    else if (o.getSyncType().equals(SMBSYNC_SYNC_TYPE_MOVE)) synctp=holder.tv_mtype_move;
                    else if (o.getSyncType().equals(SMBSYNC_SYNC_TYPE_COPY)) synctp=holder.tv_mtype_copy;
                    else synctp="ERR";
                	
                    if (o.getMasterType().equals(SMBSYNC_PROF_TYPE_REMOTE)) 
                 	   holder.iv_row_image_master.setImageResource(R.drawable.ic_16_server);
                    else if (o.getMasterType().equals(SMBSYNC_PROF_TYPE_LOCAL)) 
                 	   holder.iv_row_image_master.setImageResource(R.drawable.ic_16_mobile);
                    
                    if (o.getTargetType().equals(SMBSYNC_PROF_TYPE_REMOTE)) 
                 	   holder.iv_row_image_target.setImageResource(R.drawable.ic_16_server);
                    else if (o.getTargetType().equals(SMBSYNC_PROF_TYPE_LOCAL)) 
                 	   holder.iv_row_image_target.setImageResource(R.drawable.ic_16_mobile);
//                    holder.tv_row_master.setText(o.getMasterName());
//                    holder.tv_row_target.setText(o.getTargetName());
                    ProfileListItem pfli_master=ProfileUtility.getProfile(o.getMasterName(), items);
                    ProfileListItem pfli_target=ProfileUtility.getProfile(o.getTargetName(), items);
                    if (pfli_master!=null) {
                    	if (pfli_master.getProfileType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
                            holder.tv_row_master.setText(pfli_master.getLocalMountPoint()+"/"+pfli_master.getDirectoryName());
                    	} if (pfli_master.getProfileType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
                    		if (!pfli_master.getRemoteAddr().equals("")) {
                    			holder.tv_row_master.setText("//"+pfli_master.getRemoteAddr()+"/"+
                    					pfli_master.getRemoteShareName()+"/"+pfli_master.getDirectoryName());
                    		} else if (!pfli_master.getRemoteHostname().equals("")) {
                    			holder.tv_row_master.setText("//"+pfli_master.getRemoteHostname()+"/"+
                    					pfli_master.getRemoteShareName()+"/"+pfli_master.getDirectoryName());
                    		}
                    	}
                    }
                    if (pfli_target!=null) {
                    	if (pfli_target.getProfileType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
                            holder.tv_row_target.setText(pfli_target.getLocalMountPoint()+"/"+pfli_target.getDirectoryName());
                    	} if (pfli_target.getProfileType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
                    		if (!pfli_target.getRemoteAddr().equals("")) {
                    			holder.tv_row_target.setText("//"+pfli_target.getRemoteAddr()+"/"+
                    					pfli_target.getRemoteShareName()+"/"+pfli_target.getDirectoryName());
                    		} else if (!pfli_target.getRemoteHostname().equals("")) {
                    			holder.tv_row_target.setText("//"+pfli_target.getRemoteHostname()+"/"+
                    					pfli_target.getRemoteShareName()+"/"+pfli_target.getDirectoryName());
                    		}
                    	}
                    }
                    holder.tv_row_synctype.setText(synctp);
                    
                    if (!o.getLastSyncTime().equals("")) {
                    	String result="";
                    	holder.tv_last_sync_result.setTextColor(mThemeColorList.text_color_primary);
	        			if (o.isSyncRunning()) {
	        				result=tv_status_running;
                    		if (mThemeColorList.theme_is_light) holder.ll_view.setBackgroundColor(Color.argb(255, 0, 192, 192));
                    		else holder.ll_view.setBackgroundColor(Color.argb(255, 0, 128, 128));
	        			} else {
	            			if (o.getLastSyncResult()==SyncHistoryListItem.SYNC_STATUS_SUCCESS) {
	            				result=tv_status_success;
	            				if (getItem(position).isProfileActive()) {
	                        		holder.tv_last_sync_result.setTextColor(mThemeColorList.text_color_primary);
	            				}
	            			} else if (o.getLastSyncResult()==SyncHistoryListItem.SYNC_STATUS_CANCEL) {
	            				result=tv_status_cancel;
	                        	if (getItem(position).isProfileActive()) {
	                        		holder.tv_last_sync_result.setTextColor(mThemeColorList.text_color_warning);
	                        	}
	            			} else if (o.getLastSyncResult()==SyncHistoryListItem.SYNC_STATUS_ERROR) {
	            				result=tv_status_error;
	                        	if (getItem(position).isProfileActive()) {
	                				holder.tv_last_sync_result.setTextColor(mThemeColorList.text_color_error);
	                        	}
	            			}
	        			}
                        holder.tv_last_sync_time.setText(o.getLastSyncTime());
                        holder.tv_last_sync_result.setText(result);

                    } else {
                    	holder.ll_last_sync.setVisibility(LinearLayout.GONE);
//                    	holder.tv_last_sync_time.setText(tv_no_sync);
                    }
                    
                    if (!getItem(position).isProfileActive()) {
                    	holder.iv_row_sync_dir_image.setImageResource(R.drawable.arrow_right_disabled); 
//                    	holder.tv_row_master.setEnabled(false);
//                    	holder.tv_row_target.setEnabled(false);
//                    	holder.tv_row_synctype.setEnabled(false);
//                    	for(int i=0;i<holder.ll_last_sync.getChildCount();i++) holder.ll_last_sync.getChildAt(i).setEnabled(false);
                    	holder.tv_row_master.setTextColor(mThemeColorList.text_color_disabled);
                    	holder.tv_row_target.setTextColor(mThemeColorList.text_color_disabled);
                    	holder.tv_row_synctype.setTextColor(mThemeColorList.text_color_disabled);
                    	for(int i=0;i<holder.ll_last_sync.getChildCount();i++) ((TextView)holder.ll_last_sync.getChildAt(i)).setTextColor(mThemeColorList.text_color_disabled);
                    } else {
                    	holder.iv_row_sync_dir_image.setImageResource(R.drawable.arrow_right_enabled); 
//                    	holder.tv_row_master.setEnabled(true);
//                    	holder.tv_row_target.setEnabled(true);
//                    	holder.tv_row_synctype.setEnabled(true);
//                    	for(int i=0;i<holder.ll_last_sync.getChildCount();i++) holder.ll_last_sync.getChildAt(i).setEnabled(true);
                    	holder.tv_row_master.setTextColor(mThemeColorList.text_color_primary);
                    	holder.tv_row_target.setTextColor(mThemeColorList.text_color_primary);
                    	holder.tv_row_synctype.setTextColor(mThemeColorList.text_color_primary);
                    	for(int i=0;i<holder.ll_last_sync.getChildCount();i++) ((TextView)holder.ll_last_sync.getChildAt(i)).setTextColor(mThemeColorList.text_color_primary);
                    }
                } else if (o.getProfileType().equals("R") || o.getProfileType().equals("L")) {//Remote or Local profile
                	holder.tv_dir_name.setVisibility(LinearLayout.VISIBLE);
                	holder.ll_sync.setVisibility(LinearLayout.GONE);
                	holder.ll_last_sync.setVisibility(LinearLayout.GONE);
                	holder.iv_row_icon.setVisibility(LinearLayout.VISIBLE);
                    holder.tv_row_active.setVisibility(LinearLayout.VISIBLE);
                    holder.cbv_row_cb1.setVisibility(LinearLayout.VISIBLE);
                    if (o.getProfileType().equals("L")) {
                    	holder.tv_dir_name.setText(o.getLocalMountPoint()+"/"+o.getDirectoryName());
                    } else {
                    	if (!o.getRemoteAddr().equals("")) {
                        	holder.tv_dir_name.setText("//"+o.getRemoteAddr()+"/"+o.getRemoteShareName()+"/"+o.getDirectoryName());
                    	} else {
                    		holder.tv_dir_name.setText("//"+o.getRemoteHostname()+"/"+o.getRemoteShareName()+"/"+o.getDirectoryName());
                    	}
                    }
                	
                	if (!getItem(position).isProfileActive()) {
                    	holder.tv_dir_name.setEnabled(false);
                    } else {
                    	holder.tv_dir_name.setEnabled(true);
                    }
                } else {
                	holder.tv_dir_name.setVisibility(LinearLayout.GONE);
                	holder.ll_sync.setVisibility(LinearLayout.GONE);
                	holder.iv_row_icon.setVisibility(LinearLayout.GONE);
                    holder.tv_row_active.setVisibility(LinearLayout.GONE);
                    holder.cbv_row_cb1.setVisibility(LinearLayout.GONE);
                    holder.ll_last_sync.setVisibility(LinearLayout.GONE);
                }
                
                if (isShowCheckBox) holder.cbv_row_cb1.setVisibility(CheckBox.VISIBLE);
                else  holder.cbv_row_cb1.setVisibility(CheckBox.INVISIBLE);
                final int p = position;
             // 必ずsetChecked前にリスナを登録(convertView != null の場合は既に別行用のリスナが登録されている！)
             	holder.cbv_row_cb1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
     				@Override
     				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
     					if (o.getProfileType().equals("")) return;
     					o.setChecked(isChecked);
	     				items.set(p, o);
	     				if (mNotifyCheckBoxEvent!=null && isShowCheckBox) 
	     					mNotifyCheckBoxEvent.notifyToListener(true, null);
     				}
     			});
             	holder.cbv_row_cb1.setChecked(items.get(position).isChecked());
           	}
            return v;
		};
		
		static class ViewHolder {
			TextView tv_row_name,tv_row_active;
			ImageView iv_row_icon;
			CheckBox cbv_row_cb1;
			
			TextView tv_row_synctype, tv_row_master, tv_row_target;
			ImageView iv_row_sync_dir_image;
			ImageView iv_row_image_master,iv_row_image_target;
			String tv_mtype_mirror,tv_mtype_move,tv_mtype_copy;
			
			TextView tv_dir_name, tv_dir_const;
			
			TextView tv_last_sync_time, tv_last_sync_result;
			LinearLayout ll_sync, ll_entry, ll_last_sync, ll_view;
		}
}

class ProfileListItem implements Serializable,Comparable<ProfileListItem>{
	private static final long serialVersionUID = 1L;
	private String profileGroup="";
	private String profileType="";
	private String profileName="";
	private String profileActive="";
	private boolean profileChk=false;
	private String profileDir="";
	private String profileShare="";
	private String profileAddr="";
	private String profileHostname="";
	private String profilePort="";
	private String profileUser="";
	private String profilePass="";
	private String profileSyncType="";
	private String profileMasterType="";
	private String profileMasterName="";
	private String profileTargetType="";
	private String profileTargetName="";
	private String profileLocalMountPoint="";
	private boolean profileMasterDirFileProcess=true;
	private boolean profileConfirm=true;
	private boolean profileForceLastModifiedUseSmbsync=true;
	private boolean profileNotUsedLastModifiedForRemote=false;
	private ArrayList<String> profileFileFilter =new ArrayList<String>();
	private ArrayList<String> profileDirFilter =new ArrayList<String>();
	
	private String profileRetryCount="0";
	private boolean profileSyncEmptyDir=false;
	private boolean profileSyncHiddenFile=true;
	private boolean profileSyncHiddenDir=true;
	private boolean profileSyncSubDir=true;
	private boolean profileSyncUseRemoteSmallIoArea=false;
	
	private String profileLastSyncTime="";
	private int profileLastSyncResult=0;
	static public final int SYNC_STATUS_SUCCESS=SyncHistoryListItem.SYNC_STATUS_SUCCESS;
	static public final int SYNC_STATUS_CANCEL=SyncHistoryListItem.SYNC_STATUS_CANCEL;
	static public final int SYNC_STATUS_ERROR=SyncHistoryListItem.SYNC_STATUS_ERROR;
	
	private String profileLocalZipFileName="";
	private int profileLocalZipEncMethod=0, profileLocalZipAesStrength=256;
	private String profileRemoteZipFileName="";
	private int profileRemoteZipEncMethod=0, profileRemoteZipAesStrength=256;
	private String profileSyncZipFileName="";
	private int profileSyncZipEncMethod=0, profileSyncZipAesStrength=256;

	//Not save variables
	private boolean profileSyncRunning=false;
	
	// constructor for local profile
	public ProfileListItem(String pfg, String pft,String pfn, 
			String pfa, String pf_mp, String pf_dir, 
			String zip_file_name, int zip_enc_method, int zip_enc_key_length,
			boolean ic)
	{
		profileGroup=pfg;
		profileType = pft;
		profileName = pfn;
		profileActive=pfa;
		profileLocalMountPoint=pf_mp;
		profileDir=pf_dir;
		profileLocalZipFileName=zip_file_name;
		profileLocalZipEncMethod=zip_enc_method;
		profileLocalZipAesStrength=zip_enc_key_length;
		profileChk = ic;
	};
	// constructor for remote profile
	public ProfileListItem(String pfg, String pft,String pfn, String pfa, 
			String pf_user,String pf_pass,String pf_addr, String pf_hostname, 
			String pf_port, String pf_share, String pf_dir,
			String zip_file_name, int zip_enc_method, int zip_enc_key_length,
			boolean ic)
	{
		profileGroup=pfg;
		profileType = pft;
		profileName = pfn;
		profileActive=pfa;
		profileDir=pf_dir;
		profileUser=pf_user;
		profilePass=pf_pass;
		profileShare=pf_share;
		profileAddr=pf_addr;
		profilePort=pf_port;
		profileHostname=pf_hostname;
		profileRemoteZipFileName=zip_file_name;
		profileRemoteZipEncMethod=zip_enc_method;
		profileRemoteZipAesStrength=zip_enc_key_length;

		profileChk = ic;
	};
	// constructor for sync profile
	public ProfileListItem(String pfg, String pft,String pfn, String pfa,
			String pf_synctype,String pf_master_type,String pf_master_name,
			String pf_target_type,String pf_target_name,
			ArrayList<String> ff, ArrayList<String> df, boolean master_dir_file_process, boolean confirm, 
			boolean jlm, boolean nulm_remote, String retry_count, boolean sync_empty_dir, 
			boolean sync_hidden_dir, boolean sync_hidden_file, boolean sync_sub_dir, boolean sync_remote_small_ioarea,
			String zip_file_name, int zip_enc_method, int zip_enc_key_length,
			String last_sync_time, int last_sync_result,
			boolean ic)
	{
		profileGroup=pfg;
		profileType = pft;
		profileName = pfn;
		profileActive=pfa;
		profileSyncType=pf_synctype;
		profileMasterType=pf_master_type;
		profileMasterName=pf_master_name;
		profileTargetType=pf_target_type;
		profileTargetName=pf_target_name;
		profileFileFilter=ff;
		profileDirFilter=df;
		profileMasterDirFileProcess=master_dir_file_process;
		profileConfirm=confirm;
		profileForceLastModifiedUseSmbsync=jlm;
		profileChk = ic;
		profileNotUsedLastModifiedForRemote=nulm_remote;
		profileRetryCount=retry_count;
		profileSyncEmptyDir=sync_empty_dir;
		profileSyncHiddenFile=sync_hidden_file;
		profileSyncHiddenDir=sync_hidden_dir;
		profileSyncSubDir=sync_sub_dir;
		profileSyncUseRemoteSmallIoArea=sync_remote_small_ioarea;
		
		profileSyncZipFileName=zip_file_name;
		profileSyncZipEncMethod=zip_enc_method;
		profileSyncZipAesStrength=zip_enc_key_length;

		profileLastSyncTime=last_sync_time;
		profileLastSyncResult=last_sync_result;
	};

	public ProfileListItem() {}
	
	public String getProfileGroup()	{return profileGroup;}
	public String getProfileName()		{return profileName;}
	public String getProfileType()		{return profileType;}
	public String getProfileActive()	{return profileActive;}
	public boolean isProfileActive()   {return profileActive.equals(SMBSYNC_PROF_ACTIVE) ? true:false;}
	public String getRemoteUserID()	{return profileUser;}
	public String getRemotePassword() {return profilePass;}
	public String getRemoteShareName() {return profileShare;}
	public String getDirectoryName()		{return profileDir;}
	public String getRemoteAddr() {return profileAddr;}
	public String getRemotePort() {return profilePort;}
	public String getRemoteHostname() {return profileHostname;}
	public String getSyncType()	{return profileSyncType;}
	public String getMasterType(){return profileMasterType;}
	public String getMasterName(){return profileMasterName;}
	public String getTargetType(){return profileTargetType;}
	public String getTargetName(){return profileTargetName;}
	public ArrayList<String> getFileFilter()	{return profileFileFilter;}
	public ArrayList<String> getDirFilter()	{return profileDirFilter;}
	public boolean isMasterDirFileProcess()	{return profileMasterDirFileProcess;}
	public boolean isConfirmRequired()	{return profileConfirm;}
	public boolean isForceLastModifiedUseSmbsync()	{return profileForceLastModifiedUseSmbsync;}
	public boolean isChecked()		{return profileChk;}
	public boolean isNotUseLastModifiedForRemote() {return profileNotUsedLastModifiedForRemote;}
	public void setNotUseLastModifiedForRemote(boolean p) {profileNotUsedLastModifiedForRemote=p;}
	
	public void setProfileGroup(String p)		{profileGroup=p;}
	public void setProfileName(String p)		{profileName=p;}
	public void setProfileType(String p)		{profileType=p;}
	public void setProfileActive(String p)	    {profileActive=p;}
	public void setRemoteUserID(String p)		{profileUser=p;}
	public void setRemotePassword(String p)		{profilePass=p;}
	public void setRemoteShareName(String p)	    {profileShare=p;}
	public void setDirectoryName(String p)		{profileDir=p;}
	public void setRemoteAddr(String p)	{profileAddr=p;}
	public void setRemotePort(String p)	{profilePort=p;}
	public void setRemoteHostname(String p)	{profileHostname=p;}
	public void setSyncType(String p)	{profileSyncType=p;}
	public void setMasterType(String p) {profileMasterType=p;}
	public void setMasterName(String p) {profileMasterName=p;}
	public void setTargetType(String p) {profileTargetType=p;}
	public void setTargetName(String p) {profileTargetName=p;}
	public void setFileFilter(ArrayList<String> p){profileFileFilter=p;}
	public void setDirFilter(ArrayList<String> p){profileDirFilter=p;}
	public void setMasterDirFileProcess(boolean p) {profileMasterDirFileProcess=p;}
	public void setConfirmRequired(boolean p) {profileConfirm=p;}
	public void setForceLastModifiedUseSmbsync(boolean p) {profileForceLastModifiedUseSmbsync=p;}
	public void setChecked(boolean p)		{profileChk=p;}
	public void setLocalMountPoint(String p) {profileLocalMountPoint=p;}
	public String getLocalMountPoint() {return profileLocalMountPoint;}

	public String getRetryCount() {return profileRetryCount;}
	public void setRetryCount(String p) {profileRetryCount=p;}

	public boolean isSyncEmptyDirectory() {return profileSyncEmptyDir;}
	public void setSyncEmptyDirectory(boolean p) {profileSyncEmptyDir=p;}

	public boolean isSyncHiddenFile() {return profileSyncHiddenFile;}
	public void setSyncHiddenFile(boolean p) {profileSyncHiddenFile=p;}

	public boolean isSyncHiddenDirectory() {return profileSyncHiddenDir;}
	public void setSyncHiddenDirectory(boolean p) {profileSyncHiddenDir=p;}
	
	public boolean isSyncSubDirectory() {return profileSyncSubDir;}
	public void setSyncSubDirectory(boolean p) {profileSyncSubDir=p;}

	public boolean isSyncUseRemoteSmallIoArea() {return profileSyncUseRemoteSmallIoArea;}
	public void setSyncRemoteSmallIoArea(boolean p) {profileSyncUseRemoteSmallIoArea=p;}
	
	public void setLastSyncTime(String p) {profileLastSyncTime=p;} 
	public void setLastSyncResult(int p) {profileLastSyncResult=p;}
	public String getLastSyncTime() {return profileLastSyncTime;} 
	public int getLastSyncResult() {return profileLastSyncResult;}

	public void setLocalZipFileName(String p) {profileLocalZipFileName=p;}
	public void setLocalZipEncMethod(int p) {profileLocalZipEncMethod=p;}
	public void setLocalZipAesKeyLength(int p) {profileLocalZipAesStrength=p;}
	public String getLocalZipFileName() {return profileLocalZipFileName;}
	public int getLocalZipEncMethod() {return profileLocalZipEncMethod;}
	public int getLocalZipAesKeyLength() {return profileLocalZipAesStrength;}

	public void setRemoteZipFileName(String p) {profileRemoteZipFileName=p;}
	public void setRemoteZipEncMethod(int p) {profileRemoteZipEncMethod=p;}
	public void setRemoteZipAesKeyLength(int p) {profileRemoteZipAesStrength=p;}
	public String getRemoteZipFileName() {return profileRemoteZipFileName;}
	public int getRemoteZipEncMethod() {return profileRemoteZipEncMethod;}
	public int getRemoteZipAesKeyLength() {return profileRemoteZipAesStrength;}

	public void setSyncZipFileName(String p) {profileSyncZipFileName=p;}
	public void setSyncZipEncMethod(int p) {profileSyncZipEncMethod=p;}
	public void setSyncZipAesKeyLength(int p) {profileSyncZipAesStrength=p;}
	public String getSyncZipFileName() {return profileSyncZipFileName;}
	public int getSyncZipEncMethod() {return profileSyncZipEncMethod;}
	public int getSyncZipAesKeyLength() {return profileSyncZipAesStrength;}

	public void setSyncRunning(boolean p) {profileSyncRunning=p;}
	public boolean isSyncRunning() {return profileSyncRunning;}
	
	@SuppressLint("DefaultLocale")
	@Override
	public int compareTo(ProfileListItem o) {
		if(this.profileName != null)
			return this.profileName.toLowerCase(Locale.getDefault()).compareTo(o.getProfileName().toLowerCase()) ; 
//				return this.filename.toLowerCase().compareTo(o.getName().toLowerCase()) * (-1);
		else 
			throw new IllegalArgumentException();
	}
}