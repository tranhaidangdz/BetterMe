package com.example.betterme.data.local.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migrations for BetterMeDatabase.
 *
 * Each migration is additive — we never drop user data. Pre-v7 schemas predate this catalog,
 * so they still hit `fallbackToDestructiveMigration()` (intentionally left enabled for very
 * old installs). From v7 onward every bump must ship its own explicit migration here.
 *
 * Versioning notes:
 * - v7 → v8 (challenge curated copy expansion):
 *   added two non-null TEXT columns to `challenges` (`motivational_quote` and
 *   `completion_message`), both default to empty string so existing rows backfill cleanly.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Both columns ship with default '' on the entity, so we mirror that here. NOT NULL
        // is enforced on the entity side — using DEFAULT '' lets the SQLite engine backfill
        // existing rows without needing a manual UPDATE.
        db.execSQL(
            "ALTER TABLE challenges ADD COLUMN motivational_quote TEXT NOT NULL DEFAULT ''"
        )
        db.execSQL(
            "ALTER TABLE challenges ADD COLUMN completion_message TEXT NOT NULL DEFAULT ''"
        )
    }
}

/**
 * v8 → v9 (Cloudinary-backed catalog artwork):
 * adds nullable `image_url` columns to `achievements` and `categories`. Both default to
 * NULL so existing rows backfill cleanly and UI fallbacks (emoji / drawable resource)
 * keep working until URLs are populated.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE achievements ADD COLUMN image_url TEXT")
        db.execSQL("ALTER TABLE categories ADD COLUMN image_url TEXT")
    }
}

/**
 * v9 → v10 (AI response cache):
 * adds `ai_cache` table backing the 12h cache for Group Review + Suggestions.
 * Unique index on `(categoryId, type)` is created here so the DAO's REPLACE
 * insert resolves to the same row.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ai_cache (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                categoryId INTEGER NOT NULL,
                type TEXT NOT NULL,
                content TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_ai_cache_categoryId_type ON ai_cache(categoryId, type)"
        )
    }
}

/**
 * v10 → v11 (Habit reminder deep-linking):
 * adds nullable `habit_id` column to `notifications` so HABIT_REMINDER rows can
 * deep-link to the habit detail screen on tap. Older rows backfill to NULL — the
 * existing challenge-routing path is untouched.
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notifications ADD COLUMN habit_id INTEGER")
    }
}

/**
 * v11 → v12 (strict-daily challenge validation):
 * adds nullable `target_end_date` column to `user_challenges` and backfills it for every
 * existing row from `start_date + (duration_days - 1) * 86_400_000`. Legacy rows that
 * don't have a matching challenge row (FK should prevent this but we still null-guard) are
 * left as NULL — the runtime evaluator treats NULL `target_end_date` as "not strict" and
 * falls back to the legacy streak rule, so old data can never crash the evaluator.
 *
 * No status values are mutated by this migration; the startup backfill pass
 * (BetterMeApplication) runs the EvaluateChallengeStatusUseCase across all ACTIVE rows
 * exactly once per launch and writes COMPLETED / FAILED as appropriate.
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_challenges ADD COLUMN target_end_date INTEGER")
        // Backfill: target_end_date = start_date + (duration_days - 1) * 86_400_000
        // Day length in ms is 86_400_000. We use a sub-select against challenges so each
        // row gets its own duration.
        db.execSQL(
            """
            UPDATE user_challenges
            SET target_end_date = start_date + (
                (SELECT duration_days FROM challenges WHERE challenges.id = user_challenges.challenge_id) - 1
            ) * 86400000
            WHERE EXISTS (
                SELECT 1 FROM challenges WHERE challenges.id = user_challenges.challenge_id
            )
            """.trimIndent()
        )
    }
}

/**
 * v12 → v13 (offline-first sync columns):
 * adds `updated_at`, `synced_at`, and (where applicable) `is_deleted` to the three
 * priority synced entities — `users`, `user_challenges`, `challenge_logs`.
 *
 * `updated_at` defaults to the row's existing creation timestamp (`created_at` /
 * `joined_at`) so every legacy row enters the sync layer at a consistent epoch and
 * gets queued for an initial upload on first launch under v13. `synced_at` stays
 * NULL until the first successful push, marking every row dirty until reconciled.
 *
 * `is_deleted` is INTEGER NOT NULL DEFAULT 0 (Room maps Boolean → INTEGER 0/1).
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE users ADD COLUMN synced_at INTEGER")
        db.execSQL("UPDATE users SET updated_at = created_at WHERE updated_at = 0")

        db.execSQL("ALTER TABLE user_challenges ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_challenges ADD COLUMN synced_at INTEGER")
        db.execSQL("ALTER TABLE user_challenges ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE user_challenges SET updated_at = joined_at WHERE updated_at = 0")

        db.execSQL("ALTER TABLE challenge_logs ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE challenge_logs ADD COLUMN synced_at INTEGER")
        db.execSQL("ALTER TABLE challenge_logs ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE challenge_logs SET updated_at = created_at WHERE updated_at = 0")
    }
}

/**
 * v13 → v14 (complete sync coverage):
 *  - Adds `updated_at`, `synced_at`, `is_deleted` to `ai_chat`, `habits`, `habit_logs`
 *    (the remaining entities not covered in the v12→v13 first pass).
 *  - Creates `user_settings` table for cross-device-syncable lifestyle profile +
 *    onboarding flags. The previous in-memory `UserLifestyleProfile.Default` + the
 *    device-local DataStore flags (`HAS_SELECTED_HABITS`, `IS_FIRST_TIME`) now
 *    persist here so they survive reinstall and replicate across devices.
 *
 * Backfill of `updated_at` from `created_at` mirrors the v12→v13 approach so legacy
 * rows enter the sync layer at a known epoch.
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ai_chat
        db.execSQL("ALTER TABLE ai_chat ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE ai_chat ADD COLUMN synced_at INTEGER")
        db.execSQL("ALTER TABLE ai_chat ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE ai_chat SET updated_at = created_at WHERE updated_at = 0")

        // habits
        db.execSQL("ALTER TABLE habits ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE habits ADD COLUMN synced_at INTEGER")
        db.execSQL("ALTER TABLE habits ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE habits SET updated_at = created_at WHERE updated_at = 0")

        // habit_logs
        db.execSQL("ALTER TABLE habit_logs ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE habit_logs ADD COLUMN synced_at INTEGER")
        db.execSQL("ALTER TABLE habit_logs ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE habit_logs SET updated_at = created_at WHERE updated_at = 0")

        // user_settings (new table)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_settings (
                user_id TEXT NOT NULL PRIMARY KEY,
                sleep_start TEXT NOT NULL DEFAULT '23:00',
                sleep_end TEXT NOT NULL DEFAULT '07:00',
                sleep_duration_target_hours INTEGER NOT NULL DEFAULT 8,
                work_start TEXT NOT NULL DEFAULT '08:30',
                work_end TEXT NOT NULL DEFAULT '17:30',
                breakfast TEXT NOT NULL DEFAULT '07:30',
                lunch TEXT NOT NULL DEFAULT '12:00',
                dinner TEXT NOT NULL DEFAULT '18:30',
                activity_level TEXT NOT NULL DEFAULT 'MODERATE',
                has_selected_habits INTEGER NOT NULL DEFAULT 0,
                is_first_time INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                synced_at INTEGER
            )
            """.trimIndent()
        )
    }
}

/**
 * v14 → v15 (badges + interest categories enter the sync layer):
 *  - Adds `updated_at`, `synced_at`, `is_deleted` to `user_achievements` (badges
 *    earned by the user). Legacy rows backfill `updated_at = achieved_at` so they
 *    enter the sync layer at a known epoch; `synced_at` stays NULL so the first
 *    pass after upgrade uploads every existing badge.
 *  - Adds `updated_at`, `synced_at`, `is_deleted` to `user_categories` (onboarding
 *    interest picks). Legacy rows backfill `updated_at = created_at`.
 *
 * Both tables already had `UNIQUE(user_id, achievement_id)` / `UNIQUE(user_id, category_id)`
 * indices, which the synchronizers depend on for cross-device dedupe (Firestore doc id
 * is the stable global id, not the local autoincrement).
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // user_achievements — badges.
        db.execSQL("ALTER TABLE user_achievements ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_achievements ADD COLUMN synced_at INTEGER")
        db.execSQL("ALTER TABLE user_achievements ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE user_achievements SET updated_at = achieved_at WHERE updated_at = 0")

        // user_categories — onboarding interest selections.
        db.execSQL("ALTER TABLE user_categories ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_categories ADD COLUMN synced_at INTEGER")
        db.execSQL("ALTER TABLE user_categories ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE user_categories SET updated_at = created_at WHERE updated_at = 0")
    }
}

/** Aggregated list passed to the Room builder. Add new migrations to this list as the
 *  schema evolves. */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_7_8,
    MIGRATION_8_9,
    MIGRATION_9_10,
    MIGRATION_10_11,
    MIGRATION_11_12,
    MIGRATION_12_13,
    MIGRATION_13_14,
    MIGRATION_14_15
)
