package com.example.dailyscheduleapp.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.dailyscheduleapp.data.entity.DayConfig;
@Dao
public interface DayConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateDayConfig(DayConfig config);

    @Query("SELECT * FROM day_configs WHERE date = :date LIMIT 1")
    LiveData<DayConfig> getDayConfig(String date);
}