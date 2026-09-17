package com.example.dailyscheduleapp.ui.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.dailyscheduleapp.data.entity.Category;
import com.example.dailyscheduleapp.data.entity.Task;
import com.example.dailyscheduleapp.data.repository.TaskRepository;
import java.util.List;

public class TaskViewModel extends AndroidViewModel {

    private final TaskRepository repository;
    private final LiveData<List<Task>> allTasks;
    private final LiveData<List<Category>> allCategories;

    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskRepository(application);
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

    public void updateTask(Task task) {
        repository.updateTask(task);
    }

    public void deleteTask(Task task) {
        repository.deleteTask(task);
    }
}