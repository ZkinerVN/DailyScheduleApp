package com.example.dailyscheduleapp.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dailyscheduleapp.R;
import com.example.dailyscheduleapp.data.entity.Category;
import com.example.dailyscheduleapp.data.entity.Task;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {

    private final Map<Integer, Category> categoryMap = new HashMap<>();
    private OnTaskClickListener listener;

    private static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.isCompleted() == newItem.isCompleted()
                    && Objects.equals(oldItem.getTitle(), newItem.getTitle())
                    && Objects.equals(oldItem.getDescription(), newItem.getDescription())
                    && Objects.equals(oldItem.getDate(), newItem.getDate())
                    && Objects.equals(oldItem.getTime(), newItem.getTime())
                    && oldItem.getPriority() == newItem.getPriority()
                    && oldItem.getCategoryId() == newItem.getCategoryId()
                    && oldItem.getEstimatedDuration() == newItem.getEstimatedDuration()
                    && oldItem.getActualDuration() == newItem.getActualDuration()
                    && oldItem.isIncludeInBalance() == newItem.isIncludeInBalance()
                    && oldItem.getRepeatType() == newItem.getRepeatType();
        }
    };

    public TaskAdapter() {
        super(DIFF_CALLBACK);
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskDelete(Task task);
        void onTaskStatusChanged(Task task, boolean isCompleted);
        void onTaskPomodoro(Task task);
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<Task> tasks) {
        submitList(tasks != null ? new ArrayList<>(tasks) : null);
    }

    public void setCategories(List<Category> categories) {
        categoryMap.clear();
        if (categories != null) {
            for (Category cat : categories) {
                categoryMap.put(cat.getId(), cat);
            }
        }
        notifyItemRangeChanged(0, getItemCount());
    }

    public Task getTaskAt(int position) {
        if (position >= 0 && position < getItemCount()) {
            return getItem(position);
        }
        return null;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = getItem(position);
        if (task == null) return;
        Context context = holder.itemView.getContext();

        // Đảm bảo màu nền thẻ luôn ăn khớp theo Theme Surface
        holder.cardTask.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface));

        holder.tvTitle.setText(task.getTitle());
        holder.tvDescription.setText(task.getDescription());
        holder.tvDateTime.setText(task.getTime());

        // 1. Hiển thị Mức ưu tiên với Badge bo tròn & Dải màu Accent Strip
        int priorityStripColor;
        switch (task.getPriority()) {
            case 3:
                priorityStripColor = Color.parseColor("#FF1744"); // Vibrant Red
                holder.tvPriority.setText(context.getString(R.string.priority_high));
                holder.tvPriority.setTextColor(Color.parseColor("#E11D48"));
                holder.tvPriority.setBackground(createBadgeDrawable(context, "#FFE4E6"));
                break;
            case 2:
                priorityStripColor = Color.parseColor("#FF9100"); // Vibrant Orange
                holder.tvPriority.setText(context.getString(R.string.priority_medium));
                holder.tvPriority.setTextColor(Color.parseColor("#D97706"));
                holder.tvPriority.setBackground(createBadgeDrawable(context, "#FEF3C7"));
                break;
            default:
                priorityStripColor = Color.parseColor("#00C853"); // Vibrant Emerald Green
                holder.tvPriority.setText(context.getString(R.string.priority_low));
                holder.tvPriority.setTextColor(Color.parseColor("#059669"));
                holder.tvPriority.setBackground(createBadgeDrawable(context, "#ECFDF5"));
                break;
        }

        // 2. Hiển thị Danh mục (Đặc biệt: Lịch cố định có icon ghim 📌)
        Category category = categoryMap.get(task.getCategoryId());
        if (category != null) {
            String catName = category.getLocalizedName(context);
            if ("SYS_FIXED".equals(category.getName()) || category.getType() == 5) {
                catName = "📌 " + catName;
            }
            holder.tvCategory.setText(catName);
            try {
                holder.tvCategory.setTextColor(Color.parseColor(category.getColorHex()));
            } catch (Exception e) {
                holder.tvCategory.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            }
        } else {
            holder.tvCategory.setText(context.getString(R.string.cat_uncategorized));
            holder.tvCategory.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        }

        // 3. Hiển thị Tag Lặp lại (Recurring)
        if (task.getRepeatType() > 0) {
            holder.tvRepeat.setVisibility(View.VISIBLE);
            if (task.getRepeatType() == 1) {
                holder.tvRepeat.setText("🔄 " + context.getString(R.string.repeat_daily));
            } else if (task.getRepeatType() == 2) {
                holder.tvRepeat.setText("🔄 " + context.getString(R.string.repeat_weekdays));
            } else if (task.getRepeatType() == 3) {
                holder.tvRepeat.setText("🔄 " + context.getString(R.string.repeat_weekly));
            }
        } else {
            holder.tvRepeat.setVisibility(View.GONE);
        }

        // 4. Kiểm tra Quá hạn & Cảnh báo thị giác
        boolean isOverdue = isTaskOverdue(task);
        if (isOverdue && !task.isCompleted()) {
            holder.tvOverdue.setVisibility(View.VISIBLE);
            holder.cardTask.setStrokeColor(Color.parseColor("#FF1744"));
            holder.cardTask.setStrokeWidth(3);
            if (holder.viewPriorityStrip != null) {
                holder.viewPriorityStrip.setBackgroundColor(Color.parseColor("#D50000"));
            }
        } else {
            holder.tvOverdue.setVisibility(View.GONE);
            holder.cardTask.setStrokeColor(ContextCompat.getColor(context, R.color.outline));
            holder.cardTask.setStrokeWidth(2);
            if (holder.viewPriorityStrip != null) {
                holder.viewPriorityStrip.setBackgroundColor(priorityStripColor);
            }
        }

        // 5. Trạng thái Hoàn thành
        holder.cbCompleted.setOnCheckedChangeListener(null);
        holder.cbCompleted.setChecked(task.isCompleted());

        if (task.isCompleted()) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.tvTitle.invalidate();
            holder.cardTask.setAlpha(0.55f);
            holder.tvOverdue.setVisibility(View.GONE);
            if (holder.tvCompletedBadge != null) {
                holder.tvCompletedBadge.setVisibility(View.VISIBLE);
            }
            if (holder.viewPriorityStrip != null) {
                holder.viewPriorityStrip.setBackgroundColor(Color.parseColor("#BDBDBD"));
            }
            holder.btnPomodoro.setVisibility(View.GONE);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            holder.tvTitle.invalidate();
            holder.cardTask.setAlpha(1.0f);
            if (holder.tvCompletedBadge != null) {
                holder.tvCompletedBadge.setVisibility(View.GONE);
            }
            holder.btnPomodoro.setVisibility(View.VISIBLE);
        }

        holder.cbCompleted.setOnClickListener(v -> {
            boolean isChecked = holder.cbCompleted.isChecked();
            task.setCompleted(isChecked);
            if (isChecked) {
                holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                holder.tvTitle.invalidate();
                holder.cardTask.setAlpha(0.55f);
                holder.tvOverdue.setVisibility(View.GONE);
                if (holder.tvCompletedBadge != null) holder.tvCompletedBadge.setVisibility(View.VISIBLE);
                if (holder.viewPriorityStrip != null) holder.viewPriorityStrip.setBackgroundColor(Color.parseColor("#BDBDBD"));
                holder.btnPomodoro.setVisibility(View.GONE);
            } else {
                holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                holder.tvTitle.invalidate();
                holder.cardTask.setAlpha(1.0f);
                if (holder.tvCompletedBadge != null) holder.tvCompletedBadge.setVisibility(View.GONE);
                if (holder.viewPriorityStrip != null) holder.viewPriorityStrip.setBackgroundColor(priorityStripColor);
                holder.btnPomodoro.setVisibility(View.VISIBLE);
            }
            if (listener != null) {
                listener.onTaskStatusChanged(task, isChecked);
            }
        });

        holder.btnPomodoro.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskPomodoro(task);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskDelete(task);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task);
            }
        });
    }

    private boolean isTaskOverdue(Task task) {
        if (task.isCompleted() || task.getDate() == null || task.getTime() == null) {
            return false;
        }
        String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        return (task.getDate() + " " + task.getTime()).compareTo(currentDateTime) < 0;
    }

    private GradientDrawable createBadgeDrawable(Context context, String colorHex) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor(colorHex));
        float density = context.getResources().getDisplayMetrics().density;
        drawable.setCornerRadius(8f * density);
        return drawable;
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardTask;
        View viewPriorityStrip;
        CheckBox cbCompleted;
        TextView tvTitle, tvDescription, tvDateTime, tvPriority, tvCategory, tvRepeat, tvCompletedBadge, tvOverdue;
        ImageButton btnPomodoro, btnDelete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTask = itemView.findViewById(R.id.cardTask);
            viewPriorityStrip = itemView.findViewById(R.id.viewPriorityStrip);
            cbCompleted = itemView.findViewById(R.id.cbCompleted);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvRepeat = itemView.findViewById(R.id.tvRepeat);
            tvCompletedBadge = itemView.findViewById(R.id.tvCompletedBadge);
            tvOverdue = itemView.findViewById(R.id.tvOverdue);
            btnPomodoro = itemView.findViewById(R.id.btnPomodoro);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
