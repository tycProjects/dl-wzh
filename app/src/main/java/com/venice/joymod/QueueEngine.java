package com.venice.joymod;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

public final class QueueEngine {

    public static final String PKG = "com.joyark.cloudgames.community";

    public interface Log { void log(String s); }

    private static volatile boolean running = false;

    public static boolean isRunning() { return running; }

    private static void post(final Log log, final String s) {
        if (log == null) return;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() { log.log(s); }
        });
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public static void run(final Context ctx, final Log log, final Runnable finished) {
        if (running) {
            post(log, "Ja esta rodando.");
            return;
        }
        running = true;

        final Context app = ctx.getApplicationContext();

        new Thread(new Runnable() {
            public void run() {
                try {
                    int mode = Prefs.mode(app);
                    int attempts = Prefs.attempts(app);
                    int delay = Prefs.delay(app);
                    String target = Prefs.target(app);

                    ScreenReader sr = ScreenReader.get();
                    if (sr == null) {
                        post(log, "Servico de acessibilidade desligado.");
                        return;
                    }

                    sr.refresh();
                    sleep(250);

                    String snap = sr.getText();
                    String queue = Sniff.queueLine(snap);
                    String game = Sniff.gameTitle(snap);

                    if (game != null && game.length() > 0) {
                        Prefs.setTarget(app, game);
                        post(log, "Alvo travado: " + game);
                    }
                    if (Prefs.cloudPc(app)) post(log, "Modo Cloud PC ativo.");
                    if (queue != null) post(log, "Fila lida: " + queue);
                    else post(log, "Sem fila legivel, vou tentar mesmo assim.");

                    for (int i = 1; i <= attempts; i++) {
                        post(log, "Tentativa " + i + " de " + attempts);
                        step(app, log, mode, target);
                        sleep(delay + 1000);

                        sr.refresh();
                        sleep(400);
                        String now = sr.getText();

                        if (Sniff.inStream(now)) {
                            post(log, "Dentro do jogo. Fila pulada.");
                            break;
                        }
                        if (i < attempts) post(log, "Ainda na fila, repetindo...");
                    }
                } catch (Throwable t) {
                    post(log, "Erro: " + t.getMessage());
                } finally {
                    running = false;
                    if (finished != null) new Handler(Looper.getMainLooper()).post(finished);
                }
            }
        }, "queue-engine").start();
    }

    private static void step(Context ctx, Log log, int mode, String target) {
        ScreenReader sr = ScreenReader.get();
        if (sr != null) sr.goHome();
        sleep(600);

        if (mode >= 1) {
            try {
                ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) am.killBackgroundProcesses(PKG);
                post(log, "Sessao de fila descartada.");
            } catch (Throwable t) {
                post(log, "Kill falhou, seguindo.");
            }
            sleep(400);
        }

        launch(ctx, log, mode, target);
    }

    private static void launch(Context ctx, Log log, int mode, String target) {
        if (mode == 2 && target != null && target.startsWith("http")) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(target));
                i.setPackage(PKG);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                ctx.startActivity(i);
                post(log, "Deep link disparado.");
                return;
            } catch (Throwable t) {
                post(log, "Deep link falhou, usando launcher.");
            }
        }

        Intent l = ctx.getPackageManager().getLaunchIntentForPackage(PKG);
        if (l == null) {
            post(log, "JoyArk nao encontrado.");
            return;
        }
        l.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            ctx.startActivity(l);
            post(log, "JoyArk relancado.");
        } catch (Throwable t) {
            post(log, "Nao consegui relancar: " + t.getMessage());
        }
    }
}
