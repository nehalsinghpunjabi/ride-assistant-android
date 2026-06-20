# Ride Assistant

An Android ride-planning assistant. It accepts typed requests, Android speech recognition,
shared text, and `rideassistant://book` deep links; resolves saved places; previews current
location on Google Maps; and hands the route to a provider app for final user confirmation.

## Build

Use Android Studio's bundled JDK (17 or newer). From PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest assembleDebug
```

Add a restricted Google Maps Android API key to the user-level Gradle properties file
(`%USERPROFILE%\.gradle\gradle.properties`), never source control:

```properties
MAPS_API_KEY=your_restricted_key
```

Restrict it by Android package `com.example.rideassistant` and the signing certificate SHA-1.
Enable Maps SDK for Android. A release build must use a production application ID and signing key.

## Assistant/deep-link contract

Examples:

```text
rideassistant://book?destination=home
rideassistant://book?pickup=work&destination=Pune%20Airport&type=cab
```

Static Home, Work, and College launcher shortcuts are included. Rich Google Assistant phrases
require choosing a currently supported Built-in Intent, Play Console setup, a Digital Asset Links
domain for verified HTTPS links, and Google review. Those external publication steps cannot be
completed from this repository alone.

## Provider and estimate boundary

The app never books automatically. Uber, Ola, and Rapido are opened only after the user chooses
an option. Their consumer apps and deep-link support can vary by installed version. If a provider
link is unavailable, Ride Assistant opens a Google Maps route preview. Displayed fare/ETA ranges
are explicitly illustrative; live comparison requires approved commercial provider APIs.
