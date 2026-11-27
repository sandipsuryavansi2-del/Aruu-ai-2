package com.aruu.ai2;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class GeminiClient {

    private GeminiClient() { }

    static String chat(String key, String model, List<ChatMessage> history, String system, boolean search) throws Exception {
        JSONObject body = baseBody(system);
        body.put("contents", buildContents(history));
        if (search) {
            body.put("tools", new JSONArray().put(new JSONObject().put("google_search", new JSONObject())));
        }
        return textOf(generate(key, model, body));
    }

    static String withMedia(String key, String model, String system, String mime, String base64, String prompt) throws Exception {
        JSONObject body = baseBody(system);
        JSONArray parts = new JSONArray()
                .put(new JSONObject().put("inlineData", new JSONObject().put("mimeType", mime).put("data", base64)))
                .put(new JSONObject().put("text", prompt));
        body.put("contents", new JSONArray().put(new JSONObject().put("role", "user").put("parts", parts)));
        return textOf(generate(key, model, body));
    }

    static byte[] generateImage(String key, String model, String prompt) throws Exception {
        JSONObject body = new JSONObject();
        JSONArray parts = new JSONArray().put(new JSONObject().put("text", prompt));
        body.put("contents", new JSONArray().put(new JSONObject().put("role", "user").put("parts", parts)));
        body.put("generationConfig", new JSONObject()
                .put("responseModalities", new JSONArray().put("TEXT").put("IMAGE")));
        JSONArray out = firstParts(generate(key, model, body));
        if (out != null) {
            for (int i = 0; i < out.length(); i++) {
                JSONObject inline = out.getJSONObject(i).optJSONObject("inlineData");
                if (inline != null && inline.optString("mimeType", "").startsWith("image/")) {
                    return Base64.decode(inline.getString("data"), Base64.DEFAULT);
                }
            }
        }
        throw new IOException("Image nahi bani. Settings me image model check karo.");
    }

    static File makeVideo(String key, String model, String prompt, File dir) throws Exception {
        JSONObject body = new JSONObject()
                .put("instances", new JSONArray().put(new JSONObject().put("prompt", prompt)));
        String url = Net.GEMINI_BASE + "models/" + model + ":predictLongRunning";
        JSONObject start = new JSONObject(Net.request(url, "POST", key, body.toString()));
        String op = start.getString("name");
        for (int i = 0; i < 60; i++) {
            Thread.sleep(10000L);
            JSONObject st = new JSONObject(Net.request(Net.GEMINI_BASE + op, "GET", key, null));
            if (!st.optBoolean("done", false)) continue;
            JSONObject err = st.optJSONObject("error");
            if (err != null) throw new IOException(err.optString("message", "Video error"));
            String uri = findUri(st);
            if (uri == null) throw new IOException("Video ka link nahi mila");
            byte[] data = Net.raw(uri, "GET", key, null);
            if (!dir.exists() && !dir.mkdirs()) throw new IOException("Folder nahi bana");
            File out = new File(dir, "video_" + System.currentTimeMillis() + ".mp4");
            try (FileOutputStream fo = new FileOutputStream(out)) {
                fo.write(data);
            }
            return out;
        }
        throw new IOException("Video banne me bahut der lagi, baad me try karo");
    }

    static String textOf(JSONObject resp) throws IOException {
        StringBuilder sb = new StringBuilder();
        JSONArray parts = firstParts(resp);
        if (parts != null) {
            for (int i = 0; i < parts.length(); i++) {
                JSONObject p = parts.optJSONObject(i);
                if (p != null && !p.optBoolean("thought", false)) {
                    sb.append(p.optString("text", ""));
                }
            }
        }
        String out = sb.toString().trim();
        if (out.isEmpty()) throw new IOException("Khaali jawab mila (safety filter ya quota)");
        return out;
    }

    private static JSONObject generate(String key, String model, JSONObject body) throws Exception {
        String url = Net.GEMINI_BASE + "models/" + model + ":generateContent";
        return new JSONObject(Net.request(url, "POST", key, body.toString()));
    }

    private static JSONObject baseBody(String system) throws Exception {
        JSONObject b = new JSONObject();
        b.put("systemInstruction", new JSONObject()
                .put("parts", new JSONArray().put(new JSONObject().put("text", system))));
        b.put("generationConfig", new JSONObject().put("temperature", 0.8).put("maxOutputTokens", 8192));
        return b;
    }

    private static JSONArray firstParts(JSONObject resp) {
        JSONArray cands = resp.optJSONArray("candidates");
        if (cands == null || cands.length() == 0) return null;
        JSONObject cand = cands.optJSONObject(0);
        JSONObject content = cand == null ? null : cand.optJSONObject("content");
        return content == null ? null : content.optJSONArray("parts");
    }

    private static JSONArray buildContents(List<ChatMessage> msgs) throws Exception {
        List<String[]> turns = new ArrayList<>();
        int from = Math.max(0, msgs.size() - 24);
        for (ChatMessage m : msgs.subList(from, msgs.size())) {
            if (m.role == ChatMessage.SYSTEM || m.text.isEmpty()) continue;
            String role = m.role == ChatMessage.USER ? "user" : "model";
            if (!turns.isEmpty() && turns.get(turns.size() - 1)[0].equals(role)) {
                String[] last = turns.get(turns.size() - 1);
                last[1] = last[1] + "\n" + m.text;
            } else {
                turns.add(new String[] {role, m.text});
            }
        }
        while (!turns.isEmpty() && !turns.get(0)[0].equals("user")) {
            turns.remove(0);
        }
        JSONArray arr = new JSONArray();
        for (String[] t : turns) {
            arr.put(new JSONObject().put("role", t[0])
                    .put("parts", new JSONArray().put(new JSONObject().put("text", t[1]))));
        }
        return arr;
    }

    private static String findUri(Object node) {
        if (node instanceof JSONObject) {
            JSONObject o = (JSONObject) node;
            String u = o.optString("uri", "");
            if (u.startsWith("http")) return u;
            Iterator<String> keys = o.keys();
            while (keys.hasNext()) {
                String r = findUri(o.opt(keys.next()));
                if (r != null) return r;
            }
        } else if (node instanceof JSONArray) {
            JSONArray a = (JSONArray) node;
            for (int i = 0; i < a.length(); i++) {
                String r = findUri(a.opt(i));
                if (r != null) return r;
            }
        }
        return null;
    }
}
