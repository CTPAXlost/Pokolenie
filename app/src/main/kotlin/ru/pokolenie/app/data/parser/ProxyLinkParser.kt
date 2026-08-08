package ru.pokolenie.app.data.parser

import android.net.Uri
import android.util.Base64
import ru.pokolenie.app.data.model.ParsedProxy
import ru.pokolenie.app.data.model.ProtocolType
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object ProxyLinkParser {

    fun parseSubscriptionBody(body: String): List<ParsedProxy> {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return emptyList()

        val decoded = decodeMaybeBase64(trimmed)
        val lines = decoded
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toList()

        return lines.mapNotNull { parseLink(it) }
            .distinctBy { it.dedupeKey }
    }

    fun parseLink(link: String): ParsedProxy? {
        val raw = link.trim()
        return when {
            raw.startsWith("vless://", ignoreCase = true) -> parseVless(raw)
            raw.startsWith("trojan://", ignoreCase = true) -> parseTrojan(raw)
            else -> null
        }
    }

    private fun parseVless(raw: String): ParsedProxy? {
        val uri = Uri.parse(raw) ?: return null
        val host = uri.host?.trim().orEmpty()
        val port = if (uri.port > 0) uri.port else 443
        val uuid = uri.userInfo?.let { urlDecode(it) }.orEmpty()
        if (host.isEmpty() || uuid.isEmpty()) return null

        val name = uri.fragment?.let { urlDecode(it) }?.ifBlank { null }
            ?: "$host:$port"
        val security = query(uri, "security")
        val sni = query(uri, "sni") ?: query(uri, "peer")
        val fp = query(uri, "fp") ?: query(uri, "fingerprint")
        val flow = query(uri, "flow")
        val transport = query(uri, "type") ?: query(uri, "network") ?: "tcp"
        val pbk = query(uri, "pbk")
        val sid = query(uri, "sid")
        val path = query(uri, "path")
        val alpn = query(uri, "alpn")

        val dedupe = listOf(
            "vless", uuid, host, port.toString(), sni.orEmpty(),
            pbk.orEmpty(), sid.orEmpty(), transport, flow.orEmpty()
        ).joinToString("|")

        return ParsedProxy(
            name = name,
            protocol = ProtocolType.VLESS,
            host = host,
            port = port,
            uuidOrPassword = uuid,
            rawLink = raw,
            sni = sni,
            fingerprint = fp,
            flow = flow,
            security = security,
            transport = transport,
            publicKey = pbk,
            shortId = sid,
            path = path,
            alpn = alpn,
            dedupeKey = dedupe
        )
    }

    private fun parseTrojan(raw: String): ParsedProxy? {
        val uri = Uri.parse(raw) ?: return null
        val host = uri.host?.trim().orEmpty()
        val port = if (uri.port > 0) uri.port else 443
        val password = uri.userInfo?.let { urlDecode(it) }.orEmpty()
        if (host.isEmpty() || password.isEmpty()) return null

        val name = uri.fragment?.let { urlDecode(it) }?.ifBlank { null }
            ?: "$host:$port"
        val security = query(uri, "security") ?: "tls"
        val sni = query(uri, "sni") ?: query(uri, "peer")
        val fp = query(uri, "fp") ?: query(uri, "fingerprint")
        val transport = query(uri, "type") ?: query(uri, "network") ?: "tcp"
        val path = query(uri, "path")
        val alpn = query(uri, "alpn")

        val dedupe = listOf(
            "trojan", password, host, port.toString(), sni.orEmpty(), transport
        ).joinToString("|")

        return ParsedProxy(
            name = name,
            protocol = ProtocolType.TROJAN,
            host = host,
            port = port,
            uuidOrPassword = password,
            rawLink = raw,
            sni = sni,
            fingerprint = fp,
            security = security,
            transport = transport,
            path = path,
            alpn = alpn,
            dedupeKey = dedupe
        )
    }

    private fun query(uri: Uri, key: String): String? =
        uri.getQueryParameter(key)?.takeIf { it.isNotBlank() }

    private fun urlDecode(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    private fun decodeMaybeBase64(body: String): String {
        val compact = body.replace("\\s".toRegex(), "")
        if (compact.contains("://")) return body
        return try {
            val decoded = Base64.decode(compact, Base64.DEFAULT)
            String(decoded, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            body
        }
    }
}
