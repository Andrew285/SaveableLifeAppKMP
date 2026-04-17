package org.simpleapps.saveablekmp.domain.srs

import kotlinx.datetime.Clock

enum class ReviewDifficulty { HARD, NORMAL, EASY }

data class SrsResult(
    val nextReviewAt: Long,
    val easeFactor: Double,
    val interval: Int,
    val repetitions: Int,
    val nextReviewLabel: String, // "10 хв", "1 день", "5 днів"
)

object SpacedRepetition {

    fun calculate(
        difficulty: ReviewDifficulty,
        currentEaseFactor: Double,
        currentInterval: Int,
        currentRepetitions: Int,
    ): SrsResult {
        val now = Clock.System.now().toEpochMilliseconds()

        return when (difficulty) {
            ReviewDifficulty.HARD -> {
                // Складно — повторити скоро
                val newEase = maxOf(1.3, currentEaseFactor - 0.2)
                val newInterval = when {
                    currentRepetitions == 0 -> 1  // 1 хвилина
                    currentInterval <= 1 -> 1
                    else -> maxOf(1, (currentInterval * 0.5).toInt())
                }
                val nextReview = if (currentRepetitions == 0) {
                    now + 1 * 60 * 1000L // 1 хвилина
                } else {
                    now + newInterval * 24 * 60 * 60 * 1000L
                }
                SrsResult(
                    nextReviewAt = nextReview,
                    easeFactor = newEase,
                    interval = newInterval,
                    repetitions = maxOf(0, currentRepetitions - 1),
                    nextReviewLabel = if (currentRepetitions == 0) "1 хв" else formatDays(newInterval),
                )
            }

            ReviewDifficulty.NORMAL -> {
                val newEase = maxOf(1.3, currentEaseFactor)
                val newInterval = when (currentRepetitions) {
                    0 -> 0  // 10 хвилин
                    1 -> 1  // 1 день
                    else -> (currentInterval * newEase).toInt()
                }
                val nextReview = when (currentRepetitions) {
                    0 -> now + 10 * 60 * 1000L
                    1 -> now + 1 * 24 * 60 * 60 * 1000L
                    else -> now + newInterval * 24 * 60 * 60 * 1000L
                }
                SrsResult(
                    nextReviewAt = nextReview,
                    easeFactor = newEase,
                    interval = newInterval,
                    repetitions = currentRepetitions + 1,
                    nextReviewLabel = when (currentRepetitions) {
                        0 -> "10 хв"
                        1 -> "1 день"
                        else -> formatDays(newInterval)
                    },
                )
            }

            ReviewDifficulty.EASY -> {
                val newEase = minOf(3.0, currentEaseFactor + 0.15)
                val newInterval = when (currentRepetitions) {
                    0 -> 1
                    1 -> 4
                    else -> (currentInterval * newEase * 1.3).toInt()
                }
                val nextReview = now + newInterval * 24 * 60 * 60 * 1000L
                SrsResult(
                    nextReviewAt = nextReview,
                    easeFactor = newEase,
                    interval = newInterval,
                    repetitions = currentRepetitions + 1,
                    nextReviewLabel = formatDays(newInterval),
                )
            }
        }
    }

    // Підрахунок скільки карток потрібно повторити зараз
    fun getNextReviewLabel(nextReviewAt: Long): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val diff = nextReviewAt - now
        return when {
            diff <= 0 -> "Зараз"
            diff < 60 * 60 * 1000L -> "${diff / 60000} хв"
            diff < 24 * 60 * 60 * 1000L -> "${diff / 3600000} год"
            else -> formatDays((diff / (24 * 60 * 60 * 1000L)).toInt())
        }
    }

    private fun formatDays(days: Int): String = when (days) {
        1 -> "1 день"
        in 2..4 -> "$days дні"
        else -> "$days днів"
    }
}