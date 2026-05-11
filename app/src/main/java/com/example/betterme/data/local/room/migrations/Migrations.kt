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

/** Aggregated list passed to the Room builder. Add new migrations to this list as the
 *  schema evolves. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
