# Update 1.9.14 — Low-end four-side glass reflections

- Repositioned static edge highlights inward so rounded clipping cannot cut off the left and right reflections.
- Increased visibility of the left/right static catches while keeping them restrained and separate from the frosted substrate.
- Added an inset rounded rim plus four independent edge catches to the static artwork backdrop.
- Preserved the shader-free path: no RuntimeShader, Brush gradients, or GPU RenderEffect added to low-end static glass.
- Preserved existing jelly and capsule animation code.
