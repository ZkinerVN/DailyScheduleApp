package com.example.dailyscheduleapp.ui.view;

import android.annotation.SuppressLint;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.example.dailyscheduleapp.R;
import com.example.dailyscheduleapp.data.database.AppDatabase;
import com.example.dailyscheduleapp.data.entity.Category;
import com.example.dailyscheduleapp.data.entity.Task;
import com.example.dailyscheduleapp.ui.viewmodel.TaskViewModel;
import com.example.dailyscheduleapp.utils.EnergyPlantHelper;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StatisticsActivity extends AppCompatActivity {

    private TextView tvCompletionRate, tvTotalTasks, tvCompletedTasks, tvPendingTasks;
    private TextView tvPriorityHigh, tvPriorityMedium, tvPriorityLow;
    private TextView tvFocusInvested, tvHealthInvested, tvLeisureInvested;
    private ProgressBar progressBarCompletion;
    private ProgressBar pbPriorityHigh, pbPriorityMedium, pbPriorityLow;
    private ProgressBar pbFocusInvested, pbHealthInvested, pbLeisureInvested;
    private TextView tvStatsEmpty;
    private View[] statsCards;
    private View btnStatsOpenWrap;

    private MaterialCardView cardStreak;
    private TextView tvStreakCount, tvStreakDesc;

    private WeeklyBarChartView weeklyBarChartView;
    private TextView tvBestProductiveDay;
    private TextView tvGardenBlooming, tvGardenGrowing, tvGardenThirsty;

    private int statsFocusMinutes = 0, statsHealthMinutes = 0, statsLeisureMinutes = 0;
    private int statsCompleted = 0, statsTotal = 0;

    private final Map<Integer, Category> categoryMap = new HashMap<>();
    private List<Task> allTasksCache = new ArrayList<>();
    private int currentFilterMode = 0; // 0: All, 1: Today, 2: This Week

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        initViews();

        TaskViewModel taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        // Lắng nghe danh mục để phân loại năng lượng
        AppDatabase.getInstance(this).categoryDao().getAllCategories().observe(this, categories -> {
            categoryMap.clear();
            if (categories != null) {
                for (Category cat : categories) {
                    categoryMap.put(cat.getId(), cat);
                }
            }
            applyFilterAndCalculate();
        });

        // Lấy tất cả công việc từ Room để thống kê
        taskViewModel.getAllTasks().observe(this, tasks -> {
            allTasksCache = tasks != null ? tasks : new ArrayList<>();
            updateStreak(allTasksCache);
            applyFilterAndCalculate();
        });
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbarStats);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvStatsEmpty = findViewById(R.id.tvStatsEmpty);
        cardStreak = findViewById(R.id.cardStreak);
        tvStreakCount = findViewById(R.id.tvStreakCount);
        tvStreakDesc = findViewById(R.id.tvStreakDesc);

        statsCards = new View[]{
                findViewById(R.id.cardProgress),
                findViewById(R.id.cardWeeklyTrend),
                findViewById(R.id.cardWeeklyGarden),
                findViewById(R.id.cardTaskCount),
                findViewById(R.id.cardPriority),
                findViewById(R.id.cardEnergy)
        };

        weeklyBarChartView = findViewById(R.id.weeklyBarChartView);
        tvBestProductiveDay = findViewById(R.id.tvBestProductiveDay);
        tvGardenBlooming = findViewById(R.id.tvGardenBlooming);
        tvGardenGrowing = findViewById(R.id.tvGardenGrowing);
        tvGardenThirsty = findViewById(R.id.tvGardenThirsty);

        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        progressBarCompletion = findViewById(R.id.progressBarCompletion);

        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks);
        tvPendingTasks = findViewById(R.id.tvPendingTasks);

        tvPriorityHigh = findViewById(R.id.tvPriorityHigh);
        pbPriorityHigh = findViewById(R.id.pbPriorityHigh);
        tvPriorityMedium = findViewById(R.id.tvPriorityMedium);
        pbPriorityMedium = findViewById(R.id.pbPriorityMedium);
        tvPriorityLow = findViewById(R.id.tvPriorityLow);
        pbPriorityLow = findViewById(R.id.pbPriorityLow);

        tvFocusInvested = findViewById(R.id.tvFocusInvested);
        pbFocusInvested = findViewById(R.id.pbFocusInvested);
        tvHealthInvested = findViewById(R.id.tvHealthInvested);
        pbHealthInvested = findViewById(R.id.pbHealthInvested);
        tvLeisureInvested = findViewById(R.id.tvLeisureInvested);
        pbLeisureInvested = findViewById(R.id.pbLeisureInvested);

        btnStatsOpenWrap = findViewById(R.id.btnStatsOpenWrap);
        if (btnStatsOpenWrap != null) {
            btnStatsOpenWrap.setOnClickListener(v -> {
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                EnergyPlantHelper.EnergyPlantInfo info = EnergyPlantHelper.calculatePlantInfo(
                        statsFocusMinutes, 240,
                        statsHealthMinutes, 30,
                        statsLeisureMinutes, 90,
                        statsCompleted, statsTotal
                );
                DailyWrapDialog.show(
                        StatisticsActivity.this,
                        today,
                        info,
                        statsFocusMinutes,
                        statsHealthMinutes,
                        statsLeisureMinutes,
                        statsCompleted,
                        statsTotal
                );
            });
        }

        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.toggleGroupTimeFilter);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnFilterAll) {
                currentFilterMode = 0;
            } else if (checkedId == R.id.btnFilterToday) {
                currentFilterMode = 1;
            } else if (checkedId == R.id.btnFilterWeek) {
                currentFilterMode = 2;
            }
            applyFilterAndCalculate();
        });
    }

    private void updateStreak(List<Task> tasks) {
        if (tvStreakCount == null) return;
        int streak = calculateStreak(tasks);
        tvStreakCount.setText(getString(R.string.streak_label, streak));
        if (tvStreakDesc != null) {
            if (streak == 0) {
                tvStreakDesc.setText(R.string.streak_desc_zero);
            } else if (streak < 3) {
                tvStreakDesc.setText(R.string.streak_desc_low);
            } else if (streak < 7) {
                tvStreakDesc.setText(R.string.streak_desc_med);
            } else {
                tvStreakDesc.setText(R.string.streak_desc_high);
            }
        }
    }

    private int calculateStreak(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) return 0;
        Set<String> completedDates = new HashSet<>();
        for (Task t : tasks) {
            if (t.isCompleted() && t.getDate() != null && !t.getDate().isEmpty()) {
                completedDates.add(t.getDate());
            }
        }
        if (completedDates.isEmpty()) return 0;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        String today = sdf.format(cal.getTime());

        int streak = 0;
        if (!completedDates.contains(today)) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        while (completedDates.contains(sdf.format(cal.getTime()))) {
            streak++;
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        return streak;
    }

    private void applyFilterAndCalculate() {
        if (allTasksCache == null || allTasksCache.isEmpty()) {
            showEmptyState(true);
            return;
        }

        updateWeeklyInsights(allTasksCache);

        List<Task> filtered = filterTasks(allTasksCache, currentFilterMode);

        if (filtered.isEmpty()) {
            showEmptyState(true);
            return;
        }

        showEmptyState(false);
        calculateAndDisplayStatistics(filtered);
    }

    private void showEmptyState(boolean isEmpty) {
        tvStatsEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        for (View card : statsCards) {
            if (card != null) {
                card.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            }
        }
    }

    private List<Task> filterTasks(List<Task> tasks, int mode) {
        if (mode == 0) return tasks;

        List<Task> result = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        if (mode == 1) { // Hôm nay
            String today = sdf.format(new Date());
            for (Task t : tasks) {
                if (today.equals(t.getDate())) {
                    result.add(t);
                }
            }
        } else if (mode == 2) { // Tuần này
            Calendar cal = Calendar.getInstance();
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            String startOfWeek = sdf.format(cal.getTime());

            cal.add(Calendar.DAY_OF_WEEK, 6);
            String endOfWeek = sdf.format(cal.getTime());

            for (Task t : tasks) {
                if (t.getDate() != null
                        && t.getDate().compareTo(startOfWeek) >= 0
                        && t.getDate().compareTo(endOfWeek) <= 0) {
                    result.add(t);
                }
            }
        }
        return result;
    }

    @SuppressLint("SetTextI18n")
    private void calculateAndDisplayStatistics(List<Task> tasks) {
        int total = tasks.size();
        int completed = 0;
        int highPriority = 0, mediumPriority = 0, lowPriority = 0;
        int totalFocusMinutes = 0, totalHealthMinutes = 0, totalLeisureMinutes = 0;

        for (Task t : tasks) {
            if (t.isCompleted()) completed++;

            switch (t.getPriority()) {
                case 3: highPriority++; break;
                case 2: mediumPriority++; break;
                default: lowPriority++; break;
            }

            Category cat = categoryMap.get(t.getCategoryId());
            if (cat != null) {
                switch (cat.getType()) {
                    case 1: totalFocusMinutes += t.getEstimatedDuration(); break;
                    case 2: totalHealthMinutes += t.getEstimatedDuration(); break;
                    case 3: totalLeisureMinutes += t.getEstimatedDuration(); break;
                }
            }
        }

        int rate = total > 0 ? (int) (((float) completed / total) * 100) : 0;
        tvCompletionRate.setText(rate + "%");
        progressBarCompletion.setProgress(rate);

        tvTotalTasks.setText(String.valueOf(total));
        tvCompletedTasks.setText(String.valueOf(completed));
        tvPendingTasks.setText(String.valueOf(total - completed));

        tvPriorityHigh.setText(getString(R.string.stats_priority_high, highPriority));
        tvPriorityMedium.setText(getString(R.string.stats_priority_medium, mediumPriority));
        tvPriorityLow.setText(getString(R.string.stats_priority_low, lowPriority));

        pbPriorityHigh.setMax(total);
        pbPriorityHigh.setProgress(highPriority);
        pbPriorityMedium.setMax(total);
        pbPriorityMedium.setProgress(mediumPriority);
        pbPriorityLow.setMax(total);
        pbPriorityLow.setProgress(lowPriority);

        tvFocusInvested.setText(formatMinutes(totalFocusMinutes));
        tvHealthInvested.setText(formatMinutes(totalHealthMinutes));
        tvLeisureInvested.setText(formatMinutes(totalLeisureMinutes));

        int maxEnergyMinutes = Math.max(1, Math.max(totalFocusMinutes, Math.max(totalHealthMinutes, totalLeisureMinutes)));
        pbFocusInvested.setMax(maxEnergyMinutes);
        pbFocusInvested.setProgress(totalFocusMinutes);

        pbHealthInvested.setMax(maxEnergyMinutes);
        pbHealthInvested.setProgress(totalHealthMinutes);

        pbLeisureInvested.setMax(maxEnergyMinutes);
        pbLeisureInvested.setProgress(totalLeisureMinutes);

        statsTotal = total;
        statsCompleted = completed;
        statsFocusMinutes = totalFocusMinutes;
        statsHealthMinutes = totalHealthMinutes;
        statsLeisureMinutes = totalLeisureMinutes;
    }

    private String formatMinutes(int totalMinutes) {
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        if (hours > 0) return mins > 0 ? hours + "h " + mins + "m" : hours + "h";
        return mins + "m";
    }

    private void updateWeeklyInsights(List<Task> allTasks) {
        if (weeklyBarChartView == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);

        // Tìm chỉ số ngày hôm nay (0: Thứ 2 .. 6: Chủ nhật)
        int todayDow = cal.get(Calendar.DAY_OF_WEEK);
        int todayIndex = (todayDow == Calendar.SUNDAY) ? 6 : (todayDow - Calendar.MONDAY);

        // Lùi cal về Thứ Hai đầu tuần hiện tại
        int diffToMonday = (todayDow == Calendar.SUNDAY) ? -6 : (Calendar.MONDAY - todayDow);
        cal.add(Calendar.DAY_OF_YEAR, diffToMonday);

        boolean isVi = "vi".equalsIgnoreCase(Locale.getDefault().getLanguage());
        String[] shortLabels = isVi
                ? new String[]{"T2", "T3", "T4", "T5", "T6", "T7", "CN"}
                : new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        String[] fullNames = isVi
                ? new String[]{"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"}
                : new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        float[] dayHours = new float[7];
        int bloomingCount = 0;
        int growingCount = 0;
        int thirstyCount = 0;

        float maxHours = 0f;
        int bestDayIdx = -1;

        // Nhóm task theo ngày để tra cứu nhanh
        Map<String, List<Task>> tasksByDate = new HashMap<>();
        if (allTasks != null) {
            for (Task t : allTasks) {
                if (t.getDate() != null) {
                    List<Task> list = tasksByDate.get(t.getDate());
                    if (list == null) {
                        list = new ArrayList<>();
                        tasksByDate.put(t.getDate(), list);
                    }
                    list.add(t);
                }
            }
        }

        for (int i = 0; i < 7; i++) {
            String dateStr = sdf.format(cal.getTime());
            List<Task> dayTasks = tasksByDate.get(dateStr);

            int focusMins = 0;
            int healthMins = 0;
            int leisureMins = 0;
            int completedTasks = 0;
            int totalTasks = 0;

            if (dayTasks != null && !dayTasks.isEmpty()) {
                totalTasks = dayTasks.size();
                for (Task t : dayTasks) {
                    if (t.isCompleted()) completedTasks++;
                    int duration = t.getActualDuration() > 0 ? t.getActualDuration()
                            : (t.getEstimatedDuration() > 0 ? t.getEstimatedDuration() : 30);

                    Category cat = categoryMap.get(t.getCategoryId());
                    if (cat != null) {
                        switch (cat.getType()) {
                            case 1: focusMins += duration; break;
                            case 2: healthMins += duration; break;
                            case 3: leisureMins += duration; break;
                            default: focusMins += duration; break;
                        }
                    } else {
                        focusMins += duration;
                    }
                }

                // Đánh giá trạng thái Vườn Năng Lượng cho ngày này
                EnergyPlantHelper.EnergyPlantInfo plantInfo = EnergyPlantHelper.calculatePlantInfo(
                        focusMins, 240,
                        healthMins, 30,
                        leisureMins, 90,
                        completedTasks, totalTasks
                );

                if (plantInfo.type == EnergyPlantHelper.PlantType.BLOOMING) {
                    bloomingCount++;
                } else if (plantInfo.type == EnergyPlantHelper.PlantType.GROWING) {
                    growingCount++;
                } else if (plantInfo.type == EnergyPlantHelper.PlantType.THIRSTY) {
                    thirstyCount++;
                }
            }

            float hours = (float) focusMins / 60.0f;
            dayHours[i] = Math.round(hours * 10.0f) / 10.0f;

            if (dayHours[i] > maxHours) {
                maxHours = dayHours[i];
                bestDayIdx = i;
            }

            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Cập nhật Biểu đồ cột tuần
        weeklyBarChartView.setWeeklyData(dayHours, shortLabels, todayIndex);

        // Cập nhật Ngày năng suất nhất
        if (tvBestProductiveDay != null) {
            if (bestDayIdx >= 0 && maxHours > 0f) {
                String bestDayName = fullNames[bestDayIdx];
                String bestHoursStr = (maxHours == (int) maxHours)
                        ? String.format(Locale.getDefault(), "%.0fh", maxHours)
                        : String.format(Locale.getDefault(), "%.1fh", maxHours);
                tvBestProductiveDay.setText(getString(R.string.stats_best_day_format, bestDayName, bestHoursStr));
            } else {
                tvBestProductiveDay.setText(R.string.stats_best_day_none);
            }
        }

        // Cập nhật các huy hiệu Khu vườn tuần
        if (tvGardenBlooming != null) {
            tvGardenBlooming.setText(getString(R.string.stats_garden_blooming, bloomingCount));
        }
        if (tvGardenGrowing != null) {
            tvGardenGrowing.setText(getString(R.string.stats_garden_growing, growingCount));
        }
        if (tvGardenThirsty != null) {
            tvGardenThirsty.setText(getString(R.string.stats_garden_thirsty, thirstyCount));
        }
    }
}
