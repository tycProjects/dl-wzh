# Update 1.6.3 — Capsule back at the top, tap expands again

## Analysis (screenshot + diagnostics log)
- The drawing window is 60% of the screen tall (960 px) since 1.6.2.
- The capsule was drawn at about y=437 px, near the middle of that window, while the touch bounds
  stayed at the top (settled bounds=257,0,206,138). The log had no TOUCH_FORWARD at all, only
  OUTSIDE_TOUCH: taps on the visible capsule were treated as outside, so the island never expanded.
- Cause: the IslandRoot root uses `wrapContentHeight()` with no alignment. Its default is vertically
  CENTRED. Before, the window was exactly the content height, so this had no effect. With a taller
  fixed window, the content moved to the middle. The same cause explains the centred capsule in 1.6.1.

## Change
- The root is `wrapContentHeight(Alignment.Top)`. The visual and the touch bounds are back in the same place.

## Still to check on device
- The capsule sits at the top edge again, and a tap expands it.
- Stack and jelly expand still fit in the 60% window (see 1.6.2 notes).

Not compiled or run on a device here.
