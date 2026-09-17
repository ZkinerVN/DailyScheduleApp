package com.example.dailyscheduleapp.ui.view;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;

import com.example.dailyscheduleapp.R;
import com.example.dailyscheduleapp.data.database.AppDatabase;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Locale;

public class PomodoroActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "pomodoro_notification_channel";
    private static final String CHANNEL_NAME = "Pomodoro Channel";

    private long workTimeMillis = 25 * 60 * 1000;
    private long breakTimeMillis = 5 * 60 * 1000;
    private long longBreakTimeMillis = 15 * 60 * 1000;
    private long totalDurationMillis = workTimeMillis;
    private long timeLeftInMillis = workTimeMillis;

    private int currentCycle = 1; // Hiệp 1 -> 4
    private boolean isTimerRunning = false;
    private boolean isWorkMode = true;

    private int boundTaskId = -1;

    private TextView tvMode, tvTimer, tvCycleCount, tvFocusTaskTitle;
    private MaterialButton btnStartPause, btnReset, btnSkip, btnCompleteTask;
    private CircularProgressIndicator circularProgress;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro);

        initViews();
        resolveIncomingTaskIntent();
        setupPresetButtons();

        btnStartPause.setOnClickListener(v -> {
            if (isTimerRunning) pauseTimer();
            else startTimer();
        });

        btnReset.setOnClickListener(v -> resetTimer());
        btnSkip.setOnClickListener(v -> skipToNextSession());

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
        tvFocusTaskTitle = findViewById(R.id.tvFocusTaskTitle);
        circularProgress = findViewById(R.id.circularTimerProgress);

        btnStartPause = findViewById(R.id.btnStartPause);
        btnReset = findViewById(R.id.btnReset);
        btnSkip = findViewById(R.id.btnSkip);
        btnCompleteTask = findViewById(R.id.btnCompleteTaskFromPomodoro);
    }

    private void resolveIncomingTaskIntent() {
        boundTaskId = getIntent().getIntExtra("TASK_ID", -1);
        String taskTitle = getIntent().getStringExtra("TASK_TITLE");

        if (boundTaskId != -1 && taskTitle != null && !taskTitle.isEmpty()) {
            tvFocusTaskTitle.setText(getString(R.string.pomodoro_working_on, taskTitle));
            btnCompleteTask.setVisibility(View.VISIBLE);
            btnCompleteTask.setOnClickListener(v -> {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase.getInstance(this).taskDao().markTaskAsCompleted(boundTaskId);
                });
                Toast.makeText(this, getString(R.string.pomodoro_task_completed_toast), Toast.LENGTH_SHORT).show();
                btnCompleteTask.setEnabled(false);
                btnCompleteTask.setAlpha(0.6f);
            });
        } else {
            tvFocusTaskTitle.setText(getString(R.string.pomodoro_general_focus));
            btnCompleteTask.setVisibility(View.GONE);
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

    private void setDuration(int minutes, boolean forWork) {
        if (isTimerRunning) {
            Toast.makeText(this, getString(R.string.pomodoro_toast_reset_first), Toast.LENGTH_SHORT).show();
            return;
        }

        if (forWork) {
            workTimeMillis = (long) minutes * 60 * 1000;
            if (isWorkMode) {
                totalDurationMillis = workTimeMillis;
                timeLeftInMillis = workTimeMillis;
            }
        } else {
            breakTimeMillis = (long) minutes * 60 * 1000;
            if (!isWorkMode) {
                totalDurationMillis = breakTimeMillis;
                timeLeftInMillis = breakTimeMillis;
            }
        }
        updateUiThemeAndText();
        Toast.makeText(this, getString(R.string.pomodoro_duration_toast, minutes), Toast.LENGTH_SHORT).show();
    }

    private void showCustomDurationDialog(boolean forWork) {
        if (isTimerRunning) {
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

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                notifyTimerFinished(isWorkMode);
                proceedToNextSession();
            }
        }.start();

        isTimerRunning = true;
        btnStartPause.setText(getString(R.string.pomodoro_btn_pause));
    }

    private void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isTimerRunning = false;
        btnStartPause.setText(getString(R.string.pomodoro_btn_resume));
    }

    private void resetTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isTimerRunning = false;
        timeLeftInMillis = totalDurationMillis;
        btnStartPause.setText(getString(R.string.pomodoro_btn_start));
        updateUiThemeAndText();
    }

    private void skipToNextSession() {
        if (countDownTimer != null) countDownTimer.cancel();
        isTimerRunning = false;
        proceedToNextSession();
    }

    private void proceedToNextSession() {
        if (isWorkMode) {
            // Hết giờ tập trung -> Chuyển sang nghỉ
            isWorkMode = false;
            if (currentCycle >= 4) {
                totalDurationMillis = longBreakTimeMillis;
                tvMode.setText(getString(R.string.pomodoro_long_break_mode));
            } else {
                totalDurationMillis = breakTimeMillis;
                tvMode.setText(getString(R.string.pomodoro_break_mode));
            }
        } else {
            // Hết giờ nghỉ -> Vào chu kỳ mới
            isWorkMode = true;
            totalDurationMillis = workTimeMillis;
            if (currentCycle >= 4) currentCycle = 1;
            else currentCycle++;
            tvMode.setText(getString(R.string.pomodoro_focus_mode));
        }

        timeLeftInMillis = totalDurationMillis;
        btnStartPause.setText(getString(R.string.pomodoro_btn_start));
        updateUiThemeAndText();
    }

    private void updateUiThemeAndText() {
        int themeColor = isWorkMode ? Color.parseColor("#FF5722") : Color.parseColor("#388E3C");

        tvMode.setText(isWorkMode ? getString(R.string.pomodoro_focus_mode) : getString(R.string.pomodoro_break_mode));
        tvMode.setTextColor(themeColor);
        btnStartPause.setBackgroundTintList(ColorStateList.valueOf(themeColor));
        circularProgress.setIndicatorColor(themeColor);

        tvCycleCount.setText(getString(R.string.pomodoro_cycle_format, currentCycle));
        circularProgress.setMax((int) (totalDurationMillis / 1000));
        updateCountDownText();
    }

    private void updateCountDownText() {
        int secondsRemaining = (int) (timeLeftInMillis / 1000);
        int minutes = secondsRemaining / 60;
        int seconds = secondsRemaining % 60;

        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
        circularProgress.setProgress(secondsRemaining);
    }

    private void notifyTimerFinished(boolean wasWorkMode) {
        String title = wasWorkMode ? getString(R.string.pomodoro_notif_focus_done_title) : getString(R.string.pomodoro_notif_break_done_title);
        String content = wasWorkMode ? getString(R.string.pomodoro_notif_focus_done_desc) : getString(R.string.pomodoro_notif_break_done_desc);

        wakeUpScreen();
        vibrateDevice();
        playAlarmSound();
        showHeadsUpNotification(title, content);
    }

    private void wakeUpScreen() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            @SuppressWarnings("deprecation")
            PowerManager.WakeLock wakeLock = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK |
                            PowerManager.ACQUIRE_CAUSES_WAKEUP |
                            PowerManager.ON_AFTER_RELEASE,
                    "DailySchedule:PomodoroWakeLock"
            );
            wakeLock.acquire(4000);
        }
    }

    private void vibrateDevice() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            long[] pattern = {0, 500, 200, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    private void playAlarmSound() {
        try {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), soundUri);
            if (ringtone != null) ringtone.play();
        } catch (Exception ignored) {}
    }

    private void showHeadsUpNotification(String title, String content) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.enableVibration(true);
            nm.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true);

        nm.notify(3003, builder.build());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}