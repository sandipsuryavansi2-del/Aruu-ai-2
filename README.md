# Aruu AI 2

Java + XML Android app. APK GitHub Actions se banta hai, API key kabhi code me nahi likhi jaati.

## Setup (pehli baar)
1. ZIP extract karo aur sab files ek naye GitHub repo me push karo (`.github` folder bhi).
2. GitHub me **Actions** tab kholo, "Build Aruu AI 2 APK" run complete hone do.
3. Run ke neeche **Artifacts** me `Aruu-AI-2-debug-apk` download karo, zip khol kar APK install karo.
4. App khulte hi API key screen aayegi. Free key ke liye aistudio.google.com par API key banao aur paste karke SAVE KARO dabao.
5. Key baad me Settings (⚙️) me badal sakte ho. Model naam bhi wahi change hote hain.

## Features (is build me)
- API key screen, key SharedPreferences me save hoti hai
- Pink + purple main screen, "Aruu AI 2 - love is speaking", avatar, chat, MIC, TERMINATE, PHOTO, PREMIUM
- Gemini chat (Hindi + English), naam yaad rakhna, offline replies jab API na chale
- Voice: mic se bolo, Aruu awaaz me jawab degi, TERMINATE se voice band
- Photo samjho (gallery se photo), PDF padho aur PDF se sawal, chat ko PDF banao
- Image banao (Gemini image model), video banao (experimental, model naam Settings me)
- Code, homework, translate, email/letter, story/shayari: chips se template bharo ya seedha poocho
- Live search aur live news (Google Search grounding, "latest" ya "news" likhne par)
- Alarm/reminder, torch on/off, camera kholo, app kholo, call, SMS, gaana (YouTube), mausam
- Chat history save, "Naya chat" aur purani chats
- Premium gold page (demo)

## Abhi nahi hai (future)
- "Hey Aruu" wake word aur hamesha sunna (background listening ke liye foreground service aur wake-word engine chahiye)
- WiFi on/off (Android 10 se normal app ye nahi kar sakta)
- Ghar ke smart devices control (device platform ka integration chahiye)
- Real phone call lagana contacts se (abhi number dialer me jaata hai)

## Notes
- Key sirf is phone me SharedPreferences me rehti hai.
- Gemini model naam Settings me hain. Agar Google koi model band kare to yaha naya naam daal do.
- Video feature API access par depend karta hai, paid ya limited access ho sakta hai.
