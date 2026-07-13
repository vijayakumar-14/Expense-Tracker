Monthly Expense Tracker 📊
A fully native, offline-first Android application built with Kotlin and Jetpack Compose. The Monthly Expense Tracker helps users seamlessly log, visualize, and export their daily financial data. Designed with a clean, modern financial-app aesthetic, it features robust local storage using Room, dynamic charts, and automated PDF reporting.

This project was initially prototyped using Google AI Studio and compiled/exported via Android Studio.

✨ Key Features
🧭 Intuitive Navigation
Bottom Navigation Bar: Quick access to the three core screens: Edit Expense, Today Expense (Default Landing), and Expense Stats.

Side Navigation Drawer: Features an integrated calendar for lightning-fast date jumping to view or edit any specific day's expenses.

📝 Smart Expense Logging ("Today Expense")
Auto-Date & Live List: Automatically displays today's date and a real-time, editable, and deletable list of the day's entries with a running total.

Intelligent Autocomplete: The expense type input features a case-insensitive autocomplete dropdown, suggesting the top 5 closest matches from past entries.

Data Normalization: Expense types are automatically trimmed and capitalized (e.g., "milk", "Milk", and "MILK" are all saved as "Milk") to ensure perfectly clean data aggregation.

UI Feedback: Every add, edit, or delete action is confirmed with a brief Snackbar notification.

📅 Seamless History Management ("Edit Expense")
Swipeable Calendar: A full-month calendar view allowing selection of past, present, and future dates.

Visual Indicators: Dates with existing expense entries display a small dot indicator.

Full CRUD Support: Tapping any date allows the user to view, edit, delete, or add new entries for that specific day.

📈 Advanced Analytics & Dashboard ("Expense Stats")
Dynamic Dashboards: Card-based UI displaying totals for Today, This Week, and This Month. Automatically resets the "current month" totals to zero on the 1st of a new month without deleting historical data.

Visual Charts: * A bar chart detailing the current week's daily totals.

A grouped chart showing monthly totals aggregated by expense type.

Historical Browsing: A dropdown selector to review statistics and charts from previous months.

Empty States: Polished empty-state illustrations and messages when a day or month has no logged data.

📄 PDF Export & Sharing
Export detailed financial reports with three dedicated buttons: Export Today, Export This Week, and Export This Month.

PDFs include the date range, an itemized list of expenses, and a calculated total.

Files are saved directly to the device's Downloads folder using MediaStore (Scoped Storage) and immediately trigger an intent to view or share the document.

✉️ Built-in Support
Raise a Query: A dedicated button on the Stats tab that launches the user's default email client via ACTION_SENDTO, pre-filled with the support email and subject line.

🛠 Tech Stack
Language: Kotlin

UI Framework: Jetpack Compose (Light & Dark Mode supported)

Local Database: Room (SQLite)

Storage API: MediaStore (for Scoped Storage PDF saving)

Intents: ACTION_SENDTO (Email), ACTION_VIEW/ACTION_SEND (PDF Sharing)

🚀 Getting Started
Prerequisites
Android Studio (Latest stable version recommended)

Minimum SDK: 24 (or as configured in build.gradle)

Build Instructions
Clone the repository:

Bash
git clone https://github.com/yourusername/monthly-expense-tracker.git
Open the project in Android Studio.

Sync the Gradle files.

Build and run the app on an emulator or physical device.

📞 Support & Feedback
If you encounter any issues or have feature requests, please use the Raise a Query button inside the app (Stats Tab) to send an email directly to vijayakumar.arunachalam2007@gmail.com with the subject "Expense Tracker Support Query".

Designed and created by Vijaya Kumar
