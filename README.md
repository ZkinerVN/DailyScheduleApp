# Daily Schedule App (Lịch Trình Hàng Ngày) 📅⏱️🌱

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Language-Java%2011-orange.svg)](https://www.java.com/)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Repository-blue.svg)](https://developer.android.com/topic/architecture)
[![Database](https://img.shields.io/badge/Database-Room%20SQLite-red.svg)](https://developer.android.com/training/data-storage/room)
[![UI](https://img.shields.io/badge/UI-Material%20Design%203-purple.svg)](https://m3.material.io/)

> **Ứng dụng quản lý lịch trình thông minh kết hợp Thước đo Cân bằng Năng lượng (Daily Energy Balance), Nuôi Cây Năng Lượng (Gamification), Đồng hồ Pomodoro và Báo thức đa tầng chuẩn Android 13/14.**

---

## 🌟 Triết lý ứng dụng (Core Philosophy)

Khác với các ứng dụng To-Do List thông thường chỉ tập trung vào việc "hoàn thành càng nhiều việc càng tốt" dễ dẫn đến kiệt sức (burnout), **Daily Schedule App** được xây dựng dựa trên triết lý **"Cân bằng năng lượng cuộc sống" (Daily Energy Balance)**:
- 🔴 **Tập trung (Focus):** Học tập, nghiên cứu và làm việc chuyên sâu.
- 🟢 **Sức khỏe (Health):** Vận động thể thao, rèn luyện thể chất và nạp lại thể lực.
- 🟡 **Giải trí (Leisure):** Nghỉ ngơi, thư giãn, chơi game hoặc sở thích cá nhân.

Mỗi ngày, ứng dụng giúp bạn theo dõi, kiểm soát và duy trì sự cân bằng hài hòa giữa ba yếu tố này để làm việc hiệu quả mà vẫn bảo vệ sức khỏe lâu dài.

---

## 🚀 Tính năng nổi bật (Key Features)

### 1. 📊 Thước đo Cân bằng Năng lượng (Daily Energy Balance)
- **Thanh đo 3 màu trực quan:** Hiển thị thời gian thực tế đã lên lịch so với hạn mức trong ngày.
- **Phân tích nhận xét (Smart Insights):** Tự động phát hiện và cảnh báo khi bạn làm việc quá tải, giải trí quá giờ hoặc thiếu vận động thể chất.
- **Cấu hình hạn mức linh hoạt (DayConfig):** Tùy chỉnh số phút mục tiêu cho từng ngày hoặc sử dụng các bộ mẫu có sẵn:
  - *Bình thường (Normal):* 4h Học/Làm, 30p Thể thao, 1.5h Giải trí.
  - *Cuối tuần (Weekend):* 2h Học/Làm, 45p Thể thao, 3h Giải trí.
  - *Mùa thi (Exam Season):* 6h Học/Làm, 20p Vận động, 45p Giải trí.

### 2. 🌱 Cây Năng Lượng (Energy Plant Gamification)
- Biến thước đo năng lượng thành một **"sinh mệnh sống"** đồng hành cùng bạn suốt ngày dài.
- Tự động phản ứng theo 6 trạng thái sinh trưởng:
  - 🌱 **Mầm non khởi đầu:** Khi mới bắt đầu ngày mới, cây nhắc bạn lên lịch và bắt đầu hành động.
  - 🌸 **Cây nở hoa rực rỡ (Bậc Thầy Cân Bằng):** Đạt điểm cân bằng lý tưởng (85 - 100 điểm) giữa học tập, sức khỏe và giải trí.
  - 🌳 **Cây phát triển xanh tốt:** Duy trì nhịp độ làm việc và rèn luyện ổn định.
  - 💧 **Cây khát nước:** Khi bạn làm việc liên tục mà bỏ quên vận động $\rightarrow$ nhắc bạn vận động để tưới cây.
  - 🥀 **Cây khô héo vì quá tải:** Khi thời gian làm việc vượt quá ngưỡng an toàn $\rightarrow$ cảnh báo kiệt sức.
  - 🌿 **Cây mọc cỏ dại:** Thời gian giải trí vượt hạn mức $\rightarrow$ nhắc bạn dọn cỏ để quay lại mục tiêu.

### 3. ✨ Thẻ Tổng Kết Ngày (Daily Energy Wrap Card)
- Lấy cảm hứng từ phong cách **Spotify Wrapped** và **Instagram Story**.
- Kết xuất hình ảnh đồ họa bắt mắt với nền gradient biến đổi theo trạng thái Cây Năng Lượng.
- **Huy hiệu danh hiệu ngày:** *🌟 BẬC THẦY CÂN BẰNG*, *🔥 CHIẾN BINH NĂNG SUẤT*, *🌱 MẦM NON HY VỌNG*...
- **Thống kê tổng hợp:** Thời gian Tập trung, Vận động, Giải trí, Tỷ lệ hoàn thành công việc và Điểm cân bằng / 100.
- **Tương tác 1 chạm:**
  - 📸 **Lưu ảnh:** Chụp View độ phân giải cao và lưu trực tiếp vào thư viện ảnh (`Pictures/DailySchedule`).
  - 🚀 **Chia sẻ Story:** Tích hợp `FileProvider` chuẩn Android, mở bảng chia sẻ để gửi ảnh poster sang Zalo, Facebook Messenger, Instagram, Telegram...

### 4. 🍅 Đồng hồ Pomodoro gắn liền nhiệm vụ
- Cho phép liên kết phiên tập trung trực tiếp với một công việc cụ thể.
- Tự động cộng dồn thời gian tập trung thực tế vào công việc và đồng bộ sang thước đo Daily Energy Balance.
- Hỗ trợ các hiệp: **Phiên tập trung (Focus) $\rightarrow$ Nghỉ ngắn (Break) $\rightarrow$ Nghỉ dài (Long Break)**.
- Tùy chỉnh thời gian linh hoạt (1 - 180 phút).
- Chạy nền an toàn với **Foreground Service** hiển thị đếm ngược thời gian thực trên thanh thông báo.

### 5. ⏰ Hệ thống Báo thức & Đánh thức màn hình đa tầng
Tối ưu hóa đặc biệt cho Android 13/14 và các dòng máy có cơ chế quản lý pin khắt khe (Nubia, Xiaomi, Samsung, Oppo...):
- **Mức 1 (Ưu tiên Thấp):** Làm sáng màn hình, chuông ting nhẹ, rung ngắn (150ms).
- **Mức 2 (Ưu tiên Trung bình):** Làm sáng màn hình, rung vừa, chuông tiêu chuẩn, hiển thị biểu ngữ Heads-up.
- **Mức 3 (Ưu tiên Cao):** 
  - Đánh thức màn hình khóa (`USE_FULL_SCREEN_INTENT`, `setShowWhenLocked`, `setTurnScreenOn`).
  - Kích hoạt màn hình báo thức toàn màn hình `AlarmAlertActivity`.
  - Phát chuông và rung lặp liên tục (`FLAG_INSISTENT`) cho đến khi người dùng thao tác.
- **3 Nút hành động trên mọi mức:**
  - `[✅ Đã xong]`: Đánh dấu hoàn thành ngay từ thanh thông báo hoặc màn hình khóa.
  - `[⏳ Hoãn 10 phút]`: Tự động lập lịch báo lại sau 10 phút.
  - `[🍅 Pomodoro]`: Chuyển thẳng sang đồng hồ Pomodoro và gán đúng nhiệm vụ hiện tại.

### 6. 🛡️ Kiểm tra & Hướng dẫn cấp quyền thông minh
- Khi nhấn **Lưu công việc**, hệ thống tự động kiểm tra xem thiết bị đã bật quyền:
  1. **Thông báo** (`POST_NOTIFICATIONS` trên Android 13+).
  2. **Báo thức & lời nhắc** (`SCHEDULE_EXACT_ALARM` trên Android 12+).
- Nếu phát hiện công tắc đang tắt, ứng dụng sẽ hiện hộp thoại với nút **"Mở công tắc ngay"** dẫn người dùng thẳng đến màn hình cài đặt tương ứng.
- Đăng ký `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`: Ngay khi người dùng gạt bật công tắc, hệ thống sẽ tự động khôi phục và lập lịch báo thức chính xác từng giây cho toàn bộ công việc chưa hoàn thành.

### 7. 🎨 Giao diện & Trải nghiệm người dùng
- Thiết kế hiện đại theo chuẩn Material 3.
- Hỗ trợ đầy đủ **Chế độ Tối (Dark Mode)** và **Chế độ Sáng (Light Mode)**.
- Hỗ trợ **Đa ngôn ngữ:** Chuyển đổi linh hoạt giữa Tiếng Việt (VI) và Tiếng Anh (EN).
- Hỗ trợ nhập liệu bằng **Giọng nói (Voice-to-Text)**.

---

## 🏗️ Kiến trúc kỹ thuật (Architecture)

Dự án tuân thủ nghiêm ngặt mô hình kiến trúc **MVVM (Model - View - ViewModel)** kết hợp **Repository Pattern**:

```
           +-------------------------------------------------------+
           |                     UI LAYER                          |
           |   Activities: MainActivity, PomodoroActivity,         |
           |               StatisticsActivity, AlarmAlertActivity  |
           |   Adapters, Custom Dialogs (DailyWrapDialog)          |
           +---------------------------+---------------------------+
                                       |
                               Observes LiveData
                                       |
           +---------------------------v---------------------------+
           |                  VIEWMODEL LAYER                      |
           |                     TaskViewModel                     |
           +---------------------------+---------------------------+
                                       |
                                Calls Operations
                                       |
           +---------------------------v---------------------------+
           |                  REPOSITORY LAYER                     |
           |            TaskRepository, DayConfigRepository        |
           +---------------------------+---------------------------+
                                       |
                       +---------------+---------------+
                       |                               |
           +-----------v-----------+       +-----------v-----------+
           |     LOCAL DATABASE    |       |   SYSTEM SERVICES     |
           |   AppDatabase (Room)  |       |   AlarmManager        |
           |   TaskDao, DayConfigDao|      |   NotificationManager |
           |   CategoryDao         |       |   PowerManager (Wake) |
           +-----------------------+       +-----------------------+
```

---

## 📂 Cấu trúc thư mục (Project Structure)

```
app/src/main/
├── AndroidManifest.xml                  # Cấu hình Permissions, Receivers, Services, FileProvider
├── java/com/example/dailyscheduleapp/
│   ├── data/
│   │   ├── database/                    # AppDatabase (Room, TypeConverters, Migration)
│   │   ├── dao/                         # TaskDao, CategoryDao, DayConfigDao
│   │   ├── entity/                      # Task, Category, DayConfig
│   │   └── repository/                  # TaskRepository, DayConfigRepository
│   ├── receiver/
│   │   ├── AlarmReceiver.java           # BroadcastReceiver kích hoạt báo thức và xử lý nút bấm
│   │   └── BootReceiver.java            # Tự động lập lịch lại sau reboot hoặc cấp quyền Alarm
│   ├── service/
│   │   └── PomodoroService.java         # Foreground Service đếm ngược Pomodoro chạy ngầm
│   ├── ui/
│   │   ├── adapter/                     # TaskAdapter (DiffUtil, ItemTouchHelper swipe-to-delete)
│   │   ├── view/
│   │   │   ├── MainActivity.java        # Màn hình chính (Lịch trình, Daily Balance, Cây năng lượng)
│   │   │   ├── AddEditTaskActivity.java # Thêm/sửa công việc, kiểm tra quyền Báo thức & Thông báo
│   │   │   ├── PomodoroActivity.java    # Phòng tập trung Pomodoro gắn nhiệm vụ
│   │   │   ├── StatisticsActivity.java  # Thống kê chi tiết & mở Thẻ Ngày
│   │   │   ├── AlarmAlertActivity.java  # Màn hình báo thức toàn màn hình trên Lockscreen
│   │   │   ├── NotificationSettingsActivity.java # Cài đặt kênh chuông và độ rung
│   │   │   └── DailyWrapDialog.java     # Thẻ tổng kết ngày, render bitmap, lưu & chia sẻ Story
│   │   └── viewmodel/
│   │       └── TaskViewModel.java       # ViewModel quản lý dữ liệu và logic nghiệp vụ
│   └── utils/
│       ├── AlarmScheduler.java          # Lập lịch báo thức chuẩn AOSP setAlarmClock
│       ├── EnergyPlantHelper.java       # Tính toán trạng thái sinh trưởng Cây Năng Lượng & Thẻ Ngày
│       ├── NotificationSettings.java    # Quản lý cấu hình Notification Channels (Low/Medium/High)
│       ├── Converters.java              # TypeConverter cho Date/Time trong Room
│       └── Constants.java               # Hằng số định danh toàn ứng dụng
└── res/
    ├── drawable/                        # Vector Drawables (Icon thông báo, Background thẻ, v.v.)
    ├── layout/                          # Layout XML cho Activity, Dialog, Item
    ├── values/                          # Strings (EN), Colors, Themes (Light)
    ├── values-vi/                       # Strings (VI - Tiếng Việt)
    ├── values-night/                    # Colors, Themes (Dark Mode)
    └── xml/
        └── file_paths.xml               # Cấu hình thư mục chia sẻ an toàn cho FileProvider
```

---

## 🛠️ Công nghệ & Thư viện sử dụng (Tech Stack)

| Thành phần | Công nghệ / Thư viện | Phiên bản |
| :--- | :--- | :--- |
| **Hệ điều hành** | Android (Min SDK 26, Target SDK 34+) | API 26 - 35 |
| **Ngôn ngữ** | Java | JDK 11 |
| **Giao diện** | Material Components Android (M3) | 1.12.0 |
| **Cơ sở dữ liệu** | AndroidX Room Database | 2.6.1 |
| **Kiến trúc** | AndroidX Lifecycle (ViewModel & LiveData) | 2.8.4 |
| **Lập lịch ngầm** | Android `AlarmManager` + `BroadcastReceiver` | Native AOSP |
| **Dịch vụ nền** | Android `ForegroundService` (Special Use) | Native AOSP |
| **Chia sẻ tệp** | AndroidX `FileProvider` | 1.7.0 |
| **Kiểm thử** | JUnit 4, AndroidX Test Runner, Espresso | 4.13.2 |

---

## 🧪 Kiểm thử đơn vị (Unit Testing)

Dự án có đầy đủ bộ kiểm thử tự động (Unit Test) bao phủ các logic tính toán quan trọng:
- [`EnergyPlantHelperTest.java`](app/src/test/java/com/example/dailyscheduleapp/EnergyPlantHelperTest.java): Kiểm thử 6 trạng thái tiến hóa của Cây Năng Lượng và thuật toán tính điểm cân bằng.
- [`DayBalanceCalculationTest.java`](app/src/test/java/com/example/dailyscheduleapp/DayBalanceCalculationTest.java): Kiểm thử thuật toán tính toán tổng thời gian Focus, Health, Leisure theo Category và Task.
- [`TaskLogicTest.java`](app/src/test/java/com/example/dailyscheduleapp/TaskLogicTest.java): Kiểm thử các ràng buộc nghiệp vụ (không tạo việc trong quá khứ, validate dữ liệu đầu vào).
- [`ConvertersTest.java`](app/src/test/java/com/example/dailyscheduleapp/ConvertersTest.java): Kiểm thử chuyển đổi Date/Time cho Room Database.

Chạy toàn bộ unit test qua dòng lệnh:
```bash
./gradlew test
```

---

## 🚀 Hướng dẫn cài đặt & Chạy ứng dụng (Getting Started)

### Yêu cầu môi trường:
- **Android Studio:** Phiên bản Iguana / Jellyfish / Koala (hoặc mới hơn).
- **JDK:** Java Development Kit 11 hoặc 17.
- **Android SDK:** Hỗ trợ API Level 26 đến 34+.

### Các bước cài đặt:
1. **Clone mã nguồn về máy tính:**
   ```bash
   git clone https://github.com/<your-username>/DailyScheduleApp.git
   cd DailyScheduleApp
   ```
2. **Mở dự án trong Android Studio:**
   - Mở Android Studio $\rightarrow$ Chọn **File** > **Open** $\rightarrow$ Trỏ tới thư mục dự án vừa clone.
   - Chờ Gradle đồng bộ các thư viện (`Sync Project with Gradle Files`).
3. **Chạy kiểm thử mã nguồn:**
   ```bash
   ./gradlew test
   ./gradlew lintDebug
   ```
4. **Đóng gói file cài đặt (APK):**
   ```bash
   ./gradlew assembleDebug
   ```
   *File APK sau khi build sẽ nằm tại:* `app/build/outputs/apk/debug/app-debug.apk`
5. **Cài đặt và trải nghiệm:**
   - Cắm điện thoại Android thật hoặc bật máy ảo Android Emulator.
   - Nhấn nút **Run** (biểu tượng ▶️ hoặc `Shift + F10`) trên thanh công cụ của Android Studio.

---

## 📄 Bản quyền & Tác giả (License & Credits)

- Dự án được phát triển nhằm mục đích học tập, nghiên cứu và quản lý năng suất cá nhân.
- Giấy phép phân phối: [MIT License](LICENSE).
