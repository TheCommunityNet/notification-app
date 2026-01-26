package wiki.comnet.broadcaster.features.ble.domain.repository

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import kotlinx.coroutines.flow.StateFlow
import wiki.comnet.broadcaster.features.ble.domain.model.BlePacket

interface BleGattServerRepository {
    val packet: StateFlow<BlePacket?>

    fun start(): Boolean

    fun stop()

    fun getGattServer(): BluetoothGattServer?

    fun getCharacteristic(): BluetoothGattCharacteristic?
}