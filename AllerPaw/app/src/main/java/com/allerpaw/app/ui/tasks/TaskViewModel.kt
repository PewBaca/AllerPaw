package com.allerpaw.app.ui.tasks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allerpaw.app.data.local.entity.HundEntity
import com.allerpaw.app.data.local.entity.TaskEntity
import com.allerpaw.app.data.local.entity.TaskErledigung
import com.allerpaw.app.data.repository.HundRepository
import com.allerpaw.app.data.repository.TaskRepository
import com.allerpaw.app.util.TaskNotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TaskUiState(
    val hunde: List<HundEntity> = emptyList(),
    val selectedHundId: Long? = null,
    val tasks: List<TaskEntity> = emptyList(),
    val erledigungen: Map<Long, TaskErledigung?> = emptyMap(), // taskId → heutige Erledigung
    val editTask: TaskEntity? = null,
    val heute: LocalDate = LocalDate.now()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val hundRepo: HundRepository,
    private val taskRepo: TaskRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TaskUiState())
    val state: StateFlow<TaskUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            hundRepo.alleHunde().collect { hunde ->
                _state.update { it.copy(
                    hunde          = hunde,
                    selectedHundId = it.selectedHundId ?: hunde.firstOrNull()?.id
                ) }
            }
        }

        viewModelScope.launch {
            _state.map { it.selectedHundId }.filterNotNull().distinctUntilChanged()
                .flatMapLatest { hundId -> taskRepo.activeTasks(hundId) }
                .collect { tasks ->
                    _state.update { it.copy(tasks = tasks) }
                    ladeErledigungen(tasks)
                }
        }
    }

    private fun ladeErledigungen(tasks: List<TaskEntity>) = viewModelScope.launch {
        val heute = _state.value.heute
        val map = tasks.associate { task ->
            task.id to taskRepo.getErledigung(task.id, heute)
        }
        _state.update { it.copy(erledigungen = map) }
    }

    fun selectHund(id: Long) = _state.update { it.copy(selectedHundId = id) }

    fun erledigen(taskId: Long) = viewModelScope.launch {
        taskRepo.erledigen(taskId, _state.value.heute)
        ladeErledigungen(_state.value.tasks)
    }

    fun rueckgaengig(taskId: Long) = viewModelScope.launch {
        taskRepo.rueckgaengig(taskId, _state.value.heute)
        ladeErledigungen(_state.value.tasks)
    }

    // ── CRUD ──────────────────────────────────────────────────────────────
    fun newTask() {
        val hundId = _state.value.selectedHundId ?: return
        _state.update { it.copy(editTask = TaskEntity(
            hundId        = hundId,
            titel         = "",
            wiederholung  = "taeglich"
        )) }
    }

    fun editTask(task: TaskEntity) = _state.update { it.copy(editTask = task) }
    fun dismissEdit() = _state.update { it.copy(editTask = null) }

    fun saveTask(task: TaskEntity) = viewModelScope.launch {
        taskRepo.upsert(task)
        _state.update { it.copy(editTask = null) }
        // Notifications neu planen wenn Push aktiv
        if (task.pushAktiv) TaskNotificationWorker.schedule(ctx)
    }

    fun deleteTask(id: Long) = viewModelScope.launch {
        taskRepo.delete(id)
    }
}
