# Update 1.6.2 — Window at 60%, stack scrolls and overlaps

## Change
- The drawing window is created once at 60% of the screen (was 80%): about 25% less surface memory.
- The WhatsApp stack cap is what is left inside that window: 60% of the screen minus the capsule height,
  the expanded offset and a 56 dp reserve for the jelly overshoot. It never goes below 160 dp.
- When there are more cards than fit (estimated 112 dp each), the cards overlap like the recent-apps
  stack: about 36 dp of each card shows, and the list scrolls.
- The newest card is drawn on top (z-order by timestamp).
- Normal spacing (stack gap slider) is kept when everything fits.

## Still to check on device
- Stack with many messages: overlap, scroll, and the newest card on top.
- Jelly expand with the stack: if the bottom of the stack is cut, increase the 56 dp reserve.
- Media expanded with lyrics: the panel must still fit in 60% of the screen.

Not compiled or run on a device here.
