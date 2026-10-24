package com.example.dailyscheduleapp.ui.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.dailyscheduleapp.R;
import com.example.dailyscheduleapp.data.database.AppDatabase;
import com.example.dailyscheduleapp.data.entity.Task;
import com.example.dailyscheduleapp.utils.AmbientSoundManager;
import com.example.dailyscheduleapp.service.PomodoroService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PomodoroActivity extends AppCompatActivity implements PomodoroService.PomodoroListener {

    private long workTimeMillis = 25 * 60 * 1000L;
    private long breakTimeMillis = 5 * 60 * 1000L;
    private long longBreakTimeMillis = 15 * 60 * 1000L;
    private long totalDurationMillis = workTimeMillis;
    private long timeLeftInMillis = workTimeMillis;

    private int currentCycle = 1;
    private boolean isTimerRunning = false;
    private boolean isWorkMode = true;
    private boolean isLongBreakMode = false;

    private int boundTaskId = -1;
    private String boundTaskTitle = "";

    private TextView tvMode, tvTimer, tvCycleCount;
    private MaterialButton btnStartPause, btnReset, btnSkip;
    private CircularProgressIndicator circularProgress;
    private MaterialCardView cardFocusTask;
    private TextView tvFocusTaskTitle;
    private ImageButton btnClearTask;
    private AmbientSoundManager ambientSoundManager;
    private MaterialButton btnAmbientOff, btnAmbientRain, btnAmbientWind, btnAmbientWaves;

    private PomodoroService pomodoroService;
    private boolean isBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PomodoroService.PomodoroBinder binder = (PomodoroService.PomodoroBinder) service;
            pomodoroService = binder.getService();
            isBound = true;
            pomodoroService.setListener(PomodoroActivity.this);
            if (boundTaskId != -1) {
                pomodoroService.setBoundTask(boundTaskId, boundTaskTitle);
            }
            syncStateFromService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            pomodoroService = null;
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro);

        initViews();
        resolveIncomingTaskIntent();
        setupPresetButtons();
        setupBoundTaskCard();
        setupAmbientSoundButtons();

        btnStartPause.setOnClickListener(v -> {
            ensureServiceStarted();
            if (pomodoroService != null) {
                if (pomodoroService.isTimerRunning()) {
                    pomodoroService.pauseTimer();
                } else {
                    pomodoroService.startTimer();
                }
            }
        });

        btnReset.setOnClickListener(v -> {
            if (pomodoroService != null) {
                pomodoroService.resetTimer();
            }
        });

        btnSkip.setOnClickListener(v -> {
            if (pomodoroService != null) {
                pomodoroService.skipToNextSession();
            }
        });

        updateUiThemeAndText();
    }

    private void initViews() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbarPomodoro);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        tvMode = findViewById(R.id.tvMode);
        tvTimer = findViewById(R.id.tvTimer);
        tvCycleCount = findViewById(R.id.tvCycleCount);
        btnStartPause = findViewById(R.id.btnStartPause);
        btnReset = findViewById(R.id.btnReset);
        btnSkip = findViewById(R.id.btnSkip);
        circularProgress = findViewById(R.id.circularTimerProgress);
        cardFocusTask = findViewById(R.id.cardFocusTask);
        tvFocusTaskTitle = findViewById(R.id.tvFocusTaskTitle);
        btnClearTask = findViewById(R.id.btnClearTask);
    }

    private void setupBoundTaskCard() {
        updateBoundTaskUi();

        if (cardFocusTask != null) {
            cardFocusTask.setOnClickListener(v -> {
                if (boundTaskId == -1) {
                    showPickTaskDialog();
                }
            });
        }

        if (btnClearTask != null) {
            btnClearTask.setOnClickListener(v -> {
                boundTaskId = -1;
                boundTaskTitle = "";
                if (pomodoroService != null) {
                    pomodoroService.setBoundTask(-1, "");
                }
                updateBoundTaskUi();
            });
        }
    }

    private void updateBoundTaskUi() {
        if (tvFocusTaskTitle == null) return;
        if (boundTaskId != -1 && !boundTaskTitle.isEmpty()) {
            tvFocusTaskTitle.setText(getString(R.string.pomodoro_focusing_on, boundTaskTitle));
            tvFocusTaskTitle.setTextColor(ContextCompat.getColor(this, R.color.brand_primary));
            if (btnClearTask != null) btnClearTask.setVisibility(View.VISIBLE);
        } else {
            tvFocusTaskTitle.setText(R.string.pomodoro_general_focus);
            tvFocusTaskTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            if (btnClearTask != null) btnClearTask.setVisibility(View.GONE);
        }
    }

    private void showPickTaskDialog() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Task> pendingTasks = AppDatabase.getInstance(this).taskDao().getIncompleteTasksSync();
            runOnUiThread(() -> {
                if (pendingTasks == null || pendingTasks.isEmpty()) {
                    Toast.makeText(this, getString(R.string.empty_state), Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] taskNames = new String[pendingTasks.size()];
                for (int i = 0; i < pendingTasks.size(); i++) {
                    Task t = pendingTasks.get(i);
                    taskNames[i] = t.getTitle() + (t.getTime() != null ? " (" + t.getTime() + ")" : "");
                }

                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.pomodoro_choose_task))
                        .setItems(taskNames, (dialog, which) -> {
                            Task chosen = pendingTasks.get(which);
                            boundTaskId = chosen.getId();
                            boundTaskTitle = chosen.getTitle();
                            if (pomodoroService != null) {
                                pomodoroService.setBoundTask(boundTaskId, boundTaskTitle);
                            }
                            updateBoundTaskUi();
                        })
                        .setNegativeButton(getString(R.string.btn_cancel), (dialog, which) -> dialog.dismiss())
                        .show();
            });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, PomodoroService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            if (pomodoroService != null) {
                pomodoroService.setListener(null);
            }
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ambientSoundManager != null) {
            ambientSoundManager.stop();
        }
    }

    private void ensureServiceStarted() {
        Intent intent = new Intent(this, PomodoroService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void syncStateFromService() {
        if (pomodoroService == null) return;
        this.timeLeftInMillis = pomodoroService.getTimeLeftInMillis();
        this.totalDurationMillis = pomodoroService.getTotalDurationMillis();
        this.isTimerRunning = pomodoroService.isTimerRunning();
        this.isWorkMode = pomodoroService.isWorkMode();
        this.isLongBreakMode = pomodoroService.isLongBreakMode();
        this.currentCycle = pomodoroService.getCurrentCycle();
        updateUiThemeAndText();
    }

    private void resolveIncomingTaskIntent() {
        if (getIntent().hasExtra("TASK_ID")) {
            boundTaskId = getIntent().getIntExtra("TASK_ID", -1);
            boundTaskTitle = getIntent().getStringExtra("TASK_TITLE");
            updateBoundTaskUi();
        }
    }

    private void setupPresetButtons() {
        findViewById(R.id.btnWork15).setOnClickListener(v -> setDuration(15, true));
        findViewById(R.id.btnWork25).setOnClickListener(v -> setDuration(25, true));
        findViewById(R.id.btnWork45).setOnClickListener(v -> setDuration(45, true));
        findViewById(R.id.btnWorkCustom).setOnClickListener(v -> showCustomDurationDialog(true));

        findViewById(R.id.btnBreak5).setOnClickListener(v -> setDuration(5, false));
        findViewById(R.id.btnBreak10).setOnClickListener(v -> setDuration(10, false));
        findViewById(R.id.btnBreak15).setOnClickListener(v -> setDuration(15, false));
        findViewById(R.id.btnBreakCustom).setOnClickListener(v -> showCustomDurationDialog(false));
    }

    private void setupAmbientSoundButtons() {
        ambientSoundManager = new AmbientSoundManager();

        btnAmbientOff = findViewById(R.id.btnAmbientOff);
        btnAmbientRain = findViewById(R.id.btnAmbientRain);
        btnAmbientWind = findViewById(R.id.btnAmbientWind);
        btnAmbientWaves = findViewById(R.id.btnAmbientWaves);

        if (btnAmbientOff != null) btnAmbientOff.setOnClickListener(v -> selectAmbientSound(AmbientSoundManager.SOUND_OFF));
        if (btnAmbientRain != null) btnAmbientRain.setOnClickListener(v -> selectAmbientSound(AmbientSoundManager.SOUND_RAIN));
        if (btnAmbientWind != null) btnAmbientWind.setOnClickListener(v -> selectAmbientSound(AmbientSoundManager.SOUND_WIND));
        if (btnAmbientWaves != null) btnAmbientWaves.setOnClickListener(v -> selectAmbientSound(AmbientSoundManager.SOUND_WAVES));

        updateAmbientButtonsUi();
    }

    private void selectAmbientSound(int type) {
        if (ambientSoundManager != null) {
            ambientSoundManager.setSoundType(type);
            if (isTimerRunning && isWorkMode && type != AmbientSoundManager.SOUND_OFF) {
                ambientSoundManager.start();
            }
        }
        updateAmbientButtonsUi();
    }

    private void updateAmbientButtonsUi() {
        int activeType = ambientSoundManager != null ? ambientSoundManager.getCurrentSoundType() : AmbientSoundManager.SOUND_OFF;
        int activeColor = ContextCompat.getColor(this, R.color.brand_primary);
        int onActiveColor = ContextCompat.getColor(this, R.color.on_brand);
        int inactiveTextColor = ContextCompat.getColor(this, R.color.text_primary);

        highlightButton(btnAmbientOff, activeType == AmbientSoundManager.SOUND_OFF, activeColor, onActiveColor, inactiveTextColor);
        highlightButton(btnAmbientRain, activeType == AmbientSoundManager.SOUND_RAIN, activeColor, onActiveColor, inactiveTextColor);
        highlightButton(btnAmbientWind, activeType == AmbientSoundManager.SOUND_WIND, activeColor, onActiveColor, inactiveTextColor);
        highlightButton(btnAmbientWaves, activeType == AmbientSoundManager.SOUND_WAVES, activeColor, onActiveColor, inactiveTextColor);
    }

    private void highlightButton(MaterialButton btn, boolean isSelected, int activeColor, int onActiveColor, int inactiveTextColor) {
        if (btn == null) return;
        if (isSelected) {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(activeColor));
            btn.setTextColor(onActiveColor);
            btn.setStrokeWidth(0);
        } else {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
            btn.setTextColor(inactiveTextColor);
            btn.setStrokeWidth(2);
        }
    }

    private void setDuration(int minutes, boolean forWork) {
        long millis = minutes * 60 * 1000L;
        if (forWork) {
            workTimeMillis = millis;
        } else {
            breakTimeMillis = millis;
        }

        if (pomodoroService != null) {
            pomodoroService.setDurations(workTimeMillis, breakTimeMillis, longBreakTimeMillis);
        } else {
            if (forWork && isWorkMode) {
                timeLeftInMillis = millis;
                totalDurationMillis = millis;
            } else if (!forWork && !isWorkMode) {
                timeLeftInMillis = millis;
                totalDurationMillis = millis;
            }
        }
        updateUiThemeAndText();
    }

    private void showCustomDurationDialog(boolean forWork) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.pomodoro_dialog_custom_title));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("25");
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.dialog_apply), (dialog, which) -> {
            String value = input.getText().toString().trim();
            if (!value.isEmpty()) {
                int mins = Integer.parseInt(value);
                if (mins > 0 && mins <= 180) {
                    setDuration(mins, forWork);
                } else {
                    Toast.makeText(this, getString(R.string.pomodoro_dialog_error), Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(getString(R.string.btn_cancel), (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onTick(long timeLeftInMillis, long totalDurationMillis) {
        this.timeLeftInMillis = timeLeftInMillis;
        this.totalDurationMillis = totalDurationMillis;
        updateCountDownText();
    }

    @Override
    public void onStateChanged(boolean isRunning, boolean isWorkMode, boolean isLongBreakMode, int currentCycle) {
        this.isTimerRunning = isRunning;
        this.isWorkMode = isWorkMode;
        this.isLongBreakMode = isLongBreakMode;
        this.currentCycle = currentCycle;
        updateUiThemeAndText();

        if (isRunning && isWorkMode) {
            if (ambientSoundManager != null && ambientSoundManager.getCurrentSoundType() != AmbientSoundManager.SOUND_OFF) {
                ambientSoundManager.start();
            }
        } else {
            if (ambientSoundManager != null) {
                ambientSoundManager.stop();
            }
        }
    }

    @Override
    public void onSessionCompleted(boolean wasWorkMode) {
        runOnUiThread(() -> {
            if (wasWorkMode && boundTaskId != -1 && !boundTaskTitle.isEmpty()) {
                int mins = (int) (workTimeMillis / (60 * 1000L));
                Toast.makeText(this, getString(R.string.pomodoro_session_credited, mins, boundTaskTitle), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateUiThemeAndText() {
        int themeColor = isWorkMode
                ? ContextCompat.getColor(this, R.color.brand_primary)
                : ContextCompat.getColor(this, R.color.accent_health);

        int modeText = isWorkMode
                ? R.string.pomodoro_focus_mode
                : (isLongBreakMode
                ? R.string.pomodoro_long_break_mode
                : R.string.pomodoro_break_mode);
        tvMode.setText(modeText);
        tvMode.setTextColor(themeColor);

        btnStartPause.setText(isTimerRunning ? getString(R.string.pomodoro_btn_pause) : getString(R.string.pomodoro_btn_start));
        btnStartPause.setBackgroundTintList(ColorStateList.valueOf(themeColor));
        btnStartPause.setTextColor(ContextCompat.getColor(this, R.color.on_brand));
        circularProgress.setIndicatorColor(themeColor);

        tvCycleCount.setText(getString(R.string.pomodoro_cycle_format, currentCycle));
        circularProgress.setMax((int) (totalDurationMillis / 1000));
        updatePresetSelection();
        updateCountDownText();
    }

    private void updatePresetSelection() {
        boolean standardWorkDuration = workTimeMillis == 15 * 60 * 1000L
                || workTimeMillis == 25 * 60 * 1000L
                || workTimeMillis == 45 * 60 * 1000L;
        setPresetSelected(R.id.btnWork15, workTimeMillis == 15 * 60 * 1000L);
        setPresetSelected(R.id.btnWork25, workTimeMillis == 25 * 60 * 1000L);
        setPresetSelected(R.id.btnWork45, workTimeMillis == 45 * 60 * 1000L);
        setPresetSelected(R.id.btnWorkCustom, !standardWorkDuration);

        boolean standardBreakDuration = breakTimeMillis == 5 * 60 * 1000L
                || breakTimeMillis == 10 * 60 * 1000L
                || breakTimeMillis == 15 * 60 * 1000L;
        setPresetSelected(R.id.btnBreak5, breakTimeMillis == 5 * 60 * 1000L);
        setPresetSelected(R.id.btnBreak10, breakTimeMillis == 10 * 60 * 1000L);
        setPresetSelected(R.id.btnBreak15, breakTimeMillis == 15 * 60 * 1000L);
        setPresetSelected(R.id.btnBreakCustom, !standardBreakDuration);
    }

    private void setPresetSelected(int buttonId, boolean selected) {
        MaterialButton button = findViewById(buttonId);
        if (button == null) return;
        int backgroundColor = selected ? ContextCompat.getColor(this, R.color.brand_primary) : Color.TRANSPARENT;
        int textColor = selected ? ContextCompat.getColor(this, R.color.on_brand) : ContextCompat.getColor(this, R.color.brand_primary);
        button.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        button.setTextColor(textColor);
        button.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.outline)));
    }

    private void updateCountDownText() {
        int secondsRemaining = (int) (timeLeftInMillis / 1000);
        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;

        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
        circularProgress.setProgress(secondsRemaining);
    }
}
