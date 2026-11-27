package com.aruu.ai2;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class Net {

    static final String GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta/";

    private Net() { }

    static String request(String url, String method, String apiKey, String json) throws IOException {
        return new String(raw(url, method, apiKey, json), StandardCharsets.UTF_8);
    }

    static byte[] raw(String url, String method, String apiKey, String json) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setRequestMethod(method);
            c.setConnectTimeout(30000);
            c.setReadTimeout(180000);
            if (apiKey != null && !apiKey.isEmpty()) {
                c.setRequestProperty("x-goog-api-key", apiKey);
            }
            if (json != null) {
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream os = c.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
            }
            int code = c.getResponseCode();
            InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
            byte[] body = readAll(in);
            if (code >= 400) {
                throw new IOException("HTTP " + code + ": " + errorText(body));
            }
            return body;
        } finally {
            c.disconnect();
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        if (in == null) return new byte[0];
        try (InputStream is = in; ByteArrayOutputStream bo = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                bo.write(buf, 0, n);
            }
            return bo.toByteArray();
        }
    }

    private static String errorText(byte[] body) {
        String s = new String(body, StandardCharsets.UTF_8);
        try {
            return new JSONObject(s).getJSONObject("error").optString("message", s);
        } catch (Exception e) {
            return s.length() > 200 ? s.substring(0, 200) : s;
        }
    }
}
