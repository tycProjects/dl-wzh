# Android Island 1.4.42 — Accessibility XML build fix

## Root cause
AAPT rejects the literal string value `0` for the flag-typed attributes `android:accessibilityEventTypes` and `android:accessibilityFlags`.

## Fix
Both attributes are omitted. The service subscribes to no accessibility events and requests no accessibility flags. `canRetrieveWindowContent` remains false. `feedbackGeneric` and the service description are retained.

This preserves the AccessibilityService host role used for `TYPE_ACCESSIBILITY_OVERLAY`; it does not grant access to other apps' window node trees.

## Validation
- XML parsed successfully.
- Confirmed the two invalid attributes are absent.
- Archive integrity checked.
- Build status is reported separately; do not infer a successful APK build from XML validation.
