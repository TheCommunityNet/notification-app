package wiki.comnet.broadcaster.features.ble.domain.repository

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import wiki.comnet.broadcaster.features.ble.domain.model.RoutedPacket

interface BlePacketBroadcasterRepository {
    fun broadcastPacket(
        routed: RoutedPacket,
        gattServer: BluetoothGattServer?,
        characteristic: BluetoothGattCharacteristic?,
    )
}