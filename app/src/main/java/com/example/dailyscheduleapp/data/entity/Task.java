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

    // QUẢN LÝ THỜI GIAN & CÂN BẰNG NĂNG LƯỢNG
    private int estimatedDuration; // Thời lượng dự kiến (phút)
    private int actualDuration;    // Thời lượng thực tế sau khi làm xong (phút)
    private boolean includeInBalance; // Có tính vào Daily Balance không? (Mặc định: true)

    private int priority; // 1: Thấp, 2: Trung bình, 3: Cao
    private int categoryId;

    // TRẠNG THÁI THÔNG MINH
    private boolean isCompleted;
    private boolean isOverdue; // Bật cờ này nếu quá hạn chưa làm

    // LẶP LẠI (RECURRING / HABIT)
    // 0: Không lặp, 1: Hàng ngày, 2: Thứ 2 - Thứ 6, 3: Hàng tuần
    private int repeatType;
    // ID của task gốc định nghĩa mẫu lặp (0 nếu là task độc lập/mẫu gốc)
    private int parentTaskId;

    // Constructor mặc định
    public Task() {
        this.estimatedDuration = 60; // Mặc định 1 task 60 phút
        this.actualDuration = 0;
        this.includeInBalance = true;
        this.isOverdue = false;
        this.isCompleted = false;
        this.repeatType = 0;
        this.parentTaskId = 0;
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

    public int getRepeatType() { return repeatType; }
    public void setRepeatType(int repeatType) { this.repeatType = repeatType; }

    public int getParentTaskId() { return parentTaskId; }
    public void setParentTaskId(int parentTaskId) { this.parentTaskId = parentTaskId; }
}
