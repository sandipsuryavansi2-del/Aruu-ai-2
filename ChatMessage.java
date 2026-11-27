package com.aruu.ai2;

import org.json.JSONException;
import org.json.JSONObject;

final class ChatMessage {
    static final int USER = 0;
    static final int AI = 1;
    static final int SYSTEM = 2;

    final int role;
    final String text;
    final String imagePath;

    ChatMessage(int role, String text, String imagePath) {
        this.role = role;
        this.text = text == null ? "" : text;
        this.imagePath = imagePath == null ? "" : imagePath;
    }

    JSONObject toJson() throws JSONException {
        return new JSONObject().put("r", role).put("t", text).put("i", imagePath);
    }

    static ChatMessage fromJson(JSONObject o) {
        return new ChatMessage(o.optInt("r", AI), o.optString("t", ""), o.optString("i", ""));
    }
}
