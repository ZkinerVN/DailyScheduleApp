package com.example.dailyscheduleapp.receiver;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.dailyscheduleapp.R;
import com.example.dailyscheduleapp.data.database.AppDatabase;
import com.example.dailyscheduleapp.ui.view.MainActivity;
import com.example.dailyscheduleapp.ui.view.PomodoroActivity;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_COMPLETE_TASK = "com.example.dailyscheduleapp.ACTION_COMPLETE_TASK";
    public static final String ACTION_SNOOZE = "com.example.dailyscheduleapp.ACTION_SNOOZE";

    private static final String CHANNEL_ID_HIGH = "task_channel_high";
    private static final String CHANNEL_ID_MEDIUM = "task_channel_medium";
    private static final String CHANNEL_ID_LOW = "task_channel_low";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // 1. XỬ LÝ NÚT [ ĐÃ XONG ]
        if (ACTION_COMPLETE_TASK.equals(action)) {
            int taskId = intent.getIntExtra("TASK_ID", -1);
            if (taskId != -1) {
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.cancel(taskId);

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase.getInstance(context).taskDao().markTaskAsCompleted(taskId);
                });
            }
            return;
        }

        // 2. XỬ LÝ NÚT [ HOÃN 10 PHÚT ]
        if (ACTION_SNOOZE.equals(action)) {
            int taskId = intent.getIntExtra("TASK_ID", -1);
            String title = intent.getStringExtra("TASK_TITLE");
            String desc = intent.getStringExtra("TASK_DESC");
            int priority = intent.getIntExtra("TASK_PRIORITY", 3);

            if (taskId != -1) {
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.cancel(taskId);

                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (am != null) {
                    Intent snoozeIntent = new Intent(context, AlarmReceiver.class);
                    snoozeIntent.putExtra("TASK_ID", taskId);
                    snoozeIntent.putExtra("TASK_TITLE", title);
                    snoozeIntent.putExtra("TASK_DESC", desc);
                    snoozeIntent.putExtra("TASK_PRIORITY", priority);

                    int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;

                    PendingIntent pi = PendingIntent.getBroadcast(context, taskId, snoozeIntent, flags);
                    long triggerTime = System.currentTimeMillis() + (10 * 60 * 1000);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi);
                    } else {
                        am.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pi);
                    }
                }
                Toast.makeText(context, context.getString(R.string.task_snoozed), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // 3. XỬ LÝ KHI BÁO THỨC ĐẾN GIỜ HẸN
        int taskId = intent.getIntExtra("TASK_ID", 0);
        String taskTitle = intent.getStringExtra("TASK_TITLE");
        String taskDesc = intent.getStringExtra("TASK_DESC");
        int priority = intent.getIntExtra("TASK_PRIORITY", 1);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        createNotificationChannels(context, notificationManager);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(context, taskId, openAppIntent, flags);

        Intent completeIntent = new Intent(context, AlarmReceiver.class);
        completeIntent.setAction(ACTION_COMPLETE_TASK);
        completeIntent.putExtra("TASK_ID", taskId);
        PendingIntent completePendingIntent = PendingIntent.getBroadcast(context, taskId, completeIntent, flags);

        String content = (taskDesc != null && !taskDesc.isEmpty()) ? taskDesc : context.getString(R.string.notif_default_content);
        String titleFormatted;
        NotificationCompat.Builder builder;

        if (priority == 3) {
            // MỨC CAO: Đánh thức màn hình, ghim thông báo, hỗ trợ Đã xong, Hoãn, Pomodoro
            wakeUpScreen(context);
            titleFormatted = context.getString(R.string.notif_urgent_prefix, taskTitle != null ? taskTitle.toUpperCase() : "");

            Intent snoozeActionIntent = new Intent(context, AlarmReceiver.class);
            snoozeActionIntent.setAction(ACTION_SNOOZE);
            snoozeActionIntent.putExtra("TASK_ID", taskId);
            snoozeActionIntent.putExtra("TASK_TITLE", taskTitle);
            snoozeActionIntent.putExtra("TASK_DESC", taskDesc);
            snoozeActionIntent.putExtra("TASK_PRIORITY", priority);
            PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, taskId + 20000, snoozeActionIntent, flags);

            Intent pomodoroIntent = new Intent(context, PomodoroActivity.class);
            PendingIntent pomodoroPendingIntent = PendingIntent.getActivity(context, taskId + 10000, pomodoroIntent, flags);

            builder = new NotificationCompat.Builder(context, CHANNEL_ID_HIGH)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(titleFormatted)
                    .setContentText(content)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setContentIntent(contentPendingIntent)
                    .addAction(android.R.drawable.checkbox_on_background, context.getString(R.string.notif_action_done), completePendingIntent)
                    .addAction(android.R.drawable.ic_popup_sync, context.getString(R.string.notif_action_snooze), snoozePendingIntent)
                    .addAction(android.R.drawable.ic_menu_recent_history, context.getString(R.string.notif_action_pomodoro), pomodoroPendingIntent);

        } else if (priority == 2) {
            // MỨC TRUNG BÌNH: Thông báo chuẩn, có nút Đã xong
            titleFormatted = context.getString(R.string.notif_normal_prefix, taskTitle != null ? taskTitle : "");

            builder = new NotificationCompat.Builder(context, CHANNEL_ID_MEDIUM)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(titleFormatted)
                    .setContentText(content)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(contentPendingIntent)
                    .addAction(android.R.drawable.checkbox_on_background, context.getString(R.string.notif_action_done), completePendingIntent);

        } else {
            // MỨC THẤP: Thông báo nhẹ
            titleFormatted = context.getString(R.string.notif_normal_prefix, taskTitle != null ? taskTitle : "");

            builder = new NotificationCompat.Builder(context, CHANNEL_ID_LOW)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(titleFormatted)
                    .setContentText(content)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(true)
                    .setContentIntent(contentPendingIntent);
        }

        notificationManager.notify(taskId, builder.build());
    }

    private void createNotificationChannels(Context context, NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            Uri notifSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            AudioAttributes alarmAudioAttr = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();

            AudioAttributes notifAudioAttr = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();

            NotificationChannel highChannel = new NotificationChannel(
                    CHANNEL_ID_HIGH,
                    context.getString(R.string.channel_high_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            highChannel.setDescription(context.getString(R.string.channel_high_desc));
            highChannel.enableVibration(true);
            highChannel.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 1000});
            highChannel.setSound(alarmSound, alarmAudioAttr);
            highChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(highChannel);

            NotificationChannel mediumChannel = new NotificationChannel(
                    CHANNEL_ID_MEDIUM,
                    context.getString(R.string.channel_medium_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            mediumChannel.setDescription(context.getString(R.string.channel_medium_desc));
            mediumChannel.enableVibration(true);
            mediumChannel.setVibrationPattern(new long[]{0, 300, 200, 300});
            mediumChannel.setSound(notifSound, notifAudioAttr);
            manager.createNotificationChannel(mediumChannel);

            NotificationChannel lowChannel = new NotificationChannel(
                    CHANNEL_ID_LOW,
                    context.getString(R.string.channel_low_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            lowChannel.setDescription(context.getString(R.string.channel_low_desc));
            lowChannel.enableVibration(true);
            lowChannel.setVibrationPattern(new long[]{0, 200});
            lowChannel.setSound(notifSound, notifAudioAttr);
            manager.createNotificationChannel(lowChannel);
        }
    }

    private void wakeUpScreen(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            @SuppressWarnings("deprecation")
            PowerManager.WakeLock wakeLock = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK |
                            PowerManager.ACQUIRE_CAUSES_WAKEUP |
                            PowerManager.ON_AFTER_RELEASE,
                    "DailySchedule:AlarmWakeLock"
            );
            wakeLock.acquire(5000);
        }
    }
}