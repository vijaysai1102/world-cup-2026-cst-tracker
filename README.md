# FIFA World Cup 2026 Android App (CST)

Welcome to the ultimate native Android companion application for the **FIFA World Cup 2026**! This app is fully customized for soccer fans tracking the tournament in **Central Standard Time (CST)**.

---

## 🏆 Key Features

1. **Native HTML Parser**: Reads and decodes match fixtures directly from the official World Cup schedule source of truth, establishing an accurate, offline-ready database.
2. **Central Standard Time (CST) Scheduler**: Converts and presents kickoff times precisely transformed from Eastern Time (ET) to Central Standard Time (CST).
3. **Live Matches & Dynamic Simulation**: Highlights real-time stats, goals, cards, and elapsed game time. When API keys aren't configured, a smart background scoring coordinator simulates active matches dynamically!
4. **Instantly Filterable Stages**: Tabs styled after Material Design 3 guidelines allow users to jump between Groups A-L, the Round of 32, Round of 16, Quarterfinals, Semifinals, and the Final.
5. **Dynamic Standings Table**: Computes team rankings, wins, draws, losses, goals difference, and group points dynamically based on real-time and completed match results.
6. **Adaptive Knockout Bracket**: An interactive, horizontal-scrolling tree diagram that visually represents tournament progression across knockout legs.
7. **Team Focus Profiles**: Instant search-to-filter capability and comprehensive team details displaying all completed and upcoming fixtures for any team.
8. **Calendar Reminders**: Effortlessly save any match to Google Calendar or device calendar in one tap.
9. **Instant Favorite System & Notifications**: Bookmark teams to prioritize upcoming fixtures, and receive simulated high-importance notifications for kickoffs, goals, cards, and results!
10. **Centralized Room Persistence**: Offline-first caching using Room, utilizing sealed state structures for a seamless Material 3 Dark theme experience.

---

## 🌎 Live Scores & AI Tactical Guidance

### 🤖 Google Gemini AI Tactical Analyst (New!)
The application features a built-in **Google Gemini AI Tactical Analyst**. In any Match details overlay dialog, you can trigger real-time expert insights, tournament predictions, and team battlegrounds generated directly by Google's `gemini-3.5-flash` model using your Gemini API key!

#### To activate Gemini AI features:
1. Since you built the app in **Google AI Studio**, you already have a Google Gemini API Key!
2. Open **AI Studio**, click the **Secrets panel** on the left-hand sidebar on your project space.
3. Verify that `GEMINI_API_KEY` is set to your Google API Key.
4. Open any Match in the app and tap **Analyze**. The model will generate customized tactical and prediction overviews in real-time!

---

### ⚽ Live Match Scores and Sports Updates
The application is designed with a hybrid dual-engine scoring architecture:
- **Offline / Simulation Mode (Default)**: If no live API key is supplied, a built-in background task advances match minutes and generates soccer events (such as goals, red cards, and full-time updates) for ongoing live matches in the tournament schedule.
- **RapidAPI Football Score Sync**: To consume real-world tournament live scores from the actual World Cup stadiums, the app is prepared with integrated Retrofit network interfaces targeting `api-sports.io`.

#### To activate real-world scores:
1. Create a free API key at [API-SPORTS](https://api-sports.io/) or RapidAPI.
2. Open **AI Studio**, navigate to the **Secrets panel** on the left-hand sidebar.
3. Add a new key called `FOOTBALL_API_KEY` and paste your secret token.
4. Rebuild the app. Retrofit will now fetch official real-world live scores, match stats, and official goal scorers to supplement your offline schedule database!

---

## 📦 Build & Installation Instructions

### 📲 Install Pre-Compiled APK Directly
A fully compiled, ready-to-install build of the application is included directly in this repository (updated to include the Central Time date-alignment fixture schedule fixes):
👉 **[world-cup-2026-tracker.apk](world-cup-2026-tracker.apk)**

#### How to Install on your Device:
1. Open this GitHub repository in the web browser on your Android device.
2. Tap on the **[world-cup-2026-tracker.apk](world-cup-2026-tracker.apk)** file.
3. Click the **"Download"** or **"View Raw"** button to download the APK binary.
4. Once downloaded, tap the file in your device's notification bar or File Manager to install it.
   > [!NOTE]
   > You may need to toggle on **"Install from Unknown Sources"** in your Android device's security settings to install custom APKs.

---

### 💻 Build and Run from Source (Android Studio)
To import and modify the code yourself:
1. Clone this repository to your local machine:
   ```bash
   git clone https://github.com/vijaysai1102/world-cup-2026-cst-tracker.git
   ```
2. Open Android Studio (Koala 2024.1 or newer recommended).
3. Select **File > Open** and choose the `world-cup-2026` project directory.
4. Let the Gradle project sync complete (it will automatically download Gradle 8.10.2 and set up dependencies).
5. Run the app on your physical device or emulator using the green **Run (play triangle) button** `▶` in the top toolbar.

---

## 🛠️ Built With

* **Jetpack Compose** - Fluent declarative UI components utilizing Material Design 3.
* **Kotlin Coroutines / Flow** - High-concurrency async streaming for live match counters and clock updates.
* **Room DB** - Locally cache fixtures, favorites, and live score history.
* **Retrofit & Moshi** - Ready-to-go HTTP integration for football scores.
* **Material Symbols** - Colorful and modern contextual icons.

Enjoy tracking the World Cup 2026 in CST! ⚽🏆🌎

