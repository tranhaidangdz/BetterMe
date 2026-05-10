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

/** Aggregated list passed to the Room builder. Add new migrations to this list as the
 *  schema evolves. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_7_8)
