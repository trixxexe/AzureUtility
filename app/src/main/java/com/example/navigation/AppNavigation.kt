package com.example.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AzureBottomNav
import com.example.ui.components.NavigationItem
import com.example.ui.qrforge.QrForgeScreen
import com.example.ui.jsonlens.JsonLensScreen
import com.example.ui.markitdown.MarkitdownScreen
import com.example.ui.codeeditor.CodeEditorScreen
import com.example.ui.textpad.TextpadScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val navItems = listOf(
        NavigationItem(
            route = Screen.QrForge.route,
            label = Screen.QrForge.title,
            icon = Icons.Default.QrCodeScanner
        ),
        NavigationItem(
            route = Screen.JsonLens.route,
            label = Screen.JsonLens.title,
            icon = Icons.Default.FindInPage
        ),
        NavigationItem(
            route = Screen.MarkitDown.route,
            label = Screen.MarkitDown.title,
            icon = Icons.Default.Book
        ),
        NavigationItem(
            route = Screen.CodeEditor.route,
            label = Screen.CodeEditor.title,
            icon = Icons.Default.Code
        ),
        NavigationItem(
            route = Screen.TextPad.route,
            label = Screen.TextPad.title,
            icon = Icons.Default.Notes
        )
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: Screen.QrForge.route
            AzureBottomNav(
                items = navItems,
                selectedRoute = currentRoute,
                onTabSelected = { route ->
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.QrForge.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.QrForge.route) {
                QrForgeScreen()
            }
            composable(Screen.JsonLens.route) {
                JsonLensScreen()
            }
            composable(Screen.MarkitDown.route) {
                MarkitdownScreen()
            }
            composable(Screen.CodeEditor.route) {
                CodeEditorScreen()
            }
            composable(Screen.TextPad.route) {
                TextpadScreen()
            }
        }
    }
}
