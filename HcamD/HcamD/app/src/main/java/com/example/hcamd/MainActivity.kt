package com.example.hcamd

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var viewCameraThreat: FrameLayout
    private lateinit var viewNetworkTelemetry: FrameLayout
    private lateinit var tvCamRssi: TextView
    private lateinit var tvCamTemp: TextView
    private lateinit var redThreatBanner: LinearLayout
    private lateinit var redTargetBox: LinearLayout
    private lateinit var rightSidePanels: LinearLayout
    private lateinit var tacticalCrosshair: TacticalCrosshairView
    private lateinit var progressRssi: ProgressBar
    private lateinit var progressTemp: ProgressBar
    private lateinit var tvNetRssi: TextView
    private lateinit var tvNetTemp: TextView
    private lateinit var tvNetRssiBadge: TextView
    private lateinit var tvNetTempBadge: TextView
    private lateinit var gaugeRssi: TacticalGaugeView
    private lateinit var gaugeTemp: TacticalGaugeView
    private lateinit var btnNavCamera: FrameLayout
    private lateinit var btnNavNetwork: FrameLayout

    private var cameraControl: CameraControl? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private var detectedThreatRssi = -100
    private var currentTemp = 25.0
    private var isCritical = false

    // Ensure this matches the MAC from your ESP32 Serial Monitor exactly!
    private val MAC_ADDRESS = "1C:C3:AB:B2:E2:36"
    private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind UI
        viewFinder = findViewById(R.id.viewFinder)
        viewCameraThreat = findViewById(R.id.viewCameraThreat)
        viewNetworkTelemetry = findViewById(R.id.viewNetworkTelemetry)
        tvCamRssi = findViewById(R.id.tvCamRssi)
        tvCamTemp = findViewById(R.id.tvCamTemp)
        redThreatBanner = findViewById(R.id.redThreatBanner)
        redTargetBox = findViewById(R.id.redTargetBox)
        rightSidePanels = findViewById(R.id.rightSidePanels)
        tacticalCrosshair = findViewById(R.id.tacticalCrosshair)
        progressRssi = findViewById(R.id.progressRssi)
        progressTemp = findViewById(R.id.progressTemp)
        tvNetRssi = findViewById(R.id.tvNetRssi)
        tvNetTemp = findViewById(R.id.tvNetTemp)
        tvNetRssiBadge = findViewById(R.id.tvNetRssiBadge)
        tvNetTempBadge = findViewById(R.id.tvNetTempBadge)
        gaugeRssi = findViewById(R.id.gaugeRssi)
        gaugeTemp = findViewById(R.id.gaugeTemp)
        btnNavCamera = findViewById(R.id.btnNavCamera)
        btnNavNetwork = findViewById(R.id.btnNavNetwork)

        setupGauges()
        btnNavCamera.setOnClickListener { switchTab(true) }
        btnNavNetwork.setOnClickListener { switchTab(false) }

        setupPermissions()
    }

    private fun setupGauges() {
        gaugeRssi.minValue = -100f
        gaugeRssi.maxValue = -20f
        gaugeRssi.isDangerMode = true
        gaugeTemp.minValue = 20f
        gaugeTemp.maxValue = 50f
    }

    private fun switchTab(isCamera: Boolean) {
        viewCameraThreat.visibility = if (isCamera) View.VISIBLE else View.GONE
        viewNetworkTelemetry.visibility = if (isCamera) View.GONE else View.VISIBLE
        btnNavCamera.setBackgroundColor(if (isCamera) Color.CYAN else Color.TRANSPARENT)
        btnNavNetwork.setBackgroundColor(if (isCamera) Color.TRANSPARENT else Color.CYAN)
    }

    private fun setupPermissions() {
        val perms = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        ActivityCompat.requestPermissions(this, perms.toTypedArray(), 100)
        startCamera()
        startBleSystem()
    }

    @SuppressLint("MissingPermission")
    private fun startBleSystem() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter

        if (adapter == null || !adapter.isEnabled) {
            Toast.makeText(this, "Please Turn On Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        Log.d("Argus", "Forcing connection to: $MAC_ADDRESS")
        try {
            val device = adapter.getRemoteDevice(MAC_ADDRESS)
            // Using TRANSPORT_LE is critical for modern Android devices to avoid hanging
            bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: Exception) {
            Log.e("Argus", "Error getting remote device: ${e.message}")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("Argus", "Connected. Discovering services...")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("Argus", "Disconnected. Attempting reconnect...")
                // Optional: trigger a small delay then call startBleSystem() again
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(SERVICE_UUID)
            val characteristic = service?.getCharacteristic(CHAR_UUID)
            if (characteristic != null) {
                gatt.setCharacteristicNotification(characteristic, true)
                val desc = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                if (desc != null) {
                    desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(desc)
                    Log.d("Argus", "Notifications active.")
                }
            } else {
                Log.e("Argus", "Service or Characteristic not found!")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val value = String(characteristic.value).trim()
            Log.d("ArgusData", "Raw Value: $value")

            if (value != "nan") {
                detectedThreatRssi = value.toIntOrNull() ?: -100
                processThreatLogic()
                updateUI()
            }
        }
    }

    private fun processThreatLogic() {
        // Danger logic: Anything stronger than -45 dBm is considered close
        isCritical = detectedThreatRssi >= -45

        if (isCritical) {
            val diff = kotlin.math.abs(detectedThreatRssi - (-45))
            currentTemp = (35.0 + (diff * 0.4)).coerceAtMost(49.0)
        } else {
            currentTemp = 26.0
        }

        runOnUiThread {
            tacticalCrosshair.isDangerMode = isCritical
            redThreatBanner.visibility = if (isCritical) View.VISIBLE else View.GONE
            redTargetBox.visibility = if (isCritical) View.VISIBLE else View.GONE
            rightSidePanels.visibility = if (isCritical) View.VISIBLE else View.GONE
            cameraControl?.setZoomRatio(if (isCritical) 1.5f else 1.0f)
        }
    }

    private fun updateUI() {
        runOnUiThread {
            tvCamRssi.text = "$detectedThreatRssi dBm"
            tvCamTemp.text = "${String.format("%.1f", currentTemp)} °C"
            tvNetRssi.text = tvCamRssi.text
            tvNetTemp.text = tvCamTemp.text

            progressRssi.progress = (100 + detectedThreatRssi).coerceIn(0, 100)
            progressTemp.progress = (((currentTemp - 20) / 30.0) * 100).toInt().coerceIn(0, 100)

            gaugeRssi.currentValue = detectedThreatRssi.toFloat()
            gaugeTemp.currentValue = currentTemp.toFloat()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(viewFinder.surfaceProvider) }
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                cameraControl = camera.cameraControl
            } catch (e: Exception) {
                Log.e("Argus", "Camera error: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.close()
    }
}