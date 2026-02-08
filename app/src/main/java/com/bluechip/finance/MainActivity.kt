package com.bluechip.finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bluechip.finance.ui.screens.*
import com.bluechip.finance.ui.theme.BlueChipTheme
import com.bluechip.finance.ui.theme.LocalAppColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BlueChipTheme {
                BlueChipApp()
            }
        }
    }
}

data class NavItem(val route: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueChipApp() {
    val navController = rememberNavController()
    val colors = LocalAppColors.current
    val items = listOf(
        NavItem("home", "Ana Sayfa", Icons.Default.Home),
        NavItem("overtime", "Mesai", Icons.Default.AccessTime),
        NavItem("severance", "Kıdem", Icons.Default.AccountBalance),
        NavItem("tax", "Vergi", Icons.Default.Receipt),
        NavItem("leave", "İzin", Icons.Default.DateRange)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blue Chip Finance", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.textPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                val navBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStack?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, item.label) },
                        label = { Text(item.label, fontSize = 10.sp) },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    // DÜZELTME: Doğru navigation davranışı
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
            composable("overtime") { OvertimeScreen() }
            composable("severance") { SeveranceScreen() }
            composable("tax") { TaxScreen() }
            composable("leave") { AnnualLeaveScreen() }
        }
    }
}
