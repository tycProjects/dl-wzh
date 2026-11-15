# Android Island 1.4.34

## Notification scope
- Android Island mirrors WhatsApp and WhatsApp Business notifications only.
- Notifications from all other apps remain under Android/One UI control and continue to use the system notification pop-up.
- The notification listener returns immediately for other packages; it does not cancel, dismiss, suppress, or mirror those notifications.
- Media remains handled separately through Android MediaSessionManager.

This update makes the notification allow-list explicit in code and documentation.
