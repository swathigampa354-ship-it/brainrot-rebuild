package com.brainrot.rebuild.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.brainrot.rebuild.workers.WorkSchedule

class DateChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WorkSchedule.schedule(context)
    }
}
