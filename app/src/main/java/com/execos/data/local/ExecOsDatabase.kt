package com.execos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.execos.data.local.entity.DecisionEntity
import com.execos.data.local.entity.EnergyEntity
import com.execos.data.local.entity.ReflectionEntity
import com.execos.data.local.entity.TaskEntity
import com.execos.data.local.entity.WeeklyReviewEntity

@Database(
    entities = [
        TaskEntity::class,
        DecisionEntity::class,
        ReflectionEntity::class,
        EnergyEntity::class,
        WeeklyReviewEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ExecOsDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun decisionDao(): DecisionDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun energyDao(): EnergyDao
    abstract fun weeklyReviewDao(): WeeklyReviewDao
}
