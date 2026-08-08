package ru.pokolenie.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("pokolenie_settings")

enum class DnsMode { SYSTEM, CUSTOM, DOH }

enum class SplitMode { ALL, INCLUDE, EXCLUDE }

data class SettingsState(
    val mtu: Int = 1280,
    val dnsMode: DnsMode = DnsMode.CUSTOM,
    val dnsServers: String = "1.1.1.1, 8.8.8.8",
    val dohUrl: String = "https://dns.google/dns-query",
    val ipv6: Boolean = false,
    val allowLan: Boolean = true,
    val keepalive: Int = 25,
    val autoPingAfterRefresh: Boolean = true,
    val pingTimeoutMs: Int = 3000,
    val splitMode: SplitMode = SplitMode.ALL,
    val splitPackages: Set<String> = emptySet(),
    /** Whitelist is always enforced; this flag is informational / locked in UI. */
    val whitelistForced: Boolean = true
)

class AppSettings(private val context: Context) {
    private val mtuKey = intPreferencesKey("mtu")
    private val dnsModeKey = stringPreferencesKey("dns_mode")
    private val dnsServersKey = stringPreferencesKey("dns_servers")
    private val dohUrlKey = stringPreferencesKey("doh_url")
    private val ipv6Key = booleanPreferencesKey("ipv6")
    private val allowLanKey = booleanPreferencesKey("allow_lan")
    private val keepaliveKey = intPreferencesKey("keepalive")
    private val autoPingKey = booleanPreferencesKey("auto_ping")
    private val pingTimeoutKey = intPreferencesKey("ping_timeout")
    private val splitModeKey = stringPreferencesKey("split_mode")
    private val splitPackagesKey = stringSetPreferencesKey("split_packages")

    val state: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        SettingsState(
            mtu = prefs[mtuKey] ?: 1280,
            dnsMode = runCatching { DnsMode.valueOf(prefs[dnsModeKey] ?: DnsMode.CUSTOM.name) }
                .getOrDefault(DnsMode.CUSTOM),
            dnsServers = prefs[dnsServersKey] ?: "1.1.1.1, 8.8.8.8",
            dohUrl = prefs[dohUrlKey] ?: "https://dns.google/dns-query",
            ipv6 = prefs[ipv6Key] ?: false,
            allowLan = prefs[allowLanKey] ?: true,
            keepalive = prefs[keepaliveKey] ?: 25,
            autoPingAfterRefresh = prefs[autoPingKey] ?: true,
            pingTimeoutMs = prefs[pingTimeoutKey] ?: 3000,
            splitMode = runCatching { SplitMode.valueOf(prefs[splitModeKey] ?: SplitMode.ALL.name) }
                .getOrDefault(SplitMode.ALL),
            splitPackages = prefs[splitPackagesKey] ?: emptySet(),
            whitelistForced = true
        )
    }

    suspend fun update(transform: (SettingsState) -> SettingsState) {
        context.dataStore.edit { prefs ->
            val current = SettingsState(
                mtu = prefs[mtuKey] ?: 1280,
                dnsMode = runCatching { DnsMode.valueOf(prefs[dnsModeKey] ?: DnsMode.CUSTOM.name) }
                    .getOrDefault(DnsMode.CUSTOM),
                dnsServers = prefs[dnsServersKey] ?: "1.1.1.1, 8.8.8.8",
                dohUrl = prefs[dohUrlKey] ?: "https://dns.google/dns-query",
                ipv6 = prefs[ipv6Key] ?: false,
                allowLan = prefs[allowLanKey] ?: true,
                keepalive = prefs[keepaliveKey] ?: 25,
                autoPingAfterRefresh = prefs[autoPingKey] ?: true,
                pingTimeoutMs = prefs[pingTimeoutKey] ?: 3000,
                splitMode = runCatching { SplitMode.valueOf(prefs[splitModeKey] ?: SplitMode.ALL.name) }
                    .getOrDefault(SplitMode.ALL),
                splitPackages = prefs[splitPackagesKey] ?: emptySet()
            )
            val next = transform(current).copy(whitelistForced = true)
            prefs[mtuKey] = next.mtu.coerceIn(576, 1500)
            prefs[dnsModeKey] = next.dnsMode.name
            prefs[dnsServersKey] = next.dnsServers
            prefs[dohUrlKey] = next.dohUrl
            prefs[ipv6Key] = next.ipv6
            prefs[allowLanKey] = next.allowLan
            prefs[keepaliveKey] = next.keepalive.coerceIn(0, 120)
            prefs[autoPingKey] = next.autoPingAfterRefresh
            prefs[pingTimeoutKey] = next.pingTimeoutMs.coerceIn(500, 15000)
            prefs[splitModeKey] = next.splitMode.name
            prefs[splitPackagesKey] = next.splitPackages
        }
    }
}
