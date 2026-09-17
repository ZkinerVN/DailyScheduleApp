package com.example.dailyscheduleapp.ui.view;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dailyscheduleapp.R;
import com.example.dailyscheduleapp.data.database.AppDatabase;
import com.example.dailyscheduleapp.data.entity.Category;
import com.example.dailyscheduleapp.data.entity.DayConfig;
import com.example.dailyscheduleapp.data.entity.Task;
import com.example.dailyscheduleapp.ui.adapter.TaskAdapter;
import com.example.dailyscheduleapp.ui.viewmodel.TaskViewModel;
import com.example.dailyscheduleapp.utils.AlarmScheduler;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 1001;

    private TaskViewModel taskViewModel;
    private TaskAdapter adapter;

    // Quản lý ngày
    private Calendar currentCalendar;
    private String selectedDate; // yyyy-MM-dd

    private RecyclerView rvTasks;
    private TextView tvEmptyState, tvGreeting, tvCurrentDateFull, tvScheduleHeader;
    private ImageButton btnPrevDay, btnNextDay;

    // UI Cân bằng năng lượng
    private TextView tvFocusTime, tvHealthTime, tvLeisureTime, tvInsight;
    private ProgressBar pbFocus, pbHealth, pbLeisure;
    private LinearLayout layoutCategoryChips;

    // Dữ liệu bộ lọc & tính toán
    private final Map<Integer, Category> categoryMap = new HashMap<>();
    private final List<Category> allCategories = new ArrayList<>();
    private List<Task> currentDayTasks = new ArrayList<>();
    private int activeFilterCategoryId = -1;

    // Cấu hình định mức ngày động
    private DayConfig currentDayConfig;

    // Quản lý LiveData khi đổi ngày
    private LiveData<DayConfig> dayConfigLiveData;
    private LiveData<List<Task>> tasksLiveData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestNotificationPermission();

        initViews();
        setupLanguageSwitch();
        setupRecyclerView();
        setupBottomNav();

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        // Nút mở hộp thoại Cấu hình định mức ngày
        ImageButton btnConfigDay = findViewById(R.id.btnConfigDay);
        if (btnConfigDay != null) {
            btnConfigDay.setOnClickListener(v -> showDayConfigDialog());
        }

        // Lắng nghe danh mục để tạo chip bộ lọc
        AppDatabase.getInstance(this).categoryDao().getAllCategories().observe(this, categories -> {
            categoryMap.clear();
            allCategories.clear();
            if (categories != null) {
                allCategories.addAll(categories);
                for (Category cat : categories) {
                    categoryMap.put(cat.getId(), cat);
                }
                adapter.setCategories(categories);
                buildCategoryFilterChips(allCategories);
            }
            updateDailyBalance();
        });

        // Khởi tạo ngày hiện tại và gắn sự kiện chuyển ngày
        setupDateNavigation();

        // Nút thêm công việc
        findViewById(R.id.fabAddTask).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditTaskActivity.class);
            intent.putExtra("EXTRA_DATE", selectedDate);
            startActivity(intent);
        });
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tvGreeting);
        tvCurrentDateFull = findViewById(R.id.tvCurrentDateFull);
        tvScheduleHeader = findViewById(R.id.tvScheduleHeader);
        btnPrevDay = findViewById(R.id.btnPrevDay);
        btnNextDay = findViewById(R.id.btnNextDay);

        rvTasks = findViewById(R.id.rvTasks);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        tvFocusTime = findViewById(R.id.tvFocusTime);
        tvHealthTime = findViewById(R.id.tvHealthTime);
        tvLeisureTime = findViewById(R.id.tvLeisureTime);
        tvInsight = findViewById(R.id.tvInsight);
        pbFocus = findViewById(R.id.pbFocus);
        pbHealth = findViewById(R.id.pbHealth);
        pbLeisure = findViewById(R.id.pbLeisure);
        layoutCategoryChips = findViewById(R.id.layoutCategoryChips);
    }

    private void setupDateNavigation() {
        currentCalendar = Calendar.getInstance();
        updateSelectedDate(currentCalendar);

        btnPrevDay.setOnClickListener(v -> {
            currentCalendar.add(Calendar.DAY_OF_MONTH, -1);
            updateSelectedDate(currentCalendar);
        });

        btnNextDay.setOnClickListener(v -> {
            currentCalendar.add(Calendar.DAY_OF_MONTH, 1);
            updateSelectedDate(currentCalendar);
        });

        tvCurrentDateFull.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    currentCalendar.set(Calendar.YEAR, year);
                    currentCalendar.set(Calendar.MONTH, month);
                    currentCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateSelectedDate(currentCalendar);
                },
                currentCalendar.get(Calendar.YEAR),
                currentCalendar.get(Calendar.MONTH),
                currentCalendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void updateSelectedDate(Calendar calendar) {
        selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));

        SimpleDateFormat fullDateFormat = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault());
        tvCurrentDateFull.setText(fullDateFormat.format(calendar.getTime()));

        // Kiểm tra xem có phải là hôm nay không
        Calendar today = Calendar.getInstance();
        boolean isToday = (today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR));

        if (isToday) {
            int hour = today.get(Calendar.HOUR_OF_DAY);
            if (hour < 12) tvGreeting.setText(getString(R.string.greeting_morning));
            else if (hour < 18) tvGreeting.setText(getString(R.string.greeting_afternoon));
            else tvGreeting.setText(getString(R.string.greeting_evening));
            tvScheduleHeader.setText(getString(R.string.todays_schedule));
        } else {
            tvGreeting.setText(getString(R.string.greeting_other_day));
            tvScheduleHeader.setText(getString(R.string.schedule_date_header, selectedDate));
        }

        observeDataForSelectedDate();
    }

    private void observeDataForSelectedDate() {
        // 1. Tháo gỡ observer cũ và quan sát DayConfig của ngày mới
        if (dayConfigLiveData != null) {
            dayConfigLiveData.removeObservers(this);
        }
        dayConfigLiveData = AppDatabase.getInstance(this).dayConfigDao().getDayConfig(selectedDate);
        dayConfigLiveData.observe(this, config -> {
            if (config != null) {
                currentDayConfig = config;
            } else {
                currentDayConfig = new DayConfig(selectedDate);
            }
            updateDailyBalance();
        });

        // 2. Tháo gỡ observer cũ và quan sát danh sách task của ngày mới
        if (tasksLiveData != null) {
            tasksLiveData.removeObservers(this);
        }
        tasksLiveData = taskViewModel.getTasksByDate(selectedDate);
        tasksLiveData.observe(this, tasks -> {
            currentDayTasks = tasks != null ? tasks : new ArrayList<>();

            long nowMillis = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

            Collections.sort(currentDayTasks, (t1, t2) -> {
                if (t1.isCompleted() != t2.isCompleted()) {
                    return t1.isCompleted() ? 1 : -1;
                }

                boolean t1Overdue = false, t2Overdue = false;
                try {
                    t1Overdue = !t1.isCompleted() && sdf.parse(t1.getDate() + " " + t1.getTime()).getTime() < nowMillis;
                    t2Overdue = !t2.isCompleted() && sdf.parse(t2.getDate() + " " + t2.getTime()).getTime() < nowMillis;
                } catch (Exception ignored) {}

                if (t1Overdue != t2Overdue) {
                    return t1Overdue ? -1 : 1;
                }

                if (t1.getPriority() != t2.getPriority()) {
                    return Integer.compare(t2.getPriority(), t1.getPriority());
                }

                String time1 = t1.getTime() != null ? t1.getTime() : "";
                String time2 = t2.getTime() != null ? t2.getTime() : "";
                return time1.compareTo(time2);
            });

            applyCategoryFilter();
            updateDailyBalance();
        });
    }

    private void setupLanguageSwitch() {
        Button btnSwitchLanguage = findViewById(R.id.btnSwitchLanguage);
        if (btnSwitchLanguage != null) {
            btnSwitchLanguage.setOnClickListener(v -> {
                LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
                String currentLang = currentLocales.toLanguageTags();
                if (currentLang.isEmpty() || currentLang.contains("en")) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("vi"));
                } else {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"));
                }
            });
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_pomodoro) {
                startActivity(new Intent(MainActivity.this, PomodoroActivity.class));
                return true;
            } else if (itemId == R.id.nav_stats) {
                startActivity(new Intent(MainActivity.this, StatisticsActivity.class));
                return true;
            }
            return itemId == R.id.nav_home;
        });
    }

    private void setupRecyclerView() {
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter();
        rvTasks.setAdapter(adapter);

        adapter.setOnTaskClickListener(new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task) {
                Intent intent = new Intent(MainActivity.this, AddEditTaskActivity.class);
                intent.putExtra("EXTRA_ID", task.getId());
                intent.putExtra("EXTRA_TITLE", task.getTitle());
                intent.putExtra("EXTRA_DESC", task.getDescription());
                intent.putExtra("EXTRA_DATE", task.getDate());
                intent.putExtra("EXTRA_TIME", task.getTime());
                intent.putExtra("EXTRA_PRIORITY", task.getPriority());
                intent.putExtra("EXTRA_CATEGORY_ID", task.getCategoryId());
                intent.putExtra("EXTRA_DURATION", task.getEstimatedDuration());
                intent.putExtra("EXTRA_INCLUDE_BALANCE", task.isIncludeInBalance());
                startActivity(intent);
            }

            @Override
            public void onTaskDelete(Task task) {
                deleteTaskWithUndo(task);
            }

            @Override
            public void onTaskStatusChanged(Task task, boolean isCompleted) {
                task.setCompleted(isCompleted);
                taskViewModel.updateTask(task);
                if (isCompleted) AlarmScheduler.cancelTaskAlarm(MainActivity.this, task);
                else AlarmScheduler.scheduleTaskAlarm(MainActivity.this, task);
            }
        });

        setupSwipeActions();
    }

    private void buildCategoryFilterChips(List<Category> categories) {
        layoutCategoryChips.removeAllViews();
        layoutCategoryChips.addView(createChip(getString(R.string.filter_all), -1));

        for (Category cat : categories) {
            layoutCategoryChips.addView(createChip(cat.getLocalizedName(this), cat.getId()));
        }
    }

    private MaterialButton createChip(String text, int categoryId) {
        MaterialButton btn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btn.setText(text);
        btn.setAllCaps(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 16, 0);
        btn.setLayoutParams(params);
        btn.setCornerRadius(50);

        if (categoryId == activeFilterCategoryId) {
            btn.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"));
            btn.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        } else {
            btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            btn.setTextColor(android.graphics.Color.parseColor("#2196F3"));
        }

        btn.setOnClickListener(v -> {
            activeFilterCategoryId = categoryId;
            buildCategoryFilterChips(allCategories);
            applyCategoryFilter();
        });
        return btn;
    }

    private void applyCategoryFilter() {
        List<Task> filteredList = new ArrayList<>();
        if (activeFilterCategoryId == -1) {
            filteredList.addAll(currentDayTasks);
        } else {
            for (Task t : currentDayTasks) {
                if (t.getCategoryId() == activeFilterCategoryId) {
                    filteredList.add(t);
                }
            }
        }
        adapter.setTasks(filteredList);

        if (filteredList.isEmpty()) {
            rvTasks.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvTasks.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void updateDailyBalance() {
        int focusTotal = 0, healthTotal = 0, leisureTotal = 0;

        for (Task t : currentDayTasks) {
            if (!t.isIncludeInBalance()) continue;

            Category cat = categoryMap.get(t.getCategoryId());
            if (cat != null) {
                switch (cat.getType()) {
                    case 1: focusTotal += t.getEstimatedDuration(); break;
                    case 2: healthTotal += t.getEstimatedDuration(); break;
                    case 3: leisureTotal += t.getEstimatedDuration(); break;
                }
            }
        }

        int limitFocus = currentDayConfig != null ? currentDayConfig.getFocusLimitMinutes() : 240;
        int goalHealth = currentDayConfig != null ? currentDayConfig.getHealthGoalMinutes() : 30;
        int limitLeisure = currentDayConfig != null ? currentDayConfig.getLeisureLimitMinutes() : 90;

        tvFocusTime.setText(formatMinutes(focusTotal) + " / " + formatMinutes(limitFocus));
        pbFocus.setMax(Math.max(1, limitFocus));
        pbFocus.setProgress(Math.min(focusTotal, limitFocus));

        tvHealthTime.setText(formatMinutes(healthTotal) + " / " + formatMinutes(goalHealth));
        pbHealth.setMax(Math.max(1, goalHealth));
        pbHealth.setProgress(Math.min(healthTotal, goalHealth));

        tvLeisureTime.setText(formatMinutes(leisureTotal) + " / " + formatMinutes(limitLeisure));
        pbLeisure.setMax(Math.max(1, limitLeisure));
        pbLeisure.setProgress(Math.min(leisureTotal, limitLeisure));

        if (focusTotal > limitFocus) {
            tvInsight.setText(getString(R.string.insight_overload));
            tvInsight.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        } else if (leisureTotal > limitLeisure) {
            tvInsight.setText(getString(R.string.insight_over_leisure));
            tvInsight.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
        } else if (healthTotal < goalHealth && focusTotal > 0) {
            tvInsight.setText(getString(R.string.insight_need_health));
            tvInsight.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
        } else {
            tvInsight.setText(getString(R.string.insight_perfect));
            tvInsight.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }
    }

    private void showDayConfigDialog() {
        if (currentDayConfig == null) {
            currentDayConfig = new DayConfig(selectedDate);
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_day_config, null);
        EditText etFocus = dialogView.findViewById(R.id.etFocusLimit);
        EditText etHealth = dialogView.findViewById(R.id.etHealthGoal);
        EditText etLeisure = dialogView.findViewById(R.id.etLeisureLimit);

        etFocus.setText(String.valueOf(currentDayConfig.getFocusLimitMinutes()));
        etHealth.setText(String.valueOf(currentDayConfig.getHealthGoalMinutes()));
        etLeisure.setText(String.valueOf(currentDayConfig.getLeisureLimitMinutes()));

        dialogView.findViewById(R.id.btnPresetNormal).setOnClickListener(v -> {
            etFocus.setText("240");
            etHealth.setText("30");
            etLeisure.setText("90");
        });
        dialogView.findViewById(R.id.btnPresetWeekend).setOnClickListener(v -> {
            etFocus.setText("120");
            etHealth.setText("45");
            etLeisure.setText("180");
        });
        dialogView.findViewById(R.id.btnPresetExam).setOnClickListener(v -> {
            etFocus.setText("360");
            etHealth.setText("20");
            etLeisure.setText("45");
        });

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.config_day_title))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.dialog_apply), (dialog, which) -> {
                    try {
                        int focus = Integer.parseInt(etFocus.getText().toString().trim());
                        int health = Integer.parseInt(etHealth.getText().toString().trim());
                        int leisure = Integer.parseInt(etLeisure.getText().toString().trim());

                        if (focus <= 0 || health <= 0 || leisure <= 0) {
                            Toast.makeText(this, getString(R.string.error_invalid_quota), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        currentDayConfig.setDate(selectedDate);
                        currentDayConfig.setFocusLimitMinutes(focus);
                        currentDayConfig.setHealthGoalMinutes(health);
                        currentDayConfig.setLeisureLimitMinutes(leisure);

                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            AppDatabase.getInstance(getApplicationContext()).dayConfigDao().insertOrUpdateDayConfig(currentDayConfig);
                        });

                        Toast.makeText(this, getString(R.string.config_saved), Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, getString(R.string.error_invalid_quota), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.btn_cancel), (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String formatMinutes(int totalMinutes) {
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        if (hours > 0) return mins > 0 ? hours + "h" + mins + "m" : hours + "h";
        return mins + "m";
    }

    private void setupSwipeActions() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Task targetTask = adapter.getTaskAt(position);

                if (direction == ItemTouchHelper.LEFT) {
                    deleteTaskWithUndo(targetTask);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    boolean newStatus = !targetTask.isCompleted();
                    targetTask.setCompleted(newStatus);
                    taskViewModel.updateTask(targetTask);
                    if (newStatus) AlarmScheduler.cancelTaskAlarm(MainActivity.this, targetTask);
                    else AlarmScheduler.scheduleTaskAlarm(MainActivity.this, targetTask);
                    adapter.notifyItemChanged(position);
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvTasks);
    }

    private void deleteTaskWithUndo(Task task) {
        AlarmScheduler.cancelTaskAlarm(this, task);
        taskViewModel.deleteTask(task);
        String message = getString(R.string.task_deleted, task.getTitle());
        Snackbar snackbar = Snackbar.make(rvTasks, message, Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.undo), v -> {
                    taskViewModel.insertTask(task);
                    AlarmScheduler.scheduleTaskAlarm(this, task);
                });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) snackbar.setAnchorView(bottomNav);
        snackbar.show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_home);
    }
}