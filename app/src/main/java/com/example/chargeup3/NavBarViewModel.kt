package com.example.chargeup3

import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class NavBarViewModel : ViewModel() {
    // Состояние для управления видимостью панели навигации
    private val _showNavBar = mutableStateOf(true)
    val showNavBar: State<Boolean> get() = _showNavBar

    // Функция для изменения состояния
    fun setShowNavBar(show: Boolean) {
        _showNavBar.value = show
    }
}
