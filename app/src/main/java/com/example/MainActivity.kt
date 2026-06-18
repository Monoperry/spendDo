package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Expense
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.GoogleUser
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

// Category Configuration with emojis and Duo colors
data class CategoryInfo(
    val name: String,
    val emoji: String,
    val color: Color,
    val shadowColor: Color
)

val Categories = listOf(
    CategoryInfo("Food", "🍔", DuoOrange, DuoOrangeShadow),
    CategoryInfo("Travel", "🛫", DuoBlue, DuoBlueShadow),
    CategoryInfo("Shopping", "🛍️", DuoPurple, DuoPurpleShadow),
    CategoryInfo("Entertainment", "🎮", DuoRed, DuoRedShadow),
    CategoryInfo("Bills", "🧾", DuoGreen, DuoGreenShadow),
    CategoryInfo("Other", "✨", Color(0xFF00D2C4), Color(0xFF009B90))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: ExpenseViewModel) {
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val monthlyReports by viewModel.monthlyReports.collectAsStateWithLifecycle()
    val userState by viewModel.userState.collectAsStateWithLifecycle()
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val dailyCap by viewModel.dailyCap.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf("home") } // "home" or "report"
    var showAddSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSetCapDialog by remember { mutableStateOf(false) }

    // Startup daily cap warning alert
    var hasShownStartupWarning by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showStartupWarningDialog by remember { mutableStateOf(false) }

    LaunchedEffect(allExpenses, dailyCap) {
        if (!hasShownStartupWarning && dailyCap > 0.0) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            val todaySpend = allExpenses.filter { it.date >= startOfDay }.sumOf { it.amount }
            if (todaySpend > dailyCap) {
                showStartupWarningDialog = true
                hasShownStartupWarning = true
            }
        }
    }

    // On first launch, open the Add screen automatically as per user sketch instruction
    LaunchedEffect(isFirstLaunch) {
        if (isFirstLaunch) {
            showAddSheet = true
            viewModel.markFirstLaunchComplete()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DuoLightGray,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
            ) {
                // Background Shadow layer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .statusBarsPadding()
                        .background(
                            color = DuoBorderGray,
                            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                        )
                )
                // Foreground top header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .statusBarsPadding()
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = DuoDarkGray,
                            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                        )
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Logo icon with 3D shadow
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(DuoOrange, RoundedCornerShape(12.dp))
                                .border(2.dp, DuoDarkGray, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💸", fontSize = 20.sp)
                        }
                        Text(
                            text = "SPENDDU",
                            fontWeight = FontWeight.Black,
                            color = DuoDarkGray,
                            fontSize = 20.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "🦉",
                            fontSize = 24.sp
                        )
                    }

                    // Google-avatar-style settings icon button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, DuoDarkGray, CircleShape)
                            .clickable { showSettingsDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (userState != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(DuoBlue)
                                    .border(1.dp, DuoDarkGray, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userState!!.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }
                        } else {
                            Text("👤", fontSize = 20.sp)
                        }
                    }
                }
            }
        },
        bottomBar = {
            // DUOLINGO-STYLE CUSTOM BOTTOM NAVBAR (Aligns with User sketch)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = DuoBorderGray,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                    .navigationBarsPadding(),
                color = Color.White,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home/Add Feed Tab Button
                    Box(modifier = Modifier.weight(1f)) {
                        DuolingoTabButton(
                            text = "Add / Spend",
                            selected = selectedTab == "home",
                            onClick = { selectedTab = "home" },
                            icon = { Text("➕", fontSize = 18.sp) }
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    // Reports Tab Button
                    Box(modifier = Modifier.weight(1f)) {
                        DuolingoTabButton(
                            text = "Report",
                            selected = selectedTab == "report",
                            onClick = { selectedTab = "report" },
                            icon = { Text("📊", fontSize = 18.sp) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == "home") {
                DuolingoButton(
                    onClick = { showAddSheet = true },
                    backgroundColor = DuoBlue,
                    shadowColor = DuoBlueShadow,
                    borderColor = DuoDarkGray,
                    modifier = Modifier
                        .width(160.dp)
                        .padding(bottom = 8.dp, end = 8.dp)
                        .testTag("fab_add_expense")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add spent", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    "home" -> HomeFeedSection(
                        allExpenses = allExpenses,
                        selectedCurrency = selectedCurrency,
                        dailyCap = dailyCap,
                        onSetDailyCap = { showSetCapDialog = true },
                        onDeleteExpense = { viewModel.deleteExpense(it) },
                        onOpenAddForm = { showAddSheet = true }
                    )
                    "report" -> ReportsSection(
                        monthlyReports = monthlyReports,
                        selectedCurrency = selectedCurrency
                    )
                }
            }

            // Slide Up Bottom Sheet for Adding Expenses
            if (showAddSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAddSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Color.White,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = DuoBorderGray) },
                ) {
                    AddExpenseBottomSheetForm(
                        selectedCurrency = selectedCurrency,
                        onDismiss = { showAddSheet = false },
                        onSubmit = { amount, forWhat, paidTo, cat ->
                            viewModel.addExpense(amount, forWhat, paidTo, cat, System.currentTimeMillis())
                            showAddSheet = false
                        }
                    )
                }
            }

            // Google Login Settings Dialog
            if (showSettingsDialog) {
                SettingsProfileDialog(
                    userState = userState,
                    selectedCurrency = selectedCurrency,
                    onCurrencyChange = { viewModel.setCurrency(it) },
                    dailyCap = dailyCap,
                    onDailyCapChange = { viewModel.setDailyCap(it) },
                    onLoginWithGoogle = { name, email -> viewModel.loginWithGoogle(name, email) },
                    onLogout = { viewModel.logout() },
                    onDismiss = { showSettingsDialog = false }
                )
            }

            // Specify Daily Cap Dialog
            if (showSetCapDialog) {
                SetDailyCapDialog(
                    currentCap = dailyCap,
                    selectedCurrency = selectedCurrency,
                    onSaveCap = { viewModel.setDailyCap(it) },
                    onDismiss = { showSetCapDialog = false }
                )
            }

            // Daily Cap Crossing Warning Dialog
            if (showStartupWarningDialog) {
                Dialog(onDismissRequest = { showStartupWarningDialog = false }) {
                    DuolingoCard(
                        backgroundColor = Color.White,
                        borderColor = DuoDarkGray,
                        shadowColor = DuoShadowGray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🦉", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Duo Alert! ⚠️",
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                color = DuoRed
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "You have already crossed your daily cap of $selectedCurrency${String.format(Locale.getDefault(), "%.2f", dailyCap)} for today!",
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = DuoDarkGray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Let's be extra frugal and practice mindfulness for the rest of the day!",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            DuolingoButton(
                                onClick = { showStartupWarningDialog = false },
                                backgroundColor = DuoBlue,
                                shadowColor = DuoBlueShadow,
                                borderColor = DuoDarkGray,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("I'll Stay Strong! 💪", color = Color.White, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeFeedSection(
    allExpenses: List<Expense>,
    selectedCurrency: String,
    dailyCap: Double,
    onSetDailyCap: () -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onOpenAddForm: () -> Unit
) {
    val totalThisMonth = allExpenses.sumOf { it.amount }

    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfDay = calendar.timeInMillis

    val todayExpenses = allExpenses.filter { it.date >= startOfDay }
    val todaySpend = todayExpenses.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // DUOLINGO OWL HERO PANEL
        item {
            val isOverCap = dailyCap > 0.0 && todaySpend > dailyCap
            DuolingoCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOverCap) "🦉🚨" else "🦉",
                        fontSize = 54.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Duo says:",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = if (isOverCap) DuoRed else DuoGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (allExpenses.isEmpty()) {
                                "Tap below or open the menu to log your first spend! Let's build a streak!"
                            } else if (isOverCap) {
                                "Oh no! You crossed your daily cap! Put that wallet away and start saving! 📉"
                            } else {
                                "Great work tracking your expenses today! Keep your budget healthy! 🌟"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DuoDarkGray
                        )
                    }
                }
            }
        }

        // DAILY SUMMARY / TODAY'S SPEND CARD
        item {
            val isCapCrossed = dailyCap > 0.0 && todaySpend > dailyCap
            DuolingoCard(
                backgroundColor = if (isCapCrossed) DuoRed else DuoGreen,
                borderColor = DuoDarkGray,
                shadowColor = if (isCapCrossed) DuoRedShadow else DuoGreenShadow,
                shadowDepth = 5.dp,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TODAY'S SPEND",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%s%.2f", selectedCurrency, todaySpend),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (dailyCap > 0.0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Cap limit: $selectedCurrency${String.format(Locale.getDefault(), "%.2f", dailyCap)}" + 
                                       if (isCapCrossed) " ⚠️ OVER CAP!" else " (under control)",
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        } else {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "No daily cap set.",
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (isCapCrossed) "🚨" else "🍔", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        // Cute action button to customize cap
                        Row(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .clickable { onSetDailyCap() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎯 Set Cap",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // TOTAL SPENT OVERALL PANEL
        item {
            DuolingoCard(
                backgroundColor = Color.White,
                borderColor = DuoDarkGray,
                shadowColor = DuoShadowGray,
                shadowDepth = 5.dp,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "TOTAL SPENT OVERALL",
                            color = DuoDarkGray,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%s%.2f", selectedCurrency, totalThisMonth),
                            color = DuoGreen,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text("💰", fontSize = 42.sp)
                }
            }
        }

        // TITLE FOR TRANSACTIONS FEED
        item {
            Text(
                text = "Recent Spends",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = DuoDarkGray,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (allExpenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✨", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No spends logged yet!",
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        DuolingoButton(
                            onClick = onOpenAddForm,
                            backgroundColor = DuoBlue,
                            shadowColor = DuoBlueShadow,
                            modifier = Modifier.width(180.dp)
                        ) {
                            Text("Log Spent 🛒", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(allExpenses, key = { it.id }) { expense ->
                ExpenseItemCard(
                    expense = expense,
                    selectedCurrency = selectedCurrency,
                    onDelete = { onDeleteExpense(expense) }
                )
            }
        }

        // Dynamic spacing at the bottom to allow scrolling past FAB
        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    selectedCurrency: String = "$",
    onDelete: () -> Unit
) {
    val category = Categories.find { it.name == expense.category } ?: Categories.first()
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    DuolingoCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon with solid cartoon background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(category.color, RoundedCornerShape(12.dp))
                    .border(2.dp, category.shadowColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(category.emoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.forWhat,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = DuoDarkGray
                )
                Spacer(modifier = Modifier.height(1.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Paid to: ",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = expense.paidTo,
                        fontSize = 12.sp,
                        color = DuoDarkGray,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = dateFormat.format(Date(expense.date)),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Pricing details & Delete
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format(Locale.getDefault(), "%s%.2f", selectedCurrency, expense.amount),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = DuoDarkGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Spend",
                        tint = DuoRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Expandable Reports Section
@Composable
fun ReportsSection(
    monthlyReports: Map<String, List<Expense>>,
    selectedCurrency: String = "$"
) {
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DuolingoCard(
                backgroundColor = DuoPurple,
                borderColor = DuoDarkGray,
                shadowColor = DuoPurpleShadow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "MONTHLY HISTORY",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Check out your month-wise breakdowns below. Toggle to view listings in date-ascending order!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        if (monthlyReports.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📉", fontSize = 54.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No report records yet!",
                            color = DuoDarkGray,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        } else {
            // Render each month's aggregated card
            monthlyReports.forEach { (monthStr, expenses) ->
                val totalAmount = expenses.sumOf { it.amount }
                val isExpanded = expandedStates[monthStr] ?: false

                item {
                    DuolingoCard(
                        backgroundColor = Color.White,
                        borderColor = DuoDarkGray,
                        shadowColor = if (isExpanded) DuoBlue else DuoShadowGray,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { expandedStates[monthStr] = !isExpanded }
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📅", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = monthStr,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            color = DuoDarkGray
                                        )
                                        Text(
                                            text = "${expenses.size} spent items",
                                            fontSize = 12.sp,
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%s%.2f", selectedCurrency, totalAmount),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = DuoGreen,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand month",
                                        tint = DuoDarkGray
                                    )
                                }
                            }

                            // EXPANDED LIST: ASCENDING ORDER OF DATES
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Divider(color = DuoBorderGray, modifier = Modifier.padding(bottom = 8.dp))
                                    
                                    expenses.forEach { expense ->
                                        ReportExpenseRowItem(expense = expense, selectedCurrency = selectedCurrency)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportExpenseRowItem(
    expense: Expense,
    selectedCurrency: String = "$"
) {
    val category = Categories.find { it.name == expense.category } ?: Categories.first()
    val simpleDate = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(expense.date))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DuoLightGray, RoundedCornerShape(12.dp))
            .border(2.dp, DuoDarkGray, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date badge
        Box(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, DuoDarkGray, RoundedCornerShape(8.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = simpleDate,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = DuoDarkGray
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = category.emoji,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = expense.forWhat,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = DuoDarkGray
                )
            }
            Text(
                text = "Paid to: ${expense.paidTo}",
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Price Tag
        Text(
            text = String.format(Locale.getDefault(), "%s%.2f", selectedCurrency, expense.amount),
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = DuoDarkGray
        )
    }
}

// SLIDABLE BOTTOM SHEET FORM (Simple, cartoonish, no hectic inputs)
@Composable
fun AddExpenseBottomSheetForm(
    selectedCurrency: String = "$",
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, forWhat: String, paidTo: String, category: String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var forWhatStr by remember { mutableStateOf("") }
    var paidToStr by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }

    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "What was paid? 🦉",
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = DuoDarkGray
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Giant amount entry
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(200.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(2.dp, DuoDarkGray, RoundedCornerShape(16.dp))
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = selectedCurrency,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = DuoGreen,
                modifier = Modifier.padding(end = 4.dp)
            )
            BasicTextField(
                value = amountStr,
                onValueChange = { input: String ->
                    // Limit values to clean currency numbers (optional)
                    if (input.all { it.isDigit() || it == '.' }) {
                        amountStr = input
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = DuoDarkGray,
                    textAlign = TextAlign.Start
                ),
                singleLine = true,
                modifier = Modifier
                    .width(120.dp)
                    .testTag("amount_textfield_input")
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // For what input
        DuolingoTextField(
            value = forWhatStr,
            onValueChange = { forWhatStr = it },
            placeholder = "e.g. Tasty pizza 🍕",
            label = "Money spent for what?",
            testTag = "for_what_input"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Paid to input
        DuolingoTextField(
            value = paidToStr,
            onValueChange = { paidToStr = it },
            placeholder = "e.g. Papa Johns Pizza",
            label = "To whom it was paid?",
            testTag = "paid_to_input"
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Category Selection Row with quick taps
        Text(
            text = "Select Category",
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = DuoDarkGray,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 4.dp, bottom = 8.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(Categories) { category ->
                val isSelected = selectedCategory == category.name
                val borderCol = DuoDarkGray
                val shadowCol = if (isSelected) category.shadowColor else DuoShadowGray
                val bgCol = if (isSelected) category.color else Color.White

                DuolingoCard(
                    backgroundColor = bgCol,
                    borderColor = borderCol,
                    shadowColor = shadowCol,
                    borderWidth = 2.dp,
                    shadowDepth = 4.dp,
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        selectedCategory = category.name
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.width(100.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(category.emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = category.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = if (isSelected) Color.White else DuoDarkGray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Submit Button
        val isValid = amountStr.toDoubleOrNull() != null && forWhatStr.isNotBlank() && paidToStr.isNotBlank()

        DuolingoButton(
            onClick = {
                val amountValue = amountStr.toDoubleOrNull() ?: 0.0
                onSubmit(amountValue, forWhatStr, paidToStr, selectedCategory)
            },
            enabled = isValid,
            backgroundColor = DuoGreen,
            shadowColor = DuoGreenShadow,
            modifier = Modifier.fillMaxWidth(),
            testTag = "submit_expense_button"
        ) {
            Text(
                text = "LOG SPENT! 🚀",
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

// GOOGLE SIGN-IN INTERACTION SYSTEM (In Settings dialog)
@Composable
fun SettingsProfileDialog(
    userState: GoogleUser?,
    selectedCurrency: String,
    onCurrencyChange: (String) -> Unit,
    dailyCap: Double,
    onDailyCapChange: (Double) -> Unit,
    onLoginWithGoogle: (String, String) -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        DuolingoCard(
            backgroundColor = Color.White,
            borderColor = DuoBorderGray,
            shadowColor = DuoShadowGray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Settings ⚙️",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = DuoDarkGray
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (userState != null) {
                    // Logged in Screen User Details Panel
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(DuoBlue, CircleShape)
                            .border(3.dp, DuoBlueShadow, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userState.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Hello, ${userState.name}! 👋",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = DuoDarkGray
                    )
                    Text(
                        text = userState.email,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Backup Card
                    DuolingoCard(
                        backgroundColor = DuoLightGray,
                        borderColor = DuoBorderGray,
                        shadowColor = DuoShadowGray,
                        borderWidth = 1.dp,
                        shadowDepth = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("☁️ Google Cloud Sync Active", fontWeight = FontWeight.Bold, color = DuoGreen, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Database status: Backed up 100%", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DuolingoButton(
                        onClick = onLogout,
                        backgroundColor = DuoRed,
                        shadowColor = DuoRedShadow,
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Text("Log Out Google Account", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                } else {
                    // Google Sign-In Banner
                    Text(
                        text = "Sync & Save Your Streaks!",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = DuoDarkGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sign in to backup your daily spending and preserve your budget habits across Google devices.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // GOOGLE SIGN IN BUTTON
                    DuolingoButton(
                        onClick = {
                            // Perfect mock sign in info
                            onLoginWithGoogle("Friendly Tracker", "mrincognitohi5@gmail.com")
                        },
                        backgroundColor = Color.White,
                        borderColor = DuoBorderGray,
                        shadowColor = DuoShadowGray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DuoBorderGray, RoundedCornerShape(16.dp))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💬 ", fontSize = 16.sp) // Cute prompt icon
                            Text(
                                text = "Sign in with Google",
                                color = DuoDarkGray,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = DuoBorderGray)
                Spacer(modifier = Modifier.height(16.dp))

                // CURRENCY SELECTOR
                Text(
                    text = "SELECT CURRENCY 🌍",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = DuoDarkGray,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val currencies = listOf("$", "€", "£", "¥", "₹", "₪", "₩")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currencies) { symbol ->
                        val isSelected = selectedCurrency == symbol
                        DuolingoCard(
                            backgroundColor = if (isSelected) DuoGreen else Color.White,
                            borderColor = DuoDarkGray,
                            shadowColor = if (isSelected) DuoGreenShadow else DuoShadowGray,
                            borderWidth = 2.dp,
                            shadowDepth = 3.dp,
                            onClick = { onCurrencyChange(symbol) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = symbol,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = if (isSelected) Color.White else DuoDarkGray,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CHOSEN DAILY BUDGET CAP
                Text(
                    text = "DAILY SPENDING LIMIT 🎯",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = DuoDarkGray,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                DuolingoCard(
                    backgroundColor = DuoLightGray,
                    borderColor = DuoDarkGray,
                    shadowColor = DuoShadowGray,
                    borderWidth = 1.dp,
                    shadowDepth = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (dailyCap > 0.0) {
                                    "Limit: $selectedCurrency${String.format(Locale.getDefault(), "%.2f", dailyCap)}"
                                } else {
                                    "No spending limit set."
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = DuoDarkGray
                            )
                            Text(
                                text = "Red alert warns when crossed.",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }

                        // Customize button inline
                        var showEditField by remember { mutableStateOf(false) }
                        var capInput by remember { mutableStateOf(if (dailyCap > 0.0) dailyCap.toInt().toString() else "") }

                        if (showEditField) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                BasicTextField(
                                    value = capInput,
                                    onValueChange = { if (it.all { c -> c.isDigit() }) capInput = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = DuoDarkGray
                                    ),
                                    modifier = Modifier
                                        .width(60.dp)
                                        .background(Color.White, RoundedCornerShape(6.dp))
                                        .border(1.dp, DuoDarkGray, RoundedCornerShape(6.dp))
                                        .padding(4.dp)
                                )
                                IconButton(
                                    onClick = {
                                        val newCap = capInput.toDoubleOrNull() ?: 0.0
                                        onDailyCapChange(newCap)
                                        showEditField = false
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Save", tint = DuoGreen)
                                }
                            }
                        } else {
                            DuolingoButton(
                                onClick = { showEditField = true },
                                backgroundColor = DuoBlue,
                                shadowColor = DuoBlueShadow,
                                borderColor = DuoDarkGray,
                                modifier = Modifier.height(32.dp).width(80.dp)
                            ) {
                                Text("Modify", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = DuoBorderGray)

                Spacer(modifier = Modifier.height(12.dp))

                // App details
                Text(
                    text = "SpendDu App v1.0.0",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Crafted with cartoon 💖 in Jetpack Compose",
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun SetDailyCapDialog(
    currentCap: Double,
    selectedCurrency: String,
    onSaveCap: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var capStr by remember { mutableStateOf(if (currentCap > 0.0) currentCap.toInt().toString() else "") }

    Dialog(onDismissRequest = onDismiss) {
        DuolingoCard(
            backgroundColor = Color.White,
            borderColor = DuoDarkGray,
            shadowColor = DuoShadowGray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎯", fontSize = 54.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Daily Spending Cap",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = DuoDarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Set a budget cap limit. Duo will alert you if your single-day spends exceed this cap!",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Input box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DuoLightGray, RoundedCornerShape(12.dp))
                        .border(2.dp, DuoDarkGray, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = selectedCurrency,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = DuoGreen,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    BasicTextField(
                        value = capStr,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) capStr = input
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = DuoDarkGray
                        ),
                        modifier = Modifier.width(120.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DuolingoButton(
                        onClick = onDismiss,
                        backgroundColor = Color.White,
                        borderColor = DuoBorderGray,
                        shadowColor = DuoShadowGray,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = DuoDarkGray, fontWeight = FontWeight.Bold)
                    }

                    DuolingoButton(
                        onClick = {
                            val finalCap = capStr.toDoubleOrNull() ?: 0.0
                            onSaveCap(finalCap)
                            onDismiss()
                        },
                        backgroundColor = DuoGreen,
                        shadowColor = DuoGreenShadow,
                        borderColor = DuoDarkGray,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Cap", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
