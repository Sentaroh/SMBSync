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

import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_REMOTE;

import com.sentaroh.android.Utilities.Widget.CustomSpinnerAdapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AdapterProfileSelectionSpinner extends CustomSpinnerAdapter {//ArrayAdapter<String> {
	
//	private int mResourceId;
	private Context mContext;
	
	private Drawable mRemoteDrawable=null, mMobileDrawable=null;

	public AdapterProfileSelectionSpinner(Context c, int textViewResourceId) {
		super(c, textViewResourceId);
//		mResourceId=textViewResourceId;
		mContext=c;
		
    	Bitmap bitmap_r = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_32_server);
    	Bitmap bitmap_m = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_32_mobile);
    	Bitmap bitmap_a = BitmapFactory.decodeResource(mContext.getResources(), android.R.drawable.arrow_down_float);
    	
    	int width_r = bitmap_r.getWidth();
    	int height_r = bitmap_r.getHeight();
    	int width_m = bitmap_m.getWidth();
    	int height_m = bitmap_m.getHeight();
    	int width_a = bitmap_a.getWidth(); 
//    	int height_a = bitmap_a.getHeight(); 
    	int spacer=(int) toPixel(mContext.getResources(),5);
//    	Log.v("","sp="+spacer);
    	Bitmap rem_bmap = Bitmap.createBitmap(width_r+spacer+width_a, height_r, Bitmap.Config.ARGB_8888);
    	Canvas rem_canvas = new Canvas(rem_bmap);
    	
    	rem_canvas.drawBitmap(bitmap_r, 0, 0, (Paint)null);
//    	int dis_width_r = width_r + bitmap_a.getWidth();
    	int dis_height_r = (height_r - bitmap_a.getHeight()) / 2;
    	rem_canvas.drawBitmap(bitmap_a, width_r+spacer, dis_height_r, (Paint)null); 

    	Bitmap mob_bmap = Bitmap.createBitmap(width_m+spacer+width_a, height_m, Bitmap.Config.ARGB_8888);
    	Canvas mob_canvas = new Canvas(mob_bmap);
    	mob_canvas.drawBitmap(bitmap_m, 0, 0, (Paint)null);
//    	int dis_width_m = width_m + bitmap_a.getWidth();
    	int dis_height_m = (height_m - bitmap_a.getHeight()) / 2;
    	mob_canvas.drawBitmap(bitmap_a, width_m+spacer, dis_height_m, (Paint)null); 

    	mRemoteDrawable=new BitmapDrawable(mContext.getResources(),rem_bmap);

    	mMobileDrawable=new BitmapDrawable(mContext.getResources(),mob_bmap);
    	
//    	rem_bmap.recycle();
//    	mob_bmap.recycle();
    	bitmap_a.recycle();
    	bitmap_r.recycle();
    	bitmap_m.recycle();
	}
	
	final static private float toPixel(Resources res, int dip) {
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, res.getDisplayMetrics());
		return px;
	};

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        TextView view;
        if (convertView == null) {
//        	LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//          view = (TextView)vi.inflate(mResourceId, null);
          view=(TextView)super.getView(position,convertView,parent);
        } else {
            view = (TextView)convertView;
        }
        String type=getItem(position).substring(0, 1);
        String name=getItem(position).substring(2);
        view.setText(name);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setCompoundDrawablePadding(10);
        if (type.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
            view.setCompoundDrawablesWithIntrinsicBounds(mRemoteDrawable,null,null,null);
//            view.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(android.R.drawable.arrow_down_float),
//            		null,
//            		mContext.getResources().getDrawable(R.drawable.ic_32_server), 
//            		null);
        } else {
        	view.setCompoundDrawablesWithIntrinsicBounds(mMobileDrawable,null,null,null);
//            view.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(android.R.drawable.arrow_down_float),
//            		null,
//            		mContext.getResources().getDrawable(R.drawable.ic_32_mobile), 
//            		null);
        }

//        if (text_color!=0) view.setTextColor(text_color);
//        if (text_size!=0) view.setTextSize(text_size);

        return view;
	}
	@SuppressWarnings("deprecation")
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView text;
        text=(TextView)super.getDropDownView(position, convertView, parent);
//        text.setBackgroundColor(Color.LTGRAY);
        String type=getItem(position).substring(0, 1);
        String name=getItem(position).substring(2);
        text.setText(name);
        if (Build.VERSION.SDK_INT>=11) {
            if (type.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
            	text.setCompoundDrawablePadding(10);
//            	text.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(android.R.drawable.btn_radio),
//                		null,
//                		mContext.getResources().getDrawable(R.drawable.ic_32_server), 
//                		null);
            	text.setCompoundDrawablesWithIntrinsicBounds(
            			mContext.getResources().getDrawable(R.drawable.ic_32_server),
                		null,
                		mContext.getResources().getDrawable(android.R.drawable.btn_radio),
                		null);
            } else {
            	text.setCompoundDrawablePadding(10);
//            	text.setCompoundDrawablesWithIntrinsicBounds(
//            			mContext.getResources().getDrawable(android.R.drawable.btn_radio),
//                		null,
//                		mContext.getResources().getDrawable(R.drawable.ic_32_mobile), 
//                		null);
            	text.setCompoundDrawablesWithIntrinsicBounds(
            			mContext.getResources().getDrawable(R.drawable.ic_32_mobile),
                		null,
                		mContext.getResources().getDrawable(android.R.drawable.btn_radio),
                		null);
            }
        } else {
            if (type.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
            	text.setCompoundDrawablePadding(10);
            	text.setCompoundDrawablesWithIntrinsicBounds(
            			mContext.getResources().getDrawable(R.drawable.ic_32_server),
            			null,
                		null,
                		null);
            } else {
            	text.setCompoundDrawablePadding(10);
            	text.setCompoundDrawablesWithIntrinsicBounds(
            			mContext.getResources().getDrawable(R.drawable.ic_32_mobile),
            			null,
                		null,
                		null);
            }
        }

//        if (text_color!=0) text.setTextColor(text_color);
//        if (text_size!=0) text.setTextSize(text_size);
        
        return text;
	}
	
}
