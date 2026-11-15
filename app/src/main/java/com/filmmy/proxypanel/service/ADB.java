package com.adb.connection.proxy;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

public class ADB {
    private static volatile ADB instance = null;

    public static ADB getInstance(Context context) {
        if (instance == null) {
            synchronized (ADB.class) {
                if (instance == null) {
                    instance = new ADB(context);
                }
            }
        }
        return instance;
    }

    private final Context context;
    private final String adbPath;
    
    private final MutableLiveData<Boolean> _started = new MutableLiveData<>(false);
    public final LiveData<Boolean> started = _started;

    public final File outputBufferFile;
    private Process shellProcess = null;

    private ADB(Context context) {
        this.context = context.getApplicationContext();
        this.adbPath = context.getApplicationInfo().nativeLibraryDir + "/libadb.so";

        try {
            this.outputBufferFile = File.createTempFile("adb_buffer", ".txt");
            this.outputBufferFile.deleteOnExit();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Realiza o pareamento direto via ADB Local.
     */
    public boolean pair(String pairingPort, String pairingCode) {
        debug("Iniciando pareamento na porta: " + pairingPort);
        Process pairShell = adb(false, java.util.Arrays.asList("pair", "localhost:" + pairingPort));

        StringBuilder output = new StringBuilder();

        try {
            PrintStream printStream = new PrintStream(pairShell.getOutputStream());
            printStream.println(pairingCode);
            printStream.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(pairShell.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                debug(line);
            }

            pairShell.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            debug("Erro no pareamento: " + e.getMessage());
        } finally {
            pairShell.destroyForcibly();
        }

        String resultText = output.toString().toLowerCase();
        return resultText.contains("success") || resultText.contains("successfully");
    }

    /**
     * Conecta na porta do serviço ADB e abre o Shell.
     */
    public boolean connect(String mainPort) {
        debug("Conectando na porta principal: " + mainPort);
        try {
            adb(false, java.util.Arrays.asList("start-server")).waitFor(2, TimeUnit.SECONDS);

            Process connectProcess = adb(false, java.util.Arrays.asList("connect", "127.0.0.1:" + mainPort));
            connectProcess.waitFor(3, TimeUnit.SECONDS);
            connectProcess.destroyForcibly();

            return openShellProcess();
        } catch (Exception e) {
            e.printStackTrace();
            debug("Erro de conexão: " + e.getMessage());
            return false;
        }
    }

    /**
     * Abre a Shell interativa com o prompt personalizado RickModz.
     */
    public boolean openShellProcess() {
        try {
            shellProcess = adb(true, java.util.Arrays.asList("shell"));
            sendToShellProcess("alias adb=\"" + adbPath + "\"");
            
            // Altera o texto do prompt do terminal visualmente
            sendToShellProcess("export PS1=\"RickModz@android:\\w\\$ \"");
            
            sendToShellProcess("echo 'Shell ADB Pronta!'");

            _started.postValue(true);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            _started.postValue(false);
            return false;
        }
    }

    /**
     * Atalho público para restaurar a sessão no MainActivity.
     */
    public boolean startSavedSession() {
        return openShellProcess();
    }

    /**
     * Envia comandos de texto/terminal para a Shell ativa.
     */
    public void sendToShellProcess(String msg) {
        if (shellProcess == null || shellProcess.getOutputStream() == null) {
            return;
        }
        PrintStream printStream = new PrintStream(shellProcess.getOutputStream());
        printStream.println(msg);
        printStream.flush();
    }

    /**
     * Mata os processos do servidor ADB para desconectar.
     */
    public void disconnect() {
        try {
            if (shellProcess != null) {
                shellProcess.destroyForcibly();
                shellProcess = null;
            }
            adb(false, java.util.Arrays.asList("kill-server")).waitFor(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        _started.postValue(false);
    }

    private Process adb(boolean redirect, java.util.List<String> command) {
        java.util.List<String> commandList = new java.util.ArrayList<>(command);
        commandList.add(0, adbPath);
        return shell(redirect, commandList);
    }

    private Process shell(boolean redirect, java.util.List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .directory(context.getFilesDir());

        if (redirect) {
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(outputBufferFile);
        }

        processBuilder.environment().put("HOME", context.getFilesDir().getPath());
        processBuilder.environment().put("TMPDIR", context.getCacheDir().getPath());

        try {
            return processBuilder.start();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void debug(String msg) {
        synchronized (outputBufferFile) {
            if (outputBufferFile.exists()) {
                try {
                    java.nio.file.Files.write(
                            outputBufferFile.toPath(),
                            ("* " + msg + System.lineSeparator()).getBytes(),
                            java.nio.file.StandardOpenOption.APPEND
                    );
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
