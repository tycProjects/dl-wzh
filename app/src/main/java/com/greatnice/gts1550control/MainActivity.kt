package com.greatnice.gts1550control

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.greatnice.gts1550control.ui.AppRoot
import com.greatnice.gts1550control.ui.GtsTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GtsTheme { AppRoot(vm) } }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshAudio()
        if (vm.bt.hasAllPermissions()) vm.bt.onPermissionsGranted()
    }

    override fun onStop() {
        super.onStop()
        // Never leave discovery or a GATT probe running in the background.
        if (vm.bt.hasScan()) vm.bt.stopScan()
        vm.bt.stopGattProbe()
    }
}
