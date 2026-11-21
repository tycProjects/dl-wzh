# Update 1.4.98 — Call banner stays until answered or declined

- The incoming call banner no longer auto-settles to the capsule. It stays until the call is answered or declined.
- Outside taps do not close the call banner.
- A WhatsApp message arriving during a call is queued and does not replace the call banner.

Analysis of the diagnostics log (not yet fixed):
- Expand: the window jumps from 60 px to full height (1496 px) before the morph starts, which looks like a jump.
- Expand: morph progress overshoots to about 1.13 and bounces back to 0.97 before settling (spring overshoot).
- Collapse: the window shrinks only at the end of the morph, at about progress 0.008.
- Gestures: touch forwarding alternates between forwarded true and false while dragging, which makes drag feel uneven.
These need changes to the window resize timing and the spring specs, planned as the next step.

Heart: the expanded panel reads the player's like state (or the local store) when it is opened.
Background updates are applied only when the expanded panel is shown.

Not compiled or run on a device here.
