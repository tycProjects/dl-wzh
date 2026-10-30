package com.rayy.ai;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

public class PushMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        String title = "RayyAi";
        String body = null;

        // A "notification" payload (title/body set directly on the
        // message) is normally shown by the OS automatically while the
        // app is backgrounded/closed -- onMessageReceived only fires for
        // it while the app is in the foreground, which is exactly when we
        // still want to show something rather than let it disappear
        // silently.
        if (message.getNotification() != null) {
            if (message.getNotification().getTitle() != null) title = message.getNotification().getTitle();
            body = message.getNotification().getBody();
        } else {
            // A plain "data" message has no built-in title/body -- fall
            // back to whatever keys a backend happens to send.
            Map<String, String> data = message.getData();
            if (data.containsKey("title")) title = data.get("title");
            if (data.containsKey("body")) body = data.get("body");
            else if (data.containsKey("message")) body = data.get("message");
        }
        if (body == null) body = "";

        showNotification(title, body);
    }

    // Firebase assigns a fresh per-install token whenever the app first
    // registers or the old one expires/rotates. A real backend needs this
    // (paired with a call back to it, which is outside the scope of what
    // this wrapper can auto-generate) to know *which* installs to send a
    // given push to. Logged here so it's at least visible via logcat for
    // manual testing/registration in the meantime.
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        android.util.Log.d("PushMessagingService", "FCM token: " + token);
    }

    private void showNotification(String title, String body) {
        String channelId = "default";
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = manager.getNotificationChannel(channelId);
            if (channel == null) {
                channel = new NotificationChannel(channelId, "General", NotificationManager.IMPORTANCE_DEFAULT);
                manager.createNotificationChannel(channel);
            }
        }

        Intent tapIntent = new Intent(this, MainActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, tapIntent, flags);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        manager.notify((int) System.currentTimeMillis(), notification.build());
    }
}
