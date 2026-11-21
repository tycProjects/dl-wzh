# Update 1.4.90 — Effect layer on top of blur, call rules

## Layering
- Dome and drip effects now live in their own Box, drawn after the artwork blur and the glass body
  on every surface: expanded (media and WhatsApp stack), WhatsApp banner, and call banner.
  The blur can no longer draw over the effect.

## Call rules
- A call no longer interrupts WhatsApp. While a WhatsApp banner or stack is on screen, the call is
  held back and shown when WhatsApp clears.
- When another app is active, a call shows in the capsule (compact), not as a banner.
- Ending the call restores the previous state as before.

## Not changed yet
- "Musik harus dalam keadaan mati" is not implemented: the meaning needs confirmation.

Known limits:
- Not compiled or run on a device here. Test on Android 13+ (AGSL). Test a call during a WhatsApp
  banner, and a call while another app is open.
