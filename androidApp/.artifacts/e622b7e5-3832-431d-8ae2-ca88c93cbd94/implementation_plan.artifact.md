# Fix Local API Connection for Physical Device

The app is currently using `10.0.2.2`, which is a special address that only works from an Android Emulator to reach the host computer. Physical devices cannot use this address and must use the computer's actual local network IP.

## User Review Required

> [!IMPORTANT]
> To connect your phone to your local server:
> 1. Your **phone and computer must be on the same Wi-Fi network**.
> 2. Your backend server must be configured to listen on all interfaces (e.g., `0.0.0.0`) rather than just `localhost` (`127.0.0.1`).
> 3. Your computer's firewall must allow incoming connections on port **8080**.

## Proposed Changes

### [app]

#### [MODIFY] [TourismApi.kt](file:///C:/Users/Noah/Desktop/Kotlin Projects/TourVerse/androidApp/app/src/main/kotlin/com/tourverse/data/remote/TourismApi.kt)

Change the `baseUrl` to use your computer's local IP address: `192.168.0.150`.

## Verification Plan

### Manual Verification
- Deploy the updated app to your phone.
- Ensure the backend server is running on your computer at port 8080.
- Verify that the destinations are successfully fetched without a timeout.
