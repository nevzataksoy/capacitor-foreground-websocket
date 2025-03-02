package com.example.capacitorforegroundwebsocket

import android.content.Intent
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.PluginMethod

@CapacitorPlugin(name = "CapacitorForegroundWebsocket")
class CapacitorForegroundWebsocketPlugin : Plugin() {

    @PluginMethod
    fun start(call: PluginCall) {
        val ip = call.getString("ip") ?: run {
            call.reject("ip is required")
            return
        }
        val port = call.getInt("port") ?: run {
            call.reject("port is required")
            return
        }
        val isWss = call.getBoolean("isWss") ?: false
        val title = call.getString("title") ?: "Foreground WebSocket Service"
        val description = call.getString("description") ?: "Running..."

        // Start foreground service
        val intent = Intent(context, ForegroundWebSocketService::class.java).apply {
            putExtra("ip", ip)
            putExtra("port", port)
            putExtra("isWss", isWss)
            putExtra("title", title)
            putExtra("description", description)
        }
        context.startForegroundService(intent)
        call.resolve()
    }

    @PluginMethod
    fun stop(call: PluginCall) {
        val intent = Intent(context, ForegroundWebSocketService::class.java)
        context.stopService(intent)
        call.resolve()
    }

    @PluginMethod
    fun send(call: PluginCall) {
        val message = call.getString("message") ?: run {
            call.reject("message is required")
            return
        }
        ForegroundWebSocketService.sendMessage(message)
        call.resolve()
    }
}