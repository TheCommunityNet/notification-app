package wiki.comnet.broadcaster.app.presentation.home

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import wiki.comnet.broadcaster.app.worker.LogSyncWorker
import wiki.comnet.broadcaster.core.common.Result
import wiki.comnet.broadcaster.core.domain.repository.DeviceIdRepository
import wiki.comnet.broadcaster.features.comnet.domain.model.Voucher
import wiki.comnet.broadcaster.features.comnet.domain.repository.ComnetRepository
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val comnetRepository: ComnetRepository,
    private val deviceIdRepository: DeviceIdRepository,
    private val sharedPreferences: SharedPreferences,
) : ViewModel() {

    companion object {
        private const val KEY_REFERRED = "is_referred"
    }

    private val _activeVoucherState = MutableStateFlow<Result<Voucher?>>(Result.Initial)

    val activeVoucherState = _activeVoucherState.asStateFlow()

    private val _redeemVoucherState = MutableStateFlow<Result<Unit>>(Result.Initial)

    val redeemVoucherState = _redeemVoucherState.asStateFlow()

    private val _referState = MutableStateFlow<Result<Unit>>(Result.Initial)
    val referState = _referState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    init {
        viewModelScope.launch {
        }
    }

    fun activeVoucher() {
        viewModelScope.launch {
            comnetRepository.activeVoucher().collect {
                _activeVoucherState.value = it
            }
        }
    }

    fun redeemVoucher(code: String) {
        viewModelScope.launch {
            comnetRepository.redeemVoucher(code).collect {
                when (it) {
                    is Result.Initial -> _redeemVoucherState.value = Result.Initial
                    is Result.Loading -> _redeemVoucherState.value = Result.Loading
                    is Result.Success -> {
                        _redeemVoucherState.value = Result.Success(Unit)
                        _activeVoucherState.value = it
                    }

                    is Result.Error -> {
                        _redeemVoucherState.value = Result.Error(it.exception)
                    }
                }
            }
        }
    }

    fun submitReferCode(username: String) {
        if (username.isBlank()) return
        viewModelScope.launch {
            comnetRepository.referral(username.trim()).collect { result ->
                when (result) {
                    is Result.Initial -> _referState.value = Result.Initial
                    is Result.Loading -> _referState.value = Result.Loading
                    is Result.Success -> {
                        _referState.value = Result.Success(Unit)
                        _toastMessage.emit("ရည်ညွှန်းသူတင်သွင်းပြီးပါပြီ")
                        activeVoucher()
                    }
                    is Result.Error -> _referState.value = Result.Error(result.exception)
                }
            }
        }
    }

    fun triggerLogSync() {
        val syncRequest = OneTimeWorkRequestBuilder<LogSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "LogSyncManual",
            ExistingWorkPolicy.KEEP,
            syncRequest,
        )
        viewModelScope.launch {
            _toastMessage.emit("Syncing logs…")
        }
    }
}