package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.HabitDao
import com.example.betterme.data.local.room.entities.HabitEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Filter contract for [HabitRepositoryImpl.getActiveHabits]:
 *
 *  - excludes soft-deleted habits (is_deleted = true) → user abandoned, or a
 *    delete that arrived from Firestore sync
 *  - excludes habits whose end_date is in the past (expired / failed by time)
 *  - keeps habits with no end_date (open-ended, never expires by clock)
 *  - keeps habits with end_date == now and end_date in the future
 *  - emits reactively when the underlying DAO Flow emits (sync, local mutation)
 */
class HabitRepositoryImplActiveFilterTest {

    private val NOW = TimeUnit.DAYS.toMillis(20_000) // arbitrary fixed epoch
    private val ONE_DAY = TimeUnit.DAYS.toMillis(1)

    private fun habit(
        id: Int,
        endDate: Long? = null,
        isDeleted: Boolean = false
    ) = HabitEntity(
        id = id,
        user_id = "u1",
        category_id = 1,
        title = "habit-$id",
        description = null,
        start_date = NOW - 30 * ONE_DAY,
        end_date = endDate,
        reminder_time = null,
        created_at = NOW - 30 * ONE_DAY,
        updated_at = NOW,
        synced_at = NOW,
        is_deleted = isDeleted
    )

    private fun repo(seed: List<HabitEntity>): Pair<HabitRepositoryImpl, MutableStateFlow<List<HabitEntity>>> {
        val src = MutableStateFlow(seed)
        val dao = FakeHabitDao(src)
        val r = HabitRepositoryImpl(dao, nowProvider = { NOW })
        return r to src
    }

    @Test
    fun `excludes soft-deleted habits`() = runBlocking {
        val (r, _) = repo(listOf(
            habit(1, isDeleted = false),
            habit(2, isDeleted = true)
        ))
        val ids = r.getActiveHabits("u1").first().map { it.id }
        assertEquals(listOf(1), ids)
    }

    @Test
    fun `excludes habits whose end_date is in the past`() = runBlocking {
        val (r, _) = repo(listOf(
            habit(1, endDate = NOW - ONE_DAY),  // expired yesterday
            habit(2, endDate = NOW + ONE_DAY)   // expires tomorrow
        ))
        val ids = r.getActiveHabits("u1").first().map { it.id }
        assertEquals(listOf(2), ids)
    }

    @Test
    fun `keeps open-ended habits with null end_date`() = runBlocking {
        val (r, _) = repo(listOf(habit(1, endDate = null)))
        val ids = r.getActiveHabits("u1").first().map { it.id }
        assertEquals(listOf(1), ids)
    }

    @Test
    fun `keeps habits whose end_date equals now (boundary)`() = runBlocking {
        val (r, _) = repo(listOf(habit(1, endDate = NOW)))
        val ids = r.getActiveHabits("u1").first().map { it.id }
        assertEquals(listOf(1), ids)
    }

    @Test
    fun `mixed states produces only active subset`() = runBlocking {
        val (r, _) = repo(listOf(
            habit(1, endDate = null, isDeleted = false),         // active, open-ended
            habit(2, endDate = NOW + ONE_DAY, isDeleted = false),// active, future end
            habit(3, endDate = NOW - ONE_DAY, isDeleted = false),// expired
            habit(4, endDate = NOW + ONE_DAY, isDeleted = true), // user abandoned
            habit(5, endDate = null, isDeleted = true)           // open-ended but deleted (sync from Firebase)
        ))
        val ids = r.getActiveHabits("u1").first().map { it.id }.sorted()
        assertEquals(listOf(1, 2), ids)
    }

    @Test
    fun `re-emits when DAO source emits — reactive on sync update`() = runBlocking {
        val (r, src) = repo(listOf(habit(1, isDeleted = false)))
        val flow = r.getActiveHabits("u1")
        assertEquals(listOf(1), flow.first().map { it.id })

        // Simulate Firebase sync writing is_deleted=true into Room.
        src.value = listOf(habit(1, isDeleted = true))
        assertTrue("expired habit must drop out after DAO re-emits",
            flow.first().isEmpty())
    }

    @Test
    fun `empty source emits empty list`() = runBlocking {
        val (r, _) = repo(emptyList())
        assertTrue(r.getActiveHabits("u1").first().isEmpty())
    }

    /**
     * Minimal HabitDao stand-in: only [getHabitsByUser] is wired (the only method the
     * filter pipeline touches). Everything else throws — keeps the test honest about
     * what the new method depends on.
     */
    private class FakeHabitDao(
        private val source: MutableStateFlow<List<HabitEntity>>
    ) : HabitDao {
        override fun getHabitsByUser(userId: String): Flow<List<HabitEntity>> = source
        override suspend fun insertHabit(habit: HabitEntity): Long = nope()
        override suspend fun updateHabit(habit: HabitEntity): Unit = nope()
        override suspend fun deleteHabit(habit: HabitEntity): Unit = nope()
        override suspend fun deleteHabitById(habitId: Int): Unit = nope()
        override suspend fun softDeleteHabit(habitId: Int, updatedAt: Long): Unit = nope()
        override suspend fun getHabitById(habitId: Int): HabitEntity? = nope()
        override fun getHabitsByCategoryForUser(categoryId: Int, userId: String): Flow<List<HabitEntity>> = nope()
        override suspend fun getHabitCountByCategoryForUser(categoryId: Int, userId: String): Int = nope()
        override suspend fun deleteAllByUserId(userId: String): Unit = nope()
        override suspend fun getDirtyHabits(userId: String): List<HabitEntity> = nope()
        override suspend fun countDirtyHabits(userId: String): Int = nope()
        override suspend fun markHabitSynced(id: Int, pushedUpdatedAt: Long, syncedAt: Long): Unit = nope()

        private fun <T> nope(): T = throw UnsupportedOperationException("not used by this test")
    }
}
