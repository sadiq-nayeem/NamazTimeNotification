# Prayer Time Notification App

An Android app that provides prayer time notifications for Dhaka. It sends notifications when a prayer time starts, 15 minutes before it ends, and when it ends.

## Features:
- **Prayer Time Notifications**: Notifications when prayer time starts, 15 minutes before it ends, and when it ends.
- **CSV Import**: Import prayer times from a CSV file.
- **Home Page**: Displays today's prayer times and a countdown timer for the current prayer.
- **Settings**:  
  - Enable/disable notifications for specific prayers.
  - Change the time zone.
  - Import/export configuration across devices.

## Tech Stack:
- **Kotlin**
- **Jetpack Compose**
- **WorkManager** / **Foreground Service** for background notifications
- **DataStore** for local storage

## How to Use:
1. Import the prayer schedule via CSV.
2. Enable/disable notifications based on preferences.
3. View today's prayer times and countdown on the home screen.

## Future Enhancements:
- Cloud backup for settings and prayer times.

