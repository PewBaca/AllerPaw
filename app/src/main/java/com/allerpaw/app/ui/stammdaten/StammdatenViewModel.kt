package com.allerpaw.app.ui.stammdaten

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.allerpaw.app.data.local.entity.HundEntity
import com.allerpaw.app.data.repository.HundRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ──────────────────────────────────────────────────────────────

@HiltViewModel
class StammdatenViewModel @Inject constructor(
    private val repo: HundRepository
) : ViewModel() {

    val hunde: StateFlow<List<HundEntity>> = repo.alleHunde()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _editHund = MutableStateFlow<HundEntity?>(null)
    val editHund: StateFlow<HundEntity?> = _editHund

    fun editNew()            { _editHund.value = HundEntity(name = "") }
    fun editExisting(h: HundEntity) { _editHund.value = h }
    fun dismissEdit()        { _editHund.value = null }

    fun save(hund: HundEntity) = viewModelScope.launch {
        repo.upsert(hund)
        _editHund.value = null
    }

    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }
}

// ── Screen ─────────────────────────────────────────────────────────────────
