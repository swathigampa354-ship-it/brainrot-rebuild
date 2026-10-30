package com.brainrot.rebuild.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.brainrot.rebuild.workers.WorkSchedule

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED ->
                WorkSchedule.schedule(context)
        }
    }
}
