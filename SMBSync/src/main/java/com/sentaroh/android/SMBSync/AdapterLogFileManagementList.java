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
import com.sentaroh.android.Utilities.ThemeColorList;
import com.sentaroh.android.Utilities.ThemeUtil;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class AdapterLogFileManagementList extends BaseAdapter{

	private ArrayList<LogFileManagemntListItem>log_list=null;
	private int textViewResourceId=0;
	private Context c;
	
	private NotifyEvent mCheckBoxClickListener=null;
	private ThemeColorList mThemeColorList;
	public AdapterLogFileManagementList(Context context, int textViewResourceId,
			ArrayList<LogFileManagemntListItem> objects, NotifyEvent ntfy) {
		c=context;
		log_list=objects;
		mCheckBoxClickListener=ntfy;
		this.textViewResourceId=textViewResourceId;
		if (isAnyItemSelected()) setShowCheckBox(true);
		mThemeColorList=ThemeUtil.getThemeColorList(context);
	}
	
	public void replaceDataList(ArrayList<LogFileManagemntListItem> dl) {
		log_list=dl;
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return log_list.size();
	}

	@Override
	public LogFileManagemntListItem getItem(int pos) {
		return log_list.get(pos);
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}

	private boolean mShowCheckBox=false;
	public void setShowCheckBox(boolean p) {mShowCheckBox=p;}
	public boolean isShowCheckBox() {return mShowCheckBox;}
	
	public boolean isEmptyAdapter() {
		boolean result=true;
		if (log_list!=null) {
			if (log_list.size()>0) {
				if (log_list.get(0).log_file_name!=null) result=false;
			}
		}
		return result;
	};
	
	public boolean isAnyItemSelected() {
		boolean result=false;
		if (log_list!=null) {
			for(int i=0;i<log_list.size();i++) {
				if (log_list.get(i).isChecked) {
					result=true;
					break;
				}
			}
		}
		return result;
	};

	public int getItemSelectedCount() {
		int result=0;
		if (log_list!=null) {
			for(int i=0;i<log_list.size();i++) {
				if (log_list.get(i).isChecked) {
					result++;
				}
			}
		}
		return result;
	};

	public void setAllItemChecked(boolean p) {
		if (log_list!=null) {
			for(int i=0;i<log_list.size();i++) {
				log_list.get(i).isChecked=p;
			}
		}
		notifyDataSetChanged();
	};
	
	
	@Override
    final public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(textViewResourceId, null);
            holder=new ViewHolder();
            holder.cb_select=(CheckBox)v.findViewById(R.id.log_management_list_item_checkbox);
            holder.tv_log_file_name=(TextView)v.findViewById(R.id.log_management_list_item_log_name);
            holder.tv_log_file_size=(TextView)v.findViewById(R.id.log_management_list_item_log_size);
            holder.tv_log_file_date=(TextView)v.findViewById(R.id.log_management_list_item_log_last_modified_date);
            holder.tv_log_file_time=(TextView)v.findViewById(R.id.log_management_list_item_log_last_modified_time);
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        final LogFileManagemntListItem o = getItem(position);
        if (o.log_file_name!=null) {
        	if (o.isCurrentLogFile) holder.tv_log_file_name.setTextColor(mThemeColorList.text_color_error);
        	else {
        		holder.tv_log_file_name.setTextColor(mThemeColorList.text_color_primary);
        	}
//        	TypedValue outValue = new TypedValue();
//        	c.getTheme().resolveAttribute(android.R.attr.textColorPrimary, outValue, true);
//        	holder.tv_log_file_name.setTextColor(outValue.resourceId);
        	if (o.log_file_generation>9) holder.tv_log_file_name.setText(""+o.log_file_generation+"\u00a0 "+o.log_file_name);
        	else holder.tv_log_file_name.setText("0"+o.log_file_generation+"\u00a0"+o.log_file_name);
    		holder.tv_log_file_size.setText(o.log_file_size);
    		holder.tv_log_file_date.setText(o.log_file_last_modified_date);
    		holder.tv_log_file_time.setText(o.log_file_last_modified_time);
    		if (mShowCheckBox) holder.cb_select.setVisibility(CheckBox.VISIBLE);
    		else holder.cb_select.setVisibility(CheckBox.INVISIBLE);
         	holder.cb_select.setOnCheckedChangeListener(new OnCheckedChangeListener() {
    			@Override
    			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//    				o.isChecked=isChecked;
    				getItem(position).isChecked=isChecked;
    				if (mCheckBoxClickListener!=null && mShowCheckBox) 
    					mCheckBoxClickListener.notifyToListener(true, new Object[]{isChecked});
    			}
    		});
         	holder.cb_select.setChecked(getItem(position).isChecked);
        } else {
    		holder.tv_log_file_name.setText(c.getString(R.string.msgs_log_management_no_log_file));
    		holder.cb_select.setVisibility(TextView.GONE);
    		holder.tv_log_file_size.setVisibility(TextView.GONE);
    		holder.tv_log_file_date.setVisibility(TextView.GONE);
    		holder.tv_log_file_time.setVisibility(TextView.GONE);
        }
        return v;
	};

	static class ViewHolder {
		CheckBox cb_select;
		TextView tv_log_file_name,tv_log_file_size,tv_log_file_date, tv_log_file_time;
	}
}
class LogFileManagemntListItem {
	public boolean isChecked=false;
	public boolean isCurrentLogFile=false;
	public int log_file_generation=-1;
	public String log_file_name=null;
	public String log_file_path=null;
	public String log_file_size=null;
	public long log_file_last_modified=0;
	public String log_file_last_modified_date="";
	public String log_file_last_modified_time="";
}