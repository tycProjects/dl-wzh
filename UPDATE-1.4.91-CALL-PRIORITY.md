# Update 1.4.91 — Call priority, caller in capsule, music pauses

## Calls
- An incoming call shows as a banner for about 2.5 seconds, then settles into the capsule.
- The capsule shows the caller's photo and the elapsed time (ticks each second). Ringing shows the name.
- A call takes priority over WhatsApp. A WhatsApp message arriving during a call shows briefly, then the
  capsule returns to the call.
- When the call ends, unread WhatsApp messages show in the capsule, otherwise the previous media state.

## Music
- Music pauses when a call or a WhatsApp message interrupts it.
- Music is not resumed automatically after the call. Resume it manually.

Known limits:
- The elapsed time uses the notification or call-start time. For calls without a caller photo, the
  phone icon shows.
- Not compiled or run on a device here. Test: incoming call while WhatsApp banner shows; WhatsApp
  message during a call; music playing when a call comes in.
