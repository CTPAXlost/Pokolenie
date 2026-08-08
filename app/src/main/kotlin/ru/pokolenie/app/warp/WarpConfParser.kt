package ru.pokolenie.app.warp

import ru.pokolenie.app.data.db.WarpProfileEntity

object WarpConfParser {

    fun parse(name: String, confText: String): WarpProfileEntity {
        val map = linkedMapOf<String, String>()
        confText.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("[") || line.startsWith("#")) return@forEach
            val eq = line.indexOf('=')
            if (eq <= 0) return@forEach
            val key = line.substring(0, eq).trim()
            val value = line.substring(eq + 1).trim()
            map[key] = value
        }

        val privateKey = map["PrivateKey"] ?: error("PrivateKey missing in $name")
        val publicKey = map["PublicKey"] ?: "bmXOC+F1FxEMF9dyiK2H5/1SUtzH0JuVo51h2wPfgyo="
        val address = map["Address"] ?: "172.16.0.2"
        val parts = address.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val v4 = parts.firstOrNull { !it.contains(':') } ?: parts.first()
        val v6 = parts.firstOrNull { it.contains(':') }
        val endpoint = map["Endpoint"] ?: error("Endpoint missing in $name")
        val hostPort = endpoint.split(':')
        val host = hostPort.first()
        val port = hostPort.getOrNull(1)?.toIntOrNull() ?: 2408

        return WarpProfileEntity(
            name = name,
            privateKey = privateKey,
            publicKey = publicKey,
            addressV4 = if (v4.contains('/')) v4 else "$v4/32",
            addressV6 = v6?.let { if (it.contains('/')) it else "$it/128" },
            endpointHost = host,
            endpointPort = port,
            clientId = null,
            confText = confText.trim() + "\n"
        )
    }
}
