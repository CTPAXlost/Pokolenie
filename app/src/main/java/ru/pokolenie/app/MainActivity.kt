package ru.pokolenie.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier.modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.pokolenie.app.ui.navigation.Routes
import ru.pokolenie.app.ui.screens.AppsScreen
import ru.pokolenie.app.ui.screens.HomeScreen
import ru.pokolenie.app.ui.screens.ServersScreen
import ru.pokolenie.app.ui.screens.SettingsScreen
import ru.pokolenie.app.ui.screens.SourcesScreen
import ru.pokolenie.app.ui.screens.WarpScreen
import ru.pokolenie.app.ui.theme.Brass
import ru.pokolenie.app.ui.theme.Ink
import ru.pokolenie.app.ui.theme.InkElevated
import ru.pokolenie.app.ui.theme.MistDim
import ru.pokolenie.app.ui.theme.PokolenieTheme
import ru.pokolenie.app.viewmodel.MainViewModel
import ru.pokolenie.app.vpn.VpnController

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PokolenieTheme {
                val nav = rememberNavController()
                val snackbar = remember { SnackbarHostState() }
                val home by vm.home.collectAsStateWithLifecycle()
                val servers by vm.servers.collectAsStateWithLifecycle()
                val sources by vm.sources.collectAsStateWithLifecycle()
                val warp by vm.warpProfiles.collectAsStateWithLifecycle()
                val settings by vm.settings.collectAsStateWithLifecycle()
                val apps by vm.apps.collectAsStateWithLifecycle()
                val backStack by nav.currentBackStackEntryAsState()
                val route = backStack?.destination?.route ?: Routes.Home

                var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
                val vpnPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        pendingAction?.invoke()
                    } else {
                        vm.toast("Нужно разрешение VPN")
                    }
                    pendingAction = null
                }

                fun ensureVpnThen(block: () -> Unit) {
                    val prepare = VpnController.prepareIntent(this@MainActivity)
                    if (prepare != null) {
                        pendingAction = block
                        vpnPermission.launch(prepare)
                    } else {
                        block()
                    }
                }

                LaunchedEffect(home.snackbar) {
                    val msg = home.snackbar ?: return@LaunchedEffect
                    snackbar.showSnackbar(msg)
                    vm.clearSnackbar()
                }

                val tabs = listOf(
                    Triple(Routes.Home, "Главная", Icons.Outlined.Home),
                    Triple(Routes.Servers, "Серверы", Icons.Outlined.Shield),
                    Triple(Routes.Sources, "Источники", Icons.Outlined.Dns),
                    Triple(Routes.Warp, "Warp", Icons.Outlined.Cloud),
                    Triple(Routes.Apps, "Apps", Icons.Outlined.Apps),
                    Triple(Routes.Settings, "Ещё", Icons.Outlined.Settings)
                )

                Scaffold(
                    containerColor = Ink,
                    snackbarHost = { SnackbarHost(snackbar) },
                    bottomBar = {
                        NavigationBar(containerColor = InkElevated) {
                            tabs.forEach { (path, label, icon) ->
                                NavigationBarItem(
                                    selected = route == path,
                                    onClick = {
                                        nav.navigate(path) {
                                            popUpTo(Routes.Home) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Brass,
                                        selectedTextColor = Brass,
                                        indicatorColor = Ink,
                                        unselectedIconColor = MistDim,
                                        unselectedTextColor = MistDim
                                    )
                                )
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = nav,
                        startDestination = Routes.Home,
                        modifier = Modifier.padding(padding)
                    ) {
                        composable(Routes.Home) {
                            HomeScreen(
                                state = home,
                                onConnectProxy = {
                                    ensureVpnThen { vm.connectProxy(this@MainActivity) }
                                },
                                onConnectWarp = {
                                    ensureVpnThen { vm.connectWarp(this@MainActivity) }
                                },
                                onDisconnect = { vm.disconnect(this@MainActivity) },
                                onRefresh = { vm.refreshSubscriptions() },
                                onPingAll = { vm.pingAllServers() }
                            )
                        }
                        composable(Routes.Servers) {
                            ServersScreen(
                                servers = servers,
                                busy = home.busy,
                                onSelect = vm::selectServer,
                                onPing = vm::pingServer,
                                onPingAll = vm::pingAllServers,
                                onDelete = vm::deleteServer
                            )
                        }
                        composable(Routes.Sources) {
                            SourcesScreen(
                                sources = sources,
                                busy = home.busy,
                                onRefresh = vm::refreshSubscriptions,
                                onToggle = vm::toggleSource,
                                onDelete = vm::deleteSource,
                                onAdd = vm::addSource
                            )
                        }
                        composable(Routes.Warp) {
                            WarpScreen(
                                profiles = warp,
                                busy = home.busy,
                                onGenerate = vm::generateWarp,
                                onSelect = vm::selectWarp,
                                onCopy = vm::copyWarpConf,
                                onDelete = vm::deleteWarp,
                                onConnect = {
                                    ensureVpnThen { vm.connectWarp(this@MainActivity) }
                                }
                            )
                        }
                        composable(Routes.Apps) {
                            AppsScreen(
                                apps = apps,
                                splitMode = settings.splitMode,
                                selected = settings.splitPackages,
                                onLoad = vm::loadInstalledApps,
                                onMode = vm::setSplitMode,
                                onToggle = vm::toggleSplitPackage
                            )
                        }
                        composable(Routes.Settings) {
                            SettingsScreen(
                                settings = settings,
                                onMtu = vm::setMtu,
                                onDnsMode = vm::setDnsMode,
                                onDnsServers = vm::setDnsServers,
                                onDohUrl = vm::setDohUrl,
                                onIpv6 = vm::setIpv6,
                                onAllowLan = vm::setAllowLan,
                                onKeepalive = vm::setKeepalive,
                                onAutoPing = vm::setAutoPing,
                                onPingTimeout = vm::setPingTimeout
                            )
                        }
                    }
                }
            }
        }
    }
}
