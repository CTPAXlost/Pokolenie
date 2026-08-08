package ru.pokolenie.app.routing

import org.json.JSONArray
import org.json.JSONObject
import ru.pokolenie.app.data.db.ServerEntity
import ru.pokolenie.app.data.db.WarpProfileEntity
import ru.pokolenie.app.data.model.ProtocolType
import ru.pokolenie.app.settings.DnsMode
import ru.pokolenie.app.settings.SettingsState
import ru.pokolenie.app.settings.SplitMode

class SingBoxConfigBuilder {

    fun buildForProxy(server: ServerEntity, settings: SettingsState): String {
        val root = baseConfig(settings)
        val outbounds = root.getJSONArray("outbounds")
        outbounds.put(proxyOutbound(server))
        outbounds.put(JSONObject().put("type", "direct").put("tag", "direct"))
        outbounds.put(JSONObject().put("type", "block").put("tag", "block"))
        outbounds.put(
            JSONObject()
                .put("type", "selector")
                .put("tag", "proxy")
                .put("outbounds", JSONArray().put(serverTag(server)).put("direct"))
                .put("default", serverTag(server))
        )
        applyRouting(root, settings)
        applySplitHint(root, settings)
        return root.toString(2)
    }

    fun buildForWarp(profile: WarpProfileEntity, settings: SettingsState): String {
        val root = baseConfig(settings)
        val outbounds = root.getJSONArray("outbounds")
        outbounds.put(warpOutbound(profile, settings))
        outbounds.put(JSONObject().put("type", "direct").put("tag", "direct"))
        outbounds.put(JSONObject().put("type", "block").put("tag", "block"))
        outbounds.put(
            JSONObject()
                .put("type", "selector")
                .put("tag", "proxy")
                .put("outbounds", JSONArray().put("warp").put("direct"))
                .put("default", "warp")
        )
        applyRouting(root, settings)
        applySplitHint(root, settings)
        return root.toString(2)
    }

    private fun baseConfig(settings: SettingsState): JSONObject {
        val dnsServers = JSONArray()
        val dnsDetour = if (settings.fakeDnsEnabled || !settings.whitelistEnabled) "proxy" else "direct"
        when (settings.dnsMode) {
            DnsMode.SYSTEM -> {
                dnsServers.put(
                    JSONObject()
                        .put("tag", "local")
                        .put("address", "local")
                        .put("detour", "direct")
                )
            }
            DnsMode.CUSTOM -> {
                settings.dnsServers.split(',', ' ', ';')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEachIndexed { index, address ->
                        dnsServers.put(
                            JSONObject()
                                .put("tag", "dns-$index")
                                .put("address", address)
                                .put("detour", dnsDetour)
                        )
                    }
            }
            DnsMode.DOH -> {
                dnsServers.put(
                    JSONObject()
                        .put("tag", "doh")
                        .put("address", settings.dohUrl)
                        .put("detour", "proxy")
                )
            }
        }
        if (settings.fakeIpEnabled) {
            dnsServers.put(
                JSONObject()
                    .put("tag", "fakeip")
                    .put("address", "fakeip")
            )
        }
        if (dnsServers.length() == 0) {
            dnsServers.put(
                JSONObject().put("tag", "cloudflare").put("address", "1.1.1.1").put("detour", dnsDetour)
            )
        }

        val inet4 = JSONArray().put("172.19.0.1/30")
        val inet6 = JSONArray().put("fdfe:dcba:9876::1/126")
        val address = JSONArray().put("172.19.0.1/30").also {
            if (settings.ipv6) it.put("fdfe:dcba:9876::1/126")
        }

        val tun = JSONObject()
            .put("type", "tun")
            .put("tag", "tun-in")
            .put("interface_name", "pokolenie")
            .put("address", address)
            .put("mtu", settings.mtu)
            .put("auto_route", true)
            .put("strict_route", true)
            .put("stack", "mixed")
            .put("sniff", true)
            .put("sniff_override_destination", true)

        if (!settings.ipv6) {
            // keep IPv4 only
            tun.put("inet4_address", inet4)
        } else {
            tun.put("inet4_address", inet4)
            tun.put("inet6_address", inet6)
        }

        val dns = JSONObject()
            .put("servers", dnsServers)
            .put("strategy", if (settings.ipv6) "prefer_ipv4" else "ipv4_only")
            .put("independent_cache", true)
        if (settings.fakeIpEnabled) {
            dns.put(
                "fakeip",
                JSONObject()
                    .put("enabled", true)
                    .put("inet4_range", "198.18.0.0/15")
                    .put("inet6_range", "fc00::/18")
            )
        }

        return JSONObject()
            .put("log", JSONObject().put("level", "info").put("timestamp", true))
            .put("dns", dns)
            .put("inbounds", JSONArray().put(tun))
            .put("outbounds", JSONArray())
            .put(
                "route",
                JSONObject()
                    .put("auto_detect_interface", true)
                    .put("final", if (settings.whitelistEnabled) "direct" else "proxy")
                    .put("rules", JSONArray())
            )
            .put(
                "experimental",
                JSONObject().put(
                    "cache_file",
                    JSONObject().put("enabled", true).put("path", "cache.db")
                )
            )
    }

    private fun applyRouting(root: JSONObject, settings: SettingsState) {
        val rules = root.getJSONObject("route").getJSONArray("rules")
        if (settings.fakeDnsEnabled || settings.fakeIpEnabled || settings.whitelistEnabled) {
            rules.put(
                JSONObject()
                    .put("protocol", "dns")
                    .put("action", "hijack-dns")
            )
        }
        rules.put(
            JSONObject()
                .put("ip_is_private", true)
                .put("outbound", "direct")
        )

        if (!settings.whitelistEnabled) {
            root.getJSONObject("route").put("final", "proxy")
            return
        }

        val domainSuffix = JSONArray()
        WhitelistRules.domainSuffixes.forEach { domainSuffix.put(it) }
        rules.put(
            JSONObject()
                .put("domain_suffix", domainSuffix)
                .put("outbound", "proxy")
        )

        val geosite = JSONArray()
        WhitelistRules.geositeCategories.forEach { geosite.put(it) }
        rules.put(
            JSONObject()
                .put("geosite", geosite)
                .put("outbound", "proxy")
        )

        val geoip = JSONArray()
        WhitelistRules.geoipCategories.forEach { geoip.put(it) }
        rules.put(
            JSONObject()
                .put("geoip", geoip)
                .put("outbound", "proxy")
        )

        // Forced: everything else stays direct (final = direct)
        root.getJSONObject("route").put("final", "direct")
    }

    private fun applySplitHint(root: JSONObject, settings: SettingsState) {
        // Package include/exclude is applied at VpnService.Builder level;
        // store metadata for the service to read.
        root.put(
            "_pokolenie",
            JSONObject()
                .put("whitelist_forced", true)
                .put("split_mode", settings.splitMode.name)
                .put("split_packages", JSONArray(settings.splitPackages.toList()))
                .put("allow_lan", settings.allowLan)
                .put("mtu", settings.mtu)
                .put("ipv6", settings.ipv6)
        )
    }

    private fun serverTag(server: ServerEntity): String = "srv-${server.id}"

    private fun proxyOutbound(server: ServerEntity): JSONObject {
        return when (server.protocol) {
            ProtocolType.VLESS -> vlessOutbound(server)
            ProtocolType.TROJAN -> trojanOutbound(server)
            else -> throw IllegalArgumentException("Unsupported protocol ${server.protocol}")
        }
    }

    private fun vlessOutbound(server: ServerEntity): JSONObject {
        val outbound = JSONObject()
            .put("type", "vless")
            .put("tag", serverTag(server))
            .put("server", server.host)
            .put("server_port", server.port)
            .put("uuid", server.uuidOrPassword)

        if (!server.flow.isNullOrBlank()) {
            outbound.put("flow", server.flow)
        }

        val transport = transportObject(server)
        if (transport != null) outbound.put("transport", transport)

        val tls = tlsObject(server)
        if (tls != null) outbound.put("tls", tls)

        return outbound
    }

    private fun trojanOutbound(server: ServerEntity): JSONObject {
        val outbound = JSONObject()
            .put("type", "trojan")
            .put("tag", serverTag(server))
            .put("server", server.host)
            .put("server_port", server.port)
            .put("password", server.uuidOrPassword)

        val transport = transportObject(server)
        if (transport != null) outbound.put("transport", transport)

        val tls = tlsObject(server) ?: JSONObject()
            .put("enabled", true)
            .put("server_name", server.sni ?: server.host)
        outbound.put("tls", tls)
        return outbound
    }

    private fun transportObject(server: ServerEntity): JSONObject? {
        val type = server.transport?.lowercase() ?: return null
        return when (type) {
            "ws", "websocket" -> JSONObject()
                .put("type", "ws")
                .put("path", server.path ?: "/")
            "grpc" -> JSONObject()
                .put("type", "grpc")
                .put("service_name", server.path ?: "")
            "http", "h2" -> JSONObject()
                .put("type", "http")
                .put("path", server.path ?: "/")
            "tcp", "", "none" -> null
            else -> JSONObject().put("type", type)
        }
    }

    private fun tlsObject(server: ServerEntity): JSONObject? {
        val security = server.security?.lowercase()
        if (security.isNullOrBlank() || security == "none") {
            return if (!server.publicKey.isNullOrBlank()) realityTls(server) else null
        }
        if (security == "reality") return realityTls(server)
        val tls = JSONObject()
            .put("enabled", true)
            .put("server_name", server.sni ?: server.host)
        if (!server.fingerprint.isNullOrBlank()) {
            tls.put("utls", JSONObject().put("enabled", true).put("fingerprint", server.fingerprint))
        }
        if (!server.alpn.isNullOrBlank()) {
            tls.put("alpn", JSONArray(server.alpn.split(',').map { it.trim() }))
        }
        return tls
    }

    private fun realityTls(server: ServerEntity): JSONObject {
        val tls = JSONObject()
            .put("enabled", true)
            .put("server_name", server.sni ?: server.host)
            .put(
                "reality",
                JSONObject()
                    .put("enabled", true)
                    .put("public_key", server.publicKey ?: "")
                    .put("short_id", server.shortId ?: "")
            )
        if (!server.fingerprint.isNullOrBlank()) {
            tls.put("utls", JSONObject().put("enabled", true).put("fingerprint", server.fingerprint))
        } else {
            tls.put("utls", JSONObject().put("enabled", true).put("fingerprint", "chrome"))
        }
        return tls
    }

    private fun warpOutbound(profile: WarpProfileEntity, settings: SettingsState): JSONObject {
        val localAddress = JSONArray().put(profile.addressV4)
        if (!profile.addressV6.isNullOrBlank()) localAddress.put(profile.addressV6)

        return JSONObject()
            .put("type", "wireguard")
            .put("tag", "warp")
            .put("server", profile.endpointHost)
            .put("server_port", profile.endpointPort)
            .put("local_address", localAddress)
            .put("private_key", profile.privateKey)
            .put("peer_public_key", profile.publicKey)
            .put("mtu", settings.mtu)
            .put("reserved", reservedFromClientId(profile.clientId))
    }

    private fun reservedFromClientId(clientId: String?): JSONArray {
        if (clientId.isNullOrBlank()) return JSONArray().put(0).put(0).put(0)
        return try {
            val bytes = android.util.Base64.decode(clientId, android.util.Base64.DEFAULT)
            JSONArray().put(bytes.getOrElse(0) { 0 }.toInt() and 0xff)
                .put(bytes.getOrElse(1) { 0 }.toInt() and 0xff)
                .put(bytes.getOrElse(2) { 0 }.toInt() and 0xff)
        } catch (_: Exception) {
            JSONArray().put(0).put(0).put(0)
        }
    }
}

fun SettingsState.splitIncludePackages(): List<String> =
    if (splitMode == SplitMode.INCLUDE) splitPackages.toList() else emptyList()

fun SettingsState.splitExcludePackages(): List<String> =
    if (splitMode == SplitMode.EXCLUDE) splitPackages.toList() else emptyList()
