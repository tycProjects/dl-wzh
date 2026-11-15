# Update 1.4.82 - Dome refraction and WhatsApp stack motion

- Kept the existing three-dome liquid geometry and reduced artwork sampling displacement, edge bend, chromatic dispersion, and flow. This prevents the artwork from appearing stretched or folded while preserving visible dome highlights and bevels.
- WhatsApp stack cards now enter with a small per-card stagger (72 ms) and a slower 560 ms ease-out reveal. Low-power mode skips the stagger.
- Swiping a card upward in the expanded WhatsApp stack uses the existing tuck animation and collapses the stack into the capsule indicator; it does not clear the underlying unread items.
- Existing horizontal swipe dismissal and reply handling remain intact. A dismissed/replied card is still removed through the existing clearWhatsApp path; the upward stack-collapse gesture does not mark messages as read or responded.

Build note: source-level changes only; compile and on-device gesture validation are still required.
