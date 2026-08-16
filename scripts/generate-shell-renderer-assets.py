"""Generate the offline, antialiased Client Shell renderer assets.

The shell icon vectors are read directly from the inline SVG symbols in the
offline mockup.  The source fragment hashes are written to the manifest so a
future atlas cannot silently drift away from that provenance.  The rasterizer
is intentionally small and dependency-free: supersampling produces a stable
white/RGBA alpha mask, while Minecraft supplies the runtime tint.

No runtime network, system font, Unicode icon, browser, or third-party image
library is involved.
"""

from __future__ import annotations

import hashlib
import json
import math
import re
import struct
import zlib
from pathlib import Path
from xml.etree import ElementTree


PROJECT_ROOT = Path(__file__).resolve().parent.parent
WORKSPACE_ROOT = PROJECT_ROOT.parent.parent
SOURCE_PATH = WORKSPACE_ROOT / "mockups/projects-client-shell/index.html"
RESOURCE_ROOT = PROJECT_ROOT / "minecraft-adapter/src/client/resources/assets/projects_client"
ICON_PATH = RESOURCE_ROOT / "textures/ui/shell_icons_96.png"
SURFACE_PATH = RESOURCE_ROOT / "textures/ui/shell_surface_masks_4x.png"
MANIFEST_PATH = RESOURCE_ROOT / "licenses/icons/shell-manifest.json"

VIEWBOX = 24.0
ICON_CELL = 96  # 4x the source viewBox; the GPU can downsample a high-resolution mask.
ICON_COLUMNS = 6
ICON_ROWS = 4
ICON_WIDTH = ICON_CELL * ICON_COLUMNS
ICON_HEIGHT = ICON_CELL * ICON_ROWS
ICON_SCALE = ICON_CELL / VIEWBOX
SAMPLE = 4

SURFACE_TILE = 320  # 80 logical pixels at 4x resolution.
SURFACE_BORDER_WIDTHS = list(range(1, 9))
SURFACE_COLUMNS = 2 + len(SURFACE_BORDER_WIDTHS)  # fill, dedicated borders, shadow
SURFACE_WIDTH = SURFACE_TILE * SURFACE_COLUMNS
SURFACE_HEIGHT = SURFACE_TILE
SURFACE_SCALE = 4
SURFACE_LOGICAL_TILE = SURFACE_TILE / SURFACE_SCALE
SURFACE_SOURCE_CAP = 32
SURFACE_SOURCE_RADIUS = 30
SURFACE_SHADOW_BLUR = 8
SURFACE_BORDER_FIRST_COLUMN = 1
SURFACE_SHADOW_COLUMN = SURFACE_COLUMNS - 1

ICON_SOURCES = [
    ("brand", "icon-brand"),
    ("home", "icon-home"),
    ("library", "icon-library"),
    ("sliders", "icon-sliders"),
    ("chevron-right", "icon-chevron-right"),
    ("chevron-down", "icon-chevron-down"),
    ("arrow-right", "icon-arrow-right"),
    ("play", "icon-play"),
    ("monitor", "icon-monitor"),
    ("server", "icon-server"),
    ("check", "icon-check"),
    ("loader", "icon-loader"),
    ("close", "icon-x"),
    ("retry", "icon-rotate"),
    ("warning", "icon-warning"),
    ("eye", "icon-eye"),
    ("sparkle", "icon-sparkle"),
    ("info", "icon-info"),
    ("shield", "icon-shield"),
    ("layers", "icon-layers"),
    ("keyboard", "icon-keyboard"),
]
EXPECTED_ICON_COUNT = 21

NUMBER = r"[-+]?(?:\d*\.\d+|\d+\.?)(?:[eE][-+]?\d+)?"
TOKEN = re.compile(rf"[AaCcHhLlMmQqSsTtVvZz]|{NUMBER}")


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def png_chunk(kind: bytes, payload: bytes) -> bytes:
    return (struct.pack(">I", len(payload)) + kind + payload
            + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF))


def write_png(path: Path, width: int, height: int, alpha_rows: list[bytes]) -> bytes:
    rows = []
    for row in alpha_rows:
        rgba = bytearray([0])
        for value in row:
            rgba.extend((255, 255, 255, value))
        rows.append(bytes(rgba))
    data = (b"\x89PNG\r\n\x1a\n"
            + png_chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
            + png_chunk(b"IDAT", zlib.compress(b"".join(rows), 9))
            + png_chunk(b"IEND", b""))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    return data


def number_list(tokens: list[str], index: int, count: int) -> tuple[list[float], int]:
    if index + count > len(tokens):
        raise ValueError("truncated SVG path command")
    values = []
    for token in tokens[index:index + count]:
        if token[0].isalpha():
            raise ValueError("SVG path command needs more numeric arguments")
        values.append(float(token))
    return values, index + count


def reflected(control: tuple[float, float], point: tuple[float, float]) -> tuple[float, float]:
    return 2 * point[0] - control[0], 2 * point[1] - control[1]


def cubic(a, b, c, d, amount: float) -> tuple[float, float]:
    inverse = 1 - amount
    return (
        inverse ** 3 * a[0] + 3 * inverse ** 2 * amount * b[0]
        + 3 * inverse * amount ** 2 * c[0] + amount ** 3 * d[0],
        inverse ** 3 * a[1] + 3 * inverse ** 2 * amount * b[1]
        + 3 * inverse * amount ** 2 * c[1] + amount ** 3 * d[1],
    )


def quadratic(a, b, c, amount: float) -> tuple[float, float]:
    inverse = 1 - amount
    return (
        inverse ** 2 * a[0] + 2 * inverse * amount * b[0] + amount ** 2 * c[0],
        inverse ** 2 * a[1] + 2 * inverse * amount * b[1] + amount ** 2 * c[1],
    )


def arc_points(start, rx: float, ry: float, rotation: float,
               large_arc: float, sweep: float, end) -> list[tuple[float, float]]:
    """Flatten one SVG elliptical arc using the SVG implementation notes."""
    rx, ry = abs(rx), abs(ry)
    if rx == 0 or ry == 0 or start == end:
        return [end]
    phi = math.radians(rotation % 360)
    cos_phi, sin_phi = math.cos(phi), math.sin(phi)
    dx = (start[0] - end[0]) / 2
    dy = (start[1] - end[1]) / 2
    x_prime = cos_phi * dx + sin_phi * dy
    y_prime = -sin_phi * dx + cos_phi * dy
    radius_scale = (x_prime * x_prime) / (rx * rx) + (y_prime * y_prime) / (ry * ry)
    if radius_scale > 1:
        factor = math.sqrt(radius_scale)
        rx *= factor
        ry *= factor
    sign = -1 if bool(round(large_arc)) == bool(round(sweep)) else 1
    numerator = max(0, (rx * rx * ry * ry)
                    - (rx * rx * y_prime * y_prime)
                    - (ry * ry * x_prime * x_prime))
    denominator = rx * rx * y_prime * y_prime + ry * ry * x_prime * x_prime
    factor = sign * math.sqrt(numerator / denominator) if denominator else 0
    cx_prime = factor * (rx * y_prime / ry)
    cy_prime = factor * (-ry * x_prime / rx)
    cx = cos_phi * cx_prime - sin_phi * cy_prime + (start[0] + end[0]) / 2
    cy = sin_phi * cx_prime + cos_phi * cy_prime + (start[1] + end[1]) / 2

    def unit_angle(ax, ay, bx, by):
        return math.atan2(ax * by - ay * bx, ax * bx + ay * by)

    ux = (x_prime - cx_prime) / rx
    uy = (y_prime - cy_prime) / ry
    vx = (-x_prime - cx_prime) / rx
    vy = (-y_prime - cy_prime) / ry
    start_angle = unit_angle(1, 0, ux, uy)
    delta = unit_angle(ux, uy, vx, vy)
    if not sweep and delta > 0:
        delta -= 2 * math.pi
    elif sweep and delta < 0:
        delta += 2 * math.pi
    segments = max(4, math.ceil(abs(delta) / (math.pi / 12)))
    points = []
    for index in range(1, segments + 1):
        amount = index / segments
        angle = start_angle + delta * amount
        points.append((
            cx + cos_phi * rx * math.cos(angle) - sin_phi * ry * math.sin(angle),
            cy + sin_phi * rx * math.cos(angle) + cos_phi * ry * math.sin(angle),
        ))
    return points


def parse_path(path_data: str) -> list[tuple[list[tuple[float, float]], bool]]:
    tokens = TOKEN.findall(path_data)
    index = 0
    command = None
    current = (0.0, 0.0)
    start = current
    previous_control = None
    previous_command = None
    subpaths: list[tuple[list[tuple[float, float]], bool]] = []
    points: list[tuple[float, float]] = []
    closed = False

    def ensure_path():
        nonlocal points
        if not points:
            points = [current]

    def finish_path():
        nonlocal points, closed
        if points:
            subpaths.append((points, closed))
        points = []
        closed = False

    while index < len(tokens):
        if tokens[index][0].isalpha():
            command = tokens[index]
            index += 1
            if command.lower() == "z":
                if points and current != start:
                    points.append(start)
                current = start
                closed = True
                finish_path()
                previous_control = None
                previous_command = command
                command = None
                continue
        if command is None:
            raise ValueError("SVG path number without a command")

        relative = command.islower()
        kind = command.lower()
        count = {"m": 2, "l": 2, "h": 1, "v": 1, "c": 6,
                 "s": 4, "q": 4, "t": 2, "a": 7}.get(kind)
        if count is None:
            raise ValueError(f"unsupported SVG command: {command}")
        values, index = number_list(tokens, index, count)
        if kind == "m":
            next_point = (values[0], values[1])
            if relative:
                next_point = (current[0] + next_point[0], current[1] + next_point[1])
            if points:
                finish_path()
            current = next_point
            start = current
            points = [current]
            command = "l" if relative else "L"
            previous_control = None
        elif kind == "l":
            next_point = (values[0], values[1])
            if relative:
                next_point = (current[0] + next_point[0], current[1] + next_point[1])
            ensure_path()
            points.append(next_point)
            current = next_point
            previous_control = None
        elif kind == "h":
            next_point = (current[0] + values[0], current[1]) if relative else (values[0], current[1])
            ensure_path()
            points.append(next_point)
            current = next_point
            previous_control = None
        elif kind == "v":
            next_point = (current[0], current[1] + values[0]) if relative else (current[0], values[0])
            ensure_path()
            points.append(next_point)
            current = next_point
            previous_control = None
        elif kind in ("c", "s", "q", "t"):
            if kind == "c":
                controls = [(values[0], values[1]), (values[2], values[3])]
                next_point = (values[4], values[5])
                if relative:
                    controls = [(current[0] + x, current[1] + y) for x, y in controls]
                    next_point = (current[0] + next_point[0], current[1] + next_point[1])
                control_for_reflection = controls[1]
            elif kind == "s":
                first = (reflected(previous_control, current)
                         if previous_command and previous_command.lower() in ("c", "s")
                         and previous_control is not None else current)
                second = (values[0], values[1])
                next_point = (values[2], values[3])
                if relative:
                    second = (current[0] + second[0], current[1] + second[1])
                    next_point = (current[0] + next_point[0], current[1] + next_point[1])
                controls = [first, second]
                control_for_reflection = second
            elif kind == "q":
                control = (values[0], values[1])
                next_point = (values[2], values[3])
                if relative:
                    control = (current[0] + control[0], current[1] + control[1])
                    next_point = (current[0] + next_point[0], current[1] + next_point[1])
                controls = [control]
                control_for_reflection = control
            else:  # t
                control = (reflected(previous_control, current)
                           if previous_command and previous_command.lower() in ("q", "t")
                           and previous_control is not None else current)
                next_point = (values[0], values[1])
                if relative:
                    next_point = (current[0] + next_point[0], current[1] + next_point[1])
                controls = [control]
                control_for_reflection = control
            ensure_path()
            steps = 16
            for step in range(1, steps + 1):
                amount = step / steps
                point = (cubic(current, controls[0], controls[1], next_point, amount)
                         if kind in ("c", "s") else quadratic(current, controls[0], next_point, amount))
                points.append(point)
            current = next_point
            previous_control = control_for_reflection
        elif kind == "a":
            rx, ry, rotation, large_arc, sweep, x, y = values
            next_point = (x, y)
            if relative:
                next_point = (current[0] + x, current[1] + y)
            ensure_path()
            points.extend(arc_points(current, rx, ry, rotation, large_arc, sweep, next_point))
            current = next_point
            previous_control = None
        previous_command = command
    if points:
        finish_path()
    return subpaths


def rounded_rect_points(x: float, y: float, width: float, height: float, radius: float):
    radius = min(radius, width / 2, height / 2)
    points = []
    for center_x, center_y, start_angle in (
            (x + width - radius, y + radius, -math.pi / 2),
            (x + width - radius, y + height - radius, 0),
            (x + radius, y + height - radius, math.pi / 2),
            (x + radius, y + radius, math.pi)):
        for step in range(5):
            angle = start_angle + (math.pi / 2) * step / 4
            points.append((center_x + radius * math.cos(angle),
                           center_y + radius * math.sin(angle)))
    return points


def svg_primitives(symbol: ElementTree.Element):
    result = []
    for element in symbol:
        tag = element.tag.rsplit("}", 1)[-1]
        fill = element.attrib.get("fill", "black").lower()
        stroke = element.attrib.get("stroke", "none").lower()
        stroke_width = float(element.attrib.get("stroke-width", "1"))
        wants_fill = fill != "none"
        wants_stroke = stroke != "none"
        if tag == "path":
            for points, closed in parse_path(element.attrib["d"]):
                if wants_fill and len(points) >= 3:
                    result.append(("fill", points))
                if wants_stroke and len(points) >= 2:
                    result.append(("stroke", points, closed, stroke_width))
        elif tag == "rect":
            points = rounded_rect_points(float(element.attrib["x"]), float(element.attrib["y"]),
                                         float(element.attrib["width"]), float(element.attrib["height"]),
                                         float(element.attrib.get("rx", "0")))
            if wants_fill:
                result.append(("fill", points))
            if wants_stroke:
                result.append(("stroke", points, True, stroke_width))
        elif tag == "circle":
            result.append(("circle", float(element.attrib["cx"]), float(element.attrib["cy"]),
                           float(element.attrib["r"]), wants_fill, wants_stroke, stroke_width))
        elif tag == "line":
            points = [(float(element.attrib["x1"]), float(element.attrib["y1"])),
                      (float(element.attrib["x2"]), float(element.attrib["y2"]))]
            if wants_stroke:
                result.append(("stroke", points, False, stroke_width))
        else:
            raise ValueError(f"unsupported inline SVG element: {tag}")
    return result


def distance_to_segment(x, y, x1, y1, x2, y2):
    dx, dy = x2 - x1, y2 - y1
    length_squared = dx * dx + dy * dy
    amount = 0 if length_squared == 0 else max(0, min(1, ((x - x1) * dx + (y - y1) * dy) / length_squared))
    return math.hypot(x - (x1 + amount * dx), y - (y1 + amount * dy))


def inside_polygon(x, y, points):
    result = False
    for index, (x1, y1) in enumerate(points):
        x2, y2 = points[index - 1]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
            result = not result
    return result


def primitive_bounds(primitive):
    kind = primitive[0]
    if kind in ("fill", "stroke"):
        points = primitive[1]
        extra = primitive[3] if kind == "stroke" else 0
        return (min(point[0] for point in points) - extra,
                min(point[1] for point in points) - extra,
                max(point[0] for point in points) + extra,
                max(point[1] for point in points) + extra)
    _, cx, cy, radius, _, _, width = primitive
    return cx - radius - width, cy - radius - width, cx + radius + width, cy + radius + width


def primitive_hit(primitive, x: float, y: float) -> bool:
    kind = primitive[0]
    if kind == "fill":
        return inside_polygon(x, y, primitive[1])
    if kind == "stroke":
        points, closed, width = primitive[1], primitive[2], primitive[3]
        pairs = list(zip(points, points[1:]))
        if closed and len(points) > 2:
            pairs.append((points[-1], points[0]))
        return any(distance_to_segment(x, y, a[0], a[1], b[0], b[1]) <= width / 2 for a, b in pairs)
    _, cx, cy, radius, filled, stroked, width = primitive
    distance = math.hypot(x - cx, y - cy)
    return (filled and distance <= radius) or (stroked and abs(distance - radius) <= width / 2)


def rasterize_icon(primitives, index: int) -> list[bytearray]:
    width, height = ICON_WIDTH * SAMPLE, ICON_HEIGHT * SAMPLE
    canvas = [bytearray(width) for _ in range(height)]
    origin_x = (index % ICON_COLUMNS) * VIEWBOX
    origin_y = (index // ICON_COLUMNS) * VIEWBOX
    bounds = [min(primitive_bounds(p)[0] for p in primitives),
              min(primitive_bounds(p)[1] for p in primitives),
              max(primitive_bounds(p)[2] for p in primitives),
              max(primitive_bounds(p)[3] for p in primitives)]
    left = max(0, math.floor((origin_x + bounds[0]) * ICON_SCALE * SAMPLE))
    top = max(0, math.floor((origin_y + bounds[1]) * ICON_SCALE * SAMPLE))
    right = min(width, math.ceil((origin_x + bounds[2]) * ICON_SCALE * SAMPLE) + 1)
    bottom = min(height, math.ceil((origin_y + bounds[3]) * ICON_SCALE * SAMPLE) + 1)
    for py in range(top, bottom):
        for px in range(left, right):
            x = (px + .5) / (SAMPLE * ICON_SCALE) - origin_x
            y = (py + .5) / (SAMPLE * ICON_SCALE) - origin_y
            if any(primitive_hit(p, x, y) for p in primitives):
                canvas[py][px] = 255
    return canvas


def downsample_icon(canvas, index: int, output: list[bytearray]):
    width = ICON_WIDTH * SAMPLE
    origin_x = (index % ICON_COLUMNS) * ICON_CELL
    origin_y = (index // ICON_COLUMNS) * ICON_CELL
    for y in range(ICON_CELL):
        for x in range(ICON_CELL):
            total = 0
            for sy in range(SAMPLE):
                row = canvas[(origin_y + y) * SAMPLE + sy]
                for sx in range(SAMPLE):
                    total += row[(origin_x + x) * SAMPLE + sx]
            output[origin_y + y][origin_x + x] = total // (SAMPLE * SAMPLE)


def rounded_contains(x, y, left, top, width, height, radius):
    if x < left or y < top or x > left + width or y > top + height:
        return False
    radius = min(radius, width / 2, height / 2)
    nearest_x = min(max(x, left + radius), left + width - radius)
    nearest_y = min(max(y, top + radius), top + height - radius)
    return math.hypot(x - nearest_x, y - nearest_y) <= radius


def surface_alpha(kind: int, x: float, y: float) -> int:
    size = SURFACE_LOGICAL_TILE
    if kind == 0:  # filled rounded surface
        return 255 if rounded_contains(x, y, 0, 0, size, size, SURFACE_SOURCE_RADIUS) else 0
    if SURFACE_BORDER_FIRST_COLUMN <= kind < SURFACE_SHADOW_COLUMN:
        border_width = SURFACE_BORDER_WIDTHS[kind - SURFACE_BORDER_FIRST_COLUMN]
        outer = rounded_contains(x, y, 0, 0, size, size, SURFACE_SOURCE_RADIUS)
        inner = rounded_contains(x, y, border_width, border_width,
                                 size - 2 * border_width, size - 2 * border_width,
                                 max(0, SURFACE_SOURCE_RADIUS - border_width))
        return 255 if outer and not inner else 0
    raise ValueError(f"unknown surface mask kind: {kind}")


def box_blur(mask: list[bytearray], width: int, height: int, radius: int) -> list[bytearray]:
    """One separable box-blur pass; three passes approximate a real Gaussian blur."""
    if radius <= 0:
        return mask
    window = radius * 2 + 1
    horizontal = [bytearray(width) for _ in range(height)]
    for y, row in enumerate(mask):
        running = 0
        for x in range(width):
            running += row[x]
            if x >= window:
                running -= row[x - window]
            horizontal[y][x] = running // window
    blurred = [bytearray(width) for _ in range(height)]
    for x in range(width):
        running = 0
        for y in range(height):
            running += horizontal[y][x]
            if y >= window:
                running -= horizontal[y - window][x]
            blurred[y][x] = running // window
    return blurred


def generate_shadow_mask() -> list[bytearray]:
    """Create a genuinely blurred rounded alpha mask, not a uniform layered tint."""
    tile_pixels = SURFACE_TILE * SAMPLE
    mask = [bytearray(tile_pixels) for _ in range(tile_pixels)]
    left, top, width, height, radius = 8, 8, SURFACE_LOGICAL_TILE - 16, SURFACE_LOGICAL_TILE - 16, 25
    for py in range(tile_pixels):
        y = (py + .5) / (SAMPLE * SURFACE_SCALE)
        for px in range(tile_pixels):
            x = (px + .5) / (SAMPLE * SURFACE_SCALE)
            if rounded_contains(x, y, left, top, width, height, radius):
                mask[py][px] = 255
    blur_pixels = int(round(SURFACE_SHADOW_BLUR * SURFACE_SCALE * SAMPLE))
    for _ in range(3):
        mask = box_blur(mask, tile_pixels, tile_pixels, blur_pixels)
    return mask


def generate_surface_png() -> bytes:
    width, height = SURFACE_WIDTH * SAMPLE, SURFACE_HEIGHT * SAMPLE
    high = [bytearray(width) for _ in range(height)]
    shadow = generate_shadow_mask()
    tile_pixels = SURFACE_TILE * SAMPLE
    for kind in range(SURFACE_COLUMNS):
        left = kind * tile_pixels
        for py in range(tile_pixels):
            if kind == SURFACE_SHADOW_COLUMN:
                high[py][left:left + tile_pixels] = shadow[py]
                continue
            y = (py + .5) / (SAMPLE * SURFACE_SCALE)
            for px in range(tile_pixels):
                x = (px + .5) / (SAMPLE * SURFACE_SCALE)
                high[py][left + px] = surface_alpha(kind, x, y)
    output = [bytearray(SURFACE_WIDTH) for _ in range(SURFACE_HEIGHT)]
    for y in range(SURFACE_HEIGHT):
        for x in range(SURFACE_WIDTH):
            total = 0
            for sy in range(SAMPLE):
                row = high[y * SAMPLE + sy]
                for sx in range(SAMPLE):
                    total += row[x * SAMPLE + sx]
            output[y][x] = total // (SAMPLE * SAMPLE)
    return write_png(SURFACE_PATH, SURFACE_WIDTH, SURFACE_HEIGHT, output)


def symbol_fragment(html: str, symbol_id: str) -> str:
    match = re.search(rf"<symbol\b[^>]*\bid=[\"']{re.escape(symbol_id)}[\"'][^>]*>.*?</symbol>",
                      html, re.DOTALL)
    if not match:
        raise ValueError(f"missing inline SVG symbol: {symbol_id}")
    return match.group(0)


def generate_icon_png(html: str) -> tuple[bytes, dict]:
    output = [bytearray(ICON_WIDTH) for _ in range(ICON_HEIGHT)]
    symbols = {}
    for index, (name, symbol_id) in enumerate(ICON_SOURCES):
        fragment = symbol_fragment(html, symbol_id)
        symbols[name] = {
            "symbol": symbol_id,
            "fragmentSha256": sha256(fragment.encode()),
        }
        symbol = ElementTree.fromstring(fragment)
        primitives = svg_primitives(symbol)
        high = rasterize_icon(primitives, index)
        downsample_icon(high, index, output)
    data = write_png(ICON_PATH, ICON_WIDTH, ICON_HEIGHT, output)
    return data, symbols


def generate():
    if len(ICON_SOURCES) != EXPECTED_ICON_COUNT or ICON_COLUMNS * ICON_ROWS < EXPECTED_ICON_COUNT:
        raise ValueError("Client Shell icon contract must contain exactly 21 cells in a 6x4 atlas")
    if SURFACE_COLUMNS != 10 or len(SURFACE_BORDER_WIDTHS) != 8:
        raise ValueError("Client Shell surface contract must contain 10 columns and 8 border masks")
    html_bytes = SOURCE_PATH.read_bytes()
    html = html_bytes.decode("utf-8")
    icon_data, symbols = generate_icon_png(html)
    surface_data = generate_surface_png()
    manifest = {
        "schema": "projects-client-shell-renderer-assets-v1",
        "offlineRuntime": True,
        "runtime_downloads": False,
        "provenance": {
            "source": "mockups/projects-client-shell/index.html",
            "generatedBy": "scripts/generate-shell-renderer-assets.py",
            "sourceSha256": sha256(html_bytes),
            "inlineSvgGeometry": "exact symbol fragments are hashed and rasterized without substitution",
            "symbols": symbols,
        },
        "license": {
            "artwork": "ProjectS source license applies to original shell geometry and masks",
            "thirdPartyArtwork": False,
            "runtimeDependency": "none",
        },
        "iconAtlas": {
            "resource": "assets/projects_client/textures/ui/shell_icons_96.png",
            "dimensions": [ICON_WIDTH, ICON_HEIGHT],
            "cellSize": ICON_CELL,
            "columns": ICON_COLUMNS,
            "rows": ICON_ROWS,
            "supersample": SAMPLE,
            "sourceScale": ICON_SCALE,
            "antialiasedAlphaOnly": True,
            "keys": [f"projects:{name}" for name, _ in ICON_SOURCES],
            "symbols": [symbol_id for _, symbol_id in ICON_SOURCES],
            "sha256": sha256(icon_data),
        },
        "surfaceMasks": {
            "resource": "assets/projects_client/textures/ui/shell_surface_masks_4x.png",
            "dimensions": [SURFACE_WIDTH, SURFACE_HEIGHT],
            "tileSize": SURFACE_TILE,
            "columns": SURFACE_COLUMNS,
            "logicalTileSize": SURFACE_LOGICAL_TILE,
            "logicalSlice": SURFACE_SOURCE_CAP,
            "sourceCell": SURFACE_LOGICAL_TILE,
            "sourceCap": SURFACE_SOURCE_CAP,
            "sourceRadius": SURFACE_SOURCE_RADIUS,
            "borderWidths": SURFACE_BORDER_WIDTHS,
            "shadowBlurRadius": SURFACE_SHADOW_BLUR,
            "tileLayout": {
                "fill": 0,
                "borderFirstColumn": SURFACE_BORDER_FIRST_COLUMN,
                "shadow": SURFACE_SHADOW_COLUMN,
            },
            "kinds": ["fill", "border", "shadow"],
            "supersample": SAMPLE,
            "antialiasedAlphaOnly": True,
            "sha256": sha256(surface_data),
        },
    }
    MANIFEST_PATH.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST_PATH.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({
        "icon": [ICON_WIDTH, ICON_HEIGHT, sha256(icon_data)],
        "surface": [SURFACE_WIDTH, SURFACE_HEIGHT, sha256(surface_data)],
        "manifest": str(MANIFEST_PATH),
    }, sort_keys=True))


if __name__ == "__main__":
    generate()
