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

import java.util.ArrayList;

import com.sentaroh.android.Utilities.NotifyEvent;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class AdapterLocalFileLastModifiedMaintList extends ArrayAdapter<LocalFileLastModifiedMaintListItem>{

	private ArrayList<LocalFileLastModifiedMaintListItem>items=null;
	private int id=0;
	private Context c;
	@SuppressWarnings("unused")
	private NotifyEvent mNtfyCheckBox=null;
	
	public AdapterLocalFileLastModifiedMaintList(Context context, 
			int textViewResourceId,
			ArrayList<LocalFileLastModifiedMaintListItem> objects) {
		super(context, textViewResourceId, objects);
		c=context;
		id=textViewResourceId;
		items=objects;
	}
	
	public void setNotifyCheckBoxListener(NotifyEvent ntfy) {
		mNtfyCheckBox=ntfy;
	}

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
            holder=new ViewHolder();
            
            holder.cb_cb1=(CheckBox)v.findViewById(R.id.maint_last_modified_list_dlg_listview_item_checkbox);
            holder.tv_prof_name=(TextView)v.findViewById(R.id.maint_last_modified_list_dlg_listview_item_syncprof);
            holder.tv_prof_status=(TextView)v.findViewById(R.id.maint_last_modified_list_dlg_listview_item_status);
            
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        final LocalFileLastModifiedMaintListItem o = getItem(position);
        if (o != null) {
//            final int p = position;
            holder.tv_prof_name.setText(o.getLocalMountPoint());
            holder.tv_prof_status.setText(o.getStatus());
         	if (!o.getLocalMountPoint().equals(
         			c.getString(R.string.msgs_local_file_modified_maint_no_entry))) {
                //必ずsetChecked前にリスナを登録(convertView != null の場合は既に別行用のリスナが登録されている！)
         		holder.cb_cb1.setClickable(false);
         		holder.cb_cb1.setEnabled(false);
//             	holder.cb_cb1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//     				@Override
//     				public void onCheckedChanged(CompoundButton buttonView,
//    						boolean isChecked) {
//     					o.setChecked(isChecked);
//     					if (mNtfyCheckBox!=null) mNtfyCheckBox.notifyToListener(true, new Object[]{p,isChecked});
//     					Log.v("","pos="+p);
//     				}
//     			});
             	holder.cb_cb1.setChecked(items.get(position).isChecked());
         	} else {
         		holder.tv_prof_status.setVisibility(TextView.GONE);
         		holder.cb_cb1.setVisibility(TextView.GONE);
         	}
        }         			
         	
        return v;
	};
	
	static class ViewHolder {
		TextView tv_prof_name,tv_prof_status;
		CheckBox cb_cb1;
	};
}
class LocalFileLastModifiedMaintListItem {
	private String localMountPoint="";
	private String listStatus="";
	private boolean checked=false;
	
	LocalFileLastModifiedMaintListItem(String prof, String st, boolean ic) {
		localMountPoint=prof;
		listStatus=st;
		checked=ic;
	}
	
	public String getLocalMountPoint() {return localMountPoint;}
	public String getStatus() {return listStatus;}
	public boolean isChecked() {return checked;}
	public void setStatus(String p) {listStatus=p;}
	public void setChecked(boolean p) {checked=p;}
	
}