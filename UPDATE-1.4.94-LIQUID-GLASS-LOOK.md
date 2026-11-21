# Update 1.4.94 — Liquid glass look closer to the reference

- Stronger refraction at the glass border (lens bevel about 1.2x and about 30% wider), so the
  artwork bends at the edges like a convex lens.
- Added a crisp specular hairline on the lit (top-left) border, for a bright glass rim.
- Applies to expanded, WhatsApp banners, and call banners through the same lens and edge sheen.

Known limits:
- Not compiled or run on a device here. Compare the result with the reference on Android 13+.
- Without artwork (no avatar and no album), the glass has nothing to refract. The WhatsApp icon is used
  as the refraction source in that case.
