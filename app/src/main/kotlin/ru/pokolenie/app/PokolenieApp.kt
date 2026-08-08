package ru.pokolenie.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.pokolenie.app.data.db.AppDatabase
import ru.pokolenie.app.data.repository.SubscriptionRepository
import ru.pokolenie.app.ping.PingHealthService
import ru.pokolenie.app.settings.AppSettings
import ru.pokolenie.app.warp.WarpGenerator

class PokolenieApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var settings: AppSettings
        private set
    lateinit var subscriptions: SubscriptionRepository
        private set
    lateinit var ping: PingHealthService
        private set
    lateinit var warp: WarpGenerator
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.get(this)
        settings = AppSettings(this)
        subscriptions = SubscriptionRepository(database.sourceDao(), database.serverDao())
        ping = PingHealthService(database.serverDao(), database.warpDao())
        warp = WarpGenerator(this, database.warpDao())
        appScope.launch {
            subscriptions.ensureDefaults()
            warp.ensureBundledProfiles()
        }
    }

    companion object {
        lateinit var instance: PokolenieApp
            private set
    }
}
