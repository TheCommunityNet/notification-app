package wiki.comnet.broadcaster.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import wiki.comnet.broadcaster.core.domain.model.WifiEvent
import wiki.comnet.broadcaster.core.domain.repository.WifiConnectionRepository
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wifiConnectionRepository: WifiConnectionRepository,
) : ViewModel() {

    private val _wifi = MutableStateFlow<Pair<String, String>?>(null)
    val wifi = _wifi.asStateFlow()

    init {
        viewModelScope.launch {
            wifiConnectionRepository.listen().collect {
                if (it is WifiEvent.Connected && it.ssid != null && it.ip != null) {
                    _wifi.value = Pair(it.ssid, it.ip)
                } else {
                    _wifi.value = null
                }
            }
        }
    }
}