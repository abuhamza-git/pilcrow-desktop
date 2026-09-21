package com.pilcrowmd.desktop

import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.system.exitProcess
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import kotlinx.coroutines.Dispatchers

object SingleInstance {
    private val ipcFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val openFileRequests = ipcFlow.asSharedFlow()

    private const val MAGIC = "PILCROWMD_IPC_V1"

    fun checkAndStart(args: Array<String>, configDir: File) {
        val portFile = File(configDir, "ipc.port")
        
        if (portFile.exists()) {
            val portStr = portFile.readText().trim()
            val port = portStr.toIntOrNull()
            if (port != null) {
                try {
                    val socket = Socket(InetAddress.getByName("127.0.0.1"), port)
                    val out = PrintWriter(socket.getOutputStream(), true)
                    out.println(MAGIC)
                    if (args.isNotEmpty()) {
                        out.println(args[0])
                    } else {
                        out.println("FOCUS")
                    }
                    socket.close()
                    // Successfully sent to existing instance. Exit this instance.
                    exitProcess(0)
                } catch (e: Exception) {
                    // Failed to connect, previous instance probably crashed.
                    // We will become the first instance.
                }
            }
        }
        
        // Become the first instance
        try {
            val serverSocket = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))
            portFile.writeText(serverSocket.localPort.toString())
            portFile.deleteOnExit()
            
            Thread {
                while (true) {
                    try {
                        val client = serverSocket.accept()
                        val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                        val magic = reader.readLine()
                        if (magic == MAGIC) {
                            val msg = reader.readLine()
                            if (msg != null) {
                                @OptIn(DelicateCoroutinesApi::class)
                                GlobalScope.launch(Dispatchers.Main) {
                                    ipcFlow.emit(msg)
                                }
                            }
                        }
                        client.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.apply {
                isDaemon = true
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
