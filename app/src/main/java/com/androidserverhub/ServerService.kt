package com.androidserverhub

import android.app.*
import android.content.*
import android.os.IBinder
import java.io.*
import java.util.zip.ZipInputStream
import java.util.concurrent.Executors

class ServerService : Service() {
    companion object {
        const val START_JAVA = "START_JAVA"
        const val START_BEDROCK = "START_BEDROCK"
        const val STOP_SERVER = "STOP_SERVER"
        const val RESTART_JAVA = "RESTART_JAVA"
        const val RESTART_BEDROCK = "RESTART_BEDROCK"
        const val COMMAND = "COMMAND"
        const val START_TUNNEL = "START_TUNNEL"
        const val STOP_TUNNEL = "STOP_TUNNEL"
        const val CHANNEL = "server_hub"
    }

    private var process: Process? = null
    private var processType = ""
    private var tunnelProcess: Process? = null
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(10, notification("PocketCraft Host listo"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            START_JAVA, RESTART_JAVA -> {
                stopProcess()
                val port = intent.getIntExtra("port", 25565)
                val xms = intent.getStringExtra("xms") ?: "1G"
                val xmx = intent.getStringExtra("xmx") ?: "2G"
                executor.execute { launchJava(port, xms, xmx) }
            }
            START_BEDROCK, RESTART_BEDROCK -> {
                stopProcess()
                val port = intent.getIntExtra("port", 19132)
                val executable = File(filesDir, "servers/bedrock/bedrock_server")
                executor.execute { launchBedrock(executable, port) }
            }
            STOP_SERVER -> stopProcess()
            COMMAND -> {
                val cmd = intent.getStringExtra("command") ?: return START_STICKY
                sendCommand(cmd)
            }
            START_TUNNEL -> {
                val targetPort = intent.getIntExtra("targetPort", 25565)
                executor.execute { launchTunnel(targetPort) }
            }
            STOP_TUNNEL -> stopTunnel()
        }
        return START_STICKY
    }

    private fun launchJava(port: Int, xms: String, xmx: String) {
        val runtimeZip = File(filesDir, "runtime.zip")
        val runtimeDir = File(filesDir, "java")
        val serverJar = File(filesDir, "servers/java/server.jar")

        if (!serverJar.exists()) {
            broadcast("error", "Falta importar servers/java/server.jar.")
            return
        }
        if (!runtimeZip.exists() && !File(runtimeDir, "bin/java").exists()) {
            broadcast("error", "Falta importar el JRE Android en ZIP.")
            return
        }

        try {
            if (!File(runtimeDir, "bin/java").exists()) {
                runtimeDir.deleteRecursively()
                runtimeDir.mkdirs()
                unzipSafely(runtimeZip, runtimeDir)
            }

            val java = findJavaBinary(runtimeDir)
            if (java == null) {
                broadcast("error", "No encontré un bin/java dentro del JRE.")
                return
            }

            java.setExecutable(true, false)
            val pb = ProcessBuilder(
                java.absolutePath,
                "-Xms$xms",
                "-Xmx$xmx",
                "-Dserver.port=$port",
                "-jar",
                serverJar.absolutePath,
                "nogui"
            )
            pb.directory(serverJar.parentFile)
            pb.redirectErrorStream(true)
            startProcess(pb, "Java")
        } catch (e: Exception) {
            broadcast("error", "No se pudo iniciar Java: ${e.message}")
        }
    }

    private fun launchBedrock(executable: File, port: Int) {
        if (!executable.exists()) {
            broadcast("error", "Falta importar el ejecutable oficial de Bedrock para Android.")
            return
        }
        try {
            executable.setExecutable(true, false)
            val pb = ProcessBuilder(executable.absolutePath)
            pb.directory(executable.parentFile)
            pb.environment()["SERVER_PORT"] = port.toString()
            pb.redirectErrorStream(true)
            startProcess(pb, "Bedrock")
        } catch (e: Exception) {
            broadcast("error", "No se pudo iniciar Bedrock: ${e.message}")
        }
    }

    private fun startProcess(pb: ProcessBuilder, type: String) {
        process = pb.start()
        processType = type
        broadcast("started", "$type iniciado correctamente.")

        Thread {
            process?.inputStream?.bufferedReader()?.forEachLine { line ->
                broadcast("log", line)
            }
            val code = process?.waitFor() ?: -1
            broadcast("stopped", "$type finalizado con código $code.")
            process = null
            processType = ""
        }.start()
    }


    private fun launchTunnel(targetPort: Int) {
        if (tunnelProcess != null) {
            broadcast("tunnel", "El túnel ya está ejecutándose.")
            return
        }

        val tunnelDir = File(filesDir, "tunnel")
        val binary = File(tunnelDir, "tunnel")
        if (!binary.exists()) {
            broadcast("tunnel_error", "Falta el conector de túnel en filesDir/tunnel/tunnel.")
            return
        }

        try {
            binary.setExecutable(true, false)
            val pb = ProcessBuilder(
                binary.absolutePath,
                "--port",
                targetPort.toString()
            )
            pb.directory(tunnelDir)
            pb.redirectErrorStream(true)
            tunnelProcess = pb.start()
            broadcast("tunnel_started", "Conector de túnel iniciado para el puerto $targetPort.")

            Thread {
                tunnelProcess?.inputStream?.bufferedReader()?.forEachLine { line ->
                    broadcast("tunnel_log", line)
                }
                val code = tunnelProcess?.waitFor() ?: -1
                broadcast("tunnel_stopped", "Conector de túnel finalizado con código $code.")
                tunnelProcess = null
            }.start()
        } catch (e: Exception) {
            broadcast("tunnel_error", "No se pudo iniciar el túnel: ${e.message}")
        }
    }

    private fun stopTunnel() {
        try { tunnelProcess?.destroy() } catch (_: Exception) {}
        tunnelProcess = null
        broadcast("tunnel_stopped", "Túnel detenido.")
    }

    private fun sendCommand(command: String) {
        try {
            process?.outputStream?.let {
                it.write((command + "\n").toByteArray())
                it.flush()
            }
        } catch (e: Exception) {
            broadcast("error", "No se pudo enviar el comando: ${e.message}")
        }
    }

    private fun findJavaBinary(root: File): File? {
        val direct = File(root, "bin/java")
        if (direct.exists()) return direct
        return root.walkTopDown().firstOrNull {
            it.isFile && it.name == "java" && it.parentFile?.name == "bin"
        }
    }

    private fun unzipSafely(zip: File, dest: File) {
        ZipInputStream(FileInputStream(zip).buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val target = File(dest, entry.name).canonicalFile
                if (!target.path.startsWith(dest.canonicalPath + File.separator)) {
                    throw IOException("ZIP inválido: ruta fuera del destino")
                }
                if (entry.isDirectory) target.mkdirs()
                else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { out -> zis.copyTo(out) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun stopProcess() {
        try { process?.outputStream?.close() } catch (_: Exception) {}
        try { process?.destroy() } catch (_: Exception) {}
        process = null
        processType = ""
        broadcast("stopped", "Servidor detenido.")
    }

    private fun broadcast(event: String, message: String) {
        sendBroadcast(Intent("com.androidserverhub.EVENT").apply {
            setPackage(packageName)
            putExtra("event", event)
            putExtra("message", message)
        })
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Minecraft Server", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun notification(text: String): Notification =
        Notification.Builder(this, CHANNEL)
            .setContentTitle("PocketCraft Host")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .build()

    override fun onBind(intent: Intent?): IBinder? = null
}
