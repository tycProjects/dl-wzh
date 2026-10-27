package com.localaide.local_aide

import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import id.or.oo.pr.engine.ProotLauncher
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

/**
 * Runs the Linux builder inside this APK through the embedded proot-engine.
 * No Termux package or external terminal app is used.
 */
class MainActivity : FlutterActivity() {
    private val channelName = "local_aide/builder"
    private val workspace = File("/storage/emulated/0/LocalAIDE")
    private val projects = File(workspace, "projects")
    private val logs = File(workspace, "logs")
    private val output = File(workspace, "builds")

    private val app: LocalAideApplication
        get() = application as LocalAideApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workspace.mkdirs()
        projects.mkdirs()
        logs.mkdirs()
        output.mkdirs()
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
            .setMethodCallHandler { call, result ->
                if (call.method != "runBuilder") {
                    result.notImplemented()
                    return@setMethodCallHandler
                }

                val action = call.argument<String>("action") ?: ""
                val name = call.argument<String>("projectName") ?: ""
                if (action !in setOf("setupToolchain", "createProject", "buildDebug")) {
                    result.error("BAD_ACTION", "Unsupported local build action", null)
                    return@setMethodCallHandler
                }
                if (!name.matches(Regex("^[a-z][a-z0-9_]{0,39}$"))) {
                    result.error("BAD_NAME", "Project name is invalid", null)
                    return@setMethodCallHandler
                }
                if (!workspace.exists() || !logs.exists() || !projects.exists() || !output.exists()) {
                    result.error("WORKSPACE", "Local workspace is not accessible. Grant file access and retry.", null)
                    return@setMethodCallHandler
                }

                val logFile = File(logs, "${action}_$name.log")
                logFile.parentFile?.mkdirs()
                logFile.writeText("Local AIDE embedded builder\nAction: $action\nPreparing...\n")
                Thread({
                    var exitCode = 1
                    try {
                        appendLog(logFile, "Checking embedded runtime...\n")
                        exitCode = when (action) {
                            "setupToolchain" -> setupToolchain(logFile)
                            "createProject" -> createProject(name, logFile)
                            "buildDebug" -> buildDebug(name, logFile)
                            else -> 2
                        }
                    } catch (e: Throwable) {
                        appendLog(logFile, "ERROR: ${e.javaClass.simpleName}: ${e.message ?: "unknown error"}\n")
                        exitCode = 1
                    } finally {
                        appendLog(logFile, "\nLOCAL_AIDE_EXIT_CODE=$exitCode\n")
                    }
                }, "local-aide-$action").start()

                result.success("Started embedded on-device command. Log: ${logFile.absolutePath}")
            }
    }

    private fun setupToolchain(logFile: File): Int {
        if (!prCli().isFile) {
            appendLog(logFile, "ERROR: embedded pr-cli is missing from this APK. Rebuild with the no-Termux engine setup script.\n")
            return 20
        }
        if (!ubuntuInstalled()) {
            appendLog(logFile, "Installing Ubuntu into Local AIDE private storage...\n")
            val installCode = runCli(
                listOf("install", "ubuntu", "--override-alias", "ubuntu"),
                logFile,
            )
            if (installCode != 0) return installCode
        } else {
            appendLog(logFile, "Ubuntu already installed; keeping existing local files.\n")
        }

        val installScript = """
            set -e
            export DEBIAN_FRONTEND=noninteractive
            command -v curl >/dev/null 2>&1 || (apt-get update && apt-get install -y curl ca-certificates)
            curl -fL --retry 3 --retry-delay 2 \
              https://raw.githubusercontent.com/0xR0/arm64-flutter-setup/master/install.sh \
              -o /tmp/local-aide-arm64-flutter-setup.sh
            bash /tmp/local-aide-arm64-flutter-setup.sh
            export ANDROID_SDK_ROOT=/root/.local/android-sdk
            yes | "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_SDK_ROOT" --licenses >/dev/null 2>&1 || true
            "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_SDK_ROOT" "ndk;27.2.12479018" "cmake;3.22.1"
            echo 'Local AIDE toolchain setup complete.'
        """.trimIndent()
        appendLog(logFile, "Downloading Flutter, Java 17, Android SDK and ARM64 build-tools inside the app sandbox...\n")
        return runUbuntuShell(installScript, logFile)
    }

    private fun createProject(name: String, logFile: File): Int {
        if (!ubuntuInstalled()) {
            appendLog(logFile, "ERROR: Install the local toolchain first.\n")
            return 21
        }
        val destination = "/workspace/projects/$name"
        val script = """
            set -e
            export FLUTTER_ROOT=/root/.local/flutter-3.27.4
            export ANDROID_SDK_ROOT=/root/.local/android-sdk
            export ANDROID_HOME="${'$'}ANDROID_SDK_ROOT"
            export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
            export PATH="${'$'}FLUTTER_ROOT/bin:${'$'}ANDROID_SDK_ROOT/cmdline-tools/latest/bin:${'$'}ANDROID_SDK_ROOT/platform-tools:${'$'}PATH"
            if [ ! -x "${'$'}FLUTTER_ROOT/bin/flutter" ]; then echo 'Flutter is not installed. Tap Install toolchain first.'; exit 31; fi
            if [ -e '$destination' ]; then echo 'Project already exists: $destination'; exit 32; fi
            mkdir -p /root/local-aide/projects /workspace/projects
            if [ -e '/workspace/projects/$name' ]; then echo 'Project already exists in the shared workspace: $name'; exit 32; fi
            cd /root/local-aide/projects
            flutter create --platforms=android --org com.localaide --project-name '$name' '$name'
            rm -rf '$destination/.dart_tool' '$destination/build'
            cp -a '$name' '/workspace/projects/$name'
            rm -rf '/workspace/projects/$name/.dart_tool' '/workspace/projects/$name/build'
        """.trimIndent()
        return runUbuntuShell(script, logFile)
    }

    private fun buildDebug(name: String, logFile: File): Int {
        if (!ubuntuInstalled()) {
            appendLog(logFile, "ERROR: Install the local toolchain first.\n")
            return 21
        }
        val project = "/workspace/projects/$name"
        val script = """
            set -e
            export FLUTTER_ROOT=/root/.local/flutter-3.27.4
            export ANDROID_SDK_ROOT=/root/.local/android-sdk
            export ANDROID_HOME="${'$'}ANDROID_SDK_ROOT"
            export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
            export PATH="${'$'}FLUTTER_ROOT/bin:${'$'}ANDROID_SDK_ROOT/cmdline-tools/latest/bin:${'$'}ANDROID_SDK_ROOT/platform-tools:${'$'}PATH"
            if [ ! -x "${'$'}FLUTTER_ROOT/bin/flutter" ]; then echo 'Flutter is not installed. Tap Install toolchain first.'; exit 31; fi
            mkdir -p /root/local-aide/projects
            rm -rf '/root/local-aide/projects/$name'
            mkdir -p '/root/local-aide/projects/$name'
            cp -a '$project/.' '/root/local-aide/projects/$name/'
            rm -rf '/root/local-aide/projects/$name/build' '/root/local-aide/projects/$name/.dart_tool'
            chmod +x '/root/local-aide/projects/$name/android/gradlew'
            cd '/root/local-aide/projects/$name'
            flutter pub get
            flutter build apk --debug --target-platform android-arm64
            mkdir -p /workspace/builds
            cp build/app/outputs/flutter-apk/app-debug.apk '/workspace/builds/$name-debug.apk'
            echo 'Local APK written to /workspace/builds/$name-debug.apk'
        """.trimIndent()
        return runUbuntuShell(script, logFile)
    }

    private fun runUbuntuShell(script: String, logFile: File): Int {
        val args = listOf(
            "login", "ubuntu", "--user", "root",
            "--custom-bind", "${workspace.absolutePath}:/workspace",
            "--", "bash", "-lc", script,
        )
        return runCli(args, logFile)
    }

    private fun runCli(args: List<String>, logFile: File): Int {
        val cli = prCli()
        if (!cli.isFile) throw IllegalStateException("Embedded pr-cli was not bootstrapped: ${cli.absolutePath}")
        val fullArgs = listOf(cli.absolutePath) + args
        appendLog(logFile, "\n$ ${fullArgs.drop(1).joinToString(" ")}\n")
        val session = ProotLauncher(app).startCustomSession(fullArgs, rows = 45, cols = 140)
            ?: throw IllegalStateException("Could not create a PRoot session. Check the ARM64 engine libraries.")

        val buffer = ByteArray(8192)
        val pending = StringBuilder()
        try {
            while (true) {
                val count = session.read(buffer)
                if (count <= 0) break
                pending.append(String(buffer, 0, count, StandardCharsets.UTF_8))
                while (true) {
                    val newline = pending.indexOf("\n")
                    if (newline < 0) break
                    val line = pending.substring(0, newline).replace("\r", "")
                    pending.delete(0, newline + 1)
                    if (line.isNotBlank()) appendLog(logFile, "$line\n")
                }
            }
            if (pending.isNotEmpty()) appendLog(logFile, pending.toString().replace("\r", "") + "\n")
        } finally {
            try { session.close() } catch (_: Exception) {}
        }

        repeat(200) {
            session.exitCode?.let { return it }
            try { Thread.sleep(25) } catch (_: InterruptedException) { Thread.currentThread().interrupt(); return 130 }
        }
        appendLog(logFile, "WARNING: PRoot session ended before an exit code was reported.\n")
        return session.exitCode ?: 1
    }

    private fun prCli(): File = File(app.prefixDir, "bin/pr-cli")

    private fun ubuntuInstalled(): Boolean {
        val legacy = File(app.prefixDir, "var/lib/pr/installed-rootfs/ubuntu")
        val oci = File(app.prefixDir, "var/lib/pr/containers/ubuntu/rootfs")
        return legacy.isDirectory || oci.isDirectory
    }

    private fun appendLog(file: File, value: String) {
        file.parentFile?.mkdirs()
        FileOutputStream(file, true).bufferedWriter(Charsets.UTF_8).use { it.append(value) }
    }
}
