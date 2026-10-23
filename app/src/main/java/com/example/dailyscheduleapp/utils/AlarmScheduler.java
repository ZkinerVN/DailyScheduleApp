package com.example.dailyscheduleapp.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.example.dailyscheduleapp.data.entity.Task;
import com.example.dailyscheduleapp.receiver.AlarmReceiver;
import com.example.dailyscheduleapp.ui.view.MainActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AlarmScheduler {

    public static void scheduleTaskAlarm(Context context, Task task) {
        if (task.isCompleted()) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            calendar.setTime(sdf.parse(task.getDate() + " " + task.getTime()));
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        long now = System.currentTimeMillis();
        long triggerTime = calendar.getTimeInMillis();

        // Nếu công việc đã quá hạn hơn 1 phút trước -> Bỏ qua không nhắc việc cũ
        if (triggerTime < now - 60000) {
            return;
        }

        // Nếu giờ hẹn nằm trong đúng phút hiện tại -> Kích hoạt ngay sau 2 giây để người dùng nhận được báo thức
        if (triggerTime <= now) {
            triggerTime = now + 2000;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(AlarmReceiver.ACTION_TRIGGER_TASK);
        intent.putExtra("TASK_ID", task.getId());
        intent.putExtra("TASK_TITLE", task.getTitle());
        intent.putExtra("TASK_DESC", task.getDescription());
        intent.putExtra("TASK_PRIORITY", task.getPriority());

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.getId(),
                intent,
                flags
        );

        // Intent khi người dùng bấm vào biểu tượng báo thức trên thanh trạng thái
        Intent showIntent = new Intent(context, MainActivity.class);
        PendingIntent showPendingIntent = PendingIntent.getActivity(
                context,
                task.getId(),
                showIntent,
                flags
        );

        // DÙNG setAlarmClock: Chuẩn báo thức cao nhất của Android, đánh thức máy từ mọi chế độ Doze / Tiết kiệm pin,
        // chính xác từng giây và không phụ thuộc vào quyền SCHEDULE_EXACT_ALARM!
        try {
            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent);
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
        } catch (SecurityException | UnsupportedOperationException e) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                    );
                } else {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                    );
                }
            } catch (Exception fallbackEx) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        }
    }

    public static void cancelTaskAlarm(Context context, Task task) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(AlarmReceiver.ACTION_TRIGGER_TASK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.getId(),
                intent,
                flags
        );

        alarmManager.cancel(pendingIntent);
    }
}
