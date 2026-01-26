package wiki.comnet.broadcaster.features.ble.data.repository

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.shreyaspatil.permissionFlow.PermissionFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class BluetoothPermissionManagerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothManager: BluetoothManager,
) {


    val permissions by lazy {
        val permissions = mutableListOf<String>()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissions.addAll(
                listOf(
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        } else {
            permissions.addAll(
                listOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            )
        }

        permissions.addAll(
            listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        permissions.toTypedArray()
    }

    fun observeBluetoothReady(): Flow<Boolean> = flow {
        val permissionFlow = PermissionFlow.getInstance()
        permissionFlow.getMultiplePermissionState(*permissions).combine(
            bluetoothEnableStateFlow()
        ) { permissionState, bluetoothEnabled ->
            permissionState.allGranted && bluetoothEnabled
        }.collect {
            emit(it)
        }
    }

    fun bluetoothEnableStateFlow(): Flow<Boolean> = callbackFlow {
        if (bluetoothManager.adapter == null) {
            trySend(false)
            awaitClose { } // Close immediately since there's nothing to clean up
            return@callbackFlow
        }

        val bluetoothStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )
                    trySend(state == BluetoothAdapter.STATE_ON)
                }
            }
        }

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, filter)

        awaitClose {
            context.unregisterReceiver(bluetoothStateReceiver)
        }
    }

    fun hasBluetoothPermissions(): Boolean {
        return permissions.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}