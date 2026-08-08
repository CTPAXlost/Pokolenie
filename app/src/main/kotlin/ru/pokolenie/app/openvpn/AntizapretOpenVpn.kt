package ru.pokolenie.app.openvpn

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * OpenVPN Antizapret (DE): конфиг из assets, IP скрыт в UI.
 * Туннель через OpenVPN for Android / OpenVPN Connect (sing-box не умеет OpenVPN).
 */
object AntizapretOpenVpn {
    const val DISPLAY_NAME = "Antizapret (DE)"
    private const val ASSET = "openvpn/antizapret_de.ovpn"
    private const val HIDDEN_HOST = "antizapret-de.pokolenie.app"

    fun loadDisplaySummary(context: Context): String {
        val raw = readAsset(context)
        val ports = Regex("""remote\s+\S+\s+(\d+)""").findAll(raw).map { it.groupValues[1] }.toList()
        val proto = if (raw.contains("udp", ignoreCase = true)) "UDP" else "TCP"
        return "$DISPLAY_NAME · $HIDDEN_HOST · $proto · ports ${ports.joinToString("/")}"
    }

    fun prepareProfileFile(context: Context): File {
        val raw = readAsset(context)
        val rewritten = raw
            .replace(Regex("""setenv FRIENDLY_NAME ".*""""), """setenv FRIENDLY_NAME "$DISPLAY_NAME"""")
            .let { text ->
                // keep real remotes for connection; add comment for UI tools
                "# Pokolenie display name: $DISPLAY_NAME\n# Endpoint host hidden in app UI\n$text"
            }
        val dir = File(context.cacheDir, "openvpn").apply { mkdirs() }
        val file = File(dir, "antizapret_de.ovpn")
        file.writeText(rewritten)
        return file
    }

    fun launchExternal(context: Context): LaunchResult {
        val file = prepareProfileFile(context)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val candidates = listOf(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/x-openvpn-profile")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/plain")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("net.openvpn.openvpn")
            },
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/x-openvpn-profile")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("de.blinkt.openvpn")
            }
        )
        for (intent in candidates) {
            try {
                context.startActivity(intent)
                return LaunchResult.Ok
            } catch (_: ActivityNotFoundException) {
                // try next
            } catch (_: Exception) {
                // try next
            }
        }
        return LaunchResult.NeedClient
    }

    fun openClientStore(context: Context) {
        val market = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=de.blinkt.openvpn")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=de.blinkt.openvpn")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (_: Exception) {
            context.startActivity(web)
        }
    }

    private fun readAsset(context: Context): String =
        context.assets.open(ASSET).bufferedReader().use { it.readText() }

    enum class LaunchResult { Ok, NeedClient }
}
