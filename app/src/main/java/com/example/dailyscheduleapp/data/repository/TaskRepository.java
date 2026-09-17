package com.example.dailyscheduleapp.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.dailyscheduleapp.data.dao.CategoryDao;
import com.example.dailyscheduleapp.data.dao.TaskDao;
import com.example.dailyscheduleapp.data.database.AppDatabase;
import com.example.dailyscheduleapp.data.entity.Category;
import com.example.dailyscheduleapp.data.entity.Task;
import java.util.List;

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

    public void insertTask(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> taskDao.insert(task));
    }

    public void updateTask(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> taskDao.update(task));
    }

    public void deleteTask(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> taskDao.delete(task));
    }
}