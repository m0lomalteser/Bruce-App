package com.nostudios.bruceapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nostudios.bruceapp.store.StoreViewModel
import com.nostudios.bruceapp.ui.navigation.AppNavigation
import com.nostudios.bruceapp.ui.theme.BruceAppTheme
import com.nostudios.bruceapp.ui.theme.White
import com.nostudios.bruceapp.viewmodel.BruceViewModel

class MainActivity : ComponentActivity() {

    private val requiredPermissions = mutableListOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    ).apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    private val enableBleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestPermissions()

        setContent {
            BruceAppTheme {
                val viewModel: BruceViewModel = viewModel()
                val storeViewModel: StoreViewModel = viewModel()
                val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsState()
                val hasBlePermission = viewModel.hasBlePermission

                if (!hasBlePermission) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "BLE permissions required",
                                color = White,
                                modifier = Modifier.padding(16.dp)
                            )
                            Button(onClick = { requestPermissions() }) {
                                Text("Grant Permissions")
                            }
                        }
                    }
                } else if (!isBluetoothEnabled) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Bluetooth is off",
                                color = White,
                                modifier = Modifier.padding(16.dp)
                            )
                            Button(onClick = { enableBluetooth() }) {
                                Text("Enable Bluetooth")
                            }
                        }
                    }
                } else {
                    AppNavigation(
                        viewModel = viewModel,
                        storeViewModel = storeViewModel
                    )
                }
            }
        }
    }

    private fun requestPermissions() {
        permissionLauncher.launch(requiredPermissions)
    }

    private fun enableBluetooth() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBleLauncher.launch(intent)
    }
}
