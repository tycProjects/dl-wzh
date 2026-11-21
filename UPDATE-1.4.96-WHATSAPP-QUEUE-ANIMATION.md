# Update 1.4.96 — Removals, WhatsApp queue, slower animations

## Removed
- Lens effect and its slider ("Intensitas lensa kaca"): forced off.
- Expanded shadow slider ("Bayangan konten expand"): forced to 0.
- Expanded popup for incoming calls: calls stay as banner and capsule.
- Liquid sheen is now the same on every side and corner.

## WhatsApp
- The newest message takes the banner; older messages stay in the stack.
- The same message from the same sender is shown only once. A different message, or the same text from
  another sender, is allowed.
- Opening WhatsApp clears all WhatsApp banners, from a tap or when WhatsApp comes to the foreground.

## Animation
- Default speed lowered from 0.9 to 0.55 (slower transitions). The speed slider still works.

## Not changed yet
- The stack is already drawn inside the expanded panel. Each card keeps its own glass and background.
- Capsule visibility while the stack is open still needs checking on device.
- Per-animation smoothing is not tuned; compare on device.

Known limits:
- Not compiled or run on a device here.
