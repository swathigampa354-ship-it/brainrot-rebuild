package com.brainrot.rebuild.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.brainrot.rebuild.app.RebuildApp
import com.brainrot.rebuild.app.Thresholds
import com.brainrot.rebuild.services.FallbackUsageStatsService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var thresholdsFlow: StateFlow<Thresholds>
    private var tab by mutableStateOf(0)
    private var current by mutableStateOf<Thresholds?>(null)
    private var perms by mutableStateOf(PermissionState(false, true, false))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as RebuildApp
        thresholdsFlow = app.thresholds.flow.stateIn(
            lifecycleScope, SharingStarted.Lazily, Thresholds()
        )
        lifecycleScope.launchWhenStarted {
            thresholdsFlow.collect { current = it }
        }
        perms = checkPermissions(this)
        setContent {
            RebuildTheme {
                if (!perms.a11y) {
                    PermissionGate(state = perms, onRecheck = { perms = checkPermissions(this) })
                } else {
                    AppScaffold(
                        thresholds = current ?: Thresholds(),
                        setThresholds = { f -> modify(f) },
                        tab = tab,
                        onTab = { tab = it },
                        requestFallback = { FallbackUsageStatsService.ensure(this) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        perms = checkPermissions(this)
    }

    private fun modify(f: suspend RebuildApp.() -> Unit) {
        lifecycleScope.launch {
            val app = application as RebuildApp
            f.invoke(app)
        }
    }
}
