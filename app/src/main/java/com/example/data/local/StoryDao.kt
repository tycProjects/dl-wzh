package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CategoryEntity
import com.example.data.model.ChapterEntity
import com.example.data.model.StoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories WHERE isPublished = 1 ORDER BY isPinned DESC, readCount DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE isPublished = 1 AND isFeatured = 1")
    fun getFeaturedStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE isPublished = 1 ORDER BY readCount DESC LIMIT 10")
    fun getPopularStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE isPublished = 1 ORDER BY id DESC LIMIT 10")
    fun getNewStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE isPublished = 1 ORDER BY rating DESC, ratingCount DESC LIMIT 10")
    fun getTopRatedStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE isPublished = 1 AND categoryId = :categoryId")
    fun getStoriesByCategory(categoryId: String): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :id LIMIT 1")
    suspend fun getStoryById(id: String): StoryEntity?

    @Query("SELECT * FROM stories WHERE id = :id LIMIT 1")
    fun observeStoryById(id: String): Flow<StoryEntity?>

    @Query("""
        SELECT * FROM stories 
        WHERE isPublished = 1 AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR authorName LIKE '%' || :query || '%' OR categoryName LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
    """)
    fun searchStories(query: String): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories ORDER BY id DESC")
    fun getAllStoriesAdmin(): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<StoryEntity>)

    @Update
    suspend fun updateStory(story: StoryEntity)

    @Delete
    suspend fun deleteStory(story: StoryEntity)

    @Query("UPDATE stories SET readCount = readCount + 1 WHERE id = :storyId")
    suspend fun incrementReadCount(storyId: String)

    @Query("UPDATE stories SET likesCount = likesCount + 1 WHERE id = :storyId")
    suspend fun incrementLikes(storyId: String)

    @Query("UPDATE stories SET likesCount = CASE WHEN likesCount > 0 THEN likesCount - 1 ELSE 0 END WHERE id = :storyId")
    suspend fun decrementLikes(storyId: String)

    // Chapters
    @Query("SELECT * FROM chapters WHERE storyId = :storyId ORDER BY chapterNumber ASC")
    fun getChaptersForStory(storyId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE storyId = :storyId AND chapterNumber = :chapterNumber LIMIT 1")
    suspend fun getChapter(storyId: String, chapterNumber: Int): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)

    @Query("DELETE FROM chapters WHERE storyId = :storyId")
    suspend fun deleteChaptersForStory(storyId: String)

    // Categories
    @Query("SELECT * FROM categories ORDER BY orderIndex ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM stories")
    suspend fun getTotalStoryCount(): Int

    @Query("SELECT COUNT(*) FROM chapters")
    suspend fun getTotalChapterCount(): Int

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getTotalCategoryCount(): Int
}
