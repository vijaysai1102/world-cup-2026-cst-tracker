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

## 📦 Export, Build, and Install Instructions

### How to download and install this app from GitHub

This project is a standard Android Studio project written in modern **Kotlin** and **Jetpack Compose**. To make the APK download-ready for others right from your GitHub repository:

#### Step 1: Push the Code to GitHub
- Open the settings/toolbar in **Google AI Studio**.
- Click the **GitHub** icon to link your account and push this clean, compiled codebase directly into a new repository (e.g., `WorldCup2026-CST-Android`).

#### Step 2: Build a Release APK (or Debug APK)
To let friends download and install the app on their phone without needing developer tools:
1. Clone the repository in **Android Studio** on your computer.
2. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)** in the top menu.
3. Locate the generated executable file (usually found at `app/build/outputs/apk/debug/app-debug.apk`).

#### Step 3: Make it Downloadable on GitHub
1. Navigate to your GitHub repository in your browser.
2. Click **Releases > Create a new release**.
3. Fill out the version details (e.g., `v1.0.0`) and description.
4. Drag and drop your built **`app-debug.apk`** into the "Attach binaries" box at the bottom.
5. Publish the release! Now anyone can download the APK file from their mobile browser, enable "Install from Unknown Sources" on their device settings, and install your World Cup CST tracker instantly!

---

## 🛠️ Built With

* **Jetpack Compose** - Fluent declarative UI components utilizing Material Design 3.
* **Kotlin Coroutines / Flow** - High-concurrency async streaming for live match counters and clock updates.
* **Room DB** - Locally cache fixtures, favorites, and live score history.
* **Retrofit & Moshi** - Ready-to-go HTTP integration for football scores.
* **Material Symbols** - Colorful and modern contextual icons.

Enjoy tracking the World Cup 2026 in CST! ⚽🏆🌎
