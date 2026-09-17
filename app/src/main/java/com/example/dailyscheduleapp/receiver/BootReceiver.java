package com.example.dailyscheduleapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.dailyscheduleapp.data.database.AppDatabase;
import com.example.dailyscheduleapp.data.entity.Task;
import com.example.dailyscheduleapp.utils.AlarmScheduler;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Chạy luồng nền truy vấn lại danh sách task chưa hoàn thành
            AppDatabase.databaseWriteExecutor.execute(() -> {
                List<Task> pendingTasks = AppDatabase.getInstance(context)
                        .taskDao()
                        .getPendingTasksSync();

                if (pendingTasks != null) {
                    for (Task task : pendingTasks) {
                        AlarmScheduler.scheduleTaskAlarm(context, task);
                    }
                }
            });
        }
    }
}