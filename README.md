# MediTrack - Medicine Reminder Android App

> **⚠️ Important Setup Notes:**
> - **Grant permissions manually** in Android Settings → Apps → MediTrack → Permissions for optimal functionality
> - **Use light mode** for the best visual experience - avoid dark mode for proper UI display

A comprehensive Android application for tracking and managing medicine schedules with smart notifications, analytics, and PDF reporting capabilities.

## 📱 Demo and functional apk
https://drive.google.com/drive/folders/18XHz6SSW8w3i6B8JWD1ifiPVnokrnR6y?usp=sharing

## 📱 Features

### Core Functionality
- **Medicine Management**: Add, view, and delete medicine schedules with customizable dosing options
- **Smart Reminders**: Automated notifications 30 minutes before and after scheduled times with audio alarms
- **Daily Tracking**: Mark medicines as taken with checkbox interface and automatic daily reset at midnight
- **Weekly Scheduling**: Set specific days for each medicine with flexible day selection

### Advanced Features
- **Analytics Dashboard**: Track strike rate, average medicines per day, and time delays with comprehensive statistics
- **PDF Reports**: Generate detailed analytics and medicine schedule reports with professional formatting
- **Sound Alarms**: Audio notifications with automatic 30-second timeout and volume control
- **Persistent Storage**: SQLite database with dual database architecture for main data and analytics

## 🛠️ Technology Stack

| Component | Technology |
|-----------|------------|
| **Language** | Kotlin with modern Android development practices |
| **Database** | SQLite with custom DatabaseHelper and AnalyticsDbHelper classes |
| **UI Components** | RecyclerView, CardView, Material Design components with custom styling |
| **Notifications** | Android NotificationManager with channels and action buttons |
| **Alarms** | AlarmManager for precise weekly scheduling with follow-up reminders |
| **PDF Generation** | iTextPDF library for comprehensive report generation |
| **Architecture** | MVVM pattern with data classes and proper separation of concerns |

## 📁 Project Structure

```
app/src/main/
├── java/com/example/meditrack/
│   ├── MainActivity.kt              # Main dashboard with today's medicines
│   ├── AddMedicineActivity.kt       # Add new medicine schedules
│   ├── ViewAllMedicinesActivity.kt  # View and manage all schedules
│   ├── AnalyticsActivity.kt         # Analytics dashboard with PDF export
│   ├── DatabaseHelper.kt            # Main database operations
│   ├── AnalyticsDbHelper.kt         # Analytics data management
│   ├── AlarmReceiver.kt             # Notification and alarm handling
│   ├── DailyResetReceiver.kt        # Midnight checkbox reset
│   ├── NotificationHelper.kt        # Notification management
│   ├── MedicineAdapter.kt           # Today's medicines RecyclerView
│   ├── AllMedicineAdapter.kt        # All medicines RecyclerView
│   ├── Medicine.kt                  # Main data model
│   └── AnalyticsDataClasses.kt      # Analytics data structures
└── res/
    ├── layout/                      # XML layouts for all activities
    ├── drawable/                    # Icons, backgrounds, and vector graphics
    ├── values/                      # Colors, strings, themes, and dimensions
    └── mipmap-*/                    # App icons for different densities
```

## 🚀 Installation

### Prerequisites
- **Android Studio**: Arctic Fox or later
- **Minimum SDK**: 21 (Android 5.0)
- **Target SDK**: 24 (Android 7)

### Setup Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/A-Square8/MediTrack2
   cd meditrack
   ```

2. **Open in Android Studio**
   - Import the project using "Open an existing project"
   - Wait for Gradle sync to complete
   - Ensure all dependencies are resolved

3. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

## 📖 Usage Guide

### Adding Medicines
1. Tap the **"Add"** button on the main dashboard
2. Enter medicine name in the text field
3. Select dose from dropdown (1/4, 1/2, 1, 2, 3, 4, 5, 6 pills)
4. Set reminder time using the time picker dialog
5. Choose specific days or select "Daily" for all days
6. Submit to save and automatically activate weekly alarms

### Daily Management
- View today's scheduled medicines on the main dashboard
- Check off medicines as taken to update analytics
- Receive notifications 30 minutes before scheduled time
- Get follow-up reminders 30 minutes after if not marked as taken
- Use "Mark as Taken" action button directly from notifications

### Analytics and Reporting
- Access detailed statistics via the **"Analytics"** button
- Monitor strike rate (percentage of medicines taken on time)
- Track average daily medicine consumption
- View time delay patterns for late consumption
- Download comprehensive PDF reports with medicine history
- Export medicine schedules as formatted PDF documents

## 🔐 Permissions Required

> **⚠️ Manual Permission Setup Required**
> 
> After installation, manually grant these permissions in:
> **Settings → Apps → MediTrack → Permissions**

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

### Critical Permissions
- **Notifications**: Required for medicine reminders
- **Exact Alarms**: Essential for precise timing
- **Storage**: Needed for PDF report generation

## 🏗️ Key Components

### Database Architecture

#### Main Database (MediTrack.db)
- **`medicines` table**: Stores active medicine schedules and daily status
- Handles CRUD operations for medicine management
- Tracks daily consumption status with automatic reset

#### Analytics Database (MediTrackAnalytics.db)
- **`medicine_logs` table**: Records all scheduled and taken medicines
- **`deleted_medicines` table**: Maintains history of removed medicines
- Supports comprehensive analytics and reporting features

### Notification System
- **Reminder Notifications**: Sent 30 minutes before scheduled time
- **Follow-up Notifications**: Sent 30 minutes after if medicine not taken
- **Action Buttons**: "Mark as Taken" functionality directly from notifications
- **Sound Alerts**: Configurable alarm tones with automatic 30-second timeout
- **Notification Channels**: Separate channels for reminders and confirmations

### Analytics Engine
- **Strike Rate Calculation**: Percentage of medicines taken on schedule
- **Daily Averages**: Medicine consumption patterns and trends
- **Time Tracking**: Delay analysis for late consumption with minute precision
- **Historical Data**: Complete medicine history with deletion tracking
- **Weekly Trends**: Adherence patterns over time with visual representation

## 🎨 Development Features

### Responsive Design
- Material Design components with custom orange theme
- CardView layouts for modern appearance
- Proper spacing and typography throughout the app
- Background logo with transparency for visual appeal

### Data Management
- Dual database system for separation of concerns
- Proper error handling and data validation
- Automatic daily reset functionality
- Comprehensive logging for debugging and analytics

### User Experience
- Intuitive navigation with proper back button support
- Empty state handling with informative messages
- Toast notifications for user feedback
- Confirmation dialogs for destructive actions

## 🤝 Contributing

### Development Workflow
1. Fork the repository and create a feature branch
2. Follow Kotlin coding conventions and Material Design guidelines
3. Test thoroughly on multiple device sizes and Android versions
4. Update documentation for any new features
5. Submit pull request with detailed description of changes

### Code Standards
- Use meaningful variable and function names
- Implement proper error handling and edge cases
- Follow Android architecture best practices
- Maintain consistent code formatting and documentation

## 📄 License

This project is licensed under the **MIT License**, allowing for both personal and commercial use with proper attribution.

## 🆘 Support and Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| **Notifications not working** | Check notification permissions and battery optimization settings |
| **Alarms not triggering** | Ensure exact alarm permissions are granted manually |
| **PDF generation fails** | Verify storage permissions and available disk space |
| **UI display issues** | **Switch to light mode** - dark mode is not optimized |

### Getting Help
- Create issues in the GitHub repository for bug reports
- Include device information and Android version when reporting problems
- Check existing issues before creating new ones

---

> **💡 Pro Tips:**
> - **Always use light mode** for optimal visual experience
> - **Grant all permissions manually** during first setup
> - Test notifications after installation to ensure proper functionality

For additional support and feature requests, please use the GitHub issue tracker with detailed descriptions and steps to reproduce any problems.



