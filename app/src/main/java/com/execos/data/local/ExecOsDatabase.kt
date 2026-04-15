package com.execos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.execos.data.local.entity.DecisionEntity
import com.execos.data.local.entity.EnergyEntity
import com.execos.data.local.entity.ReflectionEntity
import com.execos.data.local.entity.StravaActivityEntity
import com.execos.data.local.entity.TaskEntity
import com.execos.data.local.entity.UserAccountEntity
import com.execos.data.local.entity.WeeklyReviewEntity

@Database(
    entities = [
        UserAccountEntity::class,
        TaskEntity::class,
        DecisionEntity::class,
        ReflectionEntity::class,
        EnergyEntity::class,
        WeeklyReviewEntity::class,
        StravaActivityEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class ExecOsDatabase : RoomDatabase() {
    abstract fun userAccountDao(): UserAccountDao
    abstract fun taskDao(): TaskDao
    abstract fun decisionDao(): DecisionDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun energyDao(): EnergyDao
    abstract fun weeklyReviewDao(): WeeklyReviewDao
    abstract fun stravaActivityDao(): StravaActivityDao
}
