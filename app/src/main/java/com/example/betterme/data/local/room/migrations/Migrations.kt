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

/** Aggregated list passed to the Room builder. Add new migrations to this list as the
 *  schema evolves. */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_7_8,
    MIGRATION_8_9,
    MIGRATION_9_10,
    MIGRATION_10_11,
    MIGRATION_11_12,
    MIGRATION_12_13
)
