package wiki.comnet.broadcaster.app.presentation.home

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import wiki.comnet.broadcaster.core.domain.model.WifiEvent
import wiki.comnet.broadcaster.core.domain.repository.DeviceIdRepository
import wiki.comnet.broadcaster.core.domain.repository.WifiConnectionRepository
import wiki.comnet.broadcaster.features.refer.data.model.ReferRequest
import wiki.comnet.broadcaster.features.refer.data.network.CommunityNetApi
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wifiConnectionRepository: WifiConnectionRepository,
    private val communityNetApi: CommunityNetApi,
    private val deviceIdRepository: DeviceIdRepository,
    private val sharedPreferences: SharedPreferences,
) : ViewModel() {

    companion object {
        private const val KEY_REFERRED = "is_referred"
    }

    private val _wifi = MutableStateFlow<Pair<String, String>?>(null)
    val wifi = _wifi.asStateFlow()

    private val _referState = MutableStateFlow<ReferState>(ReferState.Idle)
    val referState = _referState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    val isReferred: Boolean
        get() = sharedPreferences.getBoolean(KEY_REFERRED, false)

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

    fun submitReferCode(referCode: String) {
        if (referCode.isBlank()) return
        viewModelScope.launch {
            _referState.value = ReferState.Loading
            try {
                val response = communityNetApi.refer(
                    ReferRequest(
                        referCode = referCode,
                        deviceId = deviceIdRepository.getDeviceId(),
                    )
                )
                if (response.success) {
                    sharedPreferences.edit { putBoolean(KEY_REFERRED, true) }
                    _referState.value = ReferState.Success
                    _toastMessage.emit(response.message ?: "Refer code submitted successfully")
                } else {
                    _referState.value = ReferState.Error(response.message ?: "Failed to submit refer code")
                    _toastMessage.emit(response.message ?: "Failed to submit refer code")
                }
            } catch (e: Exception) {
                _referState.value = ReferState.Error(e.message ?: "Something went wrong")
                _toastMessage.emit(e.message ?: "Something went wrong")
            }
        }
    }
}

sealed class ReferState {
    data object Idle : ReferState()
    data object Loading : ReferState()
    data object Success : ReferState()
    data class Error(val message: String) : ReferState()
}