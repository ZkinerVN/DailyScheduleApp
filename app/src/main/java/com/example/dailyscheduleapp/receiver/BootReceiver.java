package com.example.dailyscheduleapp.receiver;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.example.dailyscheduleapp.data.database.AppDatabase;
import com.example.dailyscheduleapp.data.entity.Task;
import com.example.dailyscheduleapp.utils.AlarmScheduler;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        boolean isBoot = Intent.ACTION_BOOT_COMPLETED.equals(action);
        boolean isExactAlarmChanged = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(action);

        if (isBoot || isExactAlarmChanged) {
            final PendingResult pendingResult = goAsync();
            // Chạy luồng nền truy vấn lại danh sách task chưa hoàn thành
            AppDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    List<Task> pendingTasks = AppDatabase.getInstance(context)
                            .taskDao()
                            .getPendingTasksSync();

                    if (pendingTasks != null) {
                        for (Task task : pendingTasks) {
                            AlarmScheduler.scheduleTaskAlarm(context, task);
                        }
                    }
                    com.example.dailyscheduleapp.widget.TodayScheduleWidgetProvider.updateAllWidgets(context);
                } finally {
                    pendingResult.finish();
                }
            });
        }
    }
}
