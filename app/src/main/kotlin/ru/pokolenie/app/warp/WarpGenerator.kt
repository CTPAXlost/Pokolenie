package ru.pokolenie.app.warp

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.JSONObject
import ru.pokolenie.app.data.db.WarpDao
import ru.pokolenie.app.data.db.WarpProfileEntity
import ru.pokolenie.app.data.remote.HttpClient
import java.security.SecureRandom
import java.security.Security
import java.time.Instant

data class GeneratedWarp(
    val profile: WarpProfileEntity,
    val confText: String
)

class WarpGenerator(
    private val warpDao: WarpDao
) {
    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }
    }

    suspend fun generate(
        name: String = "Pokolenie Warp",
        mtu: Int = 1280,
        amneziaStyle: Boolean = true
    ): GeneratedWarp = withContext(Dispatchers.IO) {
        val keys = generateX25519()
        val install = registerWithCloudflare(keys.publicKeyBase64)
        val conf = buildConf(
            privateKey = keys.privateKeyBase64,
            addressV4 = install.addressV4,
            addressV6 = install.addressV6,
            peerPublicKey = CLOUDFLARE_PUBLIC_KEY,
            endpointHost = install.endpointHost,
            endpointPort = install.endpointPort,
            clientId = install.clientId,
            mtu = mtu,
            amneziaStyle = amneziaStyle
        )
        val entity = WarpProfileEntity(
            name = name,
            privateKey = keys.privateKeyBase64,
            publicKey = CLOUDFLARE_PUBLIC_KEY,
            addressV4 = install.addressV4,
            addressV6 = install.addressV6,
            endpointHost = install.endpointHost,
            endpointPort = install.endpointPort,
            clientId = install.clientId,
            confText = conf
        )
        val id = warpDao.insert(entity)
        GeneratedWarp(entity.copy(id = id), conf)
    }

    suspend fun select(id: Long) = withContext(Dispatchers.IO) {
        warpDao.clearSelection()
        warpDao.select(id)
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        warpDao.delete(id)
    }

    private fun registerWithCloudflare(publicKeyBase64: String): InstallResult {
        val bodyJson = JSONObject()
            .put("key", publicKeyBase64)
            .put("install_id", "")
            .put("fcm_token", "")
            .put("tos", Instant.now().toString())
            .put("type", "Android")
            .put("locale", "en_US")
            .toString()

        val request = Request.Builder()
            .url(WARP_API)
            .header("User-Agent", "okhttp/3.12.1")
            .header("Content-Type", "application/json; charset=UTF-8")
            .post(bodyJson.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .build()

        HttpClient.client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("WARP register failed: HTTP ${response.code} $body")
            }
            val result = JSONObject(body).getJSONObject("result")
            val config = result.getJSONObject("config")
            val peer = config.getJSONArray("peers").getJSONObject(0)
            val endpoint = peer.getJSONObject("endpoint")
            val addresses = config.getJSONObject("interface").getJSONObject("addresses")
            return InstallResult(
                addressV4 = addresses.getString("v4") + "/32",
                addressV6 = addresses.optString("v6").takeIf { it.isNotBlank() }?.let { "$it/128" },
                endpointHost = endpoint.optString("host").ifBlank {
                    endpoint.optString("v4")
                },
                endpointPort = endpoint.optInt("port", 2408),
                clientId = config.optString("client_id").takeIf { it.isNotBlank() }
            )
        }
    }

    private fun buildConf(
        privateKey: String,
        addressV4: String,
        addressV6: String?,
        peerPublicKey: String,
        endpointHost: String,
        endpointPort: Int,
        clientId: String?,
        mtu: Int,
        amneziaStyle: Boolean
    ): String = buildString {
        val address = buildString {
            append(addressV4)
            if (!addressV6.isNullOrBlank()) append(", ").append(addressV6)
        }
        appendLine("[Interface]")
        appendLine("PrivateKey = $privateKey")
        appendLine("Address = $address")
        appendLine("DNS = 1.1.1.1, 1.0.0.1")
        appendLine("MTU = $mtu")
        if (amneziaStyle) {
            appendLine("Jc = 4")
            appendLine("Jmin = 40")
            appendLine("Jmax = 70")
            appendLine("S1 = 0")
            appendLine("S2 = 0")
            appendLine("H1 = 1")
            appendLine("H2 = 2")
            appendLine("H3 = 3")
            appendLine("H4 = 4")
        }
        if (!clientId.isNullOrBlank()) {
            appendLine("# ClientId = $clientId")
        }
        appendLine()
        appendLine("[Peer]")
        appendLine("PublicKey = $peerPublicKey")
        appendLine("AllowedIPs = 0.0.0.0/0, ::/0")
        appendLine("Endpoint = $endpointHost:$endpointPort")
        appendLine("PersistentKeepalive = 25")
    }

    private fun generateX25519(): KeyPairBase64 {
        val privateBytes = ByteArray(32)
        SecureRandom().nextBytes(privateBytes)
        val privateKey = X25519PrivateKeyParameters(privateBytes, 0)
        val publicKey = privateKey.generatePublicKey()
        val publicBytes = ByteArray(32)
        publicKey.encode(publicBytes, 0)
        val clampedPrivate = ByteArray(32)
        privateKey.encode(clampedPrivate, 0)
        return KeyPairBase64(
            privateKeyBase64 = Base64.encodeToString(clampedPrivate, Base64.NO_WRAP),
            publicKeyBase64 = Base64.encodeToString(publicBytes, Base64.NO_WRAP)
        )
    }

    private data class KeyPairBase64(val privateKeyBase64: String, val publicKeyBase64: String)

    private data class InstallResult(
        val addressV4: String,
        val addressV6: String?,
        val endpointHost: String,
        val endpointPort: Int,
        val clientId: String?
    )

    companion object {
        private const val WARP_API = "https://api.cloudflareclient.com/v0a2158/reg"
        private const val CLOUDFLARE_PUBLIC_KEY = "bmXOC+F1FxEMF9dyiK2H5/1SUtzH0JuVo51h2wPfgyo="
    }
}
