# FILE MAX — Figma handoff specification

Figma file created: https://www.figma.com/design/Z5NpOSrIMImxMQcEKCbDM5

**Status:** the Figma document exists, but actual editable screens/components could not be authored because the connected Figma integration reached its usage limit. This file is a design handoff, not a claim that the Figma canvas is complete.

## Visual identity
- Product name: FILE MAX (independent brand; ZArchiver is only a functional reference)
- Primary background: `#FFFFFF`
- Cool yellow accent: `#F3D76B`
- Soft yellow surface: `#FFF3C4`
- Secondary background: `#F5F5F2`
- Primary text: `#252525`
- Secondary text: `#777777`
- Borders: `#E7E7E2`
- Corner radii: 12–16 px for cards/dialogs; 10 px for compact controls
- Avoid heavy shadows and decorative gradients

## Required editable frames
1. **Files — Arabic RTL:** top app bar, current path, search, toolbar, real file list with folder/file type icon, size/date metadata, selection state, bottom navigation.
2. **Files — English LTR:** same component system mirrored for LTR.
3. **Permission onboarding:** explain Android all-files access, primary grant action, limited SAF folder-selection alternative, and denied state.
4. **Archive actions:** open, extract here, extract to folder, archive information.
5. **Create ZIP dialog:** archive name, destination, compression level, password control only when supported, progress and result states.
6. **Tools:** storage analysis, large-file scan, checksum.
7. **Settings:** theme, hidden files, storage permissions, language, about.

## Reusable components
- File row: 48–64 px minimum height, icon, name, size/type, modified date, selection checkbox.
- File grid tile: icon, two-line name, size, selection control.
- Compact toolbar action with icon and Arabic/English label.
- Bottom navigation with five destinations: Home, Files, Archives, Tools, Settings.
- Dialogs with explicit destructive-action confirmation.
- Permission status banner and operation progress row.

## Layout rules
- Mobile frame baseline: 393 × 852 px.
- Respect Android status/navigation bars and display cutouts.
- Arabic screens use RTL alignment and mirrored navigation affordances.
- Keep filenames left-to-right when they contain paths/extensions, while the surrounding Arabic UI remains RTL.
- File list is the default landing surface after permission is granted.
- Do not use fake sample file names as if they were device content in product screenshots.
