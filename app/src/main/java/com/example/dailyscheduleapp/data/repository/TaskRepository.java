package com.example.dailyscheduleapp.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.dailyscheduleapp.data.dao.CategoryDao;
import com.example.dailyscheduleapp.data.dao.TaskDao;
import com.example.dailyscheduleapp.data.database.AppDatabase;
import com.example.dailyscheduleapp.data.entity.Category;
import com.example.dailyscheduleapp.data.entity.Task;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskRepository {
    private final TaskDao taskDao;
    private final CategoryDao categoryDao;
    private final LiveData<List<Task>> allTasks;
    private final LiveData<List<Category>> allCategories;

    public TaskRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        taskDao = db.taskDao();
        categoryDao = db.categoryDao();
        allTasks = taskDao.getAllTasks();
        allCategories = categoryDao.getAllCategories();
    }

    public LiveData<List<Task>> getAllTasks() { return allTasks; }
    public LiveData<List<Category>> getAllCategories() { return allCategories; }

    public LiveData<List<Task>> getTasksByDate(String date) {
        return taskDao.getTasksByDate(date);
    }

    public interface OnTaskInsertedListener {
        void onTaskInserted(long newId);
    }

    public void insertTask(Task task) {
        insertTask(task, null);
    }

    public void insertTask(Task task, OnTaskInsertedListener callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long newId = taskDao.insert(task);
            if (callback != null) {
                callback.onTaskInserted(newId);
            }
        });
    }

    public void updateTask(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> taskDao.update(task));
    }

    public void deleteTask(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> taskDao.delete(task));
    }

    public void insertCategory(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryDao.insert(category));
    }

    public void deleteCategory(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryDao.delete(category));
    }

    // TỰ ĐỘNG SINH TASK LẶP LẠI CHO NGÀY ĐƯỢC CHỌN (RECURRING / HABITS)
    public void ensureRecurringTasksForDate(String targetDate) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date target = sdf.parse(targetDate);
                if (target == null) return;

                Calendar targetCal = Calendar.getInstance();
                targetCal.setTime(target);
                int targetDayOfWeek = targetCal.get(Calendar.DAY_OF_WEEK); // 1 = Sunday, 2 = Monday, ... 7 = Saturday

                List<Task> templates = taskDao.getRecurringTemplatesSync();
                if (templates == null || templates.isEmpty()) return;

                for (Task template : templates) {
                    if (template.getDate() == null) continue;
                    Date templateDate = sdf.parse(template.getDate());
                    if (templateDate == null) continue;

                    // Chỉ sinh task cho các ngày >= ngày bắt đầu của mẫu
                    if (targetDate.compareTo(template.getDate()) < 0) continue;

                    boolean shouldGenerate = false;
                    switch (template.getRepeatType()) {
                        case 1: // Hàng ngày (Daily)
                            shouldGenerate = true;
                            break;
                        case 2: // Thứ 2 đến Thứ 6 (Weekdays)
                            if (targetDayOfWeek >= Calendar.MONDAY && targetDayOfWeek <= Calendar.FRIDAY) {
                                shouldGenerate = true;
                            }
                            break;
                        case 3: // Hàng tuần (Weekly cùng thứ)
                            Calendar templateCal = Calendar.getInstance();
                            templateCal.setTime(templateDate);
                            if (templateCal.get(Calendar.DAY_OF_WEEK) == targetDayOfWeek) {
                                shouldGenerate = true;
                            }
                            break;
                    }

                    if (shouldGenerate) {
                        int count = taskDao.countInstancesForDate(template.getId(), targetDate);
                        if (count == 0) {
                            Task instance = new Task();
                            instance.setTitle(template.getTitle());
                            instance.setDescription(template.getDescription());
                            instance.setDate(targetDate);
                            instance.setTime(template.getTime());
                            instance.setEstimatedDuration(template.getEstimatedDuration());
                            instance.setActualDuration(0);
                            instance.setIncludeInBalance(template.isIncludeInBalance());
                            instance.setPriority(template.getPriority());
                            instance.setCategoryId(template.getCategoryId());
                            instance.setRepeatType(template.getRepeatType());
                            instance.setParentTaskId(template.getId());
                            instance.setCompleted(false);
                            instance.setOverdue(false);
                            taskDao.insert(instance);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public interface OnConflictCheckListener {
        void onResult(Task conflictingFixedTask);
    }

    // KIỂM TRA TRÙNG GIỜ VỚI LỊCH CỐ ĐỊNH (SYS_FIXED CONFLICT DETECTION)
    public void checkTimeConflictWithFixedSchedule(String date, String time, int durationMinutes, int currentTaskId, OnConflictCheckListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Task conflict = null;
            try {
                if (date != null && time != null && durationMinutes > 0) {
                    int startMin = parseTimeToMinutes(time);
                    int endMin = startMin + durationMinutes;

                    List<Category> categories = categoryDao.getAllCategoriesSync();
                    Map<Integer, Boolean> isFixedCategory = new HashMap<>();
                    if (categories != null) {
                        for (Category c : categories) {
                            if ("SYS_FIXED".equals(c.getName()) || c.getType() == 5) {
                                isFixedCategory.put(c.getId(), true);
                            }
                        }
                    }

                    List<Task> tasksForDay = taskDao.getTasksByDateSync(date);
                    if (tasksForDay != null) {
                        for (Task t : tasksForDay) {
                            if (t.getId() == currentTaskId) continue;
                            if (Boolean.TRUE.equals(isFixedCategory.get(t.getCategoryId())) && t.getTime() != null) {
                                int otherStart = parseTimeToMinutes(t.getTime());
                                int otherEnd = otherStart + Math.max(15, t.getEstimatedDuration());
                                // Kiểm tra 2 khoảng thời gian [startMin, endMin] và [otherStart, otherEnd] có giao nhau không
                                if (Math.max(startMin, otherStart) < Math.min(endMin, otherEnd)) {
                                    conflict = t;
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final Task resultConflict = conflict;
            if (listener != null) {
                listener.onResult(resultConflict);
            }
        });
    }

    private int parseTimeToMinutes(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            return Integer.parseInt(parts[0].trim()) * 60 + Integer.parseInt(parts[1].trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
