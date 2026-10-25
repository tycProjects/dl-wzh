package com.ffproxy.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import com.ffproxy.engine.PacketEngine;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class ProxyVpnService extends VpnService implements Runnable {
    private Thread worker;
    private ParcelFileDescriptor iface;
    private volatile boolean running = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (running) return START_STICKY;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel("ffproxy", "FF Proxy",
                    NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
            startForeground(1, new Notification.Builder(this, "ffproxy")
                    .setContentTitle("FF Proxy")
                    .setContentText("Running")
                    .setSmallIcon(android.R.drawable.ic_lock_lock)
                    .build());
        }
        Builder b = new Builder();
        b.setSession("FFProxy").addAddress("10.8.0.2", 32)
                .addRoute("0.0.0.0", 0).addDnsServer("8.8.8.8")
                .setMtu(1500).setBlocking(true);
        String[] pkgs = {"com.dts.freefireth", "com.dts.freefiremax",
                "com.dts.freefirein", "com.garena.game.freefire"};
        for (String p : pkgs) {
            try { b.addAllowedApplication(p); } catch (Exception ignored) {}
        }
        iface = b.establish();
        if (iface == null) { stopSelf(); return START_NOT_STICKY; }
        running = true;
        worker = new Thread(this, "FF-VPN");
        worker.start();
        return START_STICKY;
    }

    @Override
    public void run() {
        try {
            FileInputStream in = new FileInputStream(iface.getFileDescriptor());
            FileOutputStream out = new FileOutputStream(iface.getFileDescriptor());
            ByteBuffer buf = ByteBuffer.allocate(32767);
            while (running) {
                int n = in.read(buf.array());
                if (n > 0) {
                    buf.limit(n);
                    byte[] r = PacketEngine.process(buf.array(), n);
                    if (r != null) out.write(r);
                    buf.clear();
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        running = false;
        try { if (iface != null) iface.close(); } catch (Exception ignored) {}
        super.onDestroy();
    }
}
