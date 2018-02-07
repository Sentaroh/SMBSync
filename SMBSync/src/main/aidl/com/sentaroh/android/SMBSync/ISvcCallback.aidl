package com.sentaroh.android.SMBSync;

interface ISvcCallback{ 
    void cbThreadEnded(String result_code, String result_msg);
    
    void cbShowConfirm(String fp, String copy_or_delete);
    
    void cbSendMessage(String cat, String flag, String sync_prof, 
    	String date, String time, String tag, String debug_or_msg, 
    	String fp, String msg_text);
    	
    void cbWifiStatusChanged(String status, String ssid);
}