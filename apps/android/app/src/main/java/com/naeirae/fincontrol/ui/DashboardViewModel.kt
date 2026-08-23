package com.naeirae.fincontrol.ui

import androidx.lifecycle.ViewModel
import com.naeirae.fincontrol.data.DemoRepository
import com.naeirae.fincontrol.domain.DashboardSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardViewModel(
    repository: DemoRepository = DemoRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(repository.snapshot())
    val state: StateFlow<DashboardSnapshot> = _state.asStateFlow()
}
