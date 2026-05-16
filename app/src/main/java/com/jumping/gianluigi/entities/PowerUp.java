package com.jumping.gianluigi.entities;

import android.graphics.Canvas;
import android.graphics.Paint;

import static com.jumping.gianluigi.Constants.P;

public class PowerUp {
    public static final int NONE       = -1;
    public static final int STAR       = 0;
    public static final int SPEED      = 1;
    public static final int SUPER_JUMP = 2;

    public float x, y;
    public final int type;
    public boolean collected;

    private float animTimer;
    private float bobOffset;

    public PowerUp(float x, float y, int type) {
        this.x = x; this.y = y; this.type = type;
        this.collected = false;
    }

    public void update(float dt) {
        animTimer += dt;
        bobOffset = (float) Math.sin(animTimer * 3.0) * 4f;
    }

    public float drawY() { return y + bobOffset; }

    public android.graphics.RectF getBounds() {
        float dy = drawY();
        return new android.graphics.RectF(x, dy, x + P * 6, dy + P * 6);
    }

    public void draw(Canvas canvas, Paint paint) {
        if (collected) return;
        float px = x;
        float py = drawY();

        switch (type) {
            case STAR:
                drawStar(canvas, paint, px, py);
                break;
            case SPEED:
                drawLightning(canvas, paint, px, py);
                break;
            case SUPER_JUMP:
                drawSpring(canvas, paint, px, py);
                break;
        }
    }

    private void drawStar(Canvas canvas, Paint paint, float px, float py) {
        // Yellow star (pixel art)
        paint.setColor(0xFFFFDD00);
        // Center
        canvas.drawRect(px + P*2, py + P, px + P*4, py + P*5, paint);
        canvas.drawRect(px + P, py + P*2, px + P*5, py + P*4, paint);
        // Tips
        paint.setColor(0xFFFFAA00);
        canvas.drawRect(px + P*2, py, px + P*4, py + P, paint);
        canvas.drawRect(px + P*2, py + P*5, px + P*4, py + P*6, paint);
        canvas.drawRect(px, py + P*2, px + P, py + P*4, paint);
        canvas.drawRect(px + P*5, py + P*2, px + P*6, py + P*4, paint);
    }

    private void drawLightning(Canvas canvas, Paint paint, float px, float py) {
        // Orange lightning bolt
        paint.setColor(0xFFFF8800);
        canvas.drawRect(px + P*3, py, px + P*6, py + P*2, paint);
        canvas.drawRect(px + P*2, py + P*2, px + P*5, py + P*4, paint);
        canvas.drawRect(px + P, py + P*4, px + P*4, py + P*6, paint);
        paint.setColor(0xFFFFCC00);
        canvas.drawRect(px + P*3, py + P, px + P*5, py + P*2, paint);
        canvas.drawRect(px + P*2, py + P*3, px + P*4, py + P*4, paint);
    }

    private void drawSpring(Canvas canvas, Paint paint, float px, float py) {
        // Green spring
        paint.setColor(0xFF00CC44);
        canvas.drawRect(px + P, py, px + P*5, py + P, paint);
        paint.setColor(0xFF00AA33);
        canvas.drawRect(px, py + P, px + P*6, py + P*2, paint);
        canvas.drawRect(px + P, py + P*2, px + P*5, py + P*3, paint);
        canvas.drawRect(px, py + P*3, px + P*6, py + P*4, paint);
        canvas.drawRect(px + P, py + P*4, px + P*5, py + P*5, paint);
        paint.setColor(0xFF008822);
        canvas.drawRect(px + P, py + P*5, px + P*5, py + P*6, paint);
    }
}
