package com.example.dailyscheduleapp.ui.view;

import android.annotation.SuppressLint;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.dailyscheduleapp.R;
import com.example.dailyscheduleapp.data.entity.Category;
import com.example.dailyscheduleapp.data.entity.Task;
import com.example.dailyscheduleapp.ui.viewmodel.TaskViewModel;
import com.example.dailyscheduleapp.utils.AlarmScheduler;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddEditTaskActivity extends AppCompatActivity {
    private EditText edtTitle, edtDescription, edtDuration;
    private SwitchCompat switchIncludeInBalance;
    private Button btnSelectDate, btnSelectTime, btnSave;
    private ImageButton btnAddCategory;
    private TextView tvDateTimeSelected;
    private RadioGroup rgPriority;
    private Spinner spinnerCategory;
    private Spinner spinnerRepeat;
    private MaterialCardView cardConflictWarning;
    private TextView tvConflictWarning;

    private TaskViewModel taskViewModel;
    private String selectedDate = "";
    private String selectedTime = "";
    private int currentTaskId = -1; // -1: Thêm mới, khác -1: Chỉnh sửa task cũ

    // Quản lý danh mục trong Spinner
    private final List<Category> categoryList = new ArrayList<>();
    private final List<String> categoryNames = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;
    private int selectedCategoryId = 1;
    private boolean existingIsCompleted = false;
    private int existingActualDuration = 0;
    private int initialRepeatType = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_task);

        // Ánh xạ View
        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        edtDuration = findViewById(R.id.edtDuration);
        switchIncludeInBalance = findViewById(R.id.switchIncludeInBalance);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        btnSave = findViewById(R.id.btnSave);
        tvDateTimeSelected = findViewById(R.id.tvDateTimeSelected);
        rgPriority = findViewById(R.id.rgPriority);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerRepeat = findViewById(R.id.spinnerRepeat);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        cardConflictWarning = findViewById(R.id.cardConflictWarning);
        tvConflictWarning = findViewById(R.id.tvConflictWarning);

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        setupRepeatSpinner();

        // Kiểm tra xem Activity mở lên để THÊM MỚI hay SỬA CÔNG VIỆC
        checkIntentData();

        // Cấu hình Spinner và nạp danh mục
        setupCategorySpinner();

        // Các sự kiện Click
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectTime.setOnClickListener(v -> showTimePicker());
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        btnSave.setOnClickListener(v -> saveTask());
        ImageButton btnManageCategory = findViewById(R.id.btnManageCategory);
        if (btnManageCategory != null) {
            btnManageCategory.setOnClickListener(v -> showManageCategoriesDialog());
        }
        Button btnCancel = findViewById(R.id.btnCancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }

        edtDuration.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int count, int after) {
                checkTimeConflict();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRepeatSpinner() {
        String[] repeatOptions = new String[] {
                getString(R.string.repeat_none),
                getString(R.string.repeat_daily),
                getString(R.string.repeat_weekdays),
                getString(R.string.repeat_weekly)
        };
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, repeatOptions);
        spinnerRepeat.setAdapter(repeatAdapter);
    }

    // Nhận dữ liệu cũ nếu là chế độ Sửa
    private void checkIntentData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EXTRA_ID")) {
            currentTaskId = intent.getIntExtra("EXTRA_ID", -1);
            edtTitle.setText(intent.getStringExtra("EXTRA_TITLE"));
            edtDescription.setText(intent.getStringExtra("EXTRA_DESC"));
            selectedDate = intent.getStringExtra("EXTRA_DATE");
            selectedTime = intent.getStringExtra("EXTRA_TIME");
            if (selectedDate == null) selectedDate = "";
            if (selectedTime == null) selectedTime = "";
            updateDateTimeText();

            // Nạp thời lượng và công tắc cũ
            int duration = intent.getIntExtra("EXTRA_DURATION", 60);
            edtDuration.setText(String.valueOf(duration));
            boolean includeBalance = intent.getBooleanExtra("EXTRA_INCLUDE_BALANCE", true);
            switchIncludeInBalance.setChecked(includeBalance);

            int priority = intent.getIntExtra("EXTRA_PRIORITY", 1);
            if (priority == 2) {
                rgPriority.check(R.id.rbMedium);
            } else if (priority == 3) {
                rgPriority.check(R.id.rbHigh);
            } else {
                rgPriority.check(R.id.rbLow);
            }

            selectedCategoryId = intent.getIntExtra("EXTRA_CATEGORY_ID", 1);
            existingIsCompleted = intent.getBooleanExtra("EXTRA_IS_COMPLETED", false);
            existingActualDuration = intent.getIntExtra("EXTRA_ACTUAL_DURATION", 0);
            initialRepeatType = intent.getIntExtra("EXTRA_REPEAT_TYPE", 0);
            if (initialRepeatType >= 0 && initialRepeatType < spinnerRepeat.getCount()) {
                spinnerRepeat.setSelection(initialRepeatType);
            }
            btnSave.setText(getString(R.string.btn_save_task));
            checkTimeConflict();
        } else if (intent != null && intent.hasExtra("EXTRA_DATE")) {
            String passedDate = intent.getStringExtra("EXTRA_DATE");
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
            if (passedDate != null && passedDate.compareTo(today) >= 0) {
                selectedDate = passedDate;
            } else {
                selectedDate = today;
            }
            updateDateTimeText();
        }
    }

    private void setupCategorySpinner() {
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryNames);
        spinnerCategory.setAdapter(categoryAdapter);

        taskViewModel.getAllCategories().observe(this, categories -> {
            categoryList.clear();
            categoryNames.clear();

            if (categories != null && !categories.isEmpty()) {
                categoryList.addAll(categories);
                int defaultSelectionIndex = 0;

                for (int i = 0; i < categoryList.size(); i++) {
                    Category cat = categoryList.get(i);
                    String displayName = cat.getLocalizedName(this);
                    if ("SYS_FIXED".equals(cat.getName()) || cat.getType() == 5) {
                        displayName = "📌 " + displayName;
                    }
                    categoryNames.add(displayName);

                    if (cat.getId() == selectedCategoryId) {
                        defaultSelectionIndex = i;
                    }
                }

                categoryAdapter.notifyDataSetChanged();
                spinnerCategory.setSelection(defaultSelectionIndex);
            }
        });

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < categoryList.size()) {
                    Category cat = categoryList.get(position);
                    selectedCategoryId = cat.getId();

                    // Nếu chọn "Lịch cố định", tự động tắt tính vào Daily Balance và gợi ý lặp lại
                    if ("SYS_FIXED".equals(cat.getName()) || cat.getType() == 5) {
                        switchIncludeInBalance.setChecked(false);
                    }
                    checkTimeConflict();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_add_category_title));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText inputName = new EditText(this);
        inputName.setHint(getString(R.string.dialog_category_name_hint));
        inputName.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        layout.addView(inputName);

        final TextView tvTypePrompt = new TextView(this);
        tvTypePrompt.setText(getString(R.string.dialog_category_type_label));
        tvTypePrompt.setPadding(0, 24, 0, 8);
        tvTypePrompt.setTextSize(13);
        layout.addView(tvTypePrompt);

        final Spinner spinnerType = new Spinner(this);
        String[] types = new String[] {
                getString(R.string.type_focus),
                getString(R.string.type_health),
                getString(R.string.type_leisure),
                getString(R.string.type_personal),
                getString(R.string.type_fixed)
        };
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(typeAdapter);
        layout.addView(spinnerType);

        builder.setView(layout);

        builder.setPositiveButton(getString(R.string.btn_save_task), (dialog, which) -> {
            String categoryName = inputName.getText().toString().trim();
            if (!categoryName.isEmpty()) {
                int selectedType = spinnerType.getSelectedItemPosition() + 1;
                String assignedColor;
                switch (selectedType) {
                    case 1: assignedColor = "#2196F3"; break;
                    case 2: assignedColor = "#4CAF50"; break;
                    case 3: assignedColor = "#FF9800"; break;
                    case 5: assignedColor = "#F44336"; break;
                    default: assignedColor = "#9E9E9E"; break;
                }

                Category newCategory = new Category(categoryName, assignedColor, selectedType);
                taskViewModel.insertCategory(newCategory);
                Toast.makeText(this, getString(R.string.category_added), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            updateDateTimeText();
            checkTimeConflict();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);
        datePickerDialog.getDatePicker().setMinDate(todayStart.getTimeInMillis());
        datePickerDialog.show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            updateDateTimeText();
            checkTimeConflict();
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    @SuppressLint("SetTextI18n")
    private void updateDateTimeText() {
        if (!selectedTime.isEmpty() && !selectedDate.isEmpty()) {
            tvDateTimeSelected.setText(getString(R.string.datetime_selected) + selectedTime + " • " + selectedDate);
        } else {
            tvDateTimeSelected.setText(getString(R.string.datetime_not_selected));
        }
    }

    private void checkTimeConflict() {
        if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
            if (cardConflictWarning != null) cardConflictWarning.setVisibility(View.GONE);
            return;
        }

        int duration = 60;
        try {
            String durStr = edtDuration.getText().toString().trim();
            if (!durStr.isEmpty()) duration = Integer.parseInt(durStr);
        } catch (Exception ignored) {}

        taskViewModel.checkTimeConflictWithFixedSchedule(selectedDate, selectedTime, duration, currentTaskId, conflict -> {
            runOnUiThread(() -> {
                if (conflict != null && cardConflictWarning != null && tvConflictWarning != null) {
                    cardConflictWarning.setVisibility(View.VISIBLE);
                    String start = conflict.getTime() != null ? conflict.getTime() : "--:--";
                    String end = calculateEndTime(start, conflict.getEstimatedDuration());
                    tvConflictWarning.setText(getString(R.string.conflict_warning_format, conflict.getTitle(), start, end));
                } else if (cardConflictWarning != null) {
                    cardConflictWarning.setVisibility(View.GONE);
                }
            });
        });
    }

    private String calculateEndTime(String startTime, int durationMinutes) {
        try {
            String[] parts = startTime.split(":");
            int min = Integer.parseInt(parts[0].trim()) * 60 + Integer.parseInt(parts[1].trim()) + durationMinutes;
            int h = (min / 60) % 24;
            int m = min % 60;
            return String.format(Locale.getDefault(), "%02d:%02d", h, m);
        } catch (Exception e) {
            return startTime;
        }
    }

    private void showManageCategoriesDialog() {
        if (categoryList.isEmpty()) return;

        List<String> displayItems = new ArrayList<>();
        for (Category cat : categoryList) {
            displayItems.add("🗑 " + cat.getLocalizedName(this));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.category_manage_title));
        builder.setItems(displayItems.toArray(new String[0]), (dialog, which) -> {
            Category selected = categoryList.get(which);
            confirmDeleteCategory(selected);
        });
        builder.setNegativeButton(getString(R.string.btn_cancel), (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void confirmDeleteCategory(Category category) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_delete_category_confirm, category.getLocalizedName(this)))
                .setPositiveButton(getString(R.string.dialog_apply), (dialog, which) -> {
                    taskViewModel.deleteCategory(category);
                    Toast.makeText(this, getString(R.string.category_deleted), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.btn_cancel), (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void saveTask() {
        String title = edtTitle.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_title_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_select_datetime), Toast.LENGTH_SHORT).show();
            return;
        }

        int duration = 60;
        String durStr = edtDuration.getText().toString().trim();
        if (!durStr.isEmpty()) {
            try {
                duration = Integer.parseInt(durStr);
            } catch (NumberFormatException e) {
                duration = 60;
            }
        }

        boolean includeInBalance = switchIncludeInBalance.isChecked();

        int priority = 1;
        int checkedId = rgPriority.getCheckedRadioButtonId();
        if (checkedId == R.id.rbMedium) priority = 2;
        else if (checkedId == R.id.rbHigh) priority = 3;

        int repeatType = spinnerRepeat != null ? spinnerRepeat.getSelectedItemPosition() : 0;

        Task taskToSave = new Task();
        if (currentTaskId != -1) {
            taskToSave.setId(currentTaskId);
        }
        taskToSave.setTitle(title);
        taskToSave.setDescription(description);
        taskToSave.setDate(selectedDate);
        taskToSave.setTime(selectedTime);
        taskToSave.setPriority(priority);
        taskToSave.setCategoryId(selectedCategoryId);
        taskToSave.setEstimatedDuration(duration);
        taskToSave.setIncludeInBalance(includeInBalance);
        taskToSave.setCompleted(existingIsCompleted);
        taskToSave.setActualDuration(existingActualDuration);
        taskToSave.setRepeatType(repeatType);

        Context appContext = getApplicationContext();
        if (currentTaskId == -1) {
            taskViewModel.insertTask(taskToSave, newId -> {
                taskToSave.setId((int) newId);
                AlarmScheduler.scheduleTaskAlarm(appContext, taskToSave);
            });
        } else {
            taskViewModel.updateTask(taskToSave);
            if (!taskToSave.isCompleted()) {
                AlarmScheduler.scheduleTaskAlarm(this, taskToSave);
            } else {
                AlarmScheduler.cancelTaskAlarm(this, taskToSave);
            }
        }

        // Kiểm tra quyền "Thông báo" và "Báo thức & lời nhắc"
        boolean hasNotification = isNotificationPermissionGranted();
        boolean hasExactAlarm = isExactAlarmPermissionGranted();

        if (hasNotification && hasExactAlarm) {
            Toast.makeText(this, getString(R.string.task_saved_success), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Hướng dẫn người dùng mở công tắc ngay nếu chưa bật
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        if (!hasNotification && !hasExactAlarm) {
            builder.setTitle(R.string.dialog_permission_both_title)
                    .setMessage(R.string.dialog_permission_both_desc)
                    .setPositiveButton(R.string.btn_open_toggle_now, (dialog, which) -> {
                        openNotificationSettings();
                        openExactAlarmSettings();
                        finish();
                    });
        } else if (!hasNotification) {
            builder.setTitle(R.string.dialog_permission_notification_title)
                    .setMessage(R.string.dialog_permission_notification_desc)
                    .setPositiveButton(R.string.btn_open_toggle_now, (dialog, which) -> {
                        openNotificationSettings();
                        finish();
                    });
        } else {
            builder.setTitle(R.string.dialog_permission_exact_alarm_title)
                    .setMessage(R.string.dialog_permission_exact_alarm_desc)
                    .setPositiveButton(R.string.btn_open_toggle_now, (dialog, which) -> {
                        openExactAlarmSettings();
                        finish();
                    });
        }

        builder.setNegativeButton(R.string.btn_later, (dialog, which) -> {
            dialog.dismiss();
            finish();
        });

        builder.show();
    }

    private boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return NotificationManagerCompat.from(this).areNotificationsEnabled();
    }

    private boolean isExactAlarmPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    private void openNotificationSettings() {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}
