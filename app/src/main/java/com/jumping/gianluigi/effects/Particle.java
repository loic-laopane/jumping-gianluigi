package com.jumping.gianluigi.effects;

import android.graphics.Canvas;
import android.graphics.Paint;

/** Single pixel-art explosion particle. */
public class Particle {
    public float x, y;
    public float vx, vy;
    public float life;      // seconds remaining
    public float maxLife;
    public int color;
    public float size;

    public Particle(float x, float y, float vx, float vy, float life, int color, float size) {
        this.x = x; this.y = y;
        this.vx = vx; this.vy = vy;
        this.life = life; this.maxLife = life;
        this.color = color; this.size = size;
    }

    public boolean isDead() { return life <= 0; }

    public void update(float dt) {
        x += vx * dt;
        y += vy * dt;
        vy += 400f * dt;  // gravity on particles
        life -= dt;
    }

    public void draw(Canvas canvas, Paint paint) {
        float alpha = life / maxLife;
        int a = (int)(alpha * 255);
        paint.setColor((color & 0x00FFFFFF) | (a << 24));
        canvas.drawRect(x, y, x + size, y + size, paint);
    }
}
