# ProjectS Icon System

`ProjectSIcon` is the canonical icon registry. Widgets use only an icon ID and
never hold atlas paths or sprite coordinates. `ProjectSIconRenderer` resolves a
Theme tint, attempts the cached atlas, and falls back to integer pixel shapes.

## Atlas contract

Place future production assets at:

- `assets/projects_client/textures/gui/icons/icons_16.png`
- `assets/projects_client/textures/gui/icons/icons_32.png`

Both atlases use 16 columns. Cell order is the stable `ProjectSIcon.values()`
order. With the current 110 registered IDs, the required image dimensions are
256×112 and 512×224. The 16px atlas uses 16×16 cells; the 32px atlas uses
32×32 cells. The file is decoded and its exact dimensions are checked after
each client-resource reload. Missing, undecodable, or incorrectly sized files
use the code fallback. Regions
outside the declared image dimensions, negative coordinates, non-cell-aligned
coordinates, or incorrect cell sizes are rejected and use code fallback.

Artwork must have a transparent background, grayscale/white one- or two-pixel
strokes, one-to-two pixel internal padding, no text, and no baked glow or theme
color. The 32px artwork should preserve the same silhouette while refining
detail and stroke balance instead of relying only on a mechanical 2× upscale.
Generated concept sheets must not be cropped into production atlases.

When the atlas order ever needs to become independently versioned, add a
validated manifest loader without exposing its coordinates to widgets. Until
then, the immutable Java registry avoids load-order and malformed-JSON risks.
