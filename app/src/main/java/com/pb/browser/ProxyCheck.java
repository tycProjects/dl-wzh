package com.pb.browser;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import org.json.JSONObject;

// Kiem tra proxy that: di qua proxy toi ip-api.com, lay IP/quoc gia/mui gio/ISP + co bao proxy/hosting, do do tre
final class ProxyCheck {
    interface Callback { void done(JSONObject r, String error); }

    static void run(final String raw, final String ptype, final Callback cb) { run(raw, ptype, 12000, cb); }

    static void run(final String raw, final String ptype, final int timeoutMs, final Callback cb) {
        final Handler ui = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            JSONObject res = null; String err = null;
            try { res = check(raw, ptype, timeoutMs); }
            catch (Exception e) { err = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage()); }
            final JSONObject fr = res; final String fe = err;
            ui.post(() -> cb.done(fr, fe));
        }).start();
    }

    static JSONObject check(String raw, String ptype, int to) throws Exception {
        String scheme = Store.schemeOf(raw, ptype);
        String rest = raw == null ? "" : raw.trim();
        int i = rest.indexOf("://");
        if (i > 0) rest = rest.substring(i + 3);
        String hostPort = rest, user = null, pass = "";
        if (rest.contains("@")) {
            int at = rest.lastIndexOf('@');
            String cred = rest.substring(0, at); hostPort = rest.substring(at + 1);
            int c = cred.indexOf(':');
            user = c < 0 ? cred : cred.substring(0, c);
            pass = c < 0 ? "" : cred.substring(c + 1);
        } else {
            String[] a = rest.split(":");
            if (a.length == 4) { hostPort = a[0] + ":" + a[1]; user = a[2]; pass = a[3]; }
        }
        Proxy proxy = null;
        if (!hostPort.isEmpty()) {
            String[] hp = hostPort.split(":");
            if (hp.length < 2) throw new Exception("Proxy thiếu cổng — nhập dạng host:port");
            Proxy.Type t = "socks5".equals(scheme) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
            proxy = new Proxy(t, new InetSocketAddress(hp[0], Integer.parseInt(hp[1])));
        }
        final String fu = user, fp = pass;
        if (user != null) java.net.Authenticator.setDefault(new java.net.Authenticator() {
            @Override protected java.net.PasswordAuthentication getPasswordAuthentication() {
                if (getRequestorType() != RequestorType.PROXY) return null;
                return new java.net.PasswordAuthentication(fu, fp.toCharArray());
            }
        });
        try { return fetch(proxy, user, pass, "http://ip-api.com/json/?fields=status,message,country,countryCode,timezone,isp,mobile,proxy,hosting,query", false, to); }
        catch (Exception e) {
            if (String.valueOf(e.getMessage()).contains("407")) throw e;
            return fetch(proxy, user, pass, "https://ipwho.is/", true, to);
        }
    }

    static JSONObject fetch(Proxy proxy, String user, String pass, String u, boolean who, int to) throws Exception {
        URL url = new URL(u);
        long t0 = System.currentTimeMillis();
        HttpURLConnection c = (HttpURLConnection) (proxy == null ? url.openConnection() : url.openConnection(proxy));
        c.setConnectTimeout(to);
        c.setReadTimeout(to);
        if (proxy != null && proxy.type() == Proxy.Type.HTTP && user != null) {
            String b = Base64.encodeToString((user + ":" + pass).getBytes("UTF-8"), Base64.NO_WRAP);
            c.setRequestProperty("Proxy-Authorization", "Basic " + b);
        }
        int code = c.getResponseCode();
        long ms = System.currentTimeMillis() - t0;
        if (code == 407) throw new Exception("Proxy yêu cầu đăng nhập (407) — sai user:pass");
        if (code != 200) throw new Exception("HTTP " + code);
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        JSONObject o = new JSONObject(sb.toString());
        if (who) {
            if (!o.optBoolean("success")) throw new Exception(o.optString("message", "Không tra được IP"));
            JSONObject n = new JSONObject();
            n.put("query", o.optString("ip"));
            n.put("country", o.optString("country"));
            n.put("countryCode", o.optString("country_code"));
            JSONObject tzo = o.optJSONObject("timezone");
            n.put("timezone", tzo == null ? "" : tzo.optString("id"));
            JSONObject cn = o.optJSONObject("connection");
            n.put("isp", cn == null ? "" : cn.optString("isp"));
            o = n;
        } else if (!"success".equals(o.optString("status"))) throw new Exception(o.optString("message", "Không tra được IP"));
        o.put("ms", ms);
        return o;
    }
}
