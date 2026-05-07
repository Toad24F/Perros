package com.example.huellasseguras.Bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateListOf
import com.example.huellasseguras.model.CollarDevice
import java.util.UUID


class BluetoothManager(private val context: Context) {
    private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter

    private val bleScanner = bluetoothAdapter?.bluetoothLeScanner

    // Lista observable para que la pantalla se actualice sola
    val dispositivosEncontrados = mutableStateListOf<CollarDevice>()

    // Callback que se activa cuando encuentra un dispositivo
    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name

            // Filtramos para que solo aparezcan tus collares
            if (name != null && name.contains("Collar", ignoreCase = true)) {
                val alreadyExists = dispositivosEncontrados.any { it.address == device.address }
                if (!alreadyExists) {
                    dispositivosEncontrados.add(CollarDevice(name, device.address))
                }
            }
        }
    }

    @SuppressLint("MissingPermission") // Los permisos los manejamos en la UI
    fun startScanning() {
        dispositivosEncontrados.clear()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bleScanner?.startScan(null, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        bleScanner?.stopScan(scanCallback)
    }
    @SuppressLint("MissingPermission")
    fun conectarYEnviarId(deviceAddress: String, ganadoId: String, onResult: (Boolean) -> Unit) {
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)

        device?.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // Una vez conectados, buscamos los servicios (el buzón)
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    onResult(false)
                    gatt.close()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(SERVICE_UUID)
                    val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)

                    if (characteristic != null) {
                        // Escribimos el petId en la característica
                        characteristic.value = ganadoId.toByteArray()
                        gatt.writeCharacteristic(characteristic)
                    }
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                onResult(status == BluetoothGatt.GATT_SUCCESS)
                gatt.disconnect() // Cerramos la conexión Bluetooth para que el ESP32 empiece a trabajar
            }
        })
    }
}