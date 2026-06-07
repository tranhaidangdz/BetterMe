package com.example.betterme.data.repository

import com.example.betterme.data.local.room.dao.UserCategoryDao
import com.example.betterme.domain.repository.UserCategoryRepository

class UserCategoryRepositoryImpl(
    private val dao: UserCategoryDao
) : UserCategoryRepository {

    override fun observeSelectedIds(userId: String) = dao.observeSelectedIds(userId)

    override suspend fun getSelectedIds(userId: String) = dao.getSelectedIds(userId)

    override fun observeSelectedCategories(userId: String) =
        dao.observeSelectedCategories(userId)

    override suspend fun getSelectedCategories(userId: String) =
        dao.getSelectedCategories(userId)

    override suspend fun saveSelections(userId: String, categoryIds: Collection<Int>) =
        dao.replaceSelections(userId, categoryIds)

    // Soft-delete every selection so the sync layer can propagate the removal
    // to Firestore. A hard DELETE would lose the rows before the next push pass
    // could surface them; the synchronizer is the one that finally erases them
    // remotely via `is_deleted=true`.
    override suspend fun clearForUser(userId: String) = dao.softDeleteAll(userId)
}
