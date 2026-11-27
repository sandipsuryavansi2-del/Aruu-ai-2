package com.aruu.ai2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.MediaStore;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LocalCommands {

    private static final Pattern NAME = Pattern.compile(
            "\\b(?:mera naam|mera nam|my name is|naam mera)\\s+([\\p{L}]{2,20})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPEN_SUFFIX = Pattern.compile(
            "^(.{2,30}?)\\s+(?:khol do|khol de|kholo|khol|open karo|open kar do|open)$");
    private static final Pattern OPEN_PREFIX = Pattern.compile("^open\\s+(.{2,30})$");
    private static final Pattern TIME = Pattern.compile(
            "\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|baje|baj|bje)?\\b");
    private static final List<String> STOP = Arrays.asList(
            "kya", "kaun", "kaisa", "batao", "bata", "pata", "hai", "yaad", "ye", "wo");

    private LocalCommands() { }

    /** Returns a reply if the text was a device command, otherwise null (goes to Gemini). */
    static String run(Activity activity, String text, Prefs prefs) {
        String raw = text.trim();
        String l = raw.toLowerCase(Locale.ROOT);

        Matcher nm = NAME.matcher(raw);
        if (nm.find() && !STOP.contains(nm.group(1).toLowerCase(Locale.ROOT))) {
            String name = nm.group(1);
            name = name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1).toLowerCase(Locale.ROOT);
            prefs.setName(name);
            return "Theek hai " + name + ", main ye yaad rakhungi 💖";
        }
        if (has(l, "naam bhool", "name bhool")) {
            prefs.setName("");
            return "Theek hai, main tumhara naam bhool gayi 🙂";
        }

        boolean off = hasWord(l, "off") || has(l, "band", "bujha");
        boolean on = !off && (hasWord(l, "on") || has(l, "chalu", "jala"));
        if ((on || off) && has(l, "torch", "flash")) {
            return setTorch(activity, on);
        }

        if (has(l, "camera khol", "camera chalu", "camera open", "photo kheen", "photo khich")) {
            return safeStart(activity, new Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                    ? "Camera khol diya 📷" : "Camera app nahi mili.";
        }

        String app = appName(l);
        if (app != null) {
            Intent launch = findLaunchIntent(activity.getPackageManager(), app);
            if (launch != null && safeStart(activity, launch)) {
                return app + " khol diya 📱";
            }
        }

        if (has(l, "gaana bajao", "gana bajao", "gaana chalao", "gana chalao", "gaana sunao", "gaana lagao",
                "song play", "music play")) {
            String q = l.replaceAll("(gaana|gana|song|music)\\s*(bajao|chalao|sunao|lagao|play)", " ")
                    .replaceAll("\\b(karo|kar do|kar de|please|mujhe|ek|ka|ki|ke|do|de)\\b", " ")
                    .replaceAll("\\s+", " ").trim();
            if (q.isEmpty()) q = "latest hindi songs";
            Intent i = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(q)));
            return safeStart(activity, i) ? "YouTube par \"" + q + "\" dhoondh diya 🎵"
                    : "Browser ya YouTube app nahi mili.";
        }

        String digits = l.replaceAll("[^0-9+]", "");
        if (hasWord(l, "call") && digits.length() >= 7) {
            return safeStart(activity, new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + digits)))
                    ? "Dialer me number daal diya 📞" : "Dialer app nahi mili.";
        }
        if (hasWord(l, "sms") && digits.length() >= 7) {
            return safeStart(activity, new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + digits)))
                    ? "SMS app khol di 💬" : "SMS app nahi mili.";
        }

        if (has(l, "alarm", "reminder", "yaad dila")) {
            int[] hm = parseTime(l);
            if (hm != null) {
                Intent i = new Intent(AlarmClock.ACTION_SET_ALARM)
                        .putExtra(AlarmClock.EXTRA_HOUR, hm[0])
                        .putExtra(AlarmClock.EXTRA_MINUTES, hm[1])
                        .putExtra(AlarmClock.EXTRA_MESSAGE, "Aruu AI 2 reminder")
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
                return safeStart(activity, i)
                        ? String.format(Locale.US, "Alarm laga diya %02d:%02d par ⏰", hm[0], hm[1])
                        : "Clock app nahi mili alarm lagane ke liye.";
            }
        }
        return null;
    }

    /** Returns the city for a weather question, "" if city missing, or null if not a weather question. */
    static String weatherCity(String text) {
        String l = text.toLowerCase(Locale.ROOT);
        if (!has(l, "mausam", "weather", "temperature", "taapman")) return null;
        return l.replaceAll("\\b(mausam|weather|temperature|taapman|ka|ki|ke|kaisa|kaisi|kaise|kya|hai|hain|"
                        + "batao|bata|do|de|aaj|abhi|me|mein|in|of|the|today|now|kitna|kitni|haal|ko|bolo|"
                        + "sunao|kaun|sa|si)\\b", " ")
                .replaceAll("[^\\p{L}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    static boolean has(String s, String... words) {
        for (String w : words) {
            if (s.contains(w)) return true;
        }
        return false;
    }

    static boolean hasWord(String s, String word) {
        return Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(s).find();
    }

    private static String setTorch(Context ctx, boolean on) {
        CameraManager cm = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
        if (cm == null) return "Torch service nahi mili.";
        try {
            for (String id : cm.getCameraIdList()) {
                CameraCharacteristics cc = cm.getCameraCharacteristics(id);
                Boolean flash = cc.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (flash != null && flash) {
                    cm.setTorchMode(id, on);
                    return on ? "Torch ON kar di 🔦" : "Torch OFF kar di 🔦";
                }
            }
            return "Is phone me torch available nahi hai.";
        } catch (CameraAccessException | IllegalArgumentException e) {
            return "Torch chalu nahi ho payi. Camera permission check karo.";
        }
    }

    private static boolean safeStart(Activity activity, Intent intent) {
        try {
            activity.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String appName(String l) {
        Matcher m = OPEN_SUFFIX.matcher(l);
        if (m.find()) return m.group(1).trim();
        m = OPEN_PREFIX.matcher(l);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private static Intent findLaunchIntent(PackageManager pm, String query) {
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
        String best = null;
        for (ResolveInfo ri : apps) {
            String label = String.valueOf(ri.loadLabel(pm)).toLowerCase(Locale.ROOT);
            if (label.equals(query)) {
                return pm.getLaunchIntentForPackage(ri.activityInfo.packageName);
            }
            if (best == null && label.contains(query)) {
                best = ri.activityInfo.packageName;
            }
        }
        return best == null ? null : pm.getLaunchIntentForPackage(best);
    }

    private static int[] parseTime(String l) {
        Matcher m = TIME.matcher(l);
        if (!m.find()) return null;
        int h = Integer.parseInt(m.group(1));
        int min = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        String suffix = m.group(3) == null ? "" : m.group(3);
        if (h > 23 || min > 59) return null;
        boolean pm = suffix.equals("pm")
                || (!suffix.equals("am") && has(l, "shaam", "sham", "raat", "dopahar"));
        if (pm && h < 12) h += 12;
        if (suffix.equals("am") && h == 12) h = 0;
        return new int[] {h, min};
    }
}
