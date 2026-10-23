package com.example.dailyscheduleapp.utils;

public final class Constants {

    private Constants() {
        // Prevent instantiation
    }

    // SharedPreferences
    public static final String PREFS_NAME = "app_preferences";
    public static final String KEY_NIGHT_MODE = "night_mode";
    public static final String KEY_SOUND_ENABLED = "notification_sound_enabled";
    public static final String KEY_SOUND_URI = "notification_sound_uri";
    public static final String KEY_VIBRATION_MODE = "notification_vibration_mode";

    // Intent Extras
    public static final String EXTRA_TASK_ID = "TASK_ID";
    public static final String EXTRA_TASK_TITLE = "TASK_TITLE";
    public static final String EXTRA_TASK_DATE = "TASK_DATE";
    public static final String EXTRA_TASK_TIME = "TASK_TIME";
    public static final String EXTRA_TASK_PRIORITY = "TASK_PRIORITY";
    public static final String EXTRA_TASK_CATEGORY_ID = "TASK_CATEGORY_ID";

    // Notification Channel IDs
    public static final String CHANNEL_HIGH_ID = "high_priority_channel";
    public static final String CHANNEL_MEDIUM_ID = "medium_priority_channel";
    public static final String CHANNEL_LOW_ID = "low_priority_channel";
    public static final String CHANNEL_POMODORO_ID = "pomodoro_ongoing_channel";

    // Date & Time Formats
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";
}
