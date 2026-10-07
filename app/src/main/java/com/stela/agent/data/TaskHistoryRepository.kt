package com.stela.agent.data

class TaskHistoryRepository(private val database: TaskHistoryDatabase) {
    suspend fun addTask(
        id: String,
        prompt: String,
        status: String,
        summary: String,
        durationMs: Long,
        errorSummary: String? = null
    ) {
        database.taskHistoryDao().insert(
            TaskHistoryEntity(
                id = id,
                timestamp = System.currentTimeMillis(),
                prompt = prompt,
                status = status,
                summary = summary,
                durationMs = durationMs,
                errorSummary = errorSummary
            )
        )
    }

    suspend fun recentTasks(): List<TaskHistoryEntity> = database.taskHistoryDao().recent()
}
