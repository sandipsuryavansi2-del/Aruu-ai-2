package com.aruu.ai2;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

final class OfflineReplies {

    private static final String[] JOKES = {
            "Teacher: Sabse aalsi jaanwar kaun? Student: Sloth, sir... kal bataunga 😴",
            "Pappu: Mummy, phone ko charge kyun karte hain? Mummy: Kyunki battery khatam ho jaati hai beta! 😄",
            "Ek bhai doctor ke paas gaye: Mujhe sab kuch bhool jaata hai. Doctor: Fees paid? Bhai: Kaunsi fees? 😂"
    };

    private static final String[] SHAYARI = {
            "Dil ki baat zubaan par aayi, to lafz bhi muskuraye 💖",
            "Chand ne poocha, kaun hai jo itna roshan hai? Maine kaha, ek dost jo roz yaad aata hai 🌙",
            "Mushkilein aayengi, par himmat ka diya jalta rahe, yahi sabse khoobsurat safar hai ✨"
    };

    private OfflineReplies() { }

    static String answer(String text, String name) {
        String l = text == null ? "" : text.toLowerCase(Locale.ROOT);
        String who = name == null || name.isEmpty() ? "dost" : name;
        if (LocalCommands.hasWord(l, "hi") || LocalCommands.hasWord(l, "hello") || l.contains("namaste")) {
            return "Namaste " + who + "! Main abhi offline hoon, par baat kar sakti hoon 😊";
        }
        if (l.contains("time") || l.contains("samay") || l.contains("kitne baje")) {
            return "Abhi ka time: " + new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new Date());
        }
        if (l.contains("date") || l.contains("tareekh") || l.contains("aaj kya")) {
            return "Aaj ki date: " + new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH).format(new Date());
        }
        if (l.contains("joke") || l.contains("chutkula")) return pick(JOKES);
        if (l.contains("shayari")) return pick(SHAYARI);
        return "Abhi internet ya Gemini connection nahi hai, isliye main offline jawab de rahi hoon. Internet on karke phir poocho! 💗";
    }

    private static String pick(String[] arr) {
        return arr[new Random().nextInt(arr.length)];
    }
}
