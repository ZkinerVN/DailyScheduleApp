package com.example.dailyscheduleapp.receiver;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.dailyscheduleapp.R;
import com.example.dailyscheduleapp.data.database.AppDatabase;
import com.example.dailyscheduleapp.ui.view.AlarmAlertActivity;
import com.example.dailyscheduleapp.ui.view.MainActivity;
import com.example.dailyscheduleapp.ui.view.PomodoroActivity;
import com.example.dailyscheduleapp.utils.NotificationSettings;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String ACTION_TRIGGER_TASK = "com.example.dailyscheduleapp.ACTION_TRIGGER_TASK";
    public static final String ACTION_COMPLETE_TASK = "com.example.dailyscheduleapp.ACTION_COMPLETE_TASK";
    public static final String ACTION_SNOOZE = "com.example.dailyscheduleapp.ACTION_SNOOZE";

    @SuppressLint("NotificationTrampoline")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // 1. XỬ LÝ NÚT [ ĐÃ XONG ] TỪ THANH THÔNG BÁO
        if (ACTION_COMPLETE_TASK.equals(action)) {
            int taskId = intent.getIntExtra("TASK_ID", -1);
            if (taskId != -1) {
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.cancel(taskId);

                final PendingResult pendingResult = goAsync();
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    try {
                        AppDatabase.getInstance(context).taskDao().markTaskAsCompleted(taskId);
                    } finally {
                        pendingResult.finish();
                    }
                });
                Toast.makeText(context, context.getString(R.string.pomodoro_task_completed_toast), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // 2. XỬ LÝ NÚT [ HOÃN 10 PHÚT ] TỪ THANH THÔNG BÁO
        if (ACTION_SNOOZE.equals(action)) {
            int taskId = intent.getIntExtra("TASK_ID", -1);
            String title = intent.getStringExtra("TASK_TITLE");
            String desc = intent.getStringExtra("TASK_DESC");
            int priority = intent.getIntExtra("TASK_PRIORITY", 1);

            if (taskId != -1) {
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.cancel(taskId);

                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (am != null) {
                    Intent snoozeIntent = new Intent(context, AlarmReceiver.class);
                    snoozeIntent.setAction(ACTION_TRIGGER_TASK);
                    snoozeIntent.putExtra("TASK_ID", taskId);
                    snoozeIntent.putExtra("TASK_TITLE", title);
                    snoozeIntent.putExtra("TASK_DESC", desc);
                    snoozeIntent.putExtra("TASK_PRIORITY", priority);

                    int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;

                    PendingIntent pi = PendingIntent.getBroadcast(context, taskId, snoozeIntent, flags);
                    long triggerTime = System.currentTimeMillis() + (10 * 60 * 1000);

                    Intent showIntent = new Intent(context, MainActivity.class);
                    PendingIntent showPendingIntent = PendingIntent.getActivity(context, taskId, showIntent, flags);

                    try {
                        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent);
                        am.setAlarmClock(alarmClockInfo, pi);
                    } catch (SecurityException | UnsupportedOperationException e) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi);
                            } else {
                                am.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pi);
                            }
                        } catch (Exception fallbackEx) {
                            am.set(AlarmManager.RTC_WAKEUP, triggerTime, pi);
                        }
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

        NotificationSettings.ensureChannels(context);

        // Bật sáng màn hình
        wakeUpScreen(context);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        // Intent mở AlarmAlertActivity (Màn hình báo thức toàn màn hình chuẩn)
        Intent alertActivityIntent = new Intent(context, AlarmAlertActivity.class);
        alertActivityIntent.putExtra("TASK_ID", taskId);
        alertActivityIntent.putExtra("TASK_TITLE", taskTitle);
        alertActivityIntent.putExtra("TASK_DESC", taskDesc);
        alertActivityIntent.putExtra("TASK_PRIORITY", priority);
        alertActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                taskId,
                alertActivityIntent,
                flags
        );

        // NÚT 1: ĐÃ XONG
        Intent completeIntent = new Intent(context, AlarmReceiver.class);
        completeIntent.setAction(ACTION_COMPLETE_TASK);
        completeIntent.putExtra("TASK_ID", taskId);
        PendingIntent completePendingIntent = PendingIntent.getBroadcast(context, taskId + 100, completeIntent, flags);

        // NÚT 2: HOÃN 10 PHÚT
        Intent snoozeIntent = new Intent(context, AlarmReceiver.class);
        snoozeIntent.setAction(ACTION_SNOOZE);
        snoozeIntent.putExtra("TASK_ID", taskId);
        snoozeIntent.putExtra("TASK_TITLE", taskTitle);
        snoozeIntent.putExtra("TASK_DESC", taskDesc);
        snoozeIntent.putExtra("TASK_PRIORITY", priority);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, taskId + 200, snoozeIntent, flags);

        // NÚT 3: POMODORO (Cho TẤT CẢ các mức ưu tiên: Thấp, Trung bình, Cao)
        // Truyền chính xác TASK_ID & TASK_TITLE để Pomodoro gán vào nhiệm vụ và tính vào thanh năng lượng
        Intent pomodoroIntent = new Intent(context, PomodoroActivity.class);
        pomodoroIntent.putExtra("TASK_ID", taskId);
        pomodoroIntent.putExtra("TASK_TITLE", taskTitle);
        pomodoroIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pomodoroPendingIntent = PendingIntent.getActivity(context, taskId + 300, pomodoroIntent, flags);

        String content = (taskDesc != null && !taskDesc.isEmpty()) ? taskDesc : context.getString(R.string.notif_default_content);
        String titleFormatted;
        if (priority == 3) {
            titleFormatted = context.getString(R.string.notif_urgent_prefix, taskTitle != null ? taskTitle.toUpperCase() : "");
        } else {
            titleFormatted = context.getString(R.string.notif_normal_prefix, taskTitle != null ? taskTitle : "");
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context,
                NotificationSettings.getChannelId(context, priority)
        )
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(titleFormatted)
                .setContentText(content)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(fullScreenPendingIntent)
                .addAction(R.drawable.ic_notif_done, context.getString(R.string.notif_action_done), completePendingIntent)
                .addAction(R.drawable.ic_notif_snooze, context.getString(R.string.notif_action_snooze), snoozePendingIntent)
                .addAction(R.drawable.ic_notif_pomodoro, context.getString(R.string.notif_action_pomodoro), pomodoroPendingIntent);

        if (priority == 3) {
            builder.setFullScreenIntent(fullScreenPendingIntent, true); // ĐÁNH THỨC MÀN HÌNH KHÓA CHUẨN ANDROID
        }

        Notification notification = builder.build();
        if (priority == 3) {
            // Cờ FLAG_INSISTENT yêu cầu hệ thống lặp lại âm thanh thông báo cho đến khi người dùng thao tác
            notification.flags |= Notification.FLAG_INSISTENT;
        }

        notificationManager.notify(taskId, notification);

        // Với mức Cao (3): Kích hoạt mở thẳng màn hình AlarmAlertActivity
        if (priority == 3) {
            try {
                context.startActivity(alertActivityIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
