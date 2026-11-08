# Update 1.4.99 — Smoother jelly, stable touch gesture

- Touch: the island decides once, on ACTION_DOWN, whether it owns a gesture, and keeps that decision for the
  whole gesture. Before, the decision flipped on some MOVE events (see TOUCH_FORWARD in the log).
- Jelly spring: higher damping, so the overshoot is shorter and the panel settles without the extra bounce
  (LOW 0.66 -> 0.74, MEDIUM 0.57 -> 0.66, HIGH 0.46 -> 0.58, NONE 0.78 -> 0.86). Squash and stretch stay.

Not changed yet:
- The window height still changes in one step when the panel opens (60 px -> full height). This needs the
  window to be sized ahead of the morph, planned for the next update together with on-device checks.

Not compiled or run on a device here.
