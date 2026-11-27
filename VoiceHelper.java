package com.aruu.ai2;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import java.util.Locale;

final class VoiceHelper {

    private TextToSpeech tts;
    private boolean ready = false;
    private String pendingText;
    private Runnable pendingStart;
    private Runnable pendingDone;
    private Runnable startCb;
    private Runnable doneCb;

    VoiceHelper(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), this::onInit);
    }

    private void onInit(int status) {
        ready = status == TextToSpeech.SUCCESS;
        if (!ready) return;
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String id) {
                fire(startCb);
            }

            @Override
            public void onDone(String id) {
                fire(doneCb);
            }

            @Override
            public void onError(String id) {
                fire(doneCb);
            }
        });
        if (pendingText != null) {
            String t = pendingText;
            pendingText = null;
            speak(t, pendingStart, pendingDone);
        }
    }

    void speak(String text, Runnable onStart, Runnable onDone) {
        if (!ready) {
            pendingText = text;
            pendingStart = onStart;
            pendingDone = onDone;
            return;
        }
        startCb = onStart;
        doneCb = onDone;
        Locale loc = hasDevanagari(text) ? new Locale("hi", "IN") : new Locale("en", "IN");
        int r = tts.setLanguage(loc);
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.US);
        }
        pickFemaleVoice();
        tts.setPitch(1.25f);
        tts.setSpeechRate(1.0f);
        String t = text.length() > 3500 ? text.substring(0, 3500) : text;
        tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "aruu_" + System.nanoTime());
    }

    void stop() {
        if (tts != null) tts.stop();
    }

    void shutdown() {
        if (tts != null) tts.shutdown();
    }

    private void pickFemaleVoice() {
        try {
            for (Voice v : tts.getVoices()) {
                if (v.getName().toLowerCase(Locale.ROOT).contains("female") && !v.isNetworkConnectionRequired()) {
                    tts.setVoice(v);
                    return;
                }
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static void fire(Runnable r) {
        if (r != null) r.run();
    }

    private static boolean hasDevanagari(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '\u0900' && c <= '\u097F') return true;
        }
        return false;
    }
}
