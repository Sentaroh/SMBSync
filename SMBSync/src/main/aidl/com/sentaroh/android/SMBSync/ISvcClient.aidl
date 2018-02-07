package com.sentaroh.android.SMBSync;

import com.sentaroh.android.SMBSync.ISvcCallback;

interface ISvcClient{
	
	void setCallBack(ISvcCallback callback);
	void removeCallBack(ISvcCallback callback);

	void aidlStartThread() ;
	void aidlCancelThread() ;
	void aidlStartForeground() ;
	void aidlStopForeground(boolean clear_notification) ;
	
	void aidlConfirmResponse(int confirmed) ;
	
	void aidlStopService() ;
	
	void aidlShowNotificationMsg(String prof, String fp, String msg);
	
//	void aidlSetNotificationIcon(int icon_res);
	
	void aidlAcqWakeLock();
	
	void aidlRelWakeLock();
	
}