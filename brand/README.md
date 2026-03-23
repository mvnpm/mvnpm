# mvnpm Brand Assets

SVG logos use outlined paths (not `<text>` elements) so they render identically on all devices regardless of installed fonts.

## Files

- `logo.svg` — Dark text on light background (`currentColor` for "mvn")
- `logo-transparent.svg` — Same as logo.svg (uses `currentColor`)
- `logo-reversed.svg` — Light text (`#F0F0F0` for "mvn") for dark backgrounds
- `favicon.svg` — Square icon with gradient background and white `{/>` mark

## Font

The paths were generated from **SF Mono Bold** (`.SF NS Mono` variable font at `wght: 700`), which is the system monospace font on macOS and what Chrome uses when rendering `font-family: 'SF Mono', monospace; font-weight: 700`.

The source font file is at `/System/Library/Fonts/SFNSMono.ttf` (macOS variable font, weight range ~295-900).

## Stroke for visual weight

All path groups use `stroke-width="1.5"` with `stroke-linejoin="round"` to simulate the visual weight of Chrome's faux bold rendering. The stroke color matches the fill color for each group.

## Regenerating paths

If you need to regenerate the SVG paths (e.g., to change text or font weight):

### Prerequisites

```bash
cd /tmp && npm install fontkit opentype.js
cp /System/Library/Fonts/SFNSMono.ttf /tmp/SFMono.ttf
```

### Extract paths

```bash
node -e "
const fontkit = require('fontkit');
const font = fontkit.openSync('/tmp/SFMono.ttf');
const bold = font.getVariation({ wght: 700 });

const texts = [
  { id: 'prefix', text: '{/>', size: 64 },
  { id: 'mvn', text: 'mvn', size: 64 },
  { id: 'pm', text: 'pm', size: 64 },
  { id: 'favicon_prefix', text: '{/>', size: 30 },
];

for (const t of texts) {
  const run = bold.layout(t.text);
  const scale = t.size / bold.unitsPerEm;
  let svgPath = '';
  let xOffset = 0;

  for (let i = 0; i < run.glyphs.length; i++) {
    const glyph = run.glyphs[i];
    const pos = run.positions[i];
    for (const cmd of glyph.path.commands) {
      const x = (v) => ((v + xOffset + (pos.xOffset || 0)) * scale).toFixed(2);
      const y = (v) => (-v * scale).toFixed(2);
      switch(cmd.command) {
        case 'moveTo': svgPath += 'M' + x(cmd.args[0]) + ' ' + y(cmd.args[1]); break;
        case 'lineTo': svgPath += 'L' + x(cmd.args[0]) + ' ' + y(cmd.args[1]); break;
        case 'quadraticCurveTo': svgPath += 'Q' + x(cmd.args[0]) + ' ' + y(cmd.args[1]) + ' ' + x(cmd.args[2]) + ' ' + y(cmd.args[3]); break;
        case 'bezierCurveTo': svgPath += 'C' + x(cmd.args[0]) + ' ' + y(cmd.args[1]) + ' ' + x(cmd.args[2]) + ' ' + y(cmd.args[3]) + ' ' + x(cmd.args[4]) + ' ' + y(cmd.args[5]); break;
        case 'closePath': svgPath += 'Z'; break;
      }
    }
    xOffset += pos.xAdvance;
  }
  console.log(t.id + ':', svgPath);
}
"
```

### SVG structure

The logos use viewBox `0 0 420 120` with baseline at y=82:

```xml
<g transform="translate(10, 82)" fill="url(#bridge)" stroke="url(#bridge)" stroke-width="1.5" stroke-linejoin="round">   <!-- {/> prefix -->
<g transform="translate(175, 82)" fill="..." stroke="..." stroke-width="1.5" stroke-linejoin="round">                    <!-- mvn wordmark -->
<g transform="translate(289, 82)" fill="#F59E0B" stroke="#F59E0B" stroke-width="1.5" stroke-linejoin="round">             <!-- pm wordmark -->
```

The favicon uses viewBox `0 0 64 64` with the `{/>` mark at font-size 30, positioned at `translate(4.31, 42)` with `stroke-width="1.5"`.

Path coordinates use baseline at y=0 (negative y = above baseline), so the `translate` y-value sets the baseline position.

## Colors

- Amber: `#F59E0B`
- Indigo: `#6366F1`
- Gradient: amber → indigo (used for `{/>` prefix)
- "pm" text: amber (`#F59E0B`)
- "mvn" text: `currentColor` (logo.svg) or `#F0F0F0` (logo-reversed.svg)
