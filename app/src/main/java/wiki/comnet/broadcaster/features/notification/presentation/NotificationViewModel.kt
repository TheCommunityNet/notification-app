package wiki.comnet.broadcaster.features.notification.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import wiki.comnet.broadcaster.features.notification.domain.model.CachedNotification
import wiki.comnet.broadcaster.features.notification.domain.repository.NotificationRepository
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private var _notifications = MutableStateFlow<List<CachedNotification>>(emptyList())

    val notifications get() = _notifications.asStateFlow()

    fun loadNotifications() {
        val data = notificationRepository.getCachedNotifications()
        _notifications.value = data.reversed()
    }

    fun clearNotifications() {
        notificationRepository.clearCachedNotifications()
        _notifications.value = emptyList()
    }
}