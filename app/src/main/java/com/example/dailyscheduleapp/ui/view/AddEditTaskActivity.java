package com.example.dailyscheduleapp.ui.view;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.dailyscheduleapp.R;
import com.example.dailyscheduleapp.data.dao.CategoryDao;
import com.example.dailyscheduleapp.data.database.AppDatabase;
import com.example.dailyscheduleapp.data.entity.Category;
import com.example.dailyscheduleapp.data.entity.Task;
import com.example.dailyscheduleapp.ui.viewmodel.TaskViewModel;
import com.example.dailyscheduleapp.utils.AlarmScheduler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddEditTaskActivity extends AppCompatActivity {
    private EditText edtTitle, edtDescription, edtDuration;
    private SwitchCompat switchIncludeInBalance;
    private Button btnSelectDate, btnSelectTime, btnSave;
    private ImageButton btnMicro, btnAddCategory;
    private TextView tvDateTimeSelected;
    private RadioGroup rgPriority;
    private Spinner spinnerCategory;

    private TaskViewModel taskViewModel;
    private String selectedDate = "";
    private String selectedTime = "";
    private int currentTaskId = -1; // -1: Thêm mới, khác -1: Chỉnh sửa task cũ

    // Quản lý danh mục trong Spinner
    private final List<Category> categoryList = new ArrayList<>();
    private final List<String> categoryNames = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;
    private int selectedCategoryId = 1;

    // Bộ lắng nghe kết quả trả về từ giọng nói
    private final ActivityResultLauncher<Intent> voiceLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> text = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (text != null && !text.isEmpty()) {
                        String spokenText = text.get(0);
                        spokenText = spokenText.substring(0, 1).toUpperCase() + spokenText.substring(1);
                        edtTitle.setText(spokenText);
                    }
                }
            }
    );

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
        btnMicro = findViewById(R.id.btnMicro);
        tvDateTimeSelected = findViewById(R.id.tvDateTimeSelected);
        rgPriority = findViewById(R.id.rgPriority);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnAddCategory = findViewById(R.id.btnAddCategory);

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        // Kiểm tra xem Activity mở lên để THÊM MỚI hay SỬA CÔNG VIỆC
        checkIntentData();

        // Cấu hình Spinner và nạp danh mục
        setupCategorySpinner();

        // Các sự kiện Click
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectTime.setOnClickListener(v -> showTimePicker());
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        btnMicro.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.task_title_hint));

            try {
                voiceLauncher.launch(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Voice input not supported", Toast.LENGTH_SHORT).show();
            }
        });

        btnSave.setOnClickListener(v -> saveTask());
        ImageButton btnManageCategory = findViewById(R.id.btnManageCategory);
        if (btnManageCategory != null) {
            btnManageCategory.setOnClickListener(v -> showManageCategoriesDialog());
        }
        Button btnCancel = findViewById(R.id.btnCancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }
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
            btnSave.setText(getString(R.string.btn_save_task));
        }
    }

    private void setupCategorySpinner() {
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryNames);
        spinnerCategory.setAdapter(categoryAdapter);

        AppDatabase.getInstance(this).categoryDao().getAllCategories().observe(this, categories -> {
            categoryList.clear();
            categoryNames.clear();

            if (categories != null && !categories.isEmpty()) {
                categoryList.addAll(categories);
                int defaultSelectionIndex = 0;

                for (int i = 0; i < categories.size(); i++) {
                    Category cat = categories.get(i);
                    categoryNames.add(cat.getLocalizedName(AddEditTaskActivity.this));

                    if (cat.getId() == selectedCategoryId) {
                        defaultSelectionIndex = i;
                    }
                }
                categoryAdapter.notifyDataSetChanged();
                spinnerCategory.setSelection(defaultSelectionIndex);
            } else {
                categoryNames.add("No Category");
                categoryAdapter.notifyDataSetChanged();
            }
        });

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!categoryList.isEmpty() && position < categoryList.size()) {
                    selectedCategoryId = categoryList.get(position).getId();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_add_category_title));

        // Tạo layout dạng động cho Dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 12);

        // 1. Ô nhập tên danh mục
        final EditText inputName = new EditText(this);
        inputName.setHint(getString(R.string.dialog_category_name_hint));
        layout.addView(inputName);

        // 2. Nhãn chọn loại năng lượng
        TextView tvTypeLabel = new TextView(this);
        tvTypeLabel.setText(getString(R.string.dialog_category_type_label));
        tvTypeLabel.setTextSize(14f);
        tvTypeLabel.setPadding(0, 24, 0, 8);
        layout.addView(tvTypeLabel);

        // 3. Spinner chọn loại năng lượng
        final Spinner spinnerType = new Spinner(this);
        String[] types = new String[]{
                getString(R.string.type_focus),    // Vị trí 0 -> Type 1
                getString(R.string.type_health),   // Vị trí 1 -> Type 2
                getString(R.string.type_leisure),  // Vị trí 2 -> Type 3
                getString(R.string.type_personal), // Vị trí 3 -> Type 4
                getString(R.string.type_fixed)     // Vị trí 4 -> Type 5
        };
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(typeAdapter);
        layout.addView(spinnerType);

        builder.setView(layout);

        builder.setPositiveButton(getString(R.string.dialog_apply), (dialog, which) -> {
            String categoryName = inputName.getText().toString().trim();
            if (!categoryName.isEmpty()) {
                // Vị trí chọn + 1 tương ứng với: 1=FOCUS, 2=HEALTH, 3=LEISURE, 4=PERSONAL, 5=FIXED
                int selectedType = spinnerType.getSelectedItemPosition() + 1;

                // Gán màu nhận diện tự động theo loại năng lượng
                String assignedColor;
                switch (selectedType) {
                    case 1: assignedColor = "#2196F3"; break; // Focus: Xanh dương
                    case 2: assignedColor = "#4CAF50"; break; // Health: Xanh lá
                    case 3: assignedColor = "#FF9800"; break; // Leisure: Cam
                    case 5: assignedColor = "#F44336"; break; // Fixed: Đỏ
                    default: assignedColor = "#9E9E9E"; break; // Personal: Xám
                }

                Category newCategory = new Category(categoryName, assignedColor, selectedType);
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase.getInstance(getApplicationContext()).categoryDao().insert(newCategory);
                });
                Toast.makeText(this, getString(R.string.category_added), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            updateDateTimeText();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            updateDateTimeText();
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private void updateDateTimeText() {
        if (!selectedTime.isEmpty() && !selectedDate.isEmpty()) {
            tvDateTimeSelected.setText(getString(R.string.datetime_selected) + selectedTime + " " + selectedDate);
        } else {
            tvDateTimeSelected.setText(getString(R.string.datetime_not_selected));
        }
    }
    private void showManageCategoriesDialog() {
        if (categoryList.isEmpty()) return;

        List<String> displayItems = new ArrayList<>();
        for (Category cat : categoryList) {
            // Cho phép xóa mọi danh mục
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
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        CategoryDao catDao = AppDatabase.getInstance(getApplicationContext()).categoryDao();
                        catDao.delete(category);
                    });
                    Toast.makeText(this, getString(R.string.category_deleted), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.btn_cancel), (dialog, which) -> dialog.dismiss())
                .show();
    }
    private void saveTask() {
        String title = edtTitle.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();

        if (title.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_fill_info), Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. ĐÃ SỬA: Lấy đúng thời lượng do người dùng nhập
        int duration = 60;
        String durStr = edtDuration.getText().toString().trim();
        if (!durStr.isEmpty()) {
            try {
                duration = Integer.parseInt(durStr);
            } catch (NumberFormatException e) {
                duration = 60;
            }
        }

        // ĐÃ SỬA: Lấy đúng trạng thái công tắc Daily Balance
        boolean includeInBalance = switchIncludeInBalance.isChecked();

        int priority = 1;
        int checkedId = rgPriority.getCheckedRadioButtonId();
        if (checkedId == R.id.rbMedium) priority = 2;
        else if (checkedId == R.id.rbHigh) priority = 3;

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

        if (currentTaskId == -1) {
            taskViewModel.insertTask(taskToSave);
        } else {
            taskViewModel.updateTask(taskToSave);
        }
        AlarmScheduler.scheduleTaskAlarm(this, taskToSave);

        finish();
    }
}