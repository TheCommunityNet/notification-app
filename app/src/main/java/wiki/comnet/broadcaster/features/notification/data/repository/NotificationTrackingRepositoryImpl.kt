package wiki.comnet.broadcaster.features.notification.data.repository

import kotlinx.coroutines.Job
import kotlinx.coroutines.supervisorScope
import wiki.comnet.broadcaster.core.common.startRepeatingTask
import wiki.comnet.broadcaster.features.notification.data.mapper.toDomain
import wiki.comnet.broadcaster.features.notification.data.mapper.toEntity
import wiki.comnet.broadcaster.features.notification.data.room.dao.NotificationTrackingDao
import wiki.comnet.broadcaster.features.notification.domain.model.NotificationTracking
import wiki.comnet.broadcaster.features.notification.domain.repository.NotificationTrackingRepository
import javax.inject.Inject

class NotificationTrackingRepositoryImpl @Inject constructor(
    private val notificationTrackingDao: NotificationTrackingDao,
) : NotificationTrackingRepository {
    companion object {
        // 15 minutes
        private const val CACHE_CHECK_INTERVAL = 900000L

        // 10 minutes
        private const val CACHE_EXPIRATION_TIME = 600000L
    }

    private val _cache = mutableMapOf<String, Long>()

    private var job: Job? = null

    override suspend fun start() {
        startCleanUpCacheTask()
    }

    override suspend fun stop() {
        job?.cancel()
    }

    override fun addCacheKey(notificationId: String) {
        _cache[notificationId] = System.currentTimeMillis()
    }

    override suspend fun addTracking(tracking: NotificationTracking) {
        notificationTrackingDao.insert(tracking.toEntity())
    }

    override suspend fun isReceived(notificationId: String): Boolean {
        if (_cache.containsKey(notificationId)) {
            return true
        }
        _cache[notificationId] = System.currentTimeMillis()
        return notificationTrackingDao.getNotificationTrackingByNotificationId(notificationId) != null
    }

    override suspend fun getNotSyncedTracking(): NotificationTracking? {
        return notificationTrackingDao.getUnsyncedNotificationTracking()?.toDomain()
    }

    override suspend fun deleteTracking(tracking: NotificationTracking) {
        notificationTrackingDao.delete(tracking.toEntity())
    }

    override suspend fun deleteOldNotifications(timestamp: Long) {
        notificationTrackingDao.deleteOldNotifications(timestamp)
    }

    private suspend fun startCleanUpCacheTask() = supervisorScope {
        job = startRepeatingTask(CACHE_CHECK_INTERVAL) {
            val currentTime = System.currentTimeMillis()
            val keys = mutableListOf<String>()
            _cache.entries.forEach { (key, value) ->
                if (currentTime - value > CACHE_EXPIRATION_TIME) {
                    keys += key
                }
            }
            keys.forEach {
                _cache.remove(it)
            }
        }
    }
}