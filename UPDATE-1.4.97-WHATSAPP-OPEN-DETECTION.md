# Update 1.4.97 — Clear WhatsApp banners when WhatsApp opens directly

- The accessibility service now subscribes to window-state events from com.whatsapp only. Before this,
  it received no events, so opening WhatsApp from its own icon did not clear the banners.
- Opening WhatsApp from any entry point (its icon, a notification, or the island) clears all WhatsApp banners.

Known limits:
- Clearing also happens when WhatsApp changes screens while open, so a new message that arrives during
  that time is cleared right away. This matches "WhatsApp dibuka = banner hilang".
- The accessibility service must be enabled in Settings for this to work (it already is for the island).
- Not compiled or run on a device here.
