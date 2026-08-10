package io.github.gyai.projects.client.ui.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Allocation-free integer geometry on a normalized 16x16 design grid. */
final class ProjectSIconGeometry {
    private GuiGraphicsExtractor graphics;
    private int x;
    private int y;
    private int size;
    private int color;
    private int stroke;

    ProjectSIconGeometry() { }

    ProjectSIconGeometry reset(
            GuiGraphicsExtractor graphics, int x, int y, int size, int color
    ) {
        this.graphics = graphics;
        this.x = x;
        this.y = y;
        this.size = Math.max(1, size);
        this.color = color;
        stroke = size >= 24 ? 2 : 1;
        return this;
    }

    int px(int point) {
        return x + Math.round(point * (size - 1) / 15.0f);
    }

    int py(int point) {
        return y + Math.round(point * (size - 1) / 15.0f);
    }

    void pixel(int gx, int gy) {
        int drawX = px(gx);
        int drawY = py(gy);
        graphics.fill(drawX, drawY,
                Math.min(x + size, drawX + stroke),
                Math.min(y + size, drawY + stroke), color);
    }

    void fill(int left, int top, int right, int bottom) {
        graphics.fill(px(left), py(top),
                Math.min(x + size, px(right) + stroke),
                Math.min(y + size, py(bottom) + stroke), color);
    }

    void h(int left, int right, int row) {
        int top = py(row);
        graphics.fill(px(left), top,
                Math.min(x + size, px(right) + stroke),
                Math.min(y + size, top + stroke), color);
    }

    void v(int column, int top, int bottom) {
        int left = px(column);
        graphics.fill(left, py(top),
                Math.min(x + size, left + stroke),
                Math.min(y + size, py(bottom) + stroke), color);
    }

    void outline(int left, int top, int right, int bottom) {
        h(left, right, top);
        h(left, right, bottom);
        v(left, top, bottom);
        v(right, top, bottom);
    }

    void line(int x1, int y1, int x2, int y2) {
        int drawX = px(x1);
        int drawY = py(y1);
        int targetX = px(x2);
        int targetY = py(y2);
        int deltaX = Math.abs(targetX - drawX);
        int deltaY = Math.abs(targetY - drawY);
        int stepX = drawX < targetX ? 1 : -1;
        int stepY = drawY < targetY ? 1 : -1;
        int error = deltaX - deltaY;
        while (true) {
            graphics.fill(drawX, drawY,
                    Math.min(x + size, drawX + stroke),
                    Math.min(y + size, drawY + stroke), color);
            if (drawX == targetX && drawY == targetY) return;
            int twice = error * 2;
            if (twice > -deltaY) {
                error -= deltaY;
                drawX += stepX;
            }
            if (twice < deltaX) {
                error += deltaX;
                drawY += stepY;
            }
        }
    }

    void diamond(int left, int top, int right, int bottom) {
        int middleX = (left + right) / 2;
        int middleY = (top + bottom) / 2;
        line(middleX, top, right, middleY);
        line(right, middleY, middleX, bottom);
        line(middleX, bottom, left, middleY);
        line(left, middleY, middleX, top);
    }

    void circle(int left, int top, int right, int bottom) {
        h(left + 2, right - 2, top);
        h(left + 2, right - 2, bottom);
        v(left, top + 2, bottom - 2);
        v(right, top + 2, bottom - 2);
        pixel(left + 1, top + 1);
        pixel(right - 1, top + 1);
        pixel(left + 1, bottom - 1);
        pixel(right - 1, bottom - 1);
    }
}
