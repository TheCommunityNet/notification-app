package wiki.comnet.broadcaster.features.ble.domain.model

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

data class BleDeviceConnection(
    val device: BluetoothDevice,
    val gatt: BluetoothGatt? = null,
    val characteristic: BluetoothGattCharacteristic? = null,
    val isClient: Boolean = false,
)