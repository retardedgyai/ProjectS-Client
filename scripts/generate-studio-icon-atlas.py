"""Generate the original ProjectS Studio 24px icon atlas.

The vectors below are the small atlas subset's ProjectS procedural geometry,
expanded to the full stable catalog order so every cell has a deterministic
preview.  The output is white alpha-only artwork: runtime tinting remains in
the adapter and the procedural renderer remains the authoritative fallback.
No third-party artwork or library is used.
"""

from __future__ import annotations

import math
import struct
import zlib
from pathlib import Path


CELL = 24
COLUMNS = 8
ROWS = 4
SAMPLE = 4
WIDTH = CELL * COLUMNS
HEIGHT = CELL * ROWS
KEYS = [
    "select", "move", "rotate", "scale", "shape", "motion", "phase", "trail",
    "add", "duplicate", "delete", "undo", "redo", "play", "pause", "stop",
    "restart", "loop", "search", "settings", "close", "timeline", "particle",
    "appearance", "inspector", "light", "dark",
]


def line(x1, y1, x2, y2, width=1.7):
    return ("line", (x1, y1, x2, y2, width))


def circle(x, y, radius, filled=False, width=1.7):
    return ("circle", (x, y, radius, filled, width))


def polygon(points, filled=False, width=1.7):
    return ("polygon", (tuple(points), filled, width))


def polyline(points, width=1.7):
    return ("polyline", (tuple(points), width))


def rectangle(x, y, width, height, filled=False, stroke=1.7):
    return ("rectangle", (x, y, width, height, filled, stroke))


def dot(x, y, radius):
    return circle(x, y, radius, True, 0)


def geometry(name):
    p = lambda *values: tuple(values)
    if name == "select": return [polygon([p(4, 3), p(4, 20), p(9, 15), p(13, 21), p(16, 19), p(11, 13), p(20, 13), p(4, 3)])]
    if name == "move": return [line(12, 3, 12, 21), line(3, 12, 21, 12), line(12, 3, 9, 6), line(12, 3, 15, 6), line(12, 21, 9, 18), line(12, 21, 15, 18), line(3, 12, 6, 9), line(3, 12, 6, 15), line(21, 12, 18, 9), line(21, 12, 18, 15)]
    if name in ("rotate", "restart"): return [circle(12, 12, 8), line(12, 4, 17, 4), line(17, 4, 17, 9), line(17, 4, 14, 7)]
    if name == "scale": return [polygon([p(4, 10), p(4, 4), p(10, 4)]), polygon([p(14, 4), p(20, 4), p(20, 10)]), polygon([p(4, 14), p(4, 20), p(10, 20)]), polygon([p(14, 20), p(20, 20), p(20, 14)])]
    if name == "shape": return [circle(8, 9, 5), polygon([p(14, 4), p(20, 7), p(18, 17), p(12, 14), p(14, 4)])]
    if name == "motion": return [line(3, 7, 12, 7), line(7, 12, 21, 12), line(3, 17, 14, 17), line(12, 5, 15, 7), line(12, 9, 15, 7), line(21, 10, 21, 12), line(21, 12, 18, 14)]
    if name == "phase": return [polyline([p(2, 13), p(6, 13), p(8, 5), p(11, 19), p(14, 9), p(17, 14), p(22, 14)])]
    if name == "trail": return [polyline([p(3, 18), p(8, 15), p(12, 12), p(17, 8), p(21, 5)]), dot(4, 18, 2), dot(10, 13, 1.6), dot(16, 8, 1.25), dot(21, 5, .9)]
    if name == "add": return [line(12, 4, 12, 20), line(4, 12, 20, 12)]
    if name == "duplicate": return [rectangle(4, 7, 11, 13), rectangle(9, 4, 11, 13)]
    if name == "delete": return [rectangle(6, 7, 12, 14), line(5, 5, 19, 5), line(9, 3, 15, 3), line(10, 10, 10, 18), line(14, 10, 14, 18)]
    if name in ("undo", "redo"):
        right = name == "redo"
        tip, inner = (20, 15) if right else (4, 9)
        return [polygon([p(tip, 7), p(inner, 12), p(tip, 17)]), line(4 if right else 20, 12, tip, 12), line(tip, 7, tip, 4), line(tip, 4, tip - (4 if right else -4), 4)]
    if name == "play": return [polygon([p(7, 4), p(19, 12), p(7, 20)], True)]
    if name == "pause": return [rectangle(5, 4, 5, 16, True), rectangle(14, 4, 5, 16, True)]
    if name == "stop": return [rectangle(5, 5, 14, 14, True)]
    if name == "loop": return [polygon([p(5, 8), p(8, 5), p(16, 5), p(19, 8)]), polygon([p(19, 16), p(16, 19), p(8, 19), p(5, 16)]), line(5, 8, 5, 13), line(5, 8, 9, 8), line(19, 16, 19, 11), line(19, 16, 15, 16)]
    if name == "search": return [circle(10, 10, 6), line(14, 14, 20, 20)]
    if name == "settings": return [circle(12, 12, 5), circle(12, 12, 9), rectangle(11, 2, 2, 4, True), rectangle(11, 18, 2, 4, True), rectangle(2, 11, 4, 2, True), rectangle(18, 11, 4, 2, True)]
    if name == "close": return [line(5, 5, 19, 19), line(19, 5, 5, 19)]
    if name == "timeline": return [line(3, 12, 21, 12), line(5, 8, 5, 16), line(10, 9, 10, 15), line(15, 8, 15, 16), line(20, 9, 20, 15), dot(15, 12, 2)]
    if name == "particle": return [dot(6, 8, 2.5), dot(17, 6, 2), dot(14, 18, 2.75), line(8, 9, 15, 7), line(8, 10, 13, 16), line(16, 8, 15, 16)]
    if name == "appearance": return [circle(12, 12, 9), dot(7, 9, 1.5), dot(12, 6, 1.5), dot(17, 11, 1.5), dot(10, 17, 1.5)]
    if name == "inspector": return [line(4, 6, 20, 6), line(4, 12, 20, 12), line(4, 18, 20, 18), dot(9, 6, 2), dot(16, 12, 2), dot(7, 18, 2)]
    if name == "light": return [circle(12, 10, 5), line(12, 2, 12, 4), line(12, 16, 12, 21), line(4, 10, 2, 10), line(20, 10, 22, 10), line(6, 4, 4, 2), line(18, 4, 20, 2)]
    if name == "dark": return [circle(11, 11, 8), polygon([p(15, 4), p(20, 8), p(20, 15), p(16, 19), p(12, 19), p(16, 14), p(17, 9)], True)]
    raise ValueError(name)


def distance_to_segment(x, y, x1, y1, x2, y2):
    dx, dy = x2 - x1, y2 - y1
    length_squared = dx * dx + dy * dy
    t = 0 if length_squared == 0 else max(0, min(1, ((x - x1) * dx + (y - y1) * dy) / length_squared))
    return math.hypot(x - (x1 + t * dx), y - (y1 + t * dy))


def inside_polygon(x, y, points):
    result = False
    for index, (x1, y1) in enumerate(points):
        x2, y2 = points[index - 1]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
            result = not result
    return result


def paint(canvas, primitive, ox, oy):
    kind, values = primitive
    if kind == "line":
        x1, y1, x2, y2, stroke = values
        bounds = (min(x1, x2) - stroke, min(y1, y2) - stroke, max(x1, x2) + stroke, max(y1, y2) + stroke)
        for y in range(max(0, int((oy + bounds[1]) * SAMPLE)), min(HEIGHT * SAMPLE, int((oy + bounds[3] + 1) * SAMPLE))):
            for x in range(max(0, int((ox + bounds[0]) * SAMPLE)), min(WIDTH * SAMPLE, int((ox + bounds[2] + 1) * SAMPLE))):
                if distance_to_segment(x / SAMPLE, y / SAMPLE, ox + x1, oy + y1, ox + x2, oy + y2) <= stroke / 2:
                    canvas[y][x] = 255
    elif kind == "polyline":
        points, stroke = values
        bounds = (min(point[0] for point in points) - stroke, min(point[1] for point in points) - stroke,
                  max(point[0] for point in points) + stroke, max(point[1] for point in points) + stroke)
        for py in range(max(0, int((oy + bounds[1]) * SAMPLE)), min(HEIGHT * SAMPLE, int((oy + bounds[3] + 1) * SAMPLE))):
            for px in range(max(0, int((ox + bounds[0]) * SAMPLE)), min(WIDTH * SAMPLE, int((ox + bounds[2] + 1) * SAMPLE))):
                lx, ly = px / SAMPLE - ox, py / SAMPLE - oy
                edge = any(distance_to_segment(lx, ly, *points[index], *points[index + 1]) <= stroke / 2
                           for index in range(len(points) - 1))
                if edge:
                    canvas[py][px] = 255
    elif kind == "circle":
        cx, cy, radius, filled, stroke = values
        for y in range(max(0, int((oy + cy - radius - 1) * SAMPLE)), min(HEIGHT * SAMPLE, int((oy + cy + radius + 1) * SAMPLE))):
            for x in range(max(0, int((ox + cx - radius - 1) * SAMPLE)), min(WIDTH * SAMPLE, int((ox + cx + radius + 1) * SAMPLE))):
                distance = math.hypot(x / SAMPLE - ox - cx, y / SAMPLE - oy - cy)
                if (filled and distance <= radius) or (not filled and abs(distance - radius) <= stroke / 2): canvas[y][x] = 255
    elif kind == "rectangle":
        x, y, width, height, filled, stroke = values
        for py in range(max(0, int((oy + y - 1) * SAMPLE)), min(HEIGHT * SAMPLE, int((oy + y + height + 1) * SAMPLE))):
            for px in range(max(0, int((ox + x - 1) * SAMPLE)), min(WIDTH * SAMPLE, int((ox + x + width + 1) * SAMPLE))):
                lx, ly = px / SAMPLE - ox, py / SAMPLE - oy
                if filled and x <= lx <= x + width and y <= ly <= y + height or not filled and (abs(lx - x) <= stroke / 2 or abs(lx - x - width) <= stroke / 2 or abs(ly - y) <= stroke / 2 or abs(ly - y - height) <= stroke / 2): canvas[py][px] = 255
    elif kind == "polygon":
        points, filled, stroke = values
        bounds = (min(point[0] for point in points) - stroke, min(point[1] for point in points) - stroke, max(point[0] for point in points) + stroke, max(point[1] for point in points) + stroke)
        for py in range(max(0, int((oy + bounds[1]) * SAMPLE)), min(HEIGHT * SAMPLE, int((oy + bounds[3] + 1) * SAMPLE))):
            for px in range(max(0, int((ox + bounds[0]) * SAMPLE)), min(WIDTH * SAMPLE, int((ox + bounds[2] + 1) * SAMPLE))):
                lx, ly = px / SAMPLE - ox, py / SAMPLE - oy
                edge = any(distance_to_segment(lx, ly, *points[index], *points[(index + 1) % len(points)]) <= stroke / 2 for index in range(len(points)))
                if (filled and inside_polygon(lx, ly, points)) or edge: canvas[py][px] = 255


def png_chunk(kind, payload):
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)


def write_png(path):
    canvas = [[0 for _ in range(WIDTH * SAMPLE)] for _ in range(HEIGHT * SAMPLE)]
    for index, name in enumerate(KEYS):
        ox = (index % COLUMNS) * CELL
        oy = (index // COLUMNS) * CELL
        for primitive in geometry(name): paint(canvas, primitive, ox, oy)
    rows = []
    for y in range(HEIGHT):
        row = bytearray([0])
        for x in range(WIDTH):
            alpha = sum(canvas[y * SAMPLE + sy][x * SAMPLE + sx] for sy in range(SAMPLE) for sx in range(SAMPLE)) // (SAMPLE * SAMPLE)
            row.extend((255, 255, 255, alpha))
        rows.append(bytes(row))
    data = b"\x89PNG\r\n\x1a\n" + png_chunk(b"IHDR", struct.pack(">IIBBBBB", WIDTH, HEIGHT, 8, 6, 0, 0, 0)) + png_chunk(b"IDAT", zlib.compress(b"".join(rows), 9)) + png_chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


if __name__ == "__main__":
    write_png(Path(__file__).parent.parent / "minecraft-adapter/src/client/resources/assets/projects_client/textures/ui/studio_icons_24.png")
