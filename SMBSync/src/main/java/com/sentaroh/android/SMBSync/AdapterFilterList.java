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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

class FilterListItem implements Comparable<FilterListItem> {

	private String filter;
	private boolean inc;// false:Exclude, true: Include
	private boolean deleted=false;
	
	public FilterListItem (String f, boolean i) {
		filter=f;
		inc=i;
		deleted=false;
	}
	
	public String getFilter() {return filter;}
	public void setFilter(String p) {filter=p;}
	public boolean getInc() {return inc;}
	public void setInc(boolean p) {inc=p;}
	
	public boolean isDeleted() {return deleted;}
	public void delete() {deleted=true;}

	
	@Override
	public int compareTo(FilterListItem o) {
		if(this.filter != null)
			return this.filter.toLowerCase().compareTo(o.getFilter().toLowerCase()) ; 
//				return this.filename.toLowerCase().compareTo(o.getName().toLowerCase()) * (-1);
		else 
			throw new IllegalArgumentException();
	}
}


public class AdapterFilterList extends ArrayAdapter<FilterListItem> {
	private Context c;
	private int id;
	private ArrayList<FilterListItem> items;
	
	public AdapterFilterList(Context context, int textViewResourceId,
			ArrayList<FilterListItem> objects) {
		super(context, textViewResourceId, objects);
		c=context;
		id=textViewResourceId;
		items=objects;
	}

	public FilterListItem getItem(int i) {
		 return items.get(i);
	}
	public void remove(int i) {
		items.remove(i);
		notifyDataSetChanged();
	}
	public void replace(FilterListItem fli, int i) {
		items.set(i, fli);
		notifyDataSetChanged();
	}
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
            holder=new ViewHolder();
//            holder.ll_entry=(LinearLayout) v.findViewById(R.id.filter_list_item_entry);
            holder.btn_row_delbtn= (Button) v.findViewById(R.id.filter_list_item_del_btn);
            holder.tv_row_filter= (TextView) v.findViewById(R.id.filter_list_item_filter);
            
            holder.rb_grp=(RadioGroup) v.findViewById(R.id.filter_list_item_rbgrp);
            holder.rb_inc=(RadioButton) v.findViewById(R.id.filter_list_item_rb_inc);
            holder.rb_exc=(RadioButton) v.findViewById(R.id.filter_list_item_rb_exc);
            
            holder.del_msg=c.getString(R.string.msgs_filter_list_filter_deleted);
            
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        final FilterListItem o = getItem(position);

        if (o != null ) {
       		holder.tv_row_filter.setText(o.getFilter());
       		holder.tv_row_filter.setVisibility(View.VISIBLE);
       		if (o.getFilter().startsWith("---")) {
       			holder.btn_row_delbtn.setVisibility(View.GONE);
       			holder.rb_grp.setVisibility(View.GONE);
       		} else {
       			holder.btn_row_delbtn.setVisibility(View.VISIBLE);
       			holder.rb_grp.setVisibility(View.VISIBLE);
       		}
       		
       		if (o.isDeleted()) {
       			holder.tv_row_filter.setEnabled(false);
       			holder.btn_row_delbtn.setEnabled(false);
       			holder.rb_inc.setEnabled(false);
       			holder.rb_exc.setEnabled(false);
	       		holder.tv_row_filter.setText(
	       				holder.del_msg+" : "+o.getFilter());
       		} else {
       			holder.tv_row_filter.setEnabled(true);
       			holder.btn_row_delbtn.setEnabled(true);
       			holder.rb_inc.setEnabled(true);
       			holder.rb_exc.setEnabled(true);
       		}
//       		Log.v("","filter="+o.getFilter()+",incexc="+o.getInc());
       		
            final int p = position;
         // 必ずsetChecked前にリスナを登録(convertView != null の場合は既に別行用のリスナが登録されている！)
         	holder.btn_row_delbtn.setOnClickListener(new OnClickListener() {
 				@Override
 				public void onClick(View view) {
 					o.delete();
 					items.set(p,o);
 					
 					holder.tv_row_filter.setEnabled(false);
 	       			holder.btn_row_delbtn.setEnabled(false);
 	       			holder.rb_inc.setEnabled(false);
 	       			holder.rb_exc.setEnabled(false);
 	       			holder.tv_row_filter.setText(
 	       					holder.del_msg+" : "+o.getFilter());
 					
// 	       			items.remove(p);
// 	       			notifyDataSetChanged();
 				}

 			});
         	holder.rb_inc.setOnClickListener(new OnClickListener() {
 				@Override
 				public void onClick(View v) {
 					o.setInc(true);
 					items.set(p, o);
// 					Log.v("","cb i filter="+o.getFilter()+",incexc="+o.getInc());
 				}
 			});
         	holder.rb_exc.setOnClickListener(new OnClickListener() {
 				@Override
 				public void onClick(View v) {
 					o.setInc(false);
 					items.set(p, o);
// 					Log.v("","cb i filter="+o.getFilter()+",incexc="+o.getInc());
 				}
 			});
         	if (o.getInc()) holder.rb_inc.setChecked(true);
         	else holder.rb_exc.setChecked(true);
       	}

   		return v;
	};
	
	static class ViewHolder {
		TextView tv_row_filter,tv_row_cat,tv_row_incExc;
		Button btn_row_delbtn;
//		EditText et_filter;
		RadioButton rb_inc,rb_exc;
		RadioGroup rb_grp;
//		LinearLayout ll_entry;
		String del_msg;
		
	}

}
