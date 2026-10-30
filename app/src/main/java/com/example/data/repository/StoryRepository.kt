package com.example.data.repository

import com.example.data.local.InteractionDao
import com.example.data.local.StoryDao
import com.example.data.model.BookmarkEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.ChapterEntity
import com.example.data.model.CommentEntity
import com.example.data.model.FavoriteStoryEntity
import com.example.data.model.FollowAuthorEntity
import com.example.data.model.HighlightEntity
import com.example.data.model.ReadingProgressEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.StoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class StoryRepository(
    private val storyDao: StoryDao,
    private val interactionDao: InteractionDao
) {
    val allStories: Flow<List<StoryEntity>> = storyDao.getAllStories()
    val featuredStories: Flow<List<StoryEntity>> = storyDao.getFeaturedStories()
    val popularStories: Flow<List<StoryEntity>> = storyDao.getPopularStories()
    val newStories: Flow<List<StoryEntity>> = storyDao.getNewStories()
    val topRatedStories: Flow<List<StoryEntity>> = storyDao.getTopRatedStories()
    val categories: Flow<List<CategoryEntity>> = storyDao.getAllCategories()
    val allStoriesAdmin: Flow<List<StoryEntity>> = storyDao.getAllStoriesAdmin()

    fun getStoriesByCategory(categoryId: String): Flow<List<StoryEntity>> =
        storyDao.getStoriesByCategory(categoryId)

    fun searchStories(query: String): Flow<List<StoryEntity>> =
        storyDao.searchStories(query)

    suspend fun getStoryById(id: String): StoryEntity? =
        storyDao.getStoryById(id)

    fun observeStoryById(id: String): Flow<StoryEntity?> =
        storyDao.observeStoryById(id)

    suspend fun incrementReadCount(storyId: String) =
        storyDao.incrementReadCount(storyId)

    // Chapters
    fun getChapters(storyId: String): Flow<List<ChapterEntity>> =
        storyDao.getChaptersForStory(storyId)

    suspend fun getChapter(storyId: String, chapterNumber: Int): ChapterEntity? =
        storyDao.getChapter(storyId, chapterNumber)

    // Bookmarks
    fun getBookmarks(userId: String): Flow<List<BookmarkEntity>> =
        interactionDao.getBookmarks(userId)

    suspend fun addBookmark(userId: String, storyId: String, chapterNumber: Int, paragraphIndex: Int, snippet: String) {
        val bookmark = BookmarkEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            storyId = storyId,
            chapterNumber = chapterNumber,
            paragraphIndex = paragraphIndex,
            paragraphSnippet = snippet
        )
        interactionDao.insertBookmark(bookmark)
    }

    suspend fun removeBookmarkAt(userId: String, storyId: String, chapterNumber: Int, paragraphIndex: Int) {
        interactionDao.removeBookmarkAt(userId, storyId, chapterNumber, paragraphIndex)
    }

    // Reading Progress
    fun getReadingProgressList(userId: String): Flow<List<ReadingProgressEntity>> =
        interactionDao.getReadingProgressList(userId)

    fun observeProgressForStory(userId: String, storyId: String): Flow<ReadingProgressEntity?> =
        interactionDao.observeProgressForStory(userId, storyId)

    suspend fun saveReadingProgress(userId: String, storyId: String, chapterNumber: Int, percent: Float) {
        val progress = ReadingProgressEntity(
            key = "${userId}_${storyId}",
            userId = userId,
            storyId = storyId,
            lastChapterNumber = chapterNumber,
            progressPercent = percent,
            lastReadTimestamp = System.currentTimeMillis()
        )
        interactionDao.saveReadingProgress(progress)
    }

    // Highlights
    fun getHighlights(userId: String, storyId: String, chapterNumber: Int): Flow<List<HighlightEntity>> =
        interactionDao.getHighlights(userId, storyId, chapterNumber)

    suspend fun addHighlight(userId: String, storyId: String, chapterNumber: Int, paragraphIndex: Int, text: String, colorHex: String) {
        val highlight = HighlightEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            storyId = storyId,
            chapterNumber = chapterNumber,
            paragraphIndex = paragraphIndex,
            highlightText = text,
            colorHex = colorHex
        )
        interactionDao.insertHighlight(highlight)
    }

    suspend fun removeHighlight(highlight: HighlightEntity) {
        interactionDao.deleteHighlight(highlight)
    }

    // Comments
    fun getCommentsForStory(storyId: String): Flow<List<CommentEntity>> =
        interactionDao.getCommentsForStory(storyId)

    suspend fun addComment(storyId: String, chapterNumber: Int, userId: String, userName: String, userAvatar: String, userVerified: Boolean, text: String) {
        val comment = CommentEntity(
            id = UUID.randomUUID().toString(),
            storyId = storyId,
            chapterNumber = chapterNumber,
            userId = userId,
            userName = userName,
            userAvatar = userAvatar,
            userVerified = userVerified,
            commentText = text,
            likes = 0
        )
        interactionDao.insertComment(comment)
    }

    // Reviews
    fun getReviewsForStory(storyId: String): Flow<List<ReviewEntity>> =
        interactionDao.getReviewsForStory(storyId)

    suspend fun addReview(storyId: String, userId: String, userName: String, rating: Int, text: String) {
        val review = ReviewEntity(
            id = UUID.randomUUID().toString(),
            storyId = storyId,
            userId = userId,
            userName = userName,
            rating = rating,
            reviewText = text
        )
        interactionDao.insertReview(review)
    }

    // Favorites
    fun isFavorite(userId: String, storyId: String): Flow<Boolean> =
        interactionDao.isFavorite(userId, storyId)

    fun getFavorites(userId: String): Flow<List<FavoriteStoryEntity>> =
        interactionDao.getFavorites(userId)

    suspend fun toggleFavorite(userId: String, storyId: String, currentStatus: Boolean) {
        if (currentStatus) {
            interactionDao.removeFavoriteById(userId, storyId)
            storyDao.decrementLikes(storyId)
        } else {
            interactionDao.addFavorite(FavoriteStoryEntity(id = "${userId}_${storyId}", userId = userId, storyId = storyId))
            storyDao.incrementLikes(storyId)
        }
    }

    // Authors
    fun isFollowingAuthor(userId: String, authorName: String): Flow<Boolean> =
        interactionDao.isFollowingAuthor(userId, authorName)

    suspend fun toggleFollowAuthor(userId: String, authorName: String, currentStatus: Boolean) {
        if (currentStatus) {
            interactionDao.unfollowAuthorByName(userId, authorName)
        } else {
            interactionDao.followAuthor(FollowAuthorEntity(id = "${userId}_${authorName}", userId = userId, authorName = authorName))
        }
    }

    // Admin Operations
    suspend fun insertStory(story: StoryEntity) = storyDao.insertStory(story)
    suspend fun updateStory(story: StoryEntity) = storyDao.updateStory(story)
    suspend fun deleteStory(story: StoryEntity) {
        storyDao.deleteChaptersForStory(story.id)
        storyDao.deleteStory(story)
    }

    suspend fun insertChapter(chapter: ChapterEntity) = storyDao.insertChapter(chapter)
    suspend fun updateChapter(chapter: ChapterEntity) = storyDao.updateChapter(chapter)
    suspend fun deleteChapter(chapter: ChapterEntity) = storyDao.deleteChapter(chapter)

    suspend fun insertCategory(category: CategoryEntity) = storyDao.insertCategory(category)
    suspend fun updateCategory(category: CategoryEntity) = storyDao.updateCategory(category)
    suspend fun deleteCategory(category: CategoryEntity) = storyDao.deleteCategory(category)

    suspend fun getTotalStats(): Triple<Int, Int, Int> {
        return Triple(
            storyDao.getTotalStoryCount(),
            storyDao.getTotalChapterCount(),
            storyDao.getTotalCategoryCount()
        )
    }
}
