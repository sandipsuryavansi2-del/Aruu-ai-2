package com.aruu.ai2;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private interface Job {
        ChatMessage run() throws Exception;
    }

    private static final String AUTHORITY = "com.aruu.ai2.fileprovider";

    private Prefs prefs;
    private VoiceHelper voice;
    private SpeechRecognizer recognizer;
    private ObjectAnimator talkAnim;
    private boolean listening = false;
    private boolean voiceOn = false;
    private String pendingPdfQuestion = "";
    private String pendingPhotoQuestion = "";

    private final List<ChatMessage> messages = new ArrayList<>();
    private final ExecutorService bg = Executors.newCachedThreadPool();
    private final Handler ui = new Handler(Looper.getMainLooper());

    private ChatAdapter adapter;
    private RecyclerView rvChat;
    private EditText etMessage;
    private TextView tvStatus;
    private TextView btnSpeaker;
    private ImageView imgAvatar;
    private Button btnMic;
    private LinearLayout chipRow;

    private ActivityResultLauncher<String> pickImage;
    private ActivityResultLauncher<String> pickPdf;
    private ActivityResultLauncher<String> requestMic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        if (!prefs.hasApiKey()) {
            startActivity(new Intent(this, ApiKeyActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        imgAvatar = findViewById(R.id.imgAvatar);
        tvStatus = findViewById(R.id.tvStatus);
        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        btnMic = findViewById(R.id.btnMic);
        btnSpeaker = findViewById(R.id.btnSpeaker);
        chipRow = findViewById(R.id.chipRow);

        adapter = new ChatAdapter(this, messages);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvChat.setLayoutManager(lm);
        rvChat.setAdapter(adapter);

        voice = new VoiceHelper(this);

        pickImage = registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImagePicked);
        pickPdf = registerForActivityResult(new ActivityResultContracts.GetContent(), this::onPdfPicked);
        requestMic = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                startListening();
            } else {
                toast("Mic permission ke bina voice nahi chalegi");
            }
        });

        findViewById(R.id.btnSend).setOnClickListener(v -> sendText(etMessage.getText().toString(), false));
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendText(etMessage.getText().toString(), false);
                return true;
            }
            return false;
        });
        btnMic.setOnClickListener(v -> toggleMic());
        findViewById(R.id.btnTerminate).setOnClickListener(v -> terminate());
        findViewById(R.id.btnPhoto).setOnClickListener(v -> {
            pendingPhotoQuestion = etMessage.getText().toString().trim();
            etMessage.setText("");
            pickImage.launch("image/*");
        });
        findViewById(R.id.btnPremium).setOnClickListener(v ->
                startActivity(new Intent(this, PremiumActivity.class)));
        findViewById(R.id.btnHistory).setOnClickListener(v -> showHistory());
        findViewById(R.id.btnSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        btnSpeaker.setOnClickListener(v -> toggleSpeaker());

        buildChips();
        updateSpeakerLabel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter == null || !prefs.hasApiKey()) return;
        messages.clear();
        messages.addAll(prefs.loadChat());
        if (messages.isEmpty()) {
            messages.add(new ChatMessage(ChatMessage.AI, greeting(), ""));
        }
        adapter.notifyDataSetChanged();
        rvChat.scrollToPosition(Math.max(0, messages.size() - 1));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adapter != null) prefs.saveChat(messages);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voice != null) voice.shutdown();
        if (recognizer != null) recognizer.destroy();
        bg.shutdownNow();
    }

    private String greeting() {
        String name = prefs.getName();
        String hello = name.isEmpty() ? "Namaste! " : "Namaste " + name + "! ";
        return hello + getString(R.string.greeting_text);
    }

    private void buildChips() {
        addChip("🎨 Image Banao", () -> setTemplate("Image banao: "));
        addChip("🎬 Video Banao", () -> setTemplate("Video banao: "));
        addChip("💻 Code Likho", () -> setTemplate("Code likho: "));
        addChip("📚 Homework", () -> setTemplate("Ye homework solve karo: "));
        addChip("🌍 Translate", () -> setTemplate("Translate karo: "));
        addChip("✉️ Email / Letter", () -> setTemplate("Email likh do: "));
        addChip("🔎 Live Search", () -> setTemplate("Live search karo: "));
        addChip("🎭 Story / Shayari", () -> setTemplate("Ek story sunao: "));
        addChip("⏰ Alarm", () -> setTemplate("7 baje ka alarm lagao"));
        addChip("🎵 Gaana", () -> setTemplate("Gaana bajao: "));
        addChip("🌤 Mausam", () -> setTemplate("Mausam batao: "));
        addChip("📄 PDF Padho / Sawal", this::openPdfPicker);
        addChip("📄 PDF Banao", () -> sendText("PDF banao", false));
    }

    private void addChip(String label, Runnable action) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextColor(0xFFFFFFFF);
        chip.setTextSize(13f);
        chip.setBackgroundResource(R.drawable.bg_pill);
        chip.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(lp);
        chip.setOnClickListener(v -> action.run());
        chipRow.addView(chip);
    }

    private void setTemplate(String t) {
        etMessage.setText(t);
        etMessage.setSelection(t.length());
        etMessage.requestFocus();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void setStatus(String s) {
        tvStatus.setText(s == null ? "" : s);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private void addMessage(ChatMessage m) {
        messages.add(m);
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);
        prefs.saveChat(messages);
    }

    private void sendText(String raw, boolean fromVoice) {
        final String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) return;
        etMessage.setText("");
        addMessage(new ChatMessage(ChatMessage.USER, text, ""));
        final boolean withVoice = fromVoice || voiceOn;
        String lower = text.toLowerCase(Locale.ROOT);

        if (LocalCommands.has(lower, "terminate", "voice band karo", "chup ho jao")) {
            terminate();
            return;
        }
        if (LocalCommands.has(lower, "pdf banao", "pdf bana do", "pdf bana de")) {
            makePdfFromLastAnswer(withVoice);
            return;
        }
        if (LocalCommands.has(lower, "image banao", "photo banao", "tasveer banao", "tasvir banao",
                "picture banao", "image bana do", "photo bana do")) {
            generateImage(text, withVoice);
            return;
        }
        if (LocalCommands.has(lower, "video banao", "video bana do", "video bana de")) {
            generateVideo(text, withVoice);
            return;
        }
        String city = LocalCommands.weatherCity(text);
        if (city != null) {
            if (city.isEmpty()) {
                addMessage(new ChatMessage(ChatMessage.AI,
                        "Kis shehar ka mausam chahiye? Shehar ka naam likho 🌤", ""));
            } else {
                fetchWeather(city, withVoice);
            }
            return;
        }
        String local = LocalCommands.run(this, text, prefs);
        if (local != null) {
            addMessage(new ChatMessage(ChatMessage.AI, local, ""));
            if (withVoice) speak(local);
            return;
        }
        askGemini(text, withVoice);
    }

    private void runJob(final String busyText, final Job job, final boolean withVoice,
                        final boolean offlineOk, final String userText) {
        setStatus(busyText);
        bg.execute(() -> {
            ChatMessage result;
            try {
                result = job.run();
            } catch (Exception e) {
                String why = explain(e);
                String text;
                if (why == null && offlineOk) {
                    text = OfflineReplies.answer(userText, prefs.getName());
                } else if (why != null) {
                    text = why;
                } else {
                    text = "Kuch gadbad ho gayi: " + e.getMessage();
                }
                result = new ChatMessage(ChatMessage.AI, text, "");
            }
            final ChatMessage out = result;
            ui.post(() -> {
                setStatus(null);
                addMessage(out);
                if (withVoice && !out.text.isEmpty()) speak(out.text);
            });
        });
    }

    private void askGemini(final String text, final boolean withVoice) {
        final List<ChatMessage> snapshot = new ArrayList<>(messages);
        final String key = prefs.getApiKey();
        final String model = prefs.getTextModel();
        final String system = systemPrompt();
        final boolean search = needsSearch(text.toLowerCase(Locale.ROOT));
        runJob(getString(R.string.status_thinking),
                () -> new ChatMessage(ChatMessage.AI,
                        GeminiClient.chat(key, model, snapshot, system, search), ""),
                withVoice, true, text);
    }

    private String systemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tumhara naam Aruu hai. Tum ek pyaari, caring aur friendly AI assistant ho, awaaz ladki wali. ");
        sb.append("User jis language me likhe (Hindi, English ya Hinglish) usi me jawab do. ");
        sb.append("Hindi me feminine forms use karo (main karungi, rakhungi). ");
        sb.append("ChatGPT jaise detail me, sahi aur saaf jawab do. ");
        sb.append("Code maange to poora working code code block me do. Homework step-by-step samjhao. ");
        sb.append("Email ya letter professional format me likho. Translate maange to saaf translation do. ");
        sb.append("Agar user poochhe ki tum AI ho ya nahi, to saaf bolo ki tum AI assistant ho. ");
        sb.append("Aaj ki date: ")
                .append(new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).format(new Date()))
                .append(". ");
        String name = prefs.getName();
        if (!name.isEmpty()) {
            sb.append("User ka naam ").append(name).append(" hai, use naturally yaad rakho. ");
        }
        return sb.toString();
    }

    private static boolean needsSearch(String l) {
        return LocalCommands.has(l, "live", "latest", "news", "khabar", "aaj ka", "abhi ka",
                "search", "taaza", "today", "current");
    }

    private String explain(Exception e) {
        String m = String.valueOf(e.getMessage());
        if (m.contains("HTTP 400") || m.contains("HTTP 403") || m.contains("API key")) {
            return "Gemini key me dikkat lag rahi hai. Settings me jaake key check karo 🔑";
        }
        if (m.contains("HTTP 404")) {
            return "Ye model naam nahi mil raha. Settings me model naam check karo.";
        }
        if (m.contains("HTTP 429")) {
            return "Free limit abhi khatam ho gayi hai, thodi der baad try karo ⏳";
        }
        return null;
    }

    private void onImagePicked(final Uri uri) {
        if (uri == null) return;
        final String question = pendingPhotoQuestion.isEmpty()
                ? "Is photo me kya hai? Detail me batao, Hindi ya English me."
                : pendingPhotoQuestion;
        pendingPhotoQuestion = "";
        final String key = prefs.getApiKey();
        final String model = prefs.getTextModel();
        final String system = systemPrompt();
        runJob("Photo dekh rahi hoon… 👀", () -> {
            Bitmap bmp = MediaUtil.decodeScaled(getContentResolver(), uri, 1024);
            final byte[] jpeg = MediaUtil.jpegBytes(bmp);
            final File saved = MediaUtil.saveFile(new File(getFilesDir(), "images"),
                    "photo_" + System.currentTimeMillis() + ".jpg", jpeg);
            ui.post(() -> addMessage(new ChatMessage(ChatMessage.USER, question, saved.getAbsolutePath())));
            String answer = GeminiClient.withMedia(key, model, system, "image/jpeg",
                    MediaUtil.base64(jpeg), question);
            return new ChatMessage(ChatMessage.AI, answer, "");
        }, voiceOn, false, question);
    }

    private void onPdfPicked(final Uri uri) {
        if (uri == null) return;
        final String question = pendingPdfQuestion.isEmpty()
                ? "Is PDF ka saaf summary do: main points aur important baatein Hindi ya English me."
                : pendingPdfQuestion;
        pendingPdfQuestion = "";
        addMessage(new ChatMessage(ChatMessage.USER, "📄 " + question, ""));
        final String key = prefs.getApiKey();
        final String model = prefs.getTextModel();
        final String system = systemPrompt();
        runJob("PDF padh rahi hoon… 📄", () -> {
            byte[] data = MediaUtil.readBytes(getContentResolver(), uri, 18L * 1024 * 1024);
            String answer = GeminiClient.withMedia(key, model, system, "application/pdf",
                    MediaUtil.base64(data), question);
            return new ChatMessage(ChatMessage.AI, answer, "");
        }, voiceOn, false, question);
    }

    private void openPdfPicker() {
        pendingPdfQuestion = etMessage.getText().toString().trim();
        etMessage.setText("");
        pickPdf.launch("application/pdf");
    }

    private void generateImage(final String prompt, final boolean withVoice) {
        final String key = prefs.getApiKey();
        final String model = prefs.getImageModel();
        runJob("Image ban rahi hai… 🎨", () -> {
            byte[] png = GeminiClient.generateImage(key, model, prompt);
            File f = MediaUtil.saveFile(new File(getFilesDir(), "images"),
                    "gen_" + System.currentTimeMillis() + ".png", png);
            return new ChatMessage(ChatMessage.AI, "Ye raha aapka image 🎨", f.getAbsolutePath());
        }, withVoice, false, prompt);
    }

    private void generateVideo(final String prompt, final boolean withVoice) {
        final String key = prefs.getApiKey();
        final String model = prefs.getVideoModel();
        runJob("Video ban rahi hai… 1 se 3 minute lag sakte hain 🎬", () -> {
            final File f = GeminiClient.makeVideo(key, model, prompt, new File(getFilesDir(), "videos"));
            ui.post(() -> openFile(f, "video/mp4"));
            return new ChatMessage(ChatMessage.AI, "Video ready ✅ (model: " + model + ")", "");
        }, withVoice, false, prompt);
    }

    private void makePdfFromLastAnswer(final boolean withVoice) {
        String last = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage m = messages.get(i);
            if (m.role == ChatMessage.AI && !m.text.isEmpty()) {
                last = m.text;
                break;
            }
        }
        if (last.isEmpty()) {
            addMessage(new ChatMessage(ChatMessage.AI,
                    "Pehle koi jawab aane do, phir main PDF bana dungi 📄", ""));
            return;
        }
        final String content = last;
        runJob("PDF ban rahi hai… 📄", () -> {
            final File pdf = PdfMaker.create(MainActivity.this, "Aruu AI 2 - Chat PDF", content);
            ui.post(() -> openFile(pdf, "application/pdf"));
            return new ChatMessage(ChatMessage.AI,
                    "PDF ban gayi ✅ Abhi khul rahi hai. Share bhi kar sakti ho.", "");
        }, withVoice, false, content);
    }

    private void fetchWeather(final String city, final boolean withVoice) {
        runJob("Mausam dekh rahi hoon… 🌤",
                () -> new ChatMessage(ChatMessage.AI, Weather.forCity(city), ""),
                withVoice, false, city);
    }

    private void speak(String text) {
        String clean = text.contains("```")
                ? "Code screen par dekho."
                : text.replaceAll("[*#_`>]+", " ").replaceAll("\\s+", " ").trim();
        if (clean.isEmpty()) return;
        voice.speak(clean, () -> ui.post(this::startTalk), () -> ui.post(this::stopTalk));
    }

    private void startTalk() {
        if (talkAnim != null) return;
        talkAnim = ObjectAnimator.ofPropertyValuesHolder(imgAvatar,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.05f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.05f));
        talkAnim.setDuration(400);
        talkAnim.setRepeatCount(ValueAnimator.INFINITE);
        talkAnim.setRepeatMode(ValueAnimator.REVERSE);
        talkAnim.start();
    }

    private void stopTalk() {
        if (talkAnim != null) {
            talkAnim.cancel();
            talkAnim = null;
        }
        imgAvatar.setScaleX(1f);
        imgAvatar.setScaleY(1f);
    }

    private void terminate() {
        if (voice != null) voice.stop();
        if (listening && recognizer != null) recognizer.cancel();
        finishListening();
        stopTalk();
        setStatus(null);
        toast("Voice band ho gayi ✋");
    }

    private void toggleSpeaker() {
        voiceOn = !voiceOn;
        updateSpeakerLabel();
        toast(voiceOn ? "Awaaz on 🔊" : "Awaaz off 🔈");
    }

    private void updateSpeakerLabel() {
        btnSpeaker.setText(voiceOn ? "🔊" : "🔈");
    }

    private void toggleMic() {
        if (listening) {
            if (recognizer != null) recognizer.stopListening();
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            toast("Is phone me speech recognition available nahi hai");
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestMic.launch(Manifest.permission.RECORD_AUDIO);
            return;
        }
        startListening();
    }

    private void startListening() {
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        }
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }

            @Override
            public void onError(int error) {
                finishListening();
                setStatus("Samajh nahi aaya, phir se bolo 🎤");
            }

            @Override
            public void onResults(Bundle results) {
                finishListening();
                ArrayList<String> r = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null && !r.isEmpty()) {
                    sendText(r.get(0), true);
                } else {
                    setStatus(null);
                }
            }

            @Override
            public void onPartialResults(Bundle partial) {
                ArrayList<String> r = partial.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (r != null && !r.isEmpty()) {
                    etMessage.setText(r.get(0));
                }
            }
        });

        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        listening = true;
        btnMic.setText(R.string.btn_mic_on);
        setStatus(getString(R.string.status_listening));
        recognizer.startListening(i);
    }

    private void finishListening() {
        listening = false;
        btnMic.setText(R.string.btn_mic);
    }

    private void openFile(File file, String mime) {
        Uri uri = FileProvider.getUriForFile(this, AUTHORITY, file);
        Intent i = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(i, "Kholo"));
        } catch (ActivityNotFoundException e) {
            toast("Is file ko kholne wali app nahi mili");
        }
    }

    private void showHistory() {
        final List<List<ChatMessage>> sessions = prefs.getArchives();
        List<String> titles = new ArrayList<>();
        titles.add("➕ Naya chat (abhi wali save ho jayegi)");
        for (List<ChatMessage> s : sessions) {
            titles.add(titleOf(s));
        }
        new AlertDialog.Builder(this)
                .setTitle("Purani chats")
                .setItems(titles.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        newChat();
                    } else {
                        restore(sessions, which - 1);
                    }
                })
                .setNegativeButton("Band karo", null)
                .show();
    }

    private void newChat() {
        archiveCurrent();
        messages.clear();
        addMessage(new ChatMessage(ChatMessage.AI, greeting(), ""));
    }

    private void archiveCurrent() {
        boolean hasUser = false;
        for (ChatMessage m : messages) {
            if (m.role == ChatMessage.USER) {
                hasUser = true;
                break;
            }
        }
        if (!hasUser) return;
        List<List<ChatMessage>> all = prefs.getArchives();
        all.add(0, new ArrayList<>(messages));
        prefs.setArchives(all);
    }

    private void restore(List<List<ChatMessage>> sessions, int index) {
        List<ChatMessage> chosen = new ArrayList<>(sessions.get(index));
        List<List<ChatMessage>> all = prefs.getArchives();
        if (index < all.size()) all.remove(index);
        prefs.setArchives(all);
        archiveCurrent();
        messages.clear();
        messages.addAll(chosen);
        adapter.notifyDataSetChanged();
        prefs.saveChat(messages);
        rvChat.scrollToPosition(Math.max(0, messages.size() - 1));
    }

    private String titleOf(List<ChatMessage> session) {
        for (ChatMessage m : session) {
            if (m.role == ChatMessage.USER && !m.text.isEmpty()) {
                return m.text.length() > 40 ? m.text.substring(0, 40) + "..." : m.text;
            }
        }
        return "Chat";
    }
}
