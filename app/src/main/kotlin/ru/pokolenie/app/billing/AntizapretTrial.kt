package ru.pokolenie.app.billing

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

private val Context.trialStore by preferencesDataStore("antizapret_trial")

data class TrialState(
    val activatedAt: Long = 0L,
    val expiresAt: Long = 0L,
    val unlockToken: String? = null
) {
    val isActive: Boolean
        get() {
            if (!unlockToken.isNullOrBlank()) return true
            if (expiresAt <= 0L) return false
            return System.currentTimeMillis() < expiresAt
        }

    val remainingMs: Long
        get() = when {
            !unlockToken.isNullOrBlank() -> Long.MAX_VALUE / 4
            expiresAt <= 0L -> 0L
            else -> (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
        }

    fun remainingLabel(): String {
        if (!unlockToken.isNullOrBlank()) return "безлимит"
        val ms = remainingMs
        if (ms <= 0L) return "истекло"
        val h = TimeUnit.MILLISECONDS.toHours(ms)
        val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        return "%02d:%02d".format(h, m)
    }
}

class AntizapretTrial(private val context: Context) {
    private val activatedKey = longPreferencesKey("activated_at")
    private val expiresKey = longPreferencesKey("expires_at")
    private val unlockKey = stringPreferencesKey("unlock_token")

    val state: Flow<TrialState> = context.trialStore.data.map { prefs ->
        TrialState(
            activatedAt = prefs[activatedKey] ?: 0L,
            expiresAt = prefs[expiresKey] ?: 0L,
            unlockToken = prefs[unlockKey]
        )
    }

    suspend fun startTrialIfNeeded(): TrialState {
        var result = TrialState()
        context.trialStore.edit { prefs ->
            val unlock = prefs[unlockKey]
            val expires = prefs[expiresKey] ?: 0L
            if (!unlock.isNullOrBlank()) {
                result = TrialState(unlockToken = unlock, expiresAt = expires, activatedAt = prefs[activatedKey] ?: 0L)
                return@edit
            }
            val now = System.currentTimeMillis()
            if (expires > now) {
                result = TrialState(
                    activatedAt = prefs[activatedKey] ?: now,
                    expiresAt = expires
                )
                return@edit
            }
            if (expires == 0L) {
                val end = now + TimeUnit.HOURS.toMillis(24)
                prefs[activatedKey] = now
                prefs[expiresKey] = end
                result = TrialState(activatedAt = now, expiresAt = end)
            } else {
                result = TrialState(activatedAt = prefs[activatedKey] ?: 0L, expiresAt = expires)
            }
        }
        return result
    }

    /** Demo unlock: ключ POKOLENIE-DE-24H или оплата позже. */
    suspend fun unlockWithKey(key: String): Boolean {
        val normalized = key.trim().uppercase()
        if (normalized != "POKOLENIE-DE-24H" && !normalized.startsWith("POKOLENIE-PAY-")) {
            return false
        }
        context.trialStore.edit { prefs ->
            prefs[unlockKey] = normalized
            prefs[expiresKey] = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)
        }
        return true
    }

    suspend fun extendPaidDay() {
        context.trialStore.edit { prefs ->
            val now = System.currentTimeMillis()
            prefs[activatedKey] = now
            prefs[expiresKey] = now + TimeUnit.HOURS.toMillis(24)
            // clear permanent unlock so paid day is explicit
            prefs.remove(unlockKey)
        }
    }
}
