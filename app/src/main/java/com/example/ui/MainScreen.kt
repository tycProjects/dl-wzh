package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.components.CelebrationBanner
import com.example.ui.screens.dialogs.AddEditHabitDialog
import com.example.ui.screens.manage.ManageHabitsScreen
import com.example.ui.screens.stats.StatsScreen
import com.example.ui.screens.today.TodayScreen
import com.example.ui.theme.FlameAmber
import com.example.ui.theme.FlameOrange
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainScreen(
    viewModel: HabitViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val habitItems by viewModel.habitItems.collectAsStateWithLifecycle()
    val streakInfo by viewModel.streakInfo.collectAsStateWithLifecycle()
    val statsData by viewModel.statsData.collectAsStateWithLifecycle()
    val allHabits by viewModel.allHabits.collectAsStateWithLifecycle()
    val isAddEditOpen by viewModel.isAddEditOpen.collectAsStateWithLifecycle()
    val editingHabit by viewModel.editingHabit.collectAsStateWithLifecycle()

    var celebrationMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.celebrationEvent.collectLatest { msg ->
            celebrationMessage = msg
        }
    }

    // Handle back button on secondary tabs
    BackHandler(enabled = selectedTab != 0) {
        viewModel.selectTab(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Whatshot else Icons.Outlined.Whatshot,
                            contentDescription = stringResource(R.string.tab_today)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_today)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FlameOrange,
                        selectedTextColor = FlameOrange,
                        indicatorColor = FlameOrange.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_tab_today")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.Insights else Icons.Outlined.Insights,
                            contentDescription = stringResource(R.string.tab_stats)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_stats)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FlameOrange,
                        selectedTextColor = FlameOrange,
                        indicatorColor = FlameOrange.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_tab_stats")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.FormatListBulleted else Icons.Outlined.FormatListBulleted,
                            contentDescription = stringResource(R.string.tab_habits)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_habits)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FlameOrange,
                        selectedTextColor = FlameOrange,
                        indicatorColor = FlameOrange.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_tab_habits")
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { viewModel.openAddHabit() },
                    containerColor = FlameOrange,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_habit")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.add_habit))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> TodayScreen(
                    habitItems = habitItems,
                    streakInfo = streakInfo,
                    onToggleHabit = { viewModel.toggleHabit(it) },
                    onEditHabit = { viewModel.openEditHabit(it) },
                    onAddNewHabit = { viewModel.openAddHabit() }
                )
                1 -> StatsScreen(
                    statsData = statsData
                )
                2 -> ManageHabitsScreen(
                    habits = allHabits,
                    onEditHabit = { viewModel.openEditHabit(it) },
                    onToggleArchive = { viewModel.toggleArchive(it) },
                    onAddNewHabit = { viewModel.openAddHabit() }
                )
            }

            // Top celebration toast
            CelebrationBanner(
                message = celebrationMessage,
                onDismiss = { celebrationMessage = null },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    if (isAddEditOpen) {
        AddEditHabitDialog(
            initialHabit = editingHabit,
            onDismiss = { viewModel.dismissAddEdit() },
            onSave = { viewModel.saveHabit(it) },
            onDelete = { viewModel.deleteHabit(it) }
        )
    }
}
