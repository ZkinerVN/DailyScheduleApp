package com.example.dailyscheduleapp.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.dailyscheduleapp.data.entity.Task;
import java.util.List;

@Dao
public interface TaskDao {
    @Insert
    long insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    // Lấy toàn bộ task dạng LiveData (tự động cập nhật UI khi dữ liệu đổi)
    @Query("SELECT * FROM tasks ORDER BY date ASC, time ASC")
    LiveData<List<Task>> getAllTasks();

    // Lọc danh sách task theo ngày cụ thể (cho Calendar/Agenda View)
    // Sắp xếp: Việc chưa xong lên trước -> Việc ưu tiên Cao nhất (3) lên đầu -> Theo giờ hẹn
    @Query("SELECT * FROM tasks WHERE date = :selectedDate ORDER BY isCompleted ASC, priority DESC, time ASC")
    LiveData<List<Task>> getTasksByDate(String selectedDate);

    // Đếm số task hoàn thành (phục vụ thống kê)
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    LiveData<Integer> getCompletedTaskCount();

    @Query("SELECT * FROM tasks WHERE isCompleted = 0")
    List<Task> getPendingTasksSync(); // Trả về List trực tiếp thay vì LiveData

    // BroadcastReceiver cập nhật ngầm không cần nạp cả Object Task
    @Query("UPDATE tasks SET isCompleted = 1 WHERE id = :taskId")
    void markTaskAsCompleted(int taskId);

    @Query("SELECT * FROM tasks WHERE isCompleted = 0")
    List<Task> getIncompleteTasksSync();

    @Query("UPDATE tasks SET actualDuration = actualDuration + :durationMinutes WHERE id = :taskId")
    void addActualDuration(int taskId, int durationMinutes);

    // QUẢN LÝ TỰ ĐỘNG LẶP LẠI (RECURRING)
    @Query("SELECT * FROM tasks WHERE repeatType > 0 AND parentTaskId = 0")
    List<Task> getRecurringTemplatesSync();

    @Query("SELECT COUNT(*) FROM tasks WHERE (parentTaskId = :templateId OR id = :templateId) AND date = :targetDate")
    int countInstancesForDate(int templateId, String targetDate);

    @Query("SELECT * FROM tasks WHERE date = :selectedDate")
    List<Task> getTasksByDateSync(String selectedDate);
}
