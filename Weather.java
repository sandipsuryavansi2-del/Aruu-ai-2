package com.aruu.ai2;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;

final class Weather {

    private Weather() { }

    static String forCity(String city) throws Exception {
        String geo = Net.request("https://geocoding-api.open-meteo.com/v1/search?count=1&format=json&name="
                + URLEncoder.encode(city, "UTF-8"), "GET", null, null);
        JSONArray res = new JSONObject(geo).optJSONArray("results");
        if (res == null || res.length() == 0) throw new IOException("Shehar nahi mila: " + city);
        JSONObject place = res.getJSONObject(0);
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + place.getDouble("latitude")
                + "&longitude=" + place.getDouble("longitude")
                + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code"
                + "&timezone=auto";
        JSONObject cur = new JSONObject(Net.request(url, "GET", null, null)).getJSONObject("current");
        return place.optString("name", city) + " me abhi " + Math.round(cur.getDouble("temperature_2m"))
                + "\u00B0C (mehsoos " + Math.round(cur.getDouble("apparent_temperature")) + "\u00B0C), "
                + describe(cur.optInt("weather_code", -1))
                + ", humidity " + cur.optInt("relative_humidity_2m")
                + "%, hawa " + Math.round(cur.getDouble("wind_speed_10m")) + " km/h 🌤";
    }

    private static String describe(int c) {
        if (c == 0) return "saaf aasmaan ☀️";
        if (c <= 3) return "halke baadal 🌤️";
        if (c == 45 || c == 48) return "kohra 🌫️";
        if (c >= 51 && c <= 57) return "halki boonda-baandi 🌦️";
        if (c >= 61 && c <= 67) return "baarish 🌧️";
        if (c >= 71 && c <= 77) return "barf 🌨️";
        if (c >= 80 && c <= 82) return "baarish ki bauchhar 🌦️";
        if (c >= 95) return "garaj ke saath toofan ⛈️";
        return "mausam mixed";
    }
}
