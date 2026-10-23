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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.dailyscheduleapp.R;
import com.example.dailyscheduleapp.service.PomodoroService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

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
        this.workTimeMillis = pomodoroService.getWorkTimeMillis();
        this.breakTimeMillis = pomodoroService.getBreakTimeMillis();
        updateUiThemeAndText();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbarPomodoro);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvMode = findViewById(R.id.tvMode);
        tvTimer = findViewById(R.id.tvTimer);
        tvCycleCount = findViewById(R.id.tvCycleCount);
        circularProgress = findViewById(R.id.circularTimerProgress);

        btnStartPause = findViewById(R.id.btnStartPause);
        btnReset = findViewById(R.id.btnReset);
        btnSkip = findViewById(R.id.btnSkip);
    }

    private void resolveIncomingTaskIntent() {
        boundTaskId = getIntent().getIntExtra("TASK_ID", -1);
        boundTaskTitle = getIntent().getStringExtra("TASK_TITLE");
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbarPomodoro);
        if (toolbar != null && boundTaskTitle != null && !boundTaskTitle.isEmpty()) {
            toolbar.setSubtitle("🎯 " + boundTaskTitle);
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

        updatePresetSelection();
    }

    private void setDuration(int minutes, boolean forWork) {
        if (pomodoroService != null && pomodoroService.isTimerRunning()) {
            Toast.makeText(this, getString(R.string.pomodoro_toast_reset_first), Toast.LENGTH_SHORT).show();
            return;
        }

        if (forWork) {
            workTimeMillis = (long) minutes * 60 * 1000L;
        } else {
            breakTimeMillis = (long) minutes * 60 * 1000L;
        }

        if (pomodoroService != null) {
            pomodoroService.setDurations(workTimeMillis, breakTimeMillis, longBreakTimeMillis);
        } else {
            if (forWork) {
                if (isWorkMode) {
                    totalDurationMillis = workTimeMillis;
                    timeLeftInMillis = workTimeMillis;
                }
            } else {
                if (!isWorkMode && !isLongBreakMode) {
                    totalDurationMillis = breakTimeMillis;
                    timeLeftInMillis = breakTimeMillis;
                }
            }
            updateUiThemeAndText();
        }

        Toast.makeText(this, getString(R.string.pomodoro_duration_toast, minutes), Toast.LENGTH_SHORT).show();
    }

    private void showCustomDurationDialog(boolean forWork) {
        if (pomodoroService != null && pomodoroService.isTimerRunning()) {
            Toast.makeText(this, getString(R.string.pomodoro_toast_reset_first), Toast.LENGTH_SHORT).show();
            return;
        }

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
    }

    @Override
    public void onSessionCompleted(boolean wasWorkMode) {
        // Notification, vibration and sound are handled by PomodoroService
    }

    private void updateUiThemeAndText() {
        int themeColor = isWorkMode ? Color.parseColor("#FF5722") : Color.parseColor("#388E3C");

        int modeText = isWorkMode
                ? R.string.pomodoro_focus_mode
                : (isLongBreakMode
                ? R.string.pomodoro_long_break_mode
                : R.string.pomodoro_break_mode);
        tvMode.setText(modeText);
        tvMode.setTextColor(themeColor);

        btnStartPause.setText(isTimerRunning ? getString(R.string.pomodoro_btn_pause) : getString(R.string.pomodoro_btn_start));
        btnStartPause.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.brand_primary)));
        btnStartPause.setTextColor(getColor(R.color.on_brand));
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
        int backgroundColor = selected ? getColor(R.color.brand_primary) : Color.TRANSPARENT;
        int textColor = selected ? getColor(R.color.on_brand) : getColor(R.color.brand_primary);
        button.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        button.setTextColor(textColor);
        button.setStrokeColor(ColorStateList.valueOf(getColor(R.color.outline)));
    }

    private void updateCountDownText() {
        int secondsRemaining = (int) (timeLeftInMillis / 1000);
        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;

        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
        circularProgress.setProgress(secondsRemaining);
    }
}
