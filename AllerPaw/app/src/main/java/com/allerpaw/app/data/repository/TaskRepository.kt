package com.allerpaw.app.data.repository

import com.allerpaw.app.data.local.dao.TaskDao
import com.allerpaw.app.data.local.entity.TaskEntity
import com.allerpaw.app.data.local.entity.TaskErledigung
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(private val dao: TaskDao) {

    fun activeTasks(hundId: Long): Flow<List<TaskEntity>> = dao.getActiveForHund(hundId)

    suspend fun getAllWithPush(): List<TaskEntity> = dao.getAllWithPush()

    suspend fun upsert(task: TaskEntity): Long =
        if (task.id == 0L) dao.insert(task)
        else { dao.update(task); task.id }

    suspend fun delete(id: Long) = dao.softDelete(id)

    suspend fun getErledigungen(taskId: Long) = dao.getErledigungenForTask(taskId)

    suspend fun getErledigung(taskId: Long, datum: LocalDate) =
        dao.getErledigung(taskId, datum)

    suspend fun erledigen(taskId: Long, datum: LocalDate, notizen: String = ""): Long {
        val existing = dao.getErledigung(taskId, datum)
        return if (existing != null) {
            dao.insertErledigung(existing.copy(erledigt = true, notizen = notizen))
        } else {
            dao.insertErledigung(TaskErledigung(
                taskId  = taskId,
                datum   = datum,
                erledigt = true,
                notizen = notizen
            ))
        }
    }

    suspend fun rueckgaengig(taskId: Long, datum: LocalDate) {
        val existing = dao.getErledigung(taskId, datum) ?: return
        dao.insertErledigung(existing.copy(erledigt = false))
    }
}
