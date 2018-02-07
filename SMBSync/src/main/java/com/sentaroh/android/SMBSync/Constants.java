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

public class Constants {

	public final static String APPLICATION_TAG="SMBSync";
	public static final String PACKAGE_NAME="com.sentaroh.android.SMBSync";
	
//	final public static int MAX_DLG_BOX_SIZE_WIDTH=600; 
//	final public static int MAX_DLG_BOX_SIZE_HEIGHT=800;
	
	public final static boolean BUILD_FOR_AMAZON=false;
	
	public final static String SMBSYNC_TAB_NAME_PROF="prof", SMBSYNC_TAB_NAME_MSG="msg", SMBSYNC_TAB_NAME_HIST="hist";
	
	public final static String SMBSYNC_MIRROR_THREAD_RESULT_SUCCESS="OK", SMBSYNC_MIRROR_THREAD_RESULT_ERROR="ERROR",
			SMBSYNC_MIRROR_THREAD_RESULT_CANCELLED="CANCELLED";
			
	public static final String SMBSYNC_CONFIRM_REQUEST_COPY="Copy";
	public static final String SMBSYNC_CONFIRM_REQUEST_DELETE="Delete";
	public static final int SMBSYNC_CONFIRM_RESP_YES = 1;
	public static final int SMBSYNC_CONFIRM_RESP_YESALL = 2;
	public static final int SMBSYNC_CONFIRM_RESP_NO = -1;
	public static final int SMBSYNC_CONFIRM_RESP_NOALL = -2;
	
	public final static String SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_OFF="0";
	public final static String SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_ON="1";
	public final static String SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP="2";
	public final static String SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_SPEC_AP="3";
	
	public final static String SMBSYNC_PROFILE_FILE_NAME_V0="profile.txt";
	public final static String SMBSYNC_PROFILE_FILE_NAME_V1="profile_v1.txt";
	public final static String SMBSYNC_PROFILE_FILE_NAME_V2="profile_v2.txt";
	public final static String SMBSYNC_PROFILE_FILE_NAME_V3="profile_v3.txt";
	public final static String SMBSYNC_PROFILE_FILE_NAME_V4="profile_v4.txt";
	public final static String SMBSYNC_PROFILE_FILE_NAME_V5="profile_v5.txt";
	public final static String SMBSYNC_PROFILE_FILE_NAME_V6="profile_v6.txt";
	public final static String SMBSYNC_PROFILE_FILE_NAME_V7="profile_v7.txt";
	public final static String SMBSYNC_PROFILE_FILE_NAME_V8="profile_v8.txt";

	public final static String SMBSYNC_PROF_VER1="PROF 1";
	public final static String SMBSYNC_PROF_VER2="PROF 2";
	public final static String SMBSYNC_PROF_VER3="PROF 3";
	public final static String SMBSYNC_PROF_VER4="PROF 4";
	public final static String SMBSYNC_PROF_VER5="PROF 5";
	public final static String SMBSYNC_PROF_VER6="PROF 6";
	public final static String SMBSYNC_PROF_VER7="PROF 7";
	public final static String SMBSYNC_PROF_VER8="PROF 8";
	public final static String SMBSYNC_PROF_ENC="ENC";
	public final static String SMBSYNC_PROF_DEC="DEC";
	
	public final static String CURRENT_SMBSYNC_PROFILE_FILE_NAME=SMBSYNC_PROFILE_FILE_NAME_V8;
	public final static String CURRENT_SMBSYNC_PROFILE_VERSION=SMBSYNC_PROF_VER8;
	
	public final static String SMBSYNC_SERIALIZABLE_FILE_NAME="serial.txt";
	public final static String SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V1=".SMBSync_lastmod_holder.";
	public final static String SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V2=".SMBSync_lastmod_V2";
	public final static String SMBSYNC_LOCAL_FILE_LAST_MODIFIED_NAME_V3=".SMBSync_lastmod_V3";
	public final static String SMBSYNC_LOCAL_FILE_LAST_MODIFIED_WAS_FORCE_LASTEST="*";

	public final static String SMBSYNC_SCHEDULER_ID="SchedulerID";
	public final static String SMBSYNC_EXTRA_PARM_AUTO_START="AutoStart";
	public final static String SMBSYNC_EXTRA_PARM_AUTO_TERM="AutoTerm";
	public final static String SMBSYNC_EXTRA_PARM_BACKGROUND_EXECUTION="Background";
	public final static String SMBSYNC_EXTRA_PARM_STARTUP_PARMS="StartupParms";
	public final static String SMBSYNC_EXTRA_PARM_SYNC_PROFILE="SyncProfile";
	
	public final static String SMBSYNC_PROF_GROUP_DEFAULT="Default";
	
	public final static String SMBSYNC_PROF_TYPE_SYNC="S";
	public final static String SMBSYNC_PROF_TYPE_LOCAL="L";
	public final static String SMBSYNC_PROF_TYPE_REMOTE="R";
	public final static String SMBSYNC_PROF_ACTIVE="A";
	public final static String SMBSYNC_PROF_INACTIVE="I";
	public final static String SMBSYNC_PROF_TYPE_SETTINGS="SETTINGS";
	
	public final static String SMBSYNC_SYNC_TYPE_MIRROR="M";
	public final static String SMBSYNC_SYNC_TYPE_COPY="C";
	public final static String SMBSYNC_SYNC_TYPE_SYNC="S";
	public final static String SMBSYNC_SYNC_TYPE_MOVE="X";
	
	public final static String SMBSYNC_SETTINGS_TYPE_STRING="S";
	public final static String SMBSYNC_SETTINGS_TYPE_BOOLEAN="B";
	public final static String SMBSYNC_SETTINGS_TYPE_INT="I";
	public final static String SMBSYNC_SETTINGS_TYPE_LONG="L";
	
	public final static String SMBSYNC_PROF_FILTER_INCLUDE="I";
	public final static String SMBSYNC_PROF_FILTER_EXCLUDE="E";
	
	public static final String SMBSYNC_BG_TERM_NOTIFY_MSG_ALWAYS = GlobalParameters.BG_TERM_NOTIFY_MSG_ALWAYS;
	public static final String SMBSYNC_BG_TERM_NOTIFY_MSG_ERROR = GlobalParameters.BG_TERM_NOTIFY_MSG_ERROR;
	public static final String SMBSYNC_BG_TERM_NOTIFY_MSG_NO = GlobalParameters.BG_TERM_NOTIFY_MSG_NO;

	public static final String SMBSYNC_PB_RINGTONE_NOTIFICATION_ALWAYS = GlobalParameters.PB_RINGTONE_WHEN_SYNC_ENDED_ALWAYS;
	public static final String SMBSYNC_PB_RINGTONE_NOTIFICATION_SUCCESS = GlobalParameters.PB_RINGTONE_WHEN_SYNC_ENDED_SUCCESS;
	public static final String SMBSYNC_PB_RINGTONE_NOTIFICATION_ERROR = GlobalParameters.PB_RINGTONE_WHEN_SYNC_ENDED_ERROR;
	public static final String SMBSYNC_PB_RINGTONE_NOTIFICATION_NO = GlobalParameters.PB_RINGTONE_WHEN_SYNC_ENDED_NONE;

	public static final String SMBSYNC_VIBRATE_WHEN_SYNC_ENDED_ALWAYS = GlobalParameters.VIBRATE_WHEN_SYNC_ENDED_ALWAYS;
	public static final String SMBSYNC_VIBRATE_WHEN_SYNC_ENDED_SUCCESS = GlobalParameters.VIBRATE_WHEN_SYNC_ENDED_SUCCESS;
	public static final String SMBSYNC_VIBRATE_WHEN_SYNC_ENDED_ERROR = GlobalParameters.VIBRATE_WHEN_SYNC_ENDED_ERROR;
	public static final String SMBSYNC_VIBRATE_WHEN_SYNC_ENDED_NO = GlobalParameters.VIBRATE_WHEN_SYNC_ENDED_NONE;
	
	public static final String SMBSYNC_PROFILE_CONFIRM_COPY_DELETE = "system_nr_profile_confirm_copy_delete";
	public static final String SMBSYNC_PROFILE_CONFIRM_COPY_DELETE_NOT_REQUIRED = "NOT_REQUIRED";
	public static final String SMBSYNC_PROFILE_CONFIRM_COPY_DELETE_REQUIRED = "REQUIRED";

	public static final String SMBSYNC_PROFILE_2_CONFIRM_COPY_DELETE = "system_nr_profile_2_confirm_copy_delete";
	public static final String SMBSYNC_PROFILE_2_CONFIRM_COPY_DELETE_NOT_REQUIRED = "NOT_REQUIRED";
	public static final String SMBSYNC_PROFILE_2_CONFIRM_COPY_DELETE_REQUIRED = "REQUIRED";

	public static final String SMBSYNC_PROFILE_RETRY_COUNT="3";
	
	public static final String SMBSYNC_USER_LOCAL_MOUNT_POINT_LIST_KEY="settings_additional_local_mount_point_list";

}
