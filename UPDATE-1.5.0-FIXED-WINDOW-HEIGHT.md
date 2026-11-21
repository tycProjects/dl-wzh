# Update 1.5.0 — No window jump on expand

## Analysis (from the diagnostics log)
- The drawing window used WRAP_CONTENT height. On every state change, WindowManager relaid out the window
  in one step: DRAW_LAYOUT went from view=720x60 to view=720x1496 when the island expanded, and back to
  60 px when it collapsed (after the morph had already ended).
- Each relayout interrupts the surface and shows as a jump, separate from the morph itself.
- The window is FLAG_NOT_TOUCHABLE and touch is handled by the separate touch window, so a taller drawing
  window does not block touches outside the island.

## Change
- The drawing window has a fixed height of 80% of the screen. The content is top-aligned, so positions do
  not change. No relayout happens on state changes.
- Earlier changes stay: gesture ownership locked on ACTION_DOWN (1.4.99), softer jelly damping (1.4.99),
  call banner stays until answered or declined (1.4.98).

## Trade-offs
- The transparent drawing window now covers 80% of the screen height. It draws nothing outside the island,
  but the compositor blends it on every frame, so check battery and frame time on low-end devices.
- Content taller than 80% of the screen would be clipped. The WhatsApp stack is already capped at 80%.

Not compiled or run on a device here. On device, check: expand, collapse, drag, the call banner, and
that status bar and apps below the island still receive touches.
