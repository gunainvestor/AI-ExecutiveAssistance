package com.execos.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2:
 * - Adds real local accounts table.
 * - Adds userId column to all journal tables to support multiple accounts.
 *
 * Existing rows are assigned to the legacy userId = "local".
 */
object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Accounts
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_accounts (
                    id TEXT NOT NULL PRIMARY KEY,
                    email TEXT NOT NULL,
                    passwordHash TEXT NOT NULL,
                    createdAt TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_accounts_email ON user_accounts(email)")

            // Tasks
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS tasks_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    userId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    impactScore INTEGER NOT NULL,
                    notes TEXT NOT NULL,
                    completed INTEGER NOT NULL,
                    date TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO tasks_new (id, userId, title, impactScore, notes, completed, date)
                SELECT id, 'local', title, impactScore, notes, completed, date FROM tasks
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE tasks")
            db.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_userId_date ON tasks(userId, date)")

            // Decisions
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS decisions_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    userId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    context TEXT NOT NULL,
                    options TEXT NOT NULL,
                    finalDecision TEXT NOT NULL,
                    confidence INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    aiAnalysis TEXT
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO decisions_new (id, userId, title, context, options, finalDecision, confidence, date, aiAnalysis)
                SELECT id, 'local', title, context, options, finalDecision, confidence, date, aiAnalysis FROM decisions
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE decisions")
            db.execSQL("ALTER TABLE decisions_new RENAME TO decisions")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_decisions_userId_date ON decisions(userId, date)")

            // Reflections
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS reflections_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    userId TEXT NOT NULL,
                    textInput TEXT NOT NULL,
                    aiOutput TEXT NOT NULL,
                    date TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO reflections_new (id, userId, textInput, aiOutput, date)
                SELECT id, 'local', textInput, aiOutput, date FROM reflections
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE reflections")
            db.execSQL("ALTER TABLE reflections_new RENAME TO reflections")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_reflections_userId_date ON reflections(userId, date)")

            // Energy
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS energy_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    userId TEXT NOT NULL,
                    morningScore INTEGER NOT NULL,
                    eveningScore INTEGER NOT NULL,
                    date TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO energy_new (id, userId, morningScore, eveningScore, date)
                SELECT id, 'local', morningScore, eveningScore, date FROM energy
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE energy")
            db.execSQL("ALTER TABLE energy_new RENAME TO energy")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_energy_userId_date ON energy(userId, date)")

            // Weekly reviews
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS weekly_reviews_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    userId TEXT NOT NULL,
                    weekStart TEXT NOT NULL,
                    wins TEXT NOT NULL,
                    mistakes TEXT NOT NULL,
                    learnings TEXT NOT NULL,
                    aiSummary TEXT
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO weekly_reviews_new (id, userId, weekStart, wins, mistakes, learnings, aiSummary)
                SELECT id, 'local', weekStart, wins, mistakes, learnings, aiSummary FROM weekly_reviews
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE weekly_reviews")
            db.execSQL("ALTER TABLE weekly_reviews_new RENAME TO weekly_reviews")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_weekly_reviews_userId_weekStart ON weekly_reviews(userId, weekStart)")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS strava_activities (
                    id INTEGER NOT NULL PRIMARY KEY,
                    userId TEXT NOT NULL,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    startDate TEXT NOT NULL,
                    elapsedTimeSec INTEGER NOT NULL,
                    movingTimeSec INTEGER NOT NULL,
                    distanceMeters REAL NOT NULL,
                    elevationGainMeters REAL NOT NULL,
                    kilojoules REAL,
                    calories REAL,
                    avgHeartRate REAL,
                    maxHeartRate REAL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_strava_activities_userId_startDate ON strava_activities(userId, startDate)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS goals (
                    id TEXT NOT NULL PRIMARY KEY,
                    userId TEXT NOT NULL,
                    periodType TEXT NOT NULL,
                    periodKey TEXT NOT NULL,
                    rank INTEGER NOT NULL,
                    title TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_goals_userId_periodType_periodKey_rank
                ON goals(userId, periodType, periodKey, rank)
                """.trimIndent(),
            )
        }
    }
}

