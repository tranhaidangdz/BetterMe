package com.example.betterme.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.betterme.data.local.room.entities.UserAchievementEntity
import com.example.betterme.data.local.room.relation.UserAchievementWithBadge
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAchievementDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(userAchievement: UserAchievementEntity): Long

    @Update
    suspend fun update(userAchievement: UserAchievementEntity)

    @Query("SELECT * FROM user_achievements WHERE user_id = :userId AND is_deleted = 0 ORDER BY achieved_at DESC")
    fun observeByUser(userId: String): Flow<List<UserAchievementEntity>>

    @Transaction
    @Query("SELECT * FROM user_achievements WHERE user_id = :userId AND is_deleted = 0 ORDER BY achieved_at DESC")
    fun observeByUserWithBadge(userId: String): Flow<List<UserAchievementWithBadge>>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM user_achievements
            WHERE user_id = :userId AND achievement_id = :achievementId AND is_deleted = 0
        )
    """)
    suspend fun hasEarned(userId: String, achievementId: Int): Boolean

    @Query("SELECT COUNT(*) FROM user_achievements WHERE user_id = :userId AND is_deleted = 0")
    suspend fun countByUser(userId: String): Int

    // ===== Sync support =====
    /**
     * Returns rows currently dirty (never synced, or local update newer than last push).
     * `is_deleted` rows are returned too so the synchronizer can propagate the removal.
     */
    @Query(
        """
        SELECT * FROM user_achievements
        WHERE user_id = :userId
          AND (synced_at IS NULL OR synced_at < updated_at)
        """
    )
    suspend fun getDirty(userId: String): List<UserAchievementEntity>

    @Query(
        """
        SELECT COUNT(*) FROM user_achievements
        WHERE user_id = :userId
          AND (synced_at IS NULL OR synced_at < updated_at)
        """
    )
    suspend fun countDirty(userId: String): Int

    @Query(
        """
        UPDATE user_achievements
        SET synced_at = :syncedAt
        WHERE id = :id AND updated_at <= :upToUpdatedAt
        """
    )
    suspend fun markSynced(id: Int, upToUpdatedAt: Long, syncedAt: Long)

    @Query(
        """
        SELECT * FROM user_achievements
        WHERE user_id = :userId AND achievement_id = :achievementId
        LIMIT 1
        """
    )
    suspend fun findByAchievement(userId: String, achievementId: Int): UserAchievementEntity?
}
