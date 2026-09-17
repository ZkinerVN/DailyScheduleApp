package com.example.dailyscheduleapp.data.entity;

import android.content.Context;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.example.dailyscheduleapp.R;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private String colorHex;
    private int type;

    public Category(String name, String colorHex, int type) {
        this.name = name;
        this.colorHex = colorHex;
        this.type = type;
    }

    // THUỘC TÍNH MỚI: Tự động dịch ngôn ngữ cho dữ liệu mẫu
    // @Ignore báo cho Room biết đây chỉ là hàm xử lý logic, không cần tạo cột trong Database
    @Ignore
    public String getLocalizedName(Context context) {
        if ("SYS_STUDY".equals(name)) return context.getString(R.string.cat_sys_study);
        if ("SYS_WORK".equals(name)) return context.getString(R.string.cat_sys_work);
        if ("SYS_SPORT".equals(name)) return context.getString(R.string.cat_sys_sport);
        if ("SYS_LEISURE".equals(name)) return context.getString(R.string.cat_sys_leisure);
        if ("SYS_PERSONAL".equals(name)) return context.getString(R.string.cat_sys_personal);
        if ("SYS_FIXED".equals(name)) return context.getString(R.string.cat_sys_fixed);

        return name;
    }

    // --- GETTER & SETTER ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
}