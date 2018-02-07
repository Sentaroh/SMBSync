package com.sentaroh.android.SMBSync;

public class SchedulerConstants {
	public static final String SCHEDULER_SCHEDULE_ENABLED_KEY="scheduler_schedule_enabled_key";
	public static final String SCHEDULER_SCHEDULE_TYPE_KEY="scheduler_schedule_type_key";
	public static final String SCHEDULER_SCHEDULE_TYPE_EVERY_HOURS="H";
	public static final String SCHEDULER_SCHEDULE_TYPE_EVERY_DAY="D";
	public static final String SCHEDULER_SCHEDULE_TYPE_DAY_OF_THE_WEEK="W";
	public static final String SCHEDULER_SCHEDULE_TYPE_INTERVAL="I";
	
	public static final String SCHEDULER_SCHEDULE_LAST_EXEC_TIME_KEY="scheduler_schedule_last_exec_time_key";
	
	public static final String SCHEDULER_SCHEDULE_HOURS_KEY="scheduler_schedule_hours_key";
	public static final String SCHEDULER_SCHEDULE_MINUTES_KEY="scheduler_schedule_minutes_key";
	public static final String SCHEDULER_SCHEDULE_DAY_OF_THE_WEEK_KEY="scheduler_schedule_day_of_the_week_key";
	
	public static final String SCHEDULER_INTENT_TIMER_EXPIRED="com.sentaroh.android.SMBSync.ACTION_TIMER_EXPIRED";
	public static final String SCHEDULER_INTENT_SET_TIMER="com.sentaroh.android.SMBSync.ACTION_SET_TIMER";
	public static final String SCHEDULER_INTENT_SET_TIMER_IF_NOT_SET="com.sentaroh.android.SMBSync.ACTION_SET_TIMER_IF_NOT_SET";
	public static final String SCHEDULER_INTENT_WIFI_OFF="com.sentaroh.android.SMBSync.ACTION_SET_WIFI_OFF";
	
	public static final String SCHEDULER_SYNC_PROFILE_KEY="scheduler_sync_profile_key";
	public static final String SCHEDULER_SYNC_OPTION_AUTOSTART_KEY="scheduler_sync_option_autostart_key";
	public static final String SCHEDULER_SYNC_OPTION_AUTOTERM_KEY="scheduler_sync_option_autoterm_key";
	public static final String SCHEDULER_SYNC_OPTION_BGEXEC_KEY="scheduler_sync_option_bgexec_key";
	
	public static final String SCHEDULER_SYNC_WIFI_ON_BEFORE_SYNC_START_KEY="scheduler_sync_wifi_on_before_sync_start_key";
	public static final String SCHEDULER_SYNC_WIFI_OFF_AFTER_SYNC_END_KEY="scheduler_sync_wifi_off_after_sync_end_key";
	public static final String SCHEDULER_SYNC_DELAYED_TIME_FOR_WIFI_ON_KEY="scheduler_sync_delayed_time_for_wifi_on_key";
	public static final String SCHEDULER_SYNC_DELAYED_TIME_FOR_WIFI_ON_DEFAULT_VALUE="5";
}
