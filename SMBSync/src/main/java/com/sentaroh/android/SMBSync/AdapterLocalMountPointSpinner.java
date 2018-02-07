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
import java.util.Collections;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sentaroh.android.Utilities.Widget.CustomSpinnerAdapter;

@SuppressLint("ViewConstructor")
public class AdapterLocalMountPointSpinner extends CustomSpinnerAdapter {

	private ArrayList<String>mpl;

	public AdapterLocalMountPointSpinner(Context context, int textViewResourceId, ArrayList<String>lmp) {
		super(context, textViewResourceId);
		mpl=lmp;
	}

	@Override
	public boolean isEnabled(int p) {
		return isMountPointAvailable(getItem(p));
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        TextView view;
        view=(TextView)super.getView(position,convertView,parent);
        return view;
	}
	
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView text;
        text=(TextView)super.getDropDownView(position, convertView, parent);
        
//		if (isMountPointAvailable(getItem(position))) {
////			text.setEnabled(true);
//			text.setTextColor(Color.WHITE);
//		} else {
////			text.setEnabled(false);
//			text.setTextColor(Color.GRAY);
//		}
//		text.setEnabled(true);
//		text.setBackgroundColor(Color.LTGRAY);
        return text;
	}
	
	private boolean isMountPointAvailable(String lurl) {
		boolean result=false;
		if (Collections.binarySearch(mpl, lurl)>=0) result=true;
//		Log.v("","size="+mpl.size()+", lu="+lurl+", result="+result);
		return result;
	};

//	public void sort() {
//		Collections.sort(mpl);
//	}
}