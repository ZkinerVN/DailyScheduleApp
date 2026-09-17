package com.example.dailyscheduleapp.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "day_configs")
public class DayConfig {
    @PrimaryKey
    @NonNull
    private String date; // yyyy-MM-dd

    private String profileName;
    private int focusLimitMinutes;   // Giới hạn Học/Làm
    private int healthGoalMinutes;   // Mục tiêu Vận động
    private int leisureLimitMinutes; // Hạn ngạch Giải trí

    // 1. Constructor rỗng bắt buộc cho Room
    public DayConfig() {
        this.date = "";
        this.profileName = "Normal Day";
        this.focusLimitMinutes = 240;
        this.healthGoalMinutes = 30;
        this.leisureLimitMinutes = 90;
    }

    // 2. Thêm @Ignore để Room không bị xung đột constructor
    @Ignore
    public DayConfig(@NonNull String date) {
        this.date = date;
        this.profileName = "Normal Day";
        this.focusLimitMinutes = 240;
        this.healthGoalMinutes = 30;
        this.leisureLimitMinutes = 90;
    }

    // --- GETTER & SETTER ---
    @NonNull
    public String getDate() { return date; }
    public void setDate(@NonNull String date) { this.date = date; }

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }

    public int getFocusLimitMinutes() { return focusLimitMinutes; }
    public void setFocusLimitMinutes(int focusLimitMinutes) { this.focusLimitMinutes = focusLimitMinutes; }

    public int getHealthGoalMinutes() { return healthGoalMinutes; }
    public void setHealthGoalMinutes(int healthGoalMinutes) { this.healthGoalMinutes = healthGoalMinutes; }

    public int getLeisureLimitMinutes() { return leisureLimitMinutes; }
    public void setLeisureLimitMinutes(int leisureLimitMinutes) { this.leisureLimitMinutes = leisureLimitMinutes; }
}