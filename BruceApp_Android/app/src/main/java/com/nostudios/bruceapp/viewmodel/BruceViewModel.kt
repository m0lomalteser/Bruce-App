package com.nostudios.bruceapp.viewmodel

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nostudios.bruceapp.BruceAppApplication
import com.nostudios.bruceapp.ble.BruceBLEManager
import com.nostudios.bruceapp.ble.ConnectionState
import com.nostudios.bruceapp.data.model.BruceDevice
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BruceViewModel(application: Application) : AndroidViewModel(application) {

    val bleManager = BruceBLEManager(application)

    private val dao = (application as BruceAppApplication).database.bruceDeviceDao()

    val savedDevices: StateFlow<List<BruceDevice>> = dao.getAllDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isBluetoothEnabled: StateFlow<Boolean> = flow {
        val manager = application.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as BluetoothManager
        emit(manager.adapter?.isEnabled == true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hasBlePermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            getApplication(), android.Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED

    init {
        viewModelScope.launch {
            savedDevices.collect { devices ->
                val ids = devices.map { device ->
                    try {
                        BluetoothAdapter.checkBluetoothAddress(device.id)
                        device.id
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }.filterNotNull()
                bleManager.savedDeviceIds = ids
                bleManager.savedDevicePins = devices
                    .filter { it.savedPin != null }
                    .associate { device ->
                        try {
                            BluetoothAdapter.checkBluetoothAddress(device.id)
                            device.id to (device.savedPin ?: "")
                        } catch (_: IllegalArgumentException) {
                            "" to (device.savedPin ?: "")
                        }
                    }
                    .filterKeys { it.isNotEmpty() }
                bleManager.monitorSavedDevices(ids)
            }
        }
    }

    fun saveDevice(device: BluetoothDevice, hardwareName: String, pin: String?) {
        viewModelScope.launch {
            val newDevice = BruceDevice(
                id = device.address,
                name = hardwareName,
                category = "Paired Hardware",
                savedPin = pin
            )
            dao.insertDevice(newDevice)
        }
    }

    fun deleteDevice(device: BruceDevice) {
        viewModelScope.launch {
            bleManager.disconnectActiveDevice()
            bleManager.removePeripheral(device.id)
            dao.deleteDevice(device)
        }
    }

    fun deleteDeviceById(id: String) {
        viewModelScope.launch {
            dao.deleteDeviceById(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.cleanup()
    }
}
