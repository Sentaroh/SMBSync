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

import java.io.File;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

public class NotificationUtil {
	
	@SuppressWarnings("deprecation")
	static final public void initNotification(GlobalParameters gwa) {
		gwa.notificationManager=(NotificationManager) gwa.appContext.getSystemService(Context.NOTIFICATION_SERVICE);
		gwa.notification=new Notification(R.drawable.ic_32_smbsync,
    			gwa.appContext.getString(R.string.app_name),0);

		gwa.notificationAppName=gwa.appContext.getString(R.string.app_name);
		
		gwa.notificationIntent = new Intent(gwa.appContext,SMBSyncMain.class);
		gwa.notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		gwa.notificationIntent.setAction(Intent.ACTION_MAIN);
		gwa.notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		gwa.notificationPendingIntent =PendingIntent.getActivity(gwa.appContext, 0, gwa.notificationIntent,
    					PendingIntent.FLAG_UPDATE_CURRENT);

		gwa.notificationBuilder=new NotificationCompat.Builder(gwa.appContext);
		gwa.notificationBuilder.setContentIntent(gwa.notificationPendingIntent)
//		   	.setTicker(gwa.notificationAppName)
			.setOngoing(true)
			.setAutoCancel(false)
			.setSmallIcon(gwa.notificationIcon)//smbsync_animation)
			.setContentTitle(gwa.notificationAppName)
			.setContentText("")
//		    .setSubText("subtext")
//		    .setLargeIcon(largeIcon)
			.setWhen(0)
//			.addAction(action_icon, action_title, action_pi)
			;
		gwa.notification=gwa.notificationBuilder.build();
		if (Build.VERSION.SDK_INT>=16) {//JB(4.1以上
			gwa.notificationBigTextStyle = 
   		   			new NotificationCompat.BigTextStyle(gwa.notificationBuilder);
			gwa.notificationBigTextStyle
				.setBigContentTitle(gwa.notificationLastShowedTitle)
				.bigText(gwa.notificationLastShowedMessage);
		}

	};

	static final public void setNotificationIcon(GlobalParameters gwa,
			int notification_icon) {
		gwa.notificationIcon=notification_icon;
		gwa.notificationBuilder.setContentIntent(gwa.notificationPendingIntent)
	   		.setSmallIcon(gwa.notificationIcon)//smbsync_animation)
	   		;
		gwa.notification=gwa.notificationBuilder.build();
		if (Build.VERSION.SDK_INT>=16) {//JB(4.1以上
			gwa.notificationBigTextStyle = 
   		   			new NotificationCompat.BigTextStyle(gwa.notificationBuilder);
			gwa.notificationBigTextStyle
				.setBigContentTitle(gwa.notificationLastShowedTitle)
				.bigText(gwa.notificationLastShowedMessage);
		}
	};

	final static public Notification getNotification(GlobalParameters gwa) {
		return gwa.notification;
	};
	
	final static public void setNotificationMessage(GlobalParameters gwa,  
			String prof, String fp, String msg) {
		if (prof.equals("")) gwa.notificationLastShowedTitle=gwa.notificationAppName;
		else gwa.notificationLastShowedTitle=prof;//gwa.notificationAppName+"       "+prof;
		
		if (fp.equals("")) gwa.notificationLastShowedMessage=msg;
		else gwa.notificationLastShowedMessage=fp+" "+msg;
	};
	
	final static public Notification showOngoingMsg(GlobalParameters gwa, long when,
			String msg ) {
		return showOngoingMsg(gwa,when,"","",msg);
	};

	final static public Notification showOngoingMsg(GlobalParameters gwa, long when,
			String prof, String msg ) {
		return showOngoingMsg(gwa,when,prof,"",msg);
	};
	
	@SuppressWarnings("deprecation")
	final static public Notification showOngoingMsg(GlobalParameters gwa, long when,
			String prof, String fp, String msg ) {
		setNotificationMessage(gwa,prof,fp,msg);
//		if (gwa.notificationNextShowedTime<=System.currentTimeMillis()) {
//			gwa.notificationNextShowedTime=System.currentTimeMillis()+500;
			gwa.notificationBuilder
				.setContentTitle(gwa.notificationLastShowedTitle)
			    .setContentText(gwa.notificationLastShowedMessage)
			    ;
			if (when!=0) gwa.notificationBuilder.setWhen(when);
			if (Build.VERSION.SDK_INT>=16) {//JB
				gwa.notificationBigTextStyle
					.setBigContentTitle(gwa.notificationLastShowedTitle)
					.bigText(gwa.notificationLastShowedMessage);
				gwa.notification=gwa.notificationBigTextStyle.build();
			} else {
				gwa.notification=gwa.notificationBuilder.build();
				gwa.notification.icon=gwa.notificationIcon;
			}
			gwa.notificationManager.notify(R.string.app_name,gwa.notification);
//		}

    	return gwa.notification;
	};

	final static public void showNoticeMsg(Context context, GlobalParameters gwa, String msg ) {
		clearNotification(gwa);
		NotificationCompat.Builder builder=new NotificationCompat.Builder(context);
		builder
//			.setTicker(gwa.notificationAppName)
			.setOngoing(false)
			.setAutoCancel(true)
			.setSmallIcon(R.drawable.ic_48_smbsync_wait)
			.setContentTitle(context.getString(R.string.app_name))
			.setContentText(msg)
			.setWhen(System.currentTimeMillis())
//			.addAction(action_icon, action_title, action_pi)
			;

		boolean valid_log_file_exists=false;
		if (!LogUtil.getLogFilePath(gwa).equals("") && !gwa.settingLogOption.equals("0")) {
			File lf=new File(LogUtil.getLogFilePath(gwa));
			if (lf.exists()) valid_log_file_exists=true;
		}
		if (valid_log_file_exists) {
			Intent br_log_intent = new Intent(android.content.Intent.ACTION_VIEW);
			br_log_intent.setDataAndType(Uri.parse("file://"+LogUtil.getLogFilePath(gwa)), "text/plain");
			PendingIntent br_log_pi=PendingIntent.getActivity(context, 0, br_log_intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			builder.setContentIntent(br_log_pi);
		} else {
			Intent dummy_intent = new Intent(context,SMBSyncMain.class);
			PendingIntent dummy_pi =PendingIntent.getActivity(context, 0, dummy_intent,
	    					PendingIntent.FLAG_UPDATE_CURRENT);
			dummy_pi.cancel();
			builder.setContentIntent(dummy_pi);
		}
		
		gwa.notificationManager.notify(R.string.app_name,builder.build());
	};

	final static public void clearNotification(GlobalParameters gwa) {
		gwa.notificationManager.cancelAll();
	};

}
