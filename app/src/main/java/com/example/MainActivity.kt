package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.repository.CobranzasRepository
import com.example.ui.components.BrandLogo
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ClientDetailScreen
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.CDPrimary
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

sealed class AppDestination(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : AppDestination("dashboard", "Inicio", Icons.Default.Dashboard)
    object Clients : AppDestination("clients", "Clientes", Icons.Default.People)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = CobranzasRepository.getInstance(applicationContext)

        setContent {
            MyApplicationTheme {
                CobranzasApp(repository = repository)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CobranzasApp(repository: CobranzasRepository) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val userSession by repository.currentUserSession.collectAsState()

    var currentDestination by remember { mutableStateOf<AppDestination>(AppDestination.Dashboard) }
    var selectedClientId by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    // If user is not authenticated, show AuthScreen
    if (userSession == null) {
        AuthScreen(
            repository = repository,
            onAuthSuccess = {
                currentDestination = AppDestination.Dashboard
            }
        )
        return
    }

    // Detail view handling with BackHandler
    if (selectedClientId != null) {
        BackHandler {
            selectedClientId = null
        }
        ClientDetailScreen(
            clientId = selectedClientId!!,
            repository = repository,
            onBack = { selectedClientId = null }
        )
        return
    }

    // BackHandler on secondary top-level tabs returns to Dashboard
    if (currentDestination != AppDestination.Dashboard) {
        BackHandler {
            currentDestination = AppDestination.Dashboard
        }
    }

    val navItems = listOf(
        AppDestination.Dashboard,
        AppDestination.Clients
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { BrandLogo() },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                repository.syncAll()
                                snackbarHostState.showSnackbar("Sincronización solicitada")
                            }
                        },
                        modifier = Modifier.testTag("btn_top_sync")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "Sincronizar", tint = CDPrimary)
                    }

                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("btn_top_menu")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sesión: ${userSession?.name}") },
                            onClick = { showMenu = false },
                            enabled = false
                        )
                        DropdownMenuItem(
                            text = { Text("Sincronizar ahora") },
                            leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                scope.launch {
                                    val ok = repository.syncAll()
                                    snackbarHostState.showSnackbar(if (ok) "Sincronizado con cobranzascd.freebuff.app" else "No se pudo conectar a la web")
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cerrar sesión") },
                            leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                repository.signOut()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                navItems.forEach { item ->
                    val isSelected = currentDestination == item
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = item },
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CDPrimary,
                            selectedTextColor = CDPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_${item.route}")
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentDestination) {
                AppDestination.Dashboard -> {
                    DashboardScreen(
                        repository = repository,
                        onNavigateToClients = { currentDestination = AppDestination.Clients },
                        onOpenAddClient = {
                            currentDestination = AppDestination.Clients
                        },
                        onOpenClientDetail = { id ->
                            selectedClientId = id
                        }
                    )
                }
                AppDestination.Clients -> {
                    ClientsScreen(
                        repository = repository,
                        onClientClick = { id ->
                            selectedClientId = id
                        }
                    )
                }
            }
        }
    }
}
