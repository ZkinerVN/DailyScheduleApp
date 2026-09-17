package com.example.dailyscheduleapp.ui.view;

import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private TextView tvCompletionRate, tvTotalTasks, tvCompletedTasks, tvPendingTasks;
    private TextView tvPriorityHigh, tvPriorityMedium, tvPriorityLow;
    private TextView tvFocusInvested, tvHealthInvested, tvLeisureInvested;
    private ProgressBar progressBarCompletion;
    private ProgressBar pbPriorityHigh, pbPriorityMedium, pbPriorityLow;
    private ProgressBar pbFocusInvested, pbHealthInvested, pbLeisureInvested;

    private final Map<Integer, Category> categoryMap = new HashMap<>();
    private List<Task> allTasksCache = new ArrayList<>();

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
            calculateStatistics(allTasksCache);
        });

        // Lấy tất cả công việc từ Room để thống kê
        taskViewModel.getAllTasks().observe(this, tasks -> {
            allTasksCache = tasks != null ? tasks : new ArrayList<>();
            calculateStatistics(allTasksCache);
        });
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbarStats);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks);
        tvPendingTasks = findViewById(R.id.tvPendingTasks);

        tvPriorityHigh = findViewById(R.id.tvPriorityHigh);
        tvPriorityMedium = findViewById(R.id.tvPriorityMedium);
        tvPriorityLow = findViewById(R.id.tvPriorityLow);

        tvFocusInvested = findViewById(R.id.tvFocusInvested);
        tvHealthInvested = findViewById(R.id.tvHealthInvested);
        tvLeisureInvested = findViewById(R.id.tvLeisureInvested);

        progressBarCompletion = findViewById(R.id.progressBarCompletion);
        pbPriorityHigh = findViewById(R.id.pbPriorityHigh);
        pbPriorityMedium = findViewById(R.id.pbPriorityMedium);
        pbPriorityLow = findViewById(R.id.pbPriorityLow);

        pbFocusInvested = findViewById(R.id.pbFocusInvested);
        pbHealthInvested = findViewById(R.id.pbHealthInvested);
        pbLeisureInvested = findViewById(R.id.pbLeisureInvested);
    }

    private void calculateStatistics(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            tvTotalTasks.setText("0");
            tvCompletedTasks.setText("0");
            tvPendingTasks.setText("0");
            tvCompletionRate.setText("0%");
            progressBarCompletion.setProgress(0);

            tvPriorityHigh.setText("0");
            tvPriorityMedium.setText("0");
            tvPriorityLow.setText("0");
            pbPriorityHigh.setProgress(0);
            pbPriorityMedium.setProgress(0);
            pbPriorityLow.setProgress(0);

            tvFocusInvested.setText("0m");
            tvHealthInvested.setText("0m");
            tvLeisureInvested.setText("0m");
            pbFocusInvested.setProgress(0);
            pbHealthInvested.setProgress(0);
            pbLeisureInvested.setProgress(0);
            return;
        }

        int total = tasks.size();
        int completed = 0;
        int highPriority = 0;
        int mediumPriority = 0;
        int lowPriority = 0;

        int totalFocusMinutes = 0;
        int totalHealthMinutes = 0;
        int totalLeisureMinutes = 0;

        for (Task t : tasks) {
            if (t.isCompleted()) {
                completed++;
            }

            switch (t.getPriority()) {
                case 3: highPriority++; break;
                case 2: mediumPriority++; break;
                default: lowPriority++; break;
            }

            // Tính thời gian theo bản chất năng lượng của danh mục
            Category cat = categoryMap.get(t.getCategoryId());
            if (cat != null) {
                switch (cat.getType()) {
                    case 1: totalFocusMinutes += t.getEstimatedDuration(); break;
                    case 2: totalHealthMinutes += t.getEstimatedDuration(); break;
                    case 3: totalLeisureMinutes += t.getEstimatedDuration(); break;
                }
            }
        }

        int pending = total - completed;
        int rate = (int) Math.round(((double) completed / total) * 100);

        // Cập nhật thẻ Tiến độ & Số lượng
        tvTotalTasks.setText(String.valueOf(total));
        tvCompletedTasks.setText(String.valueOf(completed));
        tvPendingTasks.setText(String.valueOf(pending));
        tvCompletionRate.setText(rate + "%");
        progressBarCompletion.setProgress(rate);

        // Cập nhật thẻ Phân bổ ưu tiên
        tvPriorityHigh.setText(String.valueOf(highPriority));
        tvPriorityMedium.setText(String.valueOf(mediumPriority));
        tvPriorityLow.setText(String.valueOf(lowPriority));

        pbPriorityHigh.setMax(total);
        pbPriorityHigh.setProgress(highPriority);
        pbPriorityMedium.setMax(total);
        pbPriorityMedium.setProgress(mediumPriority);
        pbPriorityLow.setMax(total);
        pbPriorityLow.setProgress(lowPriority);

        // Cập nhật thẻ Phân bổ thời gian năng lượng
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
    }

    private String formatMinutes(int totalMinutes) {
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        if (hours > 0) return mins > 0 ? hours + "h " + mins + "m" : hours + "h";
        return mins + "m";
    }
}