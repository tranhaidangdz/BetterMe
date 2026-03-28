package com.example.betterme.data.local.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.betterme.data.local.room.dao.AIChatDao
import com.example.betterme.data.local.room.dao.AchievementDao
import com.example.betterme.data.local.room.dao.CategoryDao
import com.example.betterme.data.local.room.dao.ChallengeDao
import com.example.betterme.data.local.room.dao.HabitDao
import com.example.betterme.data.local.room.dao.HabitLogDao
import com.example.betterme.data.local.room.dao.ReminderDao
import com.example.betterme.data.local.room.dao.UserAchievementDao
import com.example.betterme.data.local.room.dao.UserChallengeDao
import com.example.betterme.data.local.room.entities.AIChatEntity
import com.example.betterme.data.local.room.entities.AchievementEntity
import com.example.betterme.data.local.room.entities.CategoryEntity
import com.example.betterme.data.local.room.entities.ChallengeEntity
import com.example.betterme.data.local.room.entities.HabitEntity
import com.example.betterme.data.local.room.entities.HabitLogEntity
import com.example.betterme.data.local.room.entities.ReminderEntity
import com.example.betterme.data.local.room.entities.UserAchievementEntity
import com.example.betterme.data.local.room.entities.UserChallengeEntity
import com.example.betterme.data.local.room.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        ReminderEntity::class,
        ChallengeEntity::class,
        UserChallengeEntity::class,
        AchievementEntity::class,
        UserAchievementEntity::class,
        AIChatEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BetterMeDatabase : RoomDatabase() {

    // ===== DAO =====
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun userChallengeDao(): UserChallengeDao
    abstract fun achievementDao(): AchievementDao
    abstract fun userAchievementDao(): UserAchievementDao
    abstract fun aiChatDao(): AIChatDao

    companion object {

        @Volatile
        private var INSTANCE: BetterMeDatabase? = null

        fun getDatabase(context: Context): BetterMeDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BetterMeDatabase::class.java,
                    "better_me_db"
                )
                    .fallbackToDestructiveMigration() // dev phase
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}

/*
🧠 1. NGUYÊN TẮC VÀNG
👉 Nhớ 1 câu thôi:
Domain KHÔNG biết Data
Data PHỤ THUỘC Domain

CẤU TRÚC THƯ MỤC CHUẨN
👉 Với project của bạn, nên tổ chức như này:
com.example.betterme
│
├── data/                       ← TẦNG DATA
│   ├── local/
│   │   └── room/
│   │       ├── dao/
│   │       ├── entities/
│   │       └── database/
│   │
│   └── repository/             ← IMPLEMENTATION
│       ├── impl/
│       │     HabitRepositoryImpl.kt
│       │     HabitLogRepositoryImpl.kt
│       │     ...
│
├── domain/                     ← TẦNG DOMAIN
│   ├── repository/
│   │     HabitRepository.kt
│   │     HabitLogRepository.kt
│   │     ...
│   │
│   └── usecase/                ← (sau này dùng)
│
├── presentation/               ← UI + ViewModel
│
├── di/                         ← Koin / Hilt
🧠 1. SỰ THẬT CHUẨN (quan trọng nhất)

👉 Quy tắc đúng là:
Dependency chỉ đi từ ngoài → vào trong

Trong đó:
Presentation → Domain ← Data

👉 Nghĩa là:
Data PHỤ THUỘC Domain ✅
Domain KHÔNG phụ thuộc Data ✅
❗ 2. Hiểu sai bạn đang gặp

Bạn nói:
"tầng domain có repo truy cập data"

👉 ❌ Sai ở chỗ này:
Domain KHÔNG truy cập data trực tiếp
Domain chỉ định nghĩa interface

🔥 3. Vai trò thật của Repository
👉 Trong Domain:
interface HabitRepository {
    fun getHabits(userId: Int): Flow<List<HabitEntity>>
}

👉 Domain chỉ nói:
“Tôi cần dữ liệu kiểu này”

👉 Trong Data:
class HabitRepositoryImpl(
    private val dao: HabitDao
) : HabitRepository {
    ...
}

👉 Data nói:
“Tôi sẽ implement cách lấy dữ liệu (Room, API...)”

🧩 4. Luồng thật sự
ViewModel (presentation)
        ↓
Repository (interface - domain)
        ↓
RepositoryImpl (data)
        ↓
DAO (data)
        ↓
Room DB
🎯 5. Câu bạn nói → sửa lại cho đúng
❌ Bạn hiểu:
Domain → gọi Data
✅ Đúng phải là:
Domain ← Data implement

💡 6. Ví dụ dễ hiểu
🎬 Domain giống như:
"Tôi cần 1 dịch vụ lấy danh sách habit"
🎬 Data giống như:
"Ok, tôi dùng Room để lấy cho bạn"

🔥 7. Tại sao làm vậy?
👉 Để bạn có thể đổi data source:
Room → Firebase → API → Cache

MÀ KHÔNG sửa Domain & ViewModel

🚀 8. Sơ đồ dễ nhớ
        (interface)
Domain <------------- Data (implements)

        ↑
Presentation gọi

🧠 9. Kết luận (chốt hạ)
👉 Câu đúng phải là:
Domain định nghĩa Repository
Data implement Repository

👉 Không phải:
Domain truy cập Data ❌
*/