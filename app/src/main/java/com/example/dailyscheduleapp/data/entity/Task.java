package com.example.dailyscheduleapp.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;
    private String description;
    private String date; // Định dạng yyyy-MM-dd
    private String time; // Định dạng HH:mm

    // THUỘC TÍNH MỚI: Quản lý thời gian & Cân bằng năng lượng
    private int estimatedDuration; // Thời lượng dự kiến (phút)
    private int actualDuration;    // Thời lượng thực tế sau khi làm xong (phút)
    private boolean includeInBalance; // Có tính vào Daily Balance không? (Mặc định: true)

    private int priority; // 1: Thấp, 2: Trung bình, 3: Cao
    private int categoryId;

    // THUỘC TÍNH MỚI: Trạng thái thông minh
    private boolean isCompleted;
    private boolean isOverdue; // Bật cờ này nếu quá hạn chưa làm

    // Constructor mặc định
    public Task() {
        this.estimatedDuration = 60; // Mặc định 1 task 60 phút
        this.actualDuration = 0;
        this.includeInBalance = true;
        this.isOverdue = false;
        this.isCompleted = false;
    }

    // --- GETTER & SETTER ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(int estimatedDuration) { this.estimatedDuration = estimatedDuration; }

    public int getActualDuration() { return actualDuration; }
    public void setActualDuration(int actualDuration) { this.actualDuration = actualDuration; }

    public boolean isIncludeInBalance() { return includeInBalance; }
    public void setIncludeInBalance(boolean includeInBalance) { this.includeInBalance = includeInBalance; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public boolean isOverdue() { return isOverdue; }
    public void setOverdue(boolean overdue) { isOverdue = overdue; }
}