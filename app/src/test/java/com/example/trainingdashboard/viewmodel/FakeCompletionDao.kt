package com.example.trainingdashboard.viewmodel

import com.example.trainingdashboard.data.db.CompletionDao
import com.example.trainingdashboard.data.db.DailyCompletion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeCompletionDao : CompletionDao {
    private val completions = MutableStateFlow<List<DailyCompletion>>(emptyList())

    override fun getCompletionsForDay(day: Int): Flow<List<DailyCompletion>> =
        completions.map { list -> list.filter { it.dayNumber == day } }

    override suspend fun getCompletionsForDaySnapshot(day: Int): List<DailyCompletion> =
        completions.value.filter { it.dayNumber == day }

    override suspend fun getFullyCompletedDays(): List<Int> {
        val grouped = completions.value.groupBy { it.dayNumber }
        return grouped.entries
            .filter { (_, entries) -> entries.size == 3 && entries.all { it.completed } }
            .map { it.key }
    }

    override suspend fun upsertCompletion(completion: DailyCompletion) {
        val current = completions.value.toMutableList()
        val idx = current.indexOfFirst {
            it.dayNumber == completion.dayNumber && it.exercise == completion.exercise
        }
        if (idx >= 0) current[idx] = completion else current.add(completion)
        completions.value = current
    }

    override suspend fun updateCount(day: Int, exercise: String, count: Int, completed: Boolean) {
        val current = completions.value.toMutableList()
        val idx = current.indexOfFirst { it.dayNumber == day && it.exercise == exercise }
        if (idx >= 0) {
            current[idx] = current[idx].copy(completedCount = count, completed = completed)
            completions.value = current
        }
    }

    fun seedCompletions(vararg items: DailyCompletion) {
        completions.value = items.toList()
    }
}
