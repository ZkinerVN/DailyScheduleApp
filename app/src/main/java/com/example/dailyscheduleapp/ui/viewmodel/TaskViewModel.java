package com.example.dailyscheduleapp.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.dailyscheduleapp.data.entity.Category;
import com.example.dailyscheduleapp.data.entity.DayConfig;
import com.example.dailyscheduleapp.data.entity.Task;
import com.example.dailyscheduleapp.data.repository.DayConfigRepository;
import com.example.dailyscheduleapp.data.repository.TaskRepository;

import java.util.List;

public class TaskViewModel extends AndroidViewModel {

    private final TaskRepository repository;
    private final DayConfigRepository dayConfigRepository;
    private final LiveData<List<Task>> allTasks;
    private final LiveData<List<Category>> allCategories;

    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskRepository(application);
        dayConfigRepository = new DayConfigRepository(application);
        allTasks = repository.getAllTasks();
        allCategories = repository.getAllCategories();
    }

    public LiveData<List<Task>> getAllTasks() { return allTasks; }
    public LiveData<List<Category>> getAllCategories() { return allCategories; }

    public LiveData<List<Task>> getTasksByDate(String date) {
        return repository.getTasksByDate(date);
    }

    public void insertTask(Task task) {
        repository.insertTask(task);
    }

    public void insertTask(Task task, TaskRepository.OnTaskInsertedListener callback) {
        repository.insertTask(task, callback);
    }

    public void updateTask(Task task) {
        repository.updateTask(task);
    }

    public void deleteTask(Task task) {
        repository.deleteTask(task);
    }

    public LiveData<DayConfig> getDayConfig(String date) {
        return dayConfigRepository.getDayConfig(date);
    }

    public void saveDayConfig(DayConfig config) {
        dayConfigRepository.insertOrUpdateDayConfig(config);
    }

    public void insertCategory(Category category) {
        repository.insertCategory(category);
    }

    public void deleteCategory(Category category) {
        repository.deleteCategory(category);
    }

    // Tự động sinh công việc lặp lại khi người dùng mở ngày đó
    public void ensureRecurringTasksForDate(String targetDate) {
        repository.ensureRecurringTasksForDate(targetDate);
    }

    // Kiểm tra trùng giờ với Lịch cố định
    public void checkTimeConflictWithFixedSchedule(String date, String time, int duration, int currentTaskId, TaskRepository.OnConflictCheckListener listener) {
        repository.checkTimeConflictWithFixedSchedule(date, time, duration, currentTaskId, listener);
    }
}
