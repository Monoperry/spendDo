package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Expense
import com.example.data.ExpenseDatabase
import com.example.data.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.LinkedHashMap

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    val allExpenses: StateFlow<List<Expense>>

    // Flow for reports: months sorted descending, items inside sorted ascending by date
    val monthlyReports: StateFlow<Map<String, List<Expense>>>

    // PERSISTENT SETTINGS
    private val prefs = application.getSharedPreferences("spenddu_prefs", android.content.Context.MODE_PRIVATE)

    private val _selectedCurrency = MutableStateFlow(prefs.getString("selected_currency", "$") ?: "$")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    private val _dailyCap = MutableStateFlow(prefs.getFloat("daily_cap", 0.0f).toDouble())
    val dailyCap: StateFlow<Double> = _dailyCap.asStateFlow()

    // Sign in state (simulated Google authentication state)
    private val _userState = MutableStateFlow<GoogleUser?>(null)
    val userState: StateFlow<GoogleUser?> = _userState.asStateFlow()

    // Sheet state to control automatic opening at first launch
    private val _isFirstLaunch = MutableStateFlow(true)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    init {
        val expenseDao = ExpenseDatabase.getDatabase(application).expenseDao()
        repository = ExpenseRepository(expenseDao)

        allExpenses = repository.allExpenses
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        monthlyReports = repository.allExpenses
            .map { list ->
                val grouped = LinkedHashMap<String, MutableList<Expense>>()
                val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                
                // Group keeping the descending month order (since allExpenses query is DESC)
                list.forEach { expense ->
                    val monthStr = monthFormat.format(Date(expense.date))
                    if (!grouped.containsKey(monthStr)) {
                        grouped[monthStr] = mutableListOf()
                    }
                    grouped[monthStr]?.add(expense)
                }

                // Sort transactions within each month in date ascending order
                grouped.mapValues { entry ->
                    entry.value.sortedBy { it.date }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyMap()
            )

        // Load mock sign-in state if saved, or default to null
        // For the sake of standard settings Google Login, we start logged out
    }

    fun setCurrency(currency: String) {
        prefs.edit().putString("selected_currency", currency).apply()
        _selectedCurrency.value = currency
    }

    fun setDailyCap(cap: Double) {
        prefs.edit().putFloat("daily_cap", cap.toFloat()).apply()
        _dailyCap.value = cap
    }

    fun addExpense(amount: Double, forWhat: String, paidTo: String, category: String, date: Long) {
        viewModelScope.launch {
            repository.insert(Expense(amount = amount, forWhat = forWhat, paidTo = paidTo, category = category, date = date))
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.delete(expense)
        }
    }

    fun markFirstLaunchComplete() {
        _isFirstLaunch.value = false
    }

    // Google Sign-In Simulation Action
    fun loginWithGoogle(name: String, email: String) {
        _userState.value = GoogleUser(
            name = name,
            email = email,
            photoUrl = "https://lh3.googleusercontent.com/a/O-A" // Standard mock photo url string
        )
    }

    fun logout() {
        _userState.value = null
    }
}

data class GoogleUser(
    val name: String,
    val email: String,
    val photoUrl: String
)
