package com.productivityos.app.data.local

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class AppUsageService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: implement usage stats tracking logic
        return START_STICKY
    }

    companion object {
        fun start(context: Context) {
            context.startService(Intent(context, AppUsageService::class.java))
        }
    }
}