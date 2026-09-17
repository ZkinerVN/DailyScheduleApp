package com.example.dailyscheduleapp.data.database;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.dailyscheduleapp.data.dao.CategoryDao;
import com.example.dailyscheduleapp.data.dao.DayConfigDao;
import com.example.dailyscheduleapp.data.dao.TaskDao;
import com.example.dailyscheduleapp.data.entity.Category;
import com.example.dailyscheduleapp.data.entity.DayConfig;
import com.example.dailyscheduleapp.data.entity.Task;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TĂNG LÊN VERSION 3 ĐỂ TỰ ĐỘNG RESET DỮ LIỆU RÁC TRÙNG LẶP
@Database(entities = {Task.class, Category.class, DayConfig.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;
    private static Context appContext;

    public abstract TaskDao taskDao();
    public abstract CategoryDao categoryDao();
    public abstract DayConfigDao dayConfigDao();

    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4);

    public static AppDatabase getInstance(final Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "daily_schedule_db"
                            )
                            .fallbackToDestructiveMigration()
                            .addCallback(roomCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            // CHỈ GỌI Ở ONOPEN, KHÔNG GỌI TRONG ONCREATE ĐỂ TRÁNH CHẠY SONG SONG
            seedDefaultCategories();
        }
    };

    private static synchronized void seedDefaultCategories() {
        if (appContext == null) return;

        databaseWriteExecutor.execute(() -> {
            SharedPreferences prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            boolean isSeeded = prefs.getBoolean("categories_seeded_v3", false);

            if (!isSeeded) {
                CategoryDao dao = INSTANCE.categoryDao();

                // DANH MỤC PHỔ THÔNG CHO MỌI ĐỐI TƯỢNG
                dao.insert(new Category("SYS_STUDY", "#2196F3", 1));    // FOCUS (Xanh dương)
                dao.insert(new Category("SYS_WORK", "#3F51B5", 1));     // FOCUS (Xanh chàm)
                dao.insert(new Category("SYS_SPORT", "#4CAF50", 2));    // HEALTH (Xanh lá)
                dao.insert(new Category("SYS_LEISURE", "#FF9800", 3));  // LEISURE (Cam)
                dao.insert(new Category("SYS_PERSONAL", "#9E9E9E", 4)); // PERSONAL (Xám)
                dao.insert(new Category("SYS_FIXED", "#F44336", 5));    // FIXED (Đỏ)

                // DÙNG COMMIT ĐỒNG BỘ ĐỂ GHI NGAY LẬP TỨC
                prefs.edit().putBoolean("categories_seeded_v3", true).commit();
            }
        });
    }
}