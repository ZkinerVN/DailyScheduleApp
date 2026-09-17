package com.example.dailyscheduleapp.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.dailyscheduleapp.data.entity.Category;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Category category);

    @Update
    void update(Category category);

    // BỔ SUNG: Xóa danh mục
    @Delete
    void delete(Category category);

    @Query("SELECT * FROM categories ORDER BY id ASC")
    LiveData<List<Category>> getAllCategories();

    // BỔ SUNG: Kiểm tra xem đã có danh mục hệ thống trong DB chưa
    @Query("SELECT COUNT(*) FROM categories WHERE name LIKE 'SYS_%'")
    int countSystemCategories();

    // BỔ SUNG: Tìm ID của danh mục SYS_PERSONAL làm nơi chuyển task về khi xóa danh mục khác
    @Query("SELECT id FROM categories WHERE name = 'SYS_PERSONAL' LIMIT 1")
    int getDefaultPersonalCategoryId();
}