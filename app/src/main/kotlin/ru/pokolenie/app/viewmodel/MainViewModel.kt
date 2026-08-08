package ru.pokolenie.app.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.pokolenie.app.PokolenieApp
import ru.pokolenie.app.data.db.ServerEntity
import ru.pokolenie.app.data.db.SourceEntity
import ru.pokolenie.app.data.db.WarpProfileEntity
import ru.pokolenie.app.data.model.RefreshSummary
import ru.pokolenie.app.routing.SingBoxConfigBuilder
import ru.pokolenie.app.settings.DnsMode
import ru.pokolenie.app.settings.SettingsState
import ru.pokolenie.app.settings.SplitMode
import ru.pokolenie.app.vpn.VpnConnectionState
import ru.pokolenie.app.vpn.VpnController

data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean
)

data class HomeUiState(
    val vpnState: VpnConnectionState = VpnConnectionState.Disconnected,
    val statusMessage: String? = null,
    val selectedServer: ServerEntity? = null,
    val selectedWarp: WarpProfileEntity? = null,
    val serverCount: Int = 0,
    val bestLatency: Long? = null,
    val busy: Boolean = false,
    val snackbar: String? = null,
    val settings: SettingsState = SettingsState(),
    val libboxReady: Boolean = false
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val pokolenie = app as PokolenieApp
    private val builder = SingBoxConfigBuilder()

    private val _home = MutableStateFlow(HomeUiState())
    val home: StateFlow<HomeUiState> = _home.asStateFlow()

    val servers = pokolenie.subscriptions.servers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sources = pokolenie.subscriptions.sources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val warpProfiles = pokolenie.database.warpDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings = pokolenie.settings.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                VpnController.state,
                VpnController.message,
                servers,
                warpProfiles,
                settings
            ) { vpn, msg, srv, warp, sett ->
                HomeUiState(
                    vpnState = vpn,
                    statusMessage = msg,
                    selectedServer = srv.firstOrNull { it.isSelected } ?: srv.firstOrNull(),
                    selectedWarp = warp.firstOrNull { it.isSelected },
                    serverCount = srv.size,
                    bestLatency = srv.mapNotNull { it.latencyMs }.minOrNull(),
                    busy = _home.value.busy,
                    snackbar = _home.value.snackbar,
                    settings = sett,
                    libboxReady = try {
                        Class.forName("io.nekohasekai.libbox.Libbox")
                        true
                    } catch (_: Throwable) {
                        false
                    }
                )
            }.collect { _home.value = it }
        }
    }

    fun clearSnackbar() {
        _home.value = _home.value.copy(snackbar = null)
    }

    fun toast(message: String) {
        _home.value = _home.value.copy(snackbar = message)
    }

    fun refreshSubscriptions() {
        viewModelScope.launch {
            _home.value = _home.value.copy(busy = true)
            try {
                val summary: RefreshSummary = pokolenie.subscriptions.refreshAll()
                var removed = 0
                if (settings.value.autoPingAfterRefresh) {
                    val list = pokolenie.database.serverDao().getAll()
                    val (_, dead) = pokolenie.ping.pingAll(list, settings.value.pingTimeoutMs)
                    removed = dead
                }
                toast(
                    "Источники OK ${summary.sourcesOk}/${summary.sourcesOk + summary.sourcesFailed}. " +
                        "+${summary.added}, обновлено ${summary.updated}, удалено мёртвых $removed"
                )
            } catch (e: Exception) {
                toast(e.message ?: "Ошибка обновления")
            } finally {
                _home.value = _home.value.copy(busy = false)
            }
        }
    }

    fun pingServer(server: ServerEntity) {
        viewModelScope.launch {
            val result = pokolenie.ping.pingOne(server, settings.value.pingTimeoutMs)
            toast(
                if (result.ok) "${server.name}: ${result.latencyMs} ms"
                else "${server.name}: нет ответа — удалён"
            )
        }
    }

    fun pingAllServers() {
        viewModelScope.launch {
            _home.value = _home.value.copy(busy = true)
            try {
                val list = pokolenie.database.serverDao().getAll()
                val (results, removed) = pokolenie.ping.pingAll(list, settings.value.pingTimeoutMs)
                val ok = results.count { it.ok }
                toast("Пинг: живых $ok, удалено $removed")
            } finally {
                _home.value = _home.value.copy(busy = false)
            }
        }
    }

    fun selectServer(id: Long) {
        viewModelScope.launch { pokolenie.subscriptions.selectServer(id) }
    }

    fun deleteServer(id: Long) {
        viewModelScope.launch { pokolenie.subscriptions.deleteServer(id) }
    }

    fun addSource(name: String, url: String) {
        viewModelScope.launch {
            pokolenie.subscriptions.addSource(name, url)
            toast("Источник добавлен")
        }
    }

    fun toggleSource(source: SourceEntity, enabled: Boolean) {
        viewModelScope.launch { pokolenie.subscriptions.setSourceEnabled(source.id, enabled) }
    }

    fun deleteSource(id: Long) {
        viewModelScope.launch { pokolenie.subscriptions.deleteSource(id) }
    }

    fun generateWarp() {
        viewModelScope.launch {
            _home.value = _home.value.copy(busy = true)
            try {
                val generated = pokolenie.warp.generate(
                    name = "Warp ${System.currentTimeMillis() % 100000}",
                    mtu = settings.value.mtu,
                    amneziaStyle = true
                )
                pokolenie.warp.select(generated.profile.id)
                toast("Warp сгенерирован: ${generated.profile.endpointHost}")
            } catch (e: Exception) {
                toast(e.message ?: "Ошибка Warp")
            } finally {
                _home.value = _home.value.copy(busy = false)
            }
        }
    }

    fun selectWarp(id: Long) {
        viewModelScope.launch { pokolenie.warp.select(id) }
    }

    fun deleteWarp(id: Long) {
        viewModelScope.launch { pokolenie.warp.delete(id) }
    }

    fun copyWarpConf(profile: WarpProfileEntity) {
        val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("warp.conf", profile.confText))
        toast("Конфиг скопирован")
    }

    fun updateSettings(transform: (SettingsState) -> SettingsState) {
        viewModelScope.launch { pokolenie.settings.update(transform) }
    }

    fun setMtu(mtu: Int) = updateSettings { it.copy(mtu = mtu) }
    fun setDnsMode(mode: DnsMode) = updateSettings { it.copy(dnsMode = mode) }
    fun setDnsServers(value: String) = updateSettings { it.copy(dnsServers = value) }
    fun setDohUrl(value: String) = updateSettings { it.copy(dohUrl = value) }
    fun setIpv6(value: Boolean) = updateSettings { it.copy(ipv6 = value) }
    fun setAllowLan(value: Boolean) = updateSettings { it.copy(allowLan = value) }
    fun setKeepalive(value: Int) = updateSettings { it.copy(keepalive = value) }
    fun setAutoPing(value: Boolean) = updateSettings { it.copy(autoPingAfterRefresh = value) }
    fun setPingTimeout(value: Int) = updateSettings { it.copy(pingTimeoutMs = value) }
    fun setSplitMode(mode: SplitMode) = updateSettings { it.copy(splitMode = mode) }
    fun setWhitelist(value: Boolean) = updateSettings { it.copy(whitelistEnabled = value) }
    fun setFakeIp(value: Boolean) = updateSettings { it.copy(fakeIpEnabled = value) }
    fun setFakeDns(value: Boolean) = updateSettings { it.copy(fakeDnsEnabled = value) }

    fun toggleSplitPackage(packageName: String) {
        updateSettings { current ->
            val next = current.splitPackages.toMutableSet()
            if (!next.add(packageName)) next.remove(packageName)
            current.copy(splitPackages = next)
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            val pm = getApplication<Application>().packageManager
            val list = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .map {
                    InstalledApp(
                        packageName = it.packageName,
                        label = pm.getApplicationLabel(it).toString(),
                        isSystem = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                }
                .filter { !it.isSystem }
                .sortedBy { it.label.lowercase() }
            _apps.value = list
        }
    }

    fun toggleVpn(context: Context) {
        when (VpnController.state.value) {
            VpnConnectionState.Connected, VpnConnectionState.Connecting -> disconnect(context)
            else -> connectProxy(context)
        }
    }

    fun connectProxy(context: Context) {
        viewModelScope.launch {
            val server = pokolenie.subscriptions.getSelectedServer()
            if (server == null) {
                toast("Выбери сервер на главной или во вкладке «Серверы».")
                return@launch
            }
            val config = builder.buildForProxy(server, settings.value)
            VpnController.connect(context, config, "${server.protocol} ${server.name}")
        }
    }

    fun connectWarp(context: Context) {
        viewModelScope.launch {
            pokolenie.warp.ensureBundledProfiles()
            val warp = pokolenie.database.warpDao().getSelected()
                ?: pokolenie.database.warpDao().getAll().firstOrNull()
            if (warp == null) {
                toast("Нет Warp-профилей. Нажми «Сгенерировать» или перезапусти приложение.")
                return@launch
            }
            val config = builder.buildForWarp(warp, settings.value)
            VpnController.connect(context, config, "WARP ${warp.name}")
        }
    }

    fun disconnect(context: Context) {
        VpnController.disconnect(context)
    }
}
