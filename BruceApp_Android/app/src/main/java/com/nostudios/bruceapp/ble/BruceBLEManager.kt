package com.nostudios.bruceapp.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class BruceFileItem(
    val name: String,
    val isDirectory: Boolean,
    val size: String? = null
)

enum class ConnectionState {
    Disconnected, Scanning, Connecting, NeedsPin, Authenticating, Paired, Failed
}

private val BRUCE_SERVICE_UUID: UUID = UUID.fromString("B1234567-89AB-CDEF-0123-456789ABCDEF")
private val AUTH_CHAR_UUID: UUID = UUID.fromString("A1234567-89AB-CDEF-0123-456789ABCDEF")
private val HW_INFO_CHAR_UUID: UUID = UUID.fromString("C1234567-89AB-CDEF-0123-456789ABCDEF")
private val BATTERY_CHAR_UUID: UUID = UUID.fromString("D1234567-89AB-CDEF-0123-456789ABCDEF")
private val TERM_TX_CHAR_UUID: UUID = UUID.fromString("E1234567-89AB-CDEF-0123-456789ABCDEF")
private val TERM_RX_CHAR_UUID: UUID = UUID.fromString("F1234567-89AB-CDEF-0123-456789ABCDEF")
private val SCREEN_BLE_CHAR_UUID: UUID = UUID.fromString("99999999-89AB-CDEF-0123-456789ABCDEF")

@SuppressLint("MissingPermission")
class BruceBLEManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    private var bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter.bluetoothLeScanner

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _hardwareName = MutableStateFlow("Unknown Device")
    val hardwareName: StateFlow<String> = _hardwareName.asStateFlow()

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private val _terminalOutput = MutableStateFlow<List<String>>(emptyList())
    val terminalOutput: StateFlow<List<String>> = _terminalOutput.asStateFlow()

    private val _remoteFiles = MutableStateFlow<List<BruceFileItem>>(emptyList())
    val remoteFiles: StateFlow<List<BruceFileItem>> = _remoteFiles.asStateFlow()

    private val _isFileFolderLoading = MutableStateFlow(false)
    val isFileFolderLoading: StateFlow<Boolean> = _isFileFolderLoading.asStateFlow()
    fun setFileFolderLoading(value: Boolean) { _isFileFolderLoading.value = value }

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadingFileName = MutableStateFlow("")
    val downloadingFileName: StateFlow<String> = _downloadingFileName.asStateFlow()

    private val _lastDownloadedFilePath = MutableStateFlow<String?>(null)
    val lastDownloadedFilePath: StateFlow<String?> = _lastDownloadedFilePath.asStateFlow()

    private val _rawScreenData = MutableStateFlow<ByteArray>(byteArrayOf())
    val rawScreenData: StateFlow<ByteArray> = _rawScreenData.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _connectedPeripherals = mutableMapOf<String, BluetoothGatt>()
    val connectedPeripherals: Map<String, BluetoothGatt> get() = _connectedPeripherals

    var activePeripheral: BluetoothGatt? = null
        private set

    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var authCharacteristic: BluetoothGattCharacteristic? = null

    private var isSearchingForNewDevices = false
    var savedDeviceIds: List<String> = emptyList()
    var savedDevicePins: Map<String, String> = emptyMap()

    private var downloadDataBuffer = mutableListOf<Byte>()
    private val scope = CoroutineScope(Dispatchers.Main)

    private fun writeCharacteristic(gatt: BluetoothGatt, char: BluetoothGattCharacteristic, data: ByteArray, writeType: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val result = gatt.writeCharacteristic(char, data, writeType)
                if (result is Int) result == 0 else result as? Boolean ?: false
            } catch (_: Exception) {
                false
            }
        } else {
            @Suppress("DEPRECATION")
            try {
                char.value = data
                @Suppress("DEPRECATION")
                char.writeType = writeType
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(char)
            } catch (_: Exception) {
                false
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (isSearchingForNewDevices &&
                device.address !in savedDeviceIds &&
                _discoveredDevices.value.none { it.address == device.address }
            ) {
                val current = _discoveredDevices.value.toMutableList()
                current.add(device)
                _discoveredDevices.value = current
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectedPeripherals[gatt.device.address] = gatt
                    if (activePeripheral == null) activePeripheral = gatt
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectedPeripherals.remove(gatt.device.address)
                    if (gatt.device.address == activePeripheral?.device?.address) {
                        _connectionState.value = ConnectionState.Disconnected
                        _batteryLevel.value = null
                        _rawScreenData.value = byteArrayOf()
                        _remoteFiles.value = emptyList()
                        activePeripheral = null
                    }
                    gatt.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(java.util.UUID.fromString(BRUCE_SERVICE_UUID.toString()))
                ?: return
            for (char in service.characteristics) {
                when (char.uuid.toString().uppercase()) {
                    AUTH_CHAR_UUID.toString().uppercase() -> {
                        authCharacteristic = char
                        gatt.setCharacteristicNotification(char, true)
                        gatt.readCharacteristic(char)
                    }
                    HW_INFO_CHAR_UUID.toString().uppercase() -> gatt.readCharacteristic(char)
                    TERM_TX_CHAR_UUID.toString().uppercase() -> txCharacteristic = char
                    BATTERY_CHAR_UUID.toString().uppercase() -> {
                        gatt.setCharacteristicNotification(char, true)
                        gatt.readCharacteristic(char)
                    }
                    TERM_RX_CHAR_UUID.toString().uppercase() -> gatt.setCharacteristicNotification(char, true)
                    SCREEN_BLE_CHAR_UUID.toString().uppercase() -> gatt.setCharacteristicNotification(char, true)
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            handleCharacteristicValue(gatt, characteristic, value)
        }

        @Suppress("DEPRECATION")
        @Deprecated("Use new callback for API 33+", ReplaceWith("onCharacteristicRead(gatt, characteristic, characteristic.value, status)"))
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val value = characteristic.value ?: return
            handleCharacteristicValue(gatt, characteristic, value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicValue(gatt, characteristic, value)
        }

        @Suppress("DEPRECATION")
        @Deprecated("Use new callback for API 33+", ReplaceWith("onCharacteristicChanged(gatt, characteristic, characteristic.value)"))
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value ?: return
            handleCharacteristicValue(gatt, characteristic, value)
        }
    }

    private fun handleCharacteristicValue(gatt: BluetoothGatt, char: BluetoothGattCharacteristic, data: ByteArray) {
        val uuid = char.uuid.toString().uppercase()
        when {
            uuid == AUTH_CHAR_UUID.toString().uppercase() -> {
                val statusString = String(data).trim()
                scope.launch {
                    when {
                        statusString == "LOCKED" || statusString == "AUTH_REQUIRED" -> {
                            val autoPin = savedDevicePins[gatt.device.address]
                            if (autoPin != null) {
                                submitPin(autoPin)
                            } else {
                                _connectionState.value = ConnectionState.NeedsPin
                            }
                        }
                        statusString == "SUCCESS" || statusString == "UNLOCKED" -> {
                            _connectionState.value = ConnectionState.Paired
                        }
                        statusString == "WRONG_PIN" || statusString == "FAILED" -> {
                            _connectionState.value = ConnectionState.NeedsPin
                        }
                    }
                }
            }
            uuid == TERM_RX_CHAR_UUID.toString().uppercase() -> {
                val packetString = String(data)
                val lines = packetString.split("\n")
                for (line in lines) {
                    val cleanLine = line.trim()
                    when {
                        cleanLine.startsWith("FILE_ITEM:") -> {
                            val payload = cleanLine.removePrefix("FILE_ITEM:")
                            val parts = payload.split("|")
                            if (parts.size >= 3) {
                                val name = parts[0]
                                val isDir = parts[1] == "1"
                                val bytesSize = parts[2].toIntOrNull() ?: 0
                                val readableSize: String? = if (isDir) null
                                else if (bytesSize >= 1048576) String.format("%.1f MB", bytesSize / 1048576.0)
                                else if (bytesSize >= 1024) "${bytesSize / 1024} KB"
                                else "$bytesSize B"
                                val current = _remoteFiles.value.toMutableList()
                                if (current.none { it.name == name }) {
                                    current.add(BruceFileItem(name, isDir, readableSize))
                                    _remoteFiles.value = current
                                }
                            }
                        }
                        cleanLine == "FS_LIST_DONE" -> _isFileFolderLoading.value = false
                        cleanLine.startsWith("FS_DOWNLOAD_INIT:") -> {
                            val payload = cleanLine.removePrefix("FS_DOWNLOAD_INIT:")
                            val parts = payload.split("|")
                            if (parts.isNotEmpty()) {
                                _downloadingFileName.value = parts[0]
                                downloadDataBuffer.clear()
                                _lastDownloadedFilePath.value = null
                                _isDownloading.value = true
                            }
                        }
                        cleanLine.startsWith("FS_DOWNLOAD_CHUNK:") -> {
                            val hex = cleanLine.removePrefix("FS_DOWNLOAD_CHUNK:")
                            val chunkData = dataFromHexString(hex)
                            if (chunkData != null) {
                                downloadDataBuffer.addAll(chunkData.toList())
                            }
                        }
                        cleanLine == "FS_DOWNLOAD_END" -> {
                            val fileName = _downloadingFileName.value
                            val finalData = downloadDataBuffer.toByteArray()
                            scope.launch {
                                val file = File(context.cacheDir, fileName)
                                file.writeBytes(finalData)
                                _lastDownloadedFilePath.value = file.absolutePath
                                _isDownloading.value = false
                            }
                        }
                        cleanLine.isNotEmpty() -> {
                            val current = _terminalOutput.value.toMutableList()
                            current.add(cleanLine)
                            _terminalOutput.value = current
                        }
                    }
                }
            }
            uuid == SCREEN_BLE_CHAR_UUID.toString().uppercase() -> {
                val current = _rawScreenData.value.toMutableList()
                current.addAll(data.toList())
                if (current.size > 60000) {
                    val trimmed = current.takeLast(10000)
                    _rawScreenData.value = trimmed.toByteArray()
                } else {
                    _rawScreenData.value = current.toByteArray()
                }
            }
            uuid == HW_INFO_CHAR_UUID.toString().uppercase() -> {
                val name = String(data).trim()
                _hardwareName.value = name
            }
            uuid == BATTERY_CHAR_UUID.toString().uppercase() -> {
                if (data.isNotEmpty()) _batteryLevel.value = data[0].toInt() and 0xFF
            }
        }
    }

    fun startExplicitScan() {
        _discoveredDevices.value = emptyList()
        isSearchingForNewDevices = true
        _connectionState.value = ConnectionState.Scanning
        val scanFilters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(BRUCE_SERVICE_UUID)).build()
        )
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        bluetoothLeScanner?.startScan(scanFilters, settings, scanCallback)
    }

    fun stopExplicitScan() {
        isSearchingForNewDevices = false
        bluetoothLeScanner?.stopScan(scanCallback)
        if (_connectionState.value == ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun monitorSavedDevices(ids: List<String>) {
        savedDeviceIds = ids
        for (id in ids) {
            if (id !in _connectedPeripherals) {
                val device = bluetoothAdapter.getRemoteDevice(id)
                val gatt = device.connectGatt(context, false, gattCallback)
                _connectedPeripherals[id] = gatt
                if (activePeripheral == null) activePeripheral = gatt
            }
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        stopExplicitScan()
        _connectionState.value = ConnectionState.Connecting
        val gatt = device.connectGatt(context, false, gattCallback)
        _connectedPeripherals[device.address] = gatt
        activePeripheral = gatt
    }

    fun selectActiveDevice(address: String) {
        val gatt = _connectedPeripherals[address] ?: return
        activePeripheral = gatt
        if (!gatt.discoverServices()) {
            val device = bluetoothAdapter.getRemoteDevice(address)
            val newGatt = device.connectGatt(context, false, gattCallback)
            _connectedPeripherals[address] = newGatt
            activePeripheral = newGatt
        }
    }

    fun disconnectActiveDevice() {
        activePeripheral?.disconnect()
        activePeripheral?.close()
        activePeripheral = null
    }

    fun submitPin(pin: String) {
        val gatt = activePeripheral ?: return
        val char = authCharacteristic ?: return
        _connectionState.value = ConnectionState.Authenticating
        writeCharacteristic(gatt, char, pin.toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
    }

    fun sendCommand(command: String) {
        if (_connectionState.value != ConnectionState.Paired) return
        val gatt = activePeripheral ?: return
        val tx = txCharacteristic ?: return
        writeCharacteristic(gatt, tx, command.toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
        if (!command.contains("BTN_") && !command.contains("SCREEN_") && !command.contains("FS_")) {
            val current = _terminalOutput.value.toMutableList()
            current.add("> $command")
            _terminalOutput.value = current
        }
    }

    fun addToTerminal(text: String) {
        val current = _terminalOutput.value.toMutableList()
        current.add(text)
        _terminalOutput.value = current
    }

    private fun dataFromHexString(hex: String): ByteArray? {
        val result = mutableListOf<Byte>()
        var hexStr = hex.trim()
        while (hexStr.isNotEmpty()) {
            if (hexStr.length < 2) break
            val byteStr = hexStr.take(2)
            hexStr = hexStr.drop(2)
            val b = byteStr.toIntOrNull(16)?.toByte() ?: return null
            result.add(b)
        }
        return result.toByteArray()
    }

    fun removePeripheral(address: String) {
        _connectedPeripherals.remove(address)?.let { gatt ->
            gatt.disconnect()
            gatt.close()
        }
    }

    fun cleanup() {
        stopExplicitScan()
        for ((_, gatt) in _connectedPeripherals) {
            gatt.disconnect()
            gatt.close()
        }
        _connectedPeripherals.clear()
        activePeripheral = null
    }
}
