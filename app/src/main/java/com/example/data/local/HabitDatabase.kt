package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Database(
    entities = [HabitEntity::class, HabitCompletionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HabitDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: HabitDatabase? = null

        fun getDatabase(context: Context): HabitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    "habit_fire_database"
                )
                    .addCallback(HabitDatabaseCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class HabitDatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                populateInitialData(getDatabase(context).habitDao())
            }
        }

        private suspend fun populateInitialData(dao: HabitDao) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val calendar = Calendar.getInstance()

            val habit1Id = dao.insertHabit(
                HabitEntity(
                    title = "Пити 2 літри води",
                    description = "Підтримувати здоровий гідробаланс щодня",
                    category = "HEALTH",
                    iconName = "water",
                    colorHex = 0xFF00B0FF,
                    reminderEnabled = true,
                    reminderHour = 9,
                    reminderMinute = 0,
                    targetDaysPerWeek = 7
                )
            )

            val habit2Id = dao.insertHabit(
                HabitEntity(
                    title = "Ранкова руханка / спорт",
                    description = "15-20 хвилин активності для заряду бадьорості",
                    category = "SPORT",
                    iconName = "fitness",
                    colorHex = 0xFFFF6D00,
                    reminderEnabled = true,
                    reminderHour = 8,
                    reminderMinute = 0,
                    targetDaysPerWeek = 6
                )
            )

            val habit3Id = dao.insertHabit(
                HabitEntity(
                    title = "Читати корисну книгу",
                    description = "Мінімум 15-20 сторінок для саморозвитку",
                    category = "LEARNING",
                    iconName = "book",
                    colorHex = 0xFF00C853,
                    reminderEnabled = true,
                    reminderHour = 21,
                    reminderMinute = 30,
                    targetDaysPerWeek = 7
                )
            )

            val habit4Id = dao.insertHabit(
                HabitEntity(
                    title = "Вечірня медитація / спокій",
                    description = "10 хвилин усвідомленого дихання перед сном",
                    category = "MIND",
                    iconName = "meditate",
                    colorHex = 0xFFAB47BC,
                    reminderEnabled = false,
                    reminderHour = 22,
                    reminderMinute = 30,
                    targetDaysPerWeek = 5
                )
            )

            // Seed completions for the last 3 days to ignite an initial 3-day fire streak!
            for (daysAgo in 3 downTo 1) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
                val pastDate = dateFormat.format(calendar.time)

                dao.insertCompletion(
                    HabitCompletionEntity(
                        habitId = habit1Id,
                        date = pastDate,
                        completedAt = calendar.timeInMillis
                    )
                )
                dao.insertCompletion(
                    HabitCompletionEntity(
                        habitId = habit2Id,
                        date = pastDate,
                        completedAt = calendar.timeInMillis
                    )
                )
                if (daysAgo < 3) {
                    dao.insertCompletion(
                        HabitCompletionEntity(
                            habitId = habit3Id,
                            date = pastDate,
                            completedAt = calendar.timeInMillis
                        )
                    )
                }
            }
        }
    }
}
