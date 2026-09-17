package com.example.dailyscheduleapp.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dailyscheduleapp.R;
import com.example.dailyscheduleapp.data.entity.Category;
import com.example.dailyscheduleapp.data.entity.Task;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList = new ArrayList<>();
    private final Map<Integer, Category> categoryMap = new HashMap<>();
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskDelete(Task task);
        void onTaskStatusChanged(Task task, boolean isCompleted);
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<Task> tasks) {
        this.taskList = tasks != null ? tasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCategories(List<Category> categories) {
        categoryMap.clear();
        if (categories != null) {
            for (Category cat : categories) {
                categoryMap.put(cat.getId(), cat);
            }
        }
        notifyDataSetChanged();
    }

    public Task getTaskAt(int position) {
        return taskList.get(position);
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText(task.getTitle());
        holder.tvDescription.setText(task.getDescription());
        holder.tvDateTime.setText(task.getTime());

        // 1. Hiển thị Mức ưu tiên thuần túy đa ngôn ngữ
        switch (task.getPriority()) {
            case 3:
                holder.tvPriority.setText(context.getString(R.string.priority_high));
                holder.tvPriority.setTextColor(Color.parseColor("#D32F2F"));
                holder.tvPriority.setBackgroundColor(Color.parseColor("#FFEBEE"));
                break;
            case 2:
                holder.tvPriority.setText(context.getString(R.string.priority_medium));
                holder.tvPriority.setTextColor(Color.parseColor("#EF6C00"));
                holder.tvPriority.setBackgroundColor(Color.parseColor("#FFF3E0"));
                break;
            default:
                holder.tvPriority.setText(context.getString(R.string.priority_low));
                holder.tvPriority.setTextColor(Color.parseColor("#2E7D32"));
                holder.tvPriority.setBackgroundColor(Color.parseColor("#E8F5E9"));
                break;
        }

        // 2. Hiển thị Danh mục
        Category category = categoryMap.get(task.getCategoryId());
        if (category != null) {
            holder.tvCategory.setText(category.getLocalizedName(context));
            try {
                holder.tvCategory.setTextColor(Color.parseColor(category.getColorHex()));
            } catch (Exception e) {
                holder.tvCategory.setTextColor(Color.GRAY);
            }
        } else {
            holder.tvCategory.setText(context.getString(R.string.cat_uncategorized));
            holder.tvCategory.setTextColor(Color.GRAY);
        }

        // 3. Kiểm tra Quá hạn & Cảnh báo thị giác
        boolean isOverdue = isTaskOverdue(task);
        if (isOverdue && !task.isCompleted()) {
            holder.tvOverdue.setVisibility(View.VISIBLE);
            holder.cardTask.setStrokeColor(Color.parseColor("#D32F2F"));
            holder.cardTask.setStrokeWidth(4);
        } else {
            holder.tvOverdue.setVisibility(View.GONE);
            holder.cardTask.setStrokeColor(Color.parseColor("#EAEAEA"));
            holder.cardTask.setStrokeWidth(2);
        }

        // 4. Trạng thái Hoàn thành
        holder.cbCompleted.setOnCheckedChangeListener(null);
        holder.cbCompleted.setChecked(task.isCompleted());

        if (task.isCompleted()) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(Color.GRAY);
            holder.cardTask.setAlpha(0.55f);
            holder.tvOverdue.setVisibility(View.GONE);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setTextColor(Color.BLACK);
            holder.cardTask.setAlpha(1.0f);
        }

        holder.cbCompleted.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskStatusChanged(task, holder.cbCompleted.isChecked());
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
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Calendar taskCal = Calendar.getInstance();
            taskCal.setTime(sdf.parse(task.getDate() + " " + task.getTime()));
            return taskCal.getTimeInMillis() < System.currentTimeMillis();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardTask;
        CheckBox cbCompleted;
        TextView tvTitle, tvDescription, tvDateTime, tvPriority, tvCategory, tvOverdue;
        ImageButton btnDelete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTask = itemView.findViewById(R.id.cardTask);
            cbCompleted = itemView.findViewById(R.id.cbCompleted);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvOverdue = itemView.findViewById(R.id.tvOverdue);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}