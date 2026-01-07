package com.example.the_jury.ui.screens
import androidx.compose.foundation.layout.RowScope


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.TabOptions

object HomeScreen : Screen {
    @Composable
    override fun Content() {
        TabNavigator(ExecutionTab) {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        TabNavigationItem(ExecutionTab)
                        TabNavigationItem(AgentsTab)
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    CurrentTab()
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabNavigationItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current

    NavigationBarItem(
        selected = tabNavigator.current == tab,
        onClick = { tabNavigator.current = tab },
        label = { Text(tab.options.title) },
        icon = {
            tab.options.icon?.let { icon ->
                Icon(painter = icon, contentDescription = tab.options.title)
            }
        }
    )
}

object ExecutionTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Run"
            val icon = rememberVectorPainter(Icons.Default.PlayArrow)
            return remember {
                TabOptions(
                    index = 0u,
                    title = title,
                    icon = icon
                )
            }
        }

    @Composable
    override fun Content() {
        ExecutionScreen().Content()
    }
}

object AgentsTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "Agents"
            val icon = rememberVectorPainter(Icons.Default.Person)
            return remember {
                TabOptions(
                    index = 1u,
                    title = title,
                    icon = icon
                )
            }
        }

    @Composable
    override fun Content() {
        // We will implement AgentListScreenContent here to avoid nesting navigators if possible,
        // or just delegate to a screen.
        AgentListScreen().Content()
    }
}
