package com.greatnice.gts1550control.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Thin wrapper around documented Android Bluetooth APIs only:
 *  - classic discovery (BluetoothAdapter.startDiscovery)
 *  - system pairing dialog (BluetoothDevice.createBond)
 *  - A2DP profile proxy, READ-ONLY connection state
 *  - SDP UUID listing (BluetoothDevice.getUuids / fetchUuidsWithSdp)
 *  - optional READ-ONLY BLE GATT probe (never writes, never subscribes)
 *
 * It never uses reflection or hidden APIs, and never sends proprietary commands.
 */
@SuppressLint("MissingPermission") // every call is guarded by has(...) checks
class BtController(private val ctx: Context) {

    private val adapter: BluetoothAdapter? =
        ctx.getSystemService(BluetoothManager::class.java)?.adapter

    private val _state = MutableStateFlow(BtUiState(adapterPresent = adapter != null))
    val state: StateFlow<BtUiState> = _state.asStateFlow()

    private val main = Handler(Looper.getMainLooper())
    private val found = LinkedHashMap<String, ScanDevice>()
    private var a2dp: BluetoothA2dp? = null
    private var receiverRegistered = false
    private var gatt: BluetoothGatt? = null
    private val readQueue = ArrayDeque<BluetoothGattCharacteristic>()
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    // ---------- permissions ----------
    private fun has(p: String) = ctx.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED
    fun hasConnect() = has(Manifest.permission.BLUETOOTH_CONNECT)
    fun hasScan() = has(Manifest.permission.BLUETOOTH_SCAN)
    fun hasAllPermissions() = hasConnect() && hasScan()

    // ---------- lifecycle ----------
    fun onPermissionsGranted() {
        val a = adapter ?: return
        registerReceiver()
        if (a2dp == null) a.getProfileProxy(ctx, profileListener, BluetoothProfile.A2DP)
        refresh()
    }

    fun refresh() {
        val a = adapter ?: return
        if (!hasConnect()) return
        _state.update { it.copy(adapterEnabled = a.isEnabled) }
        runCatching { a.bondedDevices.orEmpty().forEach { addDevice(it, null) } }
        refreshA2dp()
    }

    fun release() {
        stopGattProbe()
        runCatching { if (hasScan()) adapter?.cancelDiscovery() }
        if (receiverRegistered) {
            runCatching { ctx.unregisterReceiver(receiver) }
            receiverRegistered = false
        }
        a2dp?.let { runCatching { adapter?.closeProfileProxy(BluetoothProfile.A2DP, it) } }
        a2dp = null
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        val f = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_UUID)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        }
        ctx.registerReceiver(receiver, f, Context.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val d = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
                    val rssi = i.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                        .takeIf { it != Short.MIN_VALUE }?.toInt()
                    addDevice(d, rssi)
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED ->
                    _state.update { it.copy(scanning = true, status = "Scanning…") }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED ->
                    _state.update { it.copy(scanning = false, status = "Scan finished") }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    _state.update { it.copy(adapterEnabled = adapter?.isEnabled == true) }
                    refreshA2dp()
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val d = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) ?: return
                    val st = i.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    addDevice(d, null)
                    setStatus("Bond state: ${bondName(st)}")
                    if (d.address == _state.value.selectedAddress) selectAddress(d.address)
                }
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> refreshA2dp()
                BluetoothDevice.ACTION_UUID -> buildReport()
            }
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.A2DP) {
                a2dp = proxy as BluetoothA2dp
                refreshA2dp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) {
                a2dp = null
                refreshA2dp()
            }
        }
    }

    // ---------- discovery ----------
    fun startScan() {
        val a = adapter ?: return setStatus("No Bluetooth adapter")
        if (!hasScan()) return setStatus("Scan permission missing")
        if (!a.isEnabled) return setStatus("Bluetooth is off")
        registerReceiver()
        if (a.isDiscovering) a.cancelDiscovery()
        found.values.removeAll { !it.bonded }
        publishDevices()
        val ok = a.startDiscovery()
        setStatus(if (ok) "Scanning…" else "Android refused to start discovery")
    }

    fun stopScan() {
        if (hasScan()) adapter?.cancelDiscovery()
    }

    private fun addDevice(d: BluetoothDevice, rssi: Int?) {
        if (!hasConnect()) return
        val name = d.name ?: found[d.address]?.name ?: "(unnamed)"
        val up = name.uppercase(Locale.US)
        found[d.address] = ScanDevice(
            address = d.address,
            name = name,
            bonded = d.bondState == BluetoothDevice.BOND_BONDED,
            rssi = rssi ?: found[d.address]?.rssi,
            nameLooksLikeTarget = up.contains("GTS") || up.contains("GREATNICE")
        )
        publishDevices()
    }

    private fun publishDevices() {
        val sorted = found.values.sortedWith(
            compareByDescending<ScanDevice> { it.nameLooksLikeTarget }
                .thenByDescending { it.bonded }
                .thenByDescending { it.rssi ?: -200 }
        )
        _state.update { it.copy(devices = sorted) }
    }

    // ---------- selection / pairing ----------
    fun selectAddress(address: String) {
        val a = adapter ?: return
        if (!hasConnect()) return
        val d = runCatching { a.getRemoteDevice(address) }.getOrNull() ?: return
        _state.update {
            it.copy(
                selectedAddress = address,
                selectedName = d.name ?: found[address]?.name ?: "(unnamed)",
                selectedBonded = d.bondState == BluetoothDevice.BOND_BONDED,
                gattBatteryPercent = if (it.selectedAddress == address) it.gattBatteryPercent else null
            )
        }
        buildReport()
        refreshA2dp()
    }

    /** Uses the standard system pairing dialog. We never bypass or auto-confirm pairing. */
    fun pairSelected() {
        val a = adapter ?: return
        val addr = _state.value.selectedAddress ?: return
        if (!hasConnect()) return
        if (hasScan()) a.cancelDiscovery()
        val d = a.getRemoteDevice(addr)
        if (d.bondState == BluetoothDevice.BOND_BONDED) return setStatus("Already paired")
        val ok = d.createBond()
        setStatus(if (ok) "Pairing requested — confirm in the system dialog" else "Pairing request failed")
    }

    // ---------- A2DP read-only state ----------
    private fun refreshA2dp() {
        val a = adapter ?: return
        if (!hasConnect()) return
        val p = a2dp
        val names = runCatching {
            p?.connectedDevices.orEmpty().map { it.name ?: it.address }
        }.getOrDefault(emptyList())
        val sel = _state.value.selectedAddress
        val st = if (p != null && sel != null) {
            runCatching { p.getConnectionState(a.getRemoteDevice(sel)) }
                .getOrDefault(BluetoothProfile.STATE_DISCONNECTED)
        } else BluetoothProfile.STATE_DISCONNECTED
        _state.update { it.copy(a2dpState = st, a2dpConnectedNames = names) }
    }

    // ---------- diagnostics ----------
    fun fetchUuids() {
        val addr = _state.value.selectedAddress ?: return
        if (!hasConnect()) return
        val ok = adapter?.getRemoteDevice(addr)?.fetchUuidsWithSdp() == true
        log("SDP UUID refresh requested: $ok")
    }

    private fun buildReport() {
        val a = adapter ?: return
        if (!hasConnect()) return
        val addr = _state.value.selectedAddress ?: return
        val d = runCatching { a.getRemoteDevice(addr) }.getOrNull() ?: return
        val uuids = d.uuids.orEmpty().map { describeUuid(it.uuid) }.sorted()
        val cls = d.bluetoothClass?.let {
            "major=0x%04X device=0x%04X audioService=%s".format(
                it.majorDeviceClass, it.deviceClass, it.hasService(BluetoothClass.Service.AUDIO)
            )
        } ?: "unknown"
        val report = DeviceReport(
            name = d.name ?: "(unnamed)",
            address = d.address,
            bondState = bondName(d.bondState),
            transport = when (d.type) {
                BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic (BR/EDR) only"
                BluetoothDevice.DEVICE_TYPE_LE -> "BLE only"
                BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual (Classic + BLE)"
                else -> "Unknown"
            },
            deviceClass = cls,
            uuids = uuids
        )
        _state.update { it.copy(report = report, selectedBonded = d.bondState == BluetoothDevice.BOND_BONDED) }
    }

    /**
     * READ-ONLY BLE GATT probe. Lists services/characteristics and reads only the
     * standard Battery Level and Device Information strings if they exist.
     * It performs NO writes, NO descriptor writes, NO notification subscriptions.
     */
    fun startGattProbe() {
        val a = adapter ?: return
        val addr = _state.value.selectedAddress ?: return setStatus("Select a device first")
        if (!hasConnect()) return
        stopGattProbe()
        val d = a.getRemoteDevice(addr)
        if (d.type == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            log("Note: device reports Classic-only. A BLE probe will probably fail — that is a valid result.")
        }
        log("GATT probe (read-only) → $addr")
        _state.update { it.copy(gattRunning = true) }
        gatt = d.connectGatt(ctx, false, gattCb, BluetoothDevice.TRANSPORT_LE)
        main.postDelayed({
            if (_state.value.gattRunning) {
                log("GATT probe timed out (20 s). Device probably has no connectable BLE interface.")
                stopGattProbe()
            }
        }, 20_000)
    }

    fun stopGattProbe() {
        main.removeCallbacksAndMessages(null)
        readQueue.clear()
        val g = gatt
        gatt = null
        if (g != null) {
            runCatching { g.disconnect() }
            main.postDelayed({ runCatching { g.close() } }, 400)
        }
        _state.update { it.copy(gattRunning = false) }
    }

    private val gattCb = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                log("GATT connected (status=$status). Discovering services…")
                g.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                log("GATT disconnected (status=$status).")
                main.post { stopGattProbe() }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                log("Service discovery failed (status=$status)")
                main.post { stopGattProbe() }
                return
            }
            if (g.services.isEmpty()) log("No GATT services exposed.")
            for (s in g.services) {
                log("SERVICE ${s.uuid}")
                for (c in s.characteristics) {
                    log("   CHAR ${c.uuid} [${props(c.properties)}]")
                    if (isReadableStandard(c)) readQueue.addLast(c)
                }
            }
            readNext(g)
        }

        override fun onCharacteristicRead(
            g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray, status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (c.uuid == sig(0x2A19) && value.isNotEmpty()) {
                    val pct = value[0].toInt() and 0xFF
                    log("Battery Level (standard 0x2A19) = $pct %")
                    _state.update { it.copy(gattBatteryPercent = pct) }
                } else {
                    log("READ ${c.uuid} = \"${String(value, Charsets.UTF_8).trim()}\"")
                }
            } else log("READ ${c.uuid} failed (status=$status)")
            readNext(g)
        }
    }

    private fun readNext(g: BluetoothGatt) {
        val c = readQueue.removeFirstOrNull()
        if (c == null) {
            log("Probe complete. Nothing was written to the device.")
            main.postDelayed({ stopGattProbe() }, 500)
            return
        }
        if (!g.readCharacteristic(c)) {
            log("readCharacteristic(${c.uuid}) rejected")
            readNext(g)
        }
    }

    private fun isReadableStandard(c: BluetoothGattCharacteristic): Boolean {
        val readable = c.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0
        return readable && c.uuid in setOf(sig(0x2A19), sig(0x2A29), sig(0x2A24), sig(0x2A26))
    }

    // ---------- logging / helpers ----------
    fun log(msg: String) {
        val line = "${timeFmt.format(Date())}  $msg"
        _state.update { it.copy(log = (it.log + line).takeLast(300)) }
    }

    fun clearLog() = _state.update { it.copy(log = emptyList()) }

    private fun setStatus(s: String) = _state.update { it.copy(status = s) }

    private fun bondName(s: Int) = when (s) {
        BluetoothDevice.BOND_BONDED -> "Paired"
        BluetoothDevice.BOND_BONDING -> "Pairing…"
        BluetoothDevice.BOND_NONE -> "Not paired"
        else -> "Unknown"
    }

    private fun props(p: Int): String = buildList {
        if (p and BluetoothGattCharacteristic.PROPERTY_READ != 0) add("read")
        if (p and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) add("write")
        if (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) add("write-no-rsp")
        if (p and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) add("notify")
        if (p and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) add("indicate")
    }.joinToString(",").ifEmpty { "-" }

    companion object {
        private const val BASE_SUFFIX = "-0000-1000-8000-00805f9b34fb"

        fun sig(short: Int): UUID = UUID.fromString("0000%04x$BASE_SUFFIX".format(short))

        private val KNOWN = mapOf(
            0x1101 to "Serial Port (SPP)", 0x1108 to "Headset (HSP)", 0x110A to "A2DP Source",
            0x110B to "A2DP Sink", 0x110C to "AVRCP Target", 0x110D to "A2DP",
            0x110E to "AVRCP Controller", 0x110F to "AVRCP", 0x111E to "Handsfree (HFP)",
            0x111F to "Handsfree Audio Gateway", 0x1200 to "PnP Information", 0x1124 to "HID"
        )

        /** Standard Bluetooth SIG UUIDs get a name; anything else is flagged as non-standard. */
        fun describeUuid(u: UUID): String {
            val s = u.toString().lowercase(Locale.US)
            if (s.endsWith(BASE_SUFFIX)) {
                val short = ((u.mostSignificantBits ushr 32) and 0xFFFF).toInt()
                val label = KNOWN[short] ?: "SIG-assigned (not in this app's table)"
                return "0x%04X  %s".format(short, label)
            }
            return "NON-STANDARD  $s   ← candidate vendor service"
        }
    }
}
