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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.sentaroh.android.Utilities.StringUtil;
import com.sentaroh.android.Utilities.MiscUtil;

public final class LogUtil {
	static private PrintWriter mLogWriter=null;
	static private File mLogFile=null;
	private static String log_file_path="";
	
	final public static void reopenLogFile(GlobalParameters gp) {
		if (mLogWriter!=null) {
			synchronized(log_file_path) {
				closeLogFile();
				openLogFile(gp);
			}
		}
	};

	final public static void flushLogFile(GlobalParameters gp) {
		if (mLogWriter!=null) {
			synchronized(log_file_path) {
				mLogWriter.flush();
			}
		}
	};

	final static public void writeLog(String log_msg) {
		if (mLogWriter!=null) {
			synchronized(log_file_path) {
				mLogWriter.println(log_msg);
			}
		}
	};
	
    static public ArrayList<LogFileManagemntListItem> createLogFileList(GlobalParameters gp) {
    	ArrayList<LogFileManagemntListItem> lfm_fl=new ArrayList<LogFileManagemntListItem>();
    	
    	File lf=new File(gp.settingLogMsgDir);
    	File[] file_list=lf.listFiles();
    	if (file_list!=null) {
    		for (int i=0;i<file_list.length;i++) {
    			if (file_list[i].getName().startsWith("SMBSync_log")) {
    				if (file_list[i].getName().startsWith("SMBSync_log_20")) {
        		    	LogFileManagemntListItem t=new LogFileManagemntListItem();
        		    	t.log_file_name=file_list[i].getName();
        		    	t.log_file_path=file_list[i].getPath();
        		    	t.log_file_size=MiscUtil.convertFileSize(file_list[i].length());
        		    	t.log_file_last_modified=file_list[i].lastModified();
        		    	String lm_date=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(file_list[i].lastModified());
        		    	if (file_list[i].getPath().equals(getLogFilePath(gp)))
        		    		t.isCurrentLogFile=true;
        		    	t.log_file_last_modified_date=lm_date.substring(0,10);
        		    	t.log_file_last_modified_time=lm_date.substring(11);
        		    	lfm_fl.add(t);
    				}
    			}
    		}
    		Collections.sort(lfm_fl,new Comparator<LogFileManagemntListItem>(){
				@Override
				public int compare(LogFileManagemntListItem arg0,
						LogFileManagemntListItem arg1) {
					return arg1.log_file_name.compareToIgnoreCase(arg0.log_file_name);
				}
    			
    		});
    	}
    	if (lfm_fl.size()==0) {
    		LogFileManagemntListItem t=new LogFileManagemntListItem();
    		lfm_fl.add(t);
    	} else {
    		String c_lfm_date="";
    		int gen=-1;
    		for (LogFileManagemntListItem lfmli:lfm_fl) {
    			String n_lfm_date="";
    			String lfm_date_time=lfmli.log_file_name
    					.replace("SMBSync_log_", "")
    					.replace(".txt","");
    			if (lfm_date_time.indexOf("_")>=0) {
    				n_lfm_date=lfm_date_time
    						.substring(0,lfm_date_time.indexOf("_"));
    			} else {
    				n_lfm_date=lfm_date_time;
    			}
    			if (!c_lfm_date.equals(n_lfm_date)) {
    				gen++;
    				c_lfm_date=n_lfm_date;
    			} 
    			lfmli.log_file_generation=gen;
//    			Log.v("","name="+lfmli.log_file_name+", gen="+lfmli.log_file_generation);
//    			Log.v("","lfm_date_time="+lfm_date_time+", lfm_date="+n_lfm_date);
    		}
    	}
    	return lfm_fl;
    };

	final static public void closeLogFile() {
		if (mLogWriter!=null) {
			synchronized(log_file_path) {
				putLogMsg("LogUtil", "I","Log file was closed");
				mLogWriter.flush();
				mLogWriter.close(); 
				mLogWriter=null;
				mLogFile=null;
				log_file_path="";
			}
		}
	};
	
	public static void openLogFile(GlobalParameters gp) {
		if (mLogWriter==null && gp.settingLogOption.equals("1")) {
			String dt=StringUtil.convDateTimeTo_YearMonthDayHourMin(System.currentTimeMillis());
			gp.settingLogMsgFilename="SMBSync_log_"+dt.substring(0,10).replace("/","-")+".txt";
			
			houseKeepLogFile(gp);

			synchronized(log_file_path) {
				String t_fd="",fp="";
				t_fd=gp.settingLogMsgDir;
				if (t_fd.lastIndexOf("/")==(t_fd.length()-1)) {//last is "/"
					fp=t_fd+gp.settingLogMsgFilename;
				} else fp=t_fd+"/"+gp.settingLogMsgFilename;
				File lf=new File(t_fd);
				if(!lf.exists()) lf.mkdirs();
				log_file_path=fp;
				try {
					BufferedWriter bw;
					FileWriter fw ;
					mLogFile=new File(fp);
					fw=new FileWriter(mLogFile,true);
					bw = new BufferedWriter(fw,4096*32);
					mLogWriter = new PrintWriter(bw,false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			putLogMsg("LogUtil", "I","Log file was opened");
		}
	};
	
	public static String getLogFilePath(GlobalParameters gp) {
		return log_file_path;
	};
	
	private static void putLogMsg(String lid, String cat, String msg) {
		String log_id=(lid+"                        ").substring(0,13);

		String dt=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
		String log_msg="M "+cat+" "+dt.substring(0,10)+" "+dt.substring(11)+" "+ log_id+msg;
		writeLog(log_msg);
	};

	private static void houseKeepLogFile(GlobalParameters gp) {
		ArrayList<LogFileManagemntListItem>lfm_list=createLogFileList(gp);
		Collections.sort(lfm_list,new Comparator<LogFileManagemntListItem>(){
			@Override
			public int compare(LogFileManagemntListItem arg0,
					LogFileManagemntListItem arg1) {
				int result=0;
				long comp=arg0.log_file_last_modified-arg1.log_file_last_modified;
				if (comp==0) result=0;
				else if(comp<0) result=-1;
				else if(comp>0) result=1;
				return result;
			}
		});
		ArrayList<LogFileManagemntListItem>lfm_del_list= new ArrayList<LogFileManagemntListItem>();
		for (LogFileManagemntListItem lfmli:lfm_list) {
			if (lfmli.log_file_generation>=gp.settiingLogGeneration) {
				putLogMsg("LogUtil", "I","Log file was deleted, name="+lfmli.log_file_path);
				File lf=new File(lfm_list.get(0).log_file_path);
				lf.delete();
				lfm_del_list.add(lfmli);
			}
		}
		for (LogFileManagemntListItem del_lfmli:lfm_del_list) {
			lfm_list.remove(del_lfmli);
		}
	};

}
