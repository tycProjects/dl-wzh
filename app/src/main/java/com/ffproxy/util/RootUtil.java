package com.ffproxy.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class RootUtil {
    public static boolean isRoot() {
        String r = su("id");
        return r != null && r.contains("uid=0");
    }

    public static String su(String cmd) {
        StringBuilder sb = new StringBuilder();
        Process p = null;
        try {
            p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            String line;
            while ((line = in.readLine()) != null) sb.append(line).append('\n');
            while ((line = err.readLine()) != null) sb.append(line).append('\n');
            p.waitFor();
        } catch (Exception e) {
            sb.append("ERR:").append(e.getMessage());
        } finally {
            if (p != null) p.destroy();
        }
        return sb.toString().trim();
    }
}
