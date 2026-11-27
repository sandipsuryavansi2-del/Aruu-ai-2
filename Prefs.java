package com.aruu.ai2;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

final class Prefs {

    static final String DEFAULT_TEXT_MODEL = "gemini-2.5-flash";
    static final String DEFAULT_IMAGE_MODEL = "gemini-2.5-flash-image";
    static final String DEFAULT_VIDEO_MODEL = "gemini-omni-flash";

    private static final String FILE = "aruu_ai2_prefs";
    private static final String K_KEY = "gemini_key";
    private static final String K_TEXT = "model_text";
    private static final String K_IMAGE = "model_image";
    private static final String K_VIDEO = "model_video";
    private static final String K_NAME = "user_name";
    private static final String K_CHAT = "chat_now";
    private static final String K_ARCHIVE = "chat_archive";
    private static final int MAX_CHAT = 150;
    private static final int MAX_ARCHIVE = 30;

    private final SharedPreferences sp;

    Prefs(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    String getApiKey() { return sp.getString(K_KEY, ""); }
    boolean hasApiKey() { return !getApiKey().isEmpty(); }
    void setApiKey(String key) { sp.edit().putString(K_KEY, key.trim()).apply(); }

    String getTextModel() { return modelOr(K_TEXT, DEFAULT_TEXT_MODEL); }
    String getImageModel() { return modelOr(K_IMAGE, DEFAULT_IMAGE_MODEL); }
    String getVideoModel() { return modelOr(K_VIDEO, DEFAULT_VIDEO_MODEL); }
    void setTextModel(String m) { putModel(K_TEXT, m, DEFAULT_TEXT_MODEL); }
    void setImageModel(String m) { putModel(K_IMAGE, m, DEFAULT_IMAGE_MODEL); }
    void setVideoModel(String m) { putModel(K_VIDEO, m, DEFAULT_VIDEO_MODEL); }

    String getName() { return sp.getString(K_NAME, ""); }
    void setName(String name) { sp.edit().putString(K_NAME, name == null ? "" : name.trim()).apply(); }

    void saveChat(List<ChatMessage> list) {
        sp.edit().putString(K_CHAT, toJsonArray(list).toString()).apply();
    }

    List<ChatMessage> loadChat() {
        return fromJsonArray(sp.getString(K_CHAT, "[]"));
    }

    List<List<ChatMessage>> getArchives() {
        List<List<ChatMessage>> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(sp.getString(K_ARCHIVE, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                out.add(fromJsonArray(arr.getString(i)));
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    void setArchives(List<List<ChatMessage>> sessions) {
        JSONArray all = new JSONArray();
        for (int i = 0; i < sessions.size() && i < MAX_ARCHIVE; i++) {
            all.put(toJsonArray(sessions.get(i)).toString());
        }
        sp.edit().putString(K_ARCHIVE, all.toString()).apply();
    }

    private String modelOr(String key, String def) {
        String v = sp.getString(key, "");
        return v.isEmpty() ? def : v;
    }

    private void putModel(String key, String value, String def) {
        String v = value == null ? "" : value.trim();
        sp.edit().putString(key, v.isEmpty() ? def : v).apply();
    }

    private static JSONArray toJsonArray(List<ChatMessage> list) {
        JSONArray arr = new JSONArray();
        int from = Math.max(0, list.size() - MAX_CHAT);
        for (int i = from; i < list.size(); i++) {
            try {
                arr.put(list.get(i).toJson());
            } catch (JSONException ignored) {
            }
        }
        return arr;
    }

    private static List<ChatMessage> fromJsonArray(String raw) {
        List<ChatMessage> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                out.add(ChatMessage.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
        return out;
    }
}
