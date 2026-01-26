package wiki.comnet.broadcaster.core.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import wiki.comnet.broadcaster.core.domain.model.WifiEvent

interface WifiConnectionRepository {
    fun listen(): Flow<WifiEvent> = callbackFlow {}
}